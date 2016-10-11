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

import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jruyi.common.IService;
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
import org.jruyi.io.channel.ISelector;
import org.jruyi.io.channel.IoEvent;
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

	private final ThreadLocal<ArrayDeque<IChannel>> m_channels;

	private ConnPoolConf m_conf;
	private final AtomicInteger m_poolSize;
	private final AtomicInteger m_queueSize;

	static class PooledChannel extends TcpChannel {

		private Object m_request;

		PooledChannel(IChannelService<Object, Object> cs, int selectorId) {
			super(cs, selectorId);
		}

		public void attachRequest(Object request) {
			m_request = request;
		}

		public Object detachRequest() {
			final Object request = m_request;
			m_request = null;
			return request;
		}
	}

	public ConnPool() {
		m_channels = new ThreadLocal<ArrayDeque<IChannel>>() {
			@Override
			protected ArrayDeque<IChannel> initialValue() {
				return new ArrayDeque<>();
			}
		};

		m_poolSize = new AtomicInteger(0);
		m_queueSize = new AtomicInteger(0);
	}

	@Override
	public void run(Object msg, IFilter<?, ?>[] ignore, int selectorId) {
		@SuppressWarnings("unchecked")
		final O out = (O) msg;
		final ConnPoolConf conf = m_conf;
		if (compareAndIncrement(conf.corePoolSize())) {
			connect(msg, selectorId);
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
			connect(msg, selectorId);
			return;
		}
		write(null, out);
	}

	@Override
	public void write(ISession session/* =null */, O msg) {
		final ISelector selector = getChannelAdmin().designateSelector(msg);
		selector.write(new IoEvent(this, msg, null, selector.id()));
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

		final ArrayDeque<IChannel> channels = m_channels.get();
		if (channels.removeFirstOccurrence(channel))
			m_queueSize.decrementAndGet();
		channel.close();
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
		final AtomicInteger queueSize = m_queueSize;
		final ArrayDeque<IChannel> channels = m_channels.get();
		if (keepAliveTime < 0) {
			queueSize.incrementAndGet();
			channels.addLast(channel);
			return;
		}

		if (!conf.allowsCoreConnectionTimeout()) {
			int n;
			while ((n = queueSize.get()) < conf.corePoolSize()) {
				if (queueSize.compareAndSet(n, n + 1)) {
					channels.addLast(channel);
					return;
				}
			}
		}

		if (keepAliveTime > 0) {
			channels.addLast(channel);
			queueSize.incrementAndGet();
			channel.scheduleIdleTimeout(keepAliveTime);
			return;
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
	TcpChannel newChannel(int selectorId) {
		return new PooledChannel((IChannelService<Object, Object>) this, selectorId);
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
		final ArrayDeque<IChannel> channels = m_channels.get();
		final IChannel channel = channels.pollFirst();
		if (channel != null) {
			m_queueSize.decrementAndGet();
			channel.cancelTimeout();
		}
		return channel;
	}
}