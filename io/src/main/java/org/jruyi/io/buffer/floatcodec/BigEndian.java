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

package org.jruyi.io.buffer.floatcodec;

import static org.jruyi.io.buffer.Helper.BE_NATIVE;
import static org.jruyi.io.buffer.Helper.SIZE_OF_INT;

import java.nio.BufferUnderflowException;

import org.jruyi.io.IFloatCodec;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;
import org.jruyi.io.buffer.Util;

public final class BigEndian implements IFloatCodec {

	public static final IFloatCodec INST = new BigEndian();

	private BigEndian() {
	}

	@Override
	public float read(IUnitChain unitChain) {
		IUnit unit = unitChain.currentUnit();
		int start = unit.start();
		int position = start + unit.position();
		int size = unit.size();
		int end = start + size;
		int n = position + SIZE_OF_INT;
		if (n <= end) {
			unit.position(n - start);
			if (BE_NATIVE)
				return unit.getFloat(position);
			return Float.intBitsToFloat(Integer.reverseBytes(unit.getInt(position)));
		}

		int i = 0;
		for (n = 0; n < SIZE_OF_INT;) {
			if (position < end) {
				i = (i << 8) | (unit.byteAt(position) & 0xFF);
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
		return Float.intBitsToFloat(i);
	}

	@Override
	public void write(float f, IUnitChain unitChain) {
		IUnit unit = Util.lastUnit(unitChain);
		int start = unit.start();
		int size = start + unit.size();
		int end = unit.capacity();
		int n = size + SIZE_OF_INT;
		if (n <= end) {
			unit.size(n - start);
		} else {
			unit = Util.appendNewUnit(unitChain);
			unit.size(SIZE_OF_INT);
			size = unit.start();
		}
		if (BE_NATIVE)
			unit.set(size, f);
		else
			unit.set(size, Integer.reverseBytes(Float.floatToRawIntBits(f)));
	}

	@Override
	public float get(IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();
		IUnit unit = unitChain.currentUnit();
		int size = unit.start();
		index += size;
		size += unit.size();
		if (index + SIZE_OF_INT <= size) {
			if (BE_NATIVE)
				return unit.getFloat(index);
			return Float.intBitsToFloat(Integer.reverseBytes(unit.getInt(index)));
		}

		int i = 0;
		for (int n = 0; n < SIZE_OF_INT;) {
			if (index < size) {
				i = (i << 8) | (unit.byteAt(index) & 0xFF);
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
		return Float.intBitsToFloat(i);
	}

	@Override
	public void set(float f, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();
		IUnit unit = unitChain.currentUnit();
		int size = unit.start();
		index += size;
		size += unit.size();
		if (index + SIZE_OF_INT <= size) {
			if (BE_NATIVE)
				unit.set(index, f);
			else
				unit.set(index, Integer.reverseBytes(Float.floatToRawIntBits(f)));
			return;
		}

		int i = Float.floatToRawIntBits(f);
		for (int n = 24; n >= 0;) {
			if (index < size) {
				unit.set(index, (byte) (i >> n));
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
	public void prepend(float f, IUnitChain unitChain) {
		IUnit unit = Util.firstUnit(unitChain);
		int start = unit.start();
		if (start < SIZE_OF_INT) {
			unit = Util.prependNewUnit(unitChain);
			start = unit.start();
		}
		start -= SIZE_OF_INT;
		if (BE_NATIVE)
			unit.set(start, f);
		else
			unit.set(start, Integer.reverseBytes(Float.floatToRawIntBits(f)));
		unit.start(start);
		unit.size(unit.size() + SIZE_OF_INT);
	}
}
