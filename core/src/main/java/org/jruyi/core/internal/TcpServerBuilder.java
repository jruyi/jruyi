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
import org.jruyi.core.ITcpServerBuilder;
import org.jruyi.core.ITcpServerConfiguration;
import org.jruyi.io.IoConstants;

final class TcpServerBuilder implements ITcpServerBuilder {

	private final Map<String, Object> m_properties = new HashMap<>(16);

	@Override
	public TcpServerBuilder serviceId(String serviceId) {
		if (serviceId != null && !(serviceId = serviceId.trim()).isEmpty())
			m_properties.put(IoConstants.SERVICE_ID, serviceId);
		return this;
	}

	@Override
	public TcpServerBuilder bindAddr(String bindAddr) {
		if (bindAddr == null || (bindAddr = bindAddr.trim()).isEmpty())
			m_properties.remove("bindAddr");
		else
			m_properties.put("bindAddr", bindAddr);
		return this;
	}

	@Override
	public TcpServerBuilder port(int port) {
		if (port < 0 || port > 65535)
			throw new IllegalArgumentException(StrUtil.join("Illegal port: 0 <= ", port, " <= 65535"));
		m_properties.put("port", port);
		return this;
	}

	@Override
	public TcpServerBuilder backlog(Integer backlog) {
		if (backlog == null)
			m_properties.remove("backlog");
		else {
			if (backlog < 1)
				throw new IllegalArgumentException(StrUtil.join("Illegal backlog: ", backlog, " >= 1"));
			m_properties.put("backlog", backlog);
		}
		return this;
	}

	@Override
	public TcpServerBuilder initCapacityOfChannelMap(int initCapacityOfChannelMap) {
		if (initCapacityOfChannelMap < 4)
			throw new IllegalArgumentException(
					StrUtil.join("Illegal initCapacityOfChannelMap: ", initCapacityOfChannelMap, " >= 4"));
		m_properties.put("initCapacityOfChannelMap", initCapacityOfChannelMap);
		return this;
	}

	@Override
	public TcpServerBuilder recvBufSize(Integer recvBufSize) {
		if (recvBufSize == null)
			m_properties.remove("recvBufSize");
		else {
			if (recvBufSize < 1)
				throw new IllegalArgumentException(StrUtil.join("Illegal recvBufSize: ", recvBufSize, " > 0"));
			m_properties.put("recvBufSize", recvBufSize);
		}
		return this;
	}

	@Override
	public TcpServerBuilder reuseAddr(boolean reuseAddr) {
		m_properties.put("reuseAddr", reuseAddr);
		return this;
	}

	@Override
	public TcpServerBuilder performancePreferences(int connectionTime, int latency, int bandwidth) {
		m_properties.put("performancePreferences", new int[] { connectionTime, latency, bandwidth });
		return this;
	}

	@Override
	public <I, O> INioService<I, O, ? extends ITcpServerConfiguration> build() {
		final Map<String, Object> properties = m_properties;
		if (!properties.containsKey("port"))
			throw new RuntimeException("Missing port");
		return new TcpServerWrapper<>(properties);
	}
}
