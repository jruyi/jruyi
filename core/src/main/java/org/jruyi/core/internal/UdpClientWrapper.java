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
import org.jruyi.core.INioService;
import org.jruyi.core.IUdpClientConfiguration;
import org.jruyi.io.ISession;
import org.jruyi.io.ISessionListener;
import org.jruyi.io.IoConstants;
import org.jruyi.io.udpclient.UdpClient;

final class UdpClientWrapper<I, O> implements IUdpClientConfiguration, INioService<I, O, IUdpClientConfiguration> {

	private final Map<String, Object> m_properties;
	private final UdpClient<I, O> m_udpClient = new UdpClient<>();
	private final FilterChain m_filterChain = new FilterChain();
	private IBufferFactory m_bf = RuyiCoreProvider.getInstance().defaultBufferFactory();
	private boolean m_started;

	public UdpClientWrapper(Map<String, Object> properties) {
		m_properties = properties;
	}

	@Override
	public String id() {
		return (String) m_properties.get(IoConstants.SERVICE_ID);
	}

	@Override
	public String host() {
		return (String) m_properties.get("addr");
	}

	@Override
	public int port() {
		return (int) m_properties.get("port");
	}

	@Override
	public Integer trafficClass() {
		return (Integer) m_properties.get("trafficClass");
	}

	@Override
	public IUdpClientConfiguration trafficClass(Integer trafficClass) {
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
	public IUdpClientConfiguration broadcast(boolean broadcast) {
		m_properties.put("broadcast", broadcast);
		return this;
	}

	@Override
	public IUdpClientConfiguration configuration() {
		return this;
	}

	@Override
	public void apply() throws Throwable {
		m_udpClient.update(m_properties);
	}

	@Override
	public IBufferFactory bufferFactory() {
		return m_bf;
	}

	@Override
	public UdpClientWrapper<I, O> bufferFactory(IBufferFactory bufferFactory) {
		if (bufferFactory == null)
			throw new NullPointerException("bufferFactory cannot be null");
		m_bf = bufferFactory;
		m_udpClient.setBufferFactory(bufferFactory instanceof BufferFactoryWrapper
				? ((BufferFactoryWrapper) bufferFactory).unwrap() : bufferFactory);
		return this;
	}

	@Override
	public FilterChain filterChain() {
		return m_filterChain;
	}

	@Override
	public UdpClientWrapper<I, O> sessionListener(ISessionListener<I, O> listener) {
		m_udpClient.setSessionListener(listener);
		return this;
	}

	@Override
	public void openSession() {
		m_udpClient.openSession();
	}

	@Override
	public void closeSession(ISession session) {
		m_udpClient.closeSession(session);
	}

	@Override
	public void write(ISession session, O msg) {
		m_udpClient.write(session, msg);
	}

	@Override
	public synchronized void start() throws Throwable {
		if (m_started)
			return;

		final RuyiCoreProvider ruyiCore = RuyiCoreProvider.getInstance();
		ruyiCore.start();

		final UdpClient<I, O> udpClient = m_udpClient;
		final IBufferFactory bf = m_bf;
		udpClient.setBufferFactory(bf instanceof BufferFactoryWrapper ? ((BufferFactoryWrapper) bf).unwrap() : bf);
		udpClient.setChannelAdmin(ruyiCore.channelAdmin());
		udpClient.setFilterManager(m_filterChain);
		udpClient.activate(m_properties);
		udpClient.start();

		m_started = true;
	}

	@Override
	public synchronized void stop() {
		if (!m_started)
			return;

		m_started = false;

		final RuyiCoreProvider ruyiCore = RuyiCoreProvider.getInstance();
		final UdpClient<I, O> udpClient = m_udpClient;
		udpClient.deactivate();

		udpClient.unsetFilterManager(m_filterChain);
		udpClient.unsetChannelAdmin(ruyiCore.channelAdmin());
		final IBufferFactory bf = m_bf;
		udpClient.unsetBufferFactory(bf instanceof BufferFactoryWrapper ? ((BufferFactoryWrapper) bf).unwrap() : bf);

		ruyiCore.stop();
	}
}
