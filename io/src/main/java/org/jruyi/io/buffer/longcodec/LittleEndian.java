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

package org.jruyi.io.buffer.longcodec;

import static org.jruyi.io.buffer.Helper.BE_NATIVE;
import static org.jruyi.io.buffer.Helper.SIZE_OF_LONG;

import java.nio.BufferUnderflowException;

import org.jruyi.io.ILongCodec;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;
import org.jruyi.io.buffer.Util;

public final class LittleEndian implements ILongCodec {

	public static final LittleEndian INST = new LittleEndian();

	private LittleEndian() {
	}

	@Override
	public long read(IUnitChain unitChain) {
		IUnit unit = unitChain.currentUnit();
		int start = unit.start();
		int position = start + unit.position();
		int size = unit.size();
		int end = start + size;
		int n = position + SIZE_OF_LONG;
		if (n <= end) {
			long l = unit.getLong(position);
			if (BE_NATIVE)
				l = Long.reverseBytes(l);
			unit.position(n - start);
			return l;
		}

		long l = 0L;
		for (n = 0; n < SIZE_OF_LONG;) {
			if (position < end) {
				l = (l >>> 8) | (((long) unit.byteAt(position)) << 56);
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
		return l;
	}

	@Override
	public void write(long l, IUnitChain unitChain) {
		IUnit unit = Util.lastUnit(unitChain);
		int start = unit.start();
		int size = start + unit.size();
		int end = unit.capacity();
		int n = size + SIZE_OF_LONG;
		if (BE_NATIVE)
			l = Long.reverseBytes(l);
		if (n <= end) {
			unit.set(size, l);
			unit.size(n - start);
		} else {
			unit = Util.appendNewUnit(unitChain);
			unit.set(unit.start(), l);
			unit.size(SIZE_OF_LONG);
		}
	}

	@Override
	public long get(IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();
		IUnit unit = unitChain.currentUnit();
		int size = unit.start();
		index += size;
		size += unit.size();
		if (index + SIZE_OF_LONG <= size) {
			long l = unit.getLong(index);
			if (BE_NATIVE)
				l = Long.reverseBytes(l);
			return l;
		}

		long l = 0;
		for (int n = 0; n < SIZE_OF_LONG;) {
			if (index < size) {
				l = (l >>> 8) | (((long) unit.byteAt(index)) << 56);
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
		return l;
	}

	@Override
	public void set(long l, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();
		IUnit unit = unitChain.currentUnit();
		int size = unit.start();
		index += size;
		size += unit.size();
		if (index + SIZE_OF_LONG <= size) {
			if (BE_NATIVE)
				l = Long.reverseBytes(l);
			unit.set(index, l);
			return;
		}

		for (int n = 0; n <= 56;) {
			if (index < size) {
				unit.set(index, (byte) (l >> n));
				++index;
				n += 8;
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
	public void prepend(long l, IUnitChain unitChain) {
		IUnit unit = Util.firstUnit(unitChain);
		int start = unit.start();
		if (BE_NATIVE)
			l = Long.reverseBytes(l);
		if (start < SIZE_OF_LONG) {
			unit = Util.prependNewUnit(unitChain);
			start = unit.start();
		}
		start -= SIZE_OF_LONG;
		unit.set(start, l);
		unit.start(start);
		unit.size(unit.size() + SIZE_OF_LONG);
	}
}
