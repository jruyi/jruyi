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

package org.jruyi.core;

import org.jruyi.io.IFilter;

/**
 * A filter chain is a container of {@link IFilter}s that process data in
 * series. The output of the previous filter is the input of the next one.
 * 
 * For inbound, data flows from the first filter to the last one; for outbound,
 * data flows from the last filter to the first one.
 * 
 * @since 2.2
 */
public interface IFilterChain {

	/**
	 * Adds the specified {@code filter} with the specified {@code name} after
	 * the target filter named the specified {@code targetFilterName}.
	 * 
	 * @param targetFilterName
	 *            the name of the target filter
	 * @param name
	 *            the name of the filter to add
	 * @param filter
	 *            the filter to add
	 * @return this filter chain
	 * @throws IllegalArgumentException
	 *             if the specified filter is not found in the chain
	 */
	IFilterChain addAfter(String targetFilterName, String name, IFilter<?, ?> filter);

	/**
	 * Adds the specified {@code filter} with the specified {@code name} before
	 * the target filter named the specified {@code targetFilterName}.
	 * 
	 * @param targetFilterName
	 *            the name of the target filter
	 * @param name
	 *            the name of the filter to add
	 * @param filter
	 *            the filter to add
	 * @return this filter chain
	 * @throws IllegalArgumentException
	 *             if the specified filter is not found in the chain
	 */
	IFilterChain addBefore(String targetFilterName, String name, IFilter<?, ?> filter);

	/**
	 * Adds the specified {@code filter} with the specified {@code name} to the
	 * first of this chain.
	 * 
	 * @param name
	 *            the name of the filter to add
	 * @param filter
	 *            the filter to add
	 * @return this filter chain
	 */
	IFilterChain addFirst(String name, IFilter<?, ?> filter);

	/**
	 * Adds the specified {@code filter} with the specified {@code name} to the
	 * last of this chain.
	 * 
	 * @param name
	 *            the name of the filter to add
	 * @param filter
	 *            the filter to add
	 * @return this filter chain
	 */
	IFilterChain addLast(String name, IFilter<?, ?> filter);

	/**
	 * Replaces the specified filter with the specified {@code newFilter}.
	 * 
	 * @param name
	 *            the name of the filter to be replaced
	 * @param newFilter
	 *            the filter to replace
	 * @return the replaced old filter, or {@code null} if not found
	 */
	IFilter<?, ?> replace(String name, IFilter<?, ?> newFilter);

	/**
	 * Removes the specified filter {@code name} from the chain.
	 * 
	 * @param name
	 *            the name of the filter to be removed
	 * @return the removed filter, or {@code null} if not found
	 */
	IFilter<?, ?> remove(String name);

	/**
	 * Tests if the specified filter {@code name} in this chain.
	 * 
	 * @param name
	 *            the name of the filter to be searched for
	 * @return true if the specified filter {@code name} is found in this chain,
	 *         otherwise false
	 */
	boolean contains(String name);

	/**
	 * Gets the filter called the specified {@code name}.
	 * 
	 * @param name
	 *            the name of the filter to get
	 * @return the filter called {@code name}, or {@code null} if not found
	 */
	IFilter<?, ?> get(String name);

	/**
	 * Empties this filter chain.
	 */
	void clear();
}
