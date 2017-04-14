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

package org.jruyi.io.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.InvalidMarkException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;

import org.jruyi.common.*;
import org.jruyi.common.StringBuilder;
import org.jruyi.io.*;

public final class Buffer implements IBuffer {

	private Var m_var;

	private Buffer(Var var) {
		m_var = var;
	}

	static Buffer get(BufferFactory factory) {
		final Var var = Var.get();
		var.init(factory);
		return new Buffer(var);
	}

	/**
	 * {@code fromIndex} must be less than m_size and non-negative.
	 */
	private static int indexOf(byte b, IUnit unit, int fromIndex) {
		int start = unit.start();
		int end = start + unit.size();
		fromIndex += start;
		while (fromIndex < end) {
			if (unit.byteAt(fromIndex) == b)
				return fromIndex - start;
			++fromIndex;
		}
		return -1;
	}

	private static int indexOf(byte[] bytes, IUnit unit, int leftIndex) {
		int start = unit.start();
		int end = start + unit.size();
		int index = start + leftIndex;
		int length = bytes.length;

		next: for (; index < end; ++index) {
			leftIndex = index;
			int rightIndex = index + length;
			if (rightIndex > end)
				rightIndex = end;

			int i = 0;
			for (; leftIndex < rightIndex; ++leftIndex, ++i) {
				if (unit.byteAt(leftIndex) != bytes[i])
					continue next;
			}

			return index - start;
		}

		return -1;
	}

	/**
	 * {@code fromIndex} must be less than m_size and non-negative.
	 */
	private static int lastIndexOf(byte b, IUnit unit, int fromIndex) {
		int start = unit.start();
		fromIndex += start;
		while (fromIndex >= start) {
			if (unit.byteAt(fromIndex) == b)
				return fromIndex - start;

			--fromIndex;
		}

		return -1;
	}

	private static int lastIndexOf(byte[] bytes, IUnit unit, int rightIndex) {
		int start = unit.start();
		int end = start + rightIndex;
		int length = bytes.length;

		next: for (; end > start; --end) {
			rightIndex = end;
			int leftIndex = end - length;
			if (leftIndex < start)
				leftIndex = start;

			int i = length;
			while (rightIndex > leftIndex) {
				if (unit.byteAt(--rightIndex) != bytes[--i])
					continue next;
			}

			return end - start;
		}

		return -1;
	}

	private static boolean startsWith(byte[] bytes, int offset, IUnit unit) {
		int start = unit.start();
		int end = bytes.length - offset;
		int size = unit.size();
		if (end > size)
			end = size;

		end += start;
		for (; start < end; ++start, ++offset) {
			if (unit.byteAt(start) != bytes[offset])
				return false;
		}

		return true;
	}

	private static boolean endsWith(byte[] bytes, int offset, IUnit unit) {
		int start = unit.start();
		int size = unit.size();
		int end = start + size;
		if (offset < size)
			start = end - offset;

		while (end > start) {
			if (unit.byteAt(--end) != bytes[--offset])
				return false;
		}

		return true;
	}

	private static int compare(IUnit unit, IByteSequence sequence, int from, int len) {
		int i = unit.start() + unit.position();
		len += i;
		for (; i < len; ++i, ++from) {
			byte b1 = unit.byteAt(i);
			byte b2 = sequence.byteAt(from);
			if (b1 != b2)
				return b1 < b2 ? -1 : 1;
		}

		return 0;
	}

	private static int compare(IUnit unit1, int i, IUnit unit2, int j, int len) {
		i += unit1.start();
		j += unit2.start();
		for (; len > 0; ++i, ++j, --len) {
			byte b1 = unit1.byteAt(i);
			byte b2 = unit2.byteAt(j);
			if (b1 != b2)
				return (b1 < b2) ? -1 : 1;
		}

		return 0;
	}

	@Override
	public int position() {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int position = 0;
		for (int i = 0, n = var.positionIndex(); i <= n; ++i)
			position += units[i].position();
		return position;
	}

	@Override
	public int size() {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int size = 0;
		for (int i = 0, n = var.length(); i < n; ++i)
			size += units[i].size();
		return size;
	}

	@Override
	public int remaining() {
		return m_var.remaining();
	}

	@Override
	public void reset() {
		final Var var = m_var;
		int i = var.markIndex();
		if (i < 0)
			throw new InvalidMarkException();

		final IUnit[] units = var.units();
		final int n = var.positionIndex();
		var.positionIndex(i);
		units[i].reset();
		while (++i <= n)
			units[i].rewind();
	}

	@Override
	public void rewind() {
		final Var var = m_var;
		final IUnit[] units = var.units();
		final int n = var.positionIndex();
		var.markIndex(-1);
		var.positionIndex(0);
		for (int i = 0; i <= n; ++i)
			units[i].rewind();
	}

	@Override
	public int skip(int n) {
		if (n <= 0)
			return 0;

		final Var var = m_var;
		final IUnit[] units = var.units();
		int i = var.positionIndex();
		IUnit unit = units[i];
		int size = unit.size();
		int m = size - unit.position();

		for (int len = var.length(); m < n && ++i < len;) {
			unit.position(size);
			unit = units[i];
			size = unit.size();
			m += size;
		}
		if (m < n) {
			unit.position(size);
			var.positionIndex(i - 1);
			return m;
		} else {
			unit.position(unit.position() + size - (m - n));
			var.positionIndex(i);
			return n;
		}
	}

	@Override
	public void mark() {
		final Var var = m_var;
		final int i = var.positionIndex();
		var.markIndex(i);
		final IUnit unit = var.units()[i];
		unit.mark(unit.position());
	}

	@Override
	public boolean isEmpty() {
		final Var var = m_var;
		final IUnit[] units = var.units();
		for (int i = var.positionIndex(), n = var.length(); i < n; ++i) {
			if (units[i].remaining() > 0)
				return false;
		}
		return true;
	}

	@Override
	public int indexOf(byte b) {
		return indexOf(b, 0);
	}

	@Override
	public int indexOf(byte b, int fromIndex) {
		if (fromIndex < 0)
			fromIndex = 0;

		int index = fromIndex;

		final Var var = m_var;
		final IUnit[] units = var.units();
		final int len = var.length();
		int i = 0;
		IUnit unit = units[i];
		int n;
		while (fromIndex >= (n = unit.size())) {
			fromIndex -= n;
			if (++i == len)
				return -1;
			unit = units[i];
		}

		index -= fromIndex;
		n = indexOf(b, unit, fromIndex);
		for (;;) {
			if (n >= 0)
				return index + n;

			index += unit.size();

			if (++i == len)
				break;

			unit = units[i];
			n = indexOf(b, unit, 0);
		}

		return -1;
	}

	@Override
	public int indexOf(byte[] bytes) {
		return indexOf(bytes, 0);
	}

	@Override
	public int indexOf(byte[] bytes, int fromIndex) {
		if (fromIndex < 0)
			fromIndex = 0;

		final Var var = m_var;
		final IUnit[] units = var.units();
		final int count = var.length();
		int i = 0;

		final int length = bytes.length;
		int index = fromIndex;
		IUnit unit = units[i];
		int unitSize;
		int size = 0;
		while (fromIndex >= (unitSize = unit.size())) {
			fromIndex -= unitSize;
			size += unitSize;
			if (++i == count)
				return length < 1 ? size - length : -1;
			unit = units[i];
		}

		if (length < 1)
			return index;

		// index - base index
		// fromIndex - leftIndex
		index -= fromIndex;
		int n = indexOf(bytes, unit, fromIndex);
		for (;;) {
			if (n >= 0) {
				int m = unitSize - n;
				n += index;
				if (m >= length)
					return n;

				int temp = i;
				while (++temp != count && startsWith(bytes, m, unit = units[temp])) {
					if ((m += unit.size()) >= length)
						return n;
				}
			}

			index += unitSize;
			if (++i == count)
				break;
			unit = units[i];
			unitSize = unit.size();
			n = indexOf(bytes, unit, 0);
		}

		return -1;
	}

	@Override
	public int indexOf(ByteKmp pattern) {
		return indexOf(pattern, 0);
	}

	@Override
	public int indexOf(ByteKmp pattern, int fromIndex) {
		if (fromIndex < 0)
			fromIndex = 0;

		final Var var = m_var;
		final IUnit[] units = var.units();
		final int count = var.length();
		int i = 0;

		final int length = pattern.length();
		IUnit unit = units[i];
		int index = fromIndex;
		int size = 0;
		int unitSize;
		while (index >= (unitSize = unit.size())) {
			index -= unitSize;
			size += unitSize;
			if (++i == count)
				return length < 1 ? size - length : -1;
			unit = units[i];
		}

		if (length < 1)
			return fromIndex;

		final int n;
		try (Blob blob = Blob.get()) {
			blob.add(unit, unit.start() + index, unitSize - index);
			while (++i < count) {
				unit = units[i];
				blob.add(unit, unit.start(), unit.size());
			}

			n = blob.indexOf(pattern);
		}

		return n < 0 ? n : fromIndex + n;
	}

	@Override
	public int lastIndexOf(byte b) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int i = var.length();

		int index = size();
		int fromIndex = index - 1;
		IUnit unit = units[--i];
		while (fromIndex < (index -= unit.size()))
			unit = units[--i];

		fromIndex -= index;
		int n = lastIndexOf(b, unit, fromIndex);
		for (;;) {
			if (n >= 0)
				return index + n;

			if (index <= 0)
				break;

			unit = units[--i];
			n = unit.size();
			index -= n;
			n = lastIndexOf(b, unit, n - 1);
		}

		return -1;
	}

	@Override
	public int lastIndexOf(byte b, int fromIndex) {
		if (fromIndex < 0)
			return -1;

		final Var var = m_var;
		final IUnit[] units = var.units();
		int i = var.length();

		int index = size();
		if (fromIndex >= index)
			fromIndex = index - 1;

		IUnit unit = units[--i];
		while (fromIndex < (index -= unit.size()))
			unit = units[--i];

		fromIndex -= index;
		int n = lastIndexOf(b, unit, fromIndex);
		for (;;) {
			if (n >= 0)
				return index + n;

			if (index <= 0)
				break;

			unit = units[--i];
			n = unit.size();
			index -= n;
			n = lastIndexOf(b, unit, n - 1);
		}

		return -1;
	}

	@Override
	public int lastIndexOf(byte[] bytes) {
		int length = bytes.length;
		int index = size();
		int fromIndex = index - length;

		if (fromIndex < 0)
			return -1;

		if (length < 1)
			return fromIndex;

		fromIndex += length;

		final Var var = m_var;
		final IUnit[] units = var.units();
		int i = var.length();

		IUnit unit = units[--i];
		while (fromIndex < (index -= unit.size()))
			unit = units[--i];

		// index - base index
		// fromIndex - right index
		fromIndex -= index;
		int n = lastIndexOf(bytes, fromIndex);
		for (;;) {
			if (n > 0) {
				int m = length - n;
				n = index - m;
				if (m <= 0)
					return n;

				if (n >= 0) {
					int temp = i;
					while (endsWith(bytes, m, unit = units[--temp])) {
						if ((m -= unit.size()) <= 0)
							return n;
					}
				}
			}

			if (index <= 0)
				break;

			unit = units[--i];
			int unitSize = unit.size();
			index -= unitSize;
			n = lastIndexOf(bytes, unit, unitSize);
		}

		return -1;
	}

	@Override
	public int lastIndexOf(byte[] bytes, int fromIndex) {
		int length = bytes.length;
		int index = size();
		int maxIndex = index - length;
		if (fromIndex > maxIndex)
			fromIndex = maxIndex;

		if (fromIndex < 0)
			return -1;

		if (length < 1)
			return fromIndex;

		fromIndex += length;

		final Var var = m_var;
		final IUnit[] units = var.units();
		int i = var.length();

		IUnit unit = units[--i];
		while (fromIndex < (index -= unit.size()))
			unit = units[--i];

		// index - base index
		// fromIndex - right index
		fromIndex -= index;
		int n = lastIndexOf(bytes, unit, fromIndex);
		for (;;) {
			if (n > 0) {
				int m = length - n;
				n = index - m;
				if (m <= 0)
					return n;

				if (n >= 0) {
					int temp = i;
					while (endsWith(bytes, m, unit = units[--temp])) {
						if ((m -= unit.size()) <= 0)
							return n;
					}
				}
			}

			if (index <= 0)
				break;

			unit = units[--i];
			int unitSize = unit.size();
			index -= unitSize;
			n = lastIndexOf(bytes, unit, unitSize);
		}

		return -1;
	}

	@Override
	public int lastIndexOf(ByteKmp pattern) {
		int length = pattern.length();
		int index = size();
		int fromIndex = index - length;

		if (fromIndex < 0)
			return -1;

		if (length < 1)
			return fromIndex;

		fromIndex += length;

		final Var var = m_var;
		final IUnit[] units = var.units();
		int last = var.length();

		IUnit unit = units[--last];
		while (fromIndex < (index -= unit.size()))
			unit = units[--last];

		fromIndex -= index;
		try (Blob blob = Blob.get()) {
			for (int i = 0; i < last; ++i) {
				final IUnit temp = units[i];
				blob.add(temp, temp.start(), temp.size());
			}

			blob.add(unit, unit.start(), fromIndex);
			return blob.lastIndexOf(pattern);
		}
	}

	@Override
	public int lastIndexOf(ByteKmp pattern, int fromIndex) {
		int length = pattern.length();
		int index = size();
		int n = index - length;
		if (fromIndex > n)
			fromIndex = n;

		if (fromIndex < 0)
			return -1;

		if (length < 1)
			return fromIndex;

		fromIndex += length;

		final Var var = m_var;
		final IUnit[] units = var.units();
		int last = var.length();
		IUnit unit = units[--last];
		while (fromIndex < (index -= unit.size()))
			unit = units[--last];

		fromIndex -= index;
		try (Blob blob = Blob.get()) {
			for (int i = 0; i < last; ++i) {
				final IUnit temp = units[i];
				blob.add(temp, temp.start(), temp.size());
			}

			blob.add(unit, unit.start(), fromIndex);
			return blob.lastIndexOf(pattern);
		}
	}

	@Override
	public boolean startsWith(byte[] bytes) {
		int length = bytes.length;
		if (length < 1)
			return true;

		final Var var = m_var;
		final IUnit[] units = var.units();
		final int len = var.length();
		int i = 0;

		IUnit unit;
		int n = 0;
		while (startsWith(bytes, n, unit = units[i])) {
			if ((n += unit.size()) >= length)
				return true;
			if (++i == len)
				return false;
		}

		return false;
	}

	@Override
	public boolean endsWith(byte[] bytes) {
		int n = bytes.length;
		if (n < 1)
			return true;

		final Var var = m_var;
		final IUnit[] units = var.units();
		int last = var.length();

		IUnit unit;
		while (endsWith(bytes, n, unit = units[--last])) {
			if ((n -= unit.size()) <= 0)
				return true;
			if (last == 0)
				return false;
		}

		return false;
	}

	@Override
	public IBuffer compact() {
		final Var var = m_var;
		final IUnit[] units = var.units();
		IUnit unit = units[0];
		if (!unit.isEmpty()) {
			unit.compact();
			return this;
		}

		final int len = var.length();
		int i = 1;
		if (i < len) {
			final BufferFactory factory = var.factory();
			for (;;) {
				factory.putUnit(unit);
				unit = units[i];
				if (++i == len) {
					if (unit.isEmpty())
						unit.clear();
					else
						unit.compact();
					break;
				}

				if (!unit.isEmpty()) {
					unit.compact();
					break;
				}
			}
			--i;
			int n = len - i;
			System.arraycopy(units, i, units, 0, n);
			var.length(n);
			while (n < len)
				units[n++] = null;
			var.markIndex(0);
			var.positionIndex(0);
		} else
			unit.clear();

		return this;
	}

	@Override
	public Buffer newBuffer() {
		return Buffer.get(m_var.factory());
	}

	@Override
	public IBuffer split(int size) {
		if (size == 0)
			return newBuffer();

		final Var var = m_var;
		var.markIndex(-1);

		final int len = var.length();
		final IUnit[] units = var.units();
		int i = 0;
		IUnit unit = units[i];
		int n;
		while (size > (n = unit.size())) {
			size -= n;
			if (++i == len)
				throw new IllegalArgumentException();
			unit = units[i];
		}

		int pos = var.positionIndex() - i;
		if (pos > 0)
			var.positionIndex(i);

		final Var var2;
		if (n > size) {
			var2 = Var.get(len - i);
			final IUnit[] units2 = var2.units();
			final IUnit slice = unit.slice(size, n);
			var2.init(var.factory(), slice);
			if (pos > 0) {
				var2.positionIndex(pos);
				slice.position(slice.size());
			} else if (pos == 0) {
				pos = unit.position();
				if (pos > size) {
					unit.position(size);
					slice.position(pos - size);
				}
			}
			unit.size(size);
			var.length(++i);
			n = len - i;
			System.arraycopy(units, i, units2, 1, n);
			var2.length(n + 1);
			Arrays.fill(units, i, len, null);
		} else {
			var.length(++i);
			n = len - i;
			if (n > 0) {
				var2 = Var.get(n);
				var2.init(var.factory(), units, i, n);
				Arrays.fill(units, i, len, null);
			} else {
				var2 = Var.get();
				var2.init(var.factory());
			}
			if (pos > 0)
				var2.positionIndex(pos - 1);
		}
		m_var = var2;
		return new Buffer(var);
	}

	@Override
	public void drain() {
		m_var.drain();
	}

	@Override
	public void drainTo(IBuffer dst) {
		if (!(dst instanceof Buffer)) {
			dst.write(this, Codec.byteSequence());
			drain();
			return;
		}

		final Var var = m_var;
		final IUnit[] units = var.units();
		final BufferFactory factory = var.factory();
		final int positionIndex = var.positionIndex();
		for (int i = 0; i < positionIndex; ++i) {
			factory.putUnit(units[i]);
			units[i] = null;
		}
		units[positionIndex].compact();
		int n = var.length() - positionIndex;

		final Var dstVar = ((Buffer) dst).m_var;
		final int dstLen = dstVar.length();
		final int newDstLen = dstLen + n;
		dstVar.ensureCapacity(newDstLen);
		final IUnit[] dstUnits = dstVar.units();
		System.arraycopy(units, positionIndex, dstUnits, dstLen, n);
		dstVar.length(newDstLen);

		Arrays.fill(units, positionIndex, n, null);
		var.positionIndex(0);
		var.markIndex(-1);
		var.length(1);
		units[0] = factory.getUnit();
	}

	@Override
	public int reserveHead(int size) {
		final IUnit unit = m_var.units()[0];
		if (unit.size() > 0)
			return unit.start();

		final int capacity = unit.capacity();
		if (size > capacity)
			size = capacity;
		else if (size < 0) {
			size += capacity;
			if (size < 0)
				size = 0;
		}
		unit.start(size);
		return size;
	}

	@Override
	public int headReserved() {
		return m_var.units()[0].start();
	}

	@Override
	public IBuffer setLength(int newLength) {
		if (newLength < 0)
			throw new IllegalArgumentException();

		int len = length();
		if (newLength == len)
			return this;

		if (newLength > len) {
			writeFill((byte) 0, newLength - len);
			return this;
		}

		if (newLength == 0) {
			drain();
			return this;
		}

		final Var var = m_var;
		final IUnit[] units = var.units();
		final BufferFactory factory = var.factory();
		int n = var.length();
		len = len - newLength;
		IUnit unit;
		while ((len -= (unit = units[--n]).size()) >= 0) {
			factory.putUnit(unit);
			units[n] = null;
		}
		var.length(n + 1);
		unit.size(-len);

		if (var.positionIndex() > n)
			var.positionIndex(n);
		else if (unit.position() > unit.size())
			unit.position(unit.size());

		if (var.markIndex() > n || unit.mark() > unit.position())
			var.markIndex(-1);

		return this;
	}

	@Override
	public IBuffer writeFill(byte b, int count) {
		if (count < 0)
			throw new IllegalArgumentException();

		if (count < 1)
			return this;

		final Var var = m_var;
		IUnit unit = Util.lastUnit(var);
		for (;;) {
			int size = unit.size();
			int index = unit.start() + unit.size();
			int n = unit.capacity() - index;
			if (n >= count) {
				unit.setFill(index, b, count);
				unit.size(size + count);
				break;
			} else {
				unit.setFill(index, b, n);
				unit.size(size + n);
			}
			count -= n;
			unit = Util.appendNewUnit(var);
		}

		return this;
	}

	@Override
	public IBuffer setFill(int index, byte b, int count) {
		if (count < 0)
			throw new IndexOutOfBoundsException();

		final Var var = m_var;
		final IUnit[] units = var.units();
		final int len = var.length();
		int i = 0;
		IUnit unit;
		int size;
		while (index > (size = (unit = units[i]).size())) {
			index -= size;
			if (++i == len)
				throw new IndexOutOfBoundsException();
		}

		int n = size - index;
		index += unit.start();
		for (;;) {
			if (n >= count) {
				unit.setFill(index, b, count);
				break;
			}
			unit.setFill(index, b, n);
			count -= n;
			if (++i == len)
				throw new IndexOutOfBoundsException();
			unit = units[i];
			n = unit.size();
			index = unit.start();
		}
		return this;
	}

	@Override
	public IBuffer prependFill(byte b, int count) {
		if (count < 0)
			throw new IndexOutOfBoundsException();

		if (count < 1)
			return this;

		final Var var = m_var;
		IUnit unit = Util.firstUnit(var);
		for (;;) {
			int n = unit.start();
			if (n >= count) {
				int start = n - count;
				unit.setFill(start, b, count);
				unit.start(start);
				unit.size(unit.size() + count);
				break;
			}
			unit.setFill(0, b, n);
			unit.start(0);
			unit.size(unit.size() + n);
			count -= n;
			unit = Util.prependNewUnit(var);
		}
		return this;
	}

	@Override
	public IBuffer set(int index, byte b) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		final int len = var.length();
		int i = 0;
		IUnit unit;
		int size;
		while (index >= (size = (unit = units[i]).size())) {
			index -= size;
			if (++i == len)
				throw new IndexOutOfBoundsException();
		}
		unit.set(unit.start() + index, b);
		return this;
	}

	@Override
	public IBuffer set(int index, char c, ISetCharEncoder encoder) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int n = var.length();
		int i = 0;
		int size;
		while (index > (size = units[i].size())) {
			index -= size;
			if (++i == n)
				throw new IndexOutOfBoundsException();
		}
		n = var.positionIndex();
		var.positionIndex(i);
		try {
			encoder.set(c, var, index);
		} finally {
			var.positionIndex(n);
		}
		return this;
	}

	@Override
	public IBuffer set(int index, short s, ISetShortEncoder encoder) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int n = var.length();
		int i = 0;
		int size;
		while (index > (size = units[i].size())) {
			index -= size;
			if (++i == n)
				throw new IndexOutOfBoundsException();
		}
		n = var.positionIndex();
		var.positionIndex(i);
		try {
			encoder.set(s, var, index);
		} finally {
			var.positionIndex(n);
		}
		return this;
	}

	@Override
	public IBuffer set(int index, int i, ISetIntEncoder encoder) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int n = var.length();
		int j = 0;
		int size;
		while (index > (size = units[j].size())) {
			index -= size;
			if (++j == n)
				throw new IndexOutOfBoundsException();
		}
		n = var.positionIndex();
		var.positionIndex(j);
		try {
			encoder.set(i, var, index);
		} finally {
			var.positionIndex(n);
		}
		return this;
	}

	@Override
	public IBuffer set(int index, long l, ISetLongEncoder encoder) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int n = var.length();
		int i = 0;
		int size;
		while (index > (size = units[i].size())) {
			index -= size;
			if (++i == n)
				throw new IndexOutOfBoundsException();
		}
		n = var.positionIndex();
		var.positionIndex(i);
		try {
			encoder.set(l, var, index);
		} finally {
			var.positionIndex(n);
		}
		return this;
	}

	@Override
	public IBuffer set(int index, float f, ISetFloatEncoder encoder) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int n = var.length();
		int i = 0;
		int size;
		while (index > (size = units[i].size())) {
			index -= size;
			if (++i == n)
				throw new IndexOutOfBoundsException();
		}
		n = var.positionIndex();
		var.positionIndex(i);
		try {
			encoder.set(f, var, index);
		} finally {
			var.positionIndex(n);
		}
		return this;
	}

	@Override
	public IBuffer set(int index, double d, ISetDoubleEncoder encoder) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int n = var.length();
		int i = 0;
		int size;
		while (index > (size = units[i].size())) {
			index -= size;
			if (++i == n)
				throw new IndexOutOfBoundsException();
		}
		n = var.positionIndex();
		var.positionIndex(i);
		try {
			encoder.set(d, var, index);
		} finally {
			var.positionIndex(n);
		}
		return this;
	}

	@Override
	public <T> IBuffer set(int index, T src, ISetEncoder<T> encoder) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int n = var.length();
		int i = 0;
		int size;
		while (index > (size = units[i].size())) {
			index -= size;
			if (++i == n)
				throw new IndexOutOfBoundsException();
		}
		n = var.positionIndex();
		var.positionIndex(i);
		try {
			encoder.set(src, var, index);
		} finally {
			var.positionIndex(n);
		}
		return this;
	}

	@Override
	public <T> IBuffer set(int index, T src, int offset, int length, ISetRangedEncoder<T> encoder) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int n = var.length();
		int i = 0;
		int size;
		while (index > (size = units[i].size())) {
			index -= size;
			if (++i == n)
				throw new IndexOutOfBoundsException();
		}
		n = var.positionIndex();
		var.positionIndex(i);
		try {
			encoder.set(src, offset, length, var, index);
		} finally {
			var.positionIndex(n);
		}
		return this;
	}

	@Override
	public IBuffer write(byte b) {
		final Var var = m_var;
		IUnit unit = var.lastUnit();
		if (!unit.appendable()) {
			unit = var.create();
			var.append(unit);
		}
		int size = unit.size();
		unit.set(unit.start() + size, b);
		unit.size(++size);
		return this;
	}

	@Override
	public IBuffer write(char c, IWriteCharEncoder encoder) {
		encoder.write(c, m_var);
		return this;
	}

	@Override
	public IBuffer write(short s, IWriteShortEncoder encoder) {
		encoder.write(s, m_var);
		return this;
	}

	@Override
	public IBuffer write(int i, IWriteIntEncoder encoder) {
		encoder.write(i, m_var);
		return this;
	}

	@Override
	public IBuffer write(long l, IWriteLongEncoder encoder) {
		encoder.write(l, m_var);
		return this;
	}

	@Override
	public IBuffer write(float f, IWriteFloatEncoder encoder) {
		encoder.write(f, m_var);
		return this;
	}

	@Override
	public IBuffer write(double d, IWriteDoubleEncoder encoder) {
		encoder.write(d, m_var);
		return this;
	}

	@Override
	public <T> IBuffer write(T src, IWriteEncoder<T> encoder) {
		encoder.write(src, m_var);
		return this;
	}

	@Override
	public <T> IBuffer write(T src, int offset, int length, IWriteRangedEncoder<T> encoder) {
		encoder.write(src, offset, length, m_var);
		return this;
	}

	@Override
	public byte byteAt(int index) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		final int n = var.length();
		int i = 0;
		IUnit unit = units[i];
		int size;
		while (index >= (size = unit.size())) {
			index -= size;
			if (++i == n)
				throw new IndexOutOfBoundsException();
			unit = units[i];
		}
		return unit.byteAt(unit.start() + index);
	}

	@Override
	public int getUnsignedByte(int index) {
		return byteAt(index) & 0xFF;
	}

	@Override
	public int getUnsignedShort(int index, IShortCodec codec) {
		return get(index, codec) & 0xFFFF;
	}

	@Override
	public int readUnsignedByte() {
		return read() & 0xFF;
	}

	@Override
	public int readUnsignedShort(IShortCodec codec) {
		return codec.read(m_var) & 0xFFFF;
	}

	@Override
	public char get(int index, IGetCharDecoder decoder) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int n = var.length();
		int i = 0;
		int size;
		while (index > (size = units[i].size())) {
			index -= size;
			if (++i == n)
				throw new IndexOutOfBoundsException();
		}
		n = var.positionIndex();
		var.positionIndex(i);
		try {
			return decoder.get(var, index);
		} finally {
			var.positionIndex(n);
		}
	}

	@Override
	public short get(int index, IGetShortDecoder decoder) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int n = var.length();
		int i = 0;
		int size;
		while (index > (size = units[i].size())) {
			index -= size;
			if (++i == n)
				throw new IndexOutOfBoundsException();
		}
		n = var.positionIndex();
		var.positionIndex(i);
		try {
			return decoder.get(var, index);
		} finally {
			var.positionIndex(n);
		}
	}

	@Override
	public int get(int index, IGetIntDecoder decoder) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int n = var.length();
		int i = 0;
		int size;
		while (index > (size = units[i].size())) {
			index -= size;
			if (++i == n)
				throw new IndexOutOfBoundsException();
		}
		n = var.positionIndex();
		var.positionIndex(i);
		try {
			return decoder.get(var, index);
		} finally {
			var.positionIndex(n);
		}
	}

	@Override
	public long get(int index, IGetLongDecoder decoder) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int n = var.length();
		int i = 0;
		int size;
		while (index > (size = units[i].size())) {
			index -= size;
			if (++i == n)
				throw new IndexOutOfBoundsException();
		}
		n = var.positionIndex();
		var.positionIndex(i);
		try {
			return decoder.get(var, index);
		} finally {
			var.positionIndex(n);
		}
	}

	@Override
	public float get(int index, IGetFloatDecoder decoder) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int n = var.length();
		int i = 0;
		int size;
		while (index > (size = units[i].size())) {
			index -= size;
			if (++i == n)
				throw new IndexOutOfBoundsException();
		}
		n = var.positionIndex();
		var.positionIndex(i);
		try {
			return decoder.get(var, index);
		} finally {
			var.positionIndex(n);
		}
	}

	@Override
	public double get(int index, IGetDoubleDecoder decoder) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int n = var.length();
		int i = 0;
		int size;
		while (index > (size = units[i].size())) {
			index -= size;
			if (++i == n)
				throw new IndexOutOfBoundsException();
		}
		n = var.positionIndex();
		var.positionIndex(i);
		try {
			return decoder.get(var, index);
		} finally {
			var.positionIndex(n);
		}
	}

	@Override
	public <T> T get(int index, IGetDecoder<T> decoder) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int n = var.length();
		int i = 0;
		int size;
		while (index > (size = units[i].size())) {
			index -= size;
			if (++i == n)
				throw new IndexOutOfBoundsException();
		}
		n = var.positionIndex();
		var.positionIndex(i);
		try {
			return decoder.get(var, index);
		} finally {
			var.positionIndex(n);
		}
	}

	@Override
	public <T> T get(int index, int length, IGetLimitedDecoder<T> decoder) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int n = var.length();
		int i = 0;
		int size;
		while (index > (size = units[i].size())) {
			index -= size;
			if (++i == n)
				throw new IndexOutOfBoundsException();
		}
		n = var.positionIndex();
		var.positionIndex(i);
		try {
			return decoder.get(var, index, length);
		} finally {
			var.positionIndex(n);
		}
	}

	@Override
	public <T> void get(int index, T dst, IGetToDstDecoder<T> decoder) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int n = var.length();
		int i = 0;
		int size;
		while (index > (size = units[i].size())) {
			index -= size;
			if (++i == n)
				throw new IndexOutOfBoundsException();
		}
		n = var.positionIndex();
		var.positionIndex(i);
		try {
			decoder.get(dst, var, index);
		} finally {
			var.positionIndex(n);
		}
	}

	@Override
	public <T> void get(int index, T dst, int offset, int length, IGetToRangedDstDecoder<T> decoder) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int n = var.length();
		int i = 0;
		int size;
		while (index > (size = units[i].size())) {
			index -= size;
			if (++i == n)
				throw new IndexOutOfBoundsException();
		}
		n = var.positionIndex();
		var.positionIndex(i);
		try {
			decoder.get(dst, offset, length, var, index);
		} finally {
			var.positionIndex(n);
		}
	}

	@Override
	public byte read() {
		final Var var = m_var;
		IUnit unit = var.currentUnit();
		while (unit.isEmpty()) {
			unit = var.nextUnit();
			if (unit == null)
				throw new BufferUnderflowException();
		}
		int position = unit.position();
		byte b = unit.byteAt(unit.start() + position);
		unit.position(++position);
		return b;
	}

	@Override
	public char read(IReadCharDecoder decoder) {
		return decoder.read(m_var);
	}

	@Override
	public short read(IReadShortDecoder decoder) {
		return decoder.read(m_var);
	}

	@Override
	public int read(IReadIntDecoder decoder) {
		return decoder.read(m_var);
	}

	@Override
	public long read(IReadLongDecoder decoder) {
		return decoder.read(m_var);
	}

	@Override
	public float read(IReadFloatDecoder decoder) {
		return decoder.read(m_var);
	}

	@Override
	public double read(IReadDoubleDecoder decoder) {
		return decoder.read(m_var);
	}

	@Override
	public <T> T read(IReadDecoder<T> decoder) {
		return decoder.read(m_var);
	}

	@Override
	public <T> T read(int length, IReadLimitedDecoder<T> decoder) {
		return decoder.read(m_var, length);
	}

	@Override
	public <T> int read(T dst, IReadToDstDecoder<T> decoder) {
		return decoder.read(dst, m_var);
	}

	@Override
	public <T> int read(T dst, int offset, int length, IReadToRangedDstDecoder<T> decoder) {
		return decoder.read(dst, offset, length, m_var);
	}

	@Override
	public IBuffer prepend(byte b) {
		final Var var = m_var;
		IUnit unit = var.firstUnit();
		if (!unit.prependable()) {
			unit = var.create();
			unit.start(unit.capacity());
			var.prepend(unit);
		}
		int start = unit.start();
		unit.set(--start, b);
		unit.start(start);
		unit.size(unit.size() + 1);
		return this;
	}

	@Override
	public IBuffer prepend(char c, IPrependCharEncoder encoder) {
		encoder.prepend(c, m_var);
		return this;
	}

	@Override
	public IBuffer prepend(short s, IPrependShortEncoder encoder) {
		encoder.prepend(s, m_var);
		return this;
	}

	@Override
	public IBuffer prepend(int i, IPrependIntEncoder encoder) {
		encoder.prepend(i, m_var);
		return this;
	}

	@Override
	public IBuffer prepend(long l, IPrependLongEncoder encoder) {
		encoder.prepend(l, m_var);
		return this;
	}

	@Override
	public IBuffer prepend(float f, IPrependFloatEncoder encoder) {
		encoder.prepend(f, m_var);
		return this;
	}

	@Override
	public IBuffer prepend(double d, IPrependDoubleEncoder encoder) {
		encoder.prepend(d, m_var);
		return this;
	}

	@Override
	public <T> IBuffer prepend(T src, IPrependEncoder<T> encoder) {
		encoder.prepend(src, m_var);
		return this;
	}

	@Override
	public <T> IBuffer prepend(T src, int offset, int length, IPrependRangedEncoder<T> encoder) {
		encoder.prepend(src, offset, length, m_var);
		return this;
	}

	@Override
	public void dump(StringBuilder builder) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		final int n = var.length();
		try (Blob blob = Blob.get()) {
			for (int i = 0; i < n; ++i) {
				final IUnit unit = units[i];
				blob.add(unit, unit.start(), unit.size());
			}
			blob.dump(builder);
		}
	}

	@Override
	public boolean isClosed() {
		return m_var == null;
	}

	@Override
	public void close() {
		final Var var = m_var;
		if (var == null)
			return;
		m_var = null;
		var.close();
	}

	@Override
	public byte[] getBytes(int start) {
		return getBytes(start, size() - start);
	}

	@Override
	public byte[] getBytes(int start, int length) {
		final Var var = m_var;
		final IUnit[] units = var.units();
		int n = var.length();
		int i = 0;
		IUnit unit = units[i];
		int size;
		while (start > (size = unit.size())) {
			start -= size;
			if (++i == n)
				throw new IndexOutOfBoundsException();
			unit = units[i];
		}

		n = var.positionIndex();
		var.positionIndex(i);
		try {
			return Codec.byteArray().get(var, start, length);
		} finally {
			var.positionIndex(n);
		}
	}

	@Override
	public void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin) {
		int length = srcEnd - srcBegin;
		final Var var = m_var;
		final IUnit[] units = var.units();
		int n = var.length();
		int i = 0;
		int size;
		while (srcBegin > (size = units[i].size())) {
			srcBegin -= size;
			if (++i == n)
				throw new IndexOutOfBoundsException();
		}
		n = var.positionIndex();
		var.positionIndex(i);
		try {
			Codec.byteArray().get(dst, dstBegin, length, var, srcBegin);
		} finally {
			var.positionIndex(n);
		}
	}

	@Override
	public int length() {
		return size();
	}

	@Override
	public int compareTo(IBuffer that) {
		if (!(that instanceof Buffer))
			return compareInternal(that);

		final Var var = m_var;
		Buffer thatBuf = (Buffer) that;
		final Var thatVar = thatBuf.m_var;

		int remaining = var.remaining();
		int thatRemaining = thatVar.remaining();
		int ret = 0;
		if (remaining > thatRemaining)
			ret = 1;
		else if (remaining < thatRemaining)
			ret = -1;

		if (remaining < 1 || thatRemaining < 1)
			return ret;

		final IUnit[] units = var.units();
		final int s = var.length();
		int p = var.positionIndex();
		IUnit unit = units[p];

		final IUnit[] thatUnits = thatVar.units();
		final int t = thatVar.length();
		int q = thatVar.positionIndex();
		IUnit thatUnit = thatUnits[q];

		int i = unit.position();
		int j = thatUnit.position();

		remaining = unit.remaining();
		thatRemaining = thatUnit.remaining();
		for (;;) {
			if (remaining > thatRemaining) {
				int n = compare(unit, i, thatUnit, j, thatRemaining);
				if (n != 0)
					return n;

				if (++q == t)
					break;

				i = thatRemaining;
				remaining -= thatRemaining;
				j = 0;
				thatUnit = thatUnits[q];
				thatRemaining = thatUnit.size();
			} else {
				int n = compare(unit, i, thatUnit, j, remaining);
				if (n != 0)
					return n;

				if (++p == s)
					break;

				j = remaining;
				thatRemaining -= remaining;
				i = 0;
				unit = units[p];
				remaining = unit.size();
			}
		}

		return ret;
	}

	@Override
	public int readIn(ReadableByteChannel in) throws IOException {
		IUnit unit = Util.lastUnit(m_var);
		int n = in.read(unit.getByteBufferForWrite());
		if (n > 0)
			unit.size(unit.size() + n);
		return n;
	}

	@Override
	public int writeOut(WritableByteChannel out) throws IOException {
		final Var var = m_var;
		final IUnit[] units = var.units();
		final int last = var.length() - 1;
		int p = var.positionIndex();
		IUnit unit;
		while ((unit = units[p]).isEmpty()) {
			if (p == last)
				return 0;
			var.positionIndex(++p);
		}

		final int n;
		if (p == last || !(out instanceof GatheringByteChannel)) {
			n = out.write(unit.getByteBufferForRead());
			if (n > 0)
				unit.position(unit.position() + n);
		} else {
			final ByteBufferArray bba = ByteBufferArray.get();
			try {
				bba.add(unit.getByteBufferForRead());
				while (p != last) {
					unit = units[++p];
					bba.add(unit.getByteBufferForRead());
				}
				n = (int) ((GatheringByteChannel) out).write(bba.array(), 0, bba.size());
			} finally {
				bba.clear();
			}

			if (n > 0) {
				int m = n;
				p = var.positionIndex();
				unit = units[p];
				while ((m -= unit.skip(m)) > 0)
					unit = units[++p];

				var.positionIndex(p);
			}
		}

		return n;
	}

	@Override
	public OutputStream getOutputStream() {
		return new BufferOutputStream(this);
	}

	@Override
	public InputStream getInputStream() {
		return new BufferInputStream(this);
	}

	@Override
	public IBuffer slice() {
		final Var var = m_var;
		final IUnit[] units = var.units();
		final int len = var.length();
		int pos = var.positionIndex();

		final Var sliceVar = Var.get(len - pos);
		final IUnit[] sliceUnits = sliceVar.units();
		int slicePos = 0;

		while (pos < len) {
			final IUnit unit = units[pos++];
			sliceUnits[slicePos++] = unit.slice(unit.position(), unit.size());
		}

		sliceVar.positionIndex(0);
		sliceVar.length(slicePos);

		return new Buffer(sliceVar);
	}

	@Override
	public IBuffer duplicate() {
		final Var var = m_var;
		final IUnit[] units = var.units();
		final int n = var.length();

		final Var dupVar = Var.get(n);
		final IUnit[] dupUnits = dupVar.units();
		for (int i = 0; i < n; ++i)
			dupUnits[i] = units[i].duplicate();

		dupVar.positionIndex(var.positionIndex());
		dupVar.markIndex(var.markIndex());
		dupVar.length(n);

		return new Buffer(dupVar);
	}

	public IUnitChain unitChain() {
		return m_var;
	}

	int readByte() {
		final Var var = m_var;
		IUnit unit = var.currentUnit();
		while (unit.isEmpty()) {
			unit = var.nextUnit();
			if (unit == null)
				return -1;
		}
		int position = unit.position();
		int i = unit.byteAt(unit.start() + position) & 0xFF;
		unit.position(++position);
		return i;
	}

	private int compareInternal(IBuffer that) {
		final Var var = m_var;
		int n = that.remaining();
		int remaining = var.remaining();
		if (n < remaining) {
			remaining = n;
			n = 1;
		} else if (n > remaining)
			n = -1;
		else
			n = 0;

		if (remaining > 0) {
			final IUnit[] units = var.units();
			int positionIndex = var.positionIndex();
			IUnit unit = units[positionIndex];
			int size = unit.remaining();
			int i = that.position();
			for (;;) {
				if (remaining < size)
					size = remaining;

				int ret = compare(unit, that, i, size);
				if (ret != 0)
					return ret;

				i += size;
				remaining -= size;

				if (remaining <= 0)
					break;

				unit = units[++positionIndex];
				size = unit.size();
			}
		}

		return n;
	}

	static final class Var implements ICloseable, IUnitChain {

		private static final IThreadLocalCache<Var> c_cache = ThreadLocalCache.weakLinkedCache();

		private BufferFactory m_factory;
		private int m_positionIndex = -1;
		private int m_markIndex = -1;
		private IUnit[] m_units;
		private int m_length = 0;

		private Var() {
			m_units = new IUnit[8];
		}

		private Var(int initialCapacity) {
			if (initialCapacity < 8)
				initialCapacity = 8;
			m_units = new IUnit[initialCapacity];
		}

		static Var get() {
			Var var = c_cache.take();
			if (var == null)
				var = new Var();
			else {
				var.m_positionIndex = -1;
				var.m_markIndex = -1;
				var.m_length = 0;
			}
			return var;
		}

		static Var get(int minCapacity) {
			Var var = c_cache.take();
			if (var == null)
				var = new Var(minCapacity);
			else {
				var.m_positionIndex = -1;
				var.m_markIndex = -1;
				var.m_length = 0;
				var.ensureCapacityInternal(minCapacity);
			}
			return var;
		}

		void init(BufferFactory factory) {
			m_units[0] = factory.getUnit();
			m_positionIndex = 0;
			m_length = 1;
			m_factory = factory;
		}

		void init(BufferFactory factory, IUnit unit) {
			m_positionIndex = 0;
			m_length = 1;
			m_units[0] = unit;
			m_factory = factory;
		}

		void init(BufferFactory factory, IUnit[] units, int offset, int count) {
			m_positionIndex = 0;
			m_length = count;
			System.arraycopy(units, offset, m_units, 0, count);
			m_factory = factory;
		}

		BufferFactory factory() {
			return m_factory;
		}

		IUnit[] units() {
			return m_units;
		}

		int length() {
			return m_length;
		}

		void length(int newLength) {
			m_length = newLength;
		}

		int positionIndex() {
			return m_positionIndex;
		}

		void positionIndex(int positionIndex) {
			m_positionIndex = positionIndex;
		}

		int markIndex() {
			return m_markIndex;
		}

		void markIndex(int markIndex) {
			m_markIndex = markIndex;
		}

		@Override
		public IUnit create() {
			return m_factory.getUnit();
		}

		@Override
		public IUnit create(int minimumCapacity) {
			return m_factory.getUnit(minimumCapacity);
		}

		@Override
		public IUnit currentUnit() {
			return m_units[m_positionIndex];
		}

		@Override
		public IUnit nextUnit() {
			int positionIndex = m_positionIndex;
			if (++positionIndex == m_length)
				return null;
			m_positionIndex = positionIndex;
			return m_units[positionIndex];
		}

		@Override
		public IUnit firstUnit() {
			return m_units[0];
		}

		@Override
		public IUnit lastUnit() {
			return m_units[m_length - 1];
		}

		@Override
		public void append(IUnit unit) {
			final int length = m_length;
			final int newLength = length + 1;
			if (newLength > m_units.length)
				expandCapacityForAppend(newLength);

			m_units[length] = unit;
			m_length = newLength;
		}

		@Override
		public void prepend(IUnit unit) {
			final IUnit[] units = m_units;
			final int positionIndex = m_positionIndex;
			if (positionIndex > 0 || units[positionIndex].position() > 0)
				throw new UnsupportedOperationException("prepend is not allowed when position() > 0");

			final int length = m_length;
			final int newLength = length + 1;
			if (newLength > units.length)
				expandCapacityForPrepend(newLength);
			else
				System.arraycopy(units, 0, units, 1, length);

			m_units[0] = unit;
			m_length = newLength;
		}

		@Override
		public int remaining() {
			final IUnit[] units = m_units;
			int i = m_positionIndex;
			int remaining = units[i].remaining();
			for (int n = m_length; ++i < n;)
				remaining += units[i].size();
			return remaining;
		}

		@Override
		public int size() {
			return m_length;
		}

		@Override
		public IUnit unitAt(int index) {
			if (index < 0 || index >= m_length)
				throw new IndexOutOfBoundsException();
			return m_units[index];
		}

		void ensureCapacity(int minCapacity) {
			if (m_units.length < minCapacity)
				expandCapacityForAppend(minCapacity);
		}

		private void ensureCapacityInternal(int minCapacity) {
			int len = m_units.length;
			if (len < minCapacity)
				len = (len + 1) << 1;
			if (len < 0)
				len = Integer.MAX_VALUE;
			else if (minCapacity > len)
				len = minCapacity;
			m_units = new IUnit[len];
		}

		private void expandCapacityForAppend(int minCapacity) {
			int newCapacity = (m_units.length + 1) << 1;
			if (newCapacity < 0)
				newCapacity = Integer.MAX_VALUE;
			else if (minCapacity > newCapacity)
				newCapacity = minCapacity;

			final IUnit[] units = new IUnit[newCapacity];
			System.arraycopy(m_units, 0, units, 0, m_length);
			m_units = units;
		}

		private void expandCapacityForPrepend(int minCapacity) {
			int newCapacity = (m_units.length + 1) << 1;
			if (newCapacity < 0)
				newCapacity = Integer.MAX_VALUE;
			else if (minCapacity > newCapacity)
				newCapacity = minCapacity;

			final IUnit[] units = new IUnit[newCapacity];
			System.arraycopy(m_units, 0, units, 1, m_length);
			m_units = units;
		}

		void drain() {
			final IUnit[] units = m_units;
			final int n = m_length;
			final BufferFactory factory = m_factory;
			if (factory != null) {
				for (int i = 1; i < n; ++i) {
					factory.putUnit(units[i]);
					units[i] = null;
				}
			} else {
				for (int i = 1; i < n; ++i)
					units[i] = null;
			}
			m_positionIndex = 0;
			m_markIndex = -1;
			m_length = 1;
			units[0].clear();
		}

		@Override
		public void close() {
			final IUnit[] units = m_units;
			final int n = m_length;
			final BufferFactory factory = m_factory;
			if (factory != null) {
				m_factory = null;
				for (int i = 0; i < n; ++i) {
					factory.putUnit(units[i]);
					units[i] = null;
				}
			} else {
				for (int i = 0; i < n; ++i)
					units[i] = null;
			}
			c_cache.put(this);
		}
	}
}
