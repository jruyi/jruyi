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

package org.jruyi.core.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jruyi.common.StrUtil;
import org.jruyi.common.internal.Scheduler;
import org.jruyi.core.IScheduler;

final class SchedulerWrapper implements IScheduler, IScheduler.IConfiguration {

	private final Map<String, Object> m_properties = new HashMap<>(2);
	private final Scheduler m_scheduler = new Scheduler();

	@Override
	public IConfiguration numberOfThreads(int numberOfThreads) {
		if (numberOfThreads < 1)
			throw new IllegalArgumentException(StrUtil.join("Illegal numberOfThreads: ", numberOfThreads, " > 0"));
		m_properties.put("numberOfThreads", numberOfThreads);
		return this;
	}

	@Override
	public IConfiguration terminationWaitTimeInSeconds(int terminationWaitTimeInSeconds) {
		m_properties.put("terminationWaitTimeInSeconds", terminationWaitTimeInSeconds);
		return this;
	}

	@Override
	public int numberOfThreads() {
		final Object v = m_properties.get("numberOfThreads");
		return v == null ? 1 : (int) v;
	}

	@Override
	public int terminationWaitTimeInSeconds() {
		final Object v = m_properties.get("terminationWaitTimeInSeconds");
		return v == null ? 60 : (int) v;
	}

	@Override
	public synchronized void apply() {
		m_scheduler.modified(m_properties);
	}

	@Override
	public IConfiguration configuration() {
		return this;
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		return m_scheduler.schedule(command, delay, unit);
	}

	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		return m_scheduler.schedule(callable, delay, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		return m_scheduler.scheduleAtFixedRate(command, initialDelay, period, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		return m_scheduler.scheduleWithFixedDelay(command, initialDelay, delay, unit);
	}

	Scheduler unwrap() {
		return m_scheduler;
	}

	synchronized void start() throws Throwable {
		m_scheduler.activate(m_properties);
	}

	synchronized void stop() {
		m_scheduler.deactivate();
	}
}
