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

package org.jruyi.io.udpserver;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jruyi.common.IService;
import org.jruyi.common.Service;
import org.jruyi.common.StrUtil;
import org.jruyi.io.IBufferFactory;
import org.jruyi.io.ISession;
import org.jruyi.io.ISessionListener;
import org.jruyi.io.ISessionService;
import org.jruyi.io.IoConstants;
import org.jruyi.io.channel.IChannel;
import org.jruyi.io.channel.IChannelAdmin;
import org.jruyi.io.channel.IChannelService;
import org.jruyi.io.channel.ISelector;
import org.jruyi.io.common.Util;
import org.jruyi.io.filter.IFilterList;
import org.jruyi.io.filter.IFilterManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = IoConstants.CN_UDPSERVER_FACTORY, //
		factory = "udpserver", //
		service = { IService.class }, //
		xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class UdpServer<I, O> extends Service implements IChannelService<I, O>, ISessionService<I, O> {

	private static final Logger c_logger = LoggerFactory.getLogger(UdpServer.class);

	private final AtomicLong m_sequence = new AtomicLong(0L);

	private String m_caption;
	private Configuration m_conf;
	private DatagramChannel m_datagramChannel;

	private IBufferFactory m_bf;
	private IChannelAdmin m_ca;
	private IFilterManager m_fm;

	private IFilterList m_filters;
	private volatile boolean m_stopped = true;
	private ISessionListener<I, O> m_listener;
	private ConcurrentHashMap<Object, IChannel> m_channels;

	@Override
	public long generateId() {
		return m_sequence.incrementAndGet();
	}

	@Override
	public Object getConfiguration() {
		return m_conf;
	}

	@Override
	public IChannelAdmin getChannelAdmin() {
		return m_ca;
	}

	@Override
	public IBufferFactory getBufferFactory() {
		return m_bf;
	}

	@Override
	public long throttle() {
		return 0L;
	}

	@Override
	public IFilterList getFilterChain() {
		return m_filters;
	}

	@Override
	public void onChannelClosed(IChannel channel) {
		final Object remoteAddr = channel.remoteAddress();
		c_logger.debug("{}(remoteAddr={}): CLOSED", channel, remoteAddr);

		final ConcurrentHashMap<Object, IChannel> channels = m_channels;
		if (channels != null)
			channels.remove(remoteAddr);

		final ISessionListener<I, O> listener = m_listener;
		if (listener != null) {
			try {
				listener.onSessionClosed(channel);
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(channel, " Unexpected Error: "), t);
			}
		}
	}

	@Override
	public void closeSession(ISession session) {
		((IChannel) session).close();
	}

	@Override
	public void onChannelConnectTimedOut(IChannel channel) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void onChannelException(IChannel channel, Throwable throwable) {
		try {
			c_logger.error(StrUtil.join(channel, "(remoteAddr=", channel.remoteAddress(), ") got an error"), throwable);

			final ISessionListener<I, O> listener = m_listener;
			if (listener != null)
				listener.onSessionException(channel, throwable);
		} catch (Throwable t) {
			c_logger.error(StrUtil.join(channel, "(remoteAddr=", channel.remoteAddress(), ") Unexpected Error: "), t);
		} finally {
			channel.close();
		}
	}

	@Override
	public void onChannelIdleTimedOut(IChannel channel) {
		try {
			c_logger.debug("{}(remoteAddr={}): IDLE_TIMEOUT", channel, channel.remoteAddress());

			final ISessionListener<I, O> listener = m_listener;
			if (listener != null)
				listener.onSessionIdleTimedOut(channel);
		} finally {
			channel.close();
		}
	}

	@Override
	public void onChannelOpened(IChannel channel) {
		c_logger.debug("{}(remoteAddr={}): OPENED", channel, channel.remoteAddress());

		final Object key = channel.remoteAddress();
		final ConcurrentHashMap<Object, IChannel> channels = m_channels;
		if (channels != null)
			channels.put(key, channel);

		if (m_stopped) {
			channel.close();
			return;
		}

		final ISessionListener<I, O> listener = m_listener;
		if (listener != null)
			listener.onSessionOpened(channel);
	}

	@Override
	public void onChannelReadTimedOut(IChannel channel) {
		throw new UnsupportedOperationException("Not supported yet.");
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
	public void setSessionListener(ISessionListener<I, O> listener) {
		m_listener = listener;
	}

	@Override
	public void openSession() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void write(ISession session, O msg) {
		final Object remoteAddr = session.remoteAddress();
		final IChannel channel = m_channels.get(remoteAddr);
		if (channel != null) {
			channel.write(msg);
			return;
		}

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
	public String toString() {
		return m_caption;
	}

	@Override
	protected boolean updateInternal(Map<String, ?> properties) throws Exception {

		final Configuration newConf = new Configuration();
		newConf.initialize(properties);

		final Configuration oldConf = m_conf;
		updateFilters(newConf);

		final boolean changed = oldConf.isMandatoryChanged(newConf, Configuration.getMandatoryPropsAccessors());
		if (!changed) {
			final int timeout = newConf.sessionIdleTimeoutInSeconds();
			if (timeout == 0)
				closeChannels();
		}
		m_conf = newConf;
		return changed;
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

		final SocketAddress localAddr;
		final DatagramChannel datagramChannel = DatagramChannel.open();
		try {
			final DatagramSocket socket = datagramChannel.socket();
			initSocket(socket, conf);
			localAddr = new InetSocketAddress(bindAddr, conf.port());
			socket.bind(localAddr);
			datagramChannel.configureBlocking(false);
		} catch (Exception e) {
			try {
				datagramChannel.close();
			} catch (Throwable t) {
			}
			c_logger.error(StrUtil.join(this, " failed to start"), e);
			m_datagramChannel = null;
			m_channels = null;
			throw e;
		}

		m_datagramChannel = datagramChannel;

		@SuppressWarnings("unchecked")
		final UdpServerChannel udpServerChannel = new UdpServerChannel((UdpServer<Object, Object>) this,
				datagramChannel, localAddr, m_ca);
		ISelector selector = m_ca.designateSelector(udpServerChannel.id().intValue());
		udpServerChannel.selector(selector);
		selector.accept(udpServerChannel);

		c_logger.info(StrUtil.join(this, " started: ", conf.port()));
	}

	@Override
	protected void stopInternal() {
		c_logger.info(StrUtil.join("Stopping ", this, "..."));

		m_stopped = true;

		try {
			m_datagramChannel.close();
		} catch (Throwable t) {
			c_logger.error(StrUtil.join(this, " failed to close DatagramChannel"), t);
		}

		closeChannels();

		m_datagramChannel = null;

		c_logger.info(StrUtil.join(this, " stopped"));
	}

	@Reference(name = "buffer", policy = ReferencePolicy.DYNAMIC)
	public synchronized void setBufferFactory(IBufferFactory bf) {
		m_bf = bf;
	}

	public synchronized void unsetBufferFactory(IBufferFactory bf) {
		if (m_bf == bf)
			m_bf = null;
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

	public void activate(Map<String, ?> properties) throws Exception {
		final Configuration conf = new Configuration();
		conf.initialize(properties);
		updateFilters(conf);
		m_caption = Util.genServiceId(properties, conf.ip(), conf.port(), "UdpServer");
		m_conf = conf;

		m_channels = new ConcurrentHashMap<>(conf.initCapacityOfChannelMap());
	}

	public void deactivate() {
		stop();

		updateFilters(null);
	}

	IChannel getChannel(SocketAddress key) {
		return m_channels.get(key);
	}

	private static void initSocket(DatagramSocket socket, Configuration conf) throws SocketException {

		socket.setReuseAddress(true);

		final Integer recvBufSize = conf.recvBufSize();
		if (recvBufSize != null)
			socket.setReceiveBufferSize(recvBufSize);
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

	private boolean scheduleIdleTimeout(IChannel channel) {
		final int timeout = m_conf.sessionIdleTimeoutInSeconds();
		if (timeout > 0)
			return channel.scheduleIdleTimeout(timeout);

		if (timeout == 0)
			channel.close();

		return true;
	}

	private void closeChannels() {
		final Collection<IChannel> channels = m_channels.values();
		for (IChannel channel : channels)
			channel.close();
	}
}
