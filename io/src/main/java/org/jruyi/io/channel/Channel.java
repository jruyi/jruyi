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

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;
import java.util.IdentityHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jruyi.common.ICloseable;
import org.jruyi.common.IDumpable;
import org.jruyi.common.IThreadLocalCache;
import org.jruyi.common.StrUtil;
import org.jruyi.common.StringBuilder;
import org.jruyi.common.ThreadLocalCache;
import org.jruyi.io.IBuffer;
import org.jruyi.io.IFilter;
import org.jruyi.io.IFilterOutput;
import org.jruyi.io.common.LinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Channel implements IChannel, IDumpable {

	private static final Logger c_logger = LoggerFactory.getLogger(Channel.class);

	private final Long m_id;
	private final IChannelService<Object, Object> m_channelService;
	private final AtomicBoolean m_closed;
	private final ISelector m_selector;
	private ConcurrentHashMap<Object, Object> m_attributes;
	private IdentityHashMap<Object, Object> m_storage;
	private Object m_attachment;
	private SelectionKey m_selectionKey;
	private Timer m_timer;
	private int m_connectTimeout;
	private WriteThread m_writeThread;

	static final class MsgArrayList implements ICloseable, IFilterOutput {

		private static final IThreadLocalCache<MsgArrayList> c_cache = ThreadLocalCache.weakArrayCache();
		private Object[] m_msgs;
		private int m_size;
		private boolean m_more;

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

		boolean more() {
			return m_more;
		}

		@Override
		public void more(boolean more) {
			m_more = more;
		}

		@Override
		public void add(Object output) {
			if (output == null)
				throw new NullPointerException();

			final int minCapacity = ++m_size;
			final int oldCapacity = m_msgs.length;
			if (minCapacity > oldCapacity) {
				final int newCapacity = (oldCapacity * 3) / 2 + 1;
				final Object[] oldMsgs = m_msgs;
				m_msgs = new Object[newCapacity];
				System.arraycopy(oldMsgs, 0, m_msgs, 0, oldCapacity);
			}
			m_msgs[minCapacity - 1] = output;
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

		@Override
		public void close() {
			m_msgLen = 0;
			m_data = null;
			c_cache.put(this);
		}
	}

	static final class OutMsg implements ICloseable {

		private static final IThreadLocalCache<OutMsg> c_cache = ThreadLocalCache.weakLinkedCache();

		private Object m_msg;
		private IFilter<?, ?>[] m_filters;
		private int m_filterCount;

		private OutMsg() {
		}

		static OutMsg get(Object msg, IFilter<?, ?>[] filters, int filterCount) {
			OutMsg outMsg = c_cache.take();
			if (outMsg == null)
				outMsg = new OutMsg();

			outMsg.m_msg = msg;
			outMsg.m_filters = filters;
			outMsg.m_filterCount = filterCount;

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

		Object msg() {
			return m_msg;
		}

		IFilter<?, ?>[] filters() {
			return m_filters;
		}

		int filterCount() {
			return m_filterCount;
		}

		@Override
		public void close() {
			m_msg = null;
			m_filters = null;
			c_cache.put(this);
		}
	}

	static final class WriteThread implements IIoTask {

		static final Object EOF = new Object();
		private final Channel m_channel;
		private final LinkedQueue<OutMsg> m_queue;

		private IBuffer m_data;
		private Object m_originalMsg;
		private int m_indexOfMore = -1;

		WriteThread(Channel channel) {
			m_channel = channel;
			m_queue = new LinkedQueue<>();
		}

		@Override
		public void run(Object msg, IFilter<?, ?>[] filters, int filterCount) {
			final Channel channel = m_channel;
			if (msg == EOF) {
				channel.onCloseInternal();
				return;
			}
			IBuffer data = m_data;
			if (data != null) {
				if (msg != null)
					m_queue.put(OutMsg.get(msg, filters, filterCount));
			} else {
				if (m_indexOfMore < 0) {
					final OutMsg outMsg = m_queue.poll();
					if (outMsg != null) {
						if (msg != null) {
							msg = outMsg.attachMsg(msg);
							filters = outMsg.attachFilters(filters);
							filterCount = outMsg.attachFilterCount(filterCount);
							m_queue.put(outMsg);
						} else {
							msg = outMsg.msg();
							filters = outMsg.filters();
							filterCount = outMsg.filterCount();
							outMsg.close();
						}
					}
					channel.channelService().beforeSendMessage(channel, msg);
					m_originalMsg = msg;
				}

				try {
					@SuppressWarnings("unchecked")
					final IFilter<?, Object>[] filterChain = (IFilter<?, Object>[]) filters;
					data = filter(msg, filterChain, filterCount, channel);
				} catch (Throwable t) {
					m_indexOfMore = -1;
					if (!channel.isClosed())
						channel.onException(t);
					return;
				}
				if (data == null)
					return;
			}
			write(data, channel);
		}

		void writeBackward(Object msg, IFilter<?, ?>[] filters, int filterCount) {
			final Channel channel = m_channel;
			IBuffer data = m_data;
			if (data != null)
				m_queue.put(OutMsg.get(msg, filters, filterCount));
			else {
				try {
					@SuppressWarnings("unchecked")
					final IFilter<?, Object>[] filterChain = (IFilter<?, Object>[]) filters;
					data = filter(msg, filterChain, filterCount, channel);
				} catch (Throwable t) {
					m_indexOfMore = -1;
					if (!channel.isClosed())
						channel.onException(t);
					return;
				}
				if (data == null)
					return;
			}
			write(data, channel);
		}

		private void write(IBuffer data, Channel channel) {
			final IChannelService<Object, Object> cs = channel.channelService();
			final WritableByteChannel wbc = channel.writableByteChannel();
			for (;;) {
				final Object msg;
				final IFilter<?, ?>[] filters;
				final int filterCount;
				try {
					data.writeOut(wbc);
					if (!data.isEmpty()) {
						m_data = data;
						channel.interestOps(SelectionKey.OP_WRITE);
						return;
					}

					if (m_indexOfMore >= 0) {
						clear(data);
						return;
					}

					cs.onMessageSent(channel, m_originalMsg);
					clear(data);
					final OutMsg outMsg = m_queue.poll();
					if (outMsg == null) {
						m_originalMsg = null;
						return;
					}

					msg = outMsg.msg();
					filters = outMsg.filters();
					filterCount = outMsg.filterCount();
					outMsg.close();
					cs.beforeSendMessage(channel, msg);

					m_originalMsg = msg;
				} catch (Throwable t) {
					m_originalMsg = null;
					clear(data);
					if (!channel.isClosed())
						channel.onException(t);
					return;
				}

				try {
					@SuppressWarnings("unchecked")
					final IFilter<?, Object>[] filterChain = (IFilter<?, Object>[]) filters;
					data = filter(msg, filterChain, filterCount, channel);
				} catch (Throwable t) {
					m_indexOfMore = -1;
					if (!channel.isClosed())
						channel.onException(t);
					return;
				}
				if (data == null)
					return;
			}
		}

		private void clear(IBuffer data) {
			m_data = null;
			data.close();
		}

		private IBuffer filter(Object msg, IFilter<?, Object>[] filters, int index, Channel channel) {
			if (index < 1) {
				try {
					return (IBuffer) msg;
				} catch (ClassCastException e) {
					throw new RuntimeException(
							StrUtil.join("Departure data has to be of type", IBuffer.class.getName()));
				}
			}

			int indexOfMore = m_indexOfMore;
			if (indexOfMore > filters.length)
				indexOfMore = -1;
			MsgArrayList inMsgs = MsgArrayList.get();
			MsgArrayList outMsgs = MsgArrayList.get();
			try {
				outMsgs.add(msg);
				int n = outMsgs.size();
				do {
					final MsgArrayList temp = inMsgs;
					inMsgs = outMsgs;
					outMsgs = temp;
					outMsgs.more(false);
					final IFilter<?, Object> filter = filters[--index];
					int i = 0;
					try {
						for (; i < n; ++i) {
							if (!filter.onMsgDepart(channel, inMsgs.take(i), outMsgs))
								return null;
						}
					} finally {
						if (outMsgs.more()) {
							if (indexOfMore < 0)
								indexOfMore = i;
						} else if (indexOfMore == i)
							indexOfMore = -1;
					}

					inMsgs.size(0);
					n = outMsgs.size();
				} while (index > 0 && n > 0);

				m_indexOfMore = indexOfMore;

				if (n < 1)
					return null;

				try {
					int i = 0;
					final IBuffer data = (IBuffer) outMsgs.take(i);
					while (++i < n) {
						try (IBuffer buf = (IBuffer) outMsgs.take(i)) {
							buf.drainTo(data);
						}
					}
					return data;
				} catch (ClassCastException e) {
					throw new RuntimeException(StrUtil.join(filters[0], "has to produce departure data of type ",
							IBuffer.class.getName()));
				}
			} finally {
				inMsgs.close();
				outMsgs.close();
			}
		}
	}

	static final class IdleTimeoutListener implements ITimerListener {

		static final ITimerListener INST = new IdleTimeoutListener();

		@Override
		public void onTimeout(Channel channel) {
			try {
				channel.channelService().onChannelIdleTimedOut(channel);
			} catch (Throwable t) {
				channel.onException(t);
			}
		}
	}

	static final class ConnectTimeoutListener implements ITimerListener {

		static final ITimerListener INST = new ConnectTimeoutListener();

		@Override
		public void onTimeout(Channel channel) {
			try {
				channel.channelService().onChannelConnectTimedOut(channel);
			} catch (Throwable t) {
				channel.onException(t);
			}
		}
	}

	static final class ReadTimeoutListener implements ITimerListener {

		static final ITimerListener INST = new ReadTimeoutListener();

		@Override
		public void onTimeout(Channel channel) {
			try {
				channel.channelService().onChannelReadTimedOut(channel);
			} catch (Throwable t) {
				channel.onException(t);
			}
		}
	}

	protected Channel(IChannelService<Object, Object> channelService) {
		final Long id = channelService.generateId();
		m_id = id;
		m_channelService = channelService;
		m_closed = new AtomicBoolean(false);
		m_selector = channelService.getChannelAdmin().designateSelector(id.intValue());
	}

	@Override
	public final boolean scheduleIdleTimeout(int timeout) {
		final Timer timer = m_timer;
		if (timer == null)
			return false;

		timer.listener(IdleTimeoutListener.INST);
		return timer.schedule(timeout);
	}

	@Override
	public final boolean scheduleConnectTimeout(int timeout) {
		final Timer timer = m_timer;
		if (timer == null)
			return false;

		timer.listener(ConnectTimeoutListener.INST);
		return timer.schedule(timeout);
	}

	@Override
	public final boolean scheduleReadTimeout(int timeout) {
		final Timer timer = m_timer;
		if (timer == null)
			return false;

		timer.listener(ReadTimeoutListener.INST);
		return timer.schedule(timeout);
	}

	@Override
	public final boolean cancelTimeout() {
		final Timer timer = m_timer;
		return timer != null && timer.cancel();
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
			if (msg == null || isClosed())
				return;

			final IFilter<?, ?>[] filters = channelService().getFilterChain().filters();
			m_selector.write(new IoEvent(m_writeThread, msg, filters, filters.length));
		} catch (Throwable t) {
			onException(t);
		}
	}

	@Override
	public final void close() {
		final AtomicBoolean closed = m_closed;
		if (closed.get() || !closed.compareAndSet(false, true))
			return;

		m_selector.write(new IoEvent(m_writeThread, WriteThread.EOF));
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
	public final void registerAccept() throws Throwable {
		m_storage = new IdentityHashMap<>();

		onAccepted();

		createWriteThread();

		m_timer = m_selector.createTimer(this);

		selectableChannel().configureBlocking(false);

		m_channelService.onChannelOpened(this);
		m_selectionKey = selectableChannel().register(m_selector.selector(), SelectionKey.OP_READ, this);
	}

	@Override
	public final void registerConnect() throws Throwable {
		final int timeout = m_connectTimeout;
		if (timeout < 0) {
			onConnectInternal(true);
		} else {
			if (timeout > 0)
				scheduleConnectTimeout(timeout);
			m_selectionKey = selectableChannel().register(m_selector.selector(), SelectionKey.OP_CONNECT, this);
		}
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
			m_timer = m_selector.createTimer(this);
			m_connectTimeout = connect() ? -1 : timeout;
			m_selector.connect(this);
		} catch (Throwable t) {
			onException(t);
		}
	}

	@Override
	public final void onConnect() {
		// if false, it timed out
		if (!cancelTimeout())
			return;

		onConnectInternal(false);
	}

	@Override
	public final void onRead() {
		final IChannelService<Object, Object> cs = channelService();
		final IBuffer in = cs.getBufferFactory().create();
		final ReadableByteChannel rbc = readableByteChannel();
		final long throttle = cs.throttle();
		long length = 0L;
		int n;
		try {
			for (;;) {
				n = in.readIn(rbc);
				if (n > 0) {
					length += n;
					if (length > throttle)
						break;
				} else if (in.isEmpty()) {
					in.close();
					close();
					return;
				} else
					break;
			}
		} catch (Throwable t) {
			in.close();
			if (!isClosed())
				onException(t);
			return;
		}

		try {
			if (n < 0) {
				close();
				onReadIn(in);
			} else if (onReadIn(in))
				interestOps(SelectionKey.OP_READ);
			else
				close();
		} catch (Throwable t) {
			onException(t);
		}
	}

	@Override
	public final void onWrite() {
		m_writeThread.run(null, null, 0);
	}

	@Override
	public final void onException(Throwable cause) {
		try {
			m_channelService.onChannelException(this, cause);
		} catch (Throwable t) {
			c_logger.error(StrUtil.join("Unexpected Error: ", this), t);
		}
	}

	@Override
	public final void onAccept() {
		m_selector.accept(this);
	}

	@Override
	public final void receive(IBuffer in) {
		try {
			if (onReadIn(in)) {
				selectableChannel().register(m_selector.selector(), SelectionKey.OP_READ);
				return;
			}
			close();
		} catch (Throwable t) {
			onException(t);
		}
	}

	protected abstract SelectableChannel selectableChannel();

	protected abstract void onAccepted() throws Exception;

	protected abstract void onClose() throws Exception;

	protected abstract boolean connect() throws Exception;

	protected abstract void onConnected() throws Exception;

	protected abstract ReadableByteChannel readableByteChannel();

	protected abstract WritableByteChannel writableByteChannel();

	// If returns false, this channel need be closed
	final boolean onReadIn(Object in) {
		MsgArrayList inMsgs = MsgArrayList.get();
		MsgArrayList outMsgs = MsgArrayList.get();
		final IChannelService<Object, Object> cs = m_channelService;
		final IFilter<?, ?>[] filters = cs.getFilterChain().filters();
		try {
			for (int k = 0, m = filters.length; k < m; ++k) {
				if (in instanceof IBuffer) {
					if (!onAccumulate(k, filters, inMsgs, outMsgs, (IBuffer) in))
						return false;
				} else {
					int i = 0;
					final int n = inMsgs.size();
					for (;;) {
						if (!onMsgArrive(k, filters, outMsgs, in))
							return false;
						if (++i >= n)
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

	private void onCloseInternal() {
		try {
			onClose();
		} catch (Throwable t) {
			onException(t);
		}

		final Timer timer = m_timer;
		if (timer != null)
			timer.cancel();

		try {
			m_channelService.onChannelClosed(this);
		} catch (Throwable t) {
			c_logger.error("Unexpected Error", t);
		}
	}

	@SuppressWarnings("resource")
	private boolean onAccumulate(int k, IFilter<?, ?>[] filters, MsgArrayList inMsgs, MsgArrayList outMsgs,
			IBuffer in) {
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
		final int n = inMsgs.size();
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

			createWriteThread();

			m_channelService.onChannelOpened(this);

			if (requireRegister)
				selectableChannel().register(m_selector.selector(), SelectionKey.OP_READ, this);
			else
				interestOps(SelectionKey.OP_READ);
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
		final WriteThread writeThread = m_writeThread;
		for (int i = size; i < n; ++i)
			writeThread.writeBackward(outMsgs.take(i), filters, index);

		outMsgs.size(size);
		return true;
	}

	private void createWriteThread() {
		m_writeThread = new WriteThread(this);
	}
}
