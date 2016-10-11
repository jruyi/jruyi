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
import org.jruyi.common.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Timer {

	private static final Logger c_logger = LoggerFactory.getLogger(Timer.class);

	private final Channel m_channel;
	private final TimingWheel m_wheel;
	private int m_timeout;
	private BiListNode<Timer> m_node;
	private ITimerListener m_listener;
	private State m_state = State.UNSCHEDULED;

	Timer(Channel channel, TimingWheel wheel) {
		m_channel = channel;
		m_wheel = wheel;
	}

	enum State {

		UNSCHEDULED {
			@Override
			public boolean schedule(Timer timer, int timeout) {
				timer.timerWheel().schedule(timer, timeout);
				timer.changeState(SCHEDULED);
				return true;
			}

			@Override
			public boolean cancel(Timer timer) {
				return true;
			}

			@Override
			boolean onTimeout(Timer timer) {
				return false;
			}
		},
		SCHEDULED {
			@Override
			public boolean schedule(Timer timer, int timeout) {
				timer.timerWheel().reschedule(timer, timeout);
				return true;
			}

			@Override
			public boolean cancel(Timer timer) {
				timer.timerWheel().cancel(timer);
				timer.changeState(UNSCHEDULED);
				return true;
			}

			@Override
			boolean onTimeout(Timer timer) {
				timer.changeState(TIMEDOUT);
				return true;
			}
		},
		TIMEDOUT {
			@Override
			public boolean schedule(Timer timer, int timeout) {
				return false;
			}

			@Override
			public boolean cancel(Timer timer) {
				return false;
			}

			@Override
			boolean onTimeout(Timer timer) {
				throw new IllegalStateException();
			}
		};

		abstract boolean schedule(Timer timer, int timeout);

		abstract boolean cancel(Timer timer);

		abstract boolean onTimeout(Timer timer);
	}

	public int timeout() {
		return m_timeout;
	}

	public void listener(ITimerListener listener) {
		m_listener = listener;
	}

	public boolean schedule(int timeout) {
		return m_state.schedule(this, timeout);
	}

	public boolean cancel() {
		return m_state.cancel(this);
	}

	void onTimeout() {
		if (!m_state.onTimeout(this))
			return;
		final ITimerListener listener = m_listener;
		if (listener == null)
			return;

		try {
			listener.onTimeout(m_channel);
		} catch (Throwable t) {
			c_logger.error(StrUtil.join("Error on timeout: ", m_channel), t);
		}
	}

	void timeout(int timeout) {
		m_timeout = timeout;
	}

	void node(BiListNode<Timer> node) {
		m_node = node;
	}

	BiListNode<Timer> node() {
		return m_node;
	}

	TimingWheel timerWheel() {
		return m_wheel;
	}

	void changeState(State state) {
		m_state = state;
	}
}
