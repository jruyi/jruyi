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
import java.util.Arrays;
import java.util.Map;

import org.jruyi.common.Properties;
import org.jruyi.common.StrUtil;
import org.jruyi.io.ISession;
import org.jruyi.io.ISessionService;
import org.jruyi.io.IoConstants;
import org.jruyi.io.SessionListener;
import org.jruyi.me.IConsumer;
import org.jruyi.me.IEndpoint;
import org.jruyi.me.IMessage;
import org.jruyi.me.IProducer;
import org.jruyi.me.MeConstants;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "jruyi.io.tcpclient.connpool", //
configurationPolicy = ConfigurationPolicy.REQUIRE, //
service = { IEndpoint.class }, //
xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class ConnPoolEndpoint extends SessionListener implements
		IConsumer, IEndpoint {

	private static final Logger c_logger = LoggerFactory
			.getLogger(ConnPoolEndpoint.class);

	private ComponentFactory m_cf;
	private ComponentInstance m_connPool;
	private ISessionService m_ss;
	private IProducer m_producer;

	@Override
	public void producer(IProducer producer) {
		m_producer = producer;
	}

	@Override
	public IConsumer consumer() {
		return this;
	}

	@Override
	public void onMessage(IMessage message) {
		Object attachment = message.attachment();
		if (attachment == null) {
			c_logger.warn(StrUtil.join(this, " consumes a null message: ",
					message));

			message.close();
			return;
		}

		m_ss.write(null, message);
	}

	@Override
	public void onMessageReceived(ISession session, Object msg) {
		m_producer.send((IMessage) msg);
	}

	@Override
	public void onSessionException(ISession session, Throwable t) {
		Object msg = session.withdraw(IoConstants.FID_TCPCLIENT);
		if (msg == null)
			msg = session.detach();

		if (msg == null)
			c_logger.error(StrUtil.join(session, " got an error"), t);
		else {
			c_logger.error(
					StrUtil.join(session, " got an error: ",
							StrUtil.getLineSeparator(), msg), t);

			if (msg instanceof Closeable) {
				try {
					((Closeable) msg).close();
				} catch (Throwable e) {
					c_logger.error(
							StrUtil.join(session, "Failed to close: ",
									StrUtil.getLineSeparator(), msg), e);
				}
			}
		}
	}

	@Override
	public void onSessionConnectTimedOut(ISession session) {
		Object msg = session.detach();
		c_logger.warn(StrUtil.join(session, ": CONNECT_TIMEOUT, ",
				StrUtil.getLineSeparator(), msg));

		if (msg instanceof Closeable) {
			try {
				((Closeable) msg).close();
			} catch (Throwable t) {
				c_logger.error(
						StrUtil.join(session, "Failed to close message: ", msg),
						t);
			}
		}
	}

	@Override
	public void onSessionReadTimedOut(ISession session) {
		Object msg = session.withdraw(IoConstants.FID_TCPCLIENT);
		c_logger.warn(StrUtil.join(session, ": READ_TIMEOUT, ",
				StrUtil.getLineSeparator(), msg));

		if (msg instanceof Closeable) {
			try {
				((Closeable) msg).close();
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(session,
						"Failed to close message: ",
						StrUtil.getLineSeparator(), msg), t);
			}
		}
	}

	@Reference(name = "connPool", target = "("
			+ ComponentConstants.COMPONENT_NAME + "="
			+ IoConstants.CN_TCPCLIENT_CONNPOOL_FACTORY + ")")
	protected void setConnPool(ComponentFactory cf) {
		m_cf = cf;
	}

	protected void unsetConnPool(ComponentFactory cf) {
		m_cf = null;
	}

	@Modified
	protected void modified(Map<String, ?> properties) throws Exception {
		m_ss.update(normalizeConfiguration(properties));
	}

	protected void activate(Map<String, ?> properties) throws Exception {
		final ComponentInstance connPool = m_cf
				.newInstance(normalizeConfiguration(properties));
		final ISessionService ss = (ISessionService) connPool.getInstance();
		ss.setSessionListener(this);
		try {
			ss.start();
		} catch (Throwable t) {
			// ignore
		}
		m_connPool = connPool;
		m_ss = ss;
	}

	protected void deactivate() {
		m_connPool.dispose();
	}

	private static Properties normalizeConfiguration(Map<String, ?> properties) {
		Properties conf = new Properties(properties);
		conf.put(IoConstants.SERVICE_ID, properties.get(MeConstants.EP_ID));
		String[] filters = (String[]) properties.get("filters");
		int n;
		if (filters == null || (n = filters.length) < 1)
			filters = new String[] { IoConstants.FID_TCPCLIENT };
		else {
			filters = Arrays.copyOf(filters, n + 1);
			filters[n] = IoConstants.FID_TCPCLIENT;
		}
		conf.put("filters", filters);
		return conf;
	}
}
