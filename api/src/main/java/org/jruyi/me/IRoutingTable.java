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

package org.jruyi.me;

/**
 * A collection of {@link IRouteSet}s.
 * 
 * @see IRoute
 * @see IRouteSet
 */
public interface IRoutingTable {

	/**
	 * Gets an existing {@code IRouteSet} object or creates a new one if not
	 * exists.
	 * 
	 * @param from
	 *            the source endpoint ID
	 * @return the {@code IRouteSet} object
	 */
	public IRouteSet getRouteSet(String from);

	/**
	 * Gets the existing {@code IRouteSet} object whose source endpoint ID is
	 * the specified {@code from}.
	 * 
	 * @param from
	 *            the source endpoint ID
	 * @return the {@code IRouteSet} object or {@code null} if not found
	 */
	public IRouteSet queryRouteSet(String from);

	/**
	 * Gets all the {@code IRouteSet} objects in the routing table.
	 * 
	 * @return all the existing {@code IRouteSet} objects, or an empty array if
	 *         there aren't any
	 */
	public IRouteSet[] getAllRouteSets();
}
