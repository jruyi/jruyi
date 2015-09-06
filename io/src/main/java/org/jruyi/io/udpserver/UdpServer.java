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
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.jruyi.common.IService;
import org.jruyi.common.ITimeoutNotifier;
import org.jruyi.common.ITimer;
import org.jruyi.common.ITimerAdmin;
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

	private String m_caption;
	private Configuration m_conf;
	private DatagramChannel m_datagramChannel;

	private IBufferFactory m_bf;
	private IChannelAdmin m_ca;
	private IFilterManager m_fm;
	private ITimerAdmin m_ta;

	private ITimer m_timer;

	private IFilterList m_filters;
	private boolean m_closed;
	private ISessionListener<I, O> m_listener;
	private ConcurrentHashMap<Object, IChannel> m_channels;
	private final ReentrantReadWriteLock m_lock = new ReentrantReadWriteLock();

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
	public <S> ITimeoutNotifier<S> createTimeoutNotifier(S subject) {
		return m_timer.createNotifier(subject);
	}

	@Override
	public void onChannelClosed(IChannel channel) {
		c_logger.debug("{}: CLOSED", channel);

		final ConcurrentHashMap<Object, IChannel> channels = m_channels;
		if (channels != null)
			channels.remove(channel.remoteAddress());

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
			c_logger.debug("{}: IDLE_TIMEOUT", channel);

			final ISessionListener<I, O> listener = m_listener;
			if (listener != null)
				listener.onSessionIdleTimedOut(channel);
		} finally {
			channel.close();
		}
	}

	@Override
	public void onChannelOpened(IChannel channel) {
		c_logger.debug("{}: OPENED", channel);

		final Object key = channel.remoteAddress();
		final ReadLock readLock = m_lock.readLock();
		if (!readLock.tryLock()) {
			channel.close();
			return;
		}

		try {
			if (m_closed) {
				channel.close();
				return;
			}
			m_channels.put(key, channel);
		} finally {
			readLock.unlock();
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
		final IChannel channel = m_channels.get(session.remoteAddress());
		if (channel != null) {
			channel.write(msg);
			return;
		}

		c_logger.warn(StrUtil.join(session, "(remoteAddr=", session.remoteAddress(),
				") failed to send(channel closed): ", StrUtil.getLineSeparator(), msg));

		if (msg instanceof AutoCloseable) {
			try {
				((AutoCloseable) msg).close();
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(session, "(remoteAddr=", session.remoteAddress(),
						") failed to close message: ", StrUtil.getLineSeparator(), msg), t);
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
			m_timer.configuration().wheelSize(Util.max(timeout, 0)).apply();
			if (timeout == 0)
				closeChannels();
		}
		m_conf = newConf;
		return changed;
	}

	@Override
	protected void startInternal() throws Exception {
		c_logger.info(StrUtil.join("Starting ", this, "..."));

		m_closed = false;

		final Configuration conf = m_conf;
		InetAddress bindAddr = null;
		final String host = conf.bindAddr();
		if (host != null)
			bindAddr = InetAddress.getByName(host);

		m_timer.start();

		final SocketAddress localAddr;
		final DatagramChannel datagramChannel = DatagramChannel.open();
		try {
			final DatagramSocket socket = datagramChannel.socket();
			initSocket(socket, conf);
			localAddr = new InetSocketAddress(bindAddr, conf.port());
			socket.bind(localAddr);
			datagramChannel.configureBlocking(false);
		} catch (Throwable t) {
			try {
				datagramChannel.close();
			} catch (Throwable e) {
			}
			c_logger.error(StrUtil.join(this, " failed to start"), t);
			m_datagramChannel = null;
			m_channels = null;
			m_timer.stop();
			throw t;
		}

		m_datagramChannel = datagramChannel;

		@SuppressWarnings("unchecked")
		final UdpServerChannel udpServerChannel = new UdpServerChannel((UdpServer<Object, Object>) this,
				datagramChannel, localAddr);
		m_ca.onRegisterRequired(udpServerChannel);

		c_logger.info(StrUtil.join(this, " started: ", conf.port()));
	}

	@Override
	protected void stopInternal() {
		c_logger.info(StrUtil.join("Stopping ", this, "..."));

		try {
			m_datagramChannel.close();
		} catch (Throwable t) {
			c_logger.error(StrUtil.join(this, " failed to close DatagramChannel"), t);
		}

		final WriteLock writeLock = m_lock.writeLock();
		writeLock.lock();
		try {
			m_closed = true;
		} finally {
			writeLock.unlock();
		}

		closeChannels();

		m_datagramChannel = null;

		m_timer.stop();

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

	@Reference(name = "timerAdmin", policy = ReferencePolicy.DYNAMIC)
	public synchronized void setTimerAdmin(ITimerAdmin ta) {
		m_ta = ta;
	}

	public synchronized void unsetTimerAdmin(ITimerAdmin ta) {
		if (m_ta == ta)
			m_ta = null;
	}

	public void activate(Map<String, ?> properties) throws Exception {
		final String id = (String) properties.get(IoConstants.SERVICE_ID);
		m_caption = StrUtil.join("UdpServer[", id, "]");
		final Configuration conf = new Configuration();
		conf.initialize(properties);
		updateFilters(conf);
		m_conf = conf;

		m_channels = new ConcurrentHashMap<>(conf.initCapacityOfChannelMap());
		m_timer = m_ta.createTimer(Util.max(conf.sessionIdleTimeoutInSeconds(), 0));
	}

	public void deactivate() {
		stop();
		m_timer = null;
		m_channels = null;

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
