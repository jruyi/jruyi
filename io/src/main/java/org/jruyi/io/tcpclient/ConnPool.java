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
package org.jruyi.io.tcpclient;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.jruyi.common.ArgList;
import org.jruyi.common.BiListNode;
import org.jruyi.common.IArgList;
import org.jruyi.common.IService;
import org.jruyi.common.StrUtil;
import org.jruyi.io.IBufferFactory;
import org.jruyi.io.ISession;
import org.jruyi.io.ISessionListener;
import org.jruyi.io.IoConstants;
import org.jruyi.io.channel.IChannel;
import org.jruyi.io.channel.IChannelAdmin;
import org.jruyi.io.common.SyncQueue;
import org.jruyi.io.filter.IFilterManager;
import org.jruyi.workshop.IRunnable;
import org.jruyi.workshop.IWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service(IService.class)
@Component(name = IoConstants.CN_TCPCLIENT_CONNPOOL_FACTORY, factory = "tcpclient.connpool", createPid = false, specVersion = "1.1.0")
@References({
		@Reference(name = "channelAdmin", referenceInterface = IChannelAdmin.class),
		@Reference(name = "filterManager", referenceInterface = IFilterManager.class),
		@Reference(name = "buffer", referenceInterface = IBufferFactory.class, policy = ReferencePolicy.DYNAMIC, bind = "bindBufferFactory", unbind = "unbindBufferFactory") })
public final class ConnPool extends AbstractTcpClient implements IRunnable {

	private static final Logger c_logger = LoggerFactory
			.getLogger(ConnPool.class);
	private Configuration m_conf;

	@Reference(name = "worker")
	private IWorker m_worker;

	private final SyncQueue<Object> m_msgs;
	private final ReentrantLock m_channelQueueLock;
	private final BiListNode<IChannel> m_channelQueueHead;
	private int m_channelQueueSize;
	private volatile int m_poolSize;
	private final ReentrantLock m_poolSizeLock;

	static final class Configuration extends TcpClientConf {

		private Integer m_minPoolSize;
		private Integer m_maxPoolSize;
		private Integer m_idleTimeout;

		@Override
		public void initialize(Map<String, ?> properties) {
			super.initialize(properties);

			minPoolSize((Integer) properties.get("minPoolSize"));
			maxPoolSize((Integer) properties.get("maxPoolSize"));
			idleTimeout((Integer) properties.get("idleTimeout"));
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

		public Integer idleTimeout() {
			return m_idleTimeout;
		}

		public void idleTimeout(Integer idleTimeout) {
			m_idleTimeout = idleTimeout == null ? 60 : idleTimeout;
		}
	}

	public ConnPool() {
		BiListNode<IChannel> node = BiListNode.create();
		node.previous(node);
		node.next(node);
		m_channelQueueHead = node;

		m_msgs = new SyncQueue<Object>();
		m_channelQueueLock = new ReentrantLock();
		m_poolSizeLock = new ReentrantLock();
	}

	@Override
	public void write(ISession session, Object msg) {
		Configuration conf = m_conf;
		if (compareAndIncrement(conf.minPoolSize())) {
			connect(msg);
			return;
		}

		// fetch an idle channel in the pool if any
		IChannel channel = fetchChannel();
		if (channel != null) {
			channel.write(msg);
			return;
		}

		// No idle channel found
		if (compareAndIncrement(conf.maxPoolSize())) {
			connect(msg);
			return;
		}

		// Enqueue the message for being processed on any channel available.
		// The message has to be enqueued before polling the channel pool again.
		// Otherwise, if a free channel is put into the pool after polling out
		// a null channel but before the message is enqueued, then the message
		// could be left in the queue with never being processed.
		m_msgs.put(msg);
		channel = fetchChannel();
		if (channel != null) {
			msg = m_msgs.poll();
			if (msg != null)
				channel.write(msg);
			else
				poolChannel(channel);
		}
	}

	@Override
	public void onMessageSent(IChannel channel, Object msg) {
		final ISessionListener listener = listener();
		if (listener != null) {
			try {
				listener.onMessageSent(channel, msg);
			} catch (Throwable t) {
				c_logger.error(
						StrUtil.buildString(channel, " Unexpected Error: "), t);
			}
		}

		int timeout = m_conf.readTimeout();
		if (timeout < 0)
			return;

		if (timeout > 0)
			channel.scheduleReadTimeout(timeout);
		else {
			// readTimeout == 0, means no response is expected
			msg = poolChannelIfNoMsg(channel);
			if (msg != null)
				m_worker.run(this, ArgList.create(channel, msg));
		}
	}

	@Override
	public void onMessageReceived(IChannel channel, Object msg) {
		if (!channel.cancelTimeout() // channel has timed out
				|| m_conf.readTimeout() == 0 // no response is expected
		) {
			if (msg instanceof Closeable) {
				try {
					((Closeable) msg).close();
				} catch (Throwable t) {
					c_logger.error(StrUtil.buildString(
							"Failed to close message: ",
							StrUtil.getLineSeparator(), msg), t);
				}
			}
			return;
		}

		final ISessionListener listener = listener();
		if (listener != null) {
			try {
				listener.onMessageReceived(channel, msg);
			} catch (Throwable t) {
				c_logger.error(
						StrUtil.buildString(channel, " Unexpected Error: "), t);
			}
		}

		msg = poolChannelIfNoMsg(channel);
		if (msg != null)
			channel.write(msg);
	}

	@Override
	public void onChannelOpened(IChannel channel) {
		super.onChannelOpened(channel);
		channel.write(channel.detach());
	}

	@Override
	public void onChannelClosed(IChannel channel) {
		super.onChannelClosed(channel);

		final ReentrantLock poolSizeLock = m_poolSizeLock;
		poolSizeLock.lock();
		try {
			--m_poolSize;
		} finally {
			poolSizeLock.unlock();
		}

		// If all the channels are closing and there are still some messages
		// left in queue with no new messages coming, those messages will never
		// be processed. So when the channel pool size is below minPoolSize and
		// the message queue is not empty, then make a new connection to process
		// the message.
		int minPoolSize = m_conf.minPoolSize();
		Object msg = null;
		SyncQueue<Object> messages = m_msgs;
		poolSizeLock.lock();
		try {
			if (m_poolSize < minPoolSize && (msg = messages.poll()) != null)
				++m_poolSize;
		} finally {
			poolSizeLock.unlock();
		}

		if (msg != null)
			connect(msg);
	}

	@Override
	public void onChannelException(IChannel channel, Throwable t) {
		channel.close();
		final ISessionListener listener = listener();
		if (listener != null) {
			try {
				listener.onSessionException(channel, t);
			} catch (Throwable e) {
				c_logger.error(
						StrUtil.buildString(channel, " Unexpected Error: "), e);
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
				c_logger.error(
						StrUtil.buildString(channel, " Unexpected Error: "), t);
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
				c_logger.error(
						StrUtil.buildString(channel, " Unexpected Error: "), t);
			}
		}
	}

	@Override
	public void startInternal() {
		c_logger.info(StrUtil.buildString("Starting ", this, "..."));

		super.startInternal();

		c_logger.info(StrUtil.buildString(this, " started"));
	}

	@Override
	public void stopInternal() {
		c_logger.info(StrUtil.buildString("Stopping ", this, "..."));

		super.stopInternal();

		c_logger.info(StrUtil.buildString(this, " stopped"));
	}

	@Override
	public void run(IArgList args) {
		IChannel channel = (IChannel) args.arg(0);
		channel.write(args.arg(1));
	}

	protected void bindWorker(IWorker worker) {
		m_worker = worker;
	}

	protected void unbindWorker(IWorker worker) {
		if (m_worker == worker)
			m_worker = null;
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
		if (m_poolSize < limit) {
			final ReentrantLock poolSizeLock = m_poolSizeLock;
			poolSizeLock.lock();
			try {
				if (m_poolSize < limit) {
					++m_poolSize;
					return true;
				}
			} finally {
				poolSizeLock.unlock();
			}
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
		Configuration conf = m_conf;
		final int keepAliveTime = conf.idleTimeout();
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

	private Object poolChannelIfNoMsg(IChannel channel) {
		Object msg = m_msgs.poll();
		if (msg != null)
			return msg;

		final Configuration conf = m_conf;
		final int keepAliveTime = conf.idleTimeout();
		final ReentrantLock lock = m_channelQueueLock;
		lock.lock();
		try {
			if ((msg = m_msgs.poll()) != null)
				return msg;

			if (m_channelQueueSize < conf.minPoolSize() || keepAliveTime < 0) {
				putNode(newNode(channel));
				return null;
			}

			if (keepAliveTime > 0) {
				putNode(newNode(channel));
				channel.scheduleIdleTimeout(keepAliveTime);
				return null;
			}
		} finally {
			lock.unlock();
		}

		// keepAliveTime == 0, the channel need be closed immediately
		channel.close();
		return null;
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
