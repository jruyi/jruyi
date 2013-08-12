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

public final class LittleEndianLongCodec implements ILongCodec {

	public static final LittleEndianLongCodec INST = new LittleEndianLongCodec();

	private LittleEndianLongCodec() {
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
				l = getLongL(unit, position, l, length);
				unit.position(position + length);
				break;
			}
			l = getLongL(unit, position, l, count);
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
				setLongL(unit, size, l, n);
				unit.size(size + n);
				break;
			}
			l = setLongL(unit, size, l, length);
			n -= length;
			unit.size(size + length);
			unit = Util.appendNewUnit(unitChain);
		}
	}

	@Override
	public long get(IUnitChain unitChain, int index) {
		int length = 8;
		long l = 0;
		IUnit unit = unitChain.currentUnit();
		for (;;) {
			int count = unit.size() - index;
			if (count >= length) {
				l = getLongL(unit, index, l, length);
				break;
			}
			l = getLongL(unit, index, l, count);
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
		int n = 8;
		IUnit unit = unitChain.currentUnit();
		for (;;) {
			int size = unit.size();
			int length = size - index;
			if (length >= n) {
				setLongL(unit, index, l, n);
				break;
			}
			l = setLongL(unit, index, l, length);
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
				prependLongL(unit, l, n);
				break;
			}

			l = prependLongL(unit, l, length);
			n -= length;
			unit = Util.prependNewUnit(unitChain);
		}
	}

	/**
	 * Return a long value by right shifting the next {@code length} bytes
	 * starting at {@code position} into the given {@code l} sequentially. The
	 * {@code length} passed in must be not greater than {@code size()
	 * - position}.
	 * 
	 * @param position
	 *            the offset of the first byte to be right shifted
	 * @param l
	 *            the base long value to be right shifted into
	 * @param length
	 *            number of bytes to be right shifted into {@code l}
	 * @return the resultant long value
	 */
	private static long getLongL(IUnit unit, int position, long l,
			final int length) {
		position += unit.start();
		int end = position + length;
		for (; position < end; ++position)
			l = (l >>> 8) | (((long) (unit.byteAt(position) & 0xFF)) << 56);

		return l;
	}

	private static long setLongL(IUnit unit, int position, long l,
			final int length) {
		position += unit.start();
		int end = position + length;
		for (; position < end; ++position) {
			unit.set(position, (byte) l);
			l >>>= 8;
		}

		return l;
	}

	/**
	 * Return a {@code long} value by left shifting the given {@code l} by
	 * {@code (length * 8)} bits. The shifted {@code length} bytes are written
	 * to the head of this unit sequentially. The {@code length} passed in must
	 * be not greater than {@code available()}. So {@code available()} should be
	 * called to decide {@code length} before calling this method.
	 */
	private static long prependLongL(IUnit unit, long l, int length) {
		int start = unit.start();
		int index = start - length;
		while (start > index) {
			unit.set(--start, (byte) (l >> 56));
			l <<= 8;
		}
		unit.start(start);
		unit.size(unit.size() + length);
		return l;
	}
}
