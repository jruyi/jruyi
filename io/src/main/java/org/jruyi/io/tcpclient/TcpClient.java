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
import java.util.Map;

import org.jruyi.common.IService;
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

@Component(name = IoConstants.CN_TCPCLIENT_FACTORY, //
factory = "tcpclient", //
service = { IService.class }, //
xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class TcpClient extends AbstractTcpClient {

	private static final Logger c_logger = LoggerFactory
			.getLogger(TcpClient.class);

	private TcpClientConf m_conf;

	@Override
	public void openSession() {
		connect();
	}

	@Override
	public void onMessageSent(IChannel channel, Object msg) {
		final ISessionListener listener = listener();
		if (listener != null) {
			try {
				listener.onMessageSent(channel, msg);
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(channel, " Unexpected Error: "), t);
			}
		}
		int timeout = m_conf.readTimeoutInSeconds();
		if (timeout > 0)
			scheduleReadTimeout(channel, timeout);
		else if (timeout == 0)
			onChannelReadTimedOut(channel);
	}

	@Override
	public void onMessageReceived(IChannel channel, Object msg) {
		if (!cancelReadTimeout(channel)) { // channel has timed out
			if (msg instanceof Closeable) {
				try {
					((Closeable) msg).close();
				} catch (Throwable t) {
					c_logger.error(
							StrUtil.join("Failed to close message: ",
									StrUtil.getLineSeparator(), msg), t);
				}
			}
			return;
		}

		final ISessionListener listener = listener();
		if (listener != null) {
			try {
				listener.onMessageReceived(channel, msg);
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(channel, " Unexpected Error: "), t);
			}
		}
	}

	@Override
	public void onChannelOpened(IChannel channel) {
		super.onChannelOpened(channel);

		final ISessionListener listener = listener();
		if (listener != null) {
			try {
				listener.onSessionOpened(channel);
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(channel, " Unexpected Error: "), t);
			}
		}
	}

	@Override
	public void onChannelClosed(IChannel channel) {
		super.onChannelClosed(channel);

		final ISessionListener listener = listener();
		if (listener != null) {
			try {
				listener.onSessionClosed(channel);
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(channel, " Unexpected Error: "), t);
			}
		}
	}

	@Override
	public void onChannelConnectTimedOut(IChannel channel) {
		final ISessionListener listener = listener();
		if (listener != null) {
			try {
				listener.onSessionConnectTimedOut(channel);
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(channel, " Unexpected Error: "), t);
			}
		}
	}

	@Override
	public void onChannelReadTimedOut(IChannel channel) {
		final ISessionListener listener = listener();
		if (listener != null) {
			try {
				listener.onSessionReadTimedOut(channel);
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(channel, " Unexpected Error: "), t);
			}
		}
	}

	@Override
	public void onChannelException(IChannel channel, Throwable t) {
		final ISessionListener listener = listener();
		if (listener != null) {
			try {
				listener.onSessionException(channel, t);
			} catch (Throwable e) {
				c_logger.error(StrUtil.join(channel, " Unexpected Error: "), e);
			}
		}
	}

	@Override
	public void startInternal() {
		c_logger.info(StrUtil.join("Starting ", this, "..."));

		super.startInternal();

		c_logger.info(StrUtil.join(this, " started"));
	}

	@Override
	public final void stopInternal() {
		c_logger.info(StrUtil.join("Stopping ", this, "..."));

		super.stopInternal();

		c_logger.info(StrUtil.join(this, " stopped"));
	}

	@Reference(name = "buffer", policy = ReferencePolicy.DYNAMIC)
	@Override
	protected synchronized void setBufferFactory(IBufferFactory bf) {
		super.setBufferFactory(bf);
	}

	@Override
	protected synchronized void unsetBufferFactory(IBufferFactory bf) {
		super.unsetBufferFactory(bf);
	}

	@Reference(name = "channelAdmin")
	@Override
	protected void setChannelAdmin(IChannelAdmin cm) {
		super.setChannelAdmin(cm);
	}

	@Override
	protected void unsetChannelAdmin(IChannelAdmin cm) {
		super.unsetChannelAdmin(cm);
	}

	@Reference(name = "filterManager")
	@Override
	protected void setFilterManager(IFilterManager fm) {
		super.setFilterManager(fm);
	}

	@Override
	protected void unsetFilterManager(IFilterManager fm) {
		super.unsetFilterManager(fm);
	}

	@Override
	TcpClientConf configuration() {
		return m_conf;
	}

	@Override
	TcpClientConf updateConf(Map<String, ?> props) {
		TcpClientConf conf = m_conf;
		if (props == null)
			m_conf = null;
		else {
			TcpClientConf newConf = new TcpClientConf();
			newConf.initialize(props);
			m_conf = newConf;
		}

		return conf;
	}
}
