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
import org.jruyi.core.ITcpClientConfiguration;

abstract class TcpClientConfiguration implements ITcpClientConfiguration {

	private final Map<String, Object> m_properties;

	TcpClientConfiguration(Map<String, Object> properties) {
		m_properties = properties;
	}

	@Override
	public String host() {
		return (String) m_properties.get("addr");
	}

	@Override
	public int port() {
		return (int) m_properties.get("port");
	}

	@Override
	public long throttle() {
		final Object v = m_properties.get("throttle");
		return v == null ? 0L : (Long) v;
	}

	@Override
	public TcpClientConfiguration throttle(long throttle) {
		if (throttle < -1L)
			throw new IllegalArgumentException(StrUtil.join("Illegal throttle: ", throttle, " >= -1"));
		m_properties.put("throttle", throttle);
		return this;
	}

	@Override
	public int connectTimeoutInSeconds() {
		final Object v = m_properties.get("connectTimeoutInSeconds");
		return v == null ? 6 : (Integer) v;
	}

	@Override
	public TcpClientConfiguration connectTimeoutInSeconds(int connectTimeoutInSeconds) {
		if (connectTimeoutInSeconds < 0)
			throw new IllegalArgumentException(
					StrUtil.join("Illegal connectTimeoutInSeconds: ", connectTimeoutInSeconds, " >= 0"));
		m_properties.put("connectTimeoutInSeconds", connectTimeoutInSeconds);
		return this;
	}

	@Override
	public int readTimeoutInSeconds() {
		final Object v = m_properties.get("readTimeoutInSeconds");
		return v == null ? 30 : (Integer) v;
	}

	@Override
	public TcpClientConfiguration readTimeoutInSeconds(int readTimeoutInSeconds) {
		if (readTimeoutInSeconds < -1)
			throw new IllegalArgumentException(
					StrUtil.join("Illegal readTimeoutInSeconds: ", readTimeoutInSeconds, " >= -1"));
		m_properties.put("readTimeoutInSeconds", readTimeoutInSeconds);
		return this;
	}

	@Override
	public boolean reuseAddr() {
		final Object v = m_properties.get("reuseAddr");
		return v == null ? false : (Boolean) v;
	}

	@Override
	public TcpClientConfiguration reuseAddr(boolean reuseAddr) {
		m_properties.put("reuseAddr", reuseAddr);
		return this;
	}

	@Override
	public Boolean keepAlive() {
		return (Boolean) m_properties.get("keepAlive");
	}

	@Override
	public TcpClientConfiguration keepAlive(Boolean keepAlive) {
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
	public TcpClientConfiguration soLinger(Integer soLinger) {
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
	public Integer recvBufSize() {
		return (Integer) m_properties.get("recvBufSize");
	}

	@Override
	public TcpClientConfiguration recvBufSize(Integer recvBufSize) {
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
	public Integer sendBufSize() {
		return (Integer) m_properties.get("sendBufSize");
	}

	@Override
	public TcpClientConfiguration sendBufSize(Integer sendBufSize) {
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
	public TcpClientConfiguration tcpNoDelay(Boolean tcpNoDelay) {
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
	public TcpClientConfiguration trafficClass(Integer trafficClass) {
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
	public TcpClientConfiguration oobInline(Boolean oobInline) {
		if (oobInline == null)
			m_properties.remove("oobInline");
		else
			m_properties.put("oobInline", oobInline);
		return this;
	}

	@Override
	public int[] performancePreferences() {
		return (int[]) m_properties.get("performancePreferences");
	}

	@Override
	public TcpClientConfiguration performancePreferences(int connectionTime, int latency, int bandwidth) {
		m_properties.put("performancePreferences", new int[] { connectionTime, latency, bandwidth });
		return this;
	}

	@Override
	public TcpClientConfiguration unsetPerformancePreferences() {
		m_properties.remove("performancePreferences");
		return this;
	}

	protected Map<String, Object> properties() {
		return m_properties;
	}
}
