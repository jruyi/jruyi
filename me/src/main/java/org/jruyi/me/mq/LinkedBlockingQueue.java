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

package org.jruyi.me.mq;

import java.util.concurrent.locks.ReentrantLock;

import org.jruyi.common.ListNode;

final class LinkedBlockingQueue {

	private ListNode<Message> m_head;
	private ListNode<Message> m_tail;
	private final ReentrantLock m_putLock;
	private final ReentrantLock m_takeLock;

	LinkedBlockingQueue() {
		m_head = m_tail = ListNode.create();
		m_putLock = new ReentrantLock();
		m_takeLock = new ReentrantLock();
	}

	public void put(Message message) throws InterruptedException {
		final ListNode<Message> node = ListNode.create();
		node.set(message);
		final ReentrantLock putLock = m_putLock;
		putLock.lockInterruptibly();
		try {
			m_tail.next(node);
			m_tail = node;
		} finally {
			putLock.unlock();
		}
	}

	/**
	 * In our case, the queue won't be empty when this method is called.
	 */
	public Message take() throws InterruptedException {
		final ListNode<Message> node;
		final Message msg;
		final ReentrantLock takeLock = m_takeLock;
		takeLock.lockInterruptibly();
		try {
			node = m_head;
			ListNode<Message> head = node.next();
			m_head = head;
			msg = head.get();
			head.set(null);
		} finally {
			takeLock.unlock();
		}

		node.close();
		return msg;
	}
}
