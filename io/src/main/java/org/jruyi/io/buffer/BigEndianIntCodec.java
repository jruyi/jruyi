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

import org.jruyi.io.IIntCodec;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;

public final class BigEndianIntCodec implements IIntCodec {

	public static final IIntCodec INST = new BigEndianIntCodec();

	private BigEndianIntCodec() {
	}

	@Override
	public int read(IUnitChain unitChain) {
		int length = 4;
		int i = 0;
		IUnit unit = unitChain.currentUnit();
		for (;;) {
			int size = unit.size();
			int position = unit.position();
			int count = size - position;
			if (count >= length) {
				i = getIntB(unit, position, i, length);
				unit.position(position + length);
				break;
			}
			i = getIntB(unit, position, i, count);
			unit.position(size);
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new BufferUnderflowException();
			length -= count;
		}
		return i;
	}

	@Override
	public void write(int i, IUnitChain unitChain) {
		int n = 4;
		IUnit unit = Util.lastUnit(unitChain);
		for (;;) {
			int size = unit.size();
			int capacity = unit.capacity();
			int length = capacity - size - unit.start();
			if (length >= n) {
				setIntB(unit, size, i, n);
				unit.size(size + n);
				break;
			}
			i = setIntB(unit, size, i, length);
			n -= length;
			unit.size(size + length);
			unit = Util.appendNewUnit(unitChain);
		}
	}

	@Override
	public int get(IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();
		int length = 4;
		int i = 0;
		IUnit unit = unitChain.currentUnit();
		for (;;) {
			int count = unit.size() - index;
			if (count >= length) {
				i = getIntB(unit, index, i, length);
				break;
			}
			i = getIntB(unit, index, i, count);
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new IndexOutOfBoundsException();
			length -= count;
			index = 0;
		}
		return i;
	}

	@Override
	public void set(int i, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();
		int n = 4;
		IUnit unit = unitChain.currentUnit();
		for (;;) {
			int size = unit.size();
			int length = size - index;
			if (length >= n) {
				setIntB(unit, index, i, n);
				break;
			}
			i = setIntB(unit, index, i, length);
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new IndexOutOfBoundsException();
			n -= length;
			index = 0;
		}
	}

	@Override
	public void prepend(int i, IUnitChain unitChain) {
		int n = 4;
		IUnit unit = Util.firstUnit(unitChain);
		for (;;) {
			int length = unit.start();
			if (length >= n) {
				prependIntB(unit, i, n);
				break;
			}

			i = prependIntB(unit, i, length);
			n -= length;
			unit = Util.prependNewUnit(unitChain);
		}
	}

	/**
	 * Return an int value by left shifting the next {@code length} bytes
	 * starting at {@code position} into the given {@code i} sequentially. The
	 * {@code length} passed in must be not greater than {@code size()
	 * - position}.
	 * 
	 * @param unit
	 * @param position
	 *            the offset of the first byte to be left shifted
	 * @param i
	 *            the base int value to be left shifted into
	 * @param length
	 *            number of bytes to be left shifted into {@code i}
	 * @return the resultant int value
	 */
	private static int getIntB(IUnit unit, int position, int i, int length) {
		position += unit.start();
		int end = position + length;
		for (; position < end; ++position)
			i = (i << 8) | (unit.byteAt(position) & 0xFF);

		return i;
	}

	private static int setIntB(IUnit unit, int position, int i, int length) {
		position += unit.start();
		int end = position + length;
		for (; position < end; ++position) {
			unit.set(position, (byte) (i >>> 24));
			i <<= 8;
		}

		return i;
	}

	/**
	 * Return an {@code int} value by right shifting the given {@code i} by
	 * {@code (length * 8)} bits. The shifted {@code length} bytes are written
	 * to the head of this unit sequentially. The {@code length} passed in must
	 * be not greater than {@code available()}. So {@code available()} should be
	 * called to decide {@code length} before calling this method.
	 */
	private static int prependIntB(IUnit unit, int i, int length) {
		int start = unit.start();
		int index = start - length;
		while (start > index) {
			unit.set(--start, (byte) i);
			i >>= 8;
		}
		unit.start(start);
		unit.size(unit.size() + length);
		return i;
	}
}
