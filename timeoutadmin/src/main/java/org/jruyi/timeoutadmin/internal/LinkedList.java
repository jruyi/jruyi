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

package org.jruyi.timeoutadmin.internal;

import org.jruyi.common.BiListNode;

final class LinkedList<E> {

	private final BiListNode<E> m_head;

	LinkedList() {
		BiListNode<E> head = BiListNode.create();
		head.previous(head);
		head.next(head);

		m_head = head;
	}

	BiListNode<E> addLast(E e) {
		final BiListNode<E> head = m_head;
		final BiListNode<E> headPrevious = head.previous();
		final BiListNode<E> newNode = BiListNode.create();
		newNode.set(e);
		newNode.previous(headPrevious);
		newNode.next(head);
		headPrevious.next(newNode);
		head.previous(newNode);
		return newNode;
	}

	BiListNode<E> syncInsertAfter(BiListNode<E> node, E e, Object lock) {
		final BiListNode<E> newNode = BiListNode.create();
		newNode.set(e);
		synchronized (lock) {
			final BiListNode<E> next = node.next();
			newNode.previous(node);
			newNode.next(next);
			next.previous(newNode);
			node.next(newNode);
		}
		return newNode;
	}

	void syncMoveAfter(BiListNode<E> posNode, BiListNode<E> node, Object lock1, Object lock2) {
		synchronized (lock1) {
			synchronized (lock2) {
				final BiListNode<E> previous = node.previous();
				BiListNode<E> next = node.next();

				previous.next(next);
				next.previous(previous);

				next = posNode.next();
				node.next(next);
				posNode.next(node);
				node.previous(posNode);
				next.previous(node);
			}
		}
	}

	E syncRemove(BiListNode<E> node, Object lock) {
		synchronized (lock) {
			final BiListNode<E> previous = node.previous();
			final BiListNode<E> next = node.next();
			previous.next(next);
			next.previous(previous);
		}
		E e = node.get();
		node.close();
		return e;
	}
}
