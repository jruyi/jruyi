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

import java.util.Map;

final class ConnPoolConf extends TcpClientConf {

	private static final int MAX_POOL_SIZE = 0xFFFF;

	private int m_corePoolSize;
	private int m_maxPoolSize;
	private int m_idleTimeoutInSeconds;
	private boolean m_allowsCoreConnectionTimeout;

	@Override
	public void initialize(Map<String, ?> properties) {
		super.initialize(properties);

		corePoolSize((Integer) properties.get("corePoolSize"));
		maxPoolSize((Integer) properties.get("maxPoolSize"));
		idleTimeoutInSeconds((Integer) properties.get("idleTimeoutInSeconds"));
		allowsCoreConnectionTimeout((Boolean) properties.get("allowsCoreConnectionTimeout"));

		initialCapacityOfChannelMap(maxPoolSize());
	}

	public int corePoolSize() {
		return m_corePoolSize;
	}

	public void corePoolSize(Integer corePoolSize) {
		m_corePoolSize = corePoolSize == null || corePoolSize < 0 || corePoolSize > MAX_POOL_SIZE ? 0 : corePoolSize;
	}

	public int maxPoolSize() {
		return m_maxPoolSize;
	}

	public void maxPoolSize(Integer maxPoolSize) {
		final Integer corePoolSize = corePoolSize();
		if (maxPoolSize == null)
			maxPoolSize = 10;
		if (maxPoolSize < corePoolSize)
			maxPoolSize = corePoolSize;
		if (maxPoolSize > MAX_POOL_SIZE)
			maxPoolSize = MAX_POOL_SIZE;
		m_maxPoolSize = maxPoolSize;
	}

	public boolean allowsCoreConnectionTimeout() {
		return m_allowsCoreConnectionTimeout;
	}

	public void allowsCoreConnectionTimeout(Boolean allowsCoreConnectionTimeout) {
		m_allowsCoreConnectionTimeout = allowsCoreConnectionTimeout == null ? false : allowsCoreConnectionTimeout;
	}

	public int idleTimeoutInSeconds() {
		return m_idleTimeoutInSeconds;
	}

	public void idleTimeoutInSeconds(Integer idleTimeoutInSeconds) {
		m_idleTimeoutInSeconds = idleTimeoutInSeconds == null ? 60 : idleTimeoutInSeconds;
	}
}
