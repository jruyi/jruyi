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

import org.jruyi.common.StrUtil;
import org.jruyi.core.IChannelAdminConfiguration;

final class ChannelAdminConfiguration implements IChannelAdminConfiguration {

	private Integer m_numberOfSelectorThreads;
	private Integer m_numberOfIoThreads;
	private int m_capacityOfIoRingBuffer = 1024 * 16;

	@Override
	public IChannelAdminConfiguration numberOfSelectorThreads(Integer numberOfSelectorThreads) {
		if (numberOfSelectorThreads != null && numberOfSelectorThreads < 0)
			throw new IllegalArgumentException(
					StrUtil.join("Illegal numberOfSelectorThreads: ", numberOfSelectorThreads, " >= 0"));
		m_numberOfSelectorThreads = numberOfSelectorThreads;
		return this;
	}

	@Override
	public IChannelAdminConfiguration numberOfIoThreads(Integer numberOfIoThreads) {
		if (numberOfIoThreads != null && numberOfIoThreads < 0)
			throw new IllegalArgumentException(StrUtil.join("Illegal numberOfIoThreads: ", numberOfIoThreads, " >= 0"));
		m_numberOfIoThreads = numberOfIoThreads;
		return this;
	}

	@Override
	public IChannelAdminConfiguration capacityOfIoRingBuffer(int capacityOfIoRingBuffer) {
		if (capacityOfIoRingBuffer < 1)
			throw new IllegalArgumentException(
					StrUtil.join("Illegal capacityOfIoRingBuffer: ", capacityOfIoRingBuffer, " > 0"));
		m_capacityOfIoRingBuffer = capacityOfIoRingBuffer;
		return this;
	}

	@Override
	public Integer numberOfSelectorThreads() {
		return m_numberOfSelectorThreads;
	}

	@Override
	public Integer numberOfIoThreads() {
		return m_numberOfIoThreads;
	}

	@Override
	public int capacityOfIoRingBuffer() {
		return m_capacityOfIoRingBuffer;
	}

	Map<String, ?> properties() {
		final HashMap<String, Object> properties = new HashMap<>(3);
		properties.put("numberOfSelectorThreads", m_numberOfSelectorThreads);
		properties.put("numberOfIoThreads", m_numberOfIoThreads);
		properties.put("capacityOfIoRingBuffer", m_capacityOfIoRingBuffer);
		return properties;
	}
}
