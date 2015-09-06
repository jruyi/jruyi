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
import org.jruyi.common.ITimeoutEvent;
import org.jruyi.common.ITimeoutListener;
import org.jruyi.common.ITimeoutNotifier;
import org.jruyi.common.ITimerAdmin;
import org.jruyi.common.StrUtil;
import org.jruyi.io.IBufferFactory;
import org.jruyi.io.ISessionListener;
import org.jruyi.io.IoConstants;
import org.jruyi.io.channel.IChannel;
import org.jruyi.io.channel.IChannelAdmin;
import org.jruyi.io.filter.IFilterManager;
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

	private static final Object OUTMSG_LISTENER = new Object();

	private ConcurrentHashMap<Object, ITimeoutNotifier<O>> m_notifiers;

	final class MsgTimeoutListener implements ITimeoutListener<O> {

		private final IChannel m_channel;

		MsgTimeoutListener(IChannel channel) {
			m_channel = channel;
		}

		@Override
		public void onTimeout(ITimeoutEvent<O> event) {
			final O outMsg = event.subject();
			notifiers().remove(outMsg.id());
			final ISessionListener<I, O> listener = listener();
			if (listener != null) {
				try {
					listener.onSessionReadTimedOut(m_channel, outMsg);
				} catch (Throwable t) {
					onChannelException(m_channel, t);
				}
			}
		}
	}

	@Override
	public void beforeSendMessage(IChannel channel, O outMsg) {
		final int timeout = configuration().readTimeoutInSeconds();
		if (timeout > 0) {
			final ITimeoutNotifier<O> tn = createTimeoutNotifier(outMsg);
			if (m_notifiers.put(outMsg.id(), tn) != null) {
				c_logger.error("Collision of message ID: {}", outMsg.id());
				channel.close();
				return;
			}
			tn.listener(getListener(channel));
			tn.schedule(timeout);
		}
	}

	@Override
	public void onMessageSent(IChannel channel, O outMsg) {
		final ISessionListener<I, O> listener = listener();
		if (listener != null)
			listener.onMessageSent(channel, outMsg);

		poolChannel(channel);
	}

	@Override
	public void onMessageReceived(IChannel channel, I inMsg) {
		final ITimeoutNotifier<O> tn = m_notifiers.remove(inMsg.id());
		if (tn == null || !tn.cancel()) {
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

	@Reference(name = "timerAdmin", policy = ReferencePolicy.DYNAMIC)
	@Override
	public void setTimerAdmin(ITimerAdmin ta) {
		super.setTimerAdmin(ta);
	}

	@Override
	public void activate(Map<String, ?> properties) throws Exception {
		super.activate(properties);
		final int n = configuration().initialCapacityOfChannelMap();
		int initialCapacity = n << 5;
		if (initialCapacity < 0)
			initialCapacity = n;
		m_notifiers = new ConcurrentHashMap<>(initialCapacity);
	}

	@Override
	public void deactivate() {
		super.deactivate();
		final Collection<ITimeoutNotifier<O>> notifiers = m_notifiers.values();
		m_notifiers = null;
		for (ITimeoutNotifier<?> notifier : notifiers)
			notifier.close();
	}

	ConcurrentHashMap<Object, ITimeoutNotifier<O>> notifiers() {
		return m_notifiers;
	}

	private ITimeoutListener<O> getListener(IChannel channel) {
		@SuppressWarnings("unchecked")
		ITimeoutListener<O> listener = (ITimeoutListener<O>) channel.inquiry(OUTMSG_LISTENER);
		if (listener == null) {
			listener = new MsgTimeoutListener(channel);
			channel.deposit(OUTMSG_LISTENER, listener);
		}
		return listener;
	}
}
