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

package org.jruyi.io;

/**
 * An output holder for a filter to pass the output to the next filter as an
 * input.
 */
public interface IFilterOutput {

	/**
	 * Adds the specified {@code output} to the output queue as the final output
	 * corresponding to the input from previous filter. All the output elements
	 * in the queue will be passed to the next filter one by one.
	 * 
	 * @param output
	 *            the final output corresponding to the input from previous
	 *            filter
	 * @throws NullPointerException
	 *             if the specified {@code out} is null
	 */
	void add(Object output);

	/**
	 * Used in {@link IFilter#onMsgDepart} to indicate if there will be more
	 * outputs corresponding to the input from previous filter after
	 * {@link IFilter#onMsgDepart} returns.
	 * 
	 * @param more
	 *            true if there will be more outputs
	 * @since 2.1
	 */
	void more(boolean more);
}
