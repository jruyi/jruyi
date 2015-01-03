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

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jruyi.common.ICloseable;
import org.jruyi.common.StrUtil;
import org.jruyi.io.IFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

final class IoThread implements ICloseable, EventHandler<IoEvent>, IIoWorker {

	private static final Logger c_logger = LoggerFactory.getLogger(IoThread.class);

	private Disruptor<IoEvent> m_disruptor;

	static final class IoWorkerFactory implements ThreadFactory {

		private final int m_id;

		public IoWorkerFactory(int id) {
			m_id = id;
		}

		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, StrUtil.join("jruyi-io- ", m_id));
		}
	}

	@Override
	public void onEvent(IoEvent event, long sequence, boolean endOfBatch) {
		event.task().run(event.msg(), event.filters(), event.filterCount());
	}

	@SuppressWarnings("unchecked")
	public void open(int id, int capacityOfRingBuffer) {
		final Disruptor<IoEvent> disruptor = new Disruptor<IoEvent>(IoEventFactory.INST, capacityOfRingBuffer,
				Executors.newCachedThreadPool(new IoWorkerFactory(id)));
		m_disruptor = disruptor;
		disruptor.handleEventsWith(this);
		disruptor.start();
	}

	@Override
	public void close() {
		if (m_disruptor == null)
			return;

		m_disruptor.shutdown();
	}

	@Override
	public void perform(IIoTask task) {
		final RingBuffer<IoEvent> ringBuffer = m_disruptor.getRingBuffer();
		long sequence;
		try {
			sequence = ringBuffer.tryNext();
		} catch (InsufficientCapacityException e) {
			c_logger.warn("If you see this message quite a few, please try increasing numberOfIoThreads");
			sequence = ringBuffer.next();
		}
		try {
			final IoEvent event = ringBuffer.get(sequence);
			event.task(task);
		} finally {
			ringBuffer.publish(sequence);
		}
	}

	@Override
	public void perform(IIoTask task, Object msg, IFilter<?, ?>[] filters, int filterCount) {
		final RingBuffer<IoEvent> ringBuffer = m_disruptor.getRingBuffer();
		long sequence;
		try {
			sequence = ringBuffer.tryNext();
		} catch (InsufficientCapacityException e) {
			c_logger.warn("If you see this message quite a few, please try increasing numberOfIoThreads");
			sequence = ringBuffer.next();
		}
		try {
			final IoEvent event = ringBuffer.get(sequence);
			event.task(task).msg(msg).filters(filters).filterCount(filterCount);
		} finally {
			ringBuffer.publish(sequence);
		}
	}
}
