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
 * A {@code ITimeoutAdmin} is used to create notifiers. The configuration
 * changes are allowed to apply on the fly.
 * <p>
 * For delivering timeout events, a thread pool is created by Timeout Admin.
 * </p>
 * 
 * @since 2.2
 */
public interface ITimeoutAdmin extends org.jruyi.timeoutadmin.ITimeoutAdmin {

	/**
	 * This interface defines the methods to configure a TimeoutAdmin.
	 * 
	 * @since 2.2
	 */
	interface IConfiguration {

		/**
		 * Sets {@code true} to allow core threads to time out, otherwise sets
		 * {@code false}.
		 * 
		 * @param allowCoreThreadTimeOut
		 *            indicates whether to allow core threads to time out
		 * @return this configuration object
		 */
		IConfiguration allowCoreThreadTimeOut(boolean allowCoreThreadTimeOut);

		/**
		 * Sets the core size of the thread pool that is used to deliver timeout
		 * events. The default core size is 1.
		 * 
		 * @param corePoolSize
		 *            the core pool size to set
		 * @return this configuration object
		 * @throws IllegalArgumentException
		 *             if the specified {@code corePoolSize} is negative
		 */
		IConfiguration corePoolSize(int corePoolSize);

		/**
		 * Sets the maximum size of the thread pool that is used to deliver
		 * timeout events. The default maximum size is 4.
		 * 
		 * @param maxPoolSize
		 *            the maximum pool size to set
		 * @return this configuration object
		 * @throws IllegalArgumentException
		 *             if the specified {@code maxPoolSize} is non-positive
		 */
		IConfiguration maxPoolSize(int maxPoolSize);

		/**
		 * Sets the time in seconds to keep idle threads alive. The default time
		 * is 10 seconds.
		 * 
		 * @param keepAliveTimeInSeconds
		 *            the time in seconds to set
		 * @return this configuration object
		 * @throws IllegalArgumentException
		 *             if the specified {@code keepAliveTimeInSeconds} is
		 *             negative
		 */
		IConfiguration keepAliveTimeInSeconds(int keepAliveTimeInSeconds);

		/**
		 * Sets the capacity of the queue used by the thread pool.
		 * <p>
		 * If the specified {@code queueCapacity} is negative, an unlimited
		 * queue will be created.
		 * </p>
		 * <p>
		 * If the specified {@code queueCapacity} is 0, a
		 * {@code SynchronousQueue} will be created.
		 * </p>
		 * 
		 * @param queueCapacity
		 *            the capacity of the queue to set
		 * @return this configuration object
		 */
		IConfiguration queueCapacity(int queueCapacity);

		/**
		 * Sets the time in seconds to wait for the associated thread pool to
		 * terminate.
		 * 
		 * @param terminationWaitTimeInSeconds
		 *            the time to set
		 * @return this configuration object
		 */
		IConfiguration terminationWaitTimeInSeconds(int terminationWaitTimeInSeconds);

		/**
		 * Returns whether core threads are allowed to time out.
		 * 
		 * @return true if core threads are allowed to time out, otherwise false
		 */
		boolean allowCoreThreadTimeOut();

		/**
		 * Returns the core pool size.
		 * 
		 * @return the core pool size
		 */
		int corePoolSize();

		/**
		 * Returns the maximum pool size.
		 * 
		 * @return the maximum pool size
		 */
		int maxPoolSize();

		/**
		 * Returns the time in seconds to keep idle threads alive.
		 * 
		 * @return the time in seconds to keep idle threads alive
		 */
		int keepAliveTimeInSeconds();

		/**
		 * Returns the capacity of the queue.
		 * 
		 * @return the capacity of the queue
		 */
		int queueCapacity();

		/**
		 * Returns the time in seconds to wait for the thread pool to terminate.
		 * 
		 * @return the time in seconds to wait for the thread pool to terminate
		 */
		int terminationWaitTimeInSeconds();

		/**
		 * Applies this configuration to the associated TimeoutAdmin service.
		 */
		void apply();
	}

	/**
	 * Returns the configuration of this TimeoutAdmin.
	 * 
	 * @return the configuration
	 */
	IConfiguration configuration();
}
