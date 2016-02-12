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

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.util.Util;

@Component(name = "jruyi.io.channeladmin", xmlns = "http://www.osgi.org/xmlns/scr/v1.2.0")
public final class ChannelAdmin implements IChannelAdmin {

	private static final Logger c_logger = LoggerFactory.getLogger(ChannelAdmin.class);

	private static int s_sequence = -1;

	private IoWorker[] m_iows;
	private int m_iowMask;

	private SelectorThread[] m_sts;

	@Override
	public void onRegisterRequired(ISelectableChannel channel) {
		getSelectorThread(channel.id().intValue()).onRegisterRequired(channel);
	}

	@Override
	public void onConnectRequired(ISelectableChannel channel) {
		getSelectorThread(channel.id().intValue()).onConnectRequired(channel);
	}

	@Override
	public IIoWorker designateIoWorker(ISelectableChannel channel) {
		return getIoWorker(channel.id().intValue());
	}

	@Override
	public void performIoTask(IIoTask task, Object msg) {
		getIoWorker(++s_sequence).perform(task, msg, null, 0);
	}

	public void activate(Map<String, ?> properties) throws Throwable {
		c_logger.info("Activating ChannelAdmin...");

		final int capacityOfIoRingBuffer = capacityOfIoRingBuffer(properties);

		int count = numberOfIoThreads(properties);
		final IoWorker[] iows = new IoWorker[count];
		for (int i = 0; i < count; ++i) {
			@SuppressWarnings("resource")
			final IoWorker iow = new IoWorker();
			iow.open(i, capacityOfIoRingBuffer);
			iows[i] = iow;
		}

		m_iowMask = count - 1;
		m_iows = iows;

		final SelectorThread[] sts;
		try {
			count = numberOfSelectors(properties);
			int capacityOfSelectorRingBuffer = capacityOfIoRingBuffer * count
					/ Runtime.getRuntime().availableProcessors();
			capacityOfSelectorRingBuffer = Util.ceilingNextPowerOfTwo(capacityOfSelectorRingBuffer);
			sts = new SelectorThread[count];
			for (int i = 0; i < count; ++i) {
				@SuppressWarnings("resource")
				final SelectorThread st = new SelectorThread();
				try {
					st.open(i, capacityOfSelectorRingBuffer);
				} catch (Exception e) {
					st.close();
					while (i > 0)
						sts[--i].close();
					throw e;
				}
				sts[i] = st;
			}
			m_sts = sts;
		} catch (Throwable t) {
			stopIoWorkers();
			throw t;
		}

		c_logger.info("ChannelAdmin activated: numberOfSelectors={}, numberOfIoThreads={}", sts.length, iows.length);
	}

	public void deactivate() {
		c_logger.info("Deactivating ChannelAdmin...");

		final SelectorThread[] sts = m_sts;
		m_sts = null;
		for (SelectorThread st : sts)
			st.close();

		stopIoWorkers();

		c_logger.info("ChannelAdmin deactivated");
	}

	private void stopIoWorkers() {
		final IoWorker[] iows = m_iows;
		m_iows = null;
		for (IoWorker iow : iows)
			iow.close();
	}

	private SelectorThread getSelectorThread(int id) {
		final SelectorThread[] sts = m_sts;
		final int i = (id & ~(1 << 31)) % sts.length;
		return sts[i];
	}

	private IoWorker getIoWorker(int id) {
		return m_iows[id & m_iowMask];
	}

	private static int capacityOfIoRingBuffer(Map<String, ?> properties) {
		final Object value = properties.get("capacityOfIoRingBuffer");
		int capacity;
		if (value == null || (capacity = (Integer) value) < 1)
			capacity = 1024 * 4;
		else
			capacity = Util.ceilingNextPowerOfTwo(capacity);

		return capacity;
	}

	private static int numberOfSelectors(Map<String, ?> properties) {
		final Object value = properties.get("numberOfSelectorThreads");
		int n;
		int i = Runtime.getRuntime().availableProcessors();
		if (value == null || (n = (Integer) value) < 1 || n >= i) {
			n = 0;
			while ((i >>>= 1) > 0)
				++n;
			if (n < 1)
				n = 1;
			int count = Util.ceilingNextPowerOfTwo(n);
			if (count > n)
				count >>>= 1;
			return count;
		}
		return n;
	}

	private static int numberOfIoThreads(Map<String, ?> properties) {
		final Object value = properties.get("numberOfIoThreads");
		int n;
		if (value == null || (n = (Integer) value) < 1)
			n = Runtime.getRuntime().availableProcessors();
		n = Util.ceilingNextPowerOfTwo(n);
		return n;
	}
}
