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

package org.jruyi.common.timer;

import org.jruyi.common.IThreadLocalCache;
import org.jruyi.common.ITimeoutEvent;
import org.jruyi.common.ITimeoutListener;
import org.jruyi.common.StrUtil;
import org.jruyi.common.ThreadLocalCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TimeoutEvent<S> implements ITimeoutEvent<S>, Runnable {

	private static final Logger c_logger = LoggerFactory.getLogger(TimeoutEvent.class);
	private static final IThreadLocalCache<TimeoutEvent<?>> c_cache = ThreadLocalCache.weakLinkedCache();
	private int m_timeout;
	private int m_index;
	private TimeoutNotifier<S> m_notifier;

	private TimeoutEvent() {
	}

	static <S> TimeoutEvent<S> get(TimeoutNotifier<S> notifier, int timeout) {
		@SuppressWarnings("unchecked")
		TimeoutEvent<S> event = (TimeoutEvent<S>) c_cache.take();
		if (event == null)
			event = new TimeoutEvent<S>();

		event.m_notifier = notifier;
		event.m_timeout = timeout;
		return event;
	}

	@Override
	public S subject() {
		return m_notifier.subject();
	}

	@Override
	public int timeout() {
		return m_timeout;
	}

	@Override
	public void run() {
		final ITimeoutListener<S> listener = m_notifier.listener();
		if (listener == null) {
			release();
			return;
		}

		try {
			listener.onTimeout(this);
		} catch (Throwable t) {
			c_logger.error(StrUtil.join("Error on timeout: ", subject()), t);
		}
	}

	TimeoutNotifier<?> notifier() {
		return m_notifier;
	}

	void index(int index) {
		m_index = index;
	}

	int index() {
		return m_index;
	}

	void release() {
		m_notifier = null;
		c_cache.put(this);
	}
}
