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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jruyi.common.ITimeoutNotifier;
import org.jruyi.common.ITimer;
import org.jruyi.common.ITimerAdmin;
import org.jruyi.common.Service;
import org.jruyi.common.StrUtil;
import org.jruyi.io.IBufferFactory;
import org.jruyi.io.ISession;
import org.jruyi.io.ISessionListener;
import org.jruyi.io.ISessionService;
import org.jruyi.io.channel.IChannel;
import org.jruyi.io.channel.IChannelAdmin;
import org.jruyi.io.channel.IChannelService;
import org.jruyi.io.common.Util;
import org.jruyi.io.filter.IFilterList;
import org.jruyi.io.filter.IFilterManager;
import org.jruyi.io.tcp.TcpChannel;
import org.jruyi.io.tcp.TcpChannelConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTcpClient<I, O> extends Service implements IChannelService<I, O>, ISessionService<I, O> {

	private static final Logger c_logger = LoggerFactory.getLogger(AbstractTcpClient.class);

	private final AtomicLong m_sequence = new AtomicLong(0L);

	private IChannelAdmin m_ca;
	private IFilterManager m_fm;
	private IBufferFactory m_bf;
	private ITimerAdmin m_ta;

	private ITimer m_timer;

	private String m_caption;
	private IFilterList m_filters;
	private volatile boolean m_stopped = true;
	private ISessionListener<I, O> m_listener;
	private ConcurrentHashMap<Long, IChannel> m_channels;

	static final class TcpClientChannel extends TcpChannel {

		private Object m_request;

		TcpClientChannel(IChannelService<Object, Object> cs) {
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
	}

	@Override
	public long generateId() {
		return m_sequence.incrementAndGet();
	}

	@Override
	public final Object getConfiguration() {
		return configuration();
	}

	@Override
	public IChannelAdmin getChannelAdmin() {
		return m_ca;
	}

	@Override
	public final IBufferFactory getBufferFactory() {
		return m_bf;
	}

	@Override
	public void setSessionListener(ISessionListener<I, O> listener) {
		m_listener = listener;
	}

	@Override
	public <S> ITimeoutNotifier<S> createTimeoutNotifier(S subject) {
		return m_timer.createNotifier(subject);
	}

	@Override
	public void openSession() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void write(ISession session, O msg) {
		final IChannel channel = m_channels.get(session.id());
		if (channel != null) {
			channel.write(msg);
			return;
		}

		c_logger.warn(StrUtil.join(session, " failed to send(channel closed): ", msg));

		if (msg instanceof AutoCloseable) {
			try {
				((AutoCloseable) msg).close();
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(session, " failed to close message: ", msg), t);
			}
		}
	}

	@Override
	public long throttle() {
		return configuration().throttle();
	}

	@Override
	public final IFilterList getFilterChain() {
		return m_filters;
	}

	@Override
	public void onChannelClosed(IChannel channel) {
		c_logger.debug("{}: CLOSED", channel);

		final ConcurrentHashMap<Long, IChannel> channels = m_channels;
		if (channels != null)
			channels.remove(channel.id());
	}

	@Override
	public void closeSession(ISession session) {
		((IChannel) session).close();
	}

	@Override
	public void onChannelException(IChannel channel, Throwable t) {
		try {
			final Object attachment = channel.detach();
			if (attachment != null) {
				c_logger.error(StrUtil.join(channel, " got an error: ", attachment), t);

				if (attachment instanceof AutoCloseable) {
					try {
						((AutoCloseable) attachment).close();
					} catch (Throwable e) {
						c_logger.error(StrUtil.join(channel, "Failed to close: ", attachment), e);
					}
				}
			} else
				c_logger.error(StrUtil.join(channel, " got an error"), t);
		} finally {
			channel.close();
		}
	}

	@Override
	public void onChannelOpened(IChannel channel) {
		c_logger.debug("{}: OPENED", channel);

		final Long id = channel.id();
		final ConcurrentHashMap<Long, IChannel> channels = m_channels;
		if (channels != null)
			channels.put(id, channel);

		if (m_stopped) {
			channel.close();
			return;
		}
	}

	@Override
	public void onChannelIdleTimedOut(IChannel channel) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final String toString() {
		return m_caption;
	}

	@Override
	protected final boolean updateInternal(Map<String, ?> properties) throws Exception {
		final TcpClientConf newConf = createConf(properties);
		updateFilters(newConf);
		final TcpClientConf oldConf = configuration();
		boolean changed = oldConf.isMandatoryChanged(newConf);
		if (!changed) {
			final int timeout = timeout(newConf);
			m_timer.configuration().wheelSize(Util.max(timeout, 0)).apply();
			if (timeout == 0)
				closeChannels();
		}
		configuration(newConf);
		return changed;
	}

	@Override
	protected void startInternal() {
		m_stopped = false;
		m_timer.start();
	}

	@Override
	protected void stopInternal() {
		m_stopped = true;
		closeChannels();
		m_timer.stop();
	}

	public void setChannelAdmin(IChannelAdmin cm) {
		m_ca = cm;
	}

	public void unsetChannelAdmin(IChannelAdmin cm) {
		m_ca = null;
	}

	public void setFilterManager(IFilterManager fm) {
		m_fm = fm;
	}

	public void unsetFilterManager(IFilterManager fm) {
		m_fm = null;
	}

	public synchronized void setBufferFactory(IBufferFactory bf) {
		m_bf = bf;
	}

	public synchronized void unsetBufferFactory(IBufferFactory bf) {
		if (m_bf == bf)
			m_bf = bf;
	}

	public synchronized void setTimerAdmin(ITimerAdmin ta) {
		m_ta = ta;
	}

	public synchronized void unsetTimerAdmin(ITimerAdmin ta) {
		if (m_ta == ta)
			m_ta = null;
	}

	public void activate(Map<String, ?> properties) throws Exception {
		final TcpClientConf conf = createConf(properties);
		updateFilters(conf);
		configuration(conf);

		m_caption = Util.genServiceId(properties, conf.ip(), conf.port(), "TcpClient");
		m_channels = new ConcurrentHashMap<>(conf.initialCapacityOfChannelMap());
		m_timer = m_ta.createTimer(Util.max(timeout(conf), 0));
	}

	public void deactivate() {
		stop();

		m_timer = null;

		updateFilters(null);
	}

	abstract TcpClientConf createConf(Map<String, ?> props);

	abstract TcpClientConf configuration();

	abstract void configuration(TcpClientConf conf);

	int timeout(TcpClientConf conf) {
		return Util.max(conf.connectTimeoutInSeconds(), conf.readTimeoutInSeconds());
	}

	Method[] getMandatoryPropsAccessors() {
		return TcpClientConf.getMandatoryPropsAccessors();
	}

	final ISessionListener<I, O> listener() {
		return m_listener;
	}

	final void connect() {
		final TcpChannel channel = newChannel();
		channel.connect(configuration().connectTimeoutInSeconds());
	}

	final void connect(Object attachment) {
		final TcpChannel channel = newChannel();
		channel.attach(attachment);
		channel.connect(configuration().connectTimeoutInSeconds());
	}

	final boolean cancelReadTimeout(IChannel channel) {
		return channel.cancelTimeout();
	}

	final void scheduleReadTimeout(IChannel channel, int timeout) {
		channel.scheduleReadTimeout(timeout);
	}

	@SuppressWarnings({ "unchecked" })
	TcpChannel newChannel() {
		return new TcpClientChannel((IChannelService<Object, Object>) this);
	}

	private void updateFilters(TcpChannelConf newConf) {
		final String[] newNames = newConf == null ? StrUtil.getEmptyStringArray() : newConf.filters();
		String[] oldNames = StrUtil.getEmptyStringArray();
		final IFilterManager fm = m_fm;
		final TcpChannelConf oldConf = configuration();
		if (oldConf == null)
			m_filters = fm.getFilters(oldNames);
		else
			oldNames = oldConf.filters();

		if (Arrays.equals(newNames, oldNames))
			return;

		m_filters = fm.getFilters(newNames);
		fm.ungetFilters(oldNames);
	}

	private void closeChannels() {
		final Collection<IChannel> channels = m_channels.values();
		for (IChannel channel : channels)
			channel.close();
	}
}
