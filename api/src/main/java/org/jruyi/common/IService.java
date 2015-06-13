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

import java.util.Map;

/**
 * Defines the life cycle methods that a service need implement.
 * <p>
 * A service has five states: Inactive, Stopped, Active, Starting and Stopping.
 * The initial state of a service is <i>Stopped</i>.
 * <p>
 * State transition is as follows:
 * 
 * <pre>
 * {Stopped|Inactive}-(start)-Starting-{(success)-&gt;Active|(fail)-&gt;Inactive}
 * Active-(stop)-Stopping-&gt;Stopped
 * Inactive-(stop)-&gt;Stopped
 * </pre>
 */
public interface IService {

	/**
	 * An {@code int} value representing Inactive state.
	 */
	int INACTIVE = 0x01;
	/**
	 * An {@code int} value representing Stopped state.
	 */
	int STOPPED = 0x02;
	/**
	 * An {@code int} value representing Active state.
	 */
	int ACTIVE = 0x04;
	/**
	 * An {@code int} value representing Starting state.
	 */
	int STARTING = 0x08;
	/**
	 * An {@code int} value representing Stopping state.
	 */
	int STOPPING = 0x10;

	/**
	 * Starts this service.
	 * 
	 * @throws Exception
	 *             thrown if this service failed to start
	 */
	void start() throws Exception;

	/**
	 * Starts this service with the specified {@code options}. It's up to the
	 * service implementation to define its own start options.
	 * 
	 * @param options
	 *            start options
	 * @throws Exception
	 *             thrown if this service failed to start
	 */
	void start(int options) throws Exception;

	/**
	 * Stops this service.
	 */
	void stop();

	/**
	 * Stops this service with the specified {@code options}. It's up to the
	 * service implementation to define its own stop options.
	 * 
	 * @param options
	 *            stop options
	 */
	void stop(int options);

	/**
	 * Updates the properties of this service.
	 * 
	 * @param properties
	 *            the properties to be updated to
	 * @throws Exception
	 *             if any error occurs
	 */
	void update(Map<String, ?> properties) throws Exception;

	/**
	 * Gets the current state of this service.
	 * 
	 * @return the current state of this service
	 */
	int state();
}
