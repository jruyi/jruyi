/**
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

import org.jruyi.common.IByteSequence;
import org.jruyi.io.AbstractCodec;
import org.jruyi.io.ICodec;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;

public final class ByteSequenceCodec extends AbstractCodec<IByteSequence> {

	public static ICodec<IByteSequence> INST = new ByteSequenceCodec();

	@Override
	public void write(IByteSequence src, IUnitChain unitChain) {
		int length = src.length();
		if (length == 0)
			return;

		IUnit unit = Util.lastUnit(unitChain);
		int n = 0;
		int offset = 0;
		while ((n = write(src, offset, length, unit)) < length) {
			offset += n;
			length -= n;
			unit = Util.appendNewUnit(unitChain);
		}
	}

	@Override
	public void write(IByteSequence src, int offset, int length,
			IUnitChain unitChain) {
		if ((offset | length | (offset + length) | (src.length() - (offset + length))) < 0)
			throw new IndexOutOfBoundsException();

		if (length == 0)
			return;

		IUnit unit = Util.lastUnit(unitChain);
		int n = 0;
		while ((n = write(src, offset, length, unit)) < length) {
			offset += n;
			length -= n;
			unit = Util.appendNewUnit(unitChain);
		}
	}

	@Override
	public void set(IByteSequence src, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		int length = src.length();
		if (length == 0)
			return;

		IUnit unit = unitChain.currentUnit();
		int offset = 0;
		int n = set(src, offset, length, unit, index);
		while (n < length) {
			offset += n;
			length -= n;
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new IndexOutOfBoundsException();
			n = set(src, offset, length, unit, 0);
		}
	}

	@Override
	public void set(IByteSequence src, int offset, int length,
			IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		if (length == 0)
			return;

		IUnit unit = unitChain.currentUnit();
		int n = set(src, offset, length, unit, index);
		while (n < length) {
			offset += n;
			length -= n;
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new IndexOutOfBoundsException();
			n = set(src, offset, length, unit, 0);
		}
	}

	@Override
	public void prepend(IByteSequence src, IUnitChain unitChain) {
		int length = src.length();
		if (length < 1)
			return;

		IUnit unit = Util.firstUnit(unitChain);
		while ((length -= prepend(src, 0, length, unit)) < length)
			unit = Util.prependNewUnit(unitChain);
	}

	@Override
	public void prepend(IByteSequence src, int offset, int length,
			IUnitChain unitChain) {
		if ((offset | length | (offset + length) | (src.length() - (offset + length))) < 0)
			throw new IndexOutOfBoundsException();

		if (length < 1)
			return;

		IUnit unit = Util.firstUnit(unitChain);
		while ((length -= prepend(src, offset, length, unit)) > 0)
			unit = Util.prependNewUnit(unitChain);
	}

	private static int set(IByteSequence src, int offset, int length,
			IUnit unit, int position) {
		int n = unit.size() - position;
		if (n > length)
			n = length;

		unit.set(unit.start() + position, src, offset, offset + n);
		return n;
	}

	private static int write(IByteSequence src, int offset, int length,
			IUnit unit) {
		int size = unit.size();
		int n = unit.capacity() - size;
		if (n > length)
			n = length;

		unit.set(unit.start() + size, src, offset, offset + n);
		unit.size(size + n);
		return n;
	}

	private static int prepend(IByteSequence src, int offset, int length,
			IUnit unit) {
		int start = unit.start();
		if (length > start) {
			offset += length - start;
			length = start;
		}
		start -= length;
		unit.set(start, src, offset, offset + length);
		unit.start(start);
		unit.size(unit.size() + length);
		return length;
	}
}
