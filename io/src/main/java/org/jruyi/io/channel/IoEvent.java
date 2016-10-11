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

package org.jruyi.io.channel;

import org.jruyi.io.IFilter;

public final class IoEvent {

	private final IIoTask m_task;
	private final Object m_msg;
	private final IFilter<?, ?>[] m_filters;
	private final int m_filterCount;

	public IoEvent(IIoTask task, Object msg, IFilter<?, ?>[] filters, int filterCount) {
		m_task = task;
		m_msg = msg;
		m_filters = filters;
		m_filterCount = filterCount;
	}

	public IoEvent(IIoTask task, Object msg) {
		this(task, msg, null, 0);
	}

	public IIoTask task() {
		return m_task;
	}

	public Object msg() {
		return m_msg;
	}

	public IFilter<?, ?>[] filters() {
		return m_filters;
	}

	public int filterCount() {
		return m_filterCount;
	}
}
