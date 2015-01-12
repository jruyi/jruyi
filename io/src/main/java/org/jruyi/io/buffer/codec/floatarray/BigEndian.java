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

package org.jruyi.io.buffer.codec.floatarray;

import static org.jruyi.io.buffer.Helper.BE_NATIVE;
import static org.jruyi.io.buffer.Helper.SIZE_OF_INT;

import java.nio.BufferUnderflowException;

import org.jruyi.io.ICodec;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;

public final class BigEndian extends AbstractCodec {

	public static final ICodec<float[]> INST = new BigEndian();

	private BigEndian() {
	}

	@Override
	boolean isNative() {
		return BE_NATIVE;
	}

	@Override
	IUnit readFloat(float[] dst, int offset, IUnit unit, IUnitChain unitChain) {
		int start = unit.start();
		int size = unit.size();
		int index = start + unit.position();
		int end = start + size;
		int n = 0;
		for (int i = 0; i < SIZE_OF_INT;) {
			if (index < end) {
				n = (n << 8) | (unit.byteAt(index) & 0xFF);
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
		dst[offset] = Float.intBitsToFloat(n);
		return unit;
	}

	@Override
	int getFloat(float[] dst, int offset, IUnit unit, int index, IUnitChain unitChain) {
		int size = unit.start() + unit.size();
		int n = 0;
		for (int i = 0; i < SIZE_OF_INT;) {
			if (index < size) {
				n = (n << 8) | (unit.byteAt(index) & 0xFF);
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
		dst[offset] = Float.intBitsToFloat(n);
		return index;
	}

	@Override
	int setFloat(float f, IUnit unit, int index, IUnitChain unitChain) {
		int i = Float.floatToRawIntBits(f);
		int size = unit.start() + unit.size();
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
		return index;
	}
}
