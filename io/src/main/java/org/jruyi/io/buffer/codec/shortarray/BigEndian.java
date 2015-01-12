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

package org.jruyi.io.buffer.codec.shortarray;

import static org.jruyi.io.buffer.Helper.BE_NATIVE;
import static org.jruyi.io.buffer.Helper.SIZE_OF_SHORT;

import java.nio.BufferUnderflowException;

import org.jruyi.io.ICodec;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;

public final class BigEndian extends AbstractCodec {

	public static final ICodec<short[]> INST = new BigEndian();

	private BigEndian() {
	}

	@Override
	boolean isNative() {
		return BE_NATIVE;
	}

	@Override
	IUnit readShort(short[] dst, int offset, IUnit unit, IUnitChain unitChain) {
		int start = unit.start();
		int size = unit.size();
		int index = start + unit.position();
		int end = start + size;
		int s = 0;
		for (int i = 0; i < SIZE_OF_SHORT;) {
			if (index < end) {
				s = (s << 8) | (unit.byteAt(index) & 0xFF);
				++index;
				++i;
			} else {
				unit.position(size);
				unit = unitChain.nextUnit();
				if (unit == null)
					throw new BufferUnderflowException();
				start = unit.start();
				index = start + unit.position();
				size = unit.size();
				end = start + size;
			}
		}
		unit.position(index - start);
		dst[offset] = (short) s;
		return unit;
	}

	@Override
	int getShort(short[] dst, int offset, IUnit unit, int index, IUnitChain unitChain) {
		int size = unit.start() + unit.size();
		int s = 0;
		for (int i = 0; i < SIZE_OF_SHORT;) {
			if (index < size) {
				s = (s << 8) | (unit.byteAt(index) & 0xFF);
				++index;
				++i;
			} else {
				unit = unitChain.nextUnit();
				if (unit == null)
					throw new IndexOutOfBoundsException();
				index = unit.start();
				size = index + unit.size();
			}
		}
		dst[offset] = (short) s;
		return index;
	}

	@Override
	int setShort(short l, IUnit unit, int index, IUnitChain unitChain) {
		int size = unit.start() + unit.size();
		for (int n = 8; n >= 0;) {
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
		return index;
	}
}
