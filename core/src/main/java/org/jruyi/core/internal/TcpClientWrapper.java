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

import org.jruyi.core.IBufferFactory;
import org.jruyi.core.INioService;
import org.jruyi.core.ITcpClientConfiguration;
import org.jruyi.io.ISession;
import org.jruyi.io.ISessionListener;
import org.jruyi.io.IoConstants;
import org.jruyi.io.tcpclient.AbstractTcpClient;

final class TcpClientWrapper<I, O> extends TcpClientConfiguration
		implements INioService<I, O, ITcpClientConfiguration> {

	private final AbstractTcpClient<I, O> m_tcpClient;
	private final FilterChain m_filterChain = new FilterChain();
	private IBufferFactory m_bf = RuyiCoreProvider.getInstance().defaultBufferFactory();
	private boolean m_started;

	public TcpClientWrapper(Map<String, Object> properties, AbstractTcpClient<I, O> tcpClient) {
		super(properties);
		m_tcpClient = tcpClient;
	}

	@Override
	public String id() {
		return (String) properties().get(IoConstants.SERVICE_ID);
	}

	@Override
	public ITcpClientConfiguration configuration() {
		return this;
	}

	@Override
	public synchronized void apply() throws Throwable {
		if (m_started)
			m_tcpClient.update(properties());
	}

	@Override
	public IBufferFactory bufferFactory() {
		return m_bf;
	}

	@Override
	public TcpClientWrapper<I, O> bufferFactory(IBufferFactory bufferFactory) {
		if (bufferFactory == null)
			throw new NullPointerException("bufferFactory cannot be null");
		m_bf = bufferFactory;
		m_tcpClient.setBufferFactory(bufferFactory instanceof BufferFactoryWrapper
				? ((BufferFactoryWrapper) bufferFactory).unwrap() : bufferFactory);
		return this;
	}

	@Override
	public FilterChain filterChain() {
		return m_filterChain;
	}

	@Override
	public TcpClientWrapper<I, O> sessionListener(ISessionListener<I, O> listener) {
		m_tcpClient.setSessionListener(listener);
		return this;
	}

	@Override
	public void openSession() {
		m_tcpClient.openSession();
	}

	@Override
	public void closeSession(ISession session) {
		m_tcpClient.closeSession(session);
	}

	@Override
	public void write(ISession session, O msg) {
		m_tcpClient.write(session, msg);
	}

	@Override
	public synchronized void start() throws Throwable {
		if (m_started)
			return;

		final RuyiCoreProvider ruyiCore = RuyiCoreProvider.getInstance();
		ruyiCore.start();

		final AbstractTcpClient<I, O> tcpClient = m_tcpClient;
		final IBufferFactory bf = m_bf;
		tcpClient.setBufferFactory(bf instanceof BufferFactoryWrapper ? ((BufferFactoryWrapper) bf).unwrap() : bf);
		tcpClient.setChannelAdmin(ruyiCore.channelAdmin());
		tcpClient.setFilterManager(m_filterChain);

		tcpClient.activate(properties());
		tcpClient.start();
		m_started = true;
	}

	@Override
	public synchronized void stop() {
		if (!m_started)
			return;

		m_started = false;

		final RuyiCoreProvider ruyiCore = RuyiCoreProvider.getInstance();
		final AbstractTcpClient<I, O> tcpClient = m_tcpClient;
		tcpClient.deactivate();

		tcpClient.unsetFilterManager(m_filterChain);
		tcpClient.unsetChannelAdmin(ruyiCore.channelAdmin());
		final IBufferFactory bf = m_bf;
		tcpClient.unsetBufferFactory(bf instanceof BufferFactoryWrapper ? ((BufferFactoryWrapper) bf).unwrap() : bf);

		ruyiCore.stop();
	}

}
