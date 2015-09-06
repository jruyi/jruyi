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

package org.jruyi.common;

/**
 * A timer service backed by a flat timing wheel.
 * 
 * @since 2.3
 */
public interface ITimer {

	/**
	 * Returns the configuration.
	 * 
	 * @return the configuration
	 */
	ITimerConfiguration configuration();

	/**
	 * Creates a timeout notifier with the specified {@code subject}.
	 *
	 * @param <S>
	 *            Type of subject
	 * 
	 * @param subject
	 *            the subject of the timeout event to be sent by this notifier
	 * @return a notifier object
	 */
	<S> ITimeoutNotifier<S> createNotifier(S subject);

	/**
	 * Starts this timer.
	 */
	void start();

	/**
	 * Stops this timer.
	 */
	void stop();
}
