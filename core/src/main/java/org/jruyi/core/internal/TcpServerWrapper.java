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
import org.jruyi.core.ITcpServerConfiguration;
import org.jruyi.io.ISession;
import org.jruyi.io.ISessionListener;
import org.jruyi.io.IoConstants;
import org.jruyi.io.tcpserver.TcpServer;

final class TcpServerWrapper<I, O> implements ITcpServerConfiguration, INioService<I, O, ITcpServerConfiguration> {

	private final Map<String, Object> m_properties;
	private final TcpServer<I, O> m_tcpServer = new TcpServer<>();
	private final FilterChain m_filterChain = new FilterChain();
	private IBufferFactory m_bf = RuyiCoreProvider.getInstance().defaultBufferFactory();
	private boolean m_started;

	public TcpServerWrapper(Map<String, Object> properties) {
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
	public Integer backlog() {
		return (Integer) m_properties.get("backlog");
	}

	@Override
	public boolean reuseAddr() {
		final Object v = m_properties.get("reuseAddr");
		return v == null || (boolean) v;
	}

	@Override
	public Integer recvBufSize() {
		return (Integer) m_properties.get("recvBufSize");
	}

	@Override
	public int[] performancePreferences() {
		return (int[]) m_properties.get("performancePreferences");
	}

	@Override
	public long throttle() {
		final Object v = m_properties.get("throttle");
		return v == null ? 0L : (long) v;
	}

	@Override
	public ITcpServerConfiguration throttle(long throttle) {
		if (throttle < -1L)
			throw new IllegalArgumentException(StrUtil.join("Illegal throttle: ", throttle, " >= -1"));
		m_properties.put("throttle", throttle);
		return this;
	}

	@Override
	public int sessionIdleTimeoutInSeconds() {
		final Object v = m_properties.get("sessionIdleTimeoutInSeconds");
		return v == null ? 0 : (int) v;
	}

	@Override
	public ITcpServerConfiguration sessionIdleTimeoutInSeconds(int sessionIdleTimeoutInSeconds) {
		if (sessionIdleTimeoutInSeconds < -1)
			throw new IllegalArgumentException(
					StrUtil.join("Illegal sessionIdleTimeoutInSeconds: ", sessionIdleTimeoutInSeconds, " >= -1"));
		m_properties.put("sessionIdleTimeoutInSeconds", sessionIdleTimeoutInSeconds);
		return this;
	}

	@Override
	public Boolean keepAlive() {
		return (Boolean) m_properties.get("keepAlive");
	}

	@Override
	public ITcpServerConfiguration keepAlive(Boolean keepAlive) {
		if (keepAlive == null)
			m_properties.remove("keepAlive");
		else
			m_properties.put("keepAlive", keepAlive);
		return this;
	}

	@Override
	public Integer soLinger() {
		return (Integer) m_properties.get("soLinger");
	}

	@Override
	public ITcpServerConfiguration soLinger(Integer soLinger) {
		if (soLinger == null)
			m_properties.remove("soLinger");
		else {
			if (soLinger < 0)
				throw new IllegalArgumentException(StrUtil.join("Illegal soLinger: ", soLinger, " >= 0"));
			m_properties.put("soLinger", soLinger);
		}
		return this;
	}

	@Override
	public Integer sendBufSize() {
		return (Integer) m_properties.get("sendBufSize");
	}

	@Override
	public ITcpServerConfiguration sendBufSize(Integer sendBufSize) {
		if (sendBufSize == null)
			m_properties.remove("sendBufSize");
		else {
			if (sendBufSize < 1)
				throw new IllegalArgumentException(StrUtil.join("Illegal sendBufSize: ", sendBufSize, " > 0"));
			m_properties.put("sendBufSize", sendBufSize);
		}
		return this;
	}

	@Override
	public Boolean tcpNoDelay() {
		return (Boolean) m_properties.get("tcpNoDelay");
	}

	@Override
	public ITcpServerConfiguration tcpNoDelay(Boolean tcpNoDelay) {
		if (tcpNoDelay == null)
			m_properties.remove("tcpNoDelay");
		else
			m_properties.put("tcpNoDelay", tcpNoDelay);
		return this;
	}

	@Override
	public Integer trafficClass() {
		return (Integer) m_properties.get("trafficClass");
	}

	@Override
	public ITcpServerConfiguration trafficClass(Integer trafficClass) {
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
	public Boolean oobInline() {
		return (Boolean) m_properties.get("oobInline");
	}

	@Override
	public ITcpServerConfiguration oobInline(Boolean oobInline) {
		if (oobInline == null)
			m_properties.remove("oobInline");
		else
			m_properties.put("oobInline", oobInline);
		return this;
	}

	@Override
	public ITcpServerConfiguration configuration() {
		return this;
	}

	@Override
	public synchronized void apply() throws Throwable {
		if (m_started)
			m_tcpServer.update(m_properties);
	}

	@Override
	public IBufferFactory bufferFactory() {
		return m_bf;
	}

	@Override
	public TcpServerWrapper<I, O> bufferFactory(IBufferFactory bufferFactory) {
		if (bufferFactory == null)
			throw new NullPointerException("bufferFactory cannot be null");
		m_bf = bufferFactory;
		m_tcpServer.setBufferFactory(bufferFactory instanceof BufferFactoryWrapper
				? ((BufferFactoryWrapper) bufferFactory).unwrap() : bufferFactory);
		return this;
	}

	@Override
	public FilterChain filterChain() {
		return m_filterChain;
	}

	@Override
	public TcpServerWrapper<I, O> sessionListener(ISessionListener<I, O> listener) {
		m_tcpServer.setSessionListener(listener);
		return this;
	}

	@Override
	public void openSession() {
		m_tcpServer.openSession();
	}

	@Override
	public void closeSession(ISession session) {
		m_tcpServer.closeSession(session);
	}

	@Override
	public void write(ISession session, O msg) {
		m_tcpServer.write(session, msg);
	}

	@Override
	public synchronized void start() throws Throwable {
		if (m_started)
			return;

		final RuyiCoreProvider ruyiCore = RuyiCoreProvider.getInstance();
		ruyiCore.start();

		final TcpAcceptorWrapper tcpAcceptor = ruyiCore.tcpAcceptor();
		tcpAcceptor.start();

		final TcpServer<I, O> tcpServer = m_tcpServer;
		final IBufferFactory bf = m_bf;
		tcpServer.setBufferFactory(bf instanceof BufferFactoryWrapper ? ((BufferFactoryWrapper) bf).unwrap() : bf);
		tcpServer.setTimerAdmin(ruyiCore.getTimerAdmin());
		tcpServer.setChannelAdmin(ruyiCore.channelAdmin());
		tcpServer.setTcpAcceptor(tcpAcceptor.unwrap());
		tcpServer.setFilterManager(m_filterChain);

		tcpServer.activate(m_properties);
		tcpServer.start();

		m_started = true;
	}

	@Override
	public synchronized void stop() {
		if (!m_started)
			return;

		m_started = false;

		final RuyiCoreProvider ruyiCore = RuyiCoreProvider.getInstance();
		final TcpAcceptorWrapper tcpAcceptor = ruyiCore.tcpAcceptor();
		final TcpServer<I, O> tcpServer = m_tcpServer;
		tcpServer.deactivate();

		tcpServer.unsetFilterManager(m_filterChain);
		tcpServer.unsetTcpAcceptor(tcpAcceptor.unwrap());
		tcpServer.unsetChannelAdmin(ruyiCore.channelAdmin());
		tcpServer.unsetTimerAdmin(ruyiCore.getTimerAdmin());
		final IBufferFactory bf = m_bf;
		tcpServer.unsetBufferFactory(bf instanceof BufferFactoryWrapper ? ((BufferFactoryWrapper) bf).unwrap() : bf);

		tcpAcceptor.stop();
		ruyiCore.stop();
	}
}
