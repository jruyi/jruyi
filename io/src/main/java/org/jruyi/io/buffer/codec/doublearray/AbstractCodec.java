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

package org.jruyi.io.buffer.codec.doublearray;

import static org.jruyi.io.buffer.Helper.B_SIZE_OF_LONG;
import static org.jruyi.io.buffer.Helper.SIZE_OF_LONG;

import java.nio.BufferUnderflowException;

import org.jruyi.io.ICodec;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;
import org.jruyi.io.buffer.Util;

public abstract class AbstractCodec implements ICodec<double[]> {

	@Override
	public final double[] read(IUnitChain unitChain) {
		IUnit unit = unitChain.currentUnit();
		while (unit.isEmpty()) {
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new BufferUnderflowException();
		}

		int n = unitChain.remaining();
		if (n < 1 || (n & (SIZE_OF_LONG - 1)) != 0)
			throw new BufferUnderflowException();

		n >>= B_SIZE_OF_LONG;
		return readDoubles(new double[n], 0, n, unitChain);
	}

	@Override
	public final double[] read(IUnitChain unitChain, int length) {
		if (length < 0 || (length & (SIZE_OF_LONG - 1)) != 0)
			throw new IllegalArgumentException();

		final int n = length >> B_SIZE_OF_LONG;
		return readDoubles(new double[n], 0, n, unitChain);
	}

	@Override
	public final int read(double[] dst, IUnitChain unitChain) {
		final int n = dst.length;
		readDoubles(dst, 0, n, unitChain);
		return SIZE_OF_LONG * n;
	}

	@Override
	public final int read(double[] dst, int offset, int length, IUnitChain unitChain) {
		final int n = offset + length;
		if ((offset | length | n | (dst.length - n)) < 0)
			throw new IndexOutOfBoundsException();

		readDoubles(dst, offset, length, unitChain);
		return SIZE_OF_LONG * length;
	}

	@Override
	public final void write(double[] src, IUnitChain unitChain) {
		writeDoubles(src, 0, src.length, unitChain);
	}

	@Override
	public final void write(double[] src, int offset, int length, IUnitChain unitChain) {
		final int n = offset + length;
		if ((offset | length | n | (src.length - n)) < 0)
			throw new IndexOutOfBoundsException();

		writeDoubles(src, offset, length, unitChain);
	}

	@Override
	public final double[] get(IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		IUnit unit = unitChain.currentUnit();
		int n = unitChain.remaining() + unit.position() - index;
		if (n < 1 || (n & (SIZE_OF_LONG - 1)) != 0)
			throw new IndexOutOfBoundsException();

		n >>= B_SIZE_OF_LONG;
		return getDoubles(new double[n], 0, n, unitChain, index);
	}

	@Override
	public final double[] get(IUnitChain unitChain, int index, int length) {
		if (index < 0 || length < 0 || (length & (SIZE_OF_LONG - 1)) != 0)
			throw new IndexOutOfBoundsException();

		final int n = length >> B_SIZE_OF_LONG;
		return getDoubles(new double[n], 0, n, unitChain, index);
	}

	@Override
	public final void get(double[] dst, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		getDoubles(dst, 0, dst.length, unitChain, index);
	}

	@Override
	public final void get(double[] dst, int offset, int length, IUnitChain unitChain, int index) {
		final int n = offset + length;
		if ((offset | length | n | (dst.length - n)) < 0 || index < 0)
			throw new IndexOutOfBoundsException();

		getDoubles(dst, offset, length, unitChain, index);
	}

	@Override
	public final void set(double[] src, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		setDoubles(src, 0, src.length, unitChain, index);
	}

	@Override
	public final void set(double[] src, int offset, int length, IUnitChain unitChain, int index) {
		final int n = offset + length;
		if ((offset | length | n | (src.length - n)) < 0 || index < 0)
			throw new IndexOutOfBoundsException();

		setDoubles(src, offset, length, unitChain, index);
	}

	@Override
	public final void prepend(double[] src, IUnitChain unitChain) {
		prependDoubles(src, 0, src.length, unitChain);
	}

	@Override
	public final void prepend(double[] src, int offset, int length, IUnitChain unitChain) {
		final int n = offset + length;
		if ((offset | length | n | (src.length - n)) < 0)
			throw new IndexOutOfBoundsException();
		prependDoubles(src, offset, length, unitChain);
	}

	abstract boolean isNative();

	abstract IUnit readDouble(double[] dst, int offset, IUnit unit, IUnitChain unitChain);

	abstract int getDouble(double[] dst, int offset, IUnit unit, int index, IUnitChain unitChain);

	abstract int setDouble(double l, IUnit unit, int index, IUnitChain unitChain);

	private double[] readDoubles(double[] dst, int offset, int length, IUnitChain unitChain) {
		if (length < 1)
			return dst;

		IUnit unit = unitChain.currentUnit();
		for (;;) {
			int position = unit.position();
			int index = unit.start() + position;
			int m = unit.remaining();
			int k = m >> B_SIZE_OF_LONG;
			if (k > 0) {
				if (k > length) {
					k = length;
					length = 0;
				} else
					length -= k;
				final int n = k * SIZE_OF_LONG;
				unit.position(position + n);
				if (isNative()) {
					unit.get(index, n, dst, offset);
					if (length < 1)
						break;
					offset += k;
				} else {
					int i = offset;
					offset += k;
					for (; i < offset; ++i, index += SIZE_OF_LONG)
						dst[i] = Double.longBitsToDouble(Long.reverseBytes(unit.getLong(index)));
					if (length < 1)
						break;
				}
			}
			k = m & (SIZE_OF_LONG - 1);
			if (k > 0) {
				unit = readDouble(dst, offset, unit, unitChain);
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

	private double[] getDoubles(double[] dst, int offset, int length, IUnitChain unitChain, int index) {
		if (length < 1)
			return dst;

		IUnit unit = unitChain.currentUnit();
		int m = unit.size() - index;
		index += unit.start();
		for (;;) {
			int k = m >> B_SIZE_OF_LONG;
			if (k > 0) {
				if (k > length) {
					k = length;
					length = 0;
				} else
					length -= k;
				final int n = k * SIZE_OF_LONG;
				if (isNative()) {
					unit.get(index, n, dst, offset);
					if (length < 1)
						break;
					offset += k;
					index += n;
				} else {
					int i = offset;
					offset += k;
					for (; i < offset; ++i, index += SIZE_OF_LONG)
						dst[i] = Double.longBitsToDouble(Long.reverseBytes(unit.getLong(index)));
					if (length < 1)
						break;
				}
			}
			k = m & (SIZE_OF_LONG - 1);
			if (k > 0) {
				index = getDouble(dst, offset, unit, index, unitChain);
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

	private void writeDoubles(double[] src, int offset, int length, IUnitChain unitChain) {
		if (length < 1)
			return;

		IUnit unit = Util.lastUnit(unitChain);
		for (;;) {
			int m = unit.available();
			int k = m >> B_SIZE_OF_LONG;
			if (k > 0) {
				if (k > length) {
					k = length;
					length = 0;
				} else
					length -= k;
				int size = unit.size();
				int index = unit.start() + size;
				unit.size(size + SIZE_OF_LONG * k);
				if (isNative()) {
					unit.set(index, src, offset, k);
					if (length < 1)
						break;
					offset += k;
				} else {
					for (int n = offset + k; offset < n; ++offset, index += SIZE_OF_LONG)
						unit.set(index, Long.reverseBytes(Double.doubleToRawLongBits(src[offset])));
					if (length < 1)
						break;
				}
			}
			unit = Util.appendNewUnit(unitChain);
		}
	}

	private void setDoubles(double[] src, int offset, int length, IUnitChain unitChain, int index) {
		if (length < 1)
			return;

		IUnit unit = unitChain.currentUnit();
		int m = unit.size() - index;
		index += unit.start();
		for (;;) {
			int k = m >> B_SIZE_OF_LONG;
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
					index += k * SIZE_OF_LONG;
				} else {
					for (int n = offset + k; offset < n; ++offset, index += SIZE_OF_LONG)
						unit.set(index, Long.reverseBytes(Double.doubleToRawLongBits(src[offset])));
					if (length < 1)
						break;
				}
			}
			k = m & (SIZE_OF_LONG - 1);
			if (k > 0) {
				index = setDouble(src[offset], unit, index, unitChain);
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

	private void prependDoubles(double[] src, int offset, int length, IUnitChain unitChain) {
		if (length < 1)
			return;
		IUnit unit = unitChain.firstUnit();
		for (;;) {
			int m = unit.start();
			int k = m >> B_SIZE_OF_LONG;
			if (k > 0) {
				if (k > length) {
					k = length;
					length = 0;
				} else
					length -= k;
				int len = k * SIZE_OF_LONG;
				int index = m - len;
				unit.start(index);
				unit.size(unit.size() + len);
				if (isNative()) {
					unit.set(index, src, offset, k);
					if (length < 1)
						break;
					offset += k;
				} else {
					for (int n = offset + k; offset < n; ++offset, index += SIZE_OF_LONG)
						unit.set(index, Long.reverseBytes(Double.doubleToRawLongBits(src[offset])));
					if (length < 1)
						break;
				}
			}
			unit = Util.prependNewUnit(unitChain);
		}
	}
}
