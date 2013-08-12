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
package org.jruyi.io.common;

import java.util.concurrent.locks.ReentrantLock;

import org.jruyi.common.ListNode;

public final class SyncQueue<E> {

	private ListNode<E> m_head;
	private ListNode<E> m_tail;
	private final ReentrantLock m_putLock;
	private final ReentrantLock m_pollLock;

	public SyncQueue() {
		m_head = m_tail = ListNode.create();
		m_putLock = new ReentrantLock();
		m_pollLock = new ReentrantLock();
	}

	public void put(E e) {
		ListNode<E> node = ListNode.create();
		node.set(e);
		final ReentrantLock putLock = m_putLock;
		putLock.lock();
		try {
			m_tail.next(node);
			m_tail = node;
		} finally {
			putLock.unlock();
		}
	}

	public E poll() {
		ListNode<E> head = null;
		E e = null;
		final ReentrantLock pollLock = m_pollLock;
		pollLock.lock();
		try {
			head = m_head;
			if (head == m_tail)
				return null;

			ListNode<E> node = head.next();
			e = node.get();
			node.set(null);
			m_head = node;
		} finally {
			pollLock.unlock();
		}

		head.close();
		return e;
	}
}
