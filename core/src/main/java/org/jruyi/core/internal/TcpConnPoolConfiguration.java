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
import org.jruyi.core.ITcpConnPoolConfiguration;

abstract class TcpConnPoolConfiguration extends TcpClientConfiguration implements ITcpConnPoolConfiguration {

	public TcpConnPoolConfiguration(Map<String, Object> properties) {
		super(properties);
	}

	@Override
	public int idleTimeoutInSeconds() {
		final Object v = properties().get("idleTimeoutInSeconds");
		return v == null ? 60 : (Integer) v;
	}

	@Override
	public TcpConnPoolConfiguration idleTimeoutInSeconds(int idleTimeoutInSeconds) {
		if (idleTimeoutInSeconds < -1)
			throw new IllegalArgumentException(
					StrUtil.join("Illegal idleTimeoutInSeconds: ", idleTimeoutInSeconds, " >= -1"));
		if (idleTimeoutInSeconds == -1 && allowsCoreConnectionTimeout())
			throw new IllegalArgumentException(StrUtil.join("Illegal idleTimeoutInSeconds: ", idleTimeoutInSeconds,
					" > -1 when allowsCoreConnectionTimeout"));
		properties().put("idleTimeoutInSeconds", idleTimeoutInSeconds);
		return this;
	}

	@Override
	public int corePoolSize() {
		final Object v = properties().get("corePoolSize");
		return v == null ? 0 : (Integer) v;
	}

	@Override
	public TcpConnPoolConfiguration corePoolSize(int corePoolSize) {
		if (corePoolSize < 0 || corePoolSize > 65535)
			throw new IllegalArgumentException(StrUtil.join("Illegal corePoolSize: 0 <= ", corePoolSize, " < 65536"));
		properties().put("corePoolSize", corePoolSize);
		return this;
	}

	@Override
	public int maxPoolSize() {
		final Object v = properties().get("maxPoolSize");
		return v == null ? Math.max(10, corePoolSize()) : (Integer) v;
	}

	@Override
	public TcpConnPoolConfiguration maxPoolSize(int maxPoolSize) {
		if (maxPoolSize < 1 || maxPoolSize > 65535)
			throw new IllegalArgumentException(StrUtil.join("Illegal maxPoolSize: 0 < ", maxPoolSize, " < 65536"));
		properties().put("maxPoolSize", maxPoolSize);
		return this;
	}

	@Override
	public boolean allowsCoreConnectionTimeout() {
		final Object v = properties().get("allowsCoreConnectionTimeout");
		return v == null ? false : (Boolean) v;
	}

	@Override
	public TcpConnPoolConfiguration allowsCoreConnectionTimeout(boolean allowsCoreConnectionTimeout) {
		if (allowsCoreConnectionTimeout && idleTimeoutInSeconds() < 0)
			throw new IllegalArgumentException(
					StrUtil.join("Illegal allowsCoreConnectionTimeout: must be false when idleTimeoutInSeconds() < 0"));
		properties().put("allowsCoreConnectionTimeout", allowsCoreConnectionTimeout);
		return this;
	}
}
