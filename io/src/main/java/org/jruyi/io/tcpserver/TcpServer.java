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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jruyi.common.*;
import org.jruyi.io.IBufferFactory;
import org.jruyi.io.ISession;
import org.jruyi.io.ISessionListener;
import org.jruyi.io.ISessionService;
import org.jruyi.io.IoConstants;
import org.jruyi.io.channel.IChannel;
import org.jruyi.io.channel.IChannelAdmin;
import org.jruyi.io.channel.IChannelService;
import org.jruyi.io.common.Util;
import org.jruyi.io.filter.IFilterList;
import org.jruyi.io.filter.IFilterManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = IoConstants.CN_TCPSERVER_FACTORY, //
		factory = "tcpserver", //
		service = { IService.class }, //
		xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class TcpServer<I, O> extends Service implements IChannelService<I, O>, ISessionService<I, O> {

	private static final Logger c_logger = LoggerFactory.getLogger(TcpServer.class);

	private final AtomicLong m_sequence = new AtomicLong(0L);

	private IBufferFactory m_bf;
	private ITimerAdmin m_ta;
	private IChannelAdmin m_ca;
	private IFilterManager m_fm;
	private ITcpAcceptor m_acceptor;

	private ITimer m_timer;

	private String m_caption;
	private Configuration m_conf;
	private ServerSocketChannel m_ssc;

	private IFilterList m_filters;
	private volatile boolean m_stopped = true;
	private ISessionListener<I, O> m_listener;
	private ConcurrentHashMap<Long, IChannel> m_channels;

	@Override
	public long generateId() {
		return m_sequence.incrementAndGet();
	}

	@Override
	public void setSessionListener(ISessionListener<I, O> listener) {
		m_listener = listener;
	}

	@Override
	public Configuration getConfiguration() {
		return m_conf;
	}

	@Override
	public IBufferFactory getBufferFactory() {
		return m_bf;
	}

	@Override
	public IChannelAdmin getChannelAdmin() {
		return m_ca;
	}

	@Override
	public long throttle() {
		return m_conf.throttle();
	}

	@Override
	public IFilterList getFilterChain() {
		return m_filters;
	}

	@Override
	public <S> ITimeoutNotifier<S> createTimeoutNotifier(S subject) {
		return m_timer.createNotifier(subject);
	}

	@Override
	public void onChannelIdleTimedOut(IChannel channel) {
		try {
			c_logger.debug("{}: IDLE_TIMEOUT", channel);

			final ISessionListener<I, O> listener = m_listener;
			if (listener != null)
				listener.onSessionIdleTimedOut(channel);
		} finally {
			channel.close();
		}
	}

	@Override
	public void onChannelConnectTimedOut(IChannel channel) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void onChannelReadTimedOut(IChannel channel) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void onChannelException(IChannel channel, Throwable t) {
		try {
			c_logger.error(StrUtil.join(channel, "(remoteAddr=", channel.remoteAddress(), ") got an error"), t);

			final ISessionListener<I, O> listener = m_listener;
			if (listener != null)
				listener.onSessionException(channel, t);
		} catch (Throwable e) {
			c_logger.error(StrUtil.join(channel, "(remoteAddr=", channel.remoteAddress(), ") Unexpected Error: "), e);
		} finally {
			channel.close();
		}
	}

	@Override
	public void onChannelOpened(IChannel channel) {
		c_logger.debug("{}(remoteAddr={}): OPENED", channel, channel.remoteAddress());

		final Long id = channel.id();
		final ConcurrentHashMap<Long, IChannel> channels = m_channels;
		if (channels != null)
			channels.put(id, channel);

		if (m_stopped) {
			channel.close();
			return;
		}

		// failed to schedule, channel has been closed
		if (!scheduleIdleTimeout(channel))
			return;

		final ISessionListener<I, O> listener = m_listener;
		if (listener != null)
			listener.onSessionOpened(channel);
	}

	@Override
	public void onChannelClosed(IChannel channel) {
		c_logger.debug("{}(remoteAddr={}): CLOSED", channel, channel.remoteAddress());

		final ConcurrentHashMap<Long, IChannel> channels = m_channels;
		if (channels != null)
			channels.remove(channel.id());

		final ISessionListener<I, O> listener = m_listener;
		if (listener != null) {
			try {
				listener.onSessionClosed(channel);
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(channel, "(remoteAddr=", channel.remoteAddress(), ") Unexpected Error: "),
						t);
			}
		}
	}

	@Override
	public void onMessageReceived(IChannel channel, I inMsg) {
		// failed to reschedule, channel timed out
		if (!scheduleIdleTimeout(channel))
			return;

		final ISessionListener<I, O> listener = m_listener;
		if (listener != null)
			listener.onMessageReceived(channel, inMsg);
	}

	@Override
	public void beforeSendMessage(IChannel channel, O outMsg) {
		final ISessionListener<I, O> listener = m_listener;
		if (listener != null)
			listener.beforeSendMessage(channel, outMsg);
	}

	@Override
	public void onMessageSent(IChannel channel, O outMsg) {
		final ISessionListener<I, O> listener = m_listener;
		if (listener != null)
			listener.onMessageSent(channel, outMsg);
	}

	@Override
	public void openSession() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void closeSession(ISession session) {
		((IChannel) session).close();
	}

	@Override
	public void write(ISession session, O msg) {
		final ConcurrentHashMap<Long, IChannel> channels = m_channels;
		if (channels == null)
			return;

		final IChannel channel = channels.get(session.id());
		if (channel != null) {
			channel.write(msg);
			return;
		}

		final Object remoteAddr = session.remoteAddress();
		c_logger.warn(StrUtil.join(session, "(remoteAddr=", remoteAddr, ") failed to send(channel closed): ",
				StrUtil.getLineSeparator(), msg));

		if (msg instanceof AutoCloseable) {
			try {
				((AutoCloseable) msg).close();
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(session, "(remoteAddr=", remoteAddr, ") failed to close message: ",
						StrUtil.getLineSeparator(), msg), t);
			}
		}
	}

	@Override
	protected void startInternal() throws Exception {
		c_logger.info(StrUtil.join("Starting ", this, "..."));

		m_stopped = false;

		final Configuration conf = m_conf;
		InetAddress bindAddr = null;
		final String host = conf.bindAddr();
		if (host != null)
			bindAddr = InetAddress.getByName(host);

		m_timer.start();

		final ServerSocketChannel ssc = ServerSocketChannel.open();
		try {
			final ServerSocket socket = ssc.socket();
			initSocket(socket, conf);
			final SocketAddress ep = new InetSocketAddress(bindAddr, conf.port());
			final Integer backlog = conf.backlog();
			if (backlog == null)
				socket.bind(ep);
			else
				socket.bind(ep, backlog);

			m_ssc = ssc;

			m_acceptor.doAccept(this);

			c_logger.info(StrUtil.join(this, " started, listening on ", socket.getLocalSocketAddress()));
		} catch (Exception e) {
			try {
				ssc.close();
			} catch (Throwable t) {
			}
			c_logger.error(StrUtil.join(this, " failed to start"), e);
			m_ssc = null;
			m_timer.stop();
			throw e;
		}
	}

	@Override
	protected void stopInternal() {
		stopInternal(0);
	}

	@Override
	protected void stopInternal(int options) {
		c_logger.info(StrUtil.join("Stopping ", this, "..."));

		m_stopped = true;

		try {
			m_ssc.close();
		} catch (Throwable t) {
			c_logger.error(StrUtil.join(this, " failed to close ServerSocketChannel"), t);
		}
		m_ssc = null;

		if (options == 0) {
			closeChannels();
			m_timer.stop();
		}

		c_logger.info(StrUtil.join(this, " stopped"));
	}

	@Override
	public String toString() {
		return m_caption;
	}

	@Override
	protected boolean updateInternal(Map<String, ?> properties) throws Exception {

		final String id = (String) properties.get(IoConstants.SERVICE_ID);
		m_caption = StrUtil.join("TcpServer[", id, "]");

		final Configuration newConf = new Configuration();
		newConf.initialize(properties);

		final Configuration oldConf = m_conf;
		updateFilters(newConf);

		final boolean changed = oldConf.isMandatoryChanged(newConf);
		if (!changed) {
			final int timeout = newConf.sessionIdleTimeoutInSeconds();
			m_timer.configuration().wheelSize(Util.max(timeout, 0)).apply();
			if (timeout == 0)
				closeChannels();
		}
		m_conf = newConf;
		return changed;
	}

	@Reference(name = "buffer", policy = ReferencePolicy.DYNAMIC)
	public synchronized void setBufferFactory(IBufferFactory bf) {
		m_bf = bf;
	}

	public synchronized void unsetBufferFactory(IBufferFactory bf) {
		if (m_bf == bf)
			m_bf = null;
	}

	@Reference(name = "timerAdmin", policy = ReferencePolicy.DYNAMIC)
	public synchronized void setTimerAdmin(ITimerAdmin ta) {
		m_ta = ta;
	}

	public synchronized void unsetTimerAdmin(ITimerAdmin ta) {
		if (m_ta == ta)
			m_ta = null;
	}

	@Reference(name = "channelAdmin")
	public void setChannelAdmin(IChannelAdmin ca) {
		m_ca = ca;
	}

	public void unsetChannelAdmin(IChannelAdmin ca) {
		m_ca = null;
	}

	@Reference(name = "filterManager")
	public void setFilterManager(IFilterManager fm) {
		m_fm = fm;
	}

	public void unsetFilterManager(IFilterManager fm) {
		m_fm = null;
	}

	@Reference(name = "tcpAcceptor")
	public void setTcpAcceptor(ITcpAcceptor acceptor) {
		m_acceptor = acceptor;
	}

	public void unsetTcpAcceptor(ITcpAcceptor acceptor) {
		m_acceptor = null;
	}

	public void activate(Map<String, ?> properties) throws Exception {
		final Configuration conf = new Configuration();
		conf.initialize(properties);
		updateFilters(conf);

		m_caption = Util.genServiceId(properties, conf.bindAddr(), conf.port(), "TcpServer");
		m_conf = conf;

		m_channels = new ConcurrentHashMap<>(conf.initCapacityOfChannelMap());
		m_timer = m_ta.createTimer(Util.max(conf.sessionIdleTimeoutInSeconds(), 0));
	}

	public void deactivate() {
		stop();
		m_timer = null;

		updateFilters(null);
		m_conf = null;
	}

	SelectableChannel getSelectableChannel() {
		return m_ssc;
	}

	private boolean scheduleIdleTimeout(IChannel channel) {
		final int timeout = m_conf.sessionIdleTimeoutInSeconds();
		if (timeout > 0)
			return channel.scheduleIdleTimeout(timeout);

		if (timeout == 0)
			channel.close();

		return true;
	}

	private void updateFilters(Configuration newConf) {
		final String[] newNames = newConf == null ? StrUtil.getEmptyStringArray() : newConf.filters();
		String[] oldNames = StrUtil.getEmptyStringArray();
		final IFilterManager fm = m_fm;
		if (m_conf == null)
			m_filters = fm.getFilters(oldNames);
		else
			oldNames = m_conf.filters();

		if (Arrays.equals(newNames, oldNames))
			return;

		m_filters = fm.getFilters(newNames);
		fm.ungetFilters(oldNames);
	}

	private void closeChannels() {
		final Collection<IChannel> channels = m_channels.values();
		for (final IChannel channel : channels)
			channel.close();
	}

	private static void initSocket(ServerSocket socket, Configuration conf) throws SocketException {
		final int[] performancePreferences = conf.performancePreferences();
		if (performancePreferences != null) {
			int n = performancePreferences.length;
			int connectionTime = 0;
			int latency = 0;
			int bandWidth = 0;
			if (n > 2) {
				connectionTime = performancePreferences[0];
				latency = performancePreferences[1];
				bandWidth = performancePreferences[2];
			} else if (n > 1) {
				connectionTime = performancePreferences[0];
				latency = performancePreferences[1];
			} else if (n > 0)
				connectionTime = performancePreferences[0];

			socket.setPerformancePreferences(connectionTime, latency, bandWidth);
		}

		if (conf.reuseAddr())
			socket.setReuseAddress(true);

		final Integer recvBufSize = conf.recvBufSize();
		if (recvBufSize != null)
			socket.setReceiveBufferSize(recvBufSize);
	}
}
