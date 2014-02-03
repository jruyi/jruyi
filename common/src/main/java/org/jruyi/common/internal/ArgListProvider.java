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
package org.jruyi.common.internal;

import org.jruyi.common.IArgList;
import org.jruyi.common.IThreadLocalCache;
import org.jruyi.common.StrUtil;
import org.jruyi.common.StringBuilder;
import org.jruyi.common.ThreadLocalCache;
import org.jruyi.common.ArgList.IFactory;
import org.jruyi.common.internal.ArgListProvider;

public final class ArgListProvider implements IFactory {

	private static final ArgListProvider c_inst = new ArgListProvider();

	static final class ArgList implements IArgList {

		private static final IThreadLocalCache<ArgList> c_cache = ThreadLocalCache
				.weakLinkedCache();
		private Object[] m_args;
		private int m_size;

		private ArgList() {
			this(8);
		}

		private ArgList(int capacity) {
			m_args = new Object[capacity];
		}

		public static ArgList get() {
			ArgList argList = c_cache.take();
			if (argList == null)
				argList = new ArgList();

			return argList;
		}

		@Override
		public Object arg(int index) {
			if (index >= m_size)
				throw new IndexOutOfBoundsException();

			return m_args[index];
		}

		@Override
		public int size() {
			return m_size;
		}

		@Override
		public void dump(StringBuilder builder) {
			Object[] args = m_args;
			int n = m_size;
			for (int i = 0; i < n; ++i)
				builder.append(StrUtil.getLineSeparator()).append("arg")
						.append(i).append(": ").append(args[i]);
		}

		@Override
		public String toString() {
			StringBuilder builder = StringBuilder.get();
			try {
				dump(builder);
				return builder.toString();
			} finally {
				builder.close();
			}
		}

		@Override
		public void close() {
			Object[] args = m_args;
			int n = m_size;
			for (int i = 0; i < n; ++i)
				args[i] = null;

			m_size = 0;

			c_cache.put(this);
		}

		void add(Object arg) {
			int size = m_size;
			int minCapacity = size + 1;
			if (minCapacity > m_args.length)
				expandCapacity(minCapacity);

			m_args[size] = arg;
			m_size = minCapacity;
		}

		void add(Object arg0, Object arg1) {
			int size = m_size;
			int minCapacity = size + 2;
			if (minCapacity > m_args.length)
				expandCapacity(minCapacity);

			Object[] args = m_args;
			args[size] = arg0;
			args[++size] = arg1;

			m_size = minCapacity;
		}

		void add(Object arg0, Object arg1, Object arg2) {
			int size = m_size;
			int minCapacity = size + 3;
			if (minCapacity > m_args.length)
				expandCapacity(minCapacity);

			Object[] args = m_args;
			args[size] = arg0;
			args[++size] = arg1;
			args[++size] = arg2;

			m_size = minCapacity;
		}

		void add(Object arg0, Object arg1, Object arg2, Object arg3) {
			int size = m_size;
			int minCapacity = size + 4;
			if (minCapacity > m_args.length)
				expandCapacity(minCapacity);

			Object[] args = m_args;
			args[size] = arg0;
			args[++size] = arg1;
			args[++size] = arg2;
			args[++size] = arg3;

			m_size = minCapacity;
		}

		void add(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4) {
			int size = m_size;
			int minCapacity = size + 5;
			if (minCapacity > m_args.length)
				expandCapacity(minCapacity);

			Object[] args = m_args;
			args[size] = arg0;
			args[++size] = arg1;
			args[++size] = arg2;
			args[++size] = arg3;
			args[++size] = arg4;

			m_size = minCapacity;
		}

		void add(Object arg0, Object arg1, Object arg2, Object arg3,
				Object arg4, Object arg5) {
			int size = m_size;
			int minCapacity = size + 6;
			if (minCapacity > m_args.length)
				expandCapacity(minCapacity);

			Object[] args = m_args;
			args[size] = arg0;
			args[++size] = arg1;
			args[++size] = arg2;
			args[++size] = arg3;
			args[++size] = arg4;
			args[++size] = arg5;

			m_size = minCapacity;
		}

		void add(Object arg0, Object arg1, Object arg2, Object arg3,
				Object arg4, Object arg5, Object arg6) {
			int size = m_size;
			int minCapacity = size + 7;
			if (minCapacity > m_args.length)
				expandCapacity(minCapacity);

			Object[] args = m_args;
			args[size] = arg0;
			args[++size] = arg1;
			args[++size] = arg2;
			args[++size] = arg3;
			args[++size] = arg4;
			args[++size] = arg5;
			args[++size] = arg6;

			m_size = minCapacity;
		}

		void add(Object arg0, Object arg1, Object arg2, Object arg3,
				Object arg4, Object arg5, Object arg6, Object arg7) {
			int size = m_size;
			int minCapacity = size + 8;
			if (minCapacity > m_args.length)
				expandCapacity(minCapacity);

			Object[] args = m_args;
			args[size] = arg0;
			args[++size] = arg1;
			args[++size] = arg2;
			args[++size] = arg3;
			args[++size] = arg4;
			args[++size] = arg5;
			args[++size] = arg6;
			args[++size] = arg7;

			m_size = minCapacity;
		}

		void add(Object arg0, Object[] argArray) {
			int size = m_size;
			int minCapacity = size + argArray.length;
			if (minCapacity > m_args.length)
				expandCapacity(minCapacity);

			Object[] args = m_args;
			args[size] = arg0;
			System.arraycopy(argArray, 0, args, size + 1, argArray.length);
			m_size = minCapacity;
		}

		private void expandCapacity(int minCapacity) {
			Object[] args = m_args;
			if (minCapacity <= args.length)
				return;

			int newCapacity = (args.length * 3) / 2 + 1;
			if (newCapacity < 0)
				newCapacity = Integer.MAX_VALUE;
			else if (minCapacity > newCapacity)
				newCapacity = minCapacity;

			Object[] newArray = new Object[newCapacity];
			System.arraycopy(args, 0, newArray, 0, m_size);
			m_args = newArray;
		}
	}

	private ArgListProvider() {
	}

	public static ArgListProvider getInstance() {
		return c_inst;
	}

	public IFactory getFactory() {
		return this;
	}

	@Override
	public IArgList create(Object arg) {
		ArgList argList = ArgList.get();
		argList.add(arg);
		return argList;
	}

	@Override
	public IArgList create(Object arg0, Object arg1) {
		ArgList argList = ArgList.get();
		argList.add(arg0, arg1);
		return argList;
	}

	@Override
	public IArgList create(Object arg0, Object arg1, Object arg2) {
		ArgList argList = ArgList.get();
		argList.add(arg0, arg1, arg2);
		return argList;
	}

	@Override
	public IArgList create(Object arg0, Object arg1, Object arg2, Object arg3) {
		ArgList argList = ArgList.get();
		argList.add(arg0, arg1, arg2, arg3);
		return argList;
	}

	@Override
	public IArgList create(Object arg0, Object arg1, Object arg2, Object arg3,
			Object arg4) {
		ArgList argList = ArgList.get();
		argList.add(arg0, arg1, arg2, arg3, arg4);
		return argList;
	}

	@Override
	public IArgList create(Object arg0, Object arg1, Object arg2, Object arg3,
			Object arg4, Object arg5) {
		ArgList argList = ArgList.get();
		argList.add(arg0, arg1, arg2, arg3, arg4, arg5);
		return argList;
	}

	@Override
	public IArgList create(Object arg0, Object arg1, Object arg2, Object arg3,
			Object arg4, Object arg5, Object arg6) {
		ArgList argList = ArgList.get();
		argList.add(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
		return argList;
	}

	@Override
	public IArgList create(Object arg0, Object arg1, Object arg2, Object arg3,
			Object arg4, Object arg5, Object arg6, Object arg7) {
		ArgList argList = ArgList.get();
		argList.add(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
		return argList;
	}

	@Override
	public IArgList create(Object arg0, Object... args) {
		ArgList argList = ArgList.get();
		argList.add(arg0, args);
		return argList;
	}
}
