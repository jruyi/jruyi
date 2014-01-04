/**
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
package org.jruyi.workshop;

/**
 * Service for profiling its bound workshop service.
 * 
 * <p>
 * All statistics are effective only when profiling is started and will be
 * dropped when profiling is stopped.
 * 
 * @see IWorkshop
 * @since 1.1
 */
public interface IWorkshopProfiler {

	/**
	 * Returns the ID of this workshop profiler.
	 * 
	 * @return the ID of this workshop profiler
	 */
	public long getProfilerId();

	/**
	 * Start profiling the bound workshop service.
	 */
	public void startProfiling();

	/**
	 * Stop profiling the bound workshop service.
	 */
	public void stopProfiling();

	/**
	 * Tests whether profiling is started.
	 * 
	 * @return {@code true} if profiling is started, otherwise {@code false}
	 */
	public boolean isProfiling();

	/**
	 * Returns the corePoolSize of the bound workshop.
	 * 
	 * @return the corePoolSize of the bound workshop
	 */
	public int getCorePoolSize();

	/**
	 * Returns the maxPoolSize of the bound workshop.
	 * 
	 * @return the maxPoolSize of the bound workshop
	 */
	public int getMaxPoolSize();

	/**
	 * Returns the keepAliveTime of the bound workshop.
	 * 
	 * @return the keepAliveTime of the bound workshop
	 */
	public int getKeepAliveTime();

	/**
	 * Returns the queueCapacity of the bound workshop.
	 * 
	 * @return the queueCapacity of the bound workshop
	 */
	public int getQueueCapacity();

	/**
	 * Returns the approximate number of threads that are actively executing
	 * tasks.
	 * 
	 * @return the current pool size
	 */
	public int getCurrentPoolSize();

	/**
	 * Returns the current number of requests in queue.
	 * 
	 * @return the current queue length;
	 */
	public int getCurrentQueueLength();

	/**
	 * Returns the threadPrefix of the bound workshop.
	 * 
	 * @return the threadPrefix of the bound workshop
	 */
	public String getThreadPrefix();

	/**
	 * Returns the approximate number of finished tasks since the latest
	 * profiling started.
	 * 
	 * @return the number of requests retired
	 */
	public long getNumberOfRequestsRetired();

	/**
	 * Returns the rate of the request retirement.
	 * 
	 * @return the rate of the request retirement
	 */
	public double getRequestPerSecondRetirementRate();

	/**
	 * Returns the average service time.
	 * 
	 * @return the average service time
	 */
	public double getAverageServiceTime();

	/**
	 * Returns the average time a request waiting in pool.
	 * 
	 * @return the average time a request waiting in pool
	 */
	public double getAverageTimeWaitingInPool();

	/**
	 * Returns the average time to finish a request.
	 * 
	 * @return the average response time
	 */
	public double getAverageResponseTime();

	/**
	 * Returns the estimated average number of active requests.
	 * 
	 * @return the estimated average number of active requests
	 */
	public double getEstimatedAverageNumberOfActiveRequests();

	/**
	 * Returns the ratio of waiting time to response time.
	 * 
	 * @return the ratio of dead time to response time
	 */
	public double getRatioOfDeadTimeToResponseTime();

	/**
	 * Returns the ratio of active requests to CPU core count.
	 * 
	 * @return the ratio of active requests to CPU core count
	 */
	public double getRatioOfActiveRequestsToCoreCount();
}
