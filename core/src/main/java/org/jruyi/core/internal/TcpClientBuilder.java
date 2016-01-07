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

import java.util.HashMap;
import java.util.Map;

import org.jruyi.common.IIdentifiable;
import org.jruyi.common.StrUtil;
import org.jruyi.core.INioService;
import org.jruyi.core.ITcpClientBuilder;
import org.jruyi.core.ITcpClientConfiguration;
import org.jruyi.core.ITcpConnPoolConfiguration;
import org.jruyi.io.IoConstants;
import org.jruyi.io.tcpclient.AbstractTcpClient;
import org.jruyi.io.tcpclient.ShortConn;
import org.jruyi.io.tcpclient.TcpClient;
import org.jruyi.io.tcpclient.TcpClientMux;

final class TcpClientBuilder implements ITcpClientBuilder {

	private final Map<String, Object> m_properties = new HashMap<>(16);

	@Override
	public TcpClientBuilder serviceId(String serviceId) {
		if (serviceId != null && !(serviceId = serviceId.trim()).isEmpty())
			m_properties.put(IoConstants.SERVICE_ID, serviceId);
		return this;
	}

	@Override
	public TcpClientBuilder host(String host) {
		if (host == null || (host = host.trim()).isEmpty())
			throw new IllegalArgumentException("host cannot be null or empty");
		m_properties.put("addr", host);
		return this;
	}

	@Override
	public TcpClientBuilder port(int port) {
		if (port < 0 || port > 65535)
			throw new IllegalArgumentException(StrUtil.join("Illegal port: 0 <= ", port, " <= 65535"));
		m_properties.put("port", port);
		return this;
	}

	@Override
	public TcpClientBuilder initCapacityOfChannelMap(int initCapacityOfChannelMap) {
		if (initCapacityOfChannelMap < 4)
			throw new IllegalArgumentException(
					StrUtil.join("Illegal initCapacityOfChannelMap: ", initCapacityOfChannelMap, " >= 4"));
		m_properties.put("initCapacityOfChannelMap", initCapacityOfChannelMap);
		return this;
	}

	@Override
	public <I, O> INioService<I, O, ? extends ITcpClientConfiguration> buildConn() {
		return build(new TcpClient<I, O>());
	}

	@Override
	public <I extends IIdentifiable<?>, O extends IIdentifiable<?>> INioService<I, O, ? extends ITcpClientConfiguration> buildMuxConn() {
		return build(new TcpClientMux<I, O>());
	}

	@Override
	public <I, O> INioService<I, O, ? extends ITcpClientConfiguration> buildShortConn() {
		return build(new ShortConn<I, O>());
	}

	@Override
	public <I, O> INioService<I, O, ? extends ITcpConnPoolConfiguration> buildConnPool() {
		final Map<String, Object> properties = m_properties;
		check(properties);
		return new TcpConnPoolWrapper<>(properties);
	}

	@Override
	public <I extends IIdentifiable<?>, O extends IIdentifiable<?>> INioService<I, O, ? extends ITcpConnPoolConfiguration> buildMuxConnPool() {
		final Map<String, Object> properties = m_properties;
		check(properties);
		return new TcpMuxConnPoolWrapper<>(properties);
	}

	private void check(Map<String, ?> properties) {
		if (!properties.containsKey("addr"))
			throw new RuntimeException("Missing host");
		if (!properties.containsKey("port"))
			throw new RuntimeException("Missing port");
	}

	private <I, O> INioService<I, O, ? extends ITcpClientConfiguration> build(AbstractTcpClient<I, O> tcpClient) {
		final Map<String, Object> properties = m_properties;
		check(properties);
		return new TcpClientWrapper<>(properties, tcpClient);
	}
}
