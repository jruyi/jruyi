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

import java.lang.ref.WeakReference;

/**
 * A node contains a reference to the next node and a reference to the element
 * data. It is used to form a singly linked list.
 * 
 * <p>
 * This class has a thread local cache mechanism for node instances.
 * 
 * @param <E>
 *            the type of the element
 */
public final class ListNode<E> implements ICloseable {

	E m_e;
	ListNode<E> m_next;

	static final class Stack {

		private static final ThreadLocal<WeakReference<Stack>> c_cache;
		private final ListNode<?> m_head = new ListNode<>();

		static {
			c_cache = new ThreadLocal<WeakReference<Stack>>() {

				@Override
				protected WeakReference<Stack> initialValue() {
					return new WeakReference<>(null);
				}
			};
		}

		static Stack get() {
			Stack t = c_cache.get().get();
			if (t == null) {
				t = new Stack();
				c_cache.set(new WeakReference<>(t));
			}
			return t;
		}

		boolean isEmpty() {
			return m_head.m_next == null;
		}

		<E> ListNode<E> pop() {
			@SuppressWarnings("unchecked")
			final ListNode<E> head = (ListNode<E>) m_head;
			final ListNode<E> next = head.m_next;
			head.m_next = next.m_next;
			next.m_next = null;
			return next;
		}

		<E> void push(ListNode<E> node) {
			@SuppressWarnings("unchecked")
			final ListNode<E> head = (ListNode<E>) m_head;
			node.m_next = head.m_next;
			head.m_next = node;
			node.m_e = null;
		}
	}

	/**
	 * The constructor method.
	 */
	public ListNode() {
	}

	/**
	 * The constructor method.
	 * 
	 * @param e
	 *            the initial data element
	 * @since 1.2
	 */
	public ListNode(E e) {
		m_e = e;
	}

	/**
	 * Returns a {@code ListNode} instance fetched from the current thread's
	 * local cache if the cache is not empty. Otherwise a new instance will be
	 * created and returned.
	 * 
	 * @param <T>
	 *            the type of the element
	 * @return an instance of {@code ListNode}
	 */
	@SuppressWarnings({ "unchecked" })
	public static <T> ListNode<T> create() {
		Stack cache = Stack.get();
		return (ListNode<T>) (cache.isEmpty() ? new ListNode<>() : cache.pop());
	}

	/**
	 * Returns a {@code ListNode} instance, with the specified {@code e} as the
	 * initial data element, fetched from the current thread's local cache if
	 * the cache is not empty. Otherwise a new instance will be created and
	 * returned.
	 * 
	 * @param <T>
	 *            the type of the element
	 * @param e
	 *            the initial data element
	 * @return an instance of {@code ListNode}
	 * @since 1.2
	 */
	public static <T> ListNode<T> create(T e) {
		ListNode<T> node = create();
		node.set(e);
		return node;
	}

	/**
	 * Returns the data element.
	 * 
	 * @return the data element
	 */
	public E get() {
		return m_e;
	}

	/**
	 * Returns the data element and sets it to null.
	 * 
	 * @return the data element
	 * @since 1.2
	 */
	public E take() {
		E e = m_e;
		m_e = null;
		return e;
	}

	/**
	 * Sets the data element to the specified {@code e}.
	 * 
	 * @param e
	 *            the data element to be held
	 */
	public void set(E e) {
		m_e = e;
	}

	/**
	 * Returns the next node.
	 * 
	 * @return the next node
	 */
	public ListNode<E> next() {
		return m_next;
	}

	/**
	 * Sets the next node to the specified {@code node}.
	 * 
	 * @param node
	 *            the next node to be pointed to
	 */
	public void next(ListNode<E> node) {
		m_next = node;
	}

	/**
	 * Recycles this object to the current thread's local cache.
	 */
	@Override
	public void close() {
		Stack stack = Stack.get();
		stack.push(this);
	}
}
