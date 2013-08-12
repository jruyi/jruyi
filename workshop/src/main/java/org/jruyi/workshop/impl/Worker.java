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
package org.jruyi.workshop.impl;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.jruyi.common.IArgList;
import org.jruyi.common.StrUtil;
import org.jruyi.workshop.IRunnable;
import org.jruyi.workshop.IWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Component(name = "jruyi.workshop", createPid = false)
public final class Worker implements IWorker {

	private static final Logger c_logger = LoggerFactory
			.getLogger(Worker.class);

	@Property(intValue = 10)
	private static final String P_CORE_POOLSIZE = "corePoolSize";
	@Property(intValue = 200)
	private static final String P_MAX_POOLSIZE = "maxPoolSize";
	@Property(intValue = 10)
	private static final String P_KEEPALIVE_TIME = "keepAliveTime";
	@Property(intValue = 2000)
	private static final String P_QUEUE_CAPACITY = "queueCapacity";
	@Property(intValue = 300)
	private static final String P_TERM_WAITTIME = "terminationWaitTime";

	private ThreadPoolExecutor m_executor;
	private int m_queueCapacity;
	private int m_terminationWaitTime = 300;

	@Override
	public void run(Runnable job) {
		m_executor.execute(job);
	}

	@Override
	public void run(IRunnable job, IArgList argList) {
		m_executor.execute(Task.get(job, argList));
	}

	@Modified
	protected void modified(Map<String, ?> properties) throws Exception {
		int corePoolSize = (Integer) properties.get(P_CORE_POOLSIZE);
		int maxPoolSize = (Integer) properties.get(P_MAX_POOLSIZE);
		int keepAliveTime = (Integer) properties.get(P_KEEPALIVE_TIME);
		int queueCapacity = (Integer) properties.get(P_QUEUE_CAPACITY);
		int terminationWaitTime = (Integer) properties.get(P_TERM_WAITTIME);

		if (corePoolSize < 1)
			throw new Exception("Property[" + P_CORE_POOLSIZE
					+ "] has to be positive");

		if (maxPoolSize < corePoolSize)
			throw new Exception("Property[" + P_MAX_POOLSIZE
					+ "] cannot be less than Property[" + P_CORE_POOLSIZE + "]");

		if (keepAliveTime < 0)
			throw new Exception("Property[" + P_KEEPALIVE_TIME
					+ "] has to be non-negative");

		ThreadPoolExecutor executor = m_executor;
		int oldQueueCapacity = m_queueCapacity;
		if (queueCapacity != oldQueueCapacity
				&& (queueCapacity >= 0 || oldQueueCapacity >= 0)) {
			m_executor = new BlockingThreadPoolExecutor(corePoolSize,
					maxPoolSize, keepAliveTime, queueCapacity);
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

		c_logger.info(StrUtil.buildString("ThreadPool was updated - {"
				+ P_CORE_POOLSIZE + "=", corePoolSize, ", " + P_MAX_POOLSIZE
				+ "=", maxPoolSize, ", " + P_KEEPALIVE_TIME + "=",
				keepAliveTime, "s" + ", " + P_QUEUE_CAPACITY + "=",
				queueCapacity, ", " + P_TERM_WAITTIME + "=",
				terminationWaitTime, "s}"));
	}

	protected void activate(Map<String, ?> properties) throws Exception {
		c_logger.info("Activating Workshop...");

		int corePoolSize = (Integer) properties.get(P_CORE_POOLSIZE);
		int maxPoolSize = (Integer) properties.get(P_MAX_POOLSIZE);
		int keepAliveTime = (Integer) properties.get(P_KEEPALIVE_TIME);
		int queueCapacity = (Integer) properties.get(P_QUEUE_CAPACITY);
		int terminationWaitTime = (Integer) properties.get(P_TERM_WAITTIME);

		if (corePoolSize < 1)
			throw new Exception("Property[" + P_CORE_POOLSIZE
					+ "] has to be positive");

		if (maxPoolSize < corePoolSize)
			throw new Exception("Property[" + P_MAX_POOLSIZE
					+ "] cannot be less than Property[" + P_CORE_POOLSIZE + "]");

		if (keepAliveTime < 0)
			throw new Exception("Property[" + P_KEEPALIVE_TIME
					+ "] has to be non-negative");

		m_executor = new BlockingThreadPoolExecutor(corePoolSize, maxPoolSize,
				keepAliveTime, queueCapacity);
		m_queueCapacity = queueCapacity;
		m_terminationWaitTime = terminationWaitTime;

		c_logger.info(StrUtil.buildString("ThreadPool was created - {"
				+ P_CORE_POOLSIZE + "=", corePoolSize, ", " + P_MAX_POOLSIZE
				+ "=", maxPoolSize, ", " + P_KEEPALIVE_TIME + "=",
				keepAliveTime, "s" + ", " + P_QUEUE_CAPACITY + "=",
				queueCapacity, ", " + P_TERM_WAITTIME + "=",
				terminationWaitTime, "s}"));

		c_logger.info("Workshop activated");
	}

	protected void deactivate() {
		c_logger.info("Deactivating Workshop...");

		try {
			m_executor.shutdown();
			if (m_executor.awaitTermination(m_terminationWaitTime,
					TimeUnit.SECONDS))
				c_logger.debug("Executor terminated");
			else
				c_logger.debug("Executor was time out");
		} catch (InterruptedException e) {
			c_logger.warn("Going here is abnormal");
		} catch (Exception e) {
			c_logger.error("Workshop Deactivation Error", e);
		} finally {
			m_executor = null;
		}

		c_logger.info("Workshop deactivated");
	}
}
