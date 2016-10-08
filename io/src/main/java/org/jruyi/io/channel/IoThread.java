/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jruyi.io.channel;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.jruyi.common.ICloseable;
import org.jruyi.common.StrUtil;
import org.jruyi.io.common.IoEventQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class IoThread implements ICloseable, Runnable, ISelector {

	private static final Logger c_logger = LoggerFactory.getLogger(IoThread.class);

	private static final long ONE_SEC = 1000L;

	private TimerWheel m_timerWheel;
	private Selector m_selector;

	private volatile boolean m_needWake = true;
	private Thread m_thread;

	private final IoEventQueue<ISelectableChannel> m_acceptQueue = new IoEventQueue<>();
	private final IoEventQueue<ISelectableChannel> m_connectQueue = new IoEventQueue<>();
	private final IoEventQueue<IoEvent> m_writeQueue = new IoEventQueue<>();

	public void open(int channelAdminId, int id) throws Exception {
		m_timerWheel = new TimerWheel(120);
		m_selector = Selector.open();
		m_needWake = true;
		final Thread thread = new Thread(this, "jruyi-io-" + channelAdminId + "-" + id);
		m_thread = thread;
		thread.start();
	}

	@Override
	public void close() {
		if (m_thread != null) {
			m_thread.interrupt();
			try {
				m_thread.join();
			} catch (InterruptedException e) {
			} finally {
				m_thread = null;
			}
		}

		if (m_selector != null) {
			try {
				m_selector.close();
			} catch (Throwable t) {
				c_logger.error("Failed to close the selector", t);
			}
			m_selector = null;
		}

		m_timerWheel = null;
	}

	@Override
	public void run() {
		final Thread currentThread = Thread.currentThread();

		c_logger.info("{} started", currentThread.getName());

		final TimerWheel timerWheel = m_timerWheel;
		final IoEventQueue<ISelectableChannel> acceptQueue = m_acceptQueue;
		final IoEventQueue<ISelectableChannel> connectQueue = m_connectQueue;
		final IoEventQueue<IoEvent> writeQueue = m_writeQueue;
		final Selector selector = m_selector;

		long sleepTime = 0L;
		long prevTime = 0L;
		try {
			for (;;) {
				final int n = selector.select(sleepTime);
				if (currentThread.isInterrupted())
					break;

				long currentTime = System.currentTimeMillis();
				if (timerWheel.scheduledTimers() > 0) {
					if (currentTime >= prevTime) {
						timerWheel.tick();
						prevTime += ONE_SEC;
					}
				} else {
					prevTime = currentTime + ONE_SEC;
				}

				if (n > 0) {
					final Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
					while (iter.hasNext()) {
						final SelectionKey key = iter.next();
						iter.remove();

						final ISelectableChannel channel = (ISelectableChannel) key.attachment();
						try {
							if (key.isConnectable()) {
								key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
								channel.onConnect();
							} else {
								if (key.isReadable()) {
									key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
									channel.onRead();
								}
								if (key.isWritable()) {
									key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
									channel.onWrite();
								}
							}
						} catch (RejectedExecutionException e) {
						} catch (CancelledKeyException e) {
						} catch (Throwable t) {
							c_logger.warn(StrUtil.join(currentThread.getName(), ": ", channel), t);
						}
					}
				}

				m_needWake = true;

				procIoEvents(acceptQueue, SelectorOp.ACCEPT);
				procIoEvents(connectQueue, SelectorOp.CONNECT);
				procIoEvents(writeQueue);

				if (timerWheel.scheduledTimers() > 0) {
					while ((sleepTime = prevTime - System.currentTimeMillis()) <= 0) {
						timerWheel.tick();
						prevTime += ONE_SEC;
					}
				} else
					sleepTime = 0L;
			}
		} catch (ClosedSelectorException e) {
			c_logger.error(StrUtil.join(currentThread.getName(), ": selector closed unexpectedly"), e);
		} catch (Throwable t) {
			c_logger.error(StrUtil.join(currentThread.getName(), ": unexpected error"), t);
		}

		c_logger.info("{} stopped", currentThread.getName());
	}

	@Override
	public Timer createTimer(Channel channel) {
		return m_timerWheel.createTimer(channel);
	}

	@Override
	public void accept(ISelectableChannel channel) {
		publish(SelectorOp.ACCEPT, channel, m_acceptQueue);
	}

	@Override
	public void connect(ISelectableChannel channel) {
		publish(SelectorOp.CONNECT, channel, m_connectQueue);
	}

	@Override
	public void write(IoEvent ioEvent) {
		if (isThisIoThread()) {
			ioEvent.task().run(ioEvent.msg(), ioEvent.filters(), ioEvent.filterCount());
			return;
		}

		m_writeQueue.put(ioEvent);

		if (m_needWake) {
			m_needWake = false;
			m_selector.wakeup();
		}
	}

	@Override
	public Selector selector() {
		return m_selector;
	}

	private void procIoEvents(IoEventQueue<IoEvent> queue) {
		final List<IoEvent> ioEvents = queue.elements();
		if (ioEvents == null)
			return;

		for (IoEvent ioEvent : ioEvents)
			ioEvent.task().run(ioEvent.msg(), ioEvent.filters(), ioEvent.filterCount());

		queue.cache(ioEvents);
	}

	private void procIoEvents(IoEventQueue<ISelectableChannel> queue, SelectorOp selectorOp) {
		final List<ISelectableChannel> channels = queue.elements();
		if (channels == null)
			return;

		for (ISelectableChannel channel : channels)
			selectorOp.run(channel);

		queue.cache(channels);
	}

	private void publish(SelectorOp selectorOp, ISelectableChannel channel, IoEventQueue<ISelectableChannel> queue) {
		if (isThisIoThread()) {
			selectorOp.run(channel);
			return;
		}

		queue.put(channel);

		if (m_needWake) {
			m_needWake = false;
			m_selector.wakeup();
		}
	}

	private boolean isThisIoThread() {
		return Thread.currentThread() == m_thread;
	}
}
