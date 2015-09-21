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

import java.util.concurrent.Executor;

import org.jruyi.common.ICloseable;
import org.jruyi.common.IThreadLocalCache;
import org.jruyi.common.StrUtil;
import org.jruyi.common.ThreadLocalCache;
import org.jruyi.io.IFilter;
import org.jruyi.io.common.SyncPutQueue;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

final class IoWorker implements ICloseable, EventHandler<IoEvent>, IIoWorker, Runnable {

	private static final IThreadLocalCache<IoEvent> c_cache = ThreadLocalCache.weakLinkedCache();

	private final SyncPutQueue<IoEvent> m_queue = new SyncPutQueue<>();
	private Disruptor<IoEvent> m_disruptor;

	static final class IoThread extends Thread {

		private final IoWorker m_ioWorker;

		public IoThread(Runnable command, String name, IoWorker ioWorker) {
			super(command, name);
			m_ioWorker = ioWorker;
		}

		IoWorker ioWorker() {
			return m_ioWorker;
		}
	}

	static final class IoExecutor implements Executor {

		private final int m_id;
		private final IoWorker m_ioWorker;

		IoExecutor(int id, IoWorker ioWorker) {
			m_id = id;
			m_ioWorker = ioWorker;
		}

		@Override
		public void execute(Runnable command) {
			new IoThread(command, StrUtil.join("jruyi-io-", m_id), m_ioWorker).start();
		}
	}

	@Override
	public void run() {
		final SyncPutQueue<IoEvent> queue = m_queue;
		IoEvent cachedEvent = queue.poll();
		if (cachedEvent == null)
			return;

		final RingBuffer<IoEvent> ringBuffer = m_disruptor.getRingBuffer();
		try {
			do {
				final long sequence = ringBuffer.tryNext();
				try {
					final IoEvent newEvent = ringBuffer.get(sequence);
					final Runnable command = cachedEvent.command();
					if (command == null)
						newEvent.task(cachedEvent.task()).msg(cachedEvent.msg()).filters(cachedEvent.filters())
								.filterCount(cachedEvent.filterCount());
					else
						newEvent.command(command);
				} finally {
					ringBuffer.publish(sequence);
				}
				cachedEvent.close();
			} while ((cachedEvent = queue.poll()) != null);
		} catch (InsufficientCapacityException e) {
			queue.put(cachedEvent);
		}
	}

	@Override
	public void onEvent(IoEvent event, long sequence, boolean endOfBatch) {
		Runnable command = event.command();
		if (command == null)
			event.task().run(event.msg(), event.filters(), event.filterCount());
		else
			command.run();

		run();
	}

	@SuppressWarnings("unchecked")
	public void open(int id, int capacity) {
		final Disruptor<IoEvent> disruptor = new Disruptor<>(IoEventFactory.INST, capacity, new IoExecutor(id, this));
		m_disruptor = disruptor;
		disruptor.handleEventsWith(this);
		disruptor.start();
	}

	@Override
	public void close() {
		final Disruptor<IoEvent> disruptor = m_disruptor;
		if (disruptor != null) {
			m_disruptor = null;
			disruptor.shutdown();
		}
	}

	@Override
	public void perform(IIoTask task) {
		perform(task, null, null, 0);
	}

	@Override
	public void perform(IIoTask task, Object msg, IFilter<?, ?>[] filters, int filterCount) {
		final RingBuffer<IoEvent> ringBuffer = m_disruptor.getRingBuffer();
		if (isIoThread()) {
			try {
				final long sequence = ringBuffer.tryNext();
				try {
					ringBuffer.get(sequence).task(task).msg(msg).filters(filters).filterCount(filterCount);
				} finally {
					ringBuffer.publish(sequence);
				}
			} catch (InsufficientCapacityException e) {
				final IoEvent ioEvent = createEvent(task, msg, filters, filterCount);
				m_queue.put(ioEvent);
				if (!isThisIoWorker())
					handleQueue();
			}
		} else {
			final long sequence = ringBuffer.next();
			try {
				final IoEvent event = ringBuffer.get(sequence);
				event.task(task).msg(msg).filters(filters).filterCount(filterCount);
			} finally {
				ringBuffer.publish(sequence);
			}
		}
	}

	@Override
	public void execute(Runnable command) {
		final RingBuffer<IoEvent> ringBuffer = m_disruptor.getRingBuffer();
		try {
			final long sequence = ringBuffer.tryNext();
			try {
				ringBuffer.get(sequence).command(command);
			} finally {
				ringBuffer.publish(sequence);
			}
		} catch (InsufficientCapacityException e) {
			final IoEvent ioEvent = createEvent(command);
			m_queue.put(ioEvent);
			if (!isIoThread() || !isThisIoWorker())
				handleQueue();
		}
	}

	private static boolean isIoThread() {
		return Thread.currentThread().getClass() == IoThread.class;
	}

	private static IoEvent createEvent(Runnable command) {
		IoEvent event = c_cache.take();
		if (event == null)
			event = new IoEvent();
		event.command(command);
		return event;
	}

	private static IoEvent createEvent(IIoTask task, Object msg, IFilter<?, ?>[] filters, int filterCount) {
		IoEvent event = c_cache.take();
		if (event == null)
			event = new IoEvent();
		event.task(task).msg(msg).filters(filters).filterCount(filterCount);
		return event;
	}

	private boolean isThisIoWorker() {
		return ((IoThread) Thread.currentThread()).ioWorker() == this;
	}

	private void handleQueue() {
		final RingBuffer<IoEvent> ringBuffer = m_disruptor.getRingBuffer();
		try {
			final long sequence = ringBuffer.tryNext();
			try {
				ringBuffer.get(sequence).command(this);
			} finally {
				ringBuffer.publish(sequence);
			}
		} catch (InsufficientCapacityException e) {
			// Ignore
		}
	}
}
