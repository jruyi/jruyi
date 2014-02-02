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

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public final class ByteBufferArray {

	private static final ThreadLocal<WeakReference<ByteBufferArray>> c_wtl;
	private ByteBuffer[] m_array;
	private int m_size;

	static {
		c_wtl = new ThreadLocal<WeakReference<ByteBufferArray>>() {

			@Override
			protected WeakReference<ByteBufferArray> initialValue() {
				return new WeakReference<ByteBufferArray>(null);
			}
		};
	}

	ByteBufferArray() {
		m_array = new ByteBuffer[3];
	}

	public static ByteBufferArray get() {
		ByteBufferArray bba = c_wtl.get().get();
		if (bba == null) {
			bba = new ByteBufferArray();
			c_wtl.set(new WeakReference<ByteBufferArray>(bba));
		}
		return bba;
	}

	public static ByteBufferArray get(int capacity) {
		ByteBufferArray bba = get();
		if (capacity > bba.m_array.length)
			bba.expandCapacity(capacity);

		return bba;
	}

	public ByteBuffer[] array() {
		return m_array;
	}

	public int size() {
		return m_size;
	}

	public void add(ByteBuffer buffer) {
		int minCapacity = m_size + 1;
		if (minCapacity > m_array.length)
			expandCapacity(minCapacity);

		m_array[m_size] = buffer;
		m_size = minCapacity;
	}

	public void clear() {
		int n = m_size;
		ByteBuffer[] array = m_array;
		for (int i = 0; i < n; ++i)
			array[i] = null;

		m_size = 0;
	}

	private void expandCapacity(int minCapacity) {
		int newCapacity = (m_array.length * 3) / 2 + 1;
		if (newCapacity < 0)
			newCapacity = Integer.MAX_VALUE;
		else if (minCapacity > newCapacity)
			newCapacity = minCapacity;

		ByteBuffer[] array = new ByteBuffer[newCapacity];
		System.arraycopy(m_array, 0, array, 0, m_size);
		m_array = array;
	}
}
