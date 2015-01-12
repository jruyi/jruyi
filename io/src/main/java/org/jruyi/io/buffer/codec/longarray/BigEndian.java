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

package org.jruyi.io.buffer.codec.longarray;

import static org.jruyi.io.buffer.Helper.BE_NATIVE;
import static org.jruyi.io.buffer.Helper.SIZE_OF_LONG;

import java.nio.BufferUnderflowException;

import org.jruyi.io.ICodec;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;

public final class BigEndian extends AbstractCodec {

	public static final ICodec<long[]> INST = new BigEndian();

	private BigEndian() {
	}

	@Override
	boolean isNative() {
		return BE_NATIVE;
	}

	@Override
	IUnit readLong(long[] dst, int offset, IUnit unit, IUnitChain unitChain) {
		int start = unit.start();
		int size = unit.size();
		int index = start + unit.position();
		int end = start + size;
		long l = 0;
		for (int i = 0; i < SIZE_OF_LONG;) {
			if (index < end) {
				l = (l << 8) | (unit.byteAt(index) & 0xFF);
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
		dst[offset] = l;
		return unit;
	}

	@Override
	int getLong(long[] dst, int offset, IUnit unit, int index, IUnitChain unitChain) {
		int size = unit.start() + unit.size();
		long l = 0;
		for (int i = 0; i < SIZE_OF_LONG;) {
			if (index < size) {
				l = (l << 8) | (unit.byteAt(index) & 0xFF);
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
		dst[offset] = l;
		return index;
	}

	@Override
	int setLong(long l, IUnit unit, int index, IUnitChain unitChain) {
		int size = unit.start() + unit.size();
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
		return index;
	}
}
