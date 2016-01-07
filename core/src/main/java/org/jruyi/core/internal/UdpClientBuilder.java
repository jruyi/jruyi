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

import org.jruyi.common.StrUtil;
import org.jruyi.core.INioService;
import org.jruyi.core.IUdpClientBuilder;
import org.jruyi.core.IUdpClientConfiguration;
import org.jruyi.io.IoConstants;

final class UdpClientBuilder implements IUdpClientBuilder {

	private final Map<String, Object> m_properties = new HashMap<>(8);

	@Override
	public UdpClientBuilder serviceId(String serviceId) {
		if (serviceId != null && !(serviceId = serviceId.trim()).isEmpty())
			m_properties.put(IoConstants.SERVICE_ID, serviceId);
		return this;
	}

	@Override
	public UdpClientBuilder host(String host) {
		if (host == null || (host = host.trim()).isEmpty())
			throw new IllegalArgumentException("host cannot be null or empty");
		m_properties.put("addr", host);
		return this;
	}

	@Override
	public UdpClientBuilder port(int port) {
		if (port < 0 || port > 65535)
			throw new IllegalArgumentException(StrUtil.join("Illegal port: 0 <= ", port, " <= 65535"));
		m_properties.put("port", port);
		return this;
	}

	@Override
	public <I, O> INioService<I, O, ? extends IUdpClientConfiguration> build() {
		final Map<String, Object> properties = m_properties;
		if (!properties.containsKey("addr"))
			throw new RuntimeException("Missing host");
		if (!properties.containsKey("port"))
			throw new RuntimeException("Missing port");
		return new UdpClientWrapper<>(properties);
	}
}
