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

import org.jruyi.io.ILongCodec;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;

public final class BigEndianLongCodec implements ILongCodec {

	public static final ILongCodec INST = new BigEndianLongCodec();

	private BigEndianLongCodec() {
	}

	@Override
	public long read(IUnitChain unitChain) {
		long l = 0L;
		IUnit unit = unitChain.currentUnit();
		int start = unit.start();
		int position = start + unit.position();
		int size = unit.size();
		int end = start + size;
		for (int n = 0; n < 8;) {
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
		return l;
	}

	@Override
	public void write(long l, IUnitChain unitChain) {
		IUnit unit = Util.lastUnit(unitChain);
		int start = unit.start();
		int size = start + unit.size();
		int end = unit.capacity();
		for (int n = 56; n >= 0;) {
			if (size < end) {
				unit.set(size, (byte) (l >>> n));
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
	public long get(IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();
		long l = 0L;
		IUnit unit = unitChain.currentUnit();
		int size = unit.start();
		index += size;
		size += unit.size();
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
		for (int n = 56; n >= 0;) {
			if (index < size) {
				unit.set(index, (byte) (l >>> n));
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
	public void prepend(long l, IUnitChain unitChain) {
		IUnit unit = Util.firstUnit(unitChain);
		int start = unit.start();
		for (int n = 0; n < 8;) {
			if (start > 0) {
				unit.set(--start, (byte) l);
				l >>>= 8;
				++n;
			} else {
				unit.size(unit.size() + unit.start());
				unit.start(start);
				unit = Util.prependNewUnit(unitChain);
				start = unit.start();
			}
		}
		unit.size(unit.size() + unit.start() - start);
		unit.start(start);
	}
}
