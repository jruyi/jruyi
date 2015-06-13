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

package org.jruyi.tpe;

/**
 * Service for profiling executor service.
 *
 * <p>
 * All statistics is effective only when profiling is started and will be
 * dropped when profiling is stopped.
 *
 * @since 2.0
 */
public interface IExecutorProfiler {

	/**
	 * Start profiling the executor service.
	 */
	void startProfiling();

	/**
	 * Stop profiling the executor service.
	 */
	void stopProfiling();

	/**
	 * Tests whether profiling is started.
	 *
	 * @return {@code true} if profiling is started, otherwise {@code false}
	 */
	boolean isProfiling();

	/**
	 * Returns the corePoolSize of the executor.
	 *
	 * @return the corePoolSize of the executor
	 */
	int getCorePoolSize();

	/**
	 * Returns the maxPoolSize of the executor.
	 *
	 * @return the maxPoolSize of the executor
	 */
	int getMaxPoolSize();

	/**
	 * Returns the keepAliveTime of the executor.
	 *
	 * @return the keepAliveTime of the executor
	 */
	int getKeepAliveTime();

	/**
	 * Returns the queueCapacity of the executor.
	 *
	 * @return the queueCapacity of the executor
	 */
	int getQueueCapacity();

	/**
	 * Returns the approximate number of threads that are actively executing
	 * tasks.
	 *
	 * @return the current pool size
	 */
	int getCurrentPoolSize();

	/**
	 * Returns the current number of requests in queue.
	 *
	 * @return the current queue length;
	 */
	int getCurrentQueueLength();

	/**
	 * Returns the approximate number of finished tasks since the latest
	 * profiling started.
	 *
	 * @return the number of requests retired
	 */
	long getNumberOfRequestsRetired();

	/**
	 * Returns the rate of the request retirement.
	 *
	 * @return the rate of the request retirement
	 */
	double getRequestPerSecondRetirementRate();

	/**
	 * Returns the average service time.
	 *
	 * @return the average service time
	 */
	double getAverageServiceTime();

	/**
	 * Returns the average time a request waiting in pool.
	 *
	 * @return the average time a request waiting in pool
	 */
	double getAverageTimeWaitingInPool();

	/**
	 * Returns the average time to finish a request.
	 *
	 * @return the average response time
	 */
	double getAverageResponseTime();

	/**
	 * Returns the estimated average number of active requests.
	 *
	 * @return the estimated average number of active requests
	 */
	double getEstimatedAverageNumberOfActiveRequests();

	/**
	 * Returns the ratio of waiting time to response time.
	 *
	 * @return the ratio of dead time to response time
	 */
	double getRatioOfDeadTimeToResponseTime();

	/**
	 * Returns the ratio of active requests to CPU core count.
	 *
	 * @return the ratio of active requests to CPU core count
	 */
	double getRatioOfActiveRequestsToCoreCount();
}
