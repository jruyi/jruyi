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
package org.jruyi.io.buffer.doublecodec;

import static org.jruyi.io.buffer.Helper.BE_NATIVE;
import static org.jruyi.io.buffer.Helper.SIZE_OF_LONG;

import java.nio.BufferUnderflowException;

import org.jruyi.io.IDoubleCodec;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;
import org.jruyi.io.buffer.Util;

public final class BigEndian implements IDoubleCodec {

	public static final IDoubleCodec INST = new BigEndian();

	private BigEndian() {
	}

	@Override
	public double read(IUnitChain unitChain) {
		IUnit unit = unitChain.currentUnit();
		int start = unit.start();
		int position = start + unit.position();
		int size = unit.size();
		int end = start + size;
		int n = position + SIZE_OF_LONG;
		if (n <= end) {
			unit.position(n - start);
			if (BE_NATIVE)
				return unit.getDouble(position);
			return Double.longBitsToDouble(Long.reverseBytes(unit.getLong(position)));
		}

		long l = 0;
		for (n = 0; n < 8;) {
			if (position < end) {
				l = (l << 8) | (unit.byteAt(position) & 0xFF);
				++position;
				++n;
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
		return Double.longBitsToDouble(l);
	}

	@Override
	public void write(double d, IUnitChain unitChain) {
		IUnit unit = Util.lastUnit(unitChain);
		int start = unit.start();
		int size = start + unit.size();
		int end = unit.capacity();
		int n = size + SIZE_OF_LONG;
		if (n <= end) {
			unit.size(n - start);
		} else {
			unit = Util.appendNewUnit(unitChain);
			unit.size(SIZE_OF_LONG);
			size = unit.start();
		}
		if (BE_NATIVE)
			unit.set(size, d);
		else
			unit.set(size, Long.reverseBytes(Double.doubleToRawLongBits(d)));
	}

	@Override
	public double get(IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();
		IUnit unit = unitChain.currentUnit();
		int size = unit.start();
		index += size;
		size += unit.size();
		if (index + SIZE_OF_LONG <= size) {
			if (BE_NATIVE)
				return unit.getDouble(index);
			return Double.longBitsToDouble(Long.reverseBytes(unit.getLong(index)));
		}

		long l = 0L;
		for (int n = 0; n < 8;) {
			if (index < size) {
				l = (l << 8) | (unit.byteAt(index) & 0xFF);
				++index;
				++n;
			} else {
				unit = unitChain.nextUnit();
				if (unit == null)
					throw new IndexOutOfBoundsException();
				index = unit.start();
				size = index + unit.size();
			}
		}
		return Double.longBitsToDouble(l);
	}

	@Override
	public void set(double d, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();
		IUnit unit = unitChain.currentUnit();
		int size = unit.start();
		index += size;
		size += unit.size();
		if (index + SIZE_OF_LONG <= size) {
			if (BE_NATIVE)
				unit.set(index, d);
			else
				unit.set(index, Long.reverseBytes(Double.doubleToRawLongBits(d)));
			return;
		}

		long l = Double.doubleToRawLongBits(d);
		for (int n = 56; n >= 0;) {
			if (index < size) {
				unit.set(index, (byte) (l >> n));
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
	public void prepend(double d, IUnitChain unitChain) {
		IUnit unit = Util.firstUnit(unitChain);
		int start = unit.start();
		if (start < SIZE_OF_LONG) {
			unit = Util.prependNewUnit(unitChain);
			start = unit.start();
		}
		start -= SIZE_OF_LONG;
		if (BE_NATIVE)
			unit.set(start, d);
		else
			unit.set(start, Long.reverseBytes(Double.doubleToRawLongBits(d)));
		unit.start(start);
		unit.size(unit.size() + SIZE_OF_LONG);
	}
}
