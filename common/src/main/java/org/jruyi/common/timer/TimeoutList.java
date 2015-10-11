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

import org.jruyi.common.BiListNode;

final class TimeoutList {

	private final BiListNode<TimeoutNotifier<?>> m_head;

	TimeoutList() {
		final BiListNode<TimeoutNotifier<?>> head = BiListNode.create();
		head.previous(head);
		head.next(head);

		m_head = head;
	}

	BiListNode<TimeoutNotifier<?>> addLast(TimeoutNotifier<?> event) {
		final BiListNode<TimeoutNotifier<?>> head = m_head;
		final BiListNode<TimeoutNotifier<?>> headPrevious = head.previous();
		final BiListNode<TimeoutNotifier<?>> newNode = BiListNode.create();
		newNode.set(event);
		newNode.previous(headPrevious);
		newNode.next(head);
		headPrevious.next(newNode);
		head.previous(newNode);
		return newNode;
	}

	void insertAfter(BiListNode<TimeoutNotifier<?>> node, BiListNode<TimeoutNotifier<?>> newNode) {
		final BiListNode<TimeoutNotifier<?>> next = node.next();
		newNode.previous(node);
		newNode.next(next);
		next.previous(newNode);
		node.next(newNode);
	}

	void remove(BiListNode<TimeoutNotifier<?>> node) {
		final BiListNode<TimeoutNotifier<?>> previous = node.previous();
		final BiListNode<TimeoutNotifier<?>> next = node.next();
		previous.next(next);
		next.previous(previous);
	}
}
