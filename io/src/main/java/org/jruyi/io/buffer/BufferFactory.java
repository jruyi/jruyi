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
package org.jruyi.io.buffer;

import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.jruyi.common.IThreadLocalCache;
import org.jruyi.common.ThreadLocalCache;
import org.jruyi.io.IBuffer;
import org.jruyi.io.IBufferFactory;
import org.jruyi.io.IUnit;

@Service
@Component(name = "jruyi.io.buffer", createPid = false)
public final class BufferFactory implements IBufferFactory {

	private static final int MIN_UNIT_CAPACITY = 8;

	@Property(intValue = 8192)
	private static final String UNIT_CAPACITY = "unitCapacity";

	private final IThreadLocalCache<HeapUnit> m_unitCache = ThreadLocalCache
			.weakLinkedCache();
	private int m_unitCapacity;

	@Override
	public IBuffer create() {
		return Buffer.get(this);
	}

	@Modified
	protected void modified(Map<String, ?> properties) {
		int value = (Integer) properties.get(UNIT_CAPACITY);
		m_unitCapacity = value > MIN_UNIT_CAPACITY ? value : MIN_UNIT_CAPACITY;
	}

	protected void activate(Map<String, ?> properties) {
		modified(properties);
	}

	IUnit getUnit() {
		HeapUnit unit = m_unitCache.take();
		if (unit == null)
			unit = new HeapUnit(m_unitCapacity);
		else {
			int capacity = m_unitCapacity;
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
		if (unit instanceof HeapUnit)
			m_unitCache.put((HeapUnit) unit);
	}
}
