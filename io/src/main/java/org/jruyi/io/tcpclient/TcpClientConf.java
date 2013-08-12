/**
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
package org.jruyi.io.tcpclient;

import java.lang.reflect.Method;
import java.util.Map;
import org.jruyi.io.tcp.TcpChannelConf;

class TcpClientConf extends TcpChannelConf {

	private static final String[] M_PROPS = {"addr", "port"};
	private static final Method[] c_mProps;
	private Integer m_connectTimeout;
	private Integer m_readTimeout;

	static {
		c_mProps = new Method[M_PROPS.length];
		Class<TcpClientConf> clazz = TcpClientConf.class;
		try {
			for (int i = 0; i < M_PROPS.length; ++i)
				c_mProps[i] = clazz.getMethod(M_PROPS[i]);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public static Method[] getMandatoryPropsAccessors() {
		return c_mProps;
	}

	@Override
	public void initialize(Map<String, ?> properties) {
		super.initialize(properties);

		addr((String) properties.get("addr"));
		connectTimeout((Integer) properties.get("connectTimeout"));
		readTimeout((Integer) properties.get("readTimeout"));
	}

	public final String addr() {
		return ip();
	}

	public final void addr(String addr) {
		ip(addr);
	}

	public final Integer connectTimeout() {
		return m_connectTimeout;
	}

	public final void connectTimeout(Integer connectTimeout) {
		m_connectTimeout = connectTimeout == null ? 10 : connectTimeout;
	}

	public final Integer readTimeout() {
		return m_readTimeout;
	}

	public final void readTimeout(Integer readTimeout) {
		m_readTimeout = readTimeout == null ? 10 : readTimeout;
	}
}
