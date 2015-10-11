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

import org.jruyi.common.BiListNode;
import org.jruyi.common.ITimeoutEvent;
import org.jruyi.common.ITimeoutListener;
import org.jruyi.common.ITimeoutNotifier;
import org.jruyi.common.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TimeoutNotifier<S> implements ITimeoutNotifier<S>, ITimeoutEvent<S>, Runnable {

	private static final Logger c_logger = LoggerFactory.getLogger(TimeoutNotifier.class);

	private final S m_subject;
	private final Timer m_ta;
	private int m_index;
	private int m_timeout;
	private BiListNode<TimeoutNotifier<?>> m_node;
	private volatile ITimeoutListener<S> m_listener;
	private State m_state = State.UNSCHEDULED;
	private Executor m_executor;

	TimeoutNotifier(S subject, Timer ta) {
		m_subject = subject;
		m_ta = ta;
	}

	enum State {

		UNSCHEDULED {
			@Override
			public boolean schedule(TimeoutNotifier<?> notifier, int timeout) {
				notifier.timer().schedule(notifier, timeout);
				notifier.changeState(SCHEDULED);
				return true;
			}

			@Override
			public boolean cancel(TimeoutNotifier<?> notifier) {
				return true;
			}

			@Override
			boolean onTimeout(TimeoutNotifier<?> notifier) {
				return false;
			}

			@Override
			public int state() {
				return ITimeoutNotifier.UNSCHEDULED;
			}
		},
		SCHEDULED {
			@Override
			public boolean schedule(TimeoutNotifier<?> notifier, int timeout) {
				return notifier.timer().reschedule(notifier, timeout);
			}

			@Override
			public boolean cancel(TimeoutNotifier<?> notifier) {
				if (notifier.timer().cancel(notifier)) {
					notifier.changeState(UNSCHEDULED);
					return true;
				} else
					return false;
			}

			@Override
			boolean onTimeout(TimeoutNotifier<?> notifier) {
				notifier.changeState(TIMEDOUT);
				return true;
			}

			@Override
			public int state() {
				return ITimeoutNotifier.SCHEDULED;
			}
		},
		TIMEDOUT {
			@Override
			public boolean schedule(TimeoutNotifier<?> notifier, int timeout) {
				return false;
			}

			@Override
			public boolean cancel(TimeoutNotifier<?> notifier) {
				return false;
			}

			@Override
			boolean onTimeout(TimeoutNotifier<?> notifier) {
				throw new IllegalStateException();
			}

			@Override
			public int state() {
				return ITimeoutNotifier.TIMEDOUT;
			}
		};

		abstract boolean schedule(TimeoutNotifier<?> notifier, int timeout);

		abstract boolean cancel(TimeoutNotifier<?> notifier);

		abstract boolean onTimeout(TimeoutNotifier<?> notifier);

		abstract int state();
	}

	@Override
	public S subject() {
		return m_subject;
	}

	@Override
	public int timeout() {
		return m_timeout;
	}

	@Override
	public void run() {
		if (!onTimeout())
			return;

		final ITimeoutListener<S> listener = m_listener;
		if (listener == null)
			return;

		try {
			listener.onTimeout(this);
		} catch (Throwable t) {
			c_logger.error(StrUtil.join("Error on timeout: ", subject()), t);
		}
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

		return m_state.schedule(this, timeout);
	}

	@Override
	public boolean cancel() {
		return m_state.cancel(this);
	}

	@Override
	public void listener(ITimeoutListener<S> listener) {
		m_listener = listener;
	}

	@Override
	public void executor(Executor executor) {
		m_executor = executor;
	}

	void timeout(int timeout) {
		m_timeout = timeout;
	}

	boolean onTimeout() {
		return m_state.onTimeout(this);
	}

	void index(int index) {
		m_index = index;
	}

	int index() {
		return m_index;
	}

	Executor getExecutor() {
		return m_executor;
	}

	// Set when scheduled
	void node(BiListNode<TimeoutNotifier<?>> biListNode) {
		m_node = biListNode;
	}

	// Cleared when cancelled or timeout
	void clearNode() {
		m_node = null;
	}

	Timer timer() {
		return m_ta;
	}

	BiListNode<TimeoutNotifier<?>> node() {
		return m_node;
	}

	void changeState(State state) {
		m_state = state;
	}
}
