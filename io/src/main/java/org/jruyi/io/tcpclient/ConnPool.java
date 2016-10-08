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

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.jruyi.common.BiListNode;
import org.jruyi.common.IService;
import org.jruyi.common.ITimerAdmin;
import org.jruyi.common.StrUtil;
import org.jruyi.io.IBufferFactory;
import org.jruyi.io.IFilter;
import org.jruyi.io.ISession;
import org.jruyi.io.ISessionListener;
import org.jruyi.io.IoConstants;
import org.jruyi.io.channel.IChannel;
import org.jruyi.io.channel.IChannelAdmin;
import org.jruyi.io.channel.IChannelService;
import org.jruyi.io.channel.IIoTask;
import org.jruyi.io.common.Util;
import org.jruyi.io.filter.IFilterManager;
import org.jruyi.io.tcp.TcpChannel;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = IoConstants.CN_TCPCLIENT_CONNPOOL_FACTORY, //
		factory = "tcpclient.connpool", //
		service = { IService.class }, //
		xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public class ConnPool<I, O> extends AbstractTcpClient<I, O> implements IIoTask {

	private static final Logger c_logger = LoggerFactory.getLogger(ConnPool.class);

	private ConnPoolConf m_conf;
	private final BiListNode<IChannel> m_channelQueueHead;
	private int m_channelQueueSize;
	private final ReentrantLock m_channelQueueLock;
	private final AtomicInteger m_poolSize;

	static class PooledChannel extends TcpChannel {

		private BiListNode<IChannel> m_node;
		private Object m_request;

		PooledChannel(IChannelService<Object, Object> cs) {
			super(cs);
		}

		public void attachRequest(Object request) {
			m_request = request;
		}

		public Object detachRequest() {
			final Object request = m_request;
			m_request = null;
			return request;
		}

		public void node(BiListNode<IChannel> node) {
			m_node = node;
		}

		public BiListNode<IChannel> node() {
			return m_node;
		}
	}

	public ConnPool() {
		final BiListNode<IChannel> node = BiListNode.create();
		node.previous(node);
		node.next(node);
		m_channelQueueHead = node;

		m_channelQueueLock = new ReentrantLock();
		m_poolSize = new AtomicInteger(0);
	}

	@Override
	public void run(Object msg, IFilter<?, ?>[] filters, int filterCount) {
		@SuppressWarnings("unchecked")
		final O out = (O) msg;
		write(null, out);
	}

	@Override
	public void write(ISession session/* =null */, O msg) {
		final ConnPoolConf conf = m_conf;
		if (compareAndIncrement(conf.corePoolSize())) {
			connect(msg);
			return;
		}

		// fetch an idle channel in the pool if any
		final IChannel channel = fetchChannel();
		if (channel != null) {
			channel.write(msg);
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
	public void beforeSendMessage(IChannel channel, O outMsg) {
		final int timeout = m_conf.readTimeoutInSeconds();
		if (timeout > 0)
			scheduleReadTimeout(channel, timeout);
		else if (timeout == 0) // means no response is expected
			poolChannel((PooledChannel) channel);
	}

	@Override
	public void onMessageSent(IChannel channel, O outMsg) {
		((PooledChannel) channel).attachRequest(outMsg);
		final ISessionListener<I, O> listener = listener();
		if (listener != null)
			listener.onMessageSent(channel, outMsg);
	}

	@Override
	public void onMessageReceived(IChannel channel, I inMsg) {
		if (!cancelReadTimeout(channel) // channel has timed out
				|| m_conf.readTimeoutInSeconds() == 0 // no response is expected
		) {
			if (inMsg instanceof AutoCloseable) {
				try {
					((AutoCloseable) inMsg).close();
				} catch (Throwable t) {
					c_logger.error(
							StrUtil.join(channel, " failed to close message: ", StrUtil.getLineSeparator(), inMsg), t);
				}
			}
			return;
		}

		final PooledChannel pooledChannel = (PooledChannel) channel;
		pooledChannel.attachRequest(null);
		final ISessionListener<I, O> listener = listener();
		if (listener != null)
			listener.onMessageReceived(channel, inMsg);

		poolChannel(pooledChannel);
	}

	@Override
	public void onChannelOpened(IChannel channel) {
		super.onChannelOpened(channel);
		channel.write(channel.detach());
	}

	@Override
	public void onChannelClosed(IChannel channel) {
		super.onChannelClosed(channel);
		m_poolSize.decrementAndGet();
	}

	@Override
	public void onChannelException(IChannel channel, Throwable throwable) {
		try {
			final ISessionListener<I, O> listener = listener();
			if (listener != null)
				listener.onSessionException(channel, throwable);
		} catch (Throwable t) {
			c_logger.error(StrUtil.join(channel, ", unexpected Error: "), t);
		} finally {
			channel.close();
		}
	}

	@Override
	public void onChannelIdleTimedOut(IChannel channel) {
		c_logger.debug("{}: IDLE_TIMEOUT", channel);

		final PooledChannel pooledChannel = (PooledChannel) channel;
		final BiListNode<IChannel> node;
		final ReentrantLock lock = m_channelQueueLock;
		lock.lock();
		try {
			// If null, this node has already been removed.
			// There would be a race condition between fetchChannel and
			// idleTimedOut.
			// Both are going to remove the node. So null-check of the element
			// the node holding is used to tell whether this node has already
			// been removed.
			if ((node = pooledChannel.node()) == null)
				return;

			pooledChannel.node(null);
			removeNode(node);
		} finally {
			lock.unlock();
		}
		channel.close();
		node.close();
	}

	@Override
	public void onChannelConnectTimedOut(IChannel channel) {
		final ISessionListener<I, O> listener = listener();
		if (listener != null)
			listener.onSessionConnectTimedOut(channel);
		channel.close();
	}

	@Override
	public void onChannelReadTimedOut(IChannel channel) {
		@SuppressWarnings("unchecked")
		final O outMsg = (O) ((PooledChannel) channel).detachRequest();
		final ISessionListener<I, O> listener = listener();
		if (listener != null)
			listener.onSessionReadTimedOut(channel, outMsg);
		channel.close();
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

	@Reference(name = "buffer", policy = ReferencePolicy.DYNAMIC)
	@Override
	public void setBufferFactory(IBufferFactory bf) {
		super.setBufferFactory(bf);
	}

	@Reference(name = "channelAdmin")
	@Override
	public void setChannelAdmin(IChannelAdmin cm) {
		super.setChannelAdmin(cm);
	}

	@Reference(name = "filterManager")
	@Override
	public void setFilterManager(IFilterManager fm) {
		super.setFilterManager(fm);
	}

	@Reference(name = "timerAdmin", policy = ReferencePolicy.DYNAMIC)
	@Override
	public void setTimerAdmin(ITimerAdmin ta) {
		super.setTimerAdmin(ta);
	}

	@Override
	ConnPoolConf configuration() {
		return m_conf;
	}

	@Override
	void configuration(TcpClientConf conf) {
		m_conf = (ConnPoolConf) conf;
	}

	@Override
	TcpClientConf createConf(Map<String, ?> props) {
		final ConnPoolConf conf = new ConnPoolConf();
		conf.initialize(props);
		return conf;
	}

	void poolChannel(PooledChannel channel) {
		final ConnPoolConf conf = m_conf;
		final int keepAliveTime = conf.idleTimeoutInSeconds();
		final ReentrantLock lock = m_channelQueueLock;
		lock.lock();
		try {
			if ((m_channelQueueSize < conf.corePoolSize() && !conf.allowsCoreConnectionTimeout())
					|| keepAliveTime < 0) {
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

	@Override
	int timeout(TcpClientConf conf) {
		return Util.max(super.timeout(conf), ((ConnPoolConf) conf).idleTimeoutInSeconds());
	}

	@Override
	@SuppressWarnings("unchecked")
	TcpChannel newChannel() {
		return new PooledChannel((IChannelService<Object, Object>) this);
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
				// The attachment of the channel is used to tell whether the
				// bound node has been removed. So the detach operation has to
				// be synchronized.
				channel = node.get();
				((PooledChannel) channel).node(null);
			} finally {
				lock.unlock();
			}
			node.close();
		} while (channel.isClosed() || !channel.cancelTimeout());

		return channel;
	}

	private BiListNode<IChannel> newNode(PooledChannel channel) {
		final BiListNode<IChannel> node = BiListNode.create();
		node.set(channel);
		channel.node(node);
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