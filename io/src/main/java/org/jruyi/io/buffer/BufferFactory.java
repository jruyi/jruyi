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

package org.jruyi.io.buffer;

import java.util.Map;

import org.jruyi.common.IThreadLocalCache;
import org.jruyi.common.StrUtil;
import org.jruyi.common.ThreadLocalCache;
import org.jruyi.io.IBuffer;
import org.jruyi.io.IBufferFactory;
import org.jruyi.io.IUnit;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "jruyi.io.buffer", xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class BufferFactory implements IBufferFactory {

	private static final Logger c_logger = LoggerFactory.getLogger(BufferFactory.class);

	private static final String BUFFER_ID = "jruyi.io.buffer.id";
	private static final String UNIT_CAPACITY = "unitCapacity";
	private static final int MIN_UNIT_CAPACITY = 8;

	private final IThreadLocalCache<HeapUnit> m_unitCache = ThreadLocalCache.weakLinkedCache();
	private int m_unitCapacity;

	@Override
	public IBuffer create() {
		return Buffer.get(this);
	}

	@Modified
	void modified(Map<String, ?> properties) {
		final Integer value = (Integer) properties.get(UNIT_CAPACITY);
		if (value == null)
			m_unitCapacity = 1024 * 8;
		else
			m_unitCapacity = value > MIN_UNIT_CAPACITY ? value : MIN_UNIT_CAPACITY;

		final String id = (String) properties.get(BUFFER_ID);
		final String bfName = id != null ? StrUtil.join("BufferFactory[", id, "]") : "BufferFactory";

		c_logger.info("{}: unitCapacity={}", bfName, m_unitCapacity);
	}

	void activate(Map<String, ?> properties) {
		modified(properties);
	}

	IUnit getUnit() {
		HeapUnit unit = m_unitCache.take();
		if (unit == null)
			unit = new HeapUnit(m_unitCapacity);
		else {
			final int capacity = m_unitCapacity;
			if (unit.capacity() < capacity)
				unit.setCapacity(capacity);

			unit.clear();
		}

		return unit;
	}

	IUnit getUnit(int capacity) {
		if (capacity < m_unitCapacity)
			capacity = m_unitCapacity;

		HeapUnit unit = m_unitCache.take();
		if (unit == null)
			unit = new HeapUnit(capacity);
		else {
			if (unit.capacity() < capacity)
				unit.setCapacity(capacity);

			unit.clear();
		}

		return unit;
	}

	void putUnit(IUnit unit) {
		((HeapUnit) unit).cache(this);
	}

	void cache(HeapUnit unit) {
		m_unitCache.put(unit);
	}
}
