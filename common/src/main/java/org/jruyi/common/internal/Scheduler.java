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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.jruyi.common.IScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service(IScheduler.class)
@Component(name = "jruyi.common.scheduler", createPid = false)
public final class Scheduler implements IScheduler {

	private static final Logger c_logger = LoggerFactory
			.getLogger(Scheduler.class);

	@Property(intValue = 1)
	private static final String P_NUMBER_OF_THREAD = "numberOfThread";

	@Property(intValue = 300)
	private static final String P_TERM_WAITTIME_IN_SECONDS = "terminationWaitTimeInSeconds";

	private Configuration m_conf;
	private ScheduledThreadPoolExecutor m_executor;

	static final class Configuration {

		private Integer m_numberOfThread;
		private Integer m_terminationWaitTimeInSeconds;

		private Configuration() {
		}

		static Configuration create(Map<String, ?> properties) {
			final Configuration conf = new Configuration();
			conf.numberOfThread((Integer) properties.get(P_NUMBER_OF_THREAD));
			conf.terminationWaitTimeInSeconds((Integer) properties
					.get(P_TERM_WAITTIME_IN_SECONDS));
			return conf;
		}

		public Integer numberOfThread() {
			return m_numberOfThread;
		}

		public void numberOfThread(Integer numberOfThread) {
			m_numberOfThread = numberOfThread;
		}

		public Integer terminationWaitTimeInSeconds() {
			return m_terminationWaitTimeInSeconds;
		}

		public void terminationWaitTimeInSeconds(
				Integer terminationWaitTimeInSeconds) {
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

		final Integer numberOfThread = conf.numberOfThread();
		m_executor.setCorePoolSize(numberOfThread);

		c_logger.info("Scheduler modified: numberOfThread={}", numberOfThread);
	}

	public void activate(Map<String, ?> properties) {
		final Configuration conf = Configuration.create(properties);
		final Integer numberOfThread = conf.numberOfThread();
		m_executor = new ScheduledThreadPoolExecutor(numberOfThread);
		m_conf = conf;

		c_logger.info("Scheduler activated: numberOfThread={}", numberOfThread);
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
