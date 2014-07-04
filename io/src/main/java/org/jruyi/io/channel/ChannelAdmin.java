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
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.jruyi.common.StrUtil;
import org.jruyi.io.common.SyncPutQueue;
import org.jruyi.timeoutadmin.ITimeoutAdmin;
import org.jruyi.timeoutadmin.ITimeoutNotifier;
import org.jruyi.workshop.IWorkshop;
import org.jruyi.workshop.WorkshopConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "jruyi.io.channeladmin", xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class ChannelAdmin implements IChannelAdmin {

	private static final Logger c_logger = LoggerFactory
			.getLogger(ChannelAdmin.class);

	private SelectorThread[] m_sts;
	private int m_count;

	private IWorkshop m_workshop;
	private ITimeoutAdmin m_tm;

	final class SelectorThread implements Runnable, ISelector {

		private SyncPutQueue<ISelectableChannel> m_registerQueue;
		private SyncPutQueue<ISelectableChannel> m_connectQueue;
		private SyncPutQueue<ISelectableChannel> m_readQueue;
		private SyncPutQueue<ISelectableChannel> m_writeQueue;
		private Thread m_thread;
		private Selector m_selector;

		public void open(int id) throws Exception {
			m_registerQueue = new SyncPutQueue<ISelectableChannel>();
			m_connectQueue = new SyncPutQueue<ISelectableChannel>();
			m_readQueue = new SyncPutQueue<ISelectableChannel>();
			m_writeQueue = new SyncPutQueue<ISelectableChannel>();

			m_selector = Selector.open();
			m_thread = new Thread(this, "SelectorThread-" + id);
			m_thread.start();
		}

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

			m_writeQueue = null;
			m_readQueue = null;
			m_connectQueue = null;
			m_registerQueue = null;
		}

		@Override
		public void run() {
			final Thread currentThread = Thread.currentThread();

			c_logger.info("{} started", currentThread.getName());

			final Selector selector = m_selector;
			try {
				for (;;) {
					final int n;
					try {
						n = selector.select();
					} catch (IOException e) {
						c_logger.warn(StrUtil.join(currentThread.getName(),
								": selector error"), e);
						continue;
					}

					if (currentThread.isInterrupted())
						break;

					procConnect();
					procRegister();
					procRead();
					procWrite();

					if (n < 1)
						continue;

					final IWorkshop workshop = m_workshop;
					final Iterator<SelectionKey> iter = selector.selectedKeys()
							.iterator();
					while (iter.hasNext()) {
						SelectionKey key = iter.next();
						iter.remove();

						ISelectableChannel channel = (ISelectableChannel) key
								.attachment();
						try {
							if (key.isConnectable()) {
								key.interestOps(key.interestOps()
										& ~SelectionKey.OP_CONNECT);
								channel.onConnect();
							} else {
								if (key.isReadable()) {
									key.interestOps(key.interestOps()
											& ~SelectionKey.OP_READ);
									workshop.run(channel.onRead());
								}

								if (key.isWritable()) {
									key.interestOps(key.interestOps()
											& ~SelectionKey.OP_WRITE);
									workshop.run(channel.onWrite());
								}
							}
						} catch (RejectedExecutionException e) {
						} catch (CancelledKeyException e) {
						} catch (Throwable t) {
							c_logger.warn(StrUtil.join(currentThread.getName(),
									": ", channel), t);
						}
					}
				}
			} catch (ClosedSelectorException e) {
				c_logger.error(StrUtil.join(currentThread.getName(),
						": selector closed unexpectedly"), e);
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(currentThread.getName(),
						": unexpected error"), t);
			}

			c_logger.info("{} stopped", currentThread.getName());
		}

		public void onRegisterRequired(ISelectableChannel channel) {
			m_registerQueue.put(channel);
			m_selector.wakeup();
		}

		public void onConnectRequired(ISelectableChannel channel) {
			m_connectQueue.put(channel);
			m_selector.wakeup();
		}

		@Override
		public Selector selector() {
			return m_selector;
		}

		@Override
		public void onReadRequired(ISelectableChannel channel) {
			m_readQueue.put(channel);
			m_selector.wakeup();
		}

		@Override
		public void onWriteRequired(ISelectableChannel channel) {
			m_writeQueue.put(channel);
			m_selector.wakeup();
		}

		private void procRegister() {
			final SyncPutQueue<ISelectableChannel> registerQueue = m_registerQueue;
			ISelectableChannel channel;
			while ((channel = registerQueue.poll()) != null)
				channel.register(this, SelectionKey.OP_READ);
		}

		private void procConnect() {
			final SyncPutQueue<ISelectableChannel> connectQueue = m_connectQueue;
			ISelectableChannel channel;
			while ((channel = connectQueue.poll()) != null)
				channel.register(this, SelectionKey.OP_CONNECT);
		}

		private void procRead() {
			ISelectableChannel channel;
			SyncPutQueue<ISelectableChannel> readQueue = m_readQueue;
			while ((channel = readQueue.poll()) != null) {
				try {
					channel.interestOps(SelectionKey.OP_READ);
				} catch (CancelledKeyException e) {
				} catch (Throwable t) {
					channel.onException(t);
				}
			}
		}

		private void procWrite() {
			ISelectableChannel channel;
			SyncPutQueue<ISelectableChannel> writeQueue = m_writeQueue;
			while ((channel = writeQueue.poll()) != null) {
				try {
					channel.interestOps(SelectionKey.OP_WRITE);
				} catch (CancelledKeyException e) {
				} catch (Throwable t) {
					channel.onException(t);
				}
			}
		}
	}

	@Override
	public void onRegisterRequired(ISelectableChannel channel) {
		getSelectorThread(channel).onRegisterRequired(channel);
	}

	@Override
	public void onConnectRequired(ISelectableChannel channel) {
		getSelectorThread(channel).onConnectRequired(channel);
	}

	@Override
	public void onWrite(ISelectableChannel channel) {
		m_workshop.run(channel.onWrite());
	}

	@Override
	public ITimeoutNotifier createTimeoutNotifier(ISelectableChannel channel) {
		return m_tm.createNotifier(channel);
	}

	@Reference(name = "workshop", policy = ReferencePolicy.DYNAMIC, target = WorkshopConstants.DEFAULT_WORKSHOP_TARGET)
	synchronized void setWorkshop(IWorkshop workshop) {
		m_workshop = workshop;
	}

	synchronized void unsetWorkshop(IWorkshop workshop) {
		if (m_workshop == workshop)
			m_workshop = null;
	}

	@Reference(name = "timeoutAdmin", policy = ReferencePolicy.DYNAMIC)
	synchronized void setTimeoutAdmin(ITimeoutAdmin tm) {
		m_tm = tm;
	}

	synchronized void unsetTimeoutAdmin(ITimeoutAdmin tm) {
		if (m_tm == tm)
			m_tm = null;
	}

	@Modified
	void modified(Map<String, ?> properties) throws Exception {
		final int count = numberOfSelectors(properties);
		if (count < 1)
			throw new Exception("Number of selectors must be positive.");

		int curCount = m_count;
		if (count == curCount)
			return;

		SelectorThread[] sts = m_sts;
		if (count > m_sts.length) {
			sts = new SelectorThread[count];
			System.arraycopy(m_sts, 0, sts, 0, curCount);
			m_sts = sts;
		}

		while (count < curCount) {
			sts[--curCount].close();
			sts[curCount] = null;
		}

		while (curCount < count) {
			SelectorThread st = new SelectorThread();
			try {
				st.open(curCount);
			} catch (Exception e) {
				st.close();
				break;
			}
			sts[curCount++] = st;
		}

		m_count = curCount;
	}

	void activate(Map<String, ?> properties) throws Exception {
		c_logger.info("Activating ChannelAdmin...");

		int count = numberOfSelectors(properties);
		final SelectorThread[] sts = new SelectorThread[count];
		for (int i = 0; i < count; ++i) {
			SelectorThread st = new SelectorThread();
			try {
				st.open(i);
			} catch (Exception e) {
				st.close();
				while (i > 0)
					sts[--i].close();
				throw e;
			}
			sts[i] = st;
		}

		m_count = count;
		m_sts = sts;

		c_logger.info("ChannelAdmin activated");
	}

	void deactivate() {
		c_logger.info("Deactivating ChannelAdmin...");

		final SelectorThread[] sts = m_sts;
		m_sts = null;
		for (SelectorThread st : sts)
			st.close();

		c_logger.info("ChannelAdmin deactivated");
	}

	private SelectorThread getSelectorThread(ISelectableChannel channel) {
		return m_sts[channel.id().intValue() % m_count];
	}

	private static int numberOfSelectors(Map<String, ?> properties) {
		Object numberOfSelectorThreads = properties
				.get("numberOfSelectorThreads");
		int count;
		if (numberOfSelectorThreads == null
				|| (count = (Integer) numberOfSelectorThreads) < 1) {
			int i = Runtime.getRuntime().availableProcessors();
			count = 0;
			while ((i >>>= 1) > 0)
				++count;
			if (count < 1)
				count = 1;
		}

		return count;
	}
}
