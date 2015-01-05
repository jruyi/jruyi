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

import org.jruyi.common.IByteSequence;
import org.jruyi.io.IUnit;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

final class HeapUnit implements IUnit {

	private static final Unsafe c_unsafe = com.lmax.disruptor.util.Util.getUnsafe();
	private static final long BYTE_ARRAY_BASE_OFFSET = c_unsafe.arrayBaseOffset(byte[].class);

	// offset of the next byte to be read
	private int m_position;
	// offset of the marked byte
	private int m_mark;
	// number of bytes contained in this unit
	private int m_size;
	// offset of the first byte
	private int m_start;
	// byte array containing the data
	private byte[] m_array;

	private ByteBuffer m_bb;

	public HeapUnit(int capacity) {
		final byte[] array = new byte[capacity];
		m_array = array;
		m_bb = ByteBuffer.wrap(array);
	}

	public void setCapacity(int newCapacity) {
		final byte[] array = new byte[newCapacity];
		m_array = array;
		m_bb = ByteBuffer.wrap(array);
	}

	@Override
	public HeapUnit set(int index, byte b) {
		c_unsafe.putByte(m_array, byteArrayOffset(index), b);
		return this;
	}

	@Override
	public HeapUnit set(int index, short s) {
		c_unsafe.putShort(m_array, byteArrayOffset(index), s);
		return this;
	}

	@Override
	public HeapUnit set(int index, int i) {
		c_unsafe.putInt(m_array, byteArrayOffset(index), i);
		return this;
	}

	@Override
	public HeapUnit set(int index, long l) {
		c_unsafe.putLong(m_array, byteArrayOffset(index), l);
		return this;
	}

	@Override
	public short getShort(int index) {
		return c_unsafe.getShort(m_array, byteArrayOffset(index));
	}

	@Override
	public int getInt(int index) {
		return c_unsafe.getInt(m_array, byteArrayOffset(index));
	}

	@Override
	public long getLong(int index) {
		return c_unsafe.getLong(m_array, byteArrayOffset(index));
	}

	@Override
	public HeapUnit set(int index, IByteSequence src, int srcBegin, int srcEnd) {
		src.getBytes(srcBegin, srcEnd, m_array, index);
		return this;
	}

	@Override
	public HeapUnit set(int index, byte[] src, int offset, int length) {
		c_unsafe.copyMemory(src, byteArrayOffset(offset), m_array, byteArrayOffset(index), length);
		return this;
	}

	@Override
	public HeapUnit setFill(int index, byte b, int count) {
		c_unsafe.setMemory(m_array, byteArrayOffset(index), count, b);
		return this;
	}

	@Override
	public byte byteAt(int index) {
		return c_unsafe.getByte(m_array, byteArrayOffset(index));
	}

	@Override
	public byte[] getBytes(int index) {
		final byte[] array = m_array;
		final int length = array.length - index;
		final byte[] data = new byte[length];
		c_unsafe.copyMemory(array, byteArrayOffset(index), data, BYTE_ARRAY_BASE_OFFSET, length);
		return data;
	}

	@Override
	public byte[] getBytes(int index, int length) {
		final byte[] data = new byte[length];
		c_unsafe.copyMemory(m_array, byteArrayOffset(index), data, BYTE_ARRAY_BASE_OFFSET, length);
		return data;
	}

	@Override
	public void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin) {
		c_unsafe.copyMemory(m_array, byteArrayOffset(srcBegin), dst, byteArrayOffset(dstBegin), srcEnd - srcBegin);
	}

	@Override
	public void getBytes(int srcBegin, int srcEnd, ByteBuffer dst) {
		final int dstPosition = dst.position();
		final byte[] dstByteArray;
		final long dstBaseOffset;
		if (dst.hasArray()) {
			dstByteArray = dst.array();
			dstBaseOffset = byteArrayOffset(dst.arrayOffset());
		} else {
			dstByteArray = null;
			dstBaseOffset = ((DirectBuffer) dst).address();
		}
		final int length = srcEnd - srcBegin;
		c_unsafe.copyMemory(m_array, byteArrayOffset(srcBegin), dstByteArray, dstBaseOffset + dstPosition, length);
		dst.position(dstPosition + length);
	}

	@Override
	public HeapUnit set(int index, int length, ByteBuffer src) {
		final int srcPosition = src.position();
		final byte[] srcByteArray;
		final long srcBaseOffset;
		if (src.hasArray()) {
			srcByteArray = src.array();
			srcBaseOffset = byteArrayOffset(src.arrayOffset());
		} else {
			srcByteArray = null;
			srcBaseOffset = ((DirectBuffer) src).address();
		}
		c_unsafe.copyMemory(srcByteArray, srcBaseOffset + srcPosition, m_array, byteArrayOffset(index), length);
		src.position(srcPosition + length);
		return this;
	}

	@Override
	public int length() {
		return m_array.length;
	}

	@Override
	public int start() {
		return m_start;
	}

	@Override
	public void start(int start) {
		m_start = start;
	}

	@Override
	public int position() {
		return m_position;
	}

	@Override
	public void position(int position) {
		m_position = position;
	}

	@Override
	public int size() {
		return m_size;
	}

	@Override
	public void size(int size) {
		m_size = size;
	}

	@Override
	public int mark() {
		return m_mark;
	}

	@Override
	public void mark(int mark) {
		m_mark = mark;
	}

	@Override
	public int remaining() {
		return m_size - m_position;
	}

	@Override
	public int available() {
		return m_array.length - m_size - m_start;
	}

	@Override
	public int capacity() {
		return m_array.length;
	}

	@Override
	public boolean appendable() {
		return m_start + m_size < m_array.length;
	}

	@Override
	public boolean prependable() {
		return m_start > 0;
	}

	@Override
	public boolean isEmpty() {
		return m_position >= m_size;
	}

	@Override
	public void reset() {
		m_position = m_mark;
	}

	@Override
	public void rewind() {
		m_position = m_mark = 0;
	}

	@Override
	public int skip(int n) {
		if (n < 1)
			return 0;

		int m = remaining();
		if (m > n)
			m = n;

		m_position += m;
		return m;
	}

	@Override
	public ByteBuffer getByteBufferForRead() {
		final ByteBuffer bb = m_bb;
		final int start = m_start;
		bb.limit(start + m_size);
		bb.position(start + m_position);
		return bb;
	}

	@Override
	public ByteBuffer getByteBufferForRead(int offset, int length) {
		final ByteBuffer bb = m_bb;
		bb.rewind();
		length += offset;
		if (length > m_size)
			length = m_size;

		final int start = m_start;
		bb.limit(start + length);
		bb.position(start + offset);
		return bb;
	}

	@Override
	public ByteBuffer getByteBufferForWrite() {
		final ByteBuffer bb = m_bb;
		bb.limit(m_array.length);
		bb.position(m_start + m_size);
		return bb;
	}

	@Override
	public void clear() {
		m_start = 0;
		m_position = 0;
		m_mark = 0;
		m_size = 0;
	}

	@Override
	public void compact() {
		final int position = m_position;
		if (position < 1)
			return;

		m_start += position;
		m_size -= position;
		m_position = 0;
		m_mark = 0;
	}

	private static long byteArrayOffset(long index) {
		return BYTE_ARRAY_BASE_OFFSET + index;
	}
}
