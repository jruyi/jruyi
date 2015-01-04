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

package org.jruyi.tpe.internal;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jruyi.common.IDumpable;
import org.jruyi.common.StrUtil;
import org.jruyi.common.StringBuilder;
import org.jruyi.tpe.IExecutorProfiler;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "jruyi.tpe", //
service = { Executor.class, IExecutorProfiler.class }, //
xmlns = "http://www.osgi.org/xmlns/scr/v1.2.0")
public final class ExecutorService implements Executor, IExecutorProfiler, IDumpable {

	private static final Logger c_logger = LoggerFactory.getLogger(ExecutorService.class);

	private static final String P_CORE_POOLSIZE = "corePoolSize";
	private static final String P_MAX_POOLSIZE = "maxPoolSize";
	private static final String P_KEEPALIVE_TIME = "keepAliveTimeInSeconds";
	private static final String P_QUEUE_CAPACITY = "queueCapacity";
	private static final String P_TERM_WAITTIME = "terminationWaitTimeInSeconds";

	private RuyiThreadPoolExecutor m_executor;
	private int m_queueCapacity = 8192;
	private int m_terminationWaitTime = 60;

	@Override
	public int getCorePoolSize() {
		return m_executor.getCorePoolSize();
	}

	@Override
	public int getMaxPoolSize() {
		return m_executor.getMaximumPoolSize();
	}

	@Override
	public int getKeepAliveTime() {
		return (int) m_executor.getKeepAliveTime(TimeUnit.SECONDS);
	}

	@Override
	public int getQueueCapacity() {
		return m_queueCapacity;
	}

	@Override
	public int getCurrentPoolSize() {
		return m_executor.getActiveCount();
	}

	@Override
	public int getCurrentQueueLength() {
		return m_executor.getQueue().size();
	}

	@Override
	public long getNumberOfRequestsRetired() {
		return m_executor.getNumberOfRequestsRetired();
	}

	@Override
	public void startProfiling() {
		m_executor.startProfiling(m_queueCapacity);
	}

	@Override
	public void stopProfiling() {
		m_executor.stopProfiling();
	}

	@Override
	public boolean isProfiling() {
		return m_executor.isProfiling();
	}

	@Override
	public double getRequestPerSecondRetirementRate() {
		return m_executor.getRequestPerSecondRetirementRate();
	}

	@Override
	public double getAverageServiceTime() {
		return m_executor.getAverageServiceTime();
	}

	@Override
	public double getAverageTimeWaitingInPool() {
		return m_executor.getAverageTimeWaitingInPool();
	}

	@Override
	public double getAverageResponseTime() {
		return m_executor.getAverageResponseTime();
	}

	@Override
	public double getEstimatedAverageNumberOfActiveRequests() {
		return m_executor.getEstimatedAverageNumberOfActiveRequests();
	}

	@Override
	public double getRatioOfDeadTimeToResponseTime() {
		return m_executor.getRatioOfDeadTimeToResponseTime();
	}

	@Override
	public double getRatioOfActiveRequestsToCoreCount() {
		return m_executor.getRatioOfActiveRequestsToCoreCount();
	}

	@Override
	public void execute(Runnable command) {
		m_executor.execute(command);
	}

	@Override
	public void dump(StringBuilder builder) {
		final ThreadPoolExecutor executor = m_executor;
		builder.append("{" + P_CORE_POOLSIZE + "=").append(executor.getCorePoolSize())
				.append(", " + P_MAX_POOLSIZE + "=").append(executor.getMaximumPoolSize())
				.append(", " + P_KEEPALIVE_TIME + "=").append(executor.getKeepAliveTime(TimeUnit.SECONDS))
				.append(", " + P_QUEUE_CAPACITY + "=").append(m_queueCapacity).append(", " + P_TERM_WAITTIME + "=")
				.append(m_terminationWaitTime).append('}');
	}

	@Modified
	void modified(Map<String, ?> properties) throws Exception {
		final ThreadPoolExecutor executor = m_executor;
		final int keepAliveTime = getKeepAliveTime(properties, (int) executor.getKeepAliveTime(TimeUnit.SECONDS));
		final int corePoolSize = getCorePoolSize(properties);
		final int maxPoolSize = getMaxPoolSize(properties, corePoolSize);
		final int queueCapacity = getQueueCapacity(properties);
		final int terminationWaitTime = getTerminationWaitTime(properties);

		final int oldQueueCapacity = m_queueCapacity;
		if (queueCapacity != oldQueueCapacity && (queueCapacity >= 0 || oldQueueCapacity >= 0)) {
			m_executor = newExecutor(corePoolSize, maxPoolSize, keepAliveTime, queueCapacity);
			m_queueCapacity = queueCapacity;
			executor.shutdown();
		} else {
			if (corePoolSize > executor.getMaximumPoolSize()) {
				executor.setMaximumPoolSize(maxPoolSize);
				executor.setCorePoolSize(corePoolSize);
			} else {
				executor.setCorePoolSize(corePoolSize);
				executor.setMaximumPoolSize(maxPoolSize);
			}
			executor.setKeepAliveTime(keepAliveTime, TimeUnit.SECONDS);
		}

		m_terminationWaitTime = terminationWaitTime;

		c_logger.info(StrUtil.join("ExecutorService was updated: ", this));
	}

	void activate(Map<String, ?> properties) throws Exception {
		c_logger.info("Activating ExecutorService...");

		final int keepAliveTime = getKeepAliveTime(properties, 10);
		final int corePoolSize = getCorePoolSize(properties);
		final int maxPoolSize = getMaxPoolSize(properties, corePoolSize);
		final int queueCapacity = getQueueCapacity(properties);
		final int terminationWaitTime = getTerminationWaitTime(properties);

		m_executor = newExecutor(corePoolSize, maxPoolSize, keepAliveTime, queueCapacity);
		m_queueCapacity = queueCapacity;
		m_terminationWaitTime = terminationWaitTime;

		c_logger.info(StrUtil.join("ExecutorService was activated: ", this));
	}

	void deactivate() {
		c_logger.info("Deactivating ExecutorService...");

		try {
			m_executor.shutdown();
			if (m_executor.awaitTermination(m_terminationWaitTime, TimeUnit.SECONDS))
				c_logger.debug("TPE executor terminated");
			else
				c_logger.debug("Termination of TPE executor timed out");

			c_logger.info("ExecutorService was deactivated");
		} catch (InterruptedException e) {
			c_logger.warn("Going here is abnormal");
		} catch (Exception e) {
			c_logger.error("ExecutorService Deactivation Error", e);
		} finally {
			m_executor = null;
		}
	}

	private static RuyiThreadPoolExecutor newExecutor(int corePoolSize, int maxPoolSize, long keepAliveTime,
			int queueCapacity) {
		return new RuyiThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS,
				queueCapacity < 0 ? new LinkedBlockingQueue<Runnable>()
						: (queueCapacity > 0 ? new ArrayBlockingQueue<Runnable>(queueCapacity)
								: new SynchronousQueue<Runnable>()), new PooledThreadFactory(),
				new ThreadPoolExecutor.CallerRunsPolicy());
	}

	private static int getCorePoolSize(Map<String, ?> properties) {
		Object v = properties.get(P_CORE_POOLSIZE);
		if (v == null)
			return Runtime.getRuntime().availableProcessors() << 1;
		else
			return (Integer) v;
	}

	private static int getMaxPoolSize(Map<String, ?> properties, int corePoolSize) throws Exception {
		Object v = properties.get(P_MAX_POOLSIZE);
		if (v == null) {
			int maxPoolSize = corePoolSize << 1;
			if (maxPoolSize < 1 || maxPoolSize >= 500)
				maxPoolSize = corePoolSize;
			return maxPoolSize;
		}

		int maxPoolSize = (Integer) v;
		if (maxPoolSize < corePoolSize)
			throw new Exception("Property[" + P_MAX_POOLSIZE + "] cannot be less than Property[" + P_CORE_POOLSIZE
					+ "]");

		return maxPoolSize;
	}

	private static Integer getKeepAliveTime(Map<String, ?> properties, Integer defaultValue) throws Exception {
		final Integer keepAliveTime = (Integer) properties.get(P_KEEPALIVE_TIME);
		if (keepAliveTime == null)
			return defaultValue;

		if (keepAliveTime < 0)
			throw new Exception("Property[" + P_KEEPALIVE_TIME + "] has to be non-negative");
		return keepAliveTime;
	}

	private Integer getTerminationWaitTime(Map<String, ?> properties) {
		final Integer terminationWaitTime = (Integer) properties.get(P_TERM_WAITTIME);
		if (terminationWaitTime == null)
			return m_terminationWaitTime;

		return terminationWaitTime;
	}

	private Integer getQueueCapacity(Map<String, ?> properties) {
		final Integer queueCapacity = (Integer) properties.get(P_QUEUE_CAPACITY);
		if (queueCapacity == null)
			return m_queueCapacity;

		return queueCapacity;
	}
}
