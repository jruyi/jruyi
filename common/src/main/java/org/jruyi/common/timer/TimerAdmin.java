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

import org.jruyi.common.IScheduler;
import org.jruyi.common.ITimer;
import org.jruyi.common.ITimerAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(name = "jruyi.common.timeradmin", configurationPolicy = ConfigurationPolicy.IGNORE)
public final class TimerAdmin implements ITimerAdmin {

	private IScheduler m_scheduler;

	@Reference(name = "scheduler", policy = ReferencePolicy.DYNAMIC)
	public synchronized void setScheduler(IScheduler scheduler) {
		m_scheduler = scheduler;
	}

	public synchronized void unsetScheduler(IScheduler scheduler) {
		if (m_scheduler == scheduler)
			m_scheduler = null;
	}

	@Override
	public ITimer createTimer(int wheelSize) {
		return createTimer(wheelSize, 1000L);
	}

	@Override
	public ITimer createTimer(int wheelSize, long tickTime) {
		return new Timer(new TimerConfiguration().tickTime(tickTime).wheelSize(wheelSize)).scheduler(m_scheduler);
	}
}
