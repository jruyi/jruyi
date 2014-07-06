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
package org.jruyi.io.udp;

import java.lang.reflect.Method;
import java.util.Map;

import org.jruyi.common.StrUtil;

public class UdpChannelConf {

	private String m_ip;
	private Integer m_port;
	private String[] m_filters;
	private Boolean m_reuseAddr;
	private Integer m_recvBufSize;
	private Integer m_sendBufSize;
	private Boolean m_broadcast;
	private Integer m_trafficClass;

	public void initialize(Map<String, ?> properties) {
		port((Integer) properties.get("port"));
		filters((String[]) properties.get("filters"));
		reuseAddr((Boolean) properties.get("reuseAddr"));
		recvBufSize((Integer) properties.get("recvBufSize"));
		sendBufSize((Integer) properties.get("sendBufSize"));
		broadcast((Boolean) properties.get("broadcast"));
		trafficClass((Integer) properties.get("trafficClass"));
	}

	public final String ip() {
		return m_ip;
	}

	public final void ip(String ip) {
		m_ip = ip;
	}

	public final Integer port() {
		return m_port;
	}

	public final void port(Integer port) {
		m_port = port;
	}

	public final String[] filters() {
		return m_filters;
	}

	public final void filters(String[] filters) {
		m_filters = filters == null ? StrUtil.getEmptyStringArray() : filters;
	}

	public final Boolean reuseAddr() {
		return m_reuseAddr;
	}

	public final void reuseAddr(Boolean reuseAddr) {
		m_reuseAddr = reuseAddr == null ? Boolean.FALSE : reuseAddr;
	}

	public final Integer recvBufSize() {
		return m_recvBufSize;
	}

	public final void recvBufSize(Integer recvBufSize) {
		m_recvBufSize = recvBufSize;
	}

	public final Integer sendBufSize() {
		return m_sendBufSize;
	}

	public final void sendBufSize(Integer sendBufSize) {
		m_sendBufSize = sendBufSize;
	}

	public final Boolean broadcast() {
		return m_broadcast;
	}

	public final void broadcast(Boolean broadcast) {
		m_broadcast = broadcast == null ? Boolean.FALSE : broadcast;
	}

	public final Integer trafficClass() {
		return m_trafficClass;
	}

	public final void trafficClass(Integer trafficClass) {
		m_trafficClass = trafficClass;
	}

	public final boolean isMandatoryChanged(UdpChannelConf newConf,
			Method[] mProps) throws Exception {
		for (Method m : mProps) {
			final Object v1 = m.invoke(this);
			final Object v2 = m.invoke(newConf);
			if (v1 == v2)
				continue;

			if (v1 == null || !v1.equals(v2))
				return true;
		}

		return false;
	}
}
