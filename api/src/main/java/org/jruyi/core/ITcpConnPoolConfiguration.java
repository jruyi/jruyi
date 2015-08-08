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
	 *             if the specified {@code idleTimeoutInSeconds} is less than -1
	 */
	ITcpConnPoolConfiguration idleTimeoutInSeconds(int idleTimeoutInSeconds);

	/**
	 * Returns the minimum size of the connection pool.
	 * 
	 * @return the minimum size of the connection pool
	 */
	int minPoolSize();

	/**
	 * Sets the minimum size of the connection pool.
	 * 
	 * @param minPoolSize
	 *            the minimum pool size to set
	 * @return this configuration
	 * @throws IllegalArgumentException
	 *             if the specified {@code minPoolSize} is negative
	 */
	ITcpConnPoolConfiguration minPoolSize(int minPoolSize);

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
}
