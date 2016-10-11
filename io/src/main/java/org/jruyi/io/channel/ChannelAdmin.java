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
import java.util.concurrent.atomic.AtomicInteger;

import org.jruyi.io.common.Util;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "jruyi.io.channeladmin", xmlns = "http://www.osgi.org/xmlns/scr/v1.2.0")
public final class ChannelAdmin implements IChannelAdmin {

	private static final Logger c_logger = LoggerFactory.getLogger(ChannelAdmin.class);

	private static final AtomicInteger c_sequence = new AtomicInteger(-1);
	private static final AtomicInteger c_taskSeq = new AtomicInteger(-1);

	private final int m_id;
	private int m_mask;
	private IoThread[] m_ioThreads;

	public ChannelAdmin() {
		m_id = c_sequence.incrementAndGet();
	}

	public ChannelAdmin(Integer numberOfIoThreads) {
		this();
		init(numberOfIoThreads);
	}

	@Override
	public ISelector designateSelector(int id) {
		return m_ioThreads[id & m_mask];
	}

	@Override
	public ISelector designateSelector(Object id) {
		int hash = id.hashCode() + c_taskSeq.incrementAndGet();
		return m_ioThreads[hash & m_mask];
	}

	void start() throws Throwable {
		final int id = m_id;
		final int numberOfIoThreads = m_mask + 1;
		final IoThread[] ioThreads;
		try {
			ioThreads = new IoThread[numberOfIoThreads];
			for (int i = 0; i < numberOfIoThreads; ++i) {
				@SuppressWarnings("resource")
				final IoThread ioThread = new IoThread();
				try {
					ioThread.open(id, i);
				} catch (Throwable t) {
					ioThread.close();
					while (i > 0)
						ioThreads[--i].close();
					throw t;
				}
				ioThreads[i] = ioThread;
			}
			m_ioThreads = ioThreads;
		} catch (Throwable t) {
			throw t;
		}

		c_logger.info("ChannelAdmin-{} started: numberOfIoThreads={}", id, numberOfIoThreads);
	}

	void stop() {
		final IoThread[] ioThreads = m_ioThreads;
		m_ioThreads = null;
		for (IoThread ioThread : ioThreads)
			ioThread.close();

		c_logger.info("ChannelAdmin-{} stopped", m_id);
	}

	public void activate(Map<String, ?> properties) throws Throwable {
		init((Integer) properties.get("numberOfIoThreads"));
		start();
		// c_logger.info("Default ChannelAdmin-{} activated", m_id);
	}

	public void deactivate() {
		// c_logger.info("Default ChannelAdmin-{} deactivated", m_id);
		stop();
	}

	private void init(Integer numberOfIoThreads) {
		final int n = numberOfIoThreads != null && numberOfIoThreads > 0 ? numberOfIoThreads
				: Runtime.getRuntime().availableProcessors();
		m_mask = Util.ceilingNextPowerOfTwo(n) - 1;
	}
}
