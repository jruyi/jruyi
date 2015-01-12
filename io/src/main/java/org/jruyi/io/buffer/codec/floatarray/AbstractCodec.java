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

package org.jruyi.io.buffer.codec.floatarray;

import static org.jruyi.io.buffer.Helper.B_SIZE_OF_INT;
import static org.jruyi.io.buffer.Helper.SIZE_OF_INT;

import java.nio.BufferUnderflowException;

import org.jruyi.io.ICodec;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;
import org.jruyi.io.buffer.Util;

public abstract class AbstractCodec implements ICodec<float[]> {

	@Override
	public final float[] read(IUnitChain unitChain) {
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
		return readFloats(new float[n], 0, n, unitChain);
	}

	@Override
	public final float[] read(IUnitChain unitChain, int length) {
		if (length < 0 || (length & (SIZE_OF_INT - 1)) != 0)
			throw new IllegalArgumentException();

		final int n = length >> B_SIZE_OF_INT;
		return readFloats(new float[n], 0, n, unitChain);
	}

	@Override
	public final int read(float[] dst, IUnitChain unitChain) {
		final int n = dst.length;
		readFloats(dst, 0, n, unitChain);
		return SIZE_OF_INT * n;
	}

	@Override
	public final int read(float[] dst, int offset, int length, IUnitChain unitChain) {
		final int n = offset + length;
		if ((offset | length | n | (dst.length - n)) < 0)
			throw new IndexOutOfBoundsException();

		readFloats(dst, offset, length, unitChain);
		return SIZE_OF_INT * length;
	}

	@Override
	public final void write(float[] src, IUnitChain unitChain) {
		writeFloats(src, 0, src.length, unitChain);
	}

	@Override
	public final void write(float[] src, int offset, int length, IUnitChain unitChain) {
		final int n = offset + length;
		if ((offset | length | n | (src.length - n)) < 0)
			throw new IndexOutOfBoundsException();

		writeFloats(src, offset, length, unitChain);
	}

	@Override
	public final float[] get(IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		IUnit unit = unitChain.currentUnit();
		int n = unitChain.remaining() + unit.position() - index;
		if (n < 1 || (n & (SIZE_OF_INT - 1)) != 0)
			throw new IndexOutOfBoundsException();

		n >>= B_SIZE_OF_INT;
		return getFloats(new float[n], 0, n, unitChain, index);
	}

	@Override
	public final float[] get(IUnitChain unitChain, int index, int length) {
		if (index < 0 || length < 0 || (length & (SIZE_OF_INT - 1)) != 0)
			throw new IndexOutOfBoundsException();

		final int n = length >> B_SIZE_OF_INT;
		return getFloats(new float[n], 0, n, unitChain, index);
	}

	@Override
	public final void get(float[] dst, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		getFloats(dst, 0, dst.length, unitChain, index);
	}

	@Override
	public final void get(float[] dst, int offset, int length, IUnitChain unitChain, int index) {
		final int n = offset + length;
		if ((offset | length | n | (dst.length - n)) < 0 || index < 0)
			throw new IndexOutOfBoundsException();

		getFloats(dst, offset, length, unitChain, index);
	}

	@Override
	public final void set(float[] src, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		setFloats(src, 0, src.length, unitChain, index);
	}

	@Override
	public final void set(float[] src, int offset, int length, IUnitChain unitChain, int index) {
		final int n = offset + length;
		if ((offset | length | n | (src.length - n)) < 0 || index < 0)
			throw new IndexOutOfBoundsException();

		setFloats(src, offset, length, unitChain, index);
	}

	@Override
	public final void prepend(float[] src, IUnitChain unitChain) {
		prependFloats(src, 0, src.length, unitChain);
	}

	@Override
	public final void prepend(float[] src, int offset, int length, IUnitChain unitChain) {
		final int n = offset + length;
		if ((offset | length | n | (src.length - n)) < 0)
			throw new IndexOutOfBoundsException();
		prependFloats(src, offset, length, unitChain);
	}

	abstract boolean isNative();

	abstract IUnit readFloat(float[] dst, int offset, IUnit unit, IUnitChain unitChain);

	abstract int getFloat(float[] dst, int offset, IUnit unit, int index, IUnitChain unitChain);

	abstract int setFloat(float l, IUnit unit, int index, IUnitChain unitChain);

	private float[] readFloats(float[] dst, int offset, int length, IUnitChain unitChain) {
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
				unit.position(position + n);
				if (isNative()) {
					unit.get(index, n, dst, offset);
					if (length < 1)
						break;
					offset += k;
				} else {
					int i = offset;
					offset += k;
					for (; i < offset; ++i, index += SIZE_OF_INT)
						dst[i] = Float.intBitsToFloat(Integer.reverseBytes(unit.getInt(index)));
					if (length < 1)
						break;
				}
			}
			k = m & (SIZE_OF_INT - 1);
			if (k > 0) {
				unit = readFloat(dst, offset, unit, unitChain);
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

	private float[] getFloats(float[] dst, int offset, int length, IUnitChain unitChain, int index) {
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
				if (isNative()) {
					unit.get(index, n, dst, offset);
					if (length < 1)
						break;
					offset += k;
					index += n;
				} else {
					int i = offset;
					offset += k;
					for (; i < offset; ++i, index += SIZE_OF_INT)
						dst[i] = Float.intBitsToFloat(Integer.reverseBytes(unit.getInt(index)));
					if (length < 1)
						break;
				}
			}
			k = m & (SIZE_OF_INT - 1);
			if (k > 0) {
				index = getFloat(dst, offset, unit, index, unitChain);
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

	private void writeFloats(float[] src, int offset, int length, IUnitChain unitChain) {
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
						unit.set(index, Integer.reverseBytes(Float.floatToRawIntBits(src[offset])));
					if (length < 1)
						break;
				}
			}
			unit = Util.appendNewUnit(unitChain);
		}
	}

	private void setFloats(float[] src, int offset, int length, IUnitChain unitChain, int index) {
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
						unit.set(index, Integer.reverseBytes(Float.floatToRawIntBits(src[offset])));
					if (length < 1)
						break;
				}
			}
			k = m & (SIZE_OF_INT - 1);
			if (k > 0) {
				index = setFloat(src[offset], unit, index, unitChain);
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

	private void prependFloats(float[] src, int offset, int length, IUnitChain unitChain) {
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
						unit.set(index, Integer.reverseBytes(Float.floatToRawIntBits(src[offset])));
					if (length < 1)
						break;
				}
			}
			unit = Util.prependNewUnit(unitChain);
		}
	}
}
