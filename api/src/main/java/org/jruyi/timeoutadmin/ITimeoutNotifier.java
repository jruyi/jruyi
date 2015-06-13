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

package org.jruyi.timeoutadmin;

import java.util.concurrent.Executor;

/**
 * A {@code ITimeoutNotifier} is used to schedule a timeout notification of the
 * interested <i>subject</i>. It has 4 states in all. They are Unscheduled,
 * Scheduled, TimedOut and Closed.
 * 
 * <p>
 * The state transitions are listed below.
 * 
 * <pre>
 * [Unscheduled] --(schedule)-&gt; [Scheduled]
 * [Scheduled] --(cancel)-&gt; [Unscheduled]
 * [Scheduled] --(timeout)-&gt; [TimedOut]
 * [TimedOut] --(reset)-&gt; [Unscheduled]
 * [Scheduled | Unscheduled | TimedOut] --(close)-&gt; [Closed]
 * </pre>
 * 
 * <p>
 * When {@code ITimeoutNotifier} is created, the initial state is Unscheduled.
 * Except close event, when 2 more events occur concurrently, only the one
 * winning the lock will be taken. The others will fail fast.
 * 
 * <p>
 * When {@code ITimeoutNotifier} goes into state TimedOut, <i>schedule</i>/
 * <i>cancel</i> will not work until it gets <i>reset</i>.
 * 
 * <p>
 * After {@code ITimeoutNotifier} closes, it will not work anymore.
 */
public interface ITimeoutNotifier extends AutoCloseable {

	/**
	 * An {@code int} value representing Unscheduled state.
	 */
	int UNSCHEDULED = 0x01;
	/**
	 * An {@code int} value representing Scheduled state.
	 */
	int SCHEDULED = 0x02;
	/**
	 * An {@code int} value representing Timedout state.
	 */
	int TIMEDOUT = 0x04;
	/**
	 * An {@code int} value representing Closed state.
	 */
	int CLOSED = 0x08;

	/**
	 * Returns the subject this notifier concerns.
	 * 
	 * @return the subject
	 */
	Object getSubject();

	/**
	 * Returns the current state of this notifier.
	 * 
	 * @return the current state of this notifier
	 */
	int state();

	/**
	 * Schedules a notification to be sent out in {@code timeout} seconds. The
	 * previous schedule will be dropped.
	 * 
	 * @param timeout
	 *            time in seconds in which the notifier will be sent
	 * @return false if this notifier timed out or is closed, otherwise true
	 * @throws IllegalArgumentException
	 *             if {@code timeout} is not positive
	 */
	boolean schedule(int timeout);

	/**
	 * Cancels the notifier.
	 * 
	 * @return false if either timeout or closed, otherwise true
	 */
	boolean cancel();

	/**
	 * Resets this notifier to be able to be scheduled again if it timed out.
	 * 
	 * @return true if this notifier timed out, otherwise false
	 */
	boolean reset();

	/**
	 * Closes this notifier.
	 */
	void close();

	/**
	 * Sets the listener that is interested in the notification.
	 * 
	 * @param listener
	 *            the notification receiver
	 */
	void setListener(ITimeoutListener listener);

	/**
	 * Sets the executor that is used to deliver timeout notifications from this
	 * notifier.
	 * 
	 * @param executor
	 *            the executor to set
	 * @since 2.0
	 */
	void setExecutor(Executor executor);
}
