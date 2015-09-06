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

import org.jruyi.common.IIdentifiable;
import org.jruyi.core.IBufferFactory;
import org.jruyi.core.INioService;
import org.jruyi.core.ITcpConnPoolConfiguration;
import org.jruyi.io.ISession;
import org.jruyi.io.ISessionListener;
import org.jruyi.io.IoConstants;
import org.jruyi.io.tcpclient.MuxConnPool;

final class TcpMuxConnPoolWrapper<I extends IIdentifiable<?>, O extends IIdentifiable<?>>
		extends TcpConnPoolConfiguration implements INioService<I, O, ITcpConnPoolConfiguration> {

	private final MuxConnPool<I, O> m_muxConnPool = new MuxConnPool<>();
	private final FilterChain m_filterChain = new FilterChain();
	private IBufferFactory m_bf = RuyiCoreProvider.getInstance().defaultBufferFactory();
	private boolean m_started;

	public TcpMuxConnPoolWrapper(Map<String, Object> properties) {
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
		m_muxConnPool.update(properties());
	}

	@Override
	public IBufferFactory bufferFactory() {
		return m_bf;
	}

	@Override
	public TcpMuxConnPoolWrapper<I, O> bufferFactory(IBufferFactory bufferFactory) {
		if (bufferFactory == null)
			throw new NullPointerException("bufferFactory cannot be null");
		m_bf = bufferFactory;
		m_muxConnPool.setBufferFactory(bufferFactory instanceof BufferFactoryWrapper
				? ((BufferFactoryWrapper) bufferFactory).unwrap() : bufferFactory);
		return this;
	}

	@Override
	public FilterChain filterChain() {
		return m_filterChain;
	}

	@Override
	public TcpMuxConnPoolWrapper<I, O> sessionListener(ISessionListener<I, O> listener) {
		m_muxConnPool.setSessionListener(listener);
		return this;
	}

	@Override
	public void openSession() {
		m_muxConnPool.openSession();
	}

	@Override
	public void closeSession(ISession session) {
		m_muxConnPool.closeSession(session);
	}

	@Override
	public void write(ISession session, O msg) {
		m_muxConnPool.write(session, msg);
	}

	@Override
	public void start() throws Throwable {
		if (m_started)
			return;

		final RuyiCoreProvider ruyiCore = RuyiCoreProvider.getInstance();
		ruyiCore.start();

		final MuxConnPool<I, O> muxConnPool = m_muxConnPool;
		final IBufferFactory bf = m_bf;
		muxConnPool.setBufferFactory(bf instanceof BufferFactoryWrapper ? ((BufferFactoryWrapper) bf).unwrap() : bf);
		muxConnPool.setTimerAdmin(ruyiCore.getTimerAdmin());
		muxConnPool.setChannelAdmin(ruyiCore.channelAdmin());
		muxConnPool.setFilterManager(m_filterChain);

		muxConnPool.activate(properties());
		muxConnPool.start();

		m_started = true;
	}

	@Override
	public void stop() {
		if (!m_started)
			return;
		m_started = false;

		final RuyiCoreProvider ruyiCore = RuyiCoreProvider.getInstance();
		final MuxConnPool<I, O> muxConnPool = m_muxConnPool;
		muxConnPool.deactivate();

		muxConnPool.unsetFilterManager(m_filterChain);
		muxConnPool.unsetChannelAdmin(ruyiCore.channelAdmin());
		muxConnPool.unsetTimerAdmin(ruyiCore.getTimerAdmin());
		final IBufferFactory bf = m_bf;
		muxConnPool.unsetBufferFactory(bf instanceof BufferFactoryWrapper ? ((BufferFactoryWrapper) bf).unwrap() : bf);

		ruyiCore.stop();
	}
}
