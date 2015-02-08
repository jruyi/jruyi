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
 * A route defines a rule for messages to be routed between two end points in
 * one way.
 * <p>
 * If a message is put into the message queue by end point <i>from</i> and its
 * properties match the <i>filter</i>, then it will be routed to end point
 * <i>to</i>.
 * 
 * @see IRouteSet
 * @see IRoutingTable
 */
public interface IRoute {

	/**
	 * Gets the ID of the source end point.
	 * 
	 * @return the ID of the source end point
	 */
	public String from();

	/**
	 * Gets the ID of the destination end point.
	 * 
	 * @return the ID of the destination end point
	 */
	public String to();

	/**
	 * Gets the filter string which defines a rule for filtering messages.
	 * 
	 * @return the filter string
	 */
	public String filter();

	/**
	 * Tests if the filter is the one matched regardlessly by all the messages.
	 * 
	 * @return true if the filter is the one matched regardlessly by all the
	 *         messages, otherwise false
	 */
	public boolean isFilterAll();
}
