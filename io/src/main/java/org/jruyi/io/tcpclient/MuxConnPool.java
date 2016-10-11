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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jruyi.common.IIdentifiable;
import org.jruyi.common.IService;
import org.jruyi.common.StrUtil;
import org.jruyi.io.IBufferFactory;
import org.jruyi.io.ISessionListener;
import org.jruyi.io.IoConstants;
import org.jruyi.io.channel.IChannel;
import org.jruyi.io.channel.IChannelAdmin;
import org.jruyi.io.channel.IChannelService;
import org.jruyi.io.channel.ITimerListener;
import org.jruyi.io.channel.Timer;
import org.jruyi.io.filter.IFilterManager;
import org.jruyi.io.tcp.TcpChannel;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = IoConstants.CN_TCPCLIENT_MUX_CONNPOOL_FACTORY, //
		factory = "tcpclient.mux.connpool", //
		service = { IService.class }, //
		xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class MuxConnPool<I extends IIdentifiable<?>, O extends IIdentifiable<?>> extends ConnPool<I, O> {

	private static final Logger c_logger = LoggerFactory.getLogger(MuxConnPool.class);

	private ConcurrentHashMap<Object, Timer> m_timers;

	static final class PooledMuxChannel<O extends IIdentifiable<?>> extends PooledChannel implements ITimerListener {

		PooledMuxChannel(IChannelService<Object, Object> cs, int selectorId) {
			super(cs, selectorId);
		}

		@Override
		public void onTimeout(Object subject) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			final MuxConnPool<?, O> connPool = (MuxConnPool) channelService();
			@SuppressWarnings("unchecked")
			final O outMsg = (O) subject;
			connPool.timers().remove(outMsg.id());
			final ISessionListener<?, O> listener = connPool.listener();
			if (listener != null) {
				try {
					listener.onSessionReadTimedOut(this, outMsg);
				} catch (Throwable t) {
					connPool.onChannelException(this, t);
				}
			}
		}
	}

	@Override
	public void beforeSendMessage(IChannel channel, O outMsg) {
		final Object msgId;
		final int timeout = configuration().readTimeoutInSeconds();
		if (timeout > 0 && (msgId = outMsg.id()) != null) {
			final Timer timer = channel.selector().createTimer(outMsg);
			if (m_timers.put(msgId, timer) != null) {
				c_logger.error("Collision of message ID: {}", msgId);
				channel.close();
				return;
			}
			@SuppressWarnings("unchecked")
			final PooledMuxChannel<O> pooledChannel = (PooledMuxChannel<O>) channel;
			timer.listener(pooledChannel);
			timer.schedule(timeout);
		}
	}

	@Override
	public void onMessageSent(IChannel channel, O outMsg) {
		final ISessionListener<I, O> listener = listener();
		if (listener != null)
			listener.onMessageSent(channel, outMsg);

		poolChannel((PooledChannel) channel);
	}

	@Override
	public void onMessageReceived(IChannel channel, I inMsg) {
		final Timer timer = m_timers.remove(inMsg.id());
		if (timer == null || !timer.cancel()) {
			if (inMsg instanceof AutoCloseable) {
				try {
					((AutoCloseable) inMsg).close();
				} catch (Throwable t) {
					c_logger.error(StrUtil.join("Failed to close message: ", StrUtil.getLineSeparator(), inMsg), t);
				}
			}
			return;
		}

		final ISessionListener<I, O> listener = listener();
		if (listener != null)
			listener.onMessageReceived(channel, inMsg);
	}

	@Override
	public void onChannelReadTimedOut(IChannel channel) {
		throw new UnsupportedOperationException();
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
	public void activate(Map<String, ?> properties) throws Exception {
		super.activate(properties);
		final int n = configuration().initialCapacityOfChannelMap();
		int initialCapacity = n << 5;
		if (initialCapacity < 0)
			initialCapacity = n;
		m_timers = new ConcurrentHashMap<>(initialCapacity);
	}

	@Override
	public void deactivate() {
		super.deactivate();
		final Collection<Timer> timers = m_timers.values();
		m_timers = null;
		for (Timer timer : timers)
			timer.cancel();
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	TcpChannel newChannel(int selectorId) {
		return new PooledMuxChannel<O>((IChannelService) this, selectorId);
	}

	ConcurrentHashMap<Object, Timer> timers() {
		return m_timers;
	}
}
