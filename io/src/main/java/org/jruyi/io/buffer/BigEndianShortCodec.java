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

import static org.jruyi.io.buffer.Helper.BE_NATIVE;
import static org.jruyi.io.buffer.Helper.SIZE_OF_SHORT;

import java.nio.BufferUnderflowException;

import org.jruyi.io.IShortCodec;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;

public final class BigEndianShortCodec implements IShortCodec {

	public static final IShortCodec INST = new BigEndianShortCodec();

	private BigEndianShortCodec() {
	}

	@Override
	public short read(IUnitChain unitChain) {
		IUnit unit = unitChain.currentUnit();
		int start = unit.start();
		int position = start + unit.position();
		int size = unit.size();
		int end = start + size;
		int n = position + SIZE_OF_SHORT;
		if (BE_NATIVE && n <= end) {
			final short s = unit.getShort(position);
			unit.position(n - start);
			return s;
		}

		int s = 0;
		for (n = 8; n >= 0;) {
			if (position < end) {
				s |= ((unit.byteAt(position) & 0xFF) << n);
				++position;
				n -= 8;
			} else {
				unit.position(size);
				unit = unitChain.nextUnit();
				if (unit == null)
					throw new BufferUnderflowException();
				start = unit.start();
				position = start + unit.position();
				size = unit.size();
				end = start + size;
			}
		}
		unit.position(position - start);
		return (short) s;
	}

	@Override
	public void write(short s, IUnitChain unitChain) {
		IUnit unit = Util.lastUnit(unitChain);
		int start = unit.start();
		int size = start + unit.size();
		int end = unit.capacity();
		int n = size + SIZE_OF_SHORT;
		if (BE_NATIVE) {
			if (n <= end) {
				unit.set(size, s);
				unit.size(n - start);
			} else {
				unit = Util.appendNewUnit(unitChain);
				unit.set(unit.start(), s);
				unit.size(SIZE_OF_SHORT);
			}
			return;
		}

		for (n = 8; n >= 0;) {
			if (size < end) {
				unit.set(size, (byte) (s >> n));
				++size;
				n -= 8;
			} else {
				unit.size(size - start);
				unit = Util.appendNewUnit(unitChain);
				start = unit.start();
				size = start + unit.size();
				end = unit.capacity();
			}
		}
		unit.size(size - start);
	}

	@Override
	public short get(IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();
		IUnit unit = unitChain.currentUnit();
		int size = unit.start();
		index += size;
		size += unit.size();
		if (BE_NATIVE && index + SIZE_OF_SHORT <= size)
			return unit.getShort(index);

		int s = 0;
		for (int n = 8; n >= 0;) {
			if (index < size) {
				s |= ((unit.byteAt(index) & 0xFF) << n);
				++index;
				n -= 8;
			} else {
				unit = unitChain.nextUnit();
				if (unit == null)
					throw new IndexOutOfBoundsException();
				index = unit.start();
				size = index + unit.size();
			}
		}
		return (short) s;
	}

	@Override
	public void set(short s, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();
		IUnit unit = unitChain.currentUnit();
		int size = unit.start();
		index += size;
		size += unit.size();
		if (BE_NATIVE && index + SIZE_OF_SHORT <= size) {
			unit.set(index, s);
			return;
		}

		for (int n = 8; n >= 0;) {
			if (index < size) {
				unit.set(index, (byte) (s >> n));
				++index;
				n -= 8;
			} else {
				unit = unitChain.nextUnit();
				if (unit == null)
					throw new IndexOutOfBoundsException();
				index = unit.start();
				size = index + unit.size();
			}
		}
	}

	@Override
	public void prepend(short s, IUnitChain unitChain) {
		IUnit unit = Util.firstUnit(unitChain);
		int start = unit.start();
		if (BE_NATIVE) {
			if (start < SIZE_OF_SHORT) {
				unit = Util.prependNewUnit(unitChain);
				start = unit.start();
			}
			start -= SIZE_OF_SHORT;
			unit.set(start, s);
			unit.start(start);
			unit.size(unit.size() + SIZE_OF_SHORT);
			return;
		}

		unit.set(--start, (byte) s);
		if (start < 1) {
			unit.start(start);
			unit.size(unit.size() + 1);
			unit = Util.prependNewUnit(unitChain);
			start = unit.start();
		}
		unit.set(--start, (byte) (s >> 8));
		int diff = unit.start() - start;
		unit.start(start);
		unit.size(unit.size() + diff);
	}
}
