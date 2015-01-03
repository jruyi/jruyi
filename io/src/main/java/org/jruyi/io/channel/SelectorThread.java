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

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.RejectedExecutionException;

import org.jruyi.common.ICloseable;
import org.jruyi.common.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.EventPoller;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;

final class SelectorThread implements ICloseable, Runnable, ISelector, EventPoller.Handler<SelectorEvent> {

	private static final Logger c_logger = LoggerFactory.getLogger(SelectorThread.class);

	private RingBuffer<SelectorEvent> m_ringBuffer;
	private Thread m_thread;
	private Selector m_selector;

	@Override
	public boolean onEvent(SelectorEvent event, long sequence, boolean endOfBatch) throws Exception {
		event.op().run(this, event.channel());
		return true;
	}

	public void open(int id, int capacity) throws Exception {
		m_selector = Selector.open();
		m_thread = new Thread(this, "jruyi-selector-" + id);
		m_ringBuffer = RingBuffer.createMultiProducer(SelectorEventFactory.INST, capacity);

		m_thread.start();
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

		m_ringBuffer = null;
	}

	@Override
	public void run() {
		final Thread currentThread = Thread.currentThread();

		c_logger.info("{} started", currentThread.getName());

		final EventPoller<SelectorEvent> poller = m_ringBuffer.newPoller();
		final Selector selector = m_selector;
		try {
			for (;;) {
				final int n;
				try {
					n = selector.select();
				} catch (IOException e) {
					c_logger.warn(StrUtil.join(currentThread.getName(), ": selector error"), e);
					continue;
				}

				if (currentThread.isInterrupted())
					break;

				poller.poll(this);

				if (n < 1)
					continue;

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
		} catch (ClosedSelectorException e) {
			c_logger.error(StrUtil.join(currentThread.getName(), ": selector closed unexpectedly"), e);
		} catch (Throwable t) {
			c_logger.error(StrUtil.join(currentThread.getName(), ": unexpected error"), t);
		}

		c_logger.info("{} stopped", currentThread.getName());
	}

	public void onRegisterRequired(ISelectableChannel channel) {
		publish(SelectorOp.REGISTER, channel);
	}

	public void onConnectRequired(ISelectableChannel channel) {
		publish(SelectorOp.CONNECT, channel);
	}

	@Override
	public Selector selector() {
		return m_selector;
	}

	@Override
	public void onReadRequired(ISelectableChannel channel) {
		publish(SelectorOp.READ, channel);
	}

	@Override
	public void onWriteRequired(ISelectableChannel channel) {
		publish(SelectorOp.WRITE, channel);
	}

	private void publish(SelectorOp selectorOp, ISelectableChannel channel) {
		final RingBuffer<SelectorEvent> ringBuffer = m_ringBuffer;
		long sequence;
		try {
			sequence = ringBuffer.tryNext();
		} catch (InsufficientCapacityException e) {
			c_logger.warn("If you see this message quite a few, please try increasing numberOfSelectorThreads");
			sequence = ringBuffer.next();
		}
		try {
			final SelectorEvent event = ringBuffer.get(sequence);
			event.op(selectorOp);
			event.channel(channel);
		} finally {
			ringBuffer.publish(sequence);
		}
		m_selector.wakeup();
	}
}
