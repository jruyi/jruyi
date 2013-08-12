/**
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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jruyi.common.StrUtil;
import org.jruyi.io.common.SyncPutQueue;
import org.jruyi.timeoutadmin.ITimeoutAdmin;
import org.jruyi.timeoutadmin.ITimeoutNotifier;
import org.jruyi.workshop.IWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Component(name = "jruyi.io.channeladmin", createPid = false)
public final class ChannelAdmin implements IChannelAdmin {

	private static final Logger c_logger = LoggerFactory
			.getLogger(ChannelAdmin.class);
	private SelectorThread[] m_sts;
	private int m_mask;

	@Reference(name = "worker")
	private IWorker m_worker;

	@Reference(name = "timeoutAdmin")
	private ITimeoutAdmin m_tm;

	final class SelectorThread implements Runnable {

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
				} catch (Exception e) {
					c_logger.error("Failed to close the selector", e);
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
			Thread currentThread = Thread.currentThread();

			c_logger.info("{} started", currentThread.getName());

			IWorker worker = m_worker;
			Selector selector = m_selector;
			try {
				for (;;) {
					int n = selector.select();
					if (currentThread.isInterrupted())
						break;

					procConnect();
					procRegister();
					procRead();
					procWrite();

					if (n < 1)
						continue;

					Iterator<SelectionKey> iter = selector.selectedKeys()
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
									worker.run(channel.onRead());
								}

								if (key.isWritable()) {
									key.interestOps(key.interestOps()
											& ~SelectionKey.OP_WRITE);
									worker.run(channel.onWrite());
								}
							}
						} catch (RejectedExecutionException e) {
						} catch (CancelledKeyException e) {
						} catch (Exception e) {
							c_logger.warn(StrUtil.buildString(
									currentThread.getName(), ": ", channel), e);
						}
					}
				}
			} catch (ClosedSelectorException e) {
				c_logger.error(StrUtil.buildString(currentThread.getName(),
						": selector closed unexpectedly"), e);
			} catch (IOException e) {
				c_logger.error(StrUtil.buildString(currentThread.getName(),
						": selector error"), e);
			} catch (Throwable t) {
				c_logger.error(StrUtil.buildString(currentThread.getName(),
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

		public void onReadRequired(ISelectableChannel channel) {
			m_readQueue.put(channel);
			m_selector.wakeup();
		}

		public void onWriteRequired(ISelectableChannel channel) {
			m_writeQueue.put(channel);
			m_selector.wakeup();
		}

		private void procRegister() {
			ISelectableChannel channel;
			SyncPutQueue<ISelectableChannel> registerQueue = m_registerQueue;
			Selector selector = m_selector;
			while ((channel = registerQueue.poll()) != null)
				channel.register(selector, SelectionKey.OP_READ);
		}

		private void procConnect() {
			ISelectableChannel channel;
			SyncPutQueue<ISelectableChannel> connectQueue = m_connectQueue;
			Selector selector = m_selector;
			while ((channel = connectQueue.poll()) != null)
				channel.register(selector, SelectionKey.OP_CONNECT);
		}

		private void procRead() {
			ISelectableChannel channel;
			SyncPutQueue<ISelectableChannel> readQueue = m_readQueue;
			while ((channel = readQueue.poll()) != null) {
				try {
					channel.interestOps(SelectionKey.OP_READ);
				} catch (CancelledKeyException e) {
				} catch (Exception e) {
					channel.onException(e);
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
				} catch (Exception e) {
					channel.onException(e);
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
	public void onReadRequired(ISelectableChannel channel) {
		getSelectorThread(channel).onReadRequired(channel);
	}

	@Override
	public void onWriteRequired(ISelectableChannel channel) {
		getSelectorThread(channel).onWriteRequired(channel);
	}

	@Override
	public ITimeoutNotifier createTimeoutNotifier(ISelectableChannel channel) {
		return m_tm.createNotifier(channel);
	}

	protected void bindWorker(IWorker worker) {
		m_worker = worker;
	}

	protected void unbindWorker(IWorker worker) {
		m_worker = null;
	}

	protected void bindTimeoutAdmin(ITimeoutAdmin tm) {
		m_tm = tm;
	}

	protected void unbindTimeoutAdmin(ITimeoutAdmin tm) {
		m_tm = null;
	}

	protected void activate(Map<String, ?> properties) throws Exception {
		c_logger.info("Activating ChannelAdmin...");

		Integer numberOfSelectorThreads = (Integer) properties
				.get("numberOfSelectorThreads");
		int i = numberOfSelectorThreads == null || numberOfSelectorThreads < 1 ? Runtime
				.getRuntime().availableProcessors() : numberOfSelectorThreads;
		int count = 1;
		while (i > count)
			count <<= 1;
		SelectorThread[] sts = new SelectorThread[count];
		for (i = 0; i < count; ++i) {
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

		m_mask = count - 1;
		m_sts = sts;

		c_logger.info("ChannelAdmin activated");
	}

	protected void deactivate() {
		c_logger.info("Deactivating ChannelAdmin...");

		SelectorThread[] sts = m_sts;
		m_sts = null;
		for (SelectorThread st : sts)
			st.close();

		c_logger.info("ChannelAdmin deactivated");
	}

	IWorker getWorker() {
		return m_worker;
	}

	private SelectorThread getSelectorThread(ISelectableChannel channel) {
		return m_sts[channel.id().intValue() & m_mask];
	}
}
