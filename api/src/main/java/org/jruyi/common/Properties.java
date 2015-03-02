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

package org.jruyi.common;

import java.io.Serializable;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A substitution class to {@code Hashtable} to be used for {@code Dictionary}
 * properties in OSGi, for synchronization is not necessary and does a penalty
 * to the performance.
 */
public final class Properties extends Dictionary<String, Object> implements Map<String, Object>, Cloneable,
		Serializable, IDumpable {

	private static final long serialVersionUID = -1655365857958439329L;
	private final HashMap<String, Object> m_map;

	private static final class KeyEnumeration implements Enumeration<String> {

		private final Iterator<String> m_iter;

		public KeyEnumeration(HashMap<String, Object> map) {
			m_iter = map.keySet().iterator();
		}

		@Override
		public boolean hasMoreElements() {
			return m_iter.hasNext();
		}

		@Override
		public String nextElement() {
			return m_iter.next();
		}
	}

	private static final class ValueEnumeration implements Enumeration<Object> {

		private final Iterator<Object> m_iter;

		public ValueEnumeration(HashMap<String, Object> map) {
			m_iter = map.values().iterator();
		}

		@Override
		public boolean hasMoreElements() {
			return m_iter.hasNext();
		}

		@Override
		public Object nextElement() {
			return m_iter.next();
		}
	}

	/**
	 * Constructs an empty {@code Properties} with the specified initial
	 * capacity and load factor.
	 * 
	 * @param initialCapacity
	 *            the initial capacity
	 * @param loadFactor
	 *            the load factor
	 */
	public Properties(int initialCapacity, float loadFactor) {
		m_map = new HashMap<>(initialCapacity, loadFactor);
	}

	/**
	 * Constructs an empty {@code Properties} with the default initial capacity
	 * (16) and the default load factor (0.75).
	 */
	public Properties() {
		m_map = new HashMap<>();
	}

	/**
	 * Constructs an empty {@code Properties} with the specified initial
	 * capacity and the default load factor (0.75).
	 * 
	 * @param initialCapacity
	 *            the initial capacity
	 */
	public Properties(int initialCapacity) {
		m_map = new HashMap<>(initialCapacity);
	}

	/**
	 * Constructs a new {@code Properties} with the same mappings as the
	 * specified {@code map}. The {@code Properties} is created with default
	 * load factor (0.75) and an initial capacity sufficient to hold the
	 * mappings in the specified {@code map}.
	 * 
	 * @param map
	 *            the map whose mappings are to be placed in this properties
	 */
	public Properties(Map<? extends String, ?> map) {
		m_map = new HashMap<>(map);
	}

	@Override
	public Enumeration<Object> elements() {
		return new ValueEnumeration(m_map);
	}

	@Override
	public Object get(Object key) {
		return m_map.get((String) key);
	}

	@Override
	public boolean isEmpty() {
		return m_map.isEmpty();
	}

	@Override
	public Enumeration<String> keys() {
		return new KeyEnumeration(m_map);
	}

	@Override
	public Object put(String key, Object value) {
		return m_map.put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return m_map.remove((String) key);
	}

	@Override
	public int size() {
		return m_map.size();
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new Properties(m_map);
	}

	@Override
	public void clear() {
		m_map.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return m_map.containsKey((String) key);
	}

	@Override
	public boolean containsValue(Object value) {
		return m_map.containsValue(value);
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		return m_map.entrySet();
	}

	@Override
	public Set<String> keySet() {
		return m_map.keySet();
	}

	@Override
	public void putAll(Map<? extends String, ?> m) {
		m_map.putAll(m);
	}

	@Override
	public Collection<Object> values() {
		return m_map.values();
	}

	@Override
	public void dump(StringBuilder builder) {
		Iterator<Entry<String, Object>> iter = entrySet().iterator();
		if (!iter.hasNext()) {
			builder.append("{}");
			return;
		}

		builder.append('{');
		for (;;) {
			Entry<String, Object> entry = iter.next();
			builder.append(entry.getKey()).append('=').deeplyAppend(entry.getValue());
			if (!iter.hasNext()) {
				builder.append('}');
				return;
			}

			builder.append(", ");
		}
	}

	@Override
	public String toString() {
		Iterator<Entry<String, Object>> iter = entrySet().iterator();
		if (!iter.hasNext())
			return "{}";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append('{');
			for (;;) {
				Entry<String, Object> entry = iter.next();
				builder.append(entry.getKey()).append('=').deeplyAppend(entry.getValue());
				if (!iter.hasNext())
					return builder.append('}').toString();

				builder.append(", ");
			}
		}
	}
}
