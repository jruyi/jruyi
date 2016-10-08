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

import java.util.Map;

import org.jruyi.common.IService;
import org.jruyi.common.ITimerAdmin;
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
public final class TcpClient<I, O> extends AbstractTcpClient<I, O> {

	private static final Logger c_logger = LoggerFactory.getLogger(TcpClient.class);

	private TcpClientConf m_conf;

	@Override
	public void openSession() {
		connect();
	}

	@Override
	public void beforeSendMessage(IChannel channel, O outMsg) {
		final int timeout = m_conf.readTimeoutInSeconds();
		if (timeout > 0)
			scheduleReadTimeout(channel, timeout);
	}

	@Override
	public void onMessageSent(IChannel channel, O outMsg) {
		((TcpClientChannel) channel).attachRequest(outMsg);
		final ISessionListener<I, O> listener = listener();
		if (listener != null)
			listener.onMessageSent(channel, outMsg);
	}

	@Override
	public void onMessageReceived(IChannel channel, I inMsg) {
		if (!cancelReadTimeout(channel)) { // channel has timed out
			if (inMsg instanceof AutoCloseable) {
				try {
					((AutoCloseable) inMsg).close();
				} catch (Throwable t) {
					c_logger.error(
							StrUtil.join(channel, " failed to close message: ", StrUtil.getLineSeparator(), inMsg), t);
				}
			}
			return;
		}

		((TcpClientChannel) channel).attachRequest(null);
		final ISessionListener<I, O> listener = listener();
		if (listener != null)
			listener.onMessageReceived(channel, inMsg);
	}

	@Override
	public void onChannelOpened(IChannel channel) {
		super.onChannelOpened(channel);

		final ISessionListener<I, O> listener = listener();
		if (listener != null)
			listener.onSessionOpened(channel);
	}

	@Override
	public void onChannelClosed(IChannel channel) {
		super.onChannelClosed(channel);

		final ISessionListener<I, O> listener = listener();
		if (listener != null) {
			try {
				listener.onSessionClosed(channel);
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(channel, ", unexpected error: "), t);
			}
		}
	}

	@Override
	public void onChannelConnectTimedOut(IChannel channel) {
		final ISessionListener<I, O> listener = listener();
		if (listener != null)
			listener.onSessionConnectTimedOut(channel);
	}

	@Override
	public void onChannelReadTimedOut(IChannel channel) {
		@SuppressWarnings("unchecked")
		final O outMsg = (O) ((TcpClientChannel) channel).detachRequest();
		final ISessionListener<I, O> listener = listener();
		if (listener != null)
			listener.onSessionReadTimedOut(channel, outMsg);
	}

	@Override
	public void onChannelException(IChannel channel, Throwable throwable) {
		try {
			final ISessionListener<I, O> listener = listener();
			if (listener != null)
				listener.onSessionException(channel, throwable);
		} catch (Throwable t) {
			c_logger.error(StrUtil.join(channel, ", unexpected error: "), t);
		} finally {
			channel.close();
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
	public void setBufferFactory(IBufferFactory bf) {
		super.setBufferFactory(bf);
	}

	@Reference(name = "channelAdmin")
	@Override
	public void setChannelAdmin(IChannelAdmin cm) {
		super.setChannelAdmin(cm);
	}

	@Reference(name = "filterManager")
	@Override
	public void setFilterManager(IFilterManager fm) {
		super.setFilterManager(fm);
	}

	@Reference(name = "timerAdmin", policy = ReferencePolicy.DYNAMIC)
	@Override
	public void setTimerAdmin(ITimerAdmin ta) {
		super.setTimerAdmin(ta);
	}

	@Override
	TcpClientConf configuration() {
		return m_conf;
	}

	@Override
	void configuration(TcpClientConf conf) {
		m_conf = conf;
	}

	@Override
	TcpClientConf createConf(Map<String, ?> props) {
		final TcpClientConf conf = new TcpClientConf();
		conf.initialize(props);
		return conf;
	}
}
