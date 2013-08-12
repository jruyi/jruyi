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
package org.jruyi.common.internal;

import java.util.EmptyStackException;

import org.jruyi.common.ListNode;

/**
 * A generic stack backed by a linked list.
 */
final class LinkedStack<E> {

	private final ListNode<E> m_head = ListNode.create();
	private int m_size;

	/**
	 * Tests if this stack is empty.
	 *
	 * @return {@code true} if this stack is empty. Otherwise {@code false}.
	 */
	public boolean isEmpty() {
		return m_size == 0;
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

		return m_head.next().get();
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

		return popInternal();
	}

	/**
	 * Pushes the given {@code e} onto the top of this stack. The size of this
	 * stack is increased by 1.
	 *
	 * @param e
	 *            the element to be pushed.
	 */
	public void push(E e) {
		final ListNode<E> head = m_head;
		final ListNode<E> node = ListNode.create();
		node.set(e);
		node.next(head.next());
		head.next(node);
		++m_size;
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
		final ListNode<E> head = m_head;
		ListNode<E> node = head.next();
		while (node != null) {
			ListNode<E> next = node.next();
			node.close();
			node = next;
		}

		head.next(null);
		m_size = 0;
	}

	E popInternal() {
		final ListNode<E> head = m_head;
		ListNode<E> next = head.next();
		head.next(next.next());
		--m_size;
		E e = next.get();
		next.close();
		return e;
	}
}
