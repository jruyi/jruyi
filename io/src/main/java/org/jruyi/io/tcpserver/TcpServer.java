/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use file except in compliance with the License.
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

import java.io.Closeable;
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
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
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
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@org.apache.felix.scr.annotations.Service(IService.class)
@Component(name = IoConstants.CN_TCPSERVER_FACTORY, factory = "tcpserver", createPid = false, specVersion = "1.1.0")
public final class TcpServer extends Service implements IChannelService,
		ISessionService {

	private static final Logger c_logger = LoggerFactory
			.getLogger(TcpServer.class);
	private String m_caption;
	private Configuration m_conf;
	private ServerSocketChannel m_ssc;

	@Reference(name = "channelAdmin")
	private IChannelAdmin m_ca;

	@Reference(name = "tcpAcceptor")
	private ITcpAcceptor m_acceptor;

	@Reference(name = "filterManager")
	private IFilterManager m_fm;

	@Reference(name = "buffer", policy = ReferencePolicy.DYNAMIC, bind = "bindBufferFactory", unbind = "unbindBufferFactory")
	private IBufferFactory m_bf;

	private IFilter[] m_filters;
	private boolean m_closed;
	private ISessionListener m_listener;
	private ConcurrentHashMap<Object, IChannel> m_channels;
	private final ReentrantReadWriteLock m_lock = new ReentrantReadWriteLock();

	@Override
	public void setSessionListener(ISessionListener listener) {
		m_listener = listener;
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
	public int readThreshold() {
		return m_conf.readThreshold();
	}

	@Override
	public IFilter[] getFilterChain() {
		return m_filters;
	}

	@Override
	public void onChannelIdleTimedOut(IChannel channel) {
		try {
			c_logger.debug("{}: IDLE_TIMEOUT", channel);

			final ISessionListener listener = m_listener;
			if (listener != null)
				listener.onSessionIdleTimedOut(channel);
		} catch (Throwable t) {
			c_logger.error(StrUtil.buildString(channel, " Unexpected Error: "),
					t);
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
			c_logger.error(StrUtil.buildString(channel, " got an error"), t);

			final ISessionListener listener = m_listener;
			if (listener != null)
				listener.onSessionException(channel, t);
		} catch (Throwable e) {
			c_logger.error(StrUtil.buildString(channel, " Unexpected Error: "),
					e);
		} finally {
			channel.close();
		}
	}

	@Override
	public void onChannelOpened(IChannel channel) {
		c_logger.debug("{}: OPENED", channel);

		Object id = channel.id();
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

			m_channels.put(id, channel);
		} catch (Throwable t) {
			c_logger.error(StrUtil.buildString(channel, " Unexpected Error: "),
					t);
		} finally {
			readLock.unlock();
		}

		// failed to schedule, channel has been closed
		if (!scheduleIdleTimeout(channel))
			return;

		final ISessionListener listener = m_listener;
		if (listener != null)
			listener.onSessionOpened(channel);
	}

	@Override
	public void onChannelClosed(IChannel channel) {
		c_logger.debug("{}: CLOSED", channel);

		final ConcurrentHashMap<Object, IChannel> channels = m_channels;
		if (channels != null)
			channels.remove(channel.id());

		final ISessionListener listener = m_listener;
		if (listener != null) {
			try {
				listener.onSessionClosed(channel);
			} catch (Throwable t) {
				c_logger.error(
						StrUtil.buildString(channel, " Unexpected Error: "), t);
			}
		}
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
				c_logger.error(
						StrUtil.buildString(channel, " Unexpected Error: "), t);
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
				c_logger.error(
						StrUtil.buildString(channel, " Unexpected Error: "), t);
			}
		}
	}

	@Override
	public void openSession() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void closeSession(ISession session) {
		// final ConcurrentHashMap<Object, IChannel> channels = m_channels;
		// if (channels == null)
		// return;
		//
		// IChannel channel = channels.get(session.id());
		// if (channel != null)
		// channel.close();
		((IChannel) session).close();
	}

	@Override
	public void write(ISession session, Object msg) {
		final ConcurrentHashMap<Object, IChannel> channels = m_channels;
		if (channels == null)
			return;

		IChannel channel = channels.get(session.id());
		if (channel != null) {
			channel.write(msg);
			return;
		}

		c_logger.warn(StrUtil.buildString(session,
				" failed to send(channel closed): ",
				StrUtil.getLineSeparator(), msg));

		if (msg instanceof Closeable) {
			try {
				((Closeable) msg).close();
			} catch (Throwable t) {
				c_logger.error(StrUtil.buildString(session,
						" failed to close message: ",
						StrUtil.getLineSeparator(), msg), t);
			}
		}

		// TODO: need notify failure on writing out?
	}

	@Override
	protected void startInternal() throws Exception {
		c_logger.info(StrUtil.buildString("Starting ", this, "..."));

		m_closed = false;

		Configuration conf = m_conf;
		InetAddress bindAddr = null;
		String host = conf.bindAddr();
		if (host != null)
			bindAddr = InetAddress.getByName(host);

		if (m_channels == null)
			m_channels = new ConcurrentHashMap<Object, IChannel>(
					conf.initCapacityOfChannelMap());

		ServerSocketChannel ssc = ServerSocketChannel.open();
		try {
			ServerSocket socket = ssc.socket();
			initSocket(socket, conf);
			SocketAddress ep = new InetSocketAddress(bindAddr, conf.port());
			Integer backlog = conf.backlog();
			if (backlog == null)
				socket.bind(ep);
			else
				socket.bind(ep, backlog);

			m_ssc = ssc;

			m_acceptor.doAccept(this);

			c_logger.info(StrUtil.buildString(this, " started, listening on ",
					socket.getLocalSocketAddress()));
		} catch (Exception e) {
			try {
				ssc.close();
			} catch (Throwable t) {
			}
			c_logger.error(StrUtil.buildString(this, " failed to start"), e);
			m_ssc = null;
			throw e;
		}
	}

	@Override
	protected void stopInternal() {
		stopInternal(0);
	}

	@Override
	protected void stopInternal(int options) {
		c_logger.info(StrUtil.buildString("Stopping ", this, "..."));

		try {
			m_ssc.close();
		} catch (Throwable t) {
			c_logger.error(StrUtil.buildString(this,
					" failed to close ServerSocketChannel"), t);
		}
		m_ssc = null;

		final WriteLock writeLock = m_lock.writeLock();
		writeLock.lock();
		try {
			m_closed = true;
		} finally {
			writeLock.unlock();
		}

		if (options == 0)
			closeChannels();

		c_logger.info(StrUtil.buildString(this, " stopped"));
	}

	@Override
	public String toString() {
		return m_caption;
	}

	@Override
	protected boolean updateInternal(Map<String, ?> properties)
			throws Exception {

		String id = (String) properties.get(IoConstants.SERVICE_ID);
		m_caption = StrUtil.buildString("TcpServer[", id, "]");

		Configuration newConf = new Configuration();
		newConf.initialize(properties);

		Configuration oldConf = m_conf;
		updateConf(newConf);

		return oldConf.isMandatoryChanged(newConf);
	}

	protected void bindChannelAdmin(IChannelAdmin ca) {
		m_ca = ca;
	}

	protected void unbindChannelAdmin(IChannelAdmin ca) {
		m_ca = null;
	}

	protected void bindTcpAcceptor(ITcpAcceptor acceptor) {
		m_acceptor = acceptor;
	}

	protected void unbindTcpAcceptor(ITcpAcceptor acceptor) {
		m_acceptor = null;
	}

	protected void bindFilterManager(IFilterManager fm) {
		m_fm = fm;
	}

	protected void unbindFilterManager(IFilterManager fm) {
		m_fm = null;
	}

	protected synchronized void bindBufferFactory(IBufferFactory bf) {
		m_bf = bf;
	}

	protected synchronized void unbindBufferFactory(IBufferFactory bf) {
		if (m_bf == bf)
			m_bf = null;
	}

	protected void activate(ComponentContext context, Map<String, ?> properties)
			throws Exception {
		String id = (String) properties.get(IoConstants.SERVICE_ID);
		m_caption = StrUtil.buildString("TcpServer[", id, "]");

		Configuration conf = new Configuration();
		conf.initialize(properties);
		updateConf(conf);
	}

	protected void deactivate() {
		stop();

		closeChannels();

		updateConf(null);
	}

	SelectableChannel getSelectableChannel() {
		return m_ssc;
	}

	private boolean scheduleIdleTimeout(IChannel channel) {
		int timeout = m_conf.sessionIdleTimeout();
		if (timeout > 0)
			return channel.scheduleIdleTimeout(timeout);

		if (timeout == 0)
			channel.close();

		return true;
	}

	private void updateConf(Configuration newConf) {
		String[] newNames = newConf == null ? StrUtil.getEmptyStringArray()
				: newConf.filters();
		String[] oldNames = StrUtil.getEmptyStringArray();
		IFilterManager fm = m_fm;
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

	private void closeChannels() {
		if (m_channels == null)
			return;

		if (m_channels.size() > 0) {
			Collection<IChannel> channels = m_channels.values();
			for (IChannel channel : channels)
				channel.close();
		}

		m_channels = null;
	}

	private static void initSocket(ServerSocket socket, Configuration conf)
			throws SocketException {
		Integer[] performancePreferences = conf.performancePreferences();
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

		Integer recvBufSize = conf.recvBufSize();
		if (recvBufSize != null)
			socket.setReceiveBufferSize(recvBufSize);
	}
}
