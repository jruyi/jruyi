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
import java.util.concurrent.locks.ReentrantLock;

import org.jruyi.common.BiListNode;
import org.jruyi.common.ITimeoutListener;
import org.jruyi.common.ITimeoutNotifier;
import org.jruyi.common.StrUtil;

final class TimeoutNotifier<S> implements ITimeoutNotifier<S> {

	private final S m_subject;
	private final Timer m_ta;
	private final ReentrantLock m_lock;
	private BiListNode<TimeoutEvent<?>> m_node;
	private ITimeoutListener<S> m_listener;
	private IState m_state = Unscheduled.INST;
	private Executor m_executor;

	TimeoutNotifier(S subject, Timer ta) {
		m_subject = subject;
		m_ta = ta;
		m_lock = new ReentrantLock();
	}

	interface IState {

		boolean schedule(TimeoutNotifier<?> notifier, int timeout);

		boolean cancel(TimeoutNotifier<?> notifier);

		boolean reset(TimeoutNotifier<?> notifier);

		void close(TimeoutNotifier<?> notifier);

		int state();
	}

	static final class Scheduled implements IState {

		public static final IState INST = new Scheduled();

		private Scheduled() {
		}

		@Override
		public boolean schedule(TimeoutNotifier<?> notifier, int timeout) {
			notifier.getTimeoutAdmin().reschedule(notifier, timeout);
			return true;
		}

		@Override
		public boolean cancel(TimeoutNotifier<?> notifier) {
			notifier.getTimeoutAdmin().cancel(notifier);
			notifier.changeState(Unscheduled.INST);
			return true;
		}

		@Override
		public boolean reset(TimeoutNotifier<?> notifier) {
			return false;
		}

		@Override
		public void close(TimeoutNotifier<?> notifier) {
			notifier.getTimeoutAdmin().cancel(notifier);
			notifier.changeState(Closed.INST);
		}

		@Override
		public int state() {
			return SCHEDULED;
		}
	}

	static final class Unscheduled implements IState {

		public static final IState INST = new Unscheduled();

		private Unscheduled() {
		}

		@Override
		public boolean schedule(TimeoutNotifier<?> notifier, int timeout) {
			notifier.getTimeoutAdmin().schedule(notifier, timeout);
			notifier.changeState(Scheduled.INST);
			return true;
		}

		@Override
		public boolean cancel(TimeoutNotifier<?> notifier) {
			return true;
		}

		@Override
		public boolean reset(TimeoutNotifier<?> notifier) {
			return false;
		}

		@Override
		public void close(TimeoutNotifier<?> notifier) {
			notifier.changeState(Closed.INST);
		}

		@Override
		public int state() {
			return UNSCHEDULED;
		}
	}

	static final class TimedOut implements IState {

		public static final IState INST = new TimedOut();

		private TimedOut() {
		}

		@Override
		public boolean schedule(TimeoutNotifier<?> notifier, int timeout) {
			return false;
		}

		@Override
		public boolean cancel(TimeoutNotifier<?> notifier) {
			return false;
		}

		@Override
		public boolean reset(TimeoutNotifier<?> notifier) {
			notifier.changeState(Unscheduled.INST);
			return true;
		}

		@Override
		public void close(TimeoutNotifier<?> notifier) {
			notifier.changeState(Closed.INST);
		}

		@Override
		public int state() {
			return TIMEDOUT;
		}
	}

	static final class Closed implements IState {

		public static final IState INST = new Closed();

		private Closed() {
		}

		@Override
		public boolean cancel(TimeoutNotifier<?> notifier) {
			return false;
		}

		@Override
		public boolean schedule(TimeoutNotifier<?> notifier, int timeout) {
			return false;
		}

		@Override
		public boolean reset(TimeoutNotifier<?> notifier) {
			return false;
		}

		@Override
		public void close(TimeoutNotifier<?> notifier) {
		}

		@Override
		public int state() {
			return CLOSED;
		}
	}

	@Override
	public S subject() {
		return m_subject;
	}

	@Override
	public int state() {
		return m_state.state();
	}

	@Override
	public boolean schedule(int timeout) {
		final int wheelSize = m_ta.wheelSize();
		if (timeout < 1 || timeout > wheelSize)
			throw new IllegalArgumentException(StrUtil.join("Illegal timeout: 0 < ", timeout, " <= ", wheelSize));

		final ReentrantLock lock = m_lock;
		if (!lock.tryLock()) // fail-fast
			return false;

		try {
			return m_state.schedule(this, timeout);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean cancel() {
		final ReentrantLock lock = m_lock;
		if (!lock.tryLock()) // fail-fast
			return false;

		try {
			return m_state.cancel(this);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean reset() {
		final ReentrantLock lock = m_lock;
		if (!lock.tryLock()) // fail-fast
			return false;

		try {
			return m_state.reset(this);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void close() {
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			m_state.close(this);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void listener(ITimeoutListener<S> listener) {
		m_listener = listener;
	}

	@Override
	public void executor(Executor executor) {
		m_executor = executor;
	}

	void onTimeout(int hand) {
		final ReentrantLock lock = m_lock;
		// If the lock cannot be acquired, which means this notifier is being
		// cancelled, rescheduled or closed, just skip.
		if (!lock.tryLock())
			return;

		try {
			// If this notifier is not in the same timeout list,
			// which means it has been cancelled or rescheduled,
			// then skip.
			final TimeoutEvent<?> event = m_node.get();
			if (event == null || hand != event.index())
				return;

			changeState(TimedOut.INST);
			m_ta.fireTimeout(this);
		} finally {
			lock.unlock();
		}
	}

	ITimeoutListener<S> listener() {
		return m_listener;
	}

	Executor getExecutor() {
		return m_executor;
	}

	// Set when scheduled
	void setNode(BiListNode<TimeoutEvent<?>> biListNode) {
		m_node = biListNode;
	}

	// Cleared when cancelled or timeout
	void clearNode() {
		m_node = null;
	}

	Timer getTimeoutAdmin() {
		return m_ta;
	}

	BiListNode<TimeoutEvent<?>> getNode() {
		return m_node;
	}

	void changeState(IState state) {
		m_state = state;
	}
}
