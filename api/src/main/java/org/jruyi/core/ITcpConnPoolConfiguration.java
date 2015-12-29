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

/**
 * This interface defines methods to configure a TCP client of type ConnPool or
 * MuxConnPool.
 * 
 * @since 2.2
 */
public interface ITcpConnPoolConfiguration extends ITcpClientConfiguration {

	/**
	 * Returns the timeout in seconds for idle connections.
	 * 
	 * @return the idle timeout in seconds
	 */
	int idleTimeoutInSeconds();

	/**
	 * Sets the timeout in seconds for idle connections. Sets 0 to not allow
	 * idle connections. Sets -1 to never time out.
	 * 
	 * @param idleTimeoutInSeconds
	 *            the idle timeout in seconds to set
	 * @return this configuration
	 * @throws IllegalArgumentException
	 *             if the specified {@code idleTimeoutInSeconds} is less than
	 *             -1, or if it is -1 but allowsCoreConnectionTimeout is true
	 */
	ITcpConnPoolConfiguration idleTimeoutInSeconds(int idleTimeoutInSeconds);

	/**
	 * Returns the core number of connections.
	 * 
	 * @return the core size of the connection pool
	 * @since 2.5
	 */
	int corePoolSize();

	/**
	 * Sets the core number of connections.
	 * 
	 * @param corePoolSize
	 *            the new core size
	 * @return this configuration
	 * @throws IllegalArgumentException
	 *             if the specified {@code corePoolSize} is negative
	 * @since 2.5
	 */
	ITcpConnPoolConfiguration corePoolSize(int corePoolSize);

	/**
	 * Returns the maximum size of the connection pool.
	 * 
	 * @return the maximum size of the connection pool
	 */
	int maxPoolSize();

	/**
	 * Sets the maximum size of the connection pool.
	 * 
	 * @param maxPoolSize
	 *            the maximum pool size to set
	 * @return this configuration
	 * @throws IllegalArgumentException
	 *             if the specified {@code maxPoolSize} is less than 1
	 */
	ITcpConnPoolConfiguration maxPoolSize(int maxPoolSize);

	/**
	 * Returns whether core connections are allowed to time out.
	 *
	 * @return true if core connection are allowed to time out, otherwise false
	 * @since 2.5
	 */
	boolean allowsCoreConnectionTimeout();

	/**
	 * Sets whether core connections should time out or not. If true, then core
	 * connections will time out when idle for {@code idleTimeoutInSeconds()}.
	 * 
	 * @param allowsCoreConnectionTimeout
	 *            true if should time out, otherwise false
	 * @return this configuration
	 * @throws IllegalArgumentException
	 *             if the given {@code allowsCoreConnectionTimeout} is true but
	 *             the current idleTimeoutInSeconds is -1
	 * @since 2.5
	 */
	ITcpConnPoolConfiguration allowsCoreConnectionTimeout(boolean allowsCoreConnectionTimeout);
}
