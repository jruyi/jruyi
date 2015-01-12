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

package org.jruyi.io.buffer.codec.intarray;

import static org.jruyi.io.buffer.Helper.B_SIZE_OF_INT;
import static org.jruyi.io.buffer.Helper.SIZE_OF_INT;

import java.nio.BufferUnderflowException;

import org.jruyi.io.ICodec;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;
import org.jruyi.io.buffer.Util;

public abstract class AbstractCodec implements ICodec<int[]> {

	@Override
	public final int[] read(IUnitChain unitChain) {
		IUnit unit = unitChain.currentUnit();
		while (unit.isEmpty()) {
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new BufferUnderflowException();
		}

		int n = unitChain.remaining();
		if (n < 1 || (n & (SIZE_OF_INT - 1)) != 0)
			throw new BufferUnderflowException();

		n >>= B_SIZE_OF_INT;
		return readInts(new int[n], 0, n, unitChain);
	}

	@Override
	public final int[] read(IUnitChain unitChain, int length) {
		if (length < 0 || (length & (SIZE_OF_INT - 1)) != 0)
			throw new IllegalArgumentException();

		final int n = length >> B_SIZE_OF_INT;
		return readInts(new int[n], 0, n, unitChain);
	}

	@Override
	public final int read(int[] dst, IUnitChain unitChain) {
		final int n = dst.length;
		readInts(dst, 0, n, unitChain);
		return SIZE_OF_INT * n;
	}

	@Override
	public final int read(int[] dst, int offset, int length, IUnitChain unitChain) {
		final int n = offset + length;
		if ((offset | length | n | (dst.length - n)) < 0)
			throw new IndexOutOfBoundsException();

		readInts(dst, offset, length, unitChain);
		return SIZE_OF_INT * length;
	}

	@Override
	public final void write(int[] src, IUnitChain unitChain) {
		writeInts(src, 0, src.length, unitChain);
	}

	@Override
	public final void write(int[] src, int offset, int length, IUnitChain unitChain) {
		final int n = offset + length;
		if ((offset | length | n | (src.length - n)) < 0)
			throw new IndexOutOfBoundsException();

		writeInts(src, offset, length, unitChain);
	}

	@Override
	public final int[] get(IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		IUnit unit = unitChain.currentUnit();
		int n = unitChain.remaining() + unit.position() - index;
		if (n < 1 || (n & (SIZE_OF_INT - 1)) != 0)
			throw new IndexOutOfBoundsException();

		n >>= B_SIZE_OF_INT;
		return getInts(new int[n], 0, n, unitChain, index);
	}

	@Override
	public final int[] get(IUnitChain unitChain, int index, int length) {
		if (index < 0 || length < 0 || (length & (SIZE_OF_INT - 1)) != 0)
			throw new IndexOutOfBoundsException();

		final int n = length >> B_SIZE_OF_INT;
		return getInts(new int[n], 0, n, unitChain, index);
	}

	@Override
	public final void get(int[] dst, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		getInts(dst, 0, dst.length, unitChain, index);
	}

	@Override
	public final void get(int[] dst, int offset, int length, IUnitChain unitChain, int index) {
		final int n = offset + length;
		if ((offset | length | n | (dst.length - n)) < 0 || index < 0)
			throw new IndexOutOfBoundsException();

		getInts(dst, offset, length, unitChain, index);
	}

	@Override
	public final void set(int[] src, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		setInts(src, 0, src.length, unitChain, index);
	}

	@Override
	public final void set(int[] src, int offset, int length, IUnitChain unitChain, int index) {
		final int n = offset + length;
		if ((offset | length | n | (src.length - n)) < 0 || index < 0)
			throw new IndexOutOfBoundsException();

		setInts(src, offset, length, unitChain, index);
	}

	@Override
	public final void prepend(int[] src, IUnitChain unitChain) {
		prependInts(src, 0, src.length, unitChain);
	}

	@Override
	public final void prepend(int[] src, int offset, int length, IUnitChain unitChain) {
		final int n = offset + length;
		if ((offset | length | n | (src.length - n)) < 0)
			throw new IndexOutOfBoundsException();
		prependInts(src, offset, length, unitChain);
	}

	abstract boolean isNative();

	abstract IUnit readInt(int[] dst, int offset, IUnit unit, IUnitChain unitChain);

	abstract int getInt(int[] dst, int offset, IUnit unit, int index, IUnitChain unitChain);

	abstract int setInt(int l, IUnit unit, int index, IUnitChain unitChain);

	private int[] readInts(int[] dst, int offset, int length, IUnitChain unitChain) {
		if (length < 1)
			return dst;

		IUnit unit = unitChain.currentUnit();
		for (;;) {
			int position = unit.position();
			int index = unit.start() + position;
			int m = unit.remaining();
			int k = m >> B_SIZE_OF_INT;
			if (k > 0) {
				if (k > length) {
					k = length;
					length = 0;
				} else
					length -= k;
				final int n = k * SIZE_OF_INT;
				unit.get(index, n, dst, offset);
				unit.position(position + n);
				if (isNative()) {
					if (length < 1)
						break;
					offset += k;
				} else {
					int i = offset;
					offset += k;
					for (; i < offset; ++i)
						dst[i] = Integer.reverseBytes(dst[i]);
					if (length < 1)
						break;
				}
			}
			k = m & (SIZE_OF_INT - 1);
			if (k > 0) {
				unit = readInt(dst, offset, unit, unitChain);
				if (--length < 1)
					break;
				++offset;
			} else {
				unit = unitChain.nextUnit();
				if (unit == null)
					throw new BufferUnderflowException();
			}
		}
		return dst;
	}

	private int[] getInts(int[] dst, int offset, int length, IUnitChain unitChain, int index) {
		if (length < 1)
			return dst;

		IUnit unit = unitChain.currentUnit();
		int m = unit.size() - index;
		index += unit.start();
		for (;;) {
			int k = m >> B_SIZE_OF_INT;
			if (k > 0) {
				if (k > length) {
					k = length;
					length = 0;
				} else
					length -= k;
				final int n = k * SIZE_OF_INT;
				unit.get(index, n, dst, offset);
				if (isNative()) {
					if (length < 1)
						break;
					offset += k;
				} else {
					int i = offset;
					offset += k;
					for (; i < offset; ++i)
						dst[i] = Integer.reverseBytes(dst[i]);
					if (length < 1)
						break;
				}
				index += n;
			}
			k = m & (SIZE_OF_INT - 1);
			if (k > 0) {
				index = getInt(dst, offset, unit, index, unitChain);
				if (--length < 1)
					break;
				++offset;
				unit = unitChain.currentUnit();
				m = unit.start() + unit.size() - index;
			} else {
				unit = unitChain.nextUnit();
				if (unit == null)
					throw new IndexOutOfBoundsException();
				index = unit.start();
				m = unit.size();
			}
		}

		return dst;
	}

	private void writeInts(int[] src, int offset, int length, IUnitChain unitChain) {
		if (length < 1)
			return;

		IUnit unit = Util.lastUnit(unitChain);
		for (;;) {
			int m = unit.available();
			int k = m >> B_SIZE_OF_INT;
			if (k > 0) {
				if (k > length) {
					k = length;
					length = 0;
				} else
					length -= k;
				int size = unit.size();
				int index = unit.start() + size;
				unit.size(size + SIZE_OF_INT * k);
				if (isNative()) {
					unit.set(index, src, offset, k);
					if (length < 1)
						break;
					offset += k;
				} else {
					for (int n = offset + k; offset < n; ++offset, index += SIZE_OF_INT)
						unit.set(index, Integer.reverseBytes(src[offset]));
					if (length < 1)
						break;
				}
			}
			unit = Util.appendNewUnit(unitChain);
		}
	}

	private void setInts(int[] src, int offset, int length, IUnitChain unitChain, int index) {
		if (length < 1)
			return;

		IUnit unit = unitChain.currentUnit();
		int m = unit.size() - index;
		index += unit.start();
		for (;;) {
			int k = m >> B_SIZE_OF_INT;
			if (k > 0) {
				if (k > length) {
					k = length;
					length = 0;
				} else
					length -= k;
				if (isNative()) {
					unit.set(index, src, offset, k);
					if (length < 1)
						break;
					offset += k;
					index += k * SIZE_OF_INT;
				} else {
					for (int n = offset + k; offset < n; ++offset, index += SIZE_OF_INT)
						unit.set(index, Integer.reverseBytes(src[offset]));
					if (length < 1)
						break;
				}
			}
			k = m & (SIZE_OF_INT - 1);
			if (k > 0) {
				index = setInt(src[offset], unit, index, unitChain);
				if (--length < 1)
					break;
				++offset;
				unit = unitChain.currentUnit();
				m = unit.start() + unit.size() - index;
			} else {
				unit = unitChain.nextUnit();
				if (unit == null)
					throw new IndexOutOfBoundsException();
				index = unit.start();
				m = unit.size();
			}
		}
	}

	private void prependInts(int[] src, int offset, int length, IUnitChain unitChain) {
		if (length < 1)
			return;
		IUnit unit = unitChain.firstUnit();
		for (;;) {
			int m = unit.start();
			int k = m >> B_SIZE_OF_INT;
			if (k > 0) {
				if (k > length) {
					k = length;
					length = 0;
				} else
					length -= k;
				int len = k * SIZE_OF_INT;
				int index = m - len;
				unit.start(index);
				unit.size(unit.size() + len);
				if (isNative()) {
					unit.set(index, src, offset, k);
					if (length < 1)
						break;
					offset += k;
				} else {
					for (int n = offset + k; offset < n; ++offset, index += SIZE_OF_INT)
						unit.set(index, Integer.reverseBytes(src[offset]));
					if (length < 1)
						break;
				}
			}
			unit = Util.prependNewUnit(unitChain);
		}
	}
}
