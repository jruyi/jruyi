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

package org.jruyi.io.buffer.codec;

import java.nio.BufferUnderflowException;

import org.jruyi.io.ICodec;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;
import org.jruyi.io.buffer.Util;

public final class ByteArrayCodec implements ICodec<byte[]> {

	public static final ICodec<byte[]> INST = new ByteArrayCodec();

	private ByteArrayCodec() {
	}

	@Override
	public byte[] read(IUnitChain unitChain) {
		IUnit unit = unitChain.currentUnit();
		while (unit.isEmpty()) {
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new BufferUnderflowException();
		}

		int n = unitChain.remaining();
		final byte[] dst = new byte[n];
		int offset = 0;
		do {
			n = unit.remaining();
			read(dst, offset, n, unit);
			offset += n;
			unit = unitChain.nextUnit();
		} while (unit != null);
		return dst;
	}

	@Override
	public byte[] read(IUnitChain unitChain, int length) {
		if (length < 0)
			throw new IllegalArgumentException();

		byte[] dst = new byte[length];
		if (length > 0) {
			IUnit unit = unitChain.currentUnit();
			int offset = 0;
			int n;
			while ((n = read(dst, offset, length, unit)) < length) {
				offset += n;
				length -= n;
				unit = unitChain.nextUnit();
				if (unit == null)
					throw new BufferUnderflowException();
			}
		}
		return dst;
	}

	@Override
	public int read(byte[] dst, IUnitChain unitChain) {
		int offset = 0;
		int n;
		int length = dst.length;
		IUnit unit = unitChain.currentUnit();
		while ((n = read(dst, offset, length, unit)) < length) {
			offset += n;
			length -= n;
			unit = unitChain.nextUnit();
			if (unit == null)
				return offset;
		}
		return dst.length;
	}

	@Override
	public int read(byte[] dst, int offset, int length, IUnitChain unitChain) {
		if ((offset | length | (offset + length) | (dst.length - (offset + length))) < 0)
			throw new IndexOutOfBoundsException();
		int n;
		int count = length;
		IUnit unit = unitChain.currentUnit();
		while ((n = read(dst, offset, count, unit)) < count) {
			count -= n;
			offset += n;
			unit = unitChain.nextUnit();
			if (unit == null)
				return length - count;
		}
		return length;
	}

	@Override
	public void write(byte[] src, IUnitChain unitChain) {
		int length = src.length;
		if (length == 0)
			return;

		IUnit unit = Util.lastUnit(unitChain);
		int n;
		int offset = 0;
		while ((n = write(src, offset, length, unit)) < length) {
			offset += n;
			length -= n;
			unit = Util.appendNewUnit(unitChain);
		}
	}

	@Override
	public void write(byte[] src, int offset, int length, IUnitChain unitChain) {
		if ((offset | length | (offset + length) | (src.length - (offset + length))) < 0)
			throw new IndexOutOfBoundsException();

		if (length == 0)
			return;

		IUnit unit = Util.lastUnit(unitChain);
		int n;
		while ((n = write(src, offset, length, unit)) < length) {
			offset += n;
			length -= n;
			unit = Util.appendNewUnit(unitChain);
		}
	}

	@Override
	public byte[] get(IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		IUnit unit = unitChain.currentUnit();
		if (index >= unit.size()) {
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new IndexOutOfBoundsException();
			index = 0;
		}

		int n = unitChain.remaining() + unit.position() - index;
		final byte[] dst = new byte[n];
		n = unit.size() - index;
		get(dst, 0, n, unit, index);
		int offset = n;
		while ((unit = unitChain.nextUnit()) != null) {
			n = unit.size();
			get(dst, offset, n, unit, 0);
			offset += n;
		}
		return dst;
	}

	@Override
	public byte[] get(IUnitChain unitChain, int index, int length) {
		if (index < 0 || length < 0)
			throw new IndexOutOfBoundsException();

		byte[] dst = new byte[length];
		if (length == 0)
			return dst;

		int offset = 0;
		IUnit unit = unitChain.currentUnit();
		int n = get(dst, offset, length, unit, index);
		while (n < length) {
			offset += n;
			length -= n;
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new IndexOutOfBoundsException();
			n = get(dst, offset, length, unit, 0);
		}

		return dst;
	}

	@Override
	public void get(byte[] dst, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		int length = dst.length;
		if (length == 0)
			return;

		int offset = 0;
		IUnit unit = unitChain.currentUnit();
		int n = get(dst, offset, length, unit, index);
		while (n < length) {
			offset += n;
			length -= n;
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new IndexOutOfBoundsException();
			n = get(dst, offset, length, unit, 0);
		}
	}

	@Override
	public void get(byte[] dst, int offset, int length, IUnitChain unitChain, int index) {
		if ((offset | length | (offset + length) | (dst.length - (offset + length))) < 0 || index < 0)
			throw new IndexOutOfBoundsException();

		if (length == 0)
			return;

		IUnit unit = unitChain.currentUnit();
		int n = get(dst, offset, length, unit, index);
		while (n < length) {
			offset += n;
			length -= n;
			unit = unitChain.nextUnit();
			if (unit == null)
				throw new IndexOutOfBoundsException();
			n = get(dst, offset, length, unit, 0);
		}
	}

	@Override
	public void set(byte[] src, IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();

		int length = src.length;
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
	public void set(byte[] src, int offset, int length, IUnitChain unitChain, int index) {
		if ((offset | length | (offset + length) | (src.length - (offset + length))) < 0 || index < 0)
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
	public void prepend(byte[] src, IUnitChain unitChain) {
		int length = src.length;
		if (length < 1)
			return;

		IUnit unit = Util.firstUnit(unitChain);
		while ((length -= prepend(src, 0, length, unit)) > 0)
			unit = Util.prependNewUnit(unitChain);
	}

	@Override
	public void prepend(byte[] src, int offset, int length, IUnitChain unitChain) {
		if ((offset | length | (offset + length) | (src.length - (offset + length))) < 0)
			throw new IndexOutOfBoundsException();

		if (length < 1)
			return;

		IUnit unit = Util.firstUnit(unitChain);
		while ((length -= prepend(src, offset, length, unit)) > 0)
			unit = Util.prependNewUnit(unitChain);
	}

	private static int get(byte[] dst, int offset, int length, IUnit unit, int position) {
		int n = unit.size() - position;
		if (n > length)
			n = length;

		position += unit.start();
		unit.getBytes(position, position + n, dst, offset);
		return n;
	}

	private static int set(byte[] src, int offset, int length, IUnit unit, int position) {
		int n = unit.size() - position;
		if (n > length)
			n = length;

		unit.set(unit.start() + position, src, offset, n);
		return n;
	}

	private static int read(byte[] dst, int offset, int length, IUnit unit) {
		int position = unit.position();
		int n = unit.size() - position;
		if (n > length)
			n = length;

		int begin = unit.start() + position;
		unit.getBytes(begin, begin + n, dst, offset);
		unit.position(position + n);
		return n;
	}

	private static int write(byte[] src, int offset, int length, IUnit unit) {
		int size = unit.size();
		int index = unit.start() + size;
		int n = unit.capacity() - index;
		if (n > length)
			n = length;

		unit.set(index, src, offset, n);
		unit.size(size + n);
		return n;
	}

	private static int prepend(byte[] src, int offset, int length, IUnit unit) {
		int start = unit.start();
		if (length > start) {
			offset += length - start;
			length = start;
		}
		start -= length;
		unit.set(start, src, offset, length);
		unit.start(start);
		unit.size(unit.size() + length);
		return length;
	}
}
