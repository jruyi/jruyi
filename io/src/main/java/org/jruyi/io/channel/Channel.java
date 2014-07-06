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

import java.io.Closeable;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.IdentityHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.jruyi.common.ArgList;
import org.jruyi.common.IArgList;
import org.jruyi.common.ICloseable;
import org.jruyi.common.IDumpable;
import org.jruyi.common.IThreadLocalCache;
import org.jruyi.common.ListNode;
import org.jruyi.common.StrUtil;
import org.jruyi.common.StringBuilder;
import org.jruyi.common.ThreadLocalCache;
import org.jruyi.io.IBuffer;
import org.jruyi.io.IFilter;
import org.jruyi.io.IFilterOutput;
import org.jruyi.timeoutadmin.ITimeoutEvent;
import org.jruyi.timeoutadmin.ITimeoutListener;
import org.jruyi.timeoutadmin.ITimeoutNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Channel implements IChannel, IDumpable, Runnable {

	private static final Logger c_logger = LoggerFactory
			.getLogger(Channel.class);
	private static final AtomicLong c_sequence = new AtomicLong(0L);
	private Long m_id;
	private final IChannelService m_channelService;
	private final ReentrantLock m_lock;
	private ConcurrentHashMap<String, Object> m_attributes;
	private IdentityHashMap<Object, Object> m_storage;
	private Object m_attachment;
	private volatile boolean m_closed;
	private ISelector m_selector;
	private SelectionKey m_selectionKey;
	private ITimeoutNotifier m_timeoutNotifier;
	private ReadThread m_readThread;
	private WriteThread m_writeThread;

	static final class MsgArrayList implements ICloseable, IFilterOutput {

		private static final IThreadLocalCache<MsgArrayList> c_cache = ThreadLocalCache
				.weakArrayCache();
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

		Object[] msgs() {
			return m_msgs;
		}

		Object take(int index) {
			Object msg = m_msgs[index];
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

			int minCapacity = ++m_size;
			int oldCapacity = m_msgs.length;
			if (minCapacity > oldCapacity) {
				int newCapacity = (oldCapacity * 3) / 2 + 1;
				Object[] oldMsgs = m_msgs;
				m_msgs = new Object[newCapacity];
				System.arraycopy(oldMsgs, 0, m_msgs, 0, oldCapacity);
			}
			m_msgs[minCapacity - 1] = msg;
		}

		void release() {
			Object[] msgs = m_msgs;
			int size = m_size;
			for (int i = 0; i < size; ++i) {
				Object msg = msgs[i];
				if (msg != null) {
					if (msg instanceof Closeable) {
						try {
							((Closeable) msg).close();
						} catch (Throwable t) {
							c_logger.error(StrUtil.join(
									"Failed to close message: ", msg), t);
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

		private static final IThreadLocalCache<FilterContext> c_cache = ThreadLocalCache
				.weakLinkedCache();
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
			final IChannelService cs = channel.channelService();
			final IBuffer in = cs.getBufferFactory().create();
			final ScatteringByteChannel sbc = channel.scatteringByteChannel();
			int n;
			try {
				final int throttle = cs.throttle();
				for (;;) {
					n = in.readIn(sbc);
					if (n > 0) {
						if (in.length() > throttle)
							break;
					} else if (in.isEmpty()) {
						in.close();
						channel.close();
						return;
					} else
						break;
				}
			} catch (Throwable t) {
				in.close();
				if (!channel.isClosed())
					channel.onException(t);
				return;
			}

			try {
				if (n < 0) {
					channel.close();
					channel.onReadIn(in);
				} else if (channel.onReadIn(in))
					channel.onReadRequired();
				else
					channel.close();
			} catch (Throwable t) {
				channel.onException(t);
			}
		}
	}

	static final class WriteThread implements Runnable {

		private Channel m_channel;
		// indicates whether writing is in process
		private IArgList m_out;
		private IBuffer m_data;
		private ListNode<IArgList> m_head;
		private ListNode<IArgList> m_tail;
		private final ReentrantLock m_lock;

		WriteThread(Channel channel) {
			m_channel = channel;
			m_head = m_tail = ListNode.create();
			m_lock = new ReentrantLock();
		}

		void channel(Channel channel) {
			m_channel = channel;
		}

		@Override
		public void run() {
			final Channel channel = m_channel;
			IBuffer data = m_data;
			try {
				final IChannelService cs = channel.channelService();
				final GatheringByteChannel gbc = channel.gatheringByteChannel();
				IArgList arg = peek();
				do {
					if (data == null) {
						data = filter(arg, channel);
						if (data == null)
							continue;
					}

					while (data.remaining() > 0) {
						if (data.writeOut(gbc) == 0) {
							channel.onWriteRequired();
							m_data = data;
							return;
						}
					}

					m_data = null;

					cs.onMessageSent(channel, arg.arg(0));
					data.close();
					data = null;
					arg.close();
				} while ((arg = poll()) != null);
			} catch (Throwable t) {
				if (data != null)
					data.close();

				if (!channel.isClosed())
					channel.onException(t);
			}
		}

		void write(Object msg, IFilter<?, ?>[] filters, int filterCount) {
			final IArgList out = ArgList.create(msg, filters, filterCount);
			if (put(out))
				return;

			final Channel channel = m_channel;
			channel.channelService().getChannelAdmin().onWrite(channel);
		}

		void clear() {
			final ReentrantLock lock = m_lock;
			lock.lock();
			try {
				ListNode<IArgList> head = m_head;
				ListNode<IArgList> tail = m_tail;
				if (head != tail) {
					do {
						ListNode<IArgList> node = head.next();
						head.close();
						final IArgList arg = node.get();
						final Object msg = arg.arg(0);
						arg.close();
						node.set(null);
						if (msg instanceof Closeable) {
							try {
								((Closeable) msg).close();
							} catch (Throwable t) {
								c_logger.error(StrUtil.join(
										"Failed to close message: ", msg), t);
							}
						}
						head = node;
					} while (head != tail);
					m_head = head;
				}
			} finally {
				lock.unlock();
			}
		}

		private boolean put(IArgList out) {
			final ReentrantLock lock = m_lock;
			lock.lock();
			try {
				if (m_out == null)
					m_out = out;
				else {
					ListNode<IArgList> node = ListNode.create();
					node.set(out);
					m_tail.next(node);
					m_tail = node;
					return true;
				}
			} finally {
				lock.unlock();
			}

			return false;
		}

		private IArgList peek() {
			return m_out;
		}

		private IArgList poll() {
			final ReentrantLock lock = m_lock;
			lock.lock();
			try {
				if (m_head == m_tail) {
					m_out = null;
					return null;
				}
			} finally {
				lock.unlock();
			}

			ListNode<IArgList> head = m_head;
			ListNode<IArgList> node = head.next();
			IArgList out = node.get();
			node.set(null);
			m_head = node;
			head.close();
			m_out = out;

			return out;
		}

		private IBuffer filter(IArgList arg, Channel channel) {
			IBuffer data = null;
			Object msg = arg.arg(0);
			int index = (Integer) arg.arg(2);
			if (index > 0) {
				@SuppressWarnings("unchecked")
				IFilter<Object, Object>[] filters = (IFilter<Object, Object>[]) arg
						.arg(1);
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
							if (!filters[--index].onMsgDepart(channel,
									inMsgs.take(i), outMsgs))
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
								IBuffer buf = (IBuffer) outMsgs.take(i);
								buf.drainTo(data);
								buf.close();
							}
							outMsgs.size(0);
						} catch (ClassCastException e) {
							throw new RuntimeException(StrUtil.join(filters[0],
									"has to produce departure data of type ",
									IBuffer.class.getName()));
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
					throw new RuntimeException(StrUtil.join(
							"Departure data has to be of type",
							IBuffer.class.getName()));
				}
			}

			return data;
		}
	}

	static final class IdleTimeoutListener implements ITimeoutListener {

		static final ITimeoutListener INST = new IdleTimeoutListener();

		@Override
		public void onTimeout(ITimeoutEvent event) {
			Channel channel = (Channel) event.getSubject();
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
			Channel channel = (Channel) event.getSubject();
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
			Channel channel = (Channel) event.getSubject();
			try {
				channel.channelService().onChannelReadTimedOut(channel);
			} catch (Throwable t) {
				channel.onException(t);
			}
		}
	}

	protected Channel(IChannelService channelService) {
		m_channelService = channelService;
		m_lock = new ReentrantLock();
	}

	/**
	 * Runs on accept event
	 */
	@Override
	public void run() {
		IChannelService channelService = m_channelService;
		try {
			generateId();

			m_attributes = new ConcurrentHashMap<String, Object>();
			m_storage = new IdentityHashMap<Object, Object>();

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
	public final void onWriteRequired() {
		m_selector.onWriteRequired(this);
	}

	@Override
	public final boolean scheduleIdleTimeout(int timeout) {
		ITimeoutNotifier timeoutNotifier = m_timeoutNotifier;
		if (timeoutNotifier == null)
			return false;

		timeoutNotifier.setListener(IdleTimeoutListener.INST);
		return timeoutNotifier.schedule(timeout);
	}

	@Override
	public final boolean scheduleConnectTimeout(int timeout) {
		ITimeoutNotifier timeoutNotifier = m_timeoutNotifier;
		if (timeoutNotifier == null)
			return false;

		timeoutNotifier.setListener(ConnectTimeoutListener.INST);
		return timeoutNotifier.schedule(timeout);
	}

	@Override
	public final boolean scheduleReadTimeout(int timeout) {
		ITimeoutNotifier timeoutNotifier = m_timeoutNotifier;
		if (timeoutNotifier == null)
			return false;

		timeoutNotifier.setListener(ReadTimeoutListener.INST);
		return timeoutNotifier.schedule(timeout);
	}

	@Override
	public final boolean cancelTimeout() {
		ITimeoutNotifier timeoutNotifier = m_timeoutNotifier;
		return timeoutNotifier != null && timeoutNotifier.cancel();
	}

	@Override
	public Object deposit(Object id, Object something) {
		return m_storage.put(id, something);
	}

	@Override
	public Object withdraw(Object id) {
		return m_storage.remove(id);
	}

	@Override
	public Object inquiry(Object id) {
		return m_storage.get(id);
	}

	@Override
	public IBuffer createBuffer() {
		return m_channelService.getBufferFactory().create();
	}

	@Override
	public final void write(Object msg) {
		try {
			if (msg == null)
				return;

			IFilter<?, ?>[] filters = channelService().getFilterChain();
			m_writeThread.write(msg, filters, filters.length);
		} catch (Throwable t) {
			onException(t);
		}
	}

	@Override
	public final void close() {
		if (m_closed)
			return;

		final ReentrantLock lock = m_lock;
		if (!lock.tryLock())
			return;
		try {
			if (m_closed)
				return;
			m_closed = true;
		} finally {
			lock.unlock();
		}

		try {
			onClose();
		} catch (Throwable t) {
			onException(t);
		}

		final ITimeoutNotifier tn = m_timeoutNotifier;
		if (tn != null)
			tn.close();

		final WriteThread wt = m_writeThread;
		if (wt != null)
			wt.clear();

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
		StringBuilder builder = StringBuilder.get();
		try {
			dump(builder);
			return builder.toString();
		} finally {
			builder.close();
		}
	}

	@Override
	public final Long id() {
		return m_id;
	}

	@Override
	public final Object get(String name) {
		return m_attributes.get(name);
	}

	@Override
	public final Object put(String name, Object value) {
		return m_attributes.put(name, value);
	}

	@Override
	public final Object remove(String name) {
		return m_attributes.remove(name);
	}

	@Override
	public boolean isClosed() {
		return m_closed;
	}

	@Override
	public final Object attach(Object attachment) {
		Object oldAttachment = m_attachment;
		m_attachment = attachment;
		return oldAttachment;
	}

	@Override
	public final Object attachment() {
		return m_attachment;
	}

	@Override
	public final Object detach() {
		Object oldAttachment = m_attachment;
		m_attachment = null;
		return oldAttachment;
	}

	@Override
	public final IChannelService channelService() {
		return m_channelService;
	}

	@Override
	public final void register(ISelector selector, int ops) {
		try {
			m_selectionKey = selectableChannel().register(selector.selector(),
					ops, this);
			m_selector = selector;
		} catch (Throwable t) {
			// Ignore
		}
	}

	@Override
	public final void interestOps(int ops) {
		SelectionKey selectionKey = m_selectionKey;
		try {
			selectionKey.interestOps(selectionKey.interestOps() | ops);
		} catch (Throwable t) {
			// Ignore
		}
	}

	@Override
	public final void connect(int timeout) {
		try {
			generateId();

			m_attributes = new ConcurrentHashMap<String, Object>();
			m_storage = new IdentityHashMap<Object, Object>();

			IChannelAdmin ca = m_channelService.getChannelAdmin();
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
	public final Runnable onRead() {
		return m_readThread;
	}

	@Override
	public final Runnable onWrite() {
		return m_writeThread;
	}

	@Override
	public final void receive(IBuffer in) {
		try {
			if (onReadIn(in))
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

	public final Runnable onAccept() {
		return this;
	}

	protected abstract SelectableChannel selectableChannel();

	protected abstract void onAccepted() throws Exception;

	protected abstract void onClose() throws Exception;

	protected abstract boolean connect() throws Exception;

	protected abstract void onConnected() throws Exception;

	protected abstract ScatteringByteChannel scatteringByteChannel();

	protected abstract GatheringByteChannel gatheringByteChannel();

	protected ITimeoutNotifier createTimeoutNotifier(IChannelAdmin ca) {
		return ca.createTimeoutNotifier(this);
	}

	protected final void transferWriteThread(Channel channel) {
		WriteThread wt = channel.m_writeThread;
		channel.m_writeThread = null;
		wt.channel(this);
		m_writeThread = wt;
	}

	// If returns false, this channel need be closed
	final boolean onReadIn(Object in) {
		MsgArrayList inMsgs = MsgArrayList.get();
		MsgArrayList outMsgs = MsgArrayList.get();
		IChannelService cs = m_channelService;
		IFilter<?, ?>[] filters = cs.getFilterChain();
		try {
			for (int k = 0, m = filters.length; k < m; ++k) {
				if (in instanceof IBuffer) {
					if (!onAccumulate(k, filters, inMsgs, outMsgs, (IBuffer) in))
						return false;
				} else {
					int i = 0;
					int n = inMsgs.size();
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

				MsgArrayList temp = inMsgs;
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

	@SuppressWarnings("resource")
	private boolean onAccumulate(int k, IFilter<?, ?>[] filters,
			MsgArrayList inMsgs, MsgArrayList outMsgs, IBuffer in) {
		IFilter<?, ?> filter = filters[k];
		// mergeContext -start
		int msgLen = 0;
		FilterContext context = (FilterContext) withdraw(filter);
		if (context != null) {
			IBuffer prevData = context.data();
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

		int i = 1;
		int n = inMsgs.size();
		for (;;) {
			if (msgLen == 0) {
				msgLen = filter.tellBoundary(this, in);
				if (msgLen < 0) { // ERROR
					in.close();
					return false;
				}
			}

			int inLen = in.length();
			if (msgLen == 0 || inLen < msgLen) {
				if (i < n) {
					IBuffer data = (IBuffer) inMsgs.take(i);
					++i;
					data.drainTo(in);
					data.close();
					continue;
				}
			} else if (inLen > msgLen) {
				if (!onMsgArrive(k, filters, outMsgs, in.split(msgLen)))
					return false;
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

			IChannelService channelService = m_channelService;
			channelService.onChannelOpened(this);

			IChannelAdmin ca = channelService.getChannelAdmin();
			if (requireRegister)
				ca.onRegisterRequired(this);
			else
				onReadRequired();
		} catch (Throwable t) {
			onException(t);
		}
	}

	private boolean onMsgArrive(int index, IFilter<?, ?>[] filters,
			MsgArrayList outMsgs, Object msg) {
		int size = outMsgs.size();

		@SuppressWarnings("unchecked")
		boolean ok = ((IFilter<Object, ?>) filters[index]).onMsgArrive(this,
				msg, outMsgs);
		int n = outMsgs.size();
		if (n == size || ok)
			return ok;

		WriteThread writeThread = m_writeThread;
		for (int i = size; i < n; ++i)
			writeThread.write(outMsgs.take(i), filters, index + 1);

		outMsgs.size(size);
		return true;
	}

	private void createWriteThread() {
		if (m_writeThread == null)
			m_writeThread = new WriteThread(this);
	}

	private void createReadThread() {
		if (m_readThread == null)
			m_readThread = new ReadThread(this);
	}

	private void generateId() {
		m_id = c_sequence.incrementAndGet();
	}
}
