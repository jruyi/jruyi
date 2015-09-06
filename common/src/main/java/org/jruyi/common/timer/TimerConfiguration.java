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

import org.jruyi.common.StrUtil;

final class TimerConfiguration {

	private long m_tickTime;
	private int m_wheelSize;

	public long tickTime() {
		return m_tickTime;
	}

	public int wheelSize() {
		return m_wheelSize;
	}

	public TimerConfiguration tickTime(long tickTime) {
		if (tickTime < 1)
			throw new IllegalArgumentException(StrUtil.join("Illegal tickTime: ", tickTime, " > 0"));
		m_tickTime = tickTime;
		return this;
	}

	public TimerConfiguration wheelSize(int wheelSize) {
		if (wheelSize < 0)
			throw new IllegalArgumentException(StrUtil.join("Illegal wheelSize: ", wheelSize, " >= 0"));
		m_wheelSize = wheelSize;
		return this;
	}
}
