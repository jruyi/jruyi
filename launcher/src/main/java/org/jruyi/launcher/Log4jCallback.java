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

package org.jruyi.launcher;

import java.util.ArrayList;

import org.apache.logging.log4j.core.util.Cancellable;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;

public final class Log4jCallback implements ShutdownCallbackRegistry, Cancellable {

	private static final ArrayList<Log4jCallback> c_callbacks = new ArrayList<>();
	private Runnable m_runnable;

	public Log4jCallback() {
		synchronized (c_callbacks) {
			c_callbacks.add(this);
		}
	}

	@Override
	public void cancel() {
		synchronized (c_callbacks) {
			c_callbacks.remove(this);
		}
	}

	@Override
	public void run() {
	}

	@Override
	public Cancellable addShutdownCallback(Runnable callback) {
		m_runnable = callback;
		return this;
	}

	public static void shutdown() {
		final Runnable[] runnables;
		synchronized (c_callbacks) {
			int n = c_callbacks.size();
			runnables = new Runnable[n];
			for (int i = 0; n > 0; ++i)
				runnables[i] = c_callbacks.remove(--n).m_runnable;
		}

		for (Runnable runnable : runnables) {
			if (runnable != null)
				runnable.run();
		}
	}
}
