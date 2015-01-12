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

package org.jruyi.io.buffer.codec.shortarray;

import static org.jruyi.io.buffer.Helper.B_SIZE_OF_SHORT;
import static org.jruyi.io.buffer.Helper.SIZE_OF_SHORT;

import java.nio.BufferUnderflowException;

import org.jruyi.io.ICodec;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;
import org.jruyi.io.buffer.Util;

public abstract class AbstractCodec implements ICodec<short[]> {

	@Override
	public final short[] read(IUnitChain unitChain) {
		IUnit unit = unitChain.currentUnit();
		while (unit.isEmpty()) {
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new BufferUnderflowException();
		}

		int n = unitChain.remaining();
		if (n < 1 || (n & (SIZE_OF_SHORT - 1)) != 0)
			throw new BufferUnderflowException();

		n >>= B_SIZE_OF_SHORT;
		return readShorts(new short[n], 0, n, unitChain);
	}

	@Override
	public final short[] read(IUnitChain unitChain, int length) {
		if (length < 0 || (length & (SIZE_OF_SHORT - 1)) != 0)
			throw new IllegalArgumentException();

		final int n = length >> B_SIZE_OF_SHORT;
		return readShorts(new short[n], 0, n, unitChain);
	}

	@Override
	public final int read(short[] dst, IUnitChain unitChain) {
		final int n = dst.length;
		readShorts(dst, 0, n, unitChain);
		return SIZE_OF_SHORT * n;
	}

	@Override
	public final int read(short[] dst, int offset, int length, IUnitChain unitChain) {
		final int n = offset + length;
		if ((offset | length | n | (dst.length - n)) < 0)
			throw new IndexOutOfBoundsException();

		readShorts(dst, offset, length, unitChain);
		return SIZE_OF_SHORT * length;
	}

	@Override
	public final void write(short[] src, IUnitChain unitChain) {
		writeShorts(src, 0, src.length, unitChain);
	}

	@Override
	public final void write(short[] src, int offset, int length, IUnitChain unitChain) {
		final int n = offset + length;
		if ((offset | length | n | (src.length - n)) < 0)
			throw new IndexOutOfBoundsException();

		writeShorts(src, offset, length, unitChain);
	}

	@Override
	public final short[] get(IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		IUnit unit = unitChain.currentUnit();
		int n = unitChain.remaining() + unit.position() - index;
		if (n < 1 || (n & (SIZE_OF_SHORT - 1)) != 0)
			throw new IndexOutOfBoundsException();

		n >>= B_SIZE_OF_SHORT;
		return getShorts(new short[n], 0, n, unitChain, index);
	}

	@Override
	public final short[] get(IUnitChain unitChain, int index, int length) {
		if (index < 0 || length < 0 || (length & (SIZE_OF_SHORT - 1)) != 0)
			throw new IndexOutOfBoundsException();

		final int n = length >> B_SIZE_OF_SHORT;
		return getShorts(new short[n], 0, n, unitChain, index);
	}

	@Override
	public final void get(short[] dst, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		getShorts(dst, 0, dst.length, unitChain, index);
	}

	@Override
	public final void get(short[] dst, int offset, int length, IUnitChain unitChain, int index) {
		final int n = offset + length;
		if ((offset | length | n | (dst.length - n)) < 0 || index < 0)
			throw new IndexOutOfBoundsException();

		getShorts(dst, offset, length, unitChain, index);
	}

	@Override
	public final void set(short[] src, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		setShorts(src, 0, src.length, unitChain, index);
	}

	@Override
	public final void set(short[] src, int offset, int length, IUnitChain unitChain, int index) {
		final int n = offset + length;
		if ((offset | length | n | (src.length - n)) < 0 || index < 0)
			throw new IndexOutOfBoundsException();

		setShorts(src, offset, length, unitChain, index);
	}

	@Override
	public final void prepend(short[] src, IUnitChain unitChain) {
		prependShorts(src, 0, src.length, unitChain);
	}

	@Override
	public final void prepend(short[] src, int offset, int length, IUnitChain unitChain) {
		final int n = offset + length;
		if ((offset | length | n | (src.length - n)) < 0)
			throw new IndexOutOfBoundsException();
		prependShorts(src, offset, length, unitChain);
	}

	abstract boolean isNative();

	abstract IUnit readShort(short[] dst, int offset, IUnit unit, IUnitChain unitChain);

	abstract int getShort(short[] dst, int offset, IUnit unit, int index, IUnitChain unitChain);

	abstract int setShort(short l, IUnit unit, int index, IUnitChain unitChain);

	private short[] readShorts(short[] dst, int offset, int length, IUnitChain unitChain) {
		if (length < 1)
			return dst;

		IUnit unit = unitChain.currentUnit();
		for (;;) {
			int position = unit.position();
			int index = unit.start() + position;
			int m = unit.remaining();
			int k = m >> B_SIZE_OF_SHORT;
			if (k > 0) {
				if (k > length) {
					k = length;
					length = 0;
				} else
					length -= k;
				final int n = k * SIZE_OF_SHORT;
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
						dst[i] = Short.reverseBytes(dst[i]);
					if (length < 1)
						break;
				}
			}
			k = m & (SIZE_OF_SHORT - 1);
			if (k > 0) {
				unit = readShort(dst, offset, unit, unitChain);
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

	private short[] getShorts(short[] dst, int offset, int length, IUnitChain unitChain, int index) {
		if (length < 1)
			return dst;

		IUnit unit = unitChain.currentUnit();
		int m = unit.size() - index;
		index += unit.start();
		for (;;) {
			int k = m >> B_SIZE_OF_SHORT;
			if (k > 0) {
				if (k > length) {
					k = length;
					length = 0;
				} else
					length -= k;
				final int n = k * SIZE_OF_SHORT;
				unit.get(index, n, dst, offset);
				if (isNative()) {
					if (length < 1)
						break;
					offset += k;
				} else {
					int i = offset;
					offset += k;
					for (; i < offset; ++i)
						dst[i] = Short.reverseBytes(dst[i]);
					if (length < 1)
						break;
				}
				index += n;
			}
			k = m & (SIZE_OF_SHORT - 1);
			if (k > 0) {
				index = getShort(dst, offset, unit, index, unitChain);
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

	private void writeShorts(short[] src, int offset, int length, IUnitChain unitChain) {
		if (length < 1)
			return;

		IUnit unit = Util.lastUnit(unitChain);
		for (;;) {
			int m = unit.available();
			int k = m >> B_SIZE_OF_SHORT;
			if (k > 0) {
				if (k > length) {
					k = length;
					length = 0;
				} else
					length -= k;
				int size = unit.size();
				int index = unit.start() + size;
				unit.size(size + SIZE_OF_SHORT * k);
				if (isNative()) {
					unit.set(index, src, offset, k);
					if (length < 1)
						break;
					offset += k;
				} else {
					for (int n = offset + k; offset < n; ++offset, index += SIZE_OF_SHORT)
						unit.set(index, Short.reverseBytes(src[offset]));
					if (length < 1)
						break;
				}
			}
			unit = Util.appendNewUnit(unitChain);
		}
	}

	private void setShorts(short[] src, int offset, int length, IUnitChain unitChain, int index) {
		if (length < 1)
			return;

		IUnit unit = unitChain.currentUnit();
		int m = unit.size() - index;
		index += unit.start();
		for (;;) {
			int k = m >> B_SIZE_OF_SHORT;
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
					index += k * SIZE_OF_SHORT;
				} else {
					for (int n = offset + k; offset < n; ++offset, index += SIZE_OF_SHORT)
						unit.set(index, Short.reverseBytes(src[offset]));
					if (length < 1)
						break;
				}
			}
			k = m & (SIZE_OF_SHORT - 1);
			if (k > 0) {
				index = setShort(src[offset], unit, index, unitChain);
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

	private void prependShorts(short[] src, int offset, int length, IUnitChain unitChain) {
		if (length < 1)
			return;
		IUnit unit = unitChain.firstUnit();
		for (;;) {
			int m = unit.start();
			int k = m >> B_SIZE_OF_SHORT;
			if (k > 0) {
				if (k > length) {
					k = length;
					length = 0;
				} else
					length -= k;
				int len = k * SIZE_OF_SHORT;
				int index = m - len;
				unit.start(index);
				unit.size(unit.size() + len);
				if (isNative()) {
					unit.set(index, src, offset, k);
					if (length < 1)
						break;
					offset += k;
				} else {
					for (int n = offset + k; offset < n; ++offset, index += SIZE_OF_SHORT)
						unit.set(index, Short.reverseBytes(src[offset]));
					if (length < 1)
						break;
				}
			}
			unit = Util.prependNewUnit(unitChain);
		}
	}
}
