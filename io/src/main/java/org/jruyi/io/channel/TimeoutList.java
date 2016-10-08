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

package org.jruyi.io.channel;

import org.jruyi.common.BiListNode;

final class TimeoutList {

	//private final WeakReference<BiListNode<Timer>> m_cache;
	private final BiListNode<Timer> m_head;
	
	TimeoutList() {
		//m_cache = new WeakReference<>(null);
		final BiListNode<Timer> head = new BiListNode<>();
		head.previous(head);
		head.next(head);
		m_head = head;
	}
	
	BiListNode<Timer> addLast(Timer timer) {
		final BiListNode<Timer> head = m_head;
		final BiListNode<Timer> headPrevious = head.previous();
		final BiListNode<Timer> newNode = getNode();
		newNode.set(timer);
		newNode.previous(headPrevious);
		newNode.next(head);
		headPrevious.next(newNode);
		head.previous(newNode);
		return newNode;
	}

	void insertAfter(BiListNode<Timer> node, BiListNode<Timer> newNode) {
		final BiListNode<Timer> next = node.next();
		newNode.previous(node);
		newNode.next(next);
		next.previous(newNode);
		node.next(newNode);
	}

	void remove(BiListNode<Timer> node) {
		final BiListNode<Timer> previous = node.previous();
		final BiListNode<Timer> next = node.next();
		previous.next(next);
		next.previous(previous);
	}
	
	private BiListNode<Timer> getNode() {
//		final BiListNode<Timer> head = m_cache.get();
//		if (head != null) {
//			BiListNode<Timer> node = head.next();
//			if (node != null) {
//				head.next(node.next());
//				//node.next(null);
//				return node;
//			}
//		}
		return new BiListNode<>();
	}
}
