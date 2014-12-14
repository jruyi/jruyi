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

import java.nio.ByteBuffer;

import org.jruyi.io.AbstractCodec;
import org.jruyi.io.ICodec;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;

public final class ByteBufferCodec extends AbstractCodec<ByteBuffer> {

	public static final ICodec<ByteBuffer> INST = new ByteBufferCodec();

	private ByteBufferCodec() {
	}

	@Override
	public void write(ByteBuffer src, IUnitChain unitChain) {
		int length = src.remaining();
		if (length == 0)
			return;

		IUnit unit = Util.lastUnit(unitChain);
		int n;
		while ((n = write(src, length, unit)) < length) {
			length -= n;
			unit = Util.appendNewUnit(unitChain);
		}
	}

	@Override
	public int read(ByteBuffer dst, IUnitChain unitChain) {
		final int remaining = dst.remaining();
		int length = remaining;
		int offset = 0;
		IUnit unit = unitChain.currentUnit();
		int n;
		while ((n = read(dst, length, unit)) < length) {
			offset += n;
			length -= n;
			unit = unitChain.nextUnit();
			if (unit == null)
				return offset;
		}
		return remaining;
	}

	@Override
	public void get(ByteBuffer dst, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		int length = dst.remaining();
		if (length == 0)
			return;

		IUnit unit = unitChain.currentUnit();
		int n = get(dst, length, unit, index);
		while (n < length) {
			length -= n;
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new IndexOutOfBoundsException();
			n = get(dst, length, unit, 0);
		}
	}

	@Override
	public void set(ByteBuffer src, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		int length = src.remaining();
		if (length == 0)
			return;

		IUnit unit = unitChain.currentUnit();
		int n = set(src, length, unit, index);
		while (n < length) {
			length -= n;
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new IndexOutOfBoundsException();
			n = set(src, length, unit, 0);
		}
	}

	@Override
	public void prepend(ByteBuffer src, IUnitChain unitChain) {
		int length = src.remaining();
		if (length < 1)
			return;

		final int limit = src.limit();
		IUnit unit = Util.firstUnit(unitChain);
		while ((length -= prepend(src, length, unit)) > 0)
			unit = Util.prependNewUnit(unitChain);

		src.limit(limit).position(limit);
	}

	private static int read(ByteBuffer dst, int length, IUnit unit) {
		final int position = unit.position();
		int n = unit.size() - position;
		if (n > length)
			n = length;

		final int begin = unit.start() + position;
		unit.getBytes(begin, begin + n, dst);
		unit.position(position + n);
		return n;
	}

	private static int get(ByteBuffer dst, int length, IUnit unit, int position) {
		int n = unit.size() - position;
		if (n > length)
			n = length;

		position += unit.start();
		unit.getBytes(position, position + n, dst);
		return n;
	}

	private static int set(ByteBuffer src, int length, IUnit unit, int position) {
		int n = unit.size() - position;
		if (n > length)
			n = length;

		unit.set(unit.start() + position, n, src);
		return n;
	}

	private static int write(ByteBuffer src, int length, IUnit unit) {
		final int size = unit.size();
		final int index = unit.start() + size;
		int n = unit.capacity() - index;
		if (n > length)
			n = length;

		unit.set(index, n, src);
		unit.size(size + n);
		return n;
	}

	private static int prepend(ByteBuffer src, int length, IUnit unit) {
		int start = unit.start();
		if (length > start) {
			int position = src.position();
			int limit = position + length - start;
			src.position(position + length - start);
			unit.set(0, start, src);
			unit.start(0);
			unit.size(unit.size() + start);
			src.position(position).limit(limit);
			return start;
		} else {
			start -= length;
			unit.set(start, length, src);
			unit.start(start);
			unit.size(unit.size() + length);
			return length;
		}
	}
}
