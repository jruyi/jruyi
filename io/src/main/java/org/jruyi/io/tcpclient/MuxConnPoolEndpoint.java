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
import java.util.concurrent.ConcurrentHashMap;

import org.jruyi.common.IIdentifiable;
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

@Component(name = "jruyi.io.tcpclient.mux.connpool", //
configurationPolicy = ConfigurationPolicy.REQUIRE, //
service = { IEndpoint.class }, //
xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class MuxConnPoolEndpoint extends SessionListener<IIdentifiable<?>, IIdentifiable<?>> implements
		IConsumer, IEndpoint {

	private static final Logger c_logger = LoggerFactory.getLogger(ConnPoolEndpoint.class);

	private ComponentFactory m_cf;
	private ComponentInstance m_connPool;
	private ISessionService<IIdentifiable<?>, IIdentifiable<?>> m_ss;
	private IProducer m_producer;

	private ConcurrentHashMap<Object, IMessage> m_messages;

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
		final IIdentifiable<?> attachment;
		try {
			attachment = (IIdentifiable<?>) message.attachment();
		} catch (ClassCastException e) {
			c_logger.error(StrUtil.join("Attachment must be of IIdentifiable", StrUtil.getLineSeparator(), message), e);
			message.close();
			return;
		}

		if (attachment == null) {
			c_logger.warn(StrUtil.join(this, " consumes a null message: ", message));
			message.close();
			return;
		}
		m_messages.put(attachment, message);
		m_ss.write(null, attachment);
	}

	@Override
	public void onMessageSent(ISession session, IIdentifiable<?> outMsg) {
		final IMessage message = m_messages.remove(outMsg);
		session.deposit(outMsg.id(), message);
	}

	@Override
	public void onMessageReceived(ISession session, IIdentifiable<?> inMsg) {
		final IMessage message = (IMessage) session.withdraw(inMsg.id());
		message.attach(inMsg);
		m_producer.send(message);
	}

	@Override
	public void onSessionException(ISession session, Throwable t) {
		Object msg = session.detach();
		if (msg == null)
			c_logger.error(StrUtil.join(session, " got an error"), t);
		else {
			msg = m_messages.remove(msg);
			c_logger.error(StrUtil.join(session, " got an error: ", StrUtil.getLineSeparator(), msg), t);

			if (msg instanceof AutoCloseable) {
				try {
					((AutoCloseable) msg).close();
				} catch (Throwable e) {
					c_logger.error(StrUtil.join(session, "Failed to close: ", StrUtil.getLineSeparator(), msg), e);
				}
			}
		}
	}

	@Override
	public void onSessionConnectTimedOut(ISession session) {
		final Object msg = m_messages.remove(session.detach());
		c_logger.warn(StrUtil.join(session, ": CONNECT_TIMEOUT ", StrUtil.getLineSeparator(), msg));

		if (msg instanceof AutoCloseable) {
			try {
				((AutoCloseable) msg).close();
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(session, "Failed to close message: ", msg), t);
			}
		}
	}

	@Override
	public void onSessionReadTimedOut(ISession session, IIdentifiable<?> outMsg) {
		final Object msg = session.withdraw(outMsg.id());
		c_logger.warn(StrUtil.join(session, ": READ_TIMEOUT ", StrUtil.getLineSeparator(), msg));

		if (msg instanceof AutoCloseable) {
			try {
				((AutoCloseable) msg).close();
			} catch (Throwable t) {
				c_logger.error(StrUtil.join(session, "Failed to close message: ", StrUtil.getLineSeparator(), msg), t);
			}
		}
	}

	@Reference(name = "connPool", //
	target = "(" + ComponentConstants.COMPONENT_NAME + "=" + IoConstants.CN_TCPCLIENT_CONNPOOL_FACTORY + ")")
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
		final ComponentInstance connPool = m_cf.newInstance(normalizeConfiguration(properties));
		@SuppressWarnings("unchecked")
		final ISessionService<IIdentifiable<?>, IIdentifiable<?>> ss =
				(ISessionService<IIdentifiable<?>, IIdentifiable<?>>) connPool.getInstance();
		ss.setSessionListener(this);
		ss.start();
		m_messages = new ConcurrentHashMap<>(initialCapacityOfMessageMap(properties));
		m_connPool = connPool;
		m_ss = ss;
	}

	protected void deactivate() {
		m_connPool.dispose();
	}

	private static Properties normalizeConfiguration(Map<String, ?> properties) {
		final Properties conf = new Properties(properties);
		conf.put(IoConstants.SERVICE_ID, properties.get(MeConstants.EP_ID));
		return conf;
	}

	private static int initialCapacityOfMessageMap(Map<String, ?> properties) {
		Integer minPoolSize = (Integer) properties.get("minPoolSize");
		if (minPoolSize == null || minPoolSize < 0)
			minPoolSize = 5;
		Integer maxPoolSize = (Integer) properties.get("maxPoolSize");
		if (maxPoolSize == null || maxPoolSize < minPoolSize)
			maxPoolSize = minPoolSize;
		return maxPoolSize;
	}
}
