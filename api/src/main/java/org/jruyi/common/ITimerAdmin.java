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
 * A service for creating scalable timers backed by timing wheel algorithm.
 * 
 * @since 2.3
 */
public interface ITimerAdmin {

	/**
	 * Returns a timer with the specified {@code wheelSize} and one second as
	 * the tick time.
	 * 
	 * <p>
	 * It behaves exactly the same as
	 * 
	 * <pre>
	 * createTimer(wheelSize, 1000L);
	 * </pre>
	 * 
	 * @param wheelSize
	 *            the minimum size of the timing wheel to create
	 * @return a timer
	 * @throws IllegalArgumentException
	 *             if {@code wheelSize} is negative
	 */
	ITimer createTimer(int wheelSize);

	/**
	 * Returns a timer with the given {@code wheelSize} and {@code tickTime}.
	 *
	 * @param wheelSize
	 *            size of the timing wheel to create
	 * @param tickTime
	 *            milliseconds per tick
	 * @return a timer
	 * @throws IllegalArgumentException
	 *             if {@code wheelSize} is negative or {@code tickTime} is not
	 *             positive
	 */
	ITimer createTimer(int wheelSize, long tickTime);
}
