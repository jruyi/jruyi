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

import java.util.EmptyStackException;

/**
 * An auto-growing {@code int} stack. This class also provides a simple cache
 * mechanism.
 */
public final class IntStack implements ICloseable {

	private static final IThreadLocalCache<IntStack> m_cache = ThreadLocalCache.weakArrayCache();
	private int[] m_array;
	private int m_size;

	private IntStack() {
		this(6);
	}

	private IntStack(int capacity) {
		m_array = new int[capacity];
	}

	/**
	 * Gets an object of {@code IntStack} from the local cache of the current
	 * thread, if the cache is not empty. Otherwise a new object is created and
	 * returned.
	 * 
	 * @return an object of {@code IntStack}.
	 */
	public static IntStack get() {
		IntStack stack = m_cache.take();
		if (stack == null)
			stack = new IntStack();

		return stack;
	}

	/**
	 * Gets an object of {@code IntStack} from the local cache of the current
	 * thread, if the cache is not empty. Otherwise a new object is created and
	 * returned.
	 * <p>
	 * The returned object is ensured to have the minimum capacity of the given
	 * {@code capacity}.
	 * 
	 * @param capacity
	 *            the minimum capacity of the {@code IntStack} to be returned.
	 * @return an object of {@code IntStack}.
	 */
	public static IntStack get(int capacity) {
		IntStack stack = m_cache.take();
		if (stack == null)
			stack = new IntStack(capacity);
		else
			stack.ensureCapacity(capacity);

		return stack;
	}

	/**
	 * Clears this {@code IntStack} and puts it to the local cache of the
	 * current thread.
	 */
	@Override
	public void close() {
		m_size = 0;
		m_cache.put(this);
	}

	/**
	 * Pushes the given {@code i} onto the top of this stack. The size of this
	 * stack is increased by 1.
	 * 
	 * @param i
	 *            the {@code int} to be pushed.
	 */
	public void push(int i) {
		int minCapacity = ++m_size;
		if (minCapacity > m_array.length)
			expandCapacity(minCapacity);
		m_array[minCapacity - 1] = i;
	}

	/**
	 * Returns and removes the {@code int} at the top of this stack. The size of
	 * this stack is reduced by 1.
	 * 
	 * @return the {@code int} at the top of this stack.
	 * @throws EmptyStackException
	 *             if this stack is empty.
	 */
	public int pop() {
		if (isEmpty())
			throw new EmptyStackException();
		return m_array[--m_size];
	}

	/**
	 * Returns the {@code int} at the top of this stack without removing it.
	 * 
	 * @return the {@code int} at the top of this stack.
	 * @throws EmptyStackException
	 *             if this stack is empty.
	 */
	public int peek() {
		if (isEmpty())
			throw new EmptyStackException();
		return m_array[m_size - 1];
	}

	/**
	 * Returns the current size of this stack.
	 * 
	 * @return the size of this stack.
	 */
	public int size() {
		return m_size;
	}

	/**
	 * Empties this stack.
	 */
	public void clear() {
		m_size = 0;
	}

	/**
	 * Tests if this stack is empty.
	 * 
	 * @return {@code true} if this stack is empty. Otherwise {@code false}.
	 */
	public boolean isEmpty() {
		return m_size < 1;
	}

	/**
	 * Ensures this stack to have the minimum capacity of {@code minCapacity}.
	 * 
	 * @param minCapacity
	 *            the minimum capacity of this stack to be ensured.
	 */
	public void ensureCapacity(int minCapacity) {
		if (minCapacity > m_array.length)
			expandCapacity(minCapacity);
	}

	int popInternal() {
		return m_array[--m_size];
	}

	private void expandCapacity(int minCapacity) {
		int[] array = m_array;
		int newCapacity = (array.length * 3) / 2 + 1;
		if (newCapacity < 0)
			newCapacity = Integer.MAX_VALUE;
		else if (minCapacity > newCapacity)
			newCapacity = minCapacity;

		int[] newArray = new int[newCapacity];
		System.arraycopy(array, 0, newArray, 0, m_size);
		m_array = newArray;
	}
}
