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

package org.jruyi.me.endpoint;

import java.util.Map;

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
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

@Component(name = "jruyi.me.endpoint.udpclient", //
configurationPolicy = ConfigurationPolicy.REQUIRE, //
service = { IEndpoint.class }, //
xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class UdpClientEndpoint extends SessionListener<Object, Object> implements IConsumer, IEndpoint {

	private ComponentFactory m_cf;
	private ComponentInstance m_udpClient;
	private ISessionService<Object, Object> m_ss;
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
		try {
			final Object outMsg = message.detach();
			m_ss.write(null, outMsg);
		} finally {
			message.close();
		}
	}

	@Override
	public void onMessageReceived(ISession session, Object msg) {
		IProducer producer = m_producer;
		IMessage message = producer.createMessage();
		message.attach(msg);
		producer.send(message);
	}

	@Reference(name = "udpClient", //
	target = "(" + ComponentConstants.COMPONENT_NAME + "=" + IoConstants.CN_UDPCLIENT_FACTORY + ")")
	void setUdpClient(ComponentFactory cf) {
		m_cf = cf;
	}

	void unsetUdpClient(ComponentFactory cf) {
		m_cf = null;
	}

	@Modified
	void modified(Map<String, ?> properties) throws Exception {
		m_ss.update(normalizeConfiguration(properties));
	}

	void activate(Map<String, ?> properties) throws Exception {
		final ComponentInstance udpClient = m_cf.newInstance(normalizeConfiguration(properties));
		@SuppressWarnings("unchecked")
		final ISessionService<Object, Object> ss = (ISessionService<Object, Object>) udpClient.getInstance();
		ss.setSessionListener(this);
		ss.start();
		m_udpClient = udpClient;
		m_ss = ss;
	}

	void deactivate() {
		m_udpClient.dispose();
		m_udpClient = null;
		m_ss = null;
	}

	private static Properties normalizeConfiguration(Map<String, ?> properties) {
		final Properties conf = new Properties(properties);
		conf.put(IoConstants.SERVICE_ID, properties.get(MeConstants.EP_ID));
		return conf;
	}
}
