/**
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
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.jruyi.common.IService;
import org.jruyi.common.StrUtil;
import org.jruyi.io.ISession;
import org.jruyi.io.ISessionListener;
import org.jruyi.io.IoConstants;
import org.jruyi.io.channel.IChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service(IService.class)
@Component(name = IoConstants.CN_TCPCLIENT_SHORTCONN_FACTORY, factory = "tcpclient.shortconn", createPid = false, specVersion = "1.1.0")
public final class ShortConn extends AbstractTcpClient {

	private static final Logger c_logger = LoggerFactory
			.getLogger(ShortConn.class);
	private static final Method[] EMTPY_MANDATORY_PROPS = new Method[0];
	private TcpClientConf m_conf;

	@Override
	public void onChannelOpened(IChannel channel) {
		super.onChannelOpened(channel);
		channel.write(channel.detach());
	}

	@Override
	public void onMessageSent(IChannel channel, Object msg) {
		final ISessionListener listener = listener();
		if (listener != null) {
			try {
				listener.onMessageSent(channel, msg);
			} catch (Throwable t) {
				c_logger.error(
						StrUtil.buildString(channel, " Unexpected Error: "), t);
			}
		}
		int timeout = m_conf.readTimeout();
		if (timeout > 0)
			channel.scheduleReadTimeout(timeout);
		else if (timeout == 0)
			channel.close();
	}

	@Override
	public void onMessageReceived(IChannel channel, Object msg) {

		// if false, channel has timed out.
		if (channel.cancelTimeout()) {
			channel.close();
			final ISessionListener listener = listener();
			if (listener != null) {
				try {
					listener.onMessageReceived(channel, msg);
				} catch (Throwable t) {
					c_logger.error(
							StrUtil.buildString(channel, " Unexpected Error: "),
							t);
				}
			}
		} else if (msg instanceof Closeable) {
			try {
				((Closeable) msg).close();
			} catch (Throwable t) {
				c_logger.error(StrUtil.buildString(channel,
						"Failed to close message: ",
						StrUtil.getLineSeparator(), msg), t);
			}
		}
	}

	@Override
	public void onChannelException(IChannel channel, Throwable t) {
		channel.close();
		final ISessionListener listener = listener();
		if (listener != null) {
			try {
				listener.onSessionException(channel, t);
			} catch (Throwable e) {
				c_logger.error(
						StrUtil.buildString(channel, " Unexpected Error: "), e);
			}
		}
	}

	@Override
	public void onChannelIdleTimedOut(IChannel channel) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onChannelConnectTimedOut(IChannel channel) {
		channel.close();
		final ISessionListener listener = listener();
		if (listener != null) {
			try {
				listener.onSessionConnectTimedOut(channel);
			} catch (Throwable t) {
				c_logger.error(
						StrUtil.buildString(channel, " Unexpected Error: "), t);
			}
		}
	}

	@Override
	public void onChannelReadTimedOut(IChannel channel) {
		channel.close();
		final ISessionListener listener = listener();
		if (listener != null) {
			try {
				listener.onSessionReadTimedOut(channel);
			} catch (Throwable t) {
				c_logger.error(
						StrUtil.buildString(channel, " Unexpected Error: "), t);
			}
		}
	}

	@Override
	public void write(ISession session, Object msg) {
		connect(msg);
	}

	@Override
	public void startInternal() {
		c_logger.info(StrUtil.buildString("Starting ", this, "..."));

		super.startInternal();

		c_logger.info(StrUtil.buildString(this, " started"));
	}

	@Override
	public void stopInternal() {
		c_logger.info(StrUtil.buildString("Stopping ", this, "..."));

		super.stopInternal();

		c_logger.info(StrUtil.buildString(this, " stopped"));
	}

	@Override
	TcpClientConf configuration() {
		return m_conf;
	}

	@Override
	Method[] getMandatoryPropsAccessors() {
		return EMTPY_MANDATORY_PROPS;
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
