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
package org.jruyi.timeoutadmin.impl;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.jruyi.common.BiListNode;
import org.jruyi.timeoutadmin.ITimeoutAdmin;
import org.jruyi.timeoutadmin.ITimeoutNotifier;
import org.jruyi.workshop.IWorkshop;
import org.jruyi.workshop.WorkshopConstants;

@Service(ITimeoutAdmin.class)
@Component(name = "jruyi.timeoutadmin", createPid = false)
public final class TimeoutAdmin implements Runnable, ITimeoutAdmin {

	// 1 second
	private static final long PERIOD = 1000L;
	// 30 minutes.
	private static final int DEFAULT_SCALE = 30 * 60;
	private static final int DEFAULT_LOCK_NUM = 32;

	@Property(intValue = DEFAULT_SCALE)
	private static final String P_SCALE = "scale";

	private static final Object DELAY = new Object();

	// the maximum timeout in a single schedule
	private int m_scale;
	// minus - 1 from which MUST be a power of 2 and greater than m_scale.
	private int m_capacityMask;
	// minus - 1 from which is a power of 2 and not more than 32.
	private int m_lockNumMask;
	// The hand that points to the current timeout sublist, nodes of witch are
	// between m_wheel[m_hand] and m_wheel[m_hand + 1].
	private int m_hand;
	private BiListNode<TimeoutEvent>[] m_wheel;
	private LinkedList<TimeoutEvent> m_list;
	private ReentrantLock[] m_locks;
	private Thread m_thread;

	@Reference(name = "workshop", policy = ReferencePolicy.DYNAMIC, target = WorkshopConstants.DEFAULT_WORKSHOP_TARGET)
	private IWorkshop m_workshop;

	@Override
	public ITimeoutNotifier createNotifier(Object subject) {
		return new TimeoutNotifier(subject, this);
	}

	@Override
	public void run() {
		Thread thread = Thread.currentThread();
		long nextExecutionTime = System.currentTimeMillis() + PERIOD;
		try {
			long waitTime;
			while (!thread.isInterrupted()) {
				while ((waitTime = nextExecutionTime
						- System.currentTimeMillis()) < 0) {
					nextExecutionTime += PERIOD;
					tick();
				}

				if (waitTime > 0) {
					synchronized (DELAY) {
						DELAY.wait(waitTime);
					}
				}

				nextExecutionTime = System.currentTimeMillis() + PERIOD;
				tick();
			}
		} catch (InterruptedException e) {
		}
	}

	protected synchronized void bindWorkshop(IWorkshop workshop) {
		m_workshop = workshop;
	}

	protected synchronized void unbindWorkshop(IWorkshop workshop) {
		if (m_workshop == workshop)
			m_workshop = null;
	}

	@Modified
	protected void modified(Map<String, ?> properties) {
		// This empty modified method is for changing the property
		// "workshop.target" dynamically.
		// To take the change of property "scale", reactivating is required.
	}

	protected void activate(Map<String, ?> properties) {
		int scale = (Integer) properties.get(P_SCALE);
		if (scale < 5)
			scale = DEFAULT_SCALE;

		int capacity = 1;
		for (int i = scale + 10; i != 0; i >>= 1)
			capacity <<= 1;

		final int lockNum = capacity < DEFAULT_LOCK_NUM ? capacity
				: DEFAULT_LOCK_NUM;

		m_scale = scale;
		m_capacityMask = capacity - 1;
		m_lockNumMask = lockNum - 1;

		LinkedList<TimeoutEvent> list = new LinkedList<TimeoutEvent>();
		// one more as a tail node for conveniently iterating the timeout
		// sublist
		@SuppressWarnings("unchecked")
		final BiListNode<TimeoutEvent>[] wheel = (BiListNode<TimeoutEvent>[]) new BiListNode<?>[capacity + 1];
		for (int i = 0; i < capacity + 1; ++i)
			// create sentinel nodes
			wheel[i] = list.addLast(null);

		final ReentrantLock[] locks = new ReentrantLock[lockNum];
		for (int i = 0; i < lockNum; ++i)
			locks[i] = new ReentrantLock();

		m_hand = 0;
		m_list = list;
		m_wheel = wheel;
		m_locks = locks;

		m_thread = new Thread(this, "TimeoutAdmin");
		m_thread.start();
	}

	protected void deactivate() {
		m_thread.interrupt();
		try {
			m_thread.join();
		} catch (InterruptedException e) {
		}
		m_list = null;
		m_thread = null;
		m_wheel = null;
		m_list = null;
		m_locks = null;
	}

	void schedule(TimeoutNotifier notifier, int timeout) {
		final TimeoutEvent event = TimeoutEvent.get(notifier, timeout);
		int scale = m_scale;
		if (timeout > scale) {
			event.setTimeLeft(timeout - scale);
			timeout = scale;
		} else
			event.setTimeLeft(0);

		final int index = getEffectiveIndex(m_hand + timeout);
		event.setIndex(index);
		notifier.setNode(m_list.syncInsertAfter(m_wheel[index], event,
				getLock(index)));
	}

	void reschedule(TimeoutNotifier notifier, int timeout) {
		final BiListNode<TimeoutEvent> node = notifier.getNode();
		final TimeoutEvent event = node.get();
		final ReentrantLock lock1 = getLock(event.getIndex());

		// dest sublist
		int scale = m_scale;
		if (timeout > scale) {
			event.setTimeLeft(timeout - scale);
			timeout = scale;
		} else
			event.setTimeLeft(0);

		final int index = getEffectiveIndex(m_hand + timeout);
		event.setIndex(index);
		final BiListNode<TimeoutEvent> destHead = m_wheel[index];
		final ReentrantLock lock2 = getLock(index);

		m_list.syncMoveAfter(destHead, node, lock1, lock2);
	}

	void cancel(TimeoutNotifier notifier) {
		final BiListNode<TimeoutEvent> node = notifier.getNode();
		notifier.clearNode();
		TimeoutEvent event = node.get();
		final ReentrantLock lock = getLock(event.getIndex());
		event = m_list.syncRemove(node, lock);

		// release the timeout event
		event.release();
	}

	void fireTimeout(TimeoutNotifier notifier) {
		final BiListNode<TimeoutEvent> node = notifier.getNode();
		notifier.clearNode();
		final ReentrantLock lock = getLock(m_hand);
		final TimeoutEvent event = m_list.syncRemove(node, lock);

		m_workshop.run(event);
	}

	void scheduleNextRound(TimeoutNotifier notifier) {
		final BiListNode<TimeoutEvent> node = notifier.getNode();
		final TimeoutEvent event = node.get();
		final int scale = m_scale;
		int n = event.getTimeLeft();
		// need reschedule
		if (n > scale) {
			event.setTimeLeft(n - scale);
			n = scale;
		} else
			event.setTimeLeft(0);

		final int hand = m_hand;
		n = getEffectiveIndex(hand + n);
		event.setIndex(n);

		final ReentrantLock lock1 = getLock(hand);
		final ReentrantLock lock2 = getLock(n);
		// keep using the same list node
		m_list.syncMoveAfter(m_wheel[n], node, lock1, lock2);
	}

	private int getEffectiveIndex(int index) {
		return (index & m_capacityMask);
	}

	private ReentrantLock getLock(int index) {
		return m_locks[index & m_lockNumMask];
	}

	private void tick() {
		final int hand = m_hand;
		final int nextHand = hand + 1;
		final BiListNode<TimeoutEvent>[] wheel = m_wheel;
		final BiListNode<TimeoutEvent> begin = wheel[hand];
		final BiListNode<TimeoutEvent> end = wheel[nextHand];
		BiListNode<TimeoutEvent> node;

		while ((node = begin.next()) != end) {
			TimeoutNotifier notifier;
			final TimeoutEvent event = node.get();

			// If this node has been cancelled, just skip.
			// Otherwise, go ahead.
			if (event != null && (notifier = event.getNotifier()) != null)
				// Passing "hand" for checking the notifier is still in this
				// same timeout sublist. Otherwise it may be cancelled or
				// rescheduled, and needs to be skipped.
				notifier.onTimeout(hand);
		}

		// tick
		m_hand = getEffectiveIndex(nextHand);
	}
}
