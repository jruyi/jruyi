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

import org.jruyi.io.IIntCodec;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;

public final class VarintIntCodec implements IIntCodec {

	public static final IIntCodec INST = new VarintIntCodec();

	private VarintIntCodec() {
	}

	@Override
	public int get(IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		int i = 0;
		int shift = 0;
		IUnit unit = unitChain.currentUnit();
		int start = unit.start();
		index += start;
		int end = start + unit.size();
		for (;;) {
			if (index < end) {
				final int b = unit.byteAt(index);
				i |= ((b & 0x7F) << shift);
				if ((b & 0x80) == 0)
					break;
				shift += 7;
				++index;
			} else {
				unit = unitChain.nextUnit();
				if (unit == null)
					throw new IndexOutOfBoundsException();
				index = unit.start();
				end = index + unit.size();
			}
		}
		return i;
	}

	@Override
	public void prepend(int i, IUnitChain unitChain) {
		IUnit unit = Util.firstUnit(unitChain);
		int shift = 28;
		int start = unit.start();
		int n;
		while ((n = i >>> shift) == 0) {
			shift -= 7;
			if (shift == 0)
				break;
		}
		unit.set(--start, (byte) n);

		while (shift > 0) {
			if (start > 0) {
				shift -= 7;
				n = ((i >>> shift) & 0x7F) | 0x80;
				unit.set(--start, (byte) n);
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

	@Override
	public int read(IUnitChain unitChain) {
		int i = 0;
		int shift = 0;
		IUnit unit = unitChain.currentUnit();
		int start = unit.start();
		int position = start + unit.position();
		int end = start + unit.size();
		for (;;) {
			if (position < end) {
				final int b = unit.byteAt(position++);
				i |= ((b & 0x7F) << shift);
				if ((b & 0x80) == 0)
					break;
				shift += 7;
			} else {
				unit.position(unit.size());
				unit = unitChain.nextUnit();
				if (unit == null)
					throw new BufferUnderflowException();
				start = unit.start();
				position = start + unit.position();
				end = start + unit.size();
			}
		}

		unit.position(position - start);
		return i;
	}

	@Override
	public void set(int i, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();
		IUnit unit = unitChain.currentUnit();
		int start = unit.start();
		int size = start + unit.size();
		index += start;
		for (;;) {
			if (index < size) {
				if ((i & ~0x7F) == 0) {
					unit.set(index, (byte) i);
					break;
				} else {
					unit.set(index, (byte) ((i & 0x7F) | 0x80));
					i >>>= 7;
					++index;
				}
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
	public void write(int i, IUnitChain unitChain) {
		IUnit unit = Util.lastUnit(unitChain);
		int start = unit.start();
		int end = unit.capacity();
		int size = start + unit.size();
		for (;;) {
			if (size < end) {
				if ((i & ~0x7F) == 0) {
					unit.set(size, (byte) i);
					unit.size(++size - start);
					break;
				} else {
					unit.set(size, (byte) ((i & 0x7F) | 0x80));
					i >>>= 7;
					++size;
				}
			} else {
				unit.size(size - start);
				unit = Util.appendNewUnit(unitChain);
				start = unit.start();
				end = unit.capacity();
				size = start + unit.size();
			}
		}
	}
}
