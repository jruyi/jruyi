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
import org.jruyi.core.IBufferFactory;
import org.jruyi.io.IBuffer;
import org.jruyi.io.buffer.BufferFactory;

final class BufferFactoryWrapper implements IBufferFactory, IBufferFactory.IConfiguration {

	private final Map<String, Object> m_properties = new HashMap<>(2);
	private final BufferFactory m_bf = new BufferFactory();

	public BufferFactoryWrapper() {
	}

	public BufferFactoryWrapper(String name) {
		if (name == null || (name = name.trim()).isEmpty())
			throw new IllegalArgumentException("name cannot be null or empty");
		m_properties.put(BufferFactory.BUFFER_ID, name);
	}

	@Override
	public IConfiguration unitCapacity(int unitCapacity) {
		if (unitCapacity < BufferFactory.MIN_UNIT_CAPACITY)
			throw new IllegalArgumentException(
					StrUtil.join("Illegal unitCapacity: ", unitCapacity, " >= " + BufferFactory.MIN_UNIT_CAPACITY));
		m_properties.put("unitCapacity", unitCapacity);
		return this;
	}

	@Override
	public int unitCapacity() {
		final Object v = m_properties.get("unitCapacity");
		return v == null ? 8 * 1024 : (int) v;
	}

	@Override
	public String name() {
		return (String) m_properties.get(BufferFactory.BUFFER_ID);
	}

	@Override
	public IConfiguration configuration() {
		return this;
	}

	@Override
	public IBuffer create() {
		return m_bf.create();
	}

	@Override
	public synchronized void apply() {
		m_bf.modified(m_properties);
	}

	synchronized void start() {
		m_bf.activate(m_properties);
	}

	synchronized void stop() {
	}

	BufferFactory unwrap() {
		return m_bf;
	}
}
