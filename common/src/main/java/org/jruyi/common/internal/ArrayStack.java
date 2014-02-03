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
package org.jruyi.common.internal;

import java.util.EmptyStackException;

/**
 * A generic stack backed by an auto-growing array.
 */
final class ArrayStack<E> {

	private E[] m_array;
	private int m_size;

	/**
	 * Creates an empty stack with the initial capacity of 16.
	 */
	public ArrayStack() {
		this(16);
	}

	/**
	 * Creates an empty stack with the initial capacity of {@code
	 * initialCapacity}.
	 * 
	 * @param initialCapacity
	 *            the initial capacity of this stack.
	 */
	public ArrayStack(int initialCapacity) {
		m_array = newArray(initialCapacity);
	}

	/**
	 * Pushes the given {@code e} onto the top of this stack. The size of this
	 * stack is increased by 1.
	 * 
	 * @param e
	 *            the element to be pushed.
	 */
	public void push(E e) {
		int minCapacity = ++m_size;
		if (minCapacity > m_array.length)
			expandCapacity(minCapacity);

		m_array[minCapacity - 1] = e;
	}

	/**
	 * Returns and removes the element at the top of this stack. The size of
	 * this stack is decreased by 1.
	 * 
	 * @return the element at the top of this stack.
	 * @throws EmptyStackException
	 *             if this stack is empty.
	 */
	public E pop() {
		if (isEmpty())
			throw new EmptyStackException();
		return m_array[--m_size];
	}

	/**
	 * Returns the element at the top of this stack without removing it.
	 * 
	 * @return the element at the top of this stack.
	 * @throws EmptyStackException
	 *             if this stack is empty.
	 */
	public E peek() {
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

	/**
	 * Empties this stack.
	 */
	public void clear() {
		E[] array = m_array;
		for (int n = m_size; n > 0;)
			array[--n] = null;
		m_size = 0;
	}

	E popInternal() {
		return m_array[--m_size];
	}

	@SuppressWarnings("unchecked")
	private E[] newArray(int size) {
		return (E[]) new Object[size];
	}

	private void expandCapacity(int minCapacity) {
		int newCapacity = (m_array.length * 3) / 2 + 1;
		if (newCapacity < 0)
			newCapacity = Integer.MAX_VALUE;
		else if (minCapacity > newCapacity)
			newCapacity = minCapacity;

		E[] array = newArray(newCapacity);
		System.arraycopy(m_array, 0, array, 0, m_size);
		m_array = array;
	}
}
