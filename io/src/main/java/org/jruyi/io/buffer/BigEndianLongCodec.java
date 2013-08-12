/**
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

import java.nio.BufferUnderflowException;

import org.jruyi.io.ILongCodec;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;

public final class BigEndianLongCodec implements ILongCodec {

	public static final ILongCodec INST = new BigEndianLongCodec();

	private BigEndianLongCodec() {
	}

	@Override
	public long read(IUnitChain unitChain) {
		int length = 8;
		long l = 0;
		IUnit unit = unitChain.currentUnit();
		for (;;) {
			int size = unit.size();
			int position = unit.position();
			int count = size - position;
			if (count >= length) {
				l = getLongB(unit, position, l, length);
				unit.position(position + length);
				break;
			}
			l = getLongB(unit, position, l, count);
			unit.position(size);
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new BufferUnderflowException();
			length -= count;
		}
		return l;
	}

	@Override
	public void write(long l, IUnitChain unitChain) {
		int n = 8;
		IUnit unit = Util.lastUnit(unitChain);
		for (;;) {
			int size = unit.size();
			int length = unit.capacity() - unit.start() - size;
			if (length >= n) {
				setLongB(unit, size, l, n);
				unit.size(size + n);
				break;
			}
			l = setLongB(unit, size, l, length);
			n -= length;
			unit.size(size + length);
			unit = Util.appendNewUnit(unitChain);
		}
	}

	@Override
	public long get(IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();
		int length = 8;
		long l = 0;
		IUnit unit = unitChain.currentUnit();
		for (;;) {
			int count = unit.size() - index;
			if (count >= length) {
				l = getLongB(unit, index, l, length);
				break;
			}
			l = getLongB(unit, index, l, count);
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new IndexOutOfBoundsException();
			length -= count;
			index = 0;
		}
		return l;
	}

	@Override
	public void set(long l, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();
		int n = 8;
		IUnit unit = unitChain.currentUnit();
		for (;;) {
			int size = unit.size();
			int length = size - index;
			if (length >= n) {
				setLongB(unit, index, l, n);
				break;
			}
			l = setLongB(unit, index, l, length);
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new IndexOutOfBoundsException();
			n -= length;
			index = 0;
		}
	}

	@Override
	public void prepend(long l, IUnitChain unitChain) {
		int n = 8;
		IUnit unit = Util.firstUnit(unitChain);
		for (;;) {
			int length = unit.start();
			if (length >= n) {
				prependLongB(unit, l, n);
				break;
			}
			l = prependLongB(unit, l, length);
			n -= length;
			unit = Util.prependNewUnit(unitChain);
		}
	}

	/**
	 * Return a long value by left shifting the next {@code length} bytes
	 * starting at {@code position} into the given {@code l} sequentially. The
	 * {@code length} passed in must be not greater than {@code size()
	 * - position}.
	 * 
	 * @param unit
	 * @param position
	 *            the offset of the first byte to be left shifted
	 * @param l
	 *            the base long value to be left shifted into
	 * @param length
	 *            number of bytes to be left shifted into {@code l}
	 * @return the resultant long value
	 */
	private static long getLongB(IUnit unit, int position, long l,
			final int length) {
		position += unit.start();
		int end = position + length;
		for (; position < end; ++position)
			l = (l << 8) | (unit.byteAt(position) & 0xFF);
		return l;
	}

	private static long setLongB(IUnit unit, int position, long l,
			final int length) {
		position += unit.start();
		int end = position + length;
		for (; position < end; ++position) {
			unit.set(position, (byte) (l >>> 56));
			l <<= 8;
		}
		return l;
	}

	/**
	 * Return a {@code long} value by right shifting the given {@code l} by
	 * {@code (length * 8)} bits. The shifted {@code length} bytes are written
	 * to the head of this unit sequentially. The {@code length} passed in must
	 * be not greater than {@code available()}. So {@code available()} should be
	 * called to decide {@code length} before calling this method.
	 */
	private static long prependLongB(IUnit unit, long l, int length) {
		int start = unit.start();
		int index = start - length;
		while (start > index) {
			unit.set(--start, (byte) l);
			l >>= 8;
		}
		unit.start(start);
		unit.size(unit.size() + length);
		return l;
	}
}
