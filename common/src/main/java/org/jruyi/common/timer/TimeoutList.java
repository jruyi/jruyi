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

package org.jruyi.common.timer;

import java.util.concurrent.locks.ReentrantLock;

import org.jruyi.common.BiListNode;

final class TimeoutList {

	private final BiListNode<TimeoutEvent<?>> m_head;

	TimeoutList() {
		BiListNode<TimeoutEvent<?>> head = BiListNode.create();
		head.previous(head);
		head.next(head);

		m_head = head;
	}

	BiListNode<TimeoutEvent<?>> addLast(TimeoutEvent<?> event) {
		final BiListNode<TimeoutEvent<?>> head = m_head;
		final BiListNode<TimeoutEvent<?>> headPrevious = head.previous();
		final BiListNode<TimeoutEvent<?>> newNode = BiListNode.create();
		newNode.set(event);
		newNode.previous(headPrevious);
		newNode.next(head);
		headPrevious.next(newNode);
		head.previous(newNode);
		return newNode;
	}

	BiListNode<TimeoutEvent<?>> syncInsertAfter(BiListNode<TimeoutEvent<?>> node, TimeoutEvent<?> event,
			ReentrantLock lock) {
		final BiListNode<TimeoutEvent<?>> newNode = BiListNode.create();
		newNode.set(event);
		lock.lock();
		try {
			final BiListNode<TimeoutEvent<?>> next = node.next();
			newNode.previous(node);
			newNode.next(next);
			next.previous(newNode);
			node.next(newNode);
		} finally {
			lock.unlock();
		}
		return newNode;
	}

	void syncMoveAfter(BiListNode<TimeoutEvent<?>> posNode, BiListNode<TimeoutEvent<?>> node, ReentrantLock lock1,
			ReentrantLock lock2) {
		lock1.lock();
		try {
			lock2.lock();
			try {
				final BiListNode<TimeoutEvent<?>> previous = node.previous();
				BiListNode<TimeoutEvent<?>> next = node.next();

				previous.next(next);
				next.previous(previous);

				next = posNode.next();
				node.next(next);
				posNode.next(node);
				node.previous(posNode);
				next.previous(node);
			} finally {
				lock2.unlock();
			}
		} finally {
			lock1.unlock();
		}
	}

	TimeoutEvent<?> syncRemove(BiListNode<TimeoutEvent<?>> node, ReentrantLock lock) {
		lock.lock();
		try {
			final BiListNode<TimeoutEvent<?>> previous = node.previous();
			final BiListNode<TimeoutEvent<?>> next = node.next();
			previous.next(next);
			next.previous(previous);
		} finally {
			lock.unlock();
		}
		try {
			return node.get();
		} finally {
			node.close();
		}
	}
}
