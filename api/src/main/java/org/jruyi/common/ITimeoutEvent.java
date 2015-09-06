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
 * An event indicating that time is out.
 * 
 * @param <S>
 *            Type of subject
 *
 * @since 2.3
 */
public interface ITimeoutEvent<S> {

	/**
	 * Returns the subject of this event.
	 *
	 * @return the subject
	 */
	S subject();

	/**
	 * Returns the timeout in seconds.
	 *
	 * @return the timeout in seconds
	 */
	int timeout();
}
