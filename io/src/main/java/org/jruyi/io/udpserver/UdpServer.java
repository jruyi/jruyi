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

import java.io.Closeable;
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
import org.jruyi.common.Service;
import org.jruyi.common.StrUtil;
import org.jruyi.io.IBufferFactory;
import org.jruyi.io.IFilter;
import org.jruyi.io.ISession;
import org.jruyi.io.ISessionListener;
import org.jruyi.io.ISessionService;
import org.jruyi.io.IoConstants;
import org.jruyi.io.channel.IChannel;
import org.jruyi.io.channel.IChannelAdmin;
import org.jruyi.io.channel.IChannelService;
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
public final class UdpServer extends Service implements IChannelService,
		ISessionService {

	private static final Logger c_logger = LoggerFactory
			.getLogger(UdpServer.class);
	private String m_caption;
	private Configuration m_conf;
	private DatagramChannel m_datagramChannel;

	private IBufferFactory m_bf;
	private IChannelAdmin m_ca;
	private IFilterManager m_fm;

	private IFilter<?, ?>[] m_filters;
	private boolean m_closed;
	private ISessionListener m_listener;
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
	public IFilter<?, ?>[] getFilterChain() {
		return m_filters;
	}

	@Override
	public void onChannelClosed(IChannel channel) {
		c_logger.debug("{}: CLOSED", channel);

		final ConcurrentHashMap<Object, IChannel> channels = m_channels;
		if (channels != null)
			channels.remove(channel.remoteAddress());

		final ISessionListener listener = m_listener;
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
		// final ConcurrentHashMap<Object, IChannel> channels = m_channels;
		// if (channels == null)
		// return;
		//
		// IChannel channel = channels.get(session.remoteAddress());
		// if (channel != null)
		// channel.close();
		((IChannel) session).close();
	}

	@Override
	public void onChannelConnectTimedOut(IChannel channel) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void onChannelException(IChannel channel, Throwable t) {
		try {
			c_logger.error(StrUtil.join(channel, " got an error"), t);

			final ISessionListener listener = m_listener;
			if (listener != null)
				listener.onSessionException(channel, t);
		} catch (Throwable e) {
			c_logger.error(StrUtil.join(channel, " Unexpected Error: "), e);
		} finally {
			channel.close();
		}
	}

	@Override
	public void onChannelIdleTimedOut(IChannel channel) {
		try {
			c_logger.debug("{}: IDLE_TIMEOUT", channel);

			final ISessionListener listener = m_listener;
			if (listener != null) {
				listener.onSessionIdleTimedOut(channel);
			}
		} catch (Throwable t) {
			c_logger.error(StrUtil.join(channel, " Unexpected Error: "), t);
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
		} catch (Throwable t) {
			c_logger.error(StrUtil.join(channel, " Unexpected Error: "), t);
		} finally {
			readLock.unlock();
		}

		final ISessionListener listener = m_listener;
		if (listener != null) {
			try {
				listener.onSessionOpened(channel);
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(channel, " Unexpected Error: "), t);
			}
		}
	}

	@Override
	public void onChannelReadTimedOut(IChannel channel) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void onMessageReceived(IChannel channel, Object msg) {
		// failed to reschedule, channel timed out
		if (!scheduleIdleTimeout(channel))
			return;

		final ISessionListener listener = m_listener;
		if (listener != null) {
			try {
				listener.onMessageReceived(channel, msg);
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(channel, " Unexpected Error: "), t);
			}
		}
	}

	@Override
	public void onMessageSent(IChannel channel, Object msg) {
		final ISessionListener listener = m_listener;
		if (listener != null) {
			try {
				listener.onMessageSent(channel, msg);
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(channel, " Unexpected Error: "), t);
			}
		}
	}

	@Override
	public void setSessionListener(ISessionListener listener) {
		m_listener = listener;
	}

	@Override
	public void openSession() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void write(ISession session, Object msg) {
		final IChannel channel = m_channels.get(session.remoteAddress());
		if (channel != null) {
			channel.write(msg);
			return;
		}

		c_logger.warn(StrUtil.join(session,
				" failed to send(channel closed): ",
				StrUtil.getLineSeparator(), msg));

		if (msg instanceof Closeable) {
			try {
				((Closeable) msg).close();
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(session,
						" failed to close message: ",
						StrUtil.getLineSeparator(), msg), t);
			}
		}

		// TODO: need notify failure on writing out?
	}

	@Override
	public String toString() {
		return m_caption;
	}

	@Override
	protected boolean updateInternal(Map<String, ?> properties)
			throws Exception {

		final Configuration newConf = new Configuration();
		newConf.initialize(properties);

		final Configuration oldConf = m_conf;
		updateConf(newConf);

		return oldConf.isMandatoryChanged(newConf,
				Configuration.getMandatoryPropsAccessors());
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

		m_channels = new ConcurrentHashMap<Object, IChannel>(
				conf.initCapacityOfChannelMap());

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
		m_ca.onRegisterRequired(new UdpServerChannel(this, datagramChannel,
				localAddr));

		c_logger.info(StrUtil.join(this, " started: ", conf.port()));
	}

	@Override
	protected void stopInternal() {
		c_logger.info(StrUtil.join("Stopping ", this, "..."));

		try {
			m_datagramChannel.close();
		} catch (Throwable t) {
			c_logger.error(
					StrUtil.join(this, " failed to close DatagramChannel"), t);
		}

		final WriteLock writeLock = m_lock.writeLock();
		writeLock.lock();
		try {
			m_closed = true;
		} finally {
			writeLock.unlock();
		}

		final Collection<IChannel> channels = m_channels.values();
		for (IChannel channel : channels)
			channel.close();

		m_channels = null;

		m_datagramChannel = null;

		c_logger.info(StrUtil.join(this, " stopped"));
	}

	@Reference(name = "buffer", policy = ReferencePolicy.DYNAMIC)
	protected synchronized void setBufferFactory(IBufferFactory bf) {
		m_bf = bf;
	}

	protected synchronized void unsetBufferFactory(IBufferFactory bf) {
		if (m_bf == bf)
			m_bf = null;
	}

	@Reference(name = "channelAdmin")
	protected void setChannelAdmin(IChannelAdmin ca) {
		m_ca = ca;
	}

	protected void unsetChannelAdmin(IChannelAdmin ca) {
		m_ca = null;
	}

	@Reference(name = "filterManager")
	protected void setFilterManager(IFilterManager fm) {
		m_fm = fm;
	}

	protected void unsetFilterManager(IFilterManager fm) {
		m_fm = null;
	}

	protected void activate(Map<String, ?> properties) throws Exception {
		final String id = (String) properties.get(IoConstants.SERVICE_ID);
		m_caption = StrUtil.join("UdpServer[", id, "]");
		final Configuration conf = new Configuration();
		conf.initialize(properties);
		updateConf(conf);
	}

	protected void deactivate() {
		stop();

		updateConf(null);
	}

	IChannel getChannel(SocketAddress key) {
		return m_channels.get(key);
	}

	private static void initSocket(DatagramSocket socket, Configuration conf)
			throws SocketException {

		socket.setReuseAddress(true);

		final Integer recvBufSize = conf.recvBufSize();
		if (recvBufSize != null)
			socket.setReceiveBufferSize(recvBufSize);
	}

	private void updateConf(Configuration newConf) {
		final String[] newNames = newConf == null ? StrUtil
				.getEmptyStringArray() : newConf.filters();
		String[] oldNames = StrUtil.getEmptyStringArray();
		final IFilterManager fm = m_fm;
		if (m_conf == null)
			m_filters = fm.getFilters(oldNames);
		else
			oldNames = m_conf.filters();

		m_conf = newConf;

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
}
