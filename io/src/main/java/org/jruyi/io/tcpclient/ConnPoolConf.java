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

	private Integer m_minPoolSize;
	private Integer m_maxPoolSize;
	private Integer m_idleTimeoutInSeconds;

	@Override
	public void initialize(Map<String, ?> properties) {
		super.initialize(properties);

		minPoolSize((Integer) properties.get("minPoolSize"));
		maxPoolSize((Integer) properties.get("maxPoolSize"));
		idleTimeoutInSeconds((Integer) properties.get("idleTimeoutInSeconds"));
		initialCapacityOfChannelMap(maxPoolSize());
	}

	public Integer minPoolSize() {
		return m_minPoolSize;
	}

	public void minPoolSize(Integer minPoolSize) {
		m_minPoolSize = minPoolSize == null || minPoolSize < 0 || minPoolSize > MAX_POOL_SIZE ? 0 : minPoolSize;
	}

	public Integer maxPoolSize() {
		return m_maxPoolSize;
	}

	public void maxPoolSize(Integer maxPoolSize) {
		final Integer minPoolSize = minPoolSize();
		if (maxPoolSize == null)
			maxPoolSize = 10;
		if (maxPoolSize < minPoolSize)
			maxPoolSize = minPoolSize;
		if (maxPoolSize > MAX_POOL_SIZE)
			maxPoolSize = MAX_POOL_SIZE;
		m_maxPoolSize = maxPoolSize;
	}

	public Integer idleTimeoutInSeconds() {
		return m_idleTimeoutInSeconds;
	}

	public void idleTimeoutInSeconds(Integer idleTimeoutInSeconds) {
		m_idleTimeoutInSeconds = idleTimeoutInSeconds == null ? 60 : idleTimeoutInSeconds;
	}
}
