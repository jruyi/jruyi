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

package org.jruyi.io.tcpserver;

import java.lang.reflect.Method;
import java.util.Map;

import org.jruyi.io.tcp.TcpChannelConf;

public final class Configuration extends TcpChannelConf {

	private static final String[] M_PROPS = {
		"bindAddr", "port", "backlog", "reuseAddr", "recvBufSize", "performancePreferences" };
	private static final Method[] c_mProps;
	private Integer m_backlog;
	private Integer m_sessionIdleTimeoutInSeconds;
	private Integer m_initCapacityOfChannelMap;

	static {
		c_mProps = new Method[M_PROPS.length];
		Class<Configuration> clazz = Configuration.class;
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

		bindAddr((String) properties.get("bindAddr"));
		backlog((Integer) properties.get("backlog"));
		sessionIdleTimeoutInSeconds((Integer) properties.get("sessionIdleTimeoutInSeconds"));
		initCapacityOfChannelMap((Integer) properties.get("initCapacityOfChannelMap"));
	}

	public Integer backlog() {
		return m_backlog;
	}

	public void backlog(Integer backlog) {
		m_backlog = backlog;
	}

	public String bindAddr() {
		return ip();
	}

	public void bindAddr(String bindAddr) {
		ip(bindAddr);
	}

	public Integer sessionIdleTimeoutInSeconds() {
		return m_sessionIdleTimeoutInSeconds;
	}

	public void sessionIdleTimeoutInSeconds(Integer sessionIdleTimeoutInSeconds) {
		m_sessionIdleTimeoutInSeconds = sessionIdleTimeoutInSeconds == null ? 300 : sessionIdleTimeoutInSeconds;
	}

	public Integer initCapacityOfChannelMap() {
		return m_initCapacityOfChannelMap;
	}

	public void initCapacityOfChannelMap(Integer initCapacityOfChannelMap) {
		m_initCapacityOfChannelMap = initCapacityOfChannelMap == null ? 2048 : initCapacityOfChannelMap;
	}

	public final boolean isMandatoryChanged(Configuration newConf) throws Exception {
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
