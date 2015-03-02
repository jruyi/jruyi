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

package org.jruyi.io.tcpserver;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import org.jruyi.common.StrUtil;
import org.jruyi.io.channel.IChannelAdmin;
import org.jruyi.io.common.IVisitor;
import org.jruyi.io.common.StopThread;
import org.jruyi.io.common.SyncPutQueue;
import org.jruyi.io.tcp.TcpChannel;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "jruyi.io.tcpserver.tcpacceptor", //
configurationPolicy = ConfigurationPolicy.IGNORE, //
service = { ITcpAcceptor.class }, //
xmlns = "http://www.osgi.org/xmlns/scr/v1.2.0")
public final class TcpAcceptor implements ITcpAcceptor, Runnable, IVisitor<TcpServer<?, ?>> {

	private static final Logger c_logger = LoggerFactory.getLogger(TcpAcceptor.class);

	private Selector m_selector;
	private Thread m_thread;
	private SyncPutQueue<TcpServer<?, ?>> m_queue;

	private IChannelAdmin m_ca;

	@Override
	public void doAccept(TcpServer<?, ?> server) throws Exception {
		SelectableChannel selectableChannel = server.getSelectableChannel();
		selectableChannel.configureBlocking(false);
		m_queue.put(server);
		m_selector.wakeup();
	}

	@Override
	public void visit(TcpServer<?, ?> server) {
		final SelectableChannel channel = server.getSelectableChannel();
		try {
			channel.register(m_selector, SelectionKey.OP_ACCEPT, server);
		} catch (Throwable t) {
			c_logger.error(StrUtil.join("Failed to register ", channel), t);
			// stop tcp server
			try {
				new StopThread(server).start();
			} catch (Throwable t1) {
				// Ignore
			}
		}
	}

	@Override
	public void run() {
		final SyncPutQueue<TcpServer<?, ?>> queue = m_queue;
		final Selector selector = m_selector;
		final Thread currentThread = Thread.currentThread();
		for (;;) {
			try {
				final int n = selector.select();
				if (currentThread.isInterrupted())
					break;

				// Register
				queue.accept(this);

				if (n < 1)
					continue;

				final Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
				while (iter.hasNext()) {
					final SelectionKey key = iter.next();
					iter.remove();

					if (!key.isValid())
						continue;

					@SuppressWarnings("unchecked")
					final TcpServer<Object, Object> server = (TcpServer<Object, Object>) key.attachment();
					try {
						final SocketChannel socketChannel = ((ServerSocketChannel) key.channel()).accept();
						@SuppressWarnings("resource")
						final TcpChannel tcpChannel = new TcpChannel(server, socketChannel);
						tcpChannel.onAccept();
					} catch (ClosedChannelException e) {
					} catch (Throwable t) {
						c_logger.error(StrUtil.join(server, " failed to accept"), t);
					}
				}
			} catch (ClosedSelectorException e) {
				break;
			} catch (Throwable t) {
				c_logger.error("Unexpected Error", t);
			}
		}
	}

	@Reference(name = "channelAdmin", policy = ReferencePolicy.DYNAMIC)
	synchronized void setChannelAdmin(IChannelAdmin ca) {
		m_ca = ca;
	}

	synchronized void unsetChannelAdmin(IChannelAdmin ca) {
		if (m_ca == ca)
			m_ca = null;
	}

	void activate() throws Exception {
		c_logger.info("Starting TcpAcceptor...");

		m_selector = Selector.open();
		m_queue = new SyncPutQueue<>();
		m_thread = new Thread(this, "jruyi-acceptor");
		m_thread.start();

		c_logger.info("TcpAcceptor started");
	}

	void deactivate() {
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

		c_logger.info("TcpAcceptor stopped");
	}
}
