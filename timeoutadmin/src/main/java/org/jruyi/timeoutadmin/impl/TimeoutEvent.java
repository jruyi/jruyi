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
package org.jruyi.timeoutadmin.impl;

import org.jruyi.common.IThreadLocalCache;
import org.jruyi.common.StrUtil;
import org.jruyi.common.ThreadLocalCache;
import org.jruyi.timeoutadmin.ITimeoutEvent;
import org.jruyi.timeoutadmin.ITimeoutListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TimeoutEvent implements ITimeoutEvent, Runnable {

	private static final Logger c_logger = LoggerFactory
			.getLogger(TimeoutEvent.class);
	private static final IThreadLocalCache<TimeoutEvent> c_cache = ThreadLocalCache
			.weakLinkedCache();
	private int m_timeout;
	private int m_timeLeft;
	private int m_index;
	private TimeoutNotifier m_notifier;

	private TimeoutEvent() {
	}

	static TimeoutEvent get(TimeoutNotifier notifier, int timeout) {
		TimeoutEvent event = c_cache.take();
		if (event == null)
			event = new TimeoutEvent();

		event.m_notifier = notifier;
		event.m_timeout = timeout;
		return event;
	}

	@Override
	public Object getSubject() {
		return m_notifier.getSubject();
	}

	@Override
	public int getTimeout() {
		return m_timeout;
	}

	@Override
	public void run() {
		ITimeoutListener listener = m_notifier.getListener();
		if (listener == null) {
			release();
			return;
		}

		try {
			listener.onTimeout(this);
		} catch (Throwable t) {
			c_logger.error(StrUtil.join("Error on timeout: ", getSubject()), t);
		}
	}

	TimeoutNotifier getNotifier() {
		return m_notifier;
	}

	int getIndex() {
		return m_index;
	}

	void setIndex(int index) {
		m_index = index;
	}

	int getTimeLeft() {
		return m_timeLeft;
	}

	void setTimeLeft(int timeLeft) {
		m_timeLeft = timeLeft;
	}

	void release() {
		m_notifier = null;
		m_index = -1;
		c_cache.put(this);
	}
}
