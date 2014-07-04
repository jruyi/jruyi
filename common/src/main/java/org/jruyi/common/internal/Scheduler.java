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
package org.jruyi.common.internal;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jruyi.common.IScheduler;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "jruyi.common.scheduler", xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class Scheduler implements IScheduler {

	private static final Logger c_logger = LoggerFactory
			.getLogger(Scheduler.class);

	private Configuration m_conf;
	private ScheduledThreadPoolExecutor m_executor;

	static final class Configuration {

		private static final String P_NUMBER_OF_THREADS = "numberOfThreads";
		private static final String P_TERM_WAITTIME_IN_SECONDS = "terminationWaitTimeInSeconds";

		private int m_numberOfThreads = 1;
		private int m_terminationWaitTimeInSeconds = 300;

		private Configuration() {
		}

		static Configuration create(Map<String, ?> properties) {
			final Configuration conf = new Configuration();
			conf.numberOfThreads((Integer) properties.get(P_NUMBER_OF_THREADS));
			conf.terminationWaitTimeInSeconds((Integer) properties
					.get(P_TERM_WAITTIME_IN_SECONDS));
			return conf;
		}

		public int numberOfThreads() {
			return m_numberOfThreads;
		}

		public void numberOfThreads(Integer numberOfThreads) {
			if (numberOfThreads == null || numberOfThreads < 1)
				return;
			m_numberOfThreads = numberOfThreads;
		}

		public int terminationWaitTimeInSeconds() {
			return m_terminationWaitTimeInSeconds;
		}

		public void terminationWaitTimeInSeconds(
				Integer terminationWaitTimeInSeconds) {
			if (terminationWaitTimeInSeconds == null)
				return;
			m_terminationWaitTimeInSeconds = terminationWaitTimeInSeconds;
		}
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay,
			TimeUnit unit) {
		return m_executor.schedule(command, delay, unit);
	}

	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay,
			TimeUnit unit) {
		return m_executor.schedule(callable, delay, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
			long initialDelay, long period, TimeUnit unit) {
		return m_executor.scheduleAtFixedRate(command, initialDelay, period,
				unit);
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
			long initialDelay, long delay, TimeUnit unit) {
		return m_executor.scheduleWithFixedDelay(command, initialDelay, delay,
				unit);
	}

	@Modified
	public void modified(Map<String, ?> properties) {
		final Configuration conf = Configuration.create(properties);
		m_conf = conf;

		final int numberOfThreads = conf.numberOfThreads();
		m_executor.setCorePoolSize(numberOfThreads);

		c_logger.info("Scheduler modified: numberOfThreads={}", numberOfThreads);
	}

	public void activate(Map<String, ?> properties) {
		final Configuration conf = Configuration.create(properties);
		final int numberOfThreads = conf.numberOfThreads();
		m_executor = new ScheduledThreadPoolExecutor(numberOfThreads);
		m_conf = conf;

		c_logger.info("Scheduler activated: numberOfThreads={}",
				numberOfThreads);
	}

	public void deactivate() {
		final ScheduledThreadPoolExecutor executor = m_executor;
		executor.shutdown();
		try {
			executor.awaitTermination(m_conf.terminationWaitTimeInSeconds(),
					TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// Ignore
		}

		m_executor = null;
		m_conf = null;

		c_logger.info("Scheduler deactivated");
	}
}
