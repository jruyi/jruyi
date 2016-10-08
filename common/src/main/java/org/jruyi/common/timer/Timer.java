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
	private BiListNode<TimeoutNotifier<?>>[] m_wheel;
	private int m_mask;

	private IScheduler m_scheduler;
	private ScheduledFuture<?> m_sf;
	private int m_count;

	// The hand that points to the current timeout list, nodes of which
	// are between m_wheel[m_hand] and m_wheel[m_hand + 1].
	private volatile int m_hand;

	public Timer(TimerConfiguration conf) {
		if (conf == null)
			throw new NullPointerException();

		final int capacity = capacity(conf.wheelSize());
		final ReentrantLock[] locks = new ReentrantLock[capacity];
		// one more as a tail node for conveniently iterating the timeout list
		@SuppressWarnings("unchecked")
		final BiListNode<TimeoutNotifier<?>>[] wheel = (BiListNode<TimeoutNotifier<?>>[]) new BiListNode<?>[capacity
				+ 1];
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
		if (m_count == 0) {
			if (m_mask != 0) // wheelSize <= 0
				startTicking();
		}
		++m_count;
	}

	@Override
	public synchronized void stop() {
		if (m_count == 0)
			return;

		if (--m_count == 0)
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
		final BiListNode<TimeoutNotifier<?>>[] wheel = (BiListNode<TimeoutNotifier<?>>[]) new BiListNode<?>[capacity
				+ 1];
		System.arraycopy(m_wheel, 0, wheel, 0, oldCapacity + 1);
		final TimeoutList list = m_list;
		do {
			locks[oldCapacity] = new ReentrantLock();
			wheel[++oldCapacity] = list.addLast(null);
		} while (oldCapacity < capacity);

		m_locks = locks;
		m_wheel = wheel;
		m_mask = capacity - 1;
		if (m_count > 0)
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
		final BiListNode<TimeoutNotifier<?>>[] wheel = m_wheel;
		final BiListNode<TimeoutNotifier<?>> begin = wheel[hand];
		final BiListNode<TimeoutNotifier<?>> end = wheel[nextHand];
		final TimeoutList list = m_list;
		final ReentrantLock lock = lock(hand);
		for (;;) {
			final TimeoutNotifier<?> notifier;
			final BiListNode<TimeoutNotifier<?>> node;
			lock.lock();
			try {
				node = begin.next();
				if (node == end)
					break;
				list.remove(node);
				notifier = node.get();
				notifier.clearNode();
			} finally {
				lock.unlock();
			}

			final Executor executor = notifier.getExecutor();
			if (executor != null) {
				try {
					executor.execute(notifier);
					continue;
				} catch (Throwable t) {
					c_logger.warn("Failed to execute timeout delivery. Will use tick thread to execute.", t);
				}
			}
			notifier.run();
		}

		// tick
		m_hand = getEffectiveIndex(nextHand);
	}

	void schedule(TimeoutNotifier<?> notifier, int timeout) {
		final BiListNode<TimeoutNotifier<?>> newNode = BiListNode.create();
		notifier.timeout(timeout);
		newNode.set(notifier);
		notifier.node(newNode);
		final int index = getEffectiveIndex(m_hand + timeout);
		notifier.index(index);
		final BiListNode<TimeoutNotifier<?>> node = m_wheel[index];
		final ReentrantLock lock = lock(index);
		lock.lock();
		try {
			m_list.insertAfter(node, newNode);
		} finally {
			lock.unlock();
		}
	}

	boolean reschedule(TimeoutNotifier<?> notifier, int timeout) {
		final TimeoutList list = m_list;

		final ReentrantLock lockSrc = lock(notifier.index());
		final BiListNode<TimeoutNotifier<?>> node;
		lockSrc.lock();
		try {
			node = notifier.node();
			if (node == null)
				return false;
			list.remove(node);
		} finally {
			lockSrc.unlock();
		}

		// dest list
		final int index = getEffectiveIndex(m_hand + timeout);
		notifier.index(index);
		final BiListNode<TimeoutNotifier<?>> destHead = m_wheel[index];
		final ReentrantLock lockDst = lock(index);
		lockDst.lock();
		try {
			list.insertAfter(destHead, node);
		} finally {
			lockDst.unlock();
		}
		return true;
	}

	boolean cancel(TimeoutNotifier<?> notifier) {
		final BiListNode<TimeoutNotifier<?>> node;
		final ReentrantLock lock = lock(notifier.index());
		lock.lock();
		try {
			node = notifier.node();
			if (node == null)
				return false;
			m_list.remove(node);
		} finally {
			lock.unlock();
		}
		notifier.clearNode();
		node.close();
		return true;
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
