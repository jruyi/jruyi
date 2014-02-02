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
		int s = 0;
		IUnit unit = unitChain.currentUnit();
		int start = unit.start();
		int position = start + unit.position();
		int size = unit.size();
		int end = start + size;
		for (int n = 8; n >= 0;) {
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
		int size = unit.size();
		unit.set(start + size, (byte) (s >> 8));
		if (++size >= unit.capacity() - start) {
			unit.size(size);
			unit = Util.appendNewUnit(unitChain);
			start = unit.start();
			size = 0;
		}

		unit.set(start + size, (byte) s);
		unit.size(++size);
	}

	@Override
	public short get(IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();
		int s = 0;
		IUnit unit = unitChain.currentUnit();
		int size = unit.start();
		index += size;
		size += unit.size();
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
		while (index >= unit.size()) {
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new IndexOutOfBoundsException();
			index = 0;
		}
		int start = unit.start();
		unit.set(start + index++, (byte) (s >> 8));
		while (index >= unit.size()) {
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new IndexOutOfBoundsException();
			start = unit.start();
			index = 0;
		}
		unit.set(start + index, (byte) s);
	}

	@Override
	public void prepend(short s, IUnitChain unitChain) {
		IUnit unit = Util.firstUnit(unitChain);
		int start = unit.start();
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
