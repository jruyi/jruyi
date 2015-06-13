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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.jruyi.common.*;
import org.jruyi.common.StringBuilder;
import org.jruyi.io.*;

public final class Buffer implements IBuffer, IUnitChain {

	private BufferFactory m_factory;
	private BiListNode<IUnit> m_positionNode;
	private BiListNode<IUnit> m_markNode;
	private BiListNode<IUnit> m_head;

	private Buffer() {
		final BiListNode<IUnit> head = BiListNode.create();
		head.previous(head);
		head.next(head);
		m_head = head;
		m_positionNode = head;
		m_markNode = head;
	}

	static Buffer get(BufferFactory factory) {
		final Buffer buffer = new Buffer();

		buffer.m_head.set(factory.getUnit());
		buffer.m_factory = factory;
		return buffer;
	}

	@Override
	public IUnit create() {
		return m_factory.getUnit();
	}

	@Override
	public IUnit create(int minimumCapacity) {
		return m_factory.getUnit(minimumCapacity);
	}

	@Override
	public IUnit currentUnit() {
		return m_positionNode.get();
	}

	@Override
	public IUnit nextUnit() {
		final BiListNode<IUnit> node = m_positionNode.next();
		if (node == m_head)
			return null;
		m_positionNode = node;
		return node.get();
	}

	@Override
	public IUnit firstUnit() {
		return m_head.get();
	}

	@Override
	public IUnit lastUnit() {
		return m_head.previous().get();
	}

	@Override
	public void append(IUnit unit) {
		final BiListNode<IUnit> node = BiListNode.create();
		node.set(unit);

		final BiListNode<IUnit> head = m_head;
		BiListNode<IUnit> prev = head.previous();
		node.next(head);
		node.previous(prev);
		prev.next(node);
		head.previous(node);
	}

	@Override
	public void prepend(IUnit unit) {
		final BiListNode<IUnit> head = m_head;
		if (m_positionNode != head || head.get().position() > 0)
			throw new UnsupportedOperationException("prepend is not allowed when position() > 0");

		final BiListNode<IUnit> node = BiListNode.create();
		node.set(unit);

		final BiListNode<IUnit> prev = head.previous();
		node.next(head);
		node.previous(prev);
		prev.next(node);
		head.previous(node);
		m_head = node;

		m_positionNode = node;
	}

	@Override
	public int position() {
		final BiListNode<IUnit> positionNode = m_positionNode;
		BiListNode<IUnit> node = m_head;
		int position = node.get().position();
		while (node != positionNode) {
			node = node.next();
			position += node.get().position();
		}
		return position;
	}

	@Override
	public int size() {
		final BiListNode<IUnit> head = m_head;
		BiListNode<IUnit> node = head;
		int size = node.get().size();
		while ((node = node.next()) != head)
			size += node.get().size();
		return size;
	}

	@Override
	public int remaining() {
		final BiListNode<IUnit> head = m_head;
		BiListNode<IUnit> node = m_positionNode;
		int remaining = node.get().remaining();
		while ((node = node.next()) != head)
			remaining += node.get().size();
		return remaining;
	}

	@Override
	public void reset() {
		final BiListNode<IUnit> positionNode = m_positionNode;
		BiListNode<IUnit> node = m_markNode;
		m_positionNode = node;
		node.get().reset();
		while (node != positionNode) {
			node = node.next();
			node.get().rewind();
		}
	}

	@Override
	public void rewind() {
		final BiListNode<IUnit> positionNode = m_positionNode;
		BiListNode<IUnit> node = m_head;
		m_markNode = node;
		m_positionNode = node;
		node.get().rewind();
		while (node != positionNode) {
			node = node.next();
			node.get().rewind();
		}
	}

	@Override
	public int skip(int n) {
		if (n <= 0)
			return 0;

		BiListNode<IUnit> node = m_positionNode;
		IUnit unit = node.get();
		int size = unit.size();
		int m = size - unit.position();

		final BiListNode<IUnit> head = m_head;
		BiListNode<IUnit> next;
		while (m < n && (next = node.next()) != head) {
			unit.position(size);
			node = next;
			unit = node.get();
			size = unit.size();
			m += size;
		}

		unit.position(size - (m - n));
		m_positionNode = node;
		return m < n ? m : n;
	}

	@Override
	public void mark() {
		m_markNode = m_positionNode;
		IUnit unit = m_markNode.get();
		unit.mark(unit.position());
	}

	@Override
	public boolean isEmpty() {
		final BiListNode<IUnit> head = m_head;
		BiListNode<IUnit> node = m_positionNode;
		do {
			if (node.get().remaining() > 0)
				return false;
		} while ((node = node.next()) != head);
		return true;
	}

	@Override
	public int indexOf(byte b) {
		return indexOf(b, 0);
	}

	@Override
	public int indexOf(byte b, int fromIndex) {
		if (fromIndex < 0)
			fromIndex = 0;

		int index = fromIndex;

		final BiListNode<IUnit> head = m_head;
		BiListNode<IUnit> node = head;
		IUnit unit = node.get();
		int n;
		while (fromIndex >= (n = unit.size())) {
			fromIndex -= n;
			node = node.next();
			if (node == head)
				return -1;
			unit = node.get();
		}

		index -= fromIndex;
		n = indexOf(b, unit, fromIndex);
		for (;;) {
			if (n >= 0)
				return index + n;

			index += unit.size();

			node = node.next();
			if (node == head)
				break;

			unit = node.get();
			n = indexOf(b, unit, 0);
		}

		return -1;
	}

	@Override
	public int indexOf(byte[] bytes) {
		return indexOf(bytes, 0);
	}

	@SuppressWarnings("resource")
	@Override
	public int indexOf(byte[] bytes, int fromIndex) {
		if (fromIndex < 0)
			fromIndex = 0;

		final int length = bytes.length;
		int index = fromIndex;
		final BiListNode<IUnit> head = m_head;
		BiListNode<IUnit> node = head;
		IUnit unit = node.get();
		int unitSize;
		int size = 0;
		while (fromIndex >= (unitSize = unit.size())) {
			fromIndex -= unitSize;
			size += unitSize;
			node = node.next();
			if (node == head)
				return length < 1 ? size - length : -1;
			unit = node.get();
		}

		if (length < 1)
			return index;

		// index - base index
		// fromIndex - leftIndex
		index -= fromIndex;
		int n = indexOf(bytes, unit, fromIndex);
		for (;;) {
			if (n >= 0) {
				int m = unitSize - n;
				n += index;
				if (m >= length)
					return n;

				BiListNode<IUnit> temp = node.next();
				while (temp != head && startsWith(bytes, m, unit = temp.get())) {
					if ((m += unit.size()) >= length)
						return n;
					temp = temp.next();
				}
			}

			index += unitSize;
			node = node.next();
			if (node == head)
				break;
			unit = node.get();
			unitSize = unit.size();
			n = indexOf(bytes, unit, 0);
		}

		return -1;
	}

	@Override
	public int indexOf(ByteKmp pattern) {
		return indexOf(pattern, 0);
	}

	@Override
	public int indexOf(ByteKmp pattern, int fromIndex) {
		if (fromIndex < 0)
			fromIndex = 0;

		final int length = pattern.length();
		final BiListNode<IUnit> head = m_head;
		BiListNode<IUnit> node = head;
		IUnit unit = node.get();
		int index = fromIndex;
		int size = 0;
		int unitSize;
		while (index >= (unitSize = unit.size())) {
			index -= unitSize;
			size += unitSize;
			node = node.next();
			if (node == head)
				return length < 1 ? size - length : -1;
			unit = node.get();
		}

		if (length < 1)
			return fromIndex;

		int n = 0;
		try (Blob blob = Blob.get()) {
			blob.add(unit, unit.start() + index, unitSize - index);
			while ((node = node.next()) != head) {
				unit = node.get();
				blob.add(unit, unit.start(), unit.size());
			}

			n = blob.indexOf(pattern);
		}

		return n < 0 ? n : fromIndex + n;
	}

	@Override
	public int lastIndexOf(byte b) {
		int index = size();
		int fromIndex = index - 1;
		BiListNode<IUnit> node = m_head.previous();
		IUnit unit = node.get();
		while (fromIndex < (index -= unit.size())) {
			node = node.previous();
			unit = node.get();
		}

		fromIndex -= index;
		int n = lastIndexOf(b, unit, fromIndex);
		for (;;) {
			if (n >= 0)
				return index + n;

			if (index <= 0)
				break;

			node = node.previous();
			unit = node.get();
			n = unit.size();
			index -= n;
			n = lastIndexOf(b, unit, n - 1);
		}

		return -1;
	}

	@Override
	public int lastIndexOf(byte b, int fromIndex) {
		if (fromIndex < 0)
			return -1;

		int index = size();
		if (fromIndex >= index)
			fromIndex = index - 1;

		BiListNode<IUnit> node = m_head.previous();
		IUnit unit = node.get();
		while (fromIndex < (index -= unit.size())) {
			node = node.previous();
			unit = node.get();
		}

		fromIndex -= index;
		int n = lastIndexOf(b, unit, fromIndex);
		for (;;) {
			if (n >= 0)
				return index + n;

			if (index <= 0)
				break;

			node = node.previous();
			unit = node.get();
			n = unit.size();
			index -= n;
			n = lastIndexOf(b, unit, n - 1);
		}

		return -1;
	}

	@SuppressWarnings("resource")
	@Override
	public int lastIndexOf(byte[] bytes) {
		int length = bytes.length;
		int index = size();
		int fromIndex = index - length;

		if (fromIndex < 0)
			return -1;

		if (length < 1)
			return fromIndex;

		fromIndex += length;

		BiListNode<IUnit> node = m_head.previous();
		IUnit unit = node.get();
		while (fromIndex < (index -= unit.size())) {
			node = node.previous();
			unit = node.get();
		}

		// index - base index
		// fromIndex - right index
		fromIndex -= index;
		int n = lastIndexOf(bytes, fromIndex);
		for (;;) {
			if (n > 0) {
				int m = length - n;
				n = index - m;
				if (m <= 0)
					return n;

				if (n >= 0) {
					BiListNode<IUnit> temp = node.previous();
					while (endsWith(bytes, m, unit = temp.get())) {
						if ((m -= unit.size()) <= 0)
							return n;
						temp = temp.previous();
					}
				}
			}

			if (index <= 0)
				break;

			node = node.previous();
			unit = node.get();
			int unitSize = unit.size();
			index -= unitSize;
			n = lastIndexOf(bytes, unit, unitSize);
		}

		return -1;
	}

	@SuppressWarnings("resource")
	@Override
	public int lastIndexOf(byte[] bytes, int fromIndex) {
		int length = bytes.length;
		int index = size();
		int maxIndex = index - length;
		if (fromIndex > maxIndex)
			fromIndex = maxIndex;

		if (fromIndex < 0)
			return -1;

		if (length < 1)
			return fromIndex;

		fromIndex += length;

		BiListNode<IUnit> node = m_head.previous();
		IUnit unit = node.get();
		while (fromIndex < (index -= unit.size())) {
			node = node.previous();
			unit = node.get();
		}

		// index - base index
		// fromIndex - right index
		fromIndex -= index;
		int n = lastIndexOf(bytes, unit, fromIndex);
		for (;;) {
			if (n > 0) {
				int m = length - n;
				n = index - m;
				if (m <= 0)
					return n;

				if (n >= 0) {
					BiListNode<IUnit> temp = node.previous();
					while (endsWith(bytes, m, unit = temp.get())) {
						if ((m -= unit.size()) <= 0)
							return n;
						temp = temp.previous();
					}
				}
			}

			if (index <= 0)
				break;

			node = node.previous();
			unit = node.get();
			int unitSize = unit.size();
			index -= unitSize;
			n = lastIndexOf(bytes, unit, unitSize);
		}

		return -1;
	}

	@Override
	public int lastIndexOf(ByteKmp pattern) {
		int length = pattern.length();
		int index = size();
		int fromIndex = index - length;

		if (fromIndex < 0)
			return -1;

		if (length < 1)
			return fromIndex;

		fromIndex += length;

		BiListNode<IUnit> head = m_head;
		BiListNode<IUnit> last = head.previous();
		IUnit unit = last.get();
		while (fromIndex < (index -= unit.size())) {
			last = last.previous();
			unit = last.get();
		}

		fromIndex -= index;
		try (Blob blob = Blob.get()) {
			for (BiListNode<IUnit> node = head; node != last; node = node.next()) {
				IUnit temp = node.get();
				blob.add(temp, temp.start(), temp.size());
			}

			blob.add(unit, unit.start(), fromIndex);
			return blob.lastIndexOf(pattern);
		}
	}

	@Override
	public int lastIndexOf(ByteKmp pattern, int fromIndex) {
		int length = pattern.length();
		int index = size();
		int n = index - length;
		if (fromIndex > n)
			fromIndex = n;

		if (fromIndex < 0)
			return -1;

		if (length < 1)
			return fromIndex;

		fromIndex += length;

		BiListNode<IUnit> head = m_head;
		BiListNode<IUnit> last = head.previous();
		IUnit unit = last.get();
		while (fromIndex < (index -= unit.size())) {
			last = last.previous();
			unit = last.get();
		}

		fromIndex -= index;
		try (Blob blob = Blob.get()) {
			for (BiListNode<IUnit> node = head; node != last; node = node.next()) {
				IUnit temp = node.get();
				blob.add(temp, temp.start(), temp.size());
			}

			blob.add(unit, unit.start(), fromIndex);
			return blob.lastIndexOf(pattern);
		}
	}

	@Override
	public boolean startsWith(byte[] bytes) {
		int length = bytes.length;
		if (length < 1)
			return true;

		final BiListNode<IUnit> head = m_head;
		BiListNode<IUnit> node = head;
		IUnit unit;
		int n = 0;
		while (startsWith(bytes, n, unit = node.get())) {
			if ((n += unit.size()) >= length)
				return true;
			node = node.next();
			if (node == head)
				return false;
		}

		return false;
	}

	@SuppressWarnings("resource")
	@Override
	public boolean endsWith(byte[] bytes) {
		int n = bytes.length;
		if (n < 1)
			return true;

		final BiListNode<IUnit> last = m_head.previous();
		BiListNode<IUnit> node = last;
		IUnit unit;
		while (endsWith(bytes, n, unit = node.get())) {
			if ((n -= unit.size()) <= 0)
				return true;
			node = node.previous();
			if (node == last)
				return false;
		}

		return false;
	}

	@Override
	public IBuffer compact() {
		final BiListNode<IUnit> head = m_head;
		IUnit unit = head.get();
		if (!unit.isEmpty()) {
			unit.compact();
			return this;
		}

		BiListNode<IUnit> next = head.next();
		if (next != head) {
			BufferFactory factory = m_factory;
			BiListNode<IUnit> node = head;
			BiListNode<IUnit> prev = head.previous();
			for (;;) {
				factory.putUnit(unit);
				node.close();
				node = next;
				next = node.next();
				unit = node.get();
				if (next == head) {
					if (unit.isEmpty())
						unit.clear();
					else
						unit.compact();
					break;
				}

				if (!unit.isEmpty()) {
					unit.compact();
					break;
				}
			}
			m_head = node;
			node.previous(prev);
			prev.next(node);
			m_markNode = node;
			m_positionNode = node;
		} else
			unit.clear();

		return this;
	}

	@Override
	public Buffer newBuffer() {
		return Buffer.get(m_factory);
	}

	@Override
	public IBuffer split(int size) {
		if (size == 0)
			return newBuffer();

		final Buffer slice = getForSlice(m_factory);
		final BiListNode<IUnit> head = m_head;
		slice.m_head = head;

		boolean adjustPosNode = false;
		boolean adjustMarkNode = false;
		final BiListNode<IUnit> positionNode = m_positionNode;
		final BiListNode<IUnit> markNode = m_markNode;
		BiListNode<IUnit> node = head;
		int n;
		IUnit unit = node.get();
		while (size > (n = unit.size())) {
			size -= n;
			if (node == markNode)
				adjustMarkNode = true;
			if (node == positionNode)
				adjustPosNode = true;
			node = node.next();
			if (node == head)
				throw new IllegalArgumentException();
			unit = node.get();
		}

		if (node == markNode)
			adjustMarkNode = true;
		if (node == positionNode)
			adjustPosNode = true;

		final BiListNode<IUnit> next = node.next();
		if (n > size) {
			final BiListNode<IUnit> temp = BiListNode.create();
			temp.set(unit.slice(size, n));
			m_head = temp;
			if (next == head) {
				temp.next(temp);
				temp.previous(temp);
			} else {
				BiListNode<IUnit> tail = head.previous();
				temp.next(next);
				temp.previous(tail);
				next.previous(temp);
				tail.next(temp);
				head.previous(node);
				node.next(head);
			}
		} else if (next == head) {
			final BiListNode<IUnit> temp = BiListNode.create();
			temp.previous(temp);
			temp.next(temp);
			temp.set(m_factory.getUnit());
			m_head = temp;
		} else {
			m_head = next;
			final BiListNode<IUnit> tail = head.previous();
			head.previous(node);
			node.next(head);
			next.previous(tail);
			tail.next(next);
		}

		if (adjustMarkNode) {
			slice.m_markNode = markNode;
			m_markNode = m_head;
		}

		if (adjustPosNode) {
			slice.m_positionNode = positionNode;
			m_positionNode = m_head;
		}

		return slice;
	}

	@SuppressWarnings("resource")
	@Override
	public void drain() {
		BiListNode<IUnit> head = m_head;
		m_positionNode = head;
		m_markNode = head;

		BufferFactory factory = m_factory;
		BiListNode<IUnit> node = head.next();
		if (node != head) {
			do {
				factory.putUnit(node.get());
				BiListNode<IUnit> temp = node;
				node = node.next();
				temp.close();
			} while (node != head);
			head.previous(head);
			head.next(head);
		}

		factory.putUnit(head.get());
		head.set(factory.getUnit());
	}

	@Override
	public void drainTo(IBuffer dst) {
		if (!(dst instanceof Buffer)) {
			dst.write(this, Codec.byteSequence());
			drain();
			return;
		}

		BufferFactory factory = m_factory;
		BiListNode<IUnit> thisPos = m_positionNode;
		BiListNode<IUnit> node = m_head;
		BiListNode<IUnit> thisTail = node.previous();
		while (node != thisPos) {
			factory.putUnit(node.get());
			BiListNode<IUnit> temp = node;
			node = node.next();
			temp.close();
		}

		thisPos.get().compact();
		node = ((Buffer) dst).m_head;
		BiListNode<IUnit> thatTail = node.previous();
		thisPos.previous(thatTail);
		thatTail.next(thisPos);
		thisTail.next(node);
		node.previous(thisTail);

		node = BiListNode.create();
		node.next(node);
		node.previous(node);
		node.set(factory.getUnit());
		m_head = node;
		m_positionNode = node;
		m_markNode = node;
	}

	@Override
	public int reserveHead(int size) {
		final IUnit unit = m_head.get();
		if (unit.size() > 0)
			return unit.start();

		final int capacity = unit.capacity();
		if (size > capacity)
			size = capacity;
		else if (size < 0) {
			size += capacity;
			if (size < 0)
				size = 0;
		}
		unit.start(size);
		return size;
	}

	@Override
	public int headReserved() {
		return m_head.get().start();
	}

	@Override
	public IBuffer setLength(int newLength) {
		if (newLength < 0)
			throw new IllegalArgumentException();

		int len = length();
		if (newLength == len)
			return this;

		if (newLength > len) {
			writeFill((byte) 0, newLength - len);
			return this;
		}

		if (newLength == 0) {
			drain();
			return this;
		}

		final BiListNode<IUnit> positionNode = m_positionNode;
		final BiListNode<IUnit> markNode = m_markNode;
		boolean adjustPosNode = false;
		boolean adjustMarkNode = false;
		BufferFactory factory = m_factory;
		len = len - newLength;
		BiListNode<IUnit> node = m_head.previous();
		IUnit unit = node.get();
		while ((len -= unit.size()) >= 0) {
			if (node == positionNode)
				adjustPosNode = true;
			if (node == markNode)
				adjustMarkNode = true;
			BiListNode<IUnit> next = node.next();
			BiListNode<IUnit> previous = node.previous();
			previous.next(next);
			next.previous(previous);
			node.close();
			factory.putUnit(unit);

			node = previous;
			unit = node.get();
		}

		unit.size(-len);

		if (adjustPosNode)
			m_positionNode = node;

		if (adjustMarkNode)
			m_markNode = node;

		return this;
	}

	@Override
	public IBuffer writeFill(byte b, int count) {
		if (count < 0)
			throw new IllegalArgumentException();

		if (count < 1)
			return this;

		IUnit unit = Util.lastUnit(this);
		for (;;) {
			int size = unit.size();
			int index = unit.start() + unit.size();
			int n = unit.capacity() - index;
			if (n >= count) {
				unit.setFill(index, b, count);
				unit.size(size + count);
				break;
			} else {
				unit.setFill(index, b, n);
				unit.size(size + n);
			}
			count -= n;
			unit = Util.appendNewUnit(this);
		}

		return this;
	}

	@Override
	public IBuffer setFill(int index, byte b, int count) {
		if (count < 0)
			throw new IndexOutOfBoundsException();

		final BiListNode<IUnit> head = m_head;
		BiListNode<IUnit> node = head;
		IUnit unit = node.get();
		int size;
		while (index > (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == head)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		int n = size - index;
		index += unit.start();
		for (;;) {
			if (n >= count) {
				unit.setFill(index, b, count);
				break;
			}
			unit.setFill(index, b, n);
			count -= n;
			node = node.next();
			if (node == head)
				throw new IndexOutOfBoundsException();
			unit = node.get();
			n = unit.size();
			index = unit.start();
		}
		return this;
	}

	@Override
	public IBuffer prependFill(byte b, int count) {
		if (count < 0)
			throw new IndexOutOfBoundsException();

		if (count < 1)
			return this;

		IUnit unit = Util.firstUnit(this);
		for (;;) {
			int n = unit.start();
			if (n >= count) {
				int start = n - count;
				unit.setFill(start, b, count);
				unit.start(start);
				unit.size(unit.size() + count);
				break;
			}
			unit.setFill(0, b, n);
			unit.start(0);
			unit.size(unit.size() + n);
			count -= n;
			unit = Util.prependNewUnit(this);
		}
		return this;
	}

	@Override
	public IBuffer set(int index, byte b) {
		final BiListNode<IUnit> head = m_head;
		BiListNode<IUnit> node = head;
		IUnit unit = node.get();
		int size;
		while (index >= (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == head)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		unit.set(unit.start() + index, b);
		return this;
	}

	@Override
	public IBuffer set(int index, char c, ICharCodec codec) {
		BiListNode<IUnit> temp = m_head;
		BiListNode<IUnit> node = temp;
		IUnit unit = node.get();
		int size;
		while (index > (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == temp)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		temp = m_positionNode;
		m_positionNode = node;
		try {
			codec.set(c, this, index);
		} finally {
			m_positionNode = temp;
		}
		return this;
	}

	@Override
	public IBuffer set(int index, short s, IShortCodec codec) {
		BiListNode<IUnit> temp = m_head;
		BiListNode<IUnit> node = temp;
		IUnit unit = node.get();
		int size;
		while (index > (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == temp)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		temp = m_positionNode;
		m_positionNode = node;
		try {
			codec.set(s, this, index);
		} finally {
			m_positionNode = temp;
		}
		return this;
	}

	@Override
	public IBuffer set(int index, int i, IIntCodec codec) {
		BiListNode<IUnit> temp = m_head;
		BiListNode<IUnit> node = temp;
		IUnit unit = node.get();
		int size;
		while (index > (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == temp)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		temp = m_positionNode;
		m_positionNode = node;
		try {
			codec.set(i, this, index);
		} finally {
			m_positionNode = temp;
		}
		return this;
	}

	@Override
	public IBuffer set(int index, long l, ILongCodec codec) {
		BiListNode<IUnit> temp = m_head;
		BiListNode<IUnit> node = temp;
		IUnit unit = node.get();
		int size;
		while (index > (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == temp)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		temp = m_positionNode;
		m_positionNode = node;
		try {
			codec.set(l, this, index);
		} finally {
			m_positionNode = temp;
		}
		return this;
	}

	@Override
	public IBuffer set(int index, float f, IFloatCodec codec) {
		BiListNode<IUnit> temp = m_head;
		BiListNode<IUnit> node = temp;
		IUnit unit = node.get();
		int size;
		while (index > (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == temp)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		temp = m_positionNode;
		m_positionNode = node;
		try {
			codec.set(f, this, index);
		} finally {
			m_positionNode = temp;
		}
		return this;
	}

	@Override
	public IBuffer set(int index, double d, IDoubleCodec codec) {
		BiListNode<IUnit> temp = m_head;
		BiListNode<IUnit> node = temp;
		IUnit unit = node.get();
		int size;
		while (index > (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == temp)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		temp = m_positionNode;
		m_positionNode = node;
		try {
			codec.set(d, this, index);
		} finally {
			m_positionNode = temp;
		}
		return this;
	}

	@Override
	public <T> IBuffer set(int index, T src, ICodec<T> codec) {
		BiListNode<IUnit> temp = m_head;
		BiListNode<IUnit> node = temp;
		IUnit unit = node.get();
		int size;
		while (index > (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == temp)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		temp = m_positionNode;
		m_positionNode = node;
		try {
			codec.set(src, this, index);
		} finally {
			m_positionNode = temp;
		}
		return this;
	}

	@Override
	public <T> IBuffer set(int index, T src, int offset, int length, ICodec<T> codec) {
		BiListNode<IUnit> temp = m_head;
		BiListNode<IUnit> node = temp;
		IUnit unit = node.get();
		int size;
		while (index > (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == temp)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		temp = m_positionNode;
		m_positionNode = node;
		try {
			codec.set(src, offset, length, this, index);
		} finally {
			m_positionNode = temp;
		}
		return this;
	}

	@Override
	public IBuffer write(byte b) {
		IUnit unit = lastUnit();
		if (!unit.appendable()) {
			unit = create();
			append(unit);
		}
		int size = unit.size();
		unit.set(unit.start() + size, b);
		unit.size(++size);
		return this;
	}

	@Override
	public IBuffer write(char c, ICharCodec codec) {
		codec.write(c, this);
		return this;
	}

	@Override
	public IBuffer write(short s, IShortCodec codec) {
		codec.write(s, this);
		return this;
	}

	@Override
	public IBuffer write(int i, IIntCodec codec) {
		codec.write(i, this);
		return this;
	}

	@Override
	public IBuffer write(long l, ILongCodec codec) {
		codec.write(l, this);
		return this;
	}

	@Override
	public IBuffer write(float f, IFloatCodec codec) {
		codec.write(f, this);
		return this;
	}

	@Override
	public IBuffer write(double d, IDoubleCodec codec) {
		codec.write(d, this);
		return this;
	}

	@Override
	public <T> IBuffer write(T src, ICodec<T> codec) {
		codec.write(src, this);
		return this;
	}

	@Override
	public <T> IBuffer write(T src, int offset, int length, ICodec<T> codec) {
		codec.write(src, offset, length, this);
		return this;
	}

	@Override
	public byte byteAt(int index) {
		final BiListNode<IUnit> head = m_head;
		BiListNode<IUnit> node = head;
		IUnit unit = node.get();
		int size;
		while (index >= (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == head)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		return unit.byteAt(unit.start() + index);
	}

	@Override
	public int getUnsignedByte(int index) {
		return byteAt(index) & 0xFF;
	}

	@Override
	public int getUnsignedShort(int index, IShortCodec codec) {
		return get(index, codec) & 0xFFFF;
	}

	@Override
	public int readUnsignedByte() {
		return read() & 0xFF;
	}

	@Override
	public int readUnsignedShort(IShortCodec codec) {
		return codec.read(this) & 0xFFFF;
	}

	@Override
	public char get(int index, ICharCodec codec) {
		BiListNode<IUnit> temp = m_head;
		BiListNode<IUnit> node = temp;
		IUnit unit = node.get();
		int size;
		while (index > (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == temp)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		temp = m_positionNode;
		m_positionNode = node;
		try {
			return codec.get(this, index);
		} finally {
			m_positionNode = temp;
		}
	}

	@Override
	public short get(int index, IShortCodec codec) {
		BiListNode<IUnit> temp = m_head;
		BiListNode<IUnit> node = temp;
		IUnit unit = node.get();
		int size;
		while (index > (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == temp)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		temp = m_positionNode;
		m_positionNode = node;
		try {
			return codec.get(this, index);
		} finally {
			m_positionNode = temp;
		}
	}

	@Override
	public int get(int index, IIntCodec codec) {
		BiListNode<IUnit> temp = m_head;
		BiListNode<IUnit> node = temp;
		IUnit unit = node.get();
		int size;
		while (index > (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == temp)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		temp = m_positionNode;
		m_positionNode = node;
		try {
			return codec.get(this, index);
		} finally {
			m_positionNode = temp;
		}
	}

	@Override
	public long get(int index, ILongCodec codec) {
		BiListNode<IUnit> temp = m_head;
		BiListNode<IUnit> node = temp;
		IUnit unit = node.get();
		int size;
		while (index > (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == temp)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		temp = m_positionNode;
		m_positionNode = node;
		try {
			return codec.get(this, index);
		} finally {
			m_positionNode = temp;
		}
	}

	@Override
	public float get(int index, IFloatCodec codec) {
		BiListNode<IUnit> temp = m_head;
		BiListNode<IUnit> node = temp;
		IUnit unit = node.get();
		int size;
		while (index > (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == temp)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		temp = m_positionNode;
		m_positionNode = node;
		try {
			return codec.get(this, index);
		} finally {
			m_positionNode = temp;
		}
	}

	@Override
	public double get(int index, IDoubleCodec codec) {
		BiListNode<IUnit> temp = m_head;
		BiListNode<IUnit> node = temp;
		IUnit unit = node.get();
		int size;
		while (index > (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == temp)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		temp = m_positionNode;
		m_positionNode = node;
		try {
			return codec.get(this, index);
		} finally {
			m_positionNode = temp;
		}
	}

	@Override
	public <T> T get(int index, ICodec<T> codec) {
		BiListNode<IUnit> temp = m_head;
		BiListNode<IUnit> node = temp;
		IUnit unit = node.get();
		int size;
		while (index > (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == temp)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		temp = m_positionNode;
		m_positionNode = node;
		try {
			return codec.get(this, index);
		} finally {
			m_positionNode = temp;
		}
	}

	@Override
	public <T> T get(int index, int length, ICodec<T> codec) {
		BiListNode<IUnit> temp = m_head;
		BiListNode<IUnit> node = temp;
		IUnit unit = node.get();
		int size;
		while (index > (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == temp)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		temp = m_positionNode;
		m_positionNode = node;
		try {
			return codec.get(this, index, length);
		} finally {
			m_positionNode = temp;
		}
	}

	@Override
	public <T> void get(int index, T dst, ICodec<T> codec) {
		BiListNode<IUnit> temp = m_head;
		BiListNode<IUnit> node = temp;
		IUnit unit = node.get();
		int size;
		while (index > (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == temp)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		temp = m_positionNode;
		m_positionNode = node;
		try {
			codec.get(dst, this, index);
		} finally {
			m_positionNode = temp;
		}
	}

	@Override
	public <T> void get(int index, T dst, int offset, int length, ICodec<T> codec) {
		BiListNode<IUnit> temp = m_head;
		BiListNode<IUnit> node = temp;
		IUnit unit = node.get();
		int size;
		while (index > (size = unit.size())) {
			index -= size;
			node = node.next();
			if (node == temp)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		temp = m_positionNode;
		m_positionNode = node;
		try {
			codec.get(dst, offset, length, this, index);
		} finally {
			m_positionNode = temp;
		}
	}

	@Override
	public byte read() {
		IUnit unit = currentUnit();
		while (unit.isEmpty()) {
			unit = nextUnit();
			if (unit == null)
				throw new BufferUnderflowException();
		}
		int position = unit.position();
		byte b = unit.byteAt(unit.start() + position);
		unit.position(++position);
		return b;
	}

	@Override
	public char read(ICharCodec codec) {
		return codec.read(this);
	}

	@Override
	public short read(IShortCodec codec) {
		return codec.read(this);
	}

	@Override
	public int read(IIntCodec codec) {
		return codec.read(this);
	}

	@Override
	public long read(ILongCodec codec) {
		return codec.read(this);
	}

	@Override
	public float read(IFloatCodec codec) {
		return codec.read(this);
	}

	@Override
	public double read(IDoubleCodec codec) {
		return codec.read(this);
	}

	@Override
	public <T> T read(ICodec<T> codec) {
		return codec.read(this);
	}

	@Override
	public <T> T read(int length, ICodec<T> codec) {
		return codec.read(this, length);
	}

	@Override
	public <T> int read(T dst, ICodec<T> codec) {
		return codec.read(dst, this);
	}

	@Override
	public <T> int read(T dst, int offset, int length, ICodec<T> codec) {
		return codec.read(dst, offset, length, this);
	}

	@Override
	public IBuffer prepend(byte b) {
		IUnit unit = firstUnit();
		if (!unit.prependable()) {
			unit = create();
			unit.start(unit.capacity());
			prepend(unit);
		}
		int start = unit.start();
		unit.set(--start, b);
		unit.start(start);
		unit.size(unit.size() + 1);
		return this;
	}

	@Override
	public IBuffer prepend(char c, ICharCodec codec) {
		codec.prepend(c, this);
		return this;
	}

	@Override
	public IBuffer prepend(short s, IShortCodec codec) {
		codec.prepend(s, this);
		return this;
	}

	@Override
	public IBuffer prepend(int i, IIntCodec codec) {
		codec.prepend(i, this);
		return this;
	}

	@Override
	public IBuffer prepend(long l, ILongCodec codec) {
		codec.prepend(l, this);
		return this;
	}

	@Override
	public IBuffer prepend(float f, IFloatCodec codec) {
		codec.prepend(f, this);
		return this;
	}

	@Override
	public IBuffer prepend(double d, IDoubleCodec codec) {
		codec.prepend(d, this);
		return this;
	}

	@Override
	public <T> IBuffer prepend(T src, ICodec<T> codec) {
		codec.prepend(src, this);
		return this;
	}

	@Override
	public <T> IBuffer prepend(T src, int offset, int length, ICodec<T> codec) {
		codec.prepend(src, this);
		return this;
	}

	@Override
	public void dump(StringBuilder builder) {
		final BiListNode<IUnit> head = m_head;
		BiListNode<IUnit> node = head;
		try (Blob blob = Blob.get()) {
			do {
				final IUnit unit = node.get();
				blob.add(unit, unit.start(), unit.size());
				node = node.next();
			} while (node != head);
			blob.dump(builder);
		}
	}

	@Override
	public boolean isClosed() {
		return m_factory == null;
	}

	@Override
	@SuppressWarnings("resource")
	public void close() {
		final BufferFactory factory = m_factory;
		if (factory == null)
			return;
		m_factory = null;
		final BiListNode<IUnit> head = m_head;
		m_positionNode = head;
		m_markNode = head;

		factory.putUnit(head.get());
		head.set(null);
		BiListNode<IUnit> node = head.next();
		if (node != head) {
			do {
				factory.putUnit(node.get());
				BiListNode<IUnit> temp = node;
				node = node.next();
				temp.close();
			} while (node != head);
			head.previous(head);
			head.next(head);
		}
	}

	@Override
	public byte[] getBytes(int start) {
		return getBytes(start, size() - start);
	}

	@Override
	public byte[] getBytes(int start, int length) {
		BiListNode<IUnit> temp = m_head;
		BiListNode<IUnit> node = temp;
		IUnit unit = node.get();
		int size;
		while (start > (size = unit.size())) {
			start -= size;
			node = node.next();
			if (node == temp)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		temp = m_positionNode;
		m_positionNode = node;
		try {
			return Codec.byteArray().get(this, start, length);
		} finally {
			m_positionNode = temp;
		}
	}

	@Override
	public void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin) {
		int length = srcEnd - srcBegin;
		BiListNode<IUnit> temp = m_head;
		BiListNode<IUnit> node = temp;
		IUnit unit = node.get();
		int size;
		while (srcBegin > (size = unit.size())) {
			srcBegin -= size;
			node = node.next();
			if (node == temp)
				throw new IndexOutOfBoundsException();
			unit = node.get();
		}
		temp = m_positionNode;
		m_positionNode = node;
		try {
			Codec.byteArray().get(dst, dstBegin, length, this, srcBegin);
		} finally {
			m_positionNode = temp;
		}
	}

	@Override
	public int length() {
		return size();
	}

	@Override
	public int compareTo(IBuffer that) {
		if (!(that instanceof Buffer))
			return compareInternal(that);

		Buffer thatBuf = (Buffer) that;
		int remaining = remaining();
		int thatRemaining = thatBuf.remaining();
		int ret = 0;
		if (remaining > thatRemaining)
			ret = 1;
		else if (remaining < thatRemaining)
			ret = -1;

		if (remaining < 1 || thatRemaining < 1)
			return ret;

		IUnit unit = currentUnit();
		BiListNode<IUnit> p = m_positionNode;
		BiListNode<IUnit> s = m_head.previous();

		IUnit thatUnit = thatBuf.currentUnit();
		BiListNode<IUnit> q = thatBuf.m_positionNode;
		BiListNode<IUnit> t = thatBuf.m_head.previous();

		int i = unit.position();
		int j = thatUnit.position();

		remaining = unit.remaining();
		thatRemaining = thatUnit.remaining();
		for (;;) {
			if (remaining > thatRemaining) {
				int n = compare(unit, i, thatUnit, j, thatRemaining);
				if (n != 0)
					return n;

				if (q == t)
					break;

				i = thatRemaining;
				remaining -= thatRemaining;
				j = 0;
				q = q.next();
				thatUnit = q.get();
				thatRemaining = thatUnit.size();
			} else {
				int n = compare(unit, i, thatUnit, j, remaining);
				if (n != 0)
					return n;

				if (p == s)
					break;

				j = remaining;
				thatRemaining -= remaining;
				i = 0;
				p = p.next();
				unit = p.get();
				remaining = unit.size();
			}
		}

		return ret;
	}

	@Override
	public int readIn(ReadableByteChannel in) throws IOException {
		IUnit unit = Util.lastUnit(this);
		int n = in.read(unit.getByteBufferForWrite());
		if (n > 0)
			unit.size(unit.size() + n);
		return n;
	}

	@Override
	public int writeOut(WritableByteChannel out) throws IOException {
		final BiListNode<IUnit> tail = m_head.previous();
		BiListNode<IUnit> node = m_positionNode;
		IUnit unit = node.get();
		while (unit.isEmpty()) {
			if (node == tail)
				return 0;
			node = node.next();
			unit = node.get();
			m_positionNode = node;
		}

		int n = 0;
		if (node == tail || !(out instanceof GatheringByteChannel)) {
			n = out.write(unit.getByteBufferForRead());
			if (n > 0)
				unit.position(unit.position() + n);
		} else {
			ByteBufferArray bba = ByteBufferArray.get();
			try {
				bba.add(unit.getByteBufferForRead());
				while (node != tail) {
					node = node.next();
					unit = node.get();
					bba.add(unit.getByteBufferForRead());
				}
				n = (int) ((GatheringByteChannel) out).write(bba.array(), 0, bba.size());
			} finally {
				bba.clear();
			}

			if (n > 0) {
				int m = n;
				node = m_positionNode;
				unit = node.get();
				while ((m -= unit.skip(m)) > 0) {
					node = node.next();
					unit = node.get();
				}

				m_positionNode = node;
			}
		}

		return n;
	}

	@Override
	public OutputStream getOutputStream() {
		return new BufferOutputStream(this);
	}

	@Override
	public InputStream getInputStream() {
		return new BufferInputStream(this);
	}

	int readByte() {
		IUnit unit = currentUnit();
		while (unit.isEmpty()) {
			unit = nextUnit();
			if (unit == null)
				return -1;
		}
		int position = unit.position();
		int i = unit.byteAt(unit.start() + position) & 0xFF;
		unit.position(++position);
		return i;
	}

	/**
	 * {@code fromIndex} must be less than m_size and non-negative.
	 */
	private static int indexOf(byte b, IUnit unit, int fromIndex) {
		int start = unit.start();
		int end = start + unit.size();
		fromIndex += start;
		while (fromIndex < end) {
			if (unit.byteAt(fromIndex) == b)
				return fromIndex - start;
			++fromIndex;
		}
		return -1;
	}

	private static int indexOf(byte[] bytes, IUnit unit, int leftIndex) {
		int start = unit.start();
		int end = start + unit.size();
		int index = start + leftIndex;
		int length = bytes.length;

		next: for (; index < end; ++index) {
			leftIndex = index;
			int rightIndex = index + length;
			if (rightIndex > end)
				rightIndex = end;

			int i = 0;
			for (; leftIndex < rightIndex; ++leftIndex, ++i) {
				if (unit.byteAt(leftIndex) != bytes[i])
					continue next;
			}

			return index - start;
		}

		return -1;
	}

	/**
	 * {@code fromIndex} must be less than m_size and non-negative.
	 */
	private static int lastIndexOf(byte b, IUnit unit, int fromIndex) {
		int start = unit.start();
		fromIndex += start;
		while (fromIndex >= start) {
			if (unit.byteAt(fromIndex) == b)
				return fromIndex - start;

			--fromIndex;
		}

		return -1;
	}

	private static int lastIndexOf(byte[] bytes, IUnit unit, int rightIndex) {
		int start = unit.start();
		int end = start + rightIndex;
		int length = bytes.length;

		next: for (; end > start; --end) {
			rightIndex = end;
			int leftIndex = end - length;
			if (leftIndex < start)
				leftIndex = start;

			int i = length;
			while (rightIndex > leftIndex) {
				if (unit.byteAt(--rightIndex) != bytes[--i])
					continue next;
			}

			return end - start;
		}

		return -1;
	}

	private static boolean startsWith(byte[] bytes, int offset, IUnit unit) {
		int start = unit.start();
		int end = bytes.length - offset;
		int size = unit.size();
		if (end > size)
			end = size;

		end += start;
		for (; start < end; ++start, ++offset) {
			if (unit.byteAt(start) != bytes[offset])
				return false;
		}

		return true;
	}

	private static boolean endsWith(byte[] bytes, int offset, IUnit unit) {
		int start = unit.start();
		int size = unit.size();
		int end = start + size;
		if (offset < size)
			start = end - offset;

		while (end > start) {
			if (unit.byteAt(--end) != bytes[--offset])
				return false;
		}

		return true;
	}

	private static Buffer getForSlice(BufferFactory factory) {
		// Buffer buffer = c_cache.take();
		// if (buffer == null)
		// buffer = new Buffer();

		Buffer buffer = new Buffer();
		buffer.m_factory = factory;
		return buffer;
	}

	private int compareInternal(IBuffer that) {
		int n = that.remaining();
		int remaining = remaining();
		if (n < remaining) {
			remaining = n;
			n = 1;
		} else if (n > remaining)
			n = -1;
		else
			n = 0;

		if (remaining > 0) {
			IUnit unit = currentUnit();
			BiListNode<IUnit> node = m_positionNode;
			int size = unit.remaining();
			int i = that.position();
			for (;;) {
				if (remaining < size)
					size = remaining;

				int ret = compare(unit, that, i, size);
				if (ret != 0)
					return ret;

				i += size;
				remaining -= size;

				if (remaining <= 0)
					break;

				node = node.next();
				unit = node.get();
				size = unit.size();
			}
		}

		return n;
	}

	private static int compare(IUnit unit, IByteSequence sequence, int from, int len) {
		int i = unit.start() + unit.position();
		len += i;
		for (; i < len; ++i, ++from) {
			byte b1 = unit.byteAt(i);
			byte b2 = sequence.byteAt(from);
			if (b1 != b2)
				return b1 < b2 ? -1 : 1;
		}

		return 0;
	}

	private static int compare(IUnit unit1, int i, IUnit unit2, int j, int len) {
		i += unit1.start();
		j += unit2.start();
		for (; len > 0; ++i, ++j, --len) {
			byte b1 = unit1.byteAt(i);
			byte b2 = unit2.byteAt(j);
			if (b1 != b2)
				return (b1 < b2) ? -1 : 1;
		}

		return 0;
	}
}
