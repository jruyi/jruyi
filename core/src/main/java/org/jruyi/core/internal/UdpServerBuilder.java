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
import org.jruyi.core.IUdpServerBuilder;
import org.jruyi.core.IUdpServerConfiguration;
import org.jruyi.io.IoConstants;

final class UdpServerBuilder implements IUdpServerBuilder {

	private final Map<String, Object> m_properties = new HashMap<>(8);

	@Override
	public UdpServerBuilder serviceId(String serviceId) {
		if (serviceId != null && !(serviceId = serviceId.trim()).isEmpty())
			m_properties.put(IoConstants.SERVICE_ID, serviceId);
		return this;
	}

	@Override
	public UdpServerBuilder bindAddr(String bindAddr) {
		if (bindAddr == null || (bindAddr = bindAddr.trim()).isEmpty())
			m_properties.remove("bindAddr");
		else
			m_properties.put("bindAddr", bindAddr);
		return this;
	}

	@Override
	public UdpServerBuilder port(int port) {
		if (port < 0 || port > 65535)
			throw new IllegalArgumentException(StrUtil.join("Illegal port: 0 <= ", port, " <= 65535"));
		m_properties.put("port", port);
		return this;
	}

	@Override
	public UdpServerBuilder initCapacityOfChannelMap(int initCapacityOfChannelMap) {
		if (initCapacityOfChannelMap < 4)
			throw new IllegalArgumentException(
					StrUtil.join("Illegal initCapacityOfChannelMap: ", initCapacityOfChannelMap, " >= 4"));
		m_properties.put("initCapacityOfChannelMap", initCapacityOfChannelMap);
		return this;
	}

	@Override
	public <I, O> INioService<I, O, ? extends IUdpServerConfiguration> build() {
		final Map<String, Object> properties = m_properties;
		if (!properties.containsKey("port"))
			throw new RuntimeException("Missing port");
		return new UdpServerWrapper<>(properties);
	}
}
