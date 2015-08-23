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
import org.jruyi.core.ITcpConnPoolConfiguration;
import org.jruyi.io.ISession;
import org.jruyi.io.ISessionListener;
import org.jruyi.io.IoConstants;
import org.jruyi.io.tcpclient.ConnPool;

final class TcpConnPoolWrapper<I, O> extends TcpConnPoolConfiguration
		implements INioService<I, O, ITcpConnPoolConfiguration> {

	private final ConnPool<I, O> m_connPool = new ConnPool<>();
	private final FilterChain m_filterChain = new FilterChain();
	private IBufferFactory m_bf = RuyiCoreProvider.getInstance().defaultBufferFactory();
	private boolean m_started;

	public TcpConnPoolWrapper(Map<String, Object> properties) {
		super(properties);
	}

	@Override
	public String id() {
		return (String) properties().get(IoConstants.SERVICE_ID);
	}

	@Override
	public ITcpConnPoolConfiguration configuration() {
		return this;
	}

	@Override
	public void apply() throws Throwable {
		m_connPool.update(properties());
	}

	@Override
	public IBufferFactory bufferFactory() {
		return m_bf;
	}

	@Override
	public TcpConnPoolWrapper<I, O> bufferFactory(IBufferFactory bufferFactory) {
		if (bufferFactory == null)
			throw new NullPointerException("bufferFactory cannot be null");
		m_bf = bufferFactory;
		m_connPool.setBufferFactory(bufferFactory instanceof BufferFactoryWrapper
				? ((BufferFactoryWrapper) bufferFactory).unwrap() : bufferFactory);
		return this;
	}

	@Override
	public FilterChain filterChain() {
		return m_filterChain;
	}

	@Override
	public TcpConnPoolWrapper<I, O> sessionListener(ISessionListener<I, O> listener) {
		m_connPool.setSessionListener(listener);
		return this;
	}

	@Override
	public void openSession() {
		m_connPool.openSession();
	}

	@Override
	public void closeSession(ISession session) {
		m_connPool.closeSession(session);
	}

	@Override
	public void write(ISession session, O msg) {
		m_connPool.write(session, msg);
	}

	@Override
	public synchronized void start() throws Throwable {
		if (m_started)
			return;

		final RuyiCoreProvider ruyiCore = RuyiCoreProvider.getInstance();
		ruyiCore.start();

		final ConnPool<I, O> connPool = m_connPool;
		final IBufferFactory bf = m_bf;
		connPool.setBufferFactory(bf instanceof BufferFactoryWrapper ? ((BufferFactoryWrapper) bf).unwrap() : bf);
		connPool.setChannelAdmin(ruyiCore.channelAdmin());
		connPool.setFilterManager(m_filterChain);
		connPool.activate(properties());
		connPool.start();

		m_started = true;
	}

	@Override
	public synchronized void stop() {
		if (!m_started)
			return;
		m_started = false;

		final RuyiCoreProvider ruyiCore = RuyiCoreProvider.getInstance();
		final ConnPool<I, O> connPool = m_connPool;
		connPool.deactivate();

		connPool.unsetFilterManager(m_filterChain);
		connPool.unsetChannelAdmin(ruyiCore.channelAdmin());
		final IBufferFactory bf = m_bf;
		connPool.unsetBufferFactory(bf instanceof BufferFactoryWrapper ? ((BufferFactoryWrapper) bf).unwrap() : bf);

		ruyiCore.stop();
	}
}
