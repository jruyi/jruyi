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

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.jruyi.common.BiListNode;
import org.jruyi.common.IScheduler;
import org.jruyi.common.ITimeoutNotifier;
import org.jruyi.common.ITimer;
import org.jruyi.common.ITimerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Timer implements ITimer, ITimerConfiguration, Runnable {

	private static final Logger c_logger = LoggerFactory.getLogger(Timer.class);

	private static final int MAX_CAPACITY = 64 * 1024;

	private final TimerConfiguration m_conf;
	private final TimeoutList m_list;
	private ReentrantLock[] m_locks;
	private BiListNode<TimeoutEvent<?>>[] m_wheel;
	private int m_mask;

	private IScheduler m_scheduler;
	private ScheduledFuture<?> m_sf;
	private boolean m_started;

	// The hand that points to the current timeout list, nodes of which
	// are between m_wheel[m_hand] and m_wheel[m_hand + 1].
	private int m_hand;

	public Timer(TimerConfiguration conf) {
		if (conf == null)
			throw new NullPointerException();

		final int capacity = capacity(conf.wheelSize());
		final ReentrantLock[] locks = new ReentrantLock[capacity];
		// one more as a tail node for conveniently iterating the timeout list
		@SuppressWarnings("unchecked")
		final BiListNode<TimeoutEvent<?>>[] wheel = (BiListNode<TimeoutEvent<?>>[]) new BiListNode<?>[capacity + 1];
		final TimeoutList list = new TimeoutList();
		for (int i = 0; i < capacity; ++i) {
			locks[i] = new ReentrantLock();
			// create sentinel nodes
			wheel[i] = list.addLast(null);
		}
		wheel[capacity] = list.addLast(null);

		m_list = list;
		m_conf = conf;
		m_locks = locks;
		m_wheel = wheel;
		m_mask = capacity - 1;
	}

	@Override
	public synchronized void start() {
		if (m_started)
			return;

		m_started = true;

		if (m_mask == 0) // wheelSize <= 0
			return;

		startTicking();
	}

	@Override
	public synchronized void stop() {
		if (!m_started)
			return;

		m_started = false;
		stopTicking();
	}

	@Override
	public long tickTime() {
		return m_conf.tickTime();
	}

	@Override
	public int wheelSize() {
		return m_conf.wheelSize();
	}

	@Override
	public ITimerConfiguration wheelSize(int wheelSize) {
		m_conf.wheelSize(wheelSize);
		return this;
	}

	@Override
	public synchronized void apply() {
		final int capacity = capacity(m_conf.wheelSize());
		int oldCapacity = m_locks.length;
		if (capacity == oldCapacity)
			return;

		if (capacity < oldCapacity) {
			if (capacity < 2)
				stopTicking();
			return;
		}

		final ReentrantLock[] locks = new ReentrantLock[capacity];
		System.arraycopy(m_locks, 0, locks, 0, oldCapacity);
		@SuppressWarnings("unchecked")
		final BiListNode<TimeoutEvent<?>>[] wheel = (BiListNode<TimeoutEvent<?>>[]) new BiListNode<?>[capacity + 1];
		System.arraycopy(m_wheel, 0, wheel, 0, oldCapacity + 1);
		final TimeoutList list = m_list;
		do {
			locks[oldCapacity] = new ReentrantLock();
			wheel[++oldCapacity] = list.addLast(null);
		} while (oldCapacity < capacity);

		m_locks = locks;
		m_wheel = wheel;
		m_mask = capacity - 1;
		if (m_started)
			startTicking();
	}

	@Override
	public ITimerConfiguration configuration() {
		return this;
	}

	Timer scheduler(IScheduler scheduler) {
		m_scheduler = scheduler;
		return this;
	}

	@Override
	public <S> ITimeoutNotifier<S> createNotifier(S subject) {
		return new TimeoutNotifier<>(subject, this);
	}

	@Override
	public void run() {
		final int hand = m_hand;
		final int nextHand = hand + 1;
		final BiListNode<TimeoutEvent<?>>[] wheel = m_wheel;
		final BiListNode<TimeoutEvent<?>> begin = wheel[hand];
		final BiListNode<TimeoutEvent<?>> end = wheel[nextHand];
		BiListNode<TimeoutEvent<?>> node;
		while ((node = begin.next()) != end) {
			final TimeoutNotifier<?> notifier;
			final TimeoutEvent<?> event = node.get();

			// If this node has been cancelled, just skip.
			// Otherwise, go ahead.
			if (event != null && (notifier = event.notifier()) != null)
				// Passing "hand" for checking the notifier is still in this
				// same timeout sublist. Otherwise it may be cancelled or
				// rescheduled, and needs to be skipped.
				notifier.onTimeout(hand);
		}

		// tick
		m_hand = getEffectiveIndex(nextHand);
	}

	void schedule(TimeoutNotifier<?> notifier, int timeout) {
		final TimeoutEvent<?> event = TimeoutEvent.get(notifier, timeout);
		final int index = getEffectiveIndex(m_hand + timeout);
		event.index(index);
		notifier.setNode(m_list.syncInsertAfter(m_wheel[index], event, lock(index)));
	}

	void reschedule(TimeoutNotifier<?> notifier, int timeout) {
		final BiListNode<TimeoutEvent<?>> node = notifier.getNode();
		final TimeoutEvent<?> event = node.get();
		final ReentrantLock lock1 = lock(event.index());

		// dest list
		final int index = getEffectiveIndex(m_hand + timeout);
		event.index(index);
		final BiListNode<TimeoutEvent<?>> destHead = m_wheel[index];
		final ReentrantLock lock2 = lock(index);

		m_list.syncMoveAfter(destHead, node, lock1, lock2);
	}

	void cancel(TimeoutNotifier<?> notifier) {
		final BiListNode<TimeoutEvent<?>> node = notifier.getNode();
		notifier.clearNode();
		TimeoutEvent<?> event = node.get();
		final ReentrantLock lock = lock(event.index());
		event = m_list.syncRemove(node, lock);

		// release the timeout event
		event.release();
	}

	void fireTimeout(TimeoutNotifier<?> notifier) {
		final BiListNode<TimeoutEvent<?>> node = notifier.getNode();
		notifier.clearNode();
		final ReentrantLock lock = lock(m_hand);
		final TimeoutEvent<?> event = m_list.syncRemove(node, lock);

		final Executor executor = notifier.getExecutor();
		if (executor != null) {
			try {
				executor.execute(event);
				return;
			} catch (Throwable t) {
				c_logger.warn("Failed to execute timeout delivery. Will use tick thread to execute.", t);
			}
		}

		event.run();
	}

	private void startTicking() {
		if (m_sf == null)
			m_sf = m_scheduler.scheduleAtFixedRate(this, 0L, m_conf.tickTime(), TimeUnit.MILLISECONDS);
	}

	private void stopTicking() {
		final ScheduledFuture<?> sf = m_sf;
		if (sf != null) {
			sf.cancel(false);
			m_sf = null;
		}
	}

	private int capacity(int wheelSize) {
		int capacity = 1 << (32 - Integer.numberOfLeadingZeros(wheelSize));
		if (capacity > MAX_CAPACITY)
			capacity = MAX_CAPACITY;
		return capacity;
	}

	private ReentrantLock lock(int index) {
		return m_locks[index];
	}

	private int getEffectiveIndex(int index) {
		return (index & m_mask);
	}
}
