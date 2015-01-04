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
package org.jruyi.timeoutadmin.internal;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.jruyi.common.BiListNode;
import org.jruyi.common.IScheduler;
import org.jruyi.common.StrUtil;
import org.jruyi.timeoutadmin.ITimeoutAdmin;
import org.jruyi.timeoutadmin.ITimeoutNotifier;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "jruyi.timeoutadmin", xmlns = "http://www.osgi.org/xmlns/scr/v1.2.0")
public final class TimeoutAdmin implements ITimeoutAdmin {

	private static final Logger c_logger = LoggerFactory.getLogger(TimeoutAdmin.class);

	private static final String P_ALLOW_CORETHREAD_TIMEOUT = "allowCoreThreadTimeOut";
	private static final String P_CORE_POOLSIZE = "corePoolSize";
	private static final String P_MAX_POOLSIZE = "maxPoolSize";
	private static final String P_KEEPALIVE_TIME = "keepAliveTimeInSeconds";
	private static final String P_QUEUE_CAPACITY = "queueCapacity";
	private static final String P_TERM_WAITTIME = "terminationWaitTimeInSeconds";

	// 1 hour
	private static final int SCALE = 60 * 60;
	private static final int COVER_DAYS = 7;
	private static final int UNIT_TW2;
	private static final int SCALE_TW2;

	private LinkedList<TimeoutEvent> m_list;
	private TimeWheel m_tw1;
	private TimeWheel m_tw2;

	private IScheduler m_scheduler;
	private ThreadPoolExecutor m_executor;
	private int m_queueCapacity = 2048;
	private int m_terminationWaitTime = 60;

	static {
		int n = 1;
		for (int i = SCALE / 2 + 1; i != 0; i >>= 1)
			n <<= 1;
		UNIT_TW2 = n;
		n = 1;
		for (int i = (COVER_DAYS * 24 * 60 * 60 + UNIT_TW2 - 1) / UNIT_TW2 + 1; i != 0; i >>= 1)
			n <<= 1;
		SCALE_TW2 = n;
	}

	static final class DeliveryThreadFactory implements ThreadFactory {

		static final DeliveryThreadFactory INST = new DeliveryThreadFactory();

		private DeliveryThreadFactory() {
		}

		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, "jruyi-to-delivery");
		}
	}

	final class TimeWheel implements Runnable {

		private final BiListNode<TimeoutEvent>[] m_wheel;
		private final ReentrantLock[] m_locks;
		private final int m_capacityMask;

		// The hand that points to the current timeout sublist, nodes of which
		// are between m_wheel[m_hand] and m_wheel[m_hand + 1].
		private int m_hand;

		public TimeWheel(int capacity) {
			// one more as a tail node for conveniently iterating the timeout
			// sublist
			@SuppressWarnings("unchecked")
			final BiListNode<TimeoutEvent>[] wheel = (BiListNode<TimeoutEvent>[]) new BiListNode<?>[capacity + 1];
			final ReentrantLock[] locks = new ReentrantLock[capacity];
			final LinkedList<TimeoutEvent> list = m_list;
			for (int i = 0; i < capacity; ++i) {
				// create sentinel nodes
				wheel[i] = list.addLast(null);
				locks[i] = new ReentrantLock();
			}
			wheel[capacity] = list.addLast(null);

			m_wheel = wheel;
			m_locks = locks;
			m_capacityMask = capacity - 1;
		}

		@Override
		public void run() {
			final int hand = m_hand;
			final int nextHand = hand + 1;
			final BiListNode<TimeoutEvent>[] wheel = m_wheel;
			final BiListNode<TimeoutEvent> begin = wheel[hand];
			final BiListNode<TimeoutEvent> end = wheel[nextHand];
			BiListNode<TimeoutEvent> node;
			while ((node = begin.next()) != end) {
				final TimeoutNotifier notifier;
				final TimeoutEvent event = node.get();

				// If this node has been cancelled, just skip.
				// Otherwise, go ahead.
				if (event != null && (notifier = event.getNotifier()) != null)
					// Passing "hand" for checking the notifier is still in this
					// same timeout sublist. Otherwise it may be cancelled or
					// rescheduled, and needs to be skipped.
					notifier.onTimeout(this, hand);
			}

			// tick
			m_hand = getEffectiveIndex(nextHand);
		}

		void schedule(TimeoutNotifier notifier, TimeoutEvent event, int offset) {
			final int n = getEffectiveIndex(m_hand + offset);
			event.setTimeWheelAndIndex(this, n);
			notifier.setNode(m_list.syncInsertAfter(m_wheel[n], event, getLock(n)));
		}

		void reschedule(TimeoutNotifier notifier, int offset, ReentrantLock srcLock) {
			final BiListNode<TimeoutEvent> node = notifier.getNode();
			final TimeoutEvent event = node.get();
			final int n = getEffectiveIndex(m_hand + offset);
			event.setTimeWheelAndIndex(this, n);

			final ReentrantLock dstLock = getLock(n);
			m_list.syncMoveAfter(m_wheel[n], node, srcLock, dstLock);
		}

		ReentrantLock getLock(int index) {
			return m_locks[index];
		}

		private int getEffectiveIndex(int index) {
			return (index & m_capacityMask);
		}
	}

	@Override
	public ITimeoutNotifier createNotifier(Object subject) {
		return new TimeoutNotifier(subject, this);
	}

	@Reference(name = "scheduler", policy = ReferencePolicy.DYNAMIC)
	synchronized void setScheduler(IScheduler scheduler) {
		m_scheduler = scheduler;
	}

	synchronized void unsetScheduler(IScheduler scheduler) {
		if (m_scheduler == scheduler)
			m_scheduler = null;
	}

	@Modified
	void modified(Map<String, ?> properties) throws Exception {
		final ThreadPoolExecutor executor = m_executor;
		final boolean allowCoreThreadTimeOut = getAllowCoreThreadTimeOut(properties);
		final int keepAliveTime = getKeepAliveTime(properties, (int) executor.getKeepAliveTime(TimeUnit.SECONDS));
		final int corePoolSize = getCorePoolSize(properties);
		final int maxPoolSize = getMaxPoolSize(properties, corePoolSize);
		final int queueCapacity = getQueueCapacity(properties);
		final int terminationWaitTime = getTerminationWaitTime(properties);

		final int oldQueueCapacity = m_queueCapacity;
		if (queueCapacity != oldQueueCapacity && (queueCapacity >= 0 || oldQueueCapacity >= 0)) {
			m_executor = newExecutor(corePoolSize, maxPoolSize, keepAliveTime, queueCapacity, allowCoreThreadTimeOut);
			m_queueCapacity = queueCapacity;
			executor.shutdown();
		} else {
			executor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
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

		c_logger.info(StrUtil.join("TimeoutAdmin was updated: ", this));
	}

	void activate(Map<String, ?> properties) throws Throwable {

		final boolean allowCoreThreadTimeOut = getAllowCoreThreadTimeOut(properties);
		final int keepAliveTime = getKeepAliveTime(properties, 10);
		final int corePoolSize = getCorePoolSize(properties);
		final int maxPoolSize = getMaxPoolSize(properties, corePoolSize);
		final int queueCapacity = getQueueCapacity(properties);
		final int terminationWaitTime = getTerminationWaitTime(properties);

		m_executor = newExecutor(corePoolSize, maxPoolSize, keepAliveTime, queueCapacity, allowCoreThreadTimeOut);
		m_executor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
		m_queueCapacity = queueCapacity;
		m_terminationWaitTime = terminationWaitTime;

		m_list = new LinkedList<TimeoutEvent>();

		final TimeWheel tw1 = new TimeWheel(UNIT_TW2 * 2);
		final TimeWheel tw2 = new TimeWheel(SCALE_TW2);
		final IScheduler scheduler = m_scheduler;
		scheduler.scheduleAtFixedRate(tw1, 1, 1, TimeUnit.SECONDS);
		scheduler.scheduleAtFixedRate(tw2, UNIT_TW2, UNIT_TW2, TimeUnit.SECONDS);
		m_tw1 = tw1;
		m_tw2 = tw2;

		c_logger.info("TimeoutAdmin activated");
	}

	void deactivate() {
		c_logger.info("Deactivating TimeoutAdmin...");

		try {
			m_executor.shutdown();
			if (m_executor.awaitTermination(m_terminationWaitTime, TimeUnit.SECONDS))
				c_logger.debug("Timeout delivery executor terminated");
			else
				c_logger.debug("Termination of timeout delivery executor timed out");
		} catch (InterruptedException e) {
			c_logger.warn("Going here is abnormal");
		} catch (Throwable t) {
			c_logger.error("TimeoutAdmin Deactivation Error", t);
		} finally {
			m_executor = null;
		}

		m_tw2 = null;
		m_tw1 = null;
		m_list = null;

		c_logger.info("TimeoutAdmin deactivated");
	}

	void schedule(TimeoutNotifier notifier, int timeout) {
		final TimeoutEvent event = TimeoutEvent.get(notifier, timeout);
		if (timeout < UNIT_TW2 * 2)
			m_tw1.schedule(notifier, event, timeout);
		else {
			int offset = (timeout - UNIT_TW2 - 1) / UNIT_TW2;
			if (offset > SCALE_TW2 - 1)
				offset = SCALE_TW2 - 1;
			m_tw2.schedule(notifier, event, offset);
		}
	}

	void reschedule(TimeoutNotifier notifier, int timeout) {
		final TimeoutEvent event = notifier.getNode().get();
		final ReentrantLock srcLock = event.getTimeWheel().getLock(event.getIndex());
		if (timeout < UNIT_TW2 * 2)
			m_tw1.reschedule(notifier, timeout, srcLock);
		else {
			timeout = (timeout - UNIT_TW2 - 1) / UNIT_TW2;
			if (timeout > SCALE_TW2 - 1)
				timeout = SCALE_TW2 - 1;
			m_tw2.reschedule(notifier, timeout, srcLock);
		}
	}

	void cancel(TimeoutNotifier notifier) {
		final BiListNode<TimeoutEvent> node = notifier.getNode();
		notifier.clearNode();
		final TimeoutEvent event = node.get();
		final ReentrantLock lock = event.getTimeWheel().getLock(event.getIndex());
		m_list.syncRemove(node, lock);

		// release the timeout event
		event.release();
	}

	void fireTimeout(TimeoutNotifier notifier) {
		final BiListNode<TimeoutEvent> node = notifier.getNode();
		notifier.clearNode();
		final TimeoutEvent event = node.get();
		final ReentrantLock lock = event.getTimeWheel().getLock(event.getIndex());
		m_list.syncRemove(node, lock);

		final Executor executor = notifier.getExecutor();
		if (executor != null)
			executor.execute(event);
		else
			m_executor.execute(event);
	}

	private static boolean getAllowCoreThreadTimeOut(Map<String, ?> properties) {
		final Object v = properties.get(P_ALLOW_CORETHREAD_TIMEOUT);
		return v == null ? true : (Boolean) v;
	}

	private static int getCorePoolSize(Map<String, ?> properties) {
		final Object v = properties.get(P_CORE_POOLSIZE);
		return v == null ? 1 : (Integer) v;
	}

	private static int getMaxPoolSize(Map<String, ?> properties, int corePoolSize) throws Exception {
		final Object v = properties.get(P_MAX_POOLSIZE);
		int maxPoolSize = v == null ? 4 : (Integer) v;
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

	private static ThreadPoolExecutor newExecutor(int corePoolSize, int maxPoolSize, long keepAliveTime,
			int queueCapacity, boolean allowCoreThreadTimeOut) {
		final ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime,
				TimeUnit.SECONDS, queueCapacity < 0 ? new LinkedBlockingQueue<Runnable>()
						: (queueCapacity > 0 ? new ArrayBlockingQueue<Runnable>(queueCapacity)
								: new SynchronousQueue<Runnable>()), DeliveryThreadFactory.INST,
				new ThreadPoolExecutor.CallerRunsPolicy());
		executor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
		return executor;
	}
}
