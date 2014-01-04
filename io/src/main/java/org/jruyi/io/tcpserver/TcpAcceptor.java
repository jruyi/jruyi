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
package org.jruyi.io.tcpserver;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.jruyi.common.StrUtil;
import org.jruyi.io.common.StopThread;
import org.jruyi.io.common.SyncPutQueue;
import org.jruyi.io.tcp.TcpChannel;
import org.jruyi.workshop.IWorkshop;
import org.jruyi.workshop.WorkshopConstants;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service(ITcpAcceptor.class)
@Component(name = "jruyi.io.tcpserver.tcpacceptor", createPid = false)
public final class TcpAcceptor implements ITcpAcceptor, Runnable {

	private static final Logger c_logger = LoggerFactory
			.getLogger(TcpAcceptor.class);
	private String m_name;
	private Selector m_selector;
	private Thread m_thread;
	private SyncPutQueue<TcpServer> m_queue;
	private ComponentContext m_context;

	@Reference(name = "workshop", target = WorkshopConstants.DEFAULT_WORKSHOP_TARGET)
	private IWorkshop m_workshop;

	@Override
	public void doAccept(TcpServer server) throws Exception {
		SelectableChannel selectableChannel = server.getSelectableChannel();
		selectableChannel.configureBlocking(false);
		m_queue.put(server);
		m_selector.wakeup();
	}

	@Override
	public void run() {
		final IWorkshop workshop = m_workshop;
		SyncPutQueue<TcpServer> queue = m_queue;
		Selector selector = m_selector;
		Thread currentThread = Thread.currentThread();
		TcpServer server = null;
		try {
			for (;;) {
				int n = selector.select();
				if (currentThread.isInterrupted())
					break;

				// Register
				while ((server = queue.poll()) != null) {
					SelectableChannel channel = server.getSelectableChannel();
					try {
						channel.register(selector, SelectionKey.OP_ACCEPT,
								server);
					} catch (Exception e) {
						c_logger.error(StrUtil.buildString(
								"Failed to register ", channel), e);
						// stop tcp server
						workshop.run(new StopThread(server));
					}
				}

				if (n < 1)
					continue;

				Iterator<SelectionKey> iter = selector.selectedKeys()
						.iterator();
				while (iter.hasNext()) {
					SelectionKey key = iter.next();
					iter.remove();

					if (!key.isValid())
						continue;

					try {
						server = (TcpServer) key.attachment();
						SocketChannel socketChannel = ((ServerSocketChannel) key
								.channel()).accept();

						@SuppressWarnings("resource")
						TcpChannel channel = new TcpChannel(server,
								socketChannel);
						workshop.run(channel.onAccept());
					} catch (ClosedChannelException e) {
					} catch (Throwable t) {
						c_logger.error(StrUtil.buildString(server,
								" failed to accept"), t);
					}
				}
			}
		} catch (ClosedSelectorException e) {
		} catch (Throwable t) {
			c_logger.error("TcpAcceptor Error", t);

			// disable itself
			m_context.disableComponent(m_name);
		}
	}

	protected void bindWorkshop(IWorkshop workshop) {
		m_workshop = workshop;
	}

	protected void unbindWorkshop(IWorkshop workshop) {
		m_workshop = null;
	}

	protected void activate(ComponentContext context, Map<String, ?> properties)
			throws Exception {
		c_logger.info("Starting TcpAcceptor...");

		m_name = (String) properties.get(ComponentConstants.COMPONENT_NAME);
		m_context = context;

		m_selector = Selector.open();
		m_queue = new SyncPutQueue<TcpServer>();
		m_thread = new Thread(this, "TcpAcceptor");
		m_thread.start();

		c_logger.info("TcpAcceptor started");
	}

	protected void deactivate() {
		c_logger.info("Stopping TcpAcceptor...");

		m_thread.interrupt();
		try {
			m_thread.join();
		} catch (InterruptedException e) {
		}
		m_thread = null;

		m_queue = null;

		try {
			m_selector.close();
		} catch (Throwable t) {
			c_logger.error("Failed to close the selector", t);
		}
		m_selector = null;

		m_context = null;
		m_name = null;

		c_logger.info("TcpAcceptor stopped");
	}
}
