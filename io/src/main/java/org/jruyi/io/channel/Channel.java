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
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;
import java.util.IdentityHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.jruyi.common.ICloseable;
import org.jruyi.common.IDumpable;
import org.jruyi.common.IThreadLocalCache;
import org.jruyi.common.StrUtil;
import org.jruyi.common.StringBuilder;
import org.jruyi.common.ThreadLocalCache;
import org.jruyi.io.Codec;
import org.jruyi.io.IBuffer;
import org.jruyi.io.IBufferFactory;
import org.jruyi.io.IFilter;
import org.jruyi.io.IFilterOutput;
import org.jruyi.io.common.LinkedQueue;
import org.jruyi.timeoutadmin.ITimeoutEvent;
import org.jruyi.timeoutadmin.ITimeoutListener;
import org.jruyi.timeoutadmin.ITimeoutNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Channel implements IChannel, IDumpable, Runnable {

	private static final Logger c_logger = LoggerFactory.getLogger(Channel.class);
	private static final AtomicLong c_sequence = new AtomicLong(0L);
	private final Long m_id;
	private final IChannelService<Object, Object> m_channelService;
	private final AtomicBoolean m_closed;
	private volatile ConcurrentHashMap<Object, Object> m_attributes;
	private IdentityHashMap<Object, Object> m_storage;
	private Object m_attachment;
	private ISelector m_selector;
	private final IIoWorker m_ioWorker;
	private SelectionKey m_selectionKey;
	private ITimeoutNotifier m_timeoutNotifier;
	private ReadThread m_readThread;
	private WriteThread m_writeThread;

	static final class MsgArrayList implements ICloseable, IFilterOutput {

		private static final IThreadLocalCache<MsgArrayList> c_cache = ThreadLocalCache.weakArrayCache();
		private Object[] m_msgs;
		private int m_size;

		static MsgArrayList get() {
			MsgArrayList mal = c_cache.take();
			if (mal == null)
				mal = new MsgArrayList();
			return mal;
		}

		private MsgArrayList() {
			this(16);
		}

		private MsgArrayList(int initialCapacity) {
			m_msgs = new Object[initialCapacity];
		}

		int size() {
			return m_size;
		}

		void size(int newSize) {
			m_size = newSize;
		}

		Object take(int index) {
			final Object msg = m_msgs[index];
			m_msgs[index] = null;
			return msg;
		}

		boolean isEmpty() {
			return m_size < 1;
		}

		@Override
		public void add(Object msg) {
			if (msg == null)
				throw new NullPointerException();

			final int minCapacity = ++m_size;
			final int oldCapacity = m_msgs.length;
			if (minCapacity > oldCapacity) {
				final int newCapacity = (oldCapacity * 3) / 2 + 1;
				final Object[] oldMsgs = m_msgs;
				m_msgs = new Object[newCapacity];
				System.arraycopy(oldMsgs, 0, m_msgs, 0, oldCapacity);
			}
			m_msgs[minCapacity - 1] = msg;
		}

		void release() {
			final Object[] msgs = m_msgs;
			final int size = m_size;
			for (int i = 0; i < size; ++i) {
				final Object msg = msgs[i];
				if (msg != null) {
					if (msg instanceof AutoCloseable) {
						try {
							((AutoCloseable) msg).close();
						} catch (Throwable t) {
							c_logger.error(StrUtil.join("Failed to close message: ", msg), t);
						}
					}
					msgs[i] = null;
				}
			}
			m_size = 0;
		}

		@Override
		public void close() {
			release();
			c_cache.put(this);
		}
	}

	static final class FilterContext implements ICloseable {

		private static final IThreadLocalCache<FilterContext> c_cache = ThreadLocalCache.weakLinkedCache();
		private int m_msgLen;
		private IBuffer m_data;

		private FilterContext() {
		}

		static FilterContext get() {
			FilterContext context = c_cache.take();
			if (context == null)
				context = new FilterContext();

			return context;
		}

		int msgLen() {
			return m_msgLen;
		}

		void msgLen(int msgLen) {
			m_msgLen = msgLen;
		}

		IBuffer data() {
			return m_data;
		}

		void data(IBuffer data) {
			m_data = data;
		}

		void clear() {
			m_msgLen = 0;
			m_data = null;
		}

		@Override
		public void close() {
			m_msgLen = 0;
			m_data = null;
			c_cache.put(this);
		}
	}

	static final class ReadThread implements Runnable {

		private final Channel m_channel;

		ReadThread(Channel channel) {
			m_channel = channel;
		}

		@Override
		public void run() {
			final Channel channel = m_channel;
			final IChannelService<?, ?> cs = channel.channelService();
			final ByteBuffer bb = cs.getChannelAdmin().recvDirectBuffer();
			final ReadableByteChannel rbc = channel.readableByteChannel();
			final long throttle = cs.throttle();
			final int capacity = bb.capacity();
			long length = 0L;
			for (;;) {
				int n;
				try {
					n = rbc.read(bb);
				} catch (Throwable t) {
					if (!channel.isClosed())
						channel.onException(t);
					return;
				}

				if (n < 0) {
					channel.close();
					return;
				} else if (n == 0)
					break;

				final boolean ok;
				try {
					bb.flip();
					ok = channel.onReadIn(bb);
				} catch (Throwable t) {
					channel.onException(t);
					return;
				}

				if (!ok) {
					channel.close();
					return;
				}

				if (n < capacity || (length += n) >= throttle)
					break;

				bb.clear();
			}

			try {
				channel.onReadRequired();
			} catch (Throwable t) {
				channel.onException(t);
			}
		}
	}

	static final class OutMsg implements ICloseable {

		private static final IThreadLocalCache<OutMsg> c_cache = ThreadLocalCache.weakLinkedCache();

		private Object m_msg;
		private IFilter<?, ?>[] m_filters;
		private int m_filterCount;
		private boolean m_original;

		private OutMsg() {
		}

		static OutMsg get(Object msg, IFilter<?, ?>[] filters, int filterCount, boolean original) {
			OutMsg outMsg = c_cache.take();
			if (outMsg == null)
				outMsg = new OutMsg();

			outMsg.m_msg = msg;
			outMsg.m_filters = filters;
			outMsg.m_filterCount = filterCount;
			outMsg.m_original = original;

			return outMsg;
		}

		Object attachMsg(Object msg) {
			final Object oldMsg = m_msg;
			m_msg = msg;
			return oldMsg;
		}

		IFilter<?, ?>[] attachFilters(IFilter<?, ?>[] filters) {
			final IFilter<?, ?>[] oldFilters = m_filters;
			m_filters = filters;
			return oldFilters;
		}

		int attachFilterCount(int filterCount) {
			final int oldFilterCount = m_filterCount;
			m_filterCount = filterCount;
			return oldFilterCount;
		}

		boolean attachOriginalMsg(boolean original) {
			final boolean oldOriginal = m_original;
			m_original = original;
			return oldOriginal;
		}

		Object msg() {
			return m_msg;
		}

		IFilter<?, ?>[] filters() {
			return m_filters;
		}

		int filterCount() {
			return m_filterCount;
		}

		boolean isOriginal() {
			return m_original;
		}

		@Override
		public void close() {
			m_msg = null;
			m_filters = null;
			c_cache.put(this);
		}
	}

	static final class WriteThread implements IIoTask {

		private final Channel m_channel;
		private final LinkedQueue<OutMsg> m_queue;
		private final ZombieWriteThread m_zombieWriteThread;

		private IBuffer m_data;
		private Object m_originalMsg;

		final class ZombieWriteThread implements IIoTask {

			@Override
			public void run(Object msg, IFilter<?, ?>[] filters, int filterCount) {
				write(msg, filters, filterCount, false);
			}
		}

		WriteThread(Channel channel) {
			m_channel = channel;
			m_queue = new LinkedQueue<>();
			m_zombieWriteThread = new ZombieWriteThread();
		}

		ZombieWriteThread zombieWriteThread() {
			return m_zombieWriteThread;
		}

		@Override
		public void run(Object msg, IFilter<?, ?>[] filters, int filterCount) {
			write(msg, filters, filterCount, true);
		}

		void write(Object msg, IFilter<?, ?>[] filters, int filterCount, boolean original) {
			final Channel channel = m_channel;
			final boolean newMsg;
			IBuffer data = m_data;
			if (data == null) {
				final OutMsg outMsg = m_queue.poll();
				if (outMsg != null) {
					if (msg != null) {
						msg = outMsg.attachMsg(msg);
						filters = outMsg.attachFilters(filters);
						filterCount = outMsg.attachFilterCount(filterCount);
						original = outMsg.attachOriginalMsg(original);
						m_queue.put(outMsg);
					} else {
						msg = outMsg.msg();
						filters = outMsg.filters();
						filterCount = outMsg.filterCount();
						original = outMsg.isOriginal();
						outMsg.close();
					}
				}

				try {
					data = filter(msg, filters, filterCount, channel);
				} catch (Throwable t) {
					if (!channel.isClosed())
						channel.onException(t);
					return;
				}
				if (data == null)
					return;
				newMsg = true;
			} else {
				if (msg != null)
					m_queue.put(OutMsg.get(msg, filters, filterCount, original));
				newMsg = false;
			}

			if (original)
				m_originalMsg = msg;

			final IChannelService<Object, Object> cs = channel.channelService();
			final ByteBuffer bb = cs.getChannelAdmin().sendDirectBuffer();
			final WritableByteChannel wbc = channel.writableByteChannel();
			try {
				while (!data.isEmpty()) {
					data.mark();
					data.read(bb, Codec.byteBuffer());
					bb.flip();
					int len = 0;
					do {
						final int n = wbc.write(bb);
						len += n;
						if (n < 1) {
							data.reset();
							data.skip(len);
							data.compact();
							if (newMsg)
								m_data = data;
							channel.onWriteRequired();
							return;
						}
					} while (bb.hasRemaining());
					bb.clear();
				}
				if (m_originalMsg != null)
					cs.onMessageSent(channel, m_originalMsg);
				clear(data);
			} catch (Throwable t) {
				clear(data);
				if (!channel.isClosed())
					channel.onException(t);
			}
		}

		private void clear(IBuffer data) {
			m_data = null;
			m_originalMsg = null;
			data.close();
		}

		private IBuffer filter(Object msg, IFilter<?, ?>[] filterArray, int index, Channel channel) {
			IBuffer data = null;
			if (index > 0) {
				@SuppressWarnings("unchecked")
				final IFilter<Object, Object>[] filters = (IFilter<Object, Object>[]) filterArray;
				MsgArrayList inMsgs = MsgArrayList.get();
				MsgArrayList outMsgs = MsgArrayList.get();
				try {
					outMsgs.add(msg);
					int n = outMsgs.size();
					do {
						MsgArrayList temp = inMsgs;
						inMsgs = outMsgs;
						outMsgs = temp;

						for (int i = 0; i < n; ++i) {
							if (!filters[--index].onMsgDepart(channel, inMsgs.take(i), outMsgs))
								return null;
						}

						inMsgs.size(0);
						n = outMsgs.size();
					} while (index > 0 && n > 0);

					if (n > 0) {
						try {
							int i = 0;
							data = (IBuffer) outMsgs.take(i);
							while (++i < n) {
								try (IBuffer buf = (IBuffer) outMsgs.take(i)) {
									buf.drainTo(data);
								}
							}
							outMsgs.size(0);
						} catch (ClassCastException e) {
							throw new RuntimeException(StrUtil.join(filters[0],
									"has to produce departure data of type ", IBuffer.class.getName()));
						}
					}
				} finally {
					inMsgs.close();
					outMsgs.close();
				}
			} else {
				try {
					data = (IBuffer) msg;
				} catch (ClassCastException e) {
					throw new RuntimeException(
							StrUtil.join("Departure data has to be of type", IBuffer.class.getName()));
				}
			}

			return data;
		}
	}

	static final class IdleTimeoutListener implements ITimeoutListener {

		static final ITimeoutListener INST = new IdleTimeoutListener();

		@Override
		public void onTimeout(ITimeoutEvent event) {
			final Channel channel = (Channel) event.getSubject();
			try {
				channel.channelService().onChannelIdleTimedOut(channel);
			} catch (Throwable t) {
				channel.onException(t);
			}
		}
	}

	static final class ConnectTimeoutListener implements ITimeoutListener {

		static final ITimeoutListener INST = new ConnectTimeoutListener();

		@Override
		public void onTimeout(ITimeoutEvent event) {
			final Channel channel = (Channel) event.getSubject();
			try {
				channel.channelService().onChannelConnectTimedOut(channel);
			} catch (Throwable t) {
				channel.onException(t);
			}
		}
	}

	static final class ReadTimeoutListener implements ITimeoutListener {

		static final ITimeoutListener INST = new ReadTimeoutListener();

		@Override
		public void onTimeout(ITimeoutEvent event) {
			final Channel channel = (Channel) event.getSubject();
			try {
				channel.channelService().onChannelReadTimedOut(channel);
			} catch (Throwable t) {
				channel.onException(t);
			}
		}
	}

	protected Channel(IChannelService<Object, Object> channelService) {
		m_id = generateId();
		m_channelService = channelService;
		m_closed = new AtomicBoolean(false);
		m_ioWorker = channelService.getChannelAdmin().designateIoWorker(this);
	}

	@Override
	public final void run() {
		final IChannelService<Object, Object> channelService = m_channelService;
		try {
			m_storage = new IdentityHashMap<>();

			onAccepted();

			createReadThread();
			createWriteThread();

			IChannelAdmin ca = channelService.getChannelAdmin();
			m_timeoutNotifier = createTimeoutNotifier(ca);
			selectableChannel().configureBlocking(false);

			channelService.onChannelOpened(this);

			ca.onRegisterRequired(this);

		} catch (Throwable t) {
			onException(t);
		}
	}

	@Override
	public final void onReadRequired() {
		m_selector.onReadRequired(this);
	}

	@Override
	public final boolean scheduleIdleTimeout(int timeout) {
		final ITimeoutNotifier timeoutNotifier = m_timeoutNotifier;
		if (timeoutNotifier == null)
			return false;

		timeoutNotifier.setListener(IdleTimeoutListener.INST);
		timeoutNotifier.setExecutor(m_ioWorker);
		return timeoutNotifier.schedule(timeout);
	}

	@Override
	public final boolean scheduleConnectTimeout(int timeout) {
		final ITimeoutNotifier timeoutNotifier = m_timeoutNotifier;
		if (timeoutNotifier == null)
			return false;

		timeoutNotifier.setListener(ConnectTimeoutListener.INST);
		timeoutNotifier.setExecutor(m_ioWorker);
		return timeoutNotifier.schedule(timeout);
	}

	@Override
	public final boolean scheduleReadTimeout(int timeout) {
		final ITimeoutNotifier timeoutNotifier = m_timeoutNotifier;
		if (timeoutNotifier == null)
			return false;

		timeoutNotifier.setListener(ReadTimeoutListener.INST);
		timeoutNotifier.setExecutor(m_ioWorker);
		return timeoutNotifier.schedule(timeout);
	}

	@Override
	public final boolean cancelTimeout() {
		final ITimeoutNotifier timeoutNotifier = m_timeoutNotifier;
		return timeoutNotifier != null && timeoutNotifier.cancel();
	}

	@Override
	public final Object deposit(Object id, Object something) {
		return m_storage.put(id, something);
	}

	@Override
	public final Object withdraw(Object id) {
		return m_storage.remove(id);
	}

	@Override
	public final Object inquiry(Object id) {
		return m_storage.get(id);
	}

	@Override
	public final IBuffer createBuffer() {
		return m_channelService.getBufferFactory().create();
	}

	@Override
	public final void write(Object msg) {
		try {
			if (msg == null)
				return;

			final IFilter<?, ?>[] filters = channelService().getFilterChain();
			m_ioWorker.perform(m_writeThread, msg, filters, filters.length);
		} catch (Throwable t) {
			onException(t);
		}
	}

	@Override
	public final void close() {
		final AtomicBoolean closed = m_closed;
		if (closed.get() || !closed.compareAndSet(false, true))
			return;

		try {
			onClose();
		} catch (Throwable t) {
			onException(t);
		}

		final ITimeoutNotifier tn = m_timeoutNotifier;
		if (tn != null)
			tn.close();

		try {
			m_channelService.onChannelClosed(this);
		} catch (Throwable t) {
			c_logger.error("Unexpected Error", t);
		}
	}

	@Override
	public final void dump(StringBuilder builder) {
		builder.append(m_channelService).append(" Session#").append(m_id);
	}

	@Override
	public final String toString() {
		try (final StringBuilder builder = StringBuilder.get()) {
			dump(builder);
			return builder.toString();
		}
	}

	@Override
	public final Long id() {
		return m_id;
	}

	@Override
	public final Object get(Object key) {
		final ConcurrentHashMap<Object, Object> attributes = m_attributes;
		if (attributes == null) {
			if (key == null)
				throw new NullPointerException();
			return null;
		}
		return attributes.get(key);
	}

	@Override
	public final Object put(Object key, Object value) {
		ConcurrentHashMap<Object, Object> attributes = m_attributes;
		if (attributes == null) {
			synchronized (this) {
				attributes = m_attributes;
				if (attributes == null) {
					attributes = new ConcurrentHashMap<>();
					m_attributes = attributes;
				}
			}
		}
		return attributes.put(key, value);
	}

	@Override
	public final Object remove(Object key) {
		final ConcurrentHashMap<Object, Object> attributes = m_attributes;
		if (attributes == null) {
			if (key == null)
				throw new NullPointerException();
			return null;
		}
		return attributes.remove(key);
	}

	@Override
	public boolean contains(Object key) {
		final ConcurrentHashMap<Object, Object> attributes = m_attributes;
		if (attributes == null) {
			if (key == null)
				throw new NullPointerException();
			return false;
		}
		return attributes.contains(key);
	}

	@Override
	public Object putIfAbsent(Object key, Object value) {
		ConcurrentHashMap<Object, Object> attributes = m_attributes;
		if (attributes == null) {
			synchronized (this) {
				attributes = m_attributes;
				if (attributes == null) {
					attributes = new ConcurrentHashMap<>();
					m_attributes = attributes;
				}
			}
		}
		return attributes.putIfAbsent(key, value);
	}

	@Override
	public boolean remove(Object key, Object value) {
		final ConcurrentHashMap<Object, Object> attributes = m_attributes;
		if (attributes == null) {
			if (key == null || value == null)
				throw new NullPointerException();
			return false;
		}
		return attributes.remove(key, value);
	}

	@Override
	public Object replace(Object key, Object value) {
		final ConcurrentHashMap<Object, Object> attributes = m_attributes;
		if (attributes == null) {
			if (key == null || value == null)
				throw new NullPointerException();
			return null;
		}
		return attributes.replace(key, value);
	}

	@Override
	public boolean replace(Object key, Object oldValue, Object newValue) {
		final ConcurrentHashMap<Object, Object> attributes = m_attributes;
		if (attributes == null) {
			if (key == null || oldValue == null || newValue == null)
				throw new NullPointerException();
			return false;
		}
		return attributes.replace(key, oldValue, newValue);
	}

	@Override
	public final boolean isClosed() {
		return m_closed.get();
	}

	@Override
	public final Object attach(Object attachment) {
		final Object oldAttachment = m_attachment;
		m_attachment = attachment;
		return oldAttachment;
	}

	@Override
	public final Object attachment() {
		return m_attachment;
	}

	@Override
	public final Object detach() {
		final Object oldAttachment = m_attachment;
		m_attachment = null;
		return oldAttachment;
	}

	@Override
	public final IChannelService<Object, Object> channelService() {
		return m_channelService;
	}

	@Override
	public final void register(ISelector selector, int ops) throws Throwable {
		m_selectionKey = selectableChannel().register(selector.selector(), ops, this);
		m_selector = selector;
	}

	@Override
	public final void interestOps(int ops) {
		final SelectionKey selectionKey = m_selectionKey;
		selectionKey.interestOps(selectionKey.interestOps() | ops);
	}

	@Override
	public final void connect(int timeout) {
		try {
			m_storage = new IdentityHashMap<>();

			final IChannelAdmin ca = m_channelService.getChannelAdmin();
			m_timeoutNotifier = createTimeoutNotifier(ca);
			if (connect()) {
				onConnectInternal(true);
			} else {
				if (timeout > 0)
					scheduleConnectTimeout(timeout);
				ca.onConnectRequired(this);
			}
		} catch (Throwable t) {
			onException(t);
		}
	}

	@Override
	public final void onConnect() {
		// if false, it's timeout
		if (!cancelTimeout())
			return;

		onConnectInternal(false);
	}

	@Override
	public final void onRead() {
		m_ioWorker.execute(m_readThread);
	}

	@Override
	public final void onWrite() {
		m_ioWorker.perform(m_writeThread.zombieWriteThread());
	}

	@Override
	public final void receive(ByteBuffer bb) {
		try {
			if (onReadIn(bb))
				return;
			close();
		} catch (Throwable t) {
			onException(t);
		}
	}

	@Override
	public final void onException(Throwable t) {
		try {
			m_channelService.onChannelException(this, t);
		} catch (Throwable e) {
			c_logger.error(StrUtil.join("Unexpected Error: ", this), e);
		}
	}

	@Override
	public final void onAccept() {
		m_ioWorker.execute(this);
	}

	protected abstract SelectableChannel selectableChannel();

	protected abstract void onAccepted() throws Exception;

	protected abstract void onClose() throws Exception;

	protected abstract boolean connect() throws Exception;

	protected abstract void onConnected() throws Exception;

	protected abstract ReadableByteChannel readableByteChannel();

	protected abstract WritableByteChannel writableByteChannel();

	protected ITimeoutNotifier createTimeoutNotifier(IChannelAdmin ca) {
		return ca.createTimeoutNotifier(this);
	}

	// If returns false, this channel need be closed
	final boolean onReadIn(ByteBuffer bb) {
		final IChannelService<Object, Object> cs = m_channelService;
		final IFilter<?, ?>[] filters = cs.getFilterChain();
		final int m = filters.length;
		if (m < 1) {
			final IBuffer buffer = cs.getBufferFactory().create();
			buffer.write(bb, Codec.byteBuffer());
			cs.onMessageReceived(this, buffer);
			return true;
		}

		MsgArrayList outMsgs = MsgArrayList.get();
		try {
			if (!onAccumulate(filters, outMsgs, bb))
				return false;

			if (outMsgs.isEmpty())
				return true;

			if (m < 2) {
				for (int i = 0, n = outMsgs.size(); i < n; ++i)
					cs.onMessageReceived(this, outMsgs.take(i));
				return true;
			}
		} catch (Throwable t) {
			outMsgs.close();
			throw t;
		}

		MsgArrayList inMsgs = outMsgs;
		try {
			outMsgs = MsgArrayList.get();
			Object in = inMsgs.take(0);
			for (int k = 1; k < m; ++k) {
				if (in instanceof IBuffer) {
					if (!onAccumulate(k, filters, inMsgs, outMsgs, (IBuffer) in))
						return false;
				} else {
					int n = inMsgs.size();
					for (int i = 1;; ++i) {
						if (!onMsgArrive(k, filters, outMsgs, in))
							return false;
						if (i >= n)
							break;
						in = inMsgs.take(i);
					}
				}

				if (outMsgs.isEmpty())
					return true;

				final MsgArrayList temp = inMsgs;
				inMsgs = outMsgs;
				outMsgs = temp;

				in = inMsgs.take(0);
			}

			cs.onMessageReceived(this, in);
			for (int i = 1, n = inMsgs.size(); i < n; ++i)
				cs.onMessageReceived(this, inMsgs.take(i));
		} finally {
			inMsgs.close();
			outMsgs.close();
		}
		return true;
	}

	final void onWriteRequired() {
		m_selector.onWriteRequired(this);
	}

	private static void write(ByteBuffer src, IBuffer dst, int length) {
		final int limit = src.limit();
		src.limit(src.position() + length);
		dst.write(src, Codec.byteBuffer());
		src.limit(limit);
	}

	@SuppressWarnings("resource")
	private boolean onAccumulate(IFilter<?, ?>[] filters, MsgArrayList outMsgs, ByteBuffer bb) {
		final IFilter<?, ?> filter = filters[0];
		final IBufferFactory bf = m_channelService.getBufferFactory();
		// mergeContext -start
		int msgLen;
		IBuffer in;
		FilterContext context = (FilterContext) withdraw(filter);
		if (context != null) {
			in = context.data();
			msgLen = context.msgLen();
			context.close();
			// context = null;
		} else {
			in = bf.create();
			msgLen = 0;
		}
		// mergeContext -end

		final int msgMinSize = filter.msgMinSize();
		outer: for (;;) {
			if (msgLen == 0) {
				if (msgMinSize <= 0)
					in.write(bb, Codec.byteBuffer());
				else if (bb.hasRemaining()) {
					final int n = msgMinSize - in.length();
					if (n > 0 && bb.remaining() >= n)
						write(bb, in, n);
					else {
						in.write(bb, Codec.byteBuffer());
						break;
					}
				}

				for (;;) {
					msgLen = filter.tellBoundary(this, in);
					if (msgLen < 0) { // error
						in.close();
						return false;
					}

					if (msgLen > 0)
						break;

					if (!bb.hasRemaining())
						break outer;

					in.write(bb, Codec.byteBuffer());
				}
			}

			int inLen = in.length();
			if (inLen < msgLen) {
				final int remaining = bb.remaining();
				if (remaining < 1)
					break;
				final int n = msgLen - inLen;
				if (remaining > n) {
					write(bb, in, n);
					if (!onMsgArrive(0, filters, outMsgs, in))
						return false;
					in = bf.create();
					msgLen = 0;
				} else if (remaining == n) {
					write(bb, in, n);
					return onMsgArrive(0, filters, outMsgs, in);
				} else {
					in.write(bb, Codec.byteBuffer());
					break;
				}
			} else if (inLen > msgLen) {
				if (!onMsgArrive(0, filters, outMsgs, in.split(msgLen))) {
					in.close();
					return false;
				}
				in.rewind();
				msgLen = 0;
			} else {
				if (!onMsgArrive(0, filters, outMsgs, in))
					return false;
				if (!bb.hasRemaining())
					return true;
				in = bf.create();
				msgLen = 0;
			}
		}

		// storeContext - start
		context = FilterContext.get();
		context.data(in);
		context.msgLen(msgLen);
		deposit(filter, context);
		// storeContext - end

		return true;
	}

	@SuppressWarnings("resource")
	private boolean onAccumulate(int k, IFilter<?, ?>[] filters, MsgArrayList inMsgs, MsgArrayList outMsgs, IBuffer in) {
		final IFilter<?, ?> filter = filters[k];
		// mergeContext -start
		int msgLen = 0;
		FilterContext context = (FilterContext) withdraw(filter);
		if (context != null) {
			final IBuffer prevData = context.data();
			if (prevData != null) {
				in.drainTo(prevData);
				in.close();
				in = prevData;
			}

			msgLen = context.msgLen();
			context.close();
			// context = null;
		}
		// mergeContext -end

		int i = 1; // the given buffer "in" is actually inMsgs.take(0)
		int n = inMsgs.size();
		final int msgMinSize = filter.msgMinSize();
		for (;;) {
			final int inLen = in.length();
			if (msgLen == 0 && inLen >= msgMinSize) {
				msgLen = filter.tellBoundary(this, in);
				if (msgLen < 0) { // ERROR
					in.close();
					return false;
				}
			}

			if (msgLen == 0 || inLen < msgLen) {
				if (i < n) {
					final IBuffer data = (IBuffer) inMsgs.take(i);
					++i;
					data.drainTo(in);
					data.close();
					continue;
				}
			} else if (inLen > msgLen) {
				if (!onMsgArrive(k, filters, outMsgs, in.split(msgLen))) {
					in.close();
					return false;
				}
				in.rewind();
				msgLen = 0;
				continue;
			} else {
				if (!onMsgArrive(k, filters, outMsgs, in))
					return false;
				msgLen = 0;
				if (i < n) {
					in = (IBuffer) inMsgs.take(i);
					++i;
					continue;
				} else
					in = null;
			}

			break;
		}

		inMsgs.size(0); // clear

		// storeContext - start
		if (in != null) {
			context = FilterContext.get();
			context.data(in);
			context.msgLen(msgLen);
			deposit(filter, context);
		}
		// storeContext - end

		return true;
	}

	private void onConnectInternal(boolean requireRegister) {
		try {
			onConnected();

			createReadThread();
			createWriteThread();

			final IChannelService<Object, Object> channelService = m_channelService;
			channelService.onChannelOpened(this);

			if (requireRegister)
				channelService.getChannelAdmin().onRegisterRequired(this);
			else
				onReadRequired();
		} catch (Throwable t) {
			onException(t);
		}
	}

	private boolean onMsgArrive(int index, IFilter<?, ?>[] filters, MsgArrayList outMsgs, Object msg) {
		final int size = outMsgs.size();

		@SuppressWarnings("unchecked")
		final boolean ok = ((IFilter<Object, ?>) filters[index]).onMsgArrive(this, msg, outMsgs);
		final int n = outMsgs.size();
		if (n == size || ok)
			return ok;

		++index;
		final WriteThread.ZombieWriteThread zombieWriteThread = m_writeThread.zombieWriteThread();
		final IIoWorker ioWorker = m_ioWorker;
		for (int i = size; i < n; ++i)
			ioWorker.perform(zombieWriteThread, outMsgs.take(i), filters, index);

		outMsgs.size(size);
		return true;
	}

	private void createWriteThread() {
		m_writeThread = new WriteThread(this);
	}

	private void createReadThread() {
		m_readThread = new ReadThread(this);
	}

	private long generateId() {
		return c_sequence.incrementAndGet();
	}
}
