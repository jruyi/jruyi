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
package org.jruyi.workshop.internal;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.jruyi.common.IArgList;
import org.jruyi.common.IDumpable;
import org.jruyi.common.StrUtil;
import org.jruyi.common.StringBuilder;
import org.jruyi.workshop.IRunnable;
import org.jruyi.workshop.IWorkshop;
import org.jruyi.workshop.IWorkshopProfiler;
import org.jruyi.workshop.WorkshopConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "jruyi.workshop", //
service = { IWorkshop.class, IWorkshopProfiler.class }, //
property = { WorkshopConstants.THREAD_PREFIX + "="
		+ WorkshopConstants.DEFAULT_THREADPREFIX }, //
xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class Workshop implements IWorkshop, IWorkshopProfiler, IDumpable {

	private static final Logger c_logger = LoggerFactory
			.getLogger(Workshop.class);

	private static final String P_CORE_POOLSIZE = "corePoolSize";
	private static final String P_MAX_POOLSIZE = "maxPoolSize";
	private static final String P_KEEPALIVE_TIME = "keepAliveTimeInSeconds";
	private static final String P_QUEUE_CAPACITY = "queueCapacity";
	private static final String P_THREAD_PREFIX = WorkshopConstants.THREAD_PREFIX;
	private static final String P_TERM_WAITTIME = "terminationWaitTimeInSeconds";

	private static final AtomicLong c_sequence = new AtomicLong();
	private final long m_id = c_sequence.getAndIncrement();

	private String m_threadPrefix = WorkshopConstants.DEFAULT_THREADPREFIX;;
	private RuyiThreadPoolExecutor m_executor;
	private int m_queueCapacity = 6000;
	private int m_terminationWaitTime = 300;

	@Override
	public long getProfilerId() {
		return m_id;
	}

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
	public String getThreadPrefix() {
		return m_threadPrefix;
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
	public void run(Runnable job) {
		m_executor.execute(job);
	}

	@Override
	public void run(IRunnable job, IArgList argList) {
		m_executor.execute(Task.get(job, argList));
	}

	@Override
	public void dump(StringBuilder builder) {
		final ThreadPoolExecutor executor = m_executor;
		builder.append("{" + P_CORE_POOLSIZE + "=")
				.append(executor.getCorePoolSize())
				.append(", " + P_MAX_POOLSIZE + "=")
				.append(executor.getMaximumPoolSize())
				.append(", " + P_KEEPALIVE_TIME + "=")
				.append(executor.getKeepAliveTime(TimeUnit.SECONDS))
				.append(", " + P_QUEUE_CAPACITY + "=").append(m_queueCapacity)
				.append(", " + P_TERM_WAITTIME + "=")
				.append(m_terminationWaitTime).append('}');
	}

	@Modified
	protected void modified(Map<String, ?> properties) throws Exception {
		final ThreadPoolExecutor executor = m_executor;
		final int keepAliveTime = getKeepAliveTime(properties,
				(int) executor.getKeepAliveTime(TimeUnit.SECONDS));
		final int corePoolSize = getCorePoolSize(properties);
		final int maxPoolSize = getMaxPoolSize(properties, corePoolSize);
		final int queueCapacity = getQueueCapacity(properties);
		final int terminationWaitTime = getTerminationWaitTime(properties);
		final String threadPrefix = getThreadPrefix(properties);

		final int oldQueueCapacity = m_queueCapacity;
		if (queueCapacity != oldQueueCapacity
				&& (queueCapacity >= 0 || oldQueueCapacity >= 0)) {
			m_executor = newExecutor(threadPrefix, corePoolSize, maxPoolSize,
					keepAliveTime, queueCapacity);
			m_queueCapacity = queueCapacity;
			executor.shutdown();
		} else {
			threadPrefix(threadPrefix);
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

		c_logger.info(getLog(" was updated - ", this));
	}

	protected void activate(Map<String, ?> properties) throws Exception {
		final String threadPrefix = getThreadPrefix(properties);

		c_logger.info(getLog("Activating "));

		final int keepAliveTime = getKeepAliveTime(properties, 10);
		final int corePoolSize = getCorePoolSize(properties);
		final int maxPoolSize = getMaxPoolSize(properties, corePoolSize);
		final int queueCapacity = getQueueCapacity(properties);
		final int terminationWaitTime = getTerminationWaitTime(properties);

		m_executor = newExecutor(threadPrefix, corePoolSize, maxPoolSize,
				keepAliveTime, queueCapacity);
		m_queueCapacity = queueCapacity;
		m_terminationWaitTime = terminationWaitTime;

		c_logger.info(getLog(" was activated - ", this));
	}

	protected void deactivate() {

		c_logger.info(getLog("Deactivating "));

		try {
			m_executor.shutdown();
			if (m_executor.awaitTermination(m_terminationWaitTime,
					TimeUnit.SECONDS))
				c_logger.debug("Executor terminated");
			else
				c_logger.debug("Executor was time out");

			c_logger.info(getLog(" was deactivated - ", this));
		} catch (InterruptedException e) {
			c_logger.warn("Going here is abnormal");
		} catch (Exception e) {
			c_logger.error("Workshop Deactivation Error", e);
		} finally {
			m_executor = null;
		}
	}

	private static RuyiThreadPoolExecutor newExecutor(String name,
			int corePoolSize, int maxPoolSize, long keepAliveTime,
			int queueCapacity) {
		return new RuyiThreadPoolExecutor(
				corePoolSize,
				maxPoolSize,
				keepAliveTime,
				TimeUnit.SECONDS,
				queueCapacity < 0 ? new LinkedBlockingQueue<Runnable>()
						: (queueCapacity > 0 ? new ArrayBlockingQueue<Runnable>(
								queueCapacity)
								: new SynchronousQueue<Runnable>()),
				new PooledThreadFactory(name),
				new ThreadPoolExecutor.CallerRunsPolicy());
	}

	private static int getCorePoolSize(Map<String, ?> properties) {
		Object v = properties.get(P_CORE_POOLSIZE);
		if (v == null)
			return Runtime.getRuntime().availableProcessors() << 1;
		else
			return (Integer) v;
	}

	private static int getMaxPoolSize(Map<String, ?> properties,
			int corePoolSize) throws Exception {
		Object v = properties.get(P_MAX_POOLSIZE);
		if (v == null) {
			int maxPoolSize = corePoolSize << 1;
			if (maxPoolSize < 1 || maxPoolSize >= 500)
				maxPoolSize = corePoolSize;
			return maxPoolSize;
		}

		int maxPoolSize = (Integer) v;
		if (maxPoolSize < corePoolSize)
			throw new Exception("Property[" + P_MAX_POOLSIZE
					+ "] cannot be less than Property[" + P_CORE_POOLSIZE + "]");

		return maxPoolSize;
	}

	private static Integer getKeepAliveTime(Map<String, ?> properties,
			Integer defaultValue) throws Exception {
		final Integer keepAliveTime = (Integer) properties
				.get(P_KEEPALIVE_TIME);
		if (keepAliveTime == null)
			return defaultValue;

		if (keepAliveTime < 0)
			throw new Exception("Property[" + P_KEEPALIVE_TIME
					+ "] has to be non-negative");
		return keepAliveTime;
	}

	private Integer getTerminationWaitTime(Map<String, ?> properties) {
		final Integer terminationWaitTime = (Integer) properties
				.get(P_TERM_WAITTIME);
		if (terminationWaitTime == null)
			return m_terminationWaitTime;

		return terminationWaitTime;
	}

	private Integer getQueueCapacity(Map<String, ?> properties) {
		final Integer queueCapacity = (Integer) properties
				.get(P_QUEUE_CAPACITY);
		if (queueCapacity == null)
			return m_queueCapacity;

		return queueCapacity;
	}

	private String getThreadPrefix(Map<String, ?> properties) {
		String threadPrefix = (String) properties.get(P_THREAD_PREFIX);
		if (threadPrefix == null)
			return m_threadPrefix;

		threadPrefix = threadPrefix.trim();
		if (threadPrefix.isEmpty())
			return m_threadPrefix;

		m_threadPrefix = threadPrefix;
		return threadPrefix;
	}

	private String getLog(String arg0, Object arg1) {
		return StrUtil.join("Workshop[" + P_THREAD_PREFIX + "=",
				m_threadPrefix, "]", arg0, arg1);
	}

	private String getLog(String arg) {
		return StrUtil.join(arg, "Workshop[" + P_THREAD_PREFIX + "=",
				m_threadPrefix, "]...");
	}

	private void threadPrefix(String threadPrefix) {
		((PooledThreadFactory) m_executor.getThreadFactory())
				.threadPrefix(threadPrefix);
	}
}
