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
package org.jruyi.io.common;

import java.util.concurrent.locks.ReentrantLock;

import org.jruyi.common.ListNode;

/**
 * A queue with synchronization on put but not on poll.
 */
public final class SyncPutQueue<E> {

	private ListNode<E> m_head;
	private volatile ListNode<E> m_tail;
	private final ReentrantLock m_putLock;

	public SyncPutQueue() {
		m_head = m_tail = new ListNode<E>();
		m_putLock = new ReentrantLock();
	}

	public void put(E e) {
		ListNode<E> node = new ListNode<E>();
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
		if (m_head == m_tail)
			return null;

		ListNode<E> node = m_head.next();
		E e = node.get();
		node.set(null);
		m_head = node;
		return e;
	}
}
