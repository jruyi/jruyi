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
package org.jruyi.io.tcpclient;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.jruyi.common.BiListNode;
import org.jruyi.common.IArgList;
import org.jruyi.common.IService;
import org.jruyi.common.StrUtil;
import org.jruyi.io.IBufferFactory;
import org.jruyi.io.IFilter;
import org.jruyi.io.ISession;
import org.jruyi.io.ISessionListener;
import org.jruyi.io.IoConstants;
import org.jruyi.io.channel.IChannel;
import org.jruyi.io.channel.IChannelAdmin;
import org.jruyi.io.channel.IIoTask;
import org.jruyi.io.filter.IFilterManager;
import org.jruyi.workshop.IRunnable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = IoConstants.CN_TCPCLIENT_CONNPOOL_FACTORY, //
factory = "tcpclient.connpool", //
service = { IService.class }, //
xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class ConnPool extends AbstractTcpClient implements IRunnable, IIoTask {

	private static final Logger c_logger = LoggerFactory.getLogger(ConnPool.class);

	private Configuration m_conf;
	private final BiListNode<IChannel> m_channelQueueHead;
	private int m_channelQueueSize;
	private final ReentrantLock m_channelQueueLock;
	private final AtomicInteger m_poolSize;

	static final class Configuration extends TcpClientConf {

		private Integer m_minPoolSize;
		private Integer m_maxPoolSize;
		private Integer m_idleTimeoutInSeconds;

		@Override
		public void initialize(Map<String, ?> properties) {
			super.initialize(properties);

			minPoolSize((Integer) properties.get("minPoolSize"));
			maxPoolSize((Integer) properties.get("maxPoolSize"));
			idleTimeoutInSeconds((Integer) properties.get("idleTimeoutInSeconds"));
		}

		public Integer minPoolSize() {
			return m_minPoolSize;
		}

		public void minPoolSize(Integer minPoolSize) {
			m_minPoolSize = minPoolSize == null ? 5 : minPoolSize;
		}

		public Integer maxPoolSize() {
			return m_maxPoolSize;
		}

		public void maxPoolSize(Integer maxPoolSize) {
			m_maxPoolSize = maxPoolSize == null ? 10 : maxPoolSize;
		}

		public Integer idleTimeoutInSeconds() {
			return m_idleTimeoutInSeconds;
		}

		public void idleTimeoutInSeconds(Integer idleTimeoutInSeconds) {
			m_idleTimeoutInSeconds = idleTimeoutInSeconds == null ? 60 : idleTimeoutInSeconds;
		}
	}

	public ConnPool() {
		BiListNode<IChannel> node = BiListNode.create();
		node.previous(node);
		node.next(node);
		m_channelQueueHead = node;

		m_poolSize = new AtomicInteger(0);

		m_channelQueueLock = new ReentrantLock();
	}

	@Override
	public void run(Object msg, IFilter<?, ?>[] filters, int filterCount) {
		write(null, msg);
	}

	@Override
	public void write(ISession session/* =null */, Object msg) {
		final Configuration conf = m_conf;
		if (compareAndIncrement(conf.minPoolSize())) {
			connect(msg);
			return;
		}

		// fetch an idle channel in the pool if any
		final IChannel channel = fetchChannel();
		if (channel != null) {
			writeInternal(channel, msg);
			return;
		}

		// No idle channel found
		if (compareAndIncrement(conf.maxPoolSize())) {
			connect(msg);
			return;
		}

		getChannelAdmin().performIoTask(this, msg);
	}

	@Override
	public void onMessageSent(IChannel channel, Object msg) {
		final ISessionListener listener = listener();
		if (listener != null) {
			try {
				listener.onMessageSent(channel, msg);
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(channel, " Unexpected Error: "), t);
			}
		}

		int timeout = m_conf.readTimeoutInSeconds();
		if (timeout < 0)
			return;

		if (timeout > 0)
			scheduleReadTimeout(channel, timeout);
		else
			// readTimeout == 0, means no response is expected
			poolChannel(channel);
	}

	@Override
	public void onMessageReceived(IChannel channel, Object msg) {
		if (!cancelReadTimeout(channel) // channel has timed out
				|| m_conf.readTimeoutInSeconds() == 0 // no response is expected
		) {
			if (msg instanceof Closeable) {
				try {
					((Closeable) msg).close();
				} catch (Throwable t) {
					c_logger.error(StrUtil.join("Failed to close message: ", StrUtil.getLineSeparator(), msg), t);
				}
			}
			return;
		}

		final ISessionListener listener = listener();
		if (listener != null) {
			try {
				listener.onMessageReceived(channel, msg);
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(channel, " Unexpected Error: "), t);
			}
		}

		poolChannel(channel);
	}

	@Override
	public void onChannelOpened(IChannel channel) {
		super.onChannelOpened(channel);
		writeInternal(channel, channel.detach());
	}

	@Override
	public void onChannelClosed(IChannel channel) {
		super.onChannelClosed(channel);
		m_poolSize.decrementAndGet();
	}

	@Override
	public void onChannelException(IChannel channel, Throwable t) {
		channel.close();
		final ISessionListener listener = listener();
		if (listener != null) {
			try {
				listener.onSessionException(channel, t);
			} catch (Throwable e) {
				c_logger.error(StrUtil.join(channel, " Unexpected Error: "), e);
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onChannelIdleTimedOut(IChannel channel) {
		c_logger.debug("{}: IDLE_TIMEOUT", channel);

		BiListNode<IChannel> node = null;
		final ReentrantLock lock = m_channelQueueLock;
		lock.lock();
		try {
			// If null, this node has already been removed.
			// There would be a race condition between fetchChannel and
			// idleTimedOut.
			// Both are going to remove the node. So null-check of the element
			// the node holding is used to tell whether this node has already
			// been removed.
			if ((node = (BiListNode<IChannel>) channel.detach()) == null)
				return;

			removeNode(node);
		} finally {
			lock.unlock();
			channel.close();
		}

		node.close();
	}

	@Override
	public void onChannelConnectTimedOut(IChannel channel) {
		channel.close();
		final ISessionListener listener = listener();
		if (listener != null) {
			try {
				listener.onSessionConnectTimedOut(channel);
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(channel, " Unexpected Error: "), t);
			}
		}
	}

	@Override
	public void onChannelReadTimedOut(IChannel channel) {
		channel.close();
		final ISessionListener listener = listener();
		if (listener != null) {
			try {
				listener.onSessionReadTimedOut(channel);
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(channel, " Unexpected Error: "), t);
			}
		}
	}

	@Override
	public void startInternal() {
		c_logger.info(StrUtil.join("Starting ", this, "..."));

		super.startInternal();

		c_logger.info(StrUtil.join(this, " started"));
	}

	@Override
	public void stopInternal() {
		c_logger.info(StrUtil.join("Stopping ", this, "..."));

		super.stopInternal();

		c_logger.info(StrUtil.join(this, " stopped"));
	}

	@Override
	public void run(IArgList args) {
		final IChannel channel = (IChannel) args.arg(0);
		if (!channel.isClosed())
			writeInternal(channel, args.arg(1));
		else
			write(null, args.arg(1));
	}

	@Reference(name = "buffer", policy = ReferencePolicy.DYNAMIC)
	@Override
	protected synchronized void setBufferFactory(IBufferFactory bf) {
		super.setBufferFactory(bf);
	}

	@Override
	protected synchronized void unsetBufferFactory(IBufferFactory bf) {
		super.unsetBufferFactory(bf);
	}

	@Reference(name = "channelAdmin")
	@Override
	protected void setChannelAdmin(IChannelAdmin cm) {
		super.setChannelAdmin(cm);
	}

	@Override
	protected void unsetChannelAdmin(IChannelAdmin cm) {
		super.unsetChannelAdmin(cm);
	}

	@Reference(name = "filterManager")
	@Override
	protected void setFilterManager(IFilterManager fm) {
		super.setFilterManager(fm);
	}

	@Override
	protected void unsetFilterManager(IFilterManager fm) {
		super.unsetFilterManager(fm);
	}

	@Override
	TcpClientConf configuration() {
		return m_conf;
	}

	@Override
	TcpClientConf updateConf(Map<String, ?> props) {
		Configuration conf = m_conf;
		if (props == null)
			m_conf = null;
		else {
			Configuration newConf = new Configuration();
			newConf.initialize(props);
			m_conf = newConf;
		}

		return conf;
	}

	/**
	 * Increments the pool size if it is less than the given {@code limit}.
	 * 
	 * @return true if pool size is incremented, otherwise false
	 */
	private boolean compareAndIncrement(int limit) {
		final AtomicInteger poolSize = m_poolSize;
		int n;
		while ((n = poolSize.get()) < limit) {
			if (poolSize.compareAndSet(n, n + 1))
				return true;
		}
		return false;
	}

	private IChannel fetchChannel() {
		BiListNode<IChannel> node;
		IChannel channel;
		final BiListNode<IChannel> head = m_channelQueueHead;
		final ReentrantLock lock = m_channelQueueLock;
		do {
			lock.lock();
			try {
				node = head.next();
				if (node == head)
					return null;

				removeNode(node);
				// The attachement of channel is used to tell whether the bound
				// node has been removed. So detach operation has to been
				// synchronized.
				channel = node.get();
				channel.detach();
			} finally {
				lock.unlock();
			}
			node.close();
		} while (!channel.cancelTimeout());

		return channel;
	}

	private void poolChannel(IChannel channel) {
		final Configuration conf = m_conf;
		final int keepAliveTime = conf.idleTimeoutInSeconds();
		final ReentrantLock lock = m_channelQueueLock;
		lock.lock();
		try {
			if (m_channelQueueSize < conf.minPoolSize() || keepAliveTime < 0) {
				putNode(newNode(channel));
				return;
			}

			if (keepAliveTime > 0) {
				putNode(newNode(channel));
				channel.scheduleIdleTimeout(keepAliveTime);
				return;
			}
		} finally {
			lock.unlock();
		}

		// keepAliveTime == 0, the channel need be closed immediately
		channel.close();
	}

	private BiListNode<IChannel> newNode(IChannel channel) {
		final BiListNode<IChannel> node = BiListNode.create();
		node.set(channel);
		channel.attach(node);
		return node;
	}

	private void putNode(BiListNode<IChannel> newNode) {
		final BiListNode<IChannel> head = m_channelQueueHead;
		final BiListNode<IChannel> headNext = head.next();
		newNode.previous(head);
		newNode.next(headNext);
		head.next(newNode);
		headNext.previous(newNode);

		++m_channelQueueSize;
	}

	private void removeNode(BiListNode<IChannel> node) {
		final BiListNode<IChannel> previousNode = node.previous();
		final BiListNode<IChannel> nextNode = node.next();
		previousNode.next(nextNode);
		nextNode.previous(previousNode);

		--m_channelQueueSize;
	}
}
