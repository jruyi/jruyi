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

import org.jruyi.common.IArgList;
import org.jruyi.common.ICloseable;
import org.jruyi.common.IThreadLocalCache;
import org.jruyi.common.ThreadLocalCache;
import org.jruyi.workshop.IRunnable;

final class Task implements Runnable, ICloseable {

	private static final IThreadLocalCache<Task> c_cache = ThreadLocalCache
			.weakLinkedCache();
	private IRunnable m_runnable;
	private IArgList m_argList;

	private Task() {
	}

	public static Task get(IRunnable runnable, IArgList argList) {
		Task task = c_cache.take();
		if (task == null)
			task = new Task();

		task.m_runnable = runnable;
		task.m_argList = argList;
		return task;
	}

	@Override
	public void run() {
		m_runnable.run(m_argList);
		close();
	}

	@Override
	public void close() {
		m_runnable = null;
		m_argList.close();
		m_argList = null;
		c_cache.put(this);
	}
}
