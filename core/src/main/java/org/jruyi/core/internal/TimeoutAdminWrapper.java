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
import org.jruyi.core.ITimeoutAdmin;
import org.jruyi.timeoutadmin.ITimeoutNotifier;
import org.jruyi.timeoutadmin.internal.TimeoutAdmin;

final class TimeoutAdminWrapper implements ITimeoutAdmin, ITimeoutAdmin.IConfiguration {

	private final Map<String, Object> m_properties = new HashMap<>(6);
	private final TimeoutAdmin m_ta = new TimeoutAdmin();

	@Override
	public IConfiguration allowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
		m_properties.put("allowCoreThreadTimeOut", allowCoreThreadTimeOut);
		return this;
	}

	@Override
	public IConfiguration corePoolSize(int corePoolSize) {
		if (corePoolSize < 0)
			throw new IllegalArgumentException(StrUtil.join("Illegal corePoolSize: ", corePoolSize, " >= 0"));
		m_properties.put("corePoolSize", corePoolSize);
		return this;
	}

	@Override
	public IConfiguration maxPoolSize(int maxPoolSize) {
		if (maxPoolSize < 1)
			throw new IllegalArgumentException(StrUtil.join("Illegal maxPoolSize: ", maxPoolSize, " > 0"));
		m_properties.put("maxPoolSize", maxPoolSize);
		return this;
	}

	@Override
	public IConfiguration keepAliveTimeInSeconds(int keepAliveTimeInSeconds) {
		if (keepAliveTimeInSeconds < 0)
			throw new IllegalArgumentException(
					StrUtil.join("Illegal keepAliveTimeInSeconds: ", keepAliveTimeInSeconds, " >= 0"));
		m_properties.put("keepAliveTimeInSeconds", keepAliveTimeInSeconds);
		return this;
	}

	@Override
	public IConfiguration queueCapacity(int queueCapacity) {
		m_properties.put("queueCapacity", queueCapacity);
		return this;
	}

	@Override
	public IConfiguration terminationWaitTimeInSeconds(int terminationWaitTimeInSeconds) {
		m_properties.put("terminationWaitTimeInSeconds", terminationWaitTimeInSeconds);
		return this;
	}

	@Override
	public boolean allowCoreThreadTimeOut() {
		final Object v = m_properties.get("allowCoreThreadTimeOut");
		return v == null || (boolean) v;
	}

	@Override
	public int corePoolSize() {
		final Object v = m_properties.get("corePoolSize");
		return v == null ? 1 : (int) v;
	}

	@Override
	public int maxPoolSize() {
		final Object v = m_properties.get("maxPoolSize");
		return v == null ? 4 : (int) v;
	}

	@Override
	public int keepAliveTimeInSeconds() {
		final Object v = m_properties.get("keepAliveTimeInSeconds");
		return v == null ? 10 : (int) v;
	}

	@Override
	public int queueCapacity() {
		final Object v = m_properties.get("queueCapacity");
		return v == null ? 4096 : (int) v;
	}

	@Override
	public int terminationWaitTimeInSeconds() {
		final Object v = m_properties.get("terminationWaitTimeInSeconds");
		return v == null ? 60 : (int) v;
	}

	@Override
	public synchronized void apply() {
		m_ta.modified(m_properties);
	}

	@Override
	public IConfiguration configuration() {
		return this;
	}

	@Override
	public ITimeoutNotifier createNotifier(Object subject) {
		return m_ta.createNotifier(subject);
	}

	TimeoutAdmin unwrap() {
		return m_ta;
	}

	synchronized void start() throws Throwable {
		m_ta.activate(m_properties);
	}

	synchronized void stop() {
		m_ta.deactivate();
	}
}
