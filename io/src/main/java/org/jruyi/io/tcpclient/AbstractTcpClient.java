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

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

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
import org.jruyi.io.tcp.TcpChannel;
import org.jruyi.io.tcp.TcpChannelConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTcpClient extends Service implements IChannelService, ISessionService {

	private static final Logger c_logger = LoggerFactory.getLogger(AbstractTcpClient.class);

	private String m_caption;
	private IChannelAdmin m_ca;
	private IFilterManager m_fm;
	private IBufferFactory m_bf;
	private IFilter<?, ?>[] m_filters;
	private boolean m_closed = true;
	private ISessionListener m_listener;
	private ConcurrentHashMap<Object, IChannel> m_channels;
	private final ReentrantReadWriteLock m_lock = new ReentrantReadWriteLock();

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
	public void setSessionListener(ISessionListener listener) {
		m_listener = listener;
	}

	@Override
	public void openSession() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void write(ISession session, Object msg) {
		IChannel channel = m_channels.get(session.id());
		if (channel != null) {
			channel.write(msg);
			return;
		}

		c_logger.warn(StrUtil.join(session, " failed to send(channel closed): ", msg));

		if (msg instanceof Closeable) {
			try {
				((Closeable) msg).close();
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
	public final IFilter<?, ?>[] getFilterChain() {
		return m_filters;
	}

	@Override
	public void onChannelClosed(IChannel channel) {
		c_logger.debug("{}: CLOSED", channel);

		final ConcurrentHashMap<Object, IChannel> channels = m_channels;
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
			Object attachment = channel.detach();
			if (attachment != null) {
				c_logger.error(StrUtil.join(channel, " got an error: ", attachment), t);

				if (attachment instanceof Closeable) {
					try {
						((Closeable) attachment).close();
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
		} finally {
			readLock.unlock();
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
	protected boolean updateInternal(Map<String, ?> properties) throws Exception {
		TcpClientConf oldConf = updateConf(properties);
		TcpClientConf newConf = configuration();
		updateFilters(oldConf, newConf);

		return oldConf.isMandatoryChanged(newConf);
	}

	@Override
	protected void startInternal() {
		m_closed = false;
		m_channels = new ConcurrentHashMap<Object, IChannel>(32);
	}

	@Override
	protected void stopInternal() {
		final WriteLock writeLock = m_lock.writeLock();
		writeLock.lock();
		try {
			m_closed = true;
		} finally {
			writeLock.unlock();
		}

		Collection<IChannel> channels = m_channels.values();
		m_channels = null;

		for (IChannel channel : channels)
			channel.close();
	}

	protected void setChannelAdmin(IChannelAdmin cm) {
		m_ca = cm;
	}

	protected void unsetChannelAdmin(IChannelAdmin cm) {
		m_ca = null;
	}

	protected void setFilterManager(IFilterManager fm) {
		m_fm = fm;
	}

	protected void unsetFilterManager(IFilterManager fm) {
		m_fm = null;
	}

	protected synchronized void setBufferFactory(IBufferFactory bf) {
		m_bf = bf;
	}

	protected synchronized void unsetBufferFactory(IBufferFactory bf) {
		if (m_bf == bf)
			m_bf = bf;
	}

	protected void activate(Map<String, ?> properties) throws Exception {
		String id = (String) properties.get(IoConstants.SERVICE_ID);

		m_caption = StrUtil.join("TcpClient[", id, "]");
		updateFilters(updateConf(properties), configuration());
	}

	protected void deactivate() {
		stop();

		updateFilters(updateConf(null), null);
	}

	abstract TcpClientConf updateConf(Map<String, ?> props);

	abstract TcpClientConf configuration();

	Method[] getMandatoryPropsAccessors() {
		return TcpClientConf.getMandatoryPropsAccessors();
	}

	final ISessionListener listener() {
		return m_listener;
	}

	final void connect() {
		@SuppressWarnings("resource")
		TcpChannel channel = new TcpChannel(this);
		channel.connect(configuration().connectTimeoutInSeconds());
	}

	final void connect(Object attachment) {
		@SuppressWarnings("resource")
		TcpChannel channel = new TcpChannel(this);
		channel.attach(attachment);
		channel.connect(configuration().connectTimeoutInSeconds());
	}

	final boolean cancelReadTimeout(IChannel channel) {
		return channel.cancelTimeout();
	}

	final void scheduleReadTimeout(IChannel channel, int timeout) {
		channel.scheduleReadTimeout(timeout);
	}

	private void updateFilters(TcpChannelConf oldConf, TcpChannelConf newConf) {
		String[] newNames = newConf == null ? StrUtil.getEmptyStringArray() : newConf.filters();
		String[] oldNames = StrUtil.getEmptyStringArray();
		IFilterManager fm = m_fm;
		if (oldConf == null)
			m_filters = fm.getFilters(oldNames);
		else
			oldNames = oldConf.filters();

		if (Arrays.equals(newNames, oldNames))
			return;

		m_filters = fm.getFilters(newNames);
		fm.ungetFilters(oldNames);
	}
}
