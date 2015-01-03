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

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.jruyi.common.BiListNode;
import org.jruyi.common.IScheduler;
import org.jruyi.timeoutadmin.ITimeoutAdmin;
import org.jruyi.timeoutadmin.ITimeoutNotifier;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(name = "jruyi.timeoutadmin", //
configurationPolicy = ConfigurationPolicy.IGNORE, //
xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class TimeoutAdmin implements ITimeoutAdmin {

	// 1 hour
	private static final int SCALE = 60 * 60;
	private static final int COVER_DAYS = 7;
	private static final int UNIT_TW2;
	private static final int SCALE_TW2;

	private LinkedList<TimeoutEvent> m_list;
	private TimeWheel m_tw1;
	private TimeWheel m_tw2;

	private IScheduler m_scheduler;
	private Executor m_executor;

	static {
		int n = 1;
		for (int i = SCALE / 2 + 1; i != 0; i >>= 1)
			n <<= 1;
		UNIT_TW2 = n;
		n = 1;
		for (int i = (COVER_DAYS * 24 * 60 * 60 + UNIT_TW2 - 1) / UNIT_TW2 + 1; i != 0; i >>= 1)
			n <<= 1;
		SCALE_TW2 = n;
	}

	final class TimeWheel implements Runnable {

		private final BiListNode<TimeoutEvent>[] m_wheel;
		private final ReentrantLock[] m_locks;
		private final int m_capacityMask;

		// The hand that points to the current timeout sublist, nodes of which
		// are between m_wheel[m_hand] and m_wheel[m_hand + 1].
		private int m_hand;

		public TimeWheel(int capacity) {
			// one more as a tail node for conveniently iterating the timeout
			// sublist
			@SuppressWarnings("unchecked")
			final BiListNode<TimeoutEvent>[] wheel = (BiListNode<TimeoutEvent>[]) new BiListNode<?>[capacity + 1];
			final ReentrantLock[] locks = new ReentrantLock[capacity];
			final LinkedList<TimeoutEvent> list = m_list;
			for (int i = 0; i < capacity; ++i) {
				// create sentinel nodes
				wheel[i] = list.addLast(null);
				locks[i] = new ReentrantLock();
			}
			wheel[capacity] = list.addLast(null);

			m_wheel = wheel;
			m_locks = locks;
			m_capacityMask = capacity - 1;
		}

		@Override
		public void run() {
			final int hand = m_hand;
			final int nextHand = hand + 1;
			final BiListNode<TimeoutEvent>[] wheel = m_wheel;
			final BiListNode<TimeoutEvent> begin = wheel[hand];
			final BiListNode<TimeoutEvent> end = wheel[nextHand];
			BiListNode<TimeoutEvent> node;

			while ((node = begin.next()) != end) {
				final TimeoutNotifier notifier;
				final TimeoutEvent event = node.get();

				// If this node has been cancelled, just skip.
				// Otherwise, go ahead.
				if (event != null && (notifier = event.getNotifier()) != null)
					// Passing "hand" for checking the notifier is still in this
					// same timeout sublist. Otherwise it may be cancelled or
					// rescheduled, and needs to be skipped.
					notifier.onTimeout(this, hand);
			}

			// tick
			m_hand = getEffectiveIndex(nextHand);
		}

		void schedule(TimeoutNotifier notifier, TimeoutEvent event, int offset) {
			final int n = getEffectiveIndex(m_hand + offset);
			event.setTimeWheelAndIndex(this, n);
			notifier.setNode(m_list.syncInsertAfter(m_wheel[n], event, getLock(n)));
		}

		void reschedule(TimeoutNotifier notifier, int offset, ReentrantLock srcLock) {
			final BiListNode<TimeoutEvent> node = notifier.getNode();
			final TimeoutEvent event = node.get();
			final int n = getEffectiveIndex(m_hand + offset);
			event.setTimeWheelAndIndex(this, n);

			final ReentrantLock dstLock = getLock(n);
			m_list.syncMoveAfter(m_wheel[n], node, srcLock, dstLock);
		}

		ReentrantLock getLock(int index) {
			return m_locks[index];
		}

		private int getEffectiveIndex(int index) {
			return (index & m_capacityMask);
		}
	}

	@Override
	public ITimeoutNotifier createNotifier(Object subject) {
		return new TimeoutNotifier(subject, this);
	}

	@Reference(name = "scheduler", policy = ReferencePolicy.DYNAMIC)
	synchronized void setScheduler(IScheduler scheduler) {
		m_scheduler = scheduler;
	}

	synchronized void unsetScheduler(IScheduler scheduler) {
		if (m_scheduler == scheduler)
			m_scheduler = null;
	}

	@Reference(name = "executor", policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
	synchronized void setExecutor(Executor executor) {
		m_executor = executor;
	}

	synchronized void unsetExecutor(Executor executor) {
		if (m_executor == executor)
			m_executor = null;
	}

	void activate() {
		m_list = new LinkedList<TimeoutEvent>();

		final TimeWheel tw1 = new TimeWheel(UNIT_TW2 * 2);
		final TimeWheel tw2 = new TimeWheel(SCALE_TW2);
		final IScheduler scheduler = m_scheduler;
		scheduler.scheduleAtFixedRate(tw1, 1, 1, TimeUnit.SECONDS);
		scheduler.scheduleAtFixedRate(tw2, UNIT_TW2, UNIT_TW2, TimeUnit.SECONDS);
		m_tw1 = tw1;
		m_tw2 = tw2;
	}

	void deactivate() {
		m_tw2 = null;
		m_tw1 = null;
		m_list = null;
	}

	void schedule(TimeoutNotifier notifier, int timeout) {
		final TimeoutEvent event = TimeoutEvent.get(notifier, timeout);
		if (timeout < UNIT_TW2 * 2)
			m_tw1.schedule(notifier, event, timeout);
		else {
			int offset = (timeout - UNIT_TW2 - 1) / UNIT_TW2;
			if (offset > SCALE_TW2 - 1)
				offset = SCALE_TW2 - 1;
			m_tw2.schedule(notifier, event, offset);
		}
	}

	void reschedule(TimeoutNotifier notifier, int timeout) {
		final TimeoutEvent event = notifier.getNode().get();
		final ReentrantLock srcLock = event.getTimeWheel().getLock(event.getIndex());
		if (timeout < UNIT_TW2 * 2)
			m_tw1.reschedule(notifier, timeout, srcLock);
		else {
			timeout = (timeout - UNIT_TW2 - 1) / UNIT_TW2;
			if (timeout > SCALE_TW2 - 1)
				timeout = SCALE_TW2 - 1;
			m_tw2.reschedule(notifier, timeout, srcLock);
		}
	}

	void cancel(TimeoutNotifier notifier) {
		final BiListNode<TimeoutEvent> node = notifier.getNode();
		notifier.clearNode();
		final TimeoutEvent event = node.get();
		final ReentrantLock lock = event.getTimeWheel().getLock(event.getIndex());
		m_list.syncRemove(node, lock);

		// release the timeout event
		event.release();
	}

	void fireTimeout(TimeoutNotifier notifier) {
		final BiListNode<TimeoutEvent> node = notifier.getNode();
		notifier.clearNode();
		final TimeoutEvent event = node.get();
		final ReentrantLock lock = event.getTimeWheel().getLock(event.getIndex());
		m_list.syncRemove(node, lock);

		final Executor executor = m_executor;
		if (executor != null)
			executor.execute(event);
	}
}
