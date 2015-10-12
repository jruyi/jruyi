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
 * A scheduler is used to schedule tasks to run at a certain time or run
 * periodically. It allows to apply the configuration changes on the fly.
 * 
 * @since 2.2
 */
public interface IScheduler extends org.jruyi.common.IScheduler {

	/**
	 * This interface defines the methods to configure a scheduler.
	 * 
	 * @since 2.2
	 */
	interface IConfiguration {

		/**
		 * Sets the number of threads that the associated scheduler uses to run
		 * tasks.
		 * 
		 * @param numberOfThreads
		 *            the number of thread to set
		 * @return this configuration object
		 * @throws IllegalArgumentException
		 *             if {@code numberOfThreads} is non-positive.
		 */
		IConfiguration numberOfThreads(int numberOfThreads);

		/**
		 * Sets the time in seconds to wait on termination of the associated
		 * scheduler service.
		 * 
		 * @param terminationWaitTimeInSeconds
		 *            the time to set
		 * @return this configuration object
		 */
		IConfiguration terminationWaitTimeInSeconds(int terminationWaitTimeInSeconds);

		/**
		 * Returns the number of threads used to run tasks.
		 * 
		 * @return the number of threads
		 */
		int numberOfThreads();

		/**
		 * Returns the time in seconds to wait on termination of the associated
		 * scheduler service.
		 * 
		 * @return the time to wait
		 */
		int terminationWaitTimeInSeconds();

		/**
		 * Applies this configuration to the associated scheduler service.
		 */
		void apply();
	}

	/**
	 * Returns the configuration of this scheduler.
	 * 
	 * @return the configuration
	 */
	IConfiguration configuration();

	/**
	 * Starts this scheduler.
	 *
	 * @since 2.4
	 */
	void start();

	/**
	 * Stops this scheduler.
	 *
	 * @since 2.4
	 */
	void stop();
}
