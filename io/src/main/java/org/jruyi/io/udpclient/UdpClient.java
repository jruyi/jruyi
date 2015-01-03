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
package org.jruyi.io.udpclient;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
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

@Component(name = IoConstants.CN_UDPCLIENT_FACTORY, //
factory = "udpclient", //
service = { IService.class }, //
xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class UdpClient extends Service implements IChannelService,
		ISessionService {

	private static final Logger c_logger = LoggerFactory
			.getLogger(UdpClient.class);
	private String m_caption;
	private Configuration m_conf;

	private IBufferFactory m_bf;
	private IChannelAdmin m_ca;
	private IFilterManager m_fm;

	private IFilter<?, ?>[] m_filters;
	private boolean m_closed = true;
	private ISessionListener m_listener;
	private volatile IChannel m_channel;
	private final ReentrantLock m_channelLock = new ReentrantLock();
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
	public void onChannelOpened(IChannel channel) {
		c_logger.debug("{}: OPENED", channel);

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
			m_channel = channel;
		} catch (Throwable t) {
			c_logger.error(StrUtil.join(channel, " Unexpected Error: "), t);
		} finally {
			readLock.unlock();
		}

		final ISessionListener listener = m_listener;
		if (listener != null)
			listener.onSessionOpened(channel);
	}

	@Override
	public void onChannelClosed(IChannel channel) {
		m_channel = null;

		final ISessionListener listener = m_listener;
		try {
			if (listener != null)
				listener.onSessionClosed(channel);
		} catch (Throwable t) {
			c_logger.error(StrUtil.join(channel, " Unexpected Error: "), t);
		}
	}

	@Override
	public void onMessageReceived(IChannel channel, Object msg) {
		final ISessionListener listener = m_listener;
		try {
			if (listener != null)
				listener.onMessageReceived(channel, msg);
		} catch (Throwable t) {
			c_logger.error(StrUtil.join(channel, " Unexpected Error: "), t);
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
	public void onChannelException(IChannel channel, Throwable t) {
		try {
			c_logger.error(
					StrUtil.join(this, " got an error on sending/receiving"), t);

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
		throw new UnsupportedOperationException("Not supported yet.");
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
	public void setSessionListener(ISessionListener listener) {
		m_listener = listener;
	}

	@Override
	public void openSession() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void closeSession(ISession session) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void write(ISession session, Object msg) {
		final IChannel channel = getChannel();
		if (channel != null) {
			channel.write(msg);
			return;
		}

		c_logger.warn(StrUtil.join(this, " failed to send(channel closed): ",
				StrUtil.getLineSeparator(), msg));

		if (msg instanceof Closeable) {
			try {
				((Closeable) msg).close();
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(this, " failed to close message: ",
						StrUtil.getLineSeparator(), msg), t);
			}
		}
	}

	@Override
	public String toString() {
		return m_caption;
	}

	@Override
	protected boolean updateInternal(Map<String, ?> properties)
			throws Exception {
		final Configuration oldConf = updateConf(properties);
		final Configuration newConf = m_conf;
		updateFilters(oldConf, newConf);

		return oldConf.isMandatoryChanged(newConf,
				Configuration.getMandatoryPropsAccessors());
	}

	@Override
	protected void startInternal() throws Exception {
		c_logger.info(StrUtil.join("Starting ", this, "..."));

		m_closed = false;

		c_logger.info(StrUtil.join(this, " started"));
	}

	@Override
	protected void stopInternal() {
		c_logger.info(StrUtil.join("Stopping ", this, "..."));

		final WriteLock writeLock = m_lock.writeLock();
		writeLock.lock();
		try {
			m_closed = true;
		} finally {
			writeLock.unlock();
		}

		final IChannel channel = m_channel;
		if (channel != null)
			channel.close(); // m_channel will be set to null in method onClosed

		c_logger.info(StrUtil.join(this, " stopped"));
	}

	@Reference(name = "buffer", policy = ReferencePolicy.DYNAMIC)
	protected synchronized void setBufferFactory(IBufferFactory bf) {
		m_bf = bf;
	}

	protected synchronized void unsetBufferFactory(IBufferFactory bf) {
		if (m_bf == bf)
			m_bf = bf;
	}

	@Reference(name = "channelAdmin")
	protected void setChannelAdmin(IChannelAdmin cm) {
		m_ca = cm;
	}

	protected void unsetChannelAdmin(IChannelAdmin cm) {
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
		m_caption = StrUtil.join("UdpClient[", id, "]");

		updateFilters(updateConf(properties), m_conf);
	}

	protected void deactivate() {
		stop();

		updateFilters(updateConf(null), null);
	}

	private Configuration updateConf(Map<String, ?> props) {
		final Configuration conf = m_conf;
		if (props == null)
			m_conf = null;
		else {
			final Configuration newConf = new Configuration();
			newConf.initialize(props);
			m_conf = newConf;
		}

		return conf;
	}

	private IChannel getChannel() {
		IChannel channel = m_channel;
		if (channel != null)
			return channel;

		final ReadLock readLock = m_lock.readLock();
		if (!readLock.tryLock())
			return null;

		try {
			final ReentrantLock channelLock = m_channelLock;
			channelLock.lock();
			try {
				channel = m_channel;
				if (channel == null) {
					channel = new UdpClientChannel(this);
					channel.connect(-1);
				}
			} finally {
				channelLock.unlock();
			}
		} finally {
			readLock.unlock();
		}

		return channel;
	}

	private void updateFilters(Configuration oldConf, Configuration newConf) {
		final String[] newNames = newConf == null ? StrUtil
				.getEmptyStringArray() : newConf.filters();
		String[] oldNames = StrUtil.getEmptyStringArray();
		final IFilterManager fm = m_fm;
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
