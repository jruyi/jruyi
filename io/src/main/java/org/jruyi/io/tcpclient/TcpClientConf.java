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

package org.jruyi.io.tcpclient;

import java.lang.reflect.Method;
import java.util.Map;

import org.jruyi.io.tcp.TcpChannelConf;

class TcpClientConf extends TcpChannelConf {

	private static final String[] M_PROPS = {
		"addr", "port" };
	private static final Method[] c_mProps;
	private Integer m_initialCapacityOfChannelMap;
	private Integer m_connectTimeoutInSeconds;
	private Integer m_readTimeoutInSeconds;

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
		initialCapacityOfChannelMap((Integer) properties.get("initialCapacityOfChannelMap"));
		connectTimeoutInSeconds((Integer) properties.get("connectTimeoutInSeconds"));
		readTimeoutInSeconds((Integer) properties.get("readTimeoutInSeconds"));
	}

	public final String addr() {
		return ip();
	}

	public final void addr(String addr) {
		ip(addr);
	}

	public final Integer initialCapacityOfChannelMap() {
		return m_initialCapacityOfChannelMap;
	}

	public final void initialCapacityOfChannelMap(Integer initialCapacityOfChannelMap) {
		m_initialCapacityOfChannelMap = initialCapacityOfChannelMap == null ? 512 : initialCapacityOfChannelMap;
	}

	public final Integer connectTimeoutInSeconds() {
		return m_connectTimeoutInSeconds;
	}

	public final void connectTimeoutInSeconds(Integer connectTimeoutInSeconds) {
		m_connectTimeoutInSeconds = connectTimeoutInSeconds == null ? 6 : connectTimeoutInSeconds;
	}

	public final Integer readTimeoutInSeconds() {
		return m_readTimeoutInSeconds;
	}

	public final void readTimeoutInSeconds(Integer readTimeoutInSeconds) {
		m_readTimeoutInSeconds = readTimeoutInSeconds == null ? 30 : readTimeoutInSeconds;
	}

	public final boolean isMandatoryChanged(TcpClientConf newConf) throws Exception {
		for (Method m : c_mProps) {
			Object v1 = m.invoke(this);
			Object v2 = m.invoke(newConf);
			if (v1 == v2)
				continue;

			if (!(v1 == null ? v2.equals(v1) : v1.equals(v2)))
				return true;
		}

		return false;
	}
}
