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

import org.jruyi.common.timer.TimerAdmin;
import org.jruyi.core.IChannelAdminConfiguration;
import org.jruyi.core.IFileKeyStoreBuilder;
import org.jruyi.core.ISslFilterBuilder;
import org.jruyi.core.ITcpClientBuilder;
import org.jruyi.core.ITcpServerBuilder;
import org.jruyi.core.ITextLineFilterBuilder;
import org.jruyi.core.IUdpClientBuilder;
import org.jruyi.core.IUdpServerBuilder;
import org.jruyi.core.RuyiCore;
import org.jruyi.io.IFilter;
import org.jruyi.io.channel.ChannelAdmin;
import org.jruyi.io.msglog.MsgLogFilter;

public final class RuyiCoreProvider implements RuyiCore.IRuyiCore {

	private static final RuyiCoreProvider INST = new RuyiCoreProvider();

	private final ChannelAdminConfiguration m_caConf = new ChannelAdminConfiguration();

	private final BufferFactoryWrapper m_bf = new BufferFactoryWrapper();
	private final SchedulerWrapper m_scheduler = new SchedulerWrapper();
	private final TimerAdmin m_ta = new TimerAdmin();
	private final TcpAcceptorWrapper m_tcpAcceptor = new TcpAcceptorWrapper();
	private final ChannelAdmin m_ca = new ChannelAdmin();

	private int m_count;

	static final class MsgLogFilterHolder {
		static final IFilter<?, ?> MSGLOG = new MsgLogFilter();
	}

	private RuyiCoreProvider() {
	}

	public static RuyiCoreProvider getInstance() {
		return INST;
	}

	public RuyiCore.IRuyiCore ruyiCore() {
		return this;
	}

	synchronized void start() throws Throwable {
		if (m_count == 0) {
			final SchedulerWrapper scheduler = m_scheduler;
			scheduler.start();

			m_ta.setScheduler(scheduler.unwrap());

			final ChannelAdmin ca = m_ca;
			try {
				ca.activate(m_caConf.properties());
			} catch (Throwable t) {
				scheduler.stop();
				throw t;
			}

			m_tcpAcceptor.setChannelAdmin(ca);

			m_bf.start();
		}
		++m_count;
	}

	synchronized void stop() {
		if (m_count == 0)
			return;

		if (--m_count == 0) {
			final ChannelAdmin ca = m_ca;
			m_tcpAcceptor.unsetChannelAdmin(ca);

			ca.deactivate();

			final SchedulerWrapper scheduler = m_scheduler;
			m_ta.unsetScheduler(scheduler.unwrap());
			scheduler.stop();
		}
	}

	TcpAcceptorWrapper tcpAcceptor() {
		return m_tcpAcceptor;
	}

	ChannelAdmin channelAdmin() {
		return m_ca;
	}

	@Override
	public IChannelAdminConfiguration channelAdminConfiguration() {
		return m_caConf;
	}

	@Override
	public BufferFactoryWrapper defaultBufferFactory() {
		return m_bf;
	}

	@Override
	public BufferFactoryWrapper newBufferFactory(String name) {
		return new BufferFactoryWrapper(name);
	}

	@Override
	public SchedulerWrapper getScheduler() {
		return m_scheduler;
	}

	@Override
	public TimerAdmin getTimerAdmin() {
		return m_ta;
	}

	@Override
	public IFilter<?, ?> getMsgLogFilter() {
		return MsgLogFilterHolder.MSGLOG;
	}

	@Override
	public ITextLineFilterBuilder newTextLineFilterBuilder() {
		return new TextLineFilterBuilder();
	}

	@Override
	public ISslFilterBuilder newSslFilterBuilder() {
		return new SslFilterBuilder();
	}

	@Override
	public IFileKeyStoreBuilder newFileKeyStoreBuilder() {
		return new FileKeyStoreBuilder();
	}

	@Override
	public ITcpServerBuilder newTcpServerBuilder() {
		return new TcpServerBuilder();
	}

	@Override
	public ITcpClientBuilder newTcpClientBuilder() {
		return new TcpClientBuilder();
	}

	@Override
	public IUdpServerBuilder newUdpServerBuilder() {
		return new UdpServerBuilder();
	}

	@Override
	public IUdpClientBuilder newUdpClientBuilder() {
		return new UdpClientBuilder();
	}
}
