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

final class TimerWheel {

	private static final int MAX_CAPACITY = 64 * 1024;

	private final TimeoutList m_list;
	private final BiListNode<Timer>[] m_wheel;
	private final int m_mask;
	private int m_hand;
	private int m_scheduled;

	public TimerWheel(int wheelSize) {
		final int capacity = capacity(wheelSize);
		@SuppressWarnings("unchecked")
		final BiListNode<Timer>[] wheel = (BiListNode<Timer>[]) new BiListNode<?>[capacity + 1];
		final TimeoutList list = new TimeoutList();
		for (int i = 0; i <= capacity; ++i)
			wheel[i] = list.addLast(null);
		m_list = list;
		m_wheel = wheel;
		m_mask = capacity - 1;
	}

	public Timer createTimer(Channel channel) {
		return new Timer(channel, this);
	}

	public int scheduledTimers() {
		return m_scheduled;
	}

	public void tick() {
		final int hand = m_hand;
		final int nextHand = hand + 1;
		final BiListNode<Timer>[] wheel = m_wheel;
		final BiListNode<Timer> begin = wheel[hand];
		final BiListNode<Timer> end = wheel[nextHand];
		final TimeoutList list = m_list;
		int scheduled = m_scheduled;
		for (;;) {
			final BiListNode<Timer> node = begin.next();
			if (node == end)
				break;
			final Timer timer = node.get();
			final int timeout = timer.timeout();
			if (timeout > 0)
				reschedule(timer, timeout);
			else {
				list.remove(node);
				--scheduled;
				timer.onTimeout();
			}
		}
		m_scheduled = scheduled;
		m_hand = getEffectiveIndex(nextHand);
	}

	void schedule(Timer timer, int timeout) {
		final BiListNode<Timer> newNode = new BiListNode<>();
		if (timeout > m_mask) {
			final int mask = m_mask;
			timer.timeout(timeout - mask);
			timeout = mask;
		}
		newNode.set(timer);
		timer.node(newNode);
		final int index = getEffectiveIndex(m_hand + timeout);
		timer.index(index);
		final BiListNode<Timer> node = m_wheel[index];
		m_list.insertAfter(node, newNode);
		++m_scheduled;
	}

	void reschedule(Timer timer, int timeout) {
		final TimeoutList list = m_list;

		final BiListNode<Timer> node = timer.node();
		list.remove(node);

		if (timeout > m_mask) {
			final int mask = m_mask;
			timer.timeout(timeout - mask);
			timeout = mask;
		} else {
			timer.timeout(0);
		}

		// dest list
		final int index = getEffectiveIndex(m_hand + timeout);
		timer.index(index);
		final BiListNode<Timer> destHead = m_wheel[index];
		list.insertAfter(destHead, node);
	}

	void cancel(Timer timer) {
		final BiListNode<Timer> node = timer.node();
		m_list.remove(node);
		--m_scheduled;
	}

	private int capacity(int wheelSize) {
		int capacity = 1 << (32 - Integer.numberOfLeadingZeros(wheelSize));
		if (capacity > MAX_CAPACITY)
			capacity = MAX_CAPACITY;
		return capacity;
	}

	private int getEffectiveIndex(int index) {
		return (index & m_mask);
	}
}
