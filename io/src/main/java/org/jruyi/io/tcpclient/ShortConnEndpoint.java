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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
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
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service(IEndpoint.class)
@Component(name = "jruyi.io.tcpclient.shortconn", policy = ConfigurationPolicy.REQUIRE, createPid = false)
@Reference(name = ShortConnEndpoint.SHORTCONN, referenceInterface = ComponentFactory.class, target = "(component.name="
		+ IoConstants.CN_TCPCLIENT_SHORTCONN_FACTORY + ")", strategy = ReferenceStrategy.LOOKUP)
public final class ShortConnEndpoint extends SessionListener implements
		IConsumer, IEndpoint {

	public static final String SHORTCONN = "shortConn";

	private static final Logger c_logger = LoggerFactory
			.getLogger(ShortConnEndpoint.class);

	private ComponentInstance m_shortConn;
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
				c_logger.error(StrUtil.join(session,
						"Failed to close message: ",
						StrUtil.getLineSeparator(), msg), t);
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

	@Modified
	protected void modified(Map<String, ?> properties) throws Exception {
		m_ss.update(normalizeConfiguration(properties));
	}

	protected void activate(ComponentContext context, Map<String, ?> properties)
			throws Exception {
		ComponentFactory factory = (ComponentFactory) context
				.locateService(SHORTCONN);
		ComponentInstance shortConn = factory
				.newInstance(normalizeConfiguration(properties));
		ISessionService ss = (ISessionService) shortConn.getInstance();
		ss.setSessionListener(this);
		try {
			ss.start();
		} catch (Throwable t) {
			// ignore
		}
		m_shortConn = shortConn;
		m_ss = ss;
	}

	protected void deactivate() {
		m_shortConn.dispose();
	}

	private static Properties normalizeConfiguration(Map<String, ?> properties) {
		Properties conf = new Properties(properties);
		conf.put(IoConstants.SERVICE_ID, properties.get(MeConstants.EP_ID));
		String[] filters = (String[]) properties.get("filters");
		int n = filters.length;
		if (filters == null || n < 1)
			filters = new String[] { IoConstants.FID_TCPCLIENT };
		else {
			filters = Arrays.copyOf(filters, n + 1);
			filters[n] = IoConstants.FID_TCPCLIENT;
		}
		conf.put("filters", filters);
		return conf;
	}
}
