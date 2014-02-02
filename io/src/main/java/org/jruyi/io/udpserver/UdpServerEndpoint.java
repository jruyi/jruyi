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

import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.jruyi.common.Properties;
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

@Service(IEndpoint.class)
@Component(name = "jruyi.io.udpserver", policy = ConfigurationPolicy.REQUIRE, createPid = false)
@Reference(name = UdpServerEndpoint.UDPSERVER, referenceInterface = ComponentFactory.class, target = "(component.name="
		+ IoConstants.CN_UDPSERVER_FACTORY + ")", strategy = ReferenceStrategy.LOOKUP)
@Property(name = MeConstants.EP_LAZY, boolValue = false)
public final class UdpServerEndpoint extends SessionListener implements
		IConsumer, IEndpoint {

	public static final String UDPSERVER = "udpServer";
	private ComponentInstance m_udpServer;
	private ISessionService m_ss;
	private IProducer m_producer;

	@Override
	public void producer(IProducer producer) {
		m_producer = producer;
		if (producer != null) {
			try {
				m_ss.start();
			} catch (Throwable t) {
				// Ignore
			}
		}
	}

	@Override
	public IConsumer consumer() {
		return this;
	}

	@Override
	public void onMessageReceived(ISession session, Object msg) {
		final IProducer producer = m_producer;
		final IMessage message = producer.createMessage();
		message.deposit(this, session);
		message.attach(msg);

		producer.send(message);
	}

	@Override
	public void onMessage(IMessage message) {
		try {
			Object msg = message.detach();
			ISession session = (ISession) message.withdraw(this);
			m_ss.write(session, msg);
		} finally {
			message.close();
		}
	}

	@Modified
	protected void modified(Map<String, ?> properties) throws Exception {
		m_ss.update(normalizeConfiguration(properties));
	}

	protected void activate(ComponentContext context, Map<String, ?> properties)
			throws Exception {
		ComponentFactory factory = (ComponentFactory) context
				.locateService(UDPSERVER);
		ComponentInstance udpServer = factory
				.newInstance(normalizeConfiguration(properties));
		ISessionService ss = (ISessionService) udpServer.getInstance();
		ss.setSessionListener(this);
		m_udpServer = udpServer;
		m_ss = ss;
	}

	protected void deactivate() {
		m_udpServer.dispose();
		m_udpServer = null;
		m_ss = null;
	}

	private static Properties normalizeConfiguration(Map<String, ?> properties) {
		Properties conf = new Properties(properties);
		conf.put(IoConstants.SERVICE_ID, properties.get(MeConstants.EP_ID));
		return conf;
	}
}
