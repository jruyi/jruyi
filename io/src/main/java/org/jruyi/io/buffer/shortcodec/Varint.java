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
package org.jruyi.io.buffer.shortcodec;

import org.jruyi.io.IShortCodec;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;
import org.jruyi.io.buffer.Util;

public final class Varint implements IShortCodec {

	public static final IShortCodec INST = new Varint();

	private Varint() {
	}

	@Override
	public short read(IUnitChain unitChain) {
		return (short) org.jruyi.io.buffer.intcodec.Varint.INST.read(unitChain);
	}

	@Override
	public void write(short s, IUnitChain unitChain) {
		org.jruyi.io.buffer.intcodec.Varint.INST.write(s & 0xFFFF, unitChain);
	}

	@Override
	public short get(IUnitChain unitChain, int index) {
		return (short) org.jruyi.io.buffer.intcodec.Varint.INST.get(unitChain, index);
	}

	@Override
	public void set(short s, IUnitChain unitChain, int index) {
		org.jruyi.io.buffer.intcodec.Varint.INST.set(s & 0xFFFF, unitChain, index);
	}

	@Override
	public void prepend(short s, IUnitChain unitChain) {
		IUnit unit = Util.firstUnit(unitChain);
		int shift = 14;
		final int i = s & 0xFFFF;
		int start = unit.start();
		int n;
		while ((n = i >>> shift) == 0) {
			shift -= 7;
			if (shift == 0) {
				n = i;
				break;
			}
		}
		unit.set(--start, (byte) n);

		while (shift > 0) {
			shift -= 7;
			n = ((i >>> shift) & 0x7F) | 0x80;
			if (start == 0) {
				unit.size(unit.size() + unit.start());
				unit.start(0);
				unit = Util.prependNewUnit(unitChain);
				start = unit.start();
			}
			unit.set(--start, (byte) n);
		}

		unit.size(unit.size() + unit.start());
		unit.start(start);
	}
}
