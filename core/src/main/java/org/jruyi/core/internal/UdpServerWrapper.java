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

package org.jruyi.core.internal;

import java.util.Map;

import org.jruyi.common.StrUtil;
import org.jruyi.core.IBufferFactory;
import org.jruyi.core.IFilterChain;
import org.jruyi.core.INioService;
import org.jruyi.core.IUdpServerConfiguration;
import org.jruyi.io.ISession;
import org.jruyi.io.ISessionListener;
import org.jruyi.io.IoConstants;
import org.jruyi.io.udpserver.UdpServer;

final class UdpServerWrapper<I, O> implements IUdpServerConfiguration, INioService<I, O, IUdpServerConfiguration> {

	private final Map<String, Object> m_properties;
	private UdpServer<I, O> m_udpServer = new UdpServer<>();
	private final FilterChain m_filterChain = new FilterChain();
	private IBufferFactory m_bf = RuyiCoreProvider.getInstance().defaultBufferFactory();
	private boolean m_started;

	public UdpServerWrapper(Map<String, Object> properties) {
		m_properties = properties;
	}

	@Override
	public String id() {
		return (String) m_properties.get(IoConstants.SERVICE_ID);
	}

	@Override
	public String bindAddr() {
		return (String) m_properties.get("bindAddr");
	}

	@Override
	public int port() {
		return (int) m_properties.get("port");
	}

	@Override
	public int sessionIdleTimeoutInSeconds() {
		final Object v = m_properties.get("sessionIdleTimeoutInSeconds");
		return v == null ? 120 : (int) v;
	}

	@Override
	public IUdpServerConfiguration sessionIdleTimeoutInSeconds(int sessionIdleTimeoutInSeconds) {
		if (sessionIdleTimeoutInSeconds < -1)
			throw new IllegalArgumentException(
					StrUtil.join("Illegal sessionIdleTimeoutInSeconds: ", sessionIdleTimeoutInSeconds, " >= -1"));
		m_properties.put("sessionIdleTimeoutInSeconds", sessionIdleTimeoutInSeconds);
		return this;
	}

	@Override
	public Integer trafficClass() {
		return (Integer) m_properties.get("trafficClass");
	}

	@Override
	public IUdpServerConfiguration trafficClass(Integer trafficClass) {
		if (trafficClass == null)
			m_properties.remove("trafficClass");
		else {
			if (trafficClass < 0 || trafficClass > 255)
				throw new IllegalArgumentException(
						StrUtil.join("Illegal trafficClass: 0 <= ", trafficClass, " <= 255"));
			m_properties.put("trafficClass", trafficClass);
		}
		return this;
	}

	@Override
	public boolean broadcast() {
		final Object v = m_properties.get("broadcast");
		return v != null && (boolean) v;
	}

	@Override
	public IUdpServerConfiguration broadcast(boolean broadcast) {
		m_properties.put("broadcast", broadcast);
		return this;
	}

	@Override
	public IUdpServerConfiguration configuration() {
		return this;
	}

	@Override
	public synchronized void apply() throws Throwable {
		if (m_started)
			m_udpServer.update(m_properties);
	}

	@Override
	public IBufferFactory bufferFactory() {
		return m_bf;
	}

	@Override
	public UdpServerWrapper<I, O> bufferFactory(IBufferFactory bufferFactory) {
		if (bufferFactory == null)
			throw new NullPointerException("bufferFactory cannot be null");
		m_bf = bufferFactory;
		m_udpServer.setBufferFactory(bufferFactory instanceof BufferFactoryWrapper
				? ((BufferFactoryWrapper) bufferFactory).unwrap() : bufferFactory);
		return this;
	}

	@Override
	public IFilterChain filterChain() {
		return m_filterChain;
	}

	@Override
	public UdpServerWrapper<I, O> sessionListener(ISessionListener<I, O> listener) {
		m_udpServer.setSessionListener(listener);
		return this;
	}

	@Override
	public void openSession() {
		m_udpServer.openSession();
	}

	@Override
	public void closeSession(ISession session) {
		m_udpServer.closeSession(session);
	}

	@Override
	public void write(ISession session, O msg) {
		m_udpServer.write(session, msg);
	}

	@Override
	public synchronized void start() throws Throwable {
		if (m_started)
			return;

		final RuyiCoreProvider ruyiCore = RuyiCoreProvider.getInstance();
		ruyiCore.start();

		final UdpServer<I, O> udpServer = m_udpServer;
		final IBufferFactory bf = m_bf;
		udpServer.setBufferFactory(bf instanceof BufferFactoryWrapper ? ((BufferFactoryWrapper) bf).unwrap() : bf);
		udpServer.setChannelAdmin(ruyiCore.channelAdmin());
		udpServer.setFilterManager(m_filterChain);

		udpServer.activate(m_properties);
		udpServer.start();

		m_started = true;
	}

	@Override
	public synchronized void stop() {
		if (!m_started)
			return;

		m_started = false;

		final RuyiCoreProvider ruyiCore = RuyiCoreProvider.getInstance();
		final UdpServer<I, O> udpServer = m_udpServer;
		udpServer.deactivate();

		udpServer.unsetFilterManager(m_filterChain);
		udpServer.unsetChannelAdmin(ruyiCore.channelAdmin());
		final IBufferFactory bf = m_bf;
		udpServer.unsetBufferFactory(bf instanceof BufferFactoryWrapper ? ((BufferFactoryWrapper) bf).unwrap() : bf);

		ruyiCore.stop();
	}
}
