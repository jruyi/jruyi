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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public final class RuyiThreadPoolExecutor extends ThreadPoolExecutor {

	private Profiler m_profile = InactiveProfile.get();

	interface Profiler {

		public Profiler start();

		public Profiler stop();

		public boolean isProfiling();

		public void beforeExecute();

		public void execute(Runnable task);

		public void afterExecute(Runnable task);

		public long getNumberOfRequestsRetired();

		public double getRequestPerSecondRetirementRate();

		public double getAverageServiceTime();

		public double getAverageTimeWaitingInPool();

		public double getAverageResponseTime();

		public double getEstimatedAverageNumberOfActiveRequests();

		public double getRatioOfDeadTimeToResponseTime();

		public double getRatioOfActiveRequestsToCoreCount();
	}

	static final class InactiveProfile implements Profiler {

		private static final InactiveProfile INST = new InactiveProfile();

		private InactiveProfile() {
		}

		public static InactiveProfile get() {
			return INST;
		}

		@Override
		public Profiler start() {
			return ActiveProfile.get();
		}

		@Override
		public Profiler stop() {
			return this;
		}

		@Override
		public boolean isProfiling() {
			return false;
		}

		@Override
		public void beforeExecute() {
		}

		@Override
		public void execute(Runnable task) {
		}

		@Override
		public void afterExecute(Runnable task) {
		}

		@Override
		public long getNumberOfRequestsRetired() {
			return 0L;
		}

		@Override
		public double getRequestPerSecondRetirementRate() {
			return 0.0D;
		}

		@Override
		public double getAverageServiceTime() {
			return 0.0D;
		}

		@Override
		public double getAverageTimeWaitingInPool() {
			return 0.0D;
		}

		@Override
		public double getAverageResponseTime() {
			return 0.0D;
		}

		@Override
		public double getEstimatedAverageNumberOfActiveRequests() {
			return 0.0D;
		}

		@Override
		public double getRatioOfDeadTimeToResponseTime() {
			return 0.0D;
		}

		@Override
		public double getRatioOfActiveRequestsToCoreCount() {
			return 0.0D;
		}
	}

	static final class ActiveProfile implements Profiler {

		private final ConcurrentHashMap<Runnable, Long> m_timeOfRequest;
		private final ThreadLocal<Long> m_startTime;
		private final AtomicLong m_aggregateInterRequestArrivalTime;
		private final AtomicLong m_totalServiceTime;
		private final AtomicLong m_totalPoolTime;
		private final AtomicLong m_numberOfRequestsRetired;
		private final ReentrantLock m_lock;
		private Long m_lastArrivalTime;

		private ActiveProfile() {
			m_startTime = new ThreadLocal<Long>();
			m_timeOfRequest = new ConcurrentHashMap<Runnable, Long>();
			m_totalServiceTime = new AtomicLong();
			m_totalPoolTime = new AtomicLong();
			m_aggregateInterRequestArrivalTime = new AtomicLong();
			m_numberOfRequestsRetired = new AtomicLong();
			m_lock = new ReentrantLock();
		}

		public static ActiveProfile get() {
			return new ActiveProfile();
		}

		@Override
		public Profiler start() {
			return this;
		}

		@Override
		public Profiler stop() {
			return InactiveProfile.get();
		}

		@Override
		public boolean isProfiling() {
			return true;
		}

		@Override
		public void execute(Runnable task) {
			long now = System.nanoTime();
			final ReentrantLock lock = m_lock;
			lock.lock();
			try {
				if (m_lastArrivalTime != null)
					m_aggregateInterRequestArrivalTime.addAndGet(now
							- m_lastArrivalTime);

				m_lastArrivalTime = now;
				m_timeOfRequest.put(task, now);
			} finally {
				lock.unlock();
			}
		}

		@Override
		public void beforeExecute() {
			m_startTime.set(System.nanoTime());
		}

		@Override
		public void afterExecute(Runnable task) {
			Long startTime = m_startTime.get();
			if (startTime == null)
				return;

			m_totalServiceTime.addAndGet(System.nanoTime() - startTime);
			m_totalPoolTime.addAndGet(startTime - m_timeOfRequest.remove(task));
			m_numberOfRequestsRetired.incrementAndGet();
		}

		@Override
		public long getNumberOfRequestsRetired() {
			return m_numberOfRequestsRetired.longValue();
		}

		@Override
		public double getRequestPerSecondRetirementRate() {
			return m_numberOfRequestsRetired.doubleValue()
					/ fromNanoToSeconds(m_aggregateInterRequestArrivalTime);
		}

		@Override
		public double getAverageServiceTime() {
			return fromNanoToSeconds(m_totalServiceTime)
					/ m_numberOfRequestsRetired.doubleValue();
		}

		@Override
		public double getAverageTimeWaitingInPool() {
			return fromNanoToSeconds(m_totalPoolTime)
					/ m_numberOfRequestsRetired.doubleValue();
		}

		@Override
		public double getAverageResponseTime() {
			return getAverageServiceTime() + getAverageTimeWaitingInPool();
		}

		@Override
		public double getEstimatedAverageNumberOfActiveRequests() {
			return getRequestPerSecondRetirementRate()
					* (getAverageServiceTime() + getAverageTimeWaitingInPool());
		}

		@Override
		public double getRatioOfDeadTimeToResponseTime() {
			double poolTime = m_totalPoolTime.doubleValue();
			return poolTime / (poolTime + m_totalServiceTime.doubleValue());
		}

		@Override
		public double getRatioOfActiveRequestsToCoreCount() {
			return getEstimatedAverageNumberOfActiveRequests()
					/ (double) Runtime.getRuntime().availableProcessors();
		}

		private static double fromNanoToSeconds(AtomicLong nano) {
			return nano.doubleValue() / 1000000000.0D;
		}
	}

	public RuyiThreadPoolExecutor(int corePoolSize, int maxPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
			RejectedExecutionHandler handler) {
		super(corePoolSize, maxPoolSize, keepAliveTime, unit, workQueue,
				threadFactory, handler);
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		m_profile.beforeExecute();
	}

	@Override
	public void execute(Runnable command) {
		m_profile.execute(command);
		super.execute(command);
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		try {
			m_profile.afterExecute(r);
		} finally {
			super.afterExecute(r, t);
		}
	}

	public void startProfiling() {
		m_profile = m_profile.start();
	}

	public void stopProfiling() {
		m_profile = m_profile.stop();
	}

	public long getNumberOfRequestsRetired() {
		return m_profile.getNumberOfRequestsRetired();
	}

	public double getRequestPerSecondRetirementRate() {
		return m_profile.getRequestPerSecondRetirementRate();
	}

	public double getAverageServiceTime() {
		return m_profile.getAverageServiceTime();
	}

	public double getAverageTimeWaitingInPool() {
		return m_profile.getAverageTimeWaitingInPool();
	}

	public double getAverageResponseTime() {
		return m_profile.getAverageResponseTime();
	}

	public double getEstimatedAverageNumberOfActiveRequests() {
		return m_profile.getEstimatedAverageNumberOfActiveRequests();
	}

	public double getRatioOfDeadTimeToResponseTime() {
		return m_profile.getRatioOfDeadTimeToResponseTime();
	}

	public double getRatioOfActiveRequestsToCoreCount() {
		return m_profile.getRatioOfActiveRequestsToCoreCount();
	}

	public boolean isProfiling() {
		return m_profile.isProfiling();
	}
}
