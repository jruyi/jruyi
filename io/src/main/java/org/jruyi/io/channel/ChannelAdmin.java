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

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jruyi.timeoutadmin.ITimeoutAdmin;
import org.jruyi.timeoutadmin.ITimeoutNotifier;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.util.Util;

@Component(name = "jruyi.io.channeladmin", xmlns = "http://www.osgi.org/xmlns/scr/v1.2.0")
public final class ChannelAdmin implements IChannelAdmin {

	private static final Logger c_logger = LoggerFactory.getLogger(ChannelAdmin.class);

	private static final AtomicInteger c_msgId = new AtomicInteger(-1);

	private BufferCache m_recvDirectBuffer;
	private BufferCache m_sendDirectBuffer;

	private IoThread[] m_iots;
	private int m_iotMask;

	private SelectorThread[] m_sts;
	private int m_stMask;

	private ITimeoutAdmin m_tm;

	static final class BufferCache extends ThreadLocal<ByteBuffer> {

		private final int m_capacity;

		BufferCache(int capacity) {
			m_capacity = capacity;
		}

		public int capacity() {
			return m_capacity;
		}

		@Override
		protected ByteBuffer initialValue() {
			return ByteBuffer.allocateDirect(m_capacity);
		}
	}

	@Override
	public void onRegisterRequired(ISelectableChannel channel) {
		getSelectorThread(channel.id().intValue()).onRegisterRequired(channel);
	}

	@Override
	public void onConnectRequired(ISelectableChannel channel) {
		getSelectorThread(channel.id().intValue()).onConnectRequired(channel);
	}

	@Override
	public void onAccept(ISelectableChannel channel) {
		channel.ioWorker(getIoThread(channel.id().intValue()));
		channel.onAccept();
	}

	@Override
	public ITimeoutNotifier createTimeoutNotifier(ISelectableChannel channel) {
		return m_tm.createNotifier(channel);
	}

	@Override
	public ByteBuffer recvDirectBuffer() {
		final ByteBuffer bb = m_recvDirectBuffer.get();
		bb.clear();
		return bb;
	}

	@Override
	public ByteBuffer sendDirectBuffer() {
		final ByteBuffer bb = m_sendDirectBuffer.get();
		bb.clear();
		return bb;
	}

	@Override
	public void performIoTask(IIoTask task, Object msg) {
		getIoThread(c_msgId.incrementAndGet()).perform(task, msg, null, 0);
	}

	@Reference(name = "timeoutAdmin", policy = ReferencePolicy.DYNAMIC)
	synchronized void setTimeoutAdmin(ITimeoutAdmin tm) {
		m_tm = tm;
	}

	synchronized void unsetTimeoutAdmin(ITimeoutAdmin tm) {
		if (m_tm == tm)
			m_tm = null;
	}

	void activate(Map<String, ?> properties) throws Throwable {
		c_logger.info("Activating ChannelAdmin...");

		m_recvDirectBuffer = new BufferCache(initCapacityOfRecvDirectBuffer(properties));
		m_sendDirectBuffer = new BufferCache(initCapacityOfSendDirectBuffer(properties));

		final int capacityOfIoRingBuffer = capacityOfIoRingBuffer(properties);

		int count = numberOfIoThreads(properties);
		final IoThread[] iots = new IoThread[count];
		for (int i = 0; i < count; ++i) {
			@SuppressWarnings("resource")
			final IoThread iot = new IoThread();
			iot.open(i, capacityOfIoRingBuffer);
			iots[i] = iot;
		}

		m_iotMask = count - 1;
		m_iots = iots;

		try {
			count = numberOfSelectors(properties);
			int capacityOfSelectorRingBuffer = capacityOfIoRingBuffer * count
					/ Runtime.getRuntime().availableProcessors();
			capacityOfSelectorRingBuffer = Util.ceilingNextPowerOfTwo(capacityOfSelectorRingBuffer);
			final SelectorThread[] sts = new SelectorThread[count];
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
			m_stMask = count - 1;
			m_sts = sts;
		} catch (Throwable t) {
			stopIoThreads();
			throw t;
		}

		c_logger.info("ChannelAdmin activated");
	}

	void deactivate() {
		c_logger.info("Deactivating ChannelAdmin...");

		final SelectorThread[] sts = m_sts;
		m_sts = null;
		for (SelectorThread st : sts)
			st.close();

		stopIoThreads();

		c_logger.info("ChannelAdmin deactivated");
	}

	private void stopIoThreads() {
		final IoThread[] iots = m_iots;
		m_iots = null;
		for (IoThread iot : iots)
			iot.close();
	}

	private SelectorThread getSelectorThread(int id) {
		return m_sts[id & m_stMask];
	}

	private IoThread getIoThread(int id) {
		return m_iots[id & m_iotMask];
	}

	private static int initCapacityOfRecvDirectBuffer(Map<String, ?> properties) {
		final Object value = properties.get("initCapacityOfRecvDirectBuffer");
		int capacity;
		if (value == null || (capacity = (Integer) value) < 8)
			capacity = 1024 * 64;

		return capacity;
	}

	private static int initCapacityOfSendDirectBuffer(Map<String, ?> properties) {
		final Object value = properties.get("initCapacityOfSendDirectBuffer");
		int capacity;
		if (value == null || (capacity = (Integer) value) < 8)
			capacity = 1024 * 64;

		return capacity;
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
		if (value == null || (n = (Integer) value) < 1) {
			int i = Runtime.getRuntime().availableProcessors();
			n = 0;
			while ((i >>>= 1) > 0)
				++n;
			if (n < 1)
				n = 1;
		}

		int count = Util.ceilingNextPowerOfTwo(n);
		if (count > n)
			count >>>= 1;
		return count;
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
