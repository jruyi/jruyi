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
package org.jruyi.io;

import java.nio.ByteBuffer;

import org.jruyi.common.IByteSequence;

/**
 * A buffer unit, which contains a linear, finite sequence of {@code byte}s and
 * has the following 4 properties: <i>start</i>, <i>size</i>, <i>position</i>
 * and <i>mark</i>.
 * <ul>
 * <li><i>start</i> is the index of the first byte of the byte sequence this
 * unit represents. It is never negative and is always smaller than
 * {@code capacity()}.
 * <li><i>size</i> is the number of {@code byte}s that the underlying buffer
 * contains. It is never negative.
 * <li><i>position</i> is the offset (to <i>start</i>) of the next {@code byte}
 * to be read. It is never negative and is never greater than <i>size</i>.
 * <li><i>mark</i> is the offset (to <i>start</i>) to which <i>position</i> will
 * be reset when the method {@link #reset reset} is invoked. It is never
 * negative and is never greater than <i>position</i>. Its initial value is
 * {@code 0}.
 * </ul>
 *
 * <p>
 * <B>Note</B>
 * </p>
 * <p>
 * All operations defined in this interface may not do bounds checking. So
 * caller/consumer has to do bounds checking when consuming this interface.
 * </p>
 * 
 * @see IUnitChain
 * @see IBuffer
 * @see IBufferFactory
 */
public interface IUnit extends IByteSequence {

	/**
	 * Sets the byte at the specified {@code index} to the specified {@code b}.
	 * 
	 * @param index
	 *            the index of the byte to be set
	 * @param b
	 *            the byte to set
	 * @return this buffer unit
	 */
	public IUnit set(int index, byte b);

	/**
	 * Sets the bytes starting at the specified {@code index} to the ones
	 * encoded from the specified {@code short} value {@code s} in the native
	 * byte order.
	 *
	 * @param index
	 *            the index of the first byte to be set
	 * @param s
	 *            the {@code short} value to be encoded
	 * @return this buffer unit
	 * @since 2.0
	 */
	public IUnit set(int index, short s);

	/**
	 * Sets the bytes starting at the specified {@code index} to the ones
	 * encoded from the specified {@code int} value {@code i} in the native byte
	 * order.
	 * 
	 * @param index
	 *            the index of the first byte to be set
	 * @param i
	 *            the {@code int} value to be encoded
	 * @return this buffer unit
	 * @since 2.0
	 */
	public IUnit set(int index, int i);

	/**
	 * Sets the bytes starting at the specified {@code index} to the ones
	 * encoded from the specified {@code long} value {@code l} in the native
	 * byte order.
	 * 
	 * @param index
	 *            the index of the first byte to be set
	 * @param l
	 *            the {@code long} value to be encoded
	 * @return this buffer unit
	 * @since 2.0
	 */
	public IUnit set(int index, long l);

	/**
	 * Decodes 2 bytes starting at the specified {@code index} from the
	 * underlying buffer into a {@code short} value in the native byte order and
	 * returns the resultant {@code short} value.
	 * 
	 * @param index
	 *            the index of the first byte to be decoded
	 * @return the resultant {@code short} value
	 * @since 2.0
	 */
	public short getShort(int index);

	/**
	 * Decodes 4 bytes starting at the specified {@code index} from the
	 * underlying buffer into an {@code int} value in the native byte order and
	 * returns the resultant {@code int} value.
	 *
	 * @param index
	 *            the index of the first byte to be decoded
	 * @return the resultant {@code int} value
	 * @since 2.0
	 */
	public int getInt(int index);

	/**
	 * Decodes 8 bytes starting at the specified {@code index} from the
	 * underlying buffer into a {@code long} value in the native byte order and
	 * returns the resultant {@code long} value.
	 *
	 * @param index
	 *            the index of the first byte to be decoded
	 * @return the resultant {@code long} value
	 * @since 2.0
	 */
	public long getLong(int index);

	/**
	 * Sets {@code (srcEnd - srcBegin)} bytes starting at the specified
	 * {@code index} to the ones contained in the given byte sequence
	 * {@code src}, starting at the specified {@code srcBegin}.
	 * 
	 * @param index
	 *            the offset of the first byte to be set
	 * @param src
	 *            the bytes from which to set
	 * @param srcBegin
	 *            the offset of the byte in {@code src} to set as the first byte
	 * @param srcEnd
	 *            the offset of the byte in {@code src} to set as the last byte
	 * @return this buffer unit
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or not smaller than
	 *             {@code capacity()}, minus {@code (srcEnd - srcBegin)}, or
	 *             {@code srcBegin} is negative, or {@code srcEnd} is smaller
	 *             than {@code srcBegin}, or {@code srcEnd} is greater than
	 *             {@code src.length()}
	 */
	public IUnit set(int index, IByteSequence src, int srcBegin, int srcEnd);

	/**
	 * Sets {@code length} bytes starting at the specified {@code index} to the
	 * ones contained in the given byte array {@code src}, starting at the
	 * specified {@code offset}.
	 * 
	 * @param index
	 *            the offset of the first byte to be set
	 * @param src
	 *            the bytes from which to set
	 * @param offset
	 *            the offset of the byte in {@code src} to set as the first byte
	 * @param length
	 *            the number of bytes from {@code src} to set
	 * @return this buffer unit
	 */
	public IUnit set(int index, byte[] src, int offset, int length);

	/**
	 * Sets all the {@code count} bytes starting at the specified {@code index}
	 * to the specified byte {@code b}.
	 * 
	 * @param index
	 *            the offset of the first byte to be set
	 * @param b
	 *            the byte to set
	 * @param count
	 *            the number of bytes to set
	 * @return this buffer unit
	 */
	public IUnit setFill(int index, byte b, int count);

	/**
	 * Returns the index of the first byte of the byte sequence this buffer unit
	 * represents.
	 * 
	 * @return the <i>start</i>
	 */
	public int start();

	/**
	 * Sets the <i>start</i> of this buffer unit to the specified {@code start}.
	 * 
	 * @param start
	 *            the index to set as <i>start</i>
	 */
	public void start(int start);

	/**
	 * Returns the <i>position</i> of this buffer unit.
	 * 
	 * @return the <i>position</i>
	 */
	public int position();

	/**
	 * Sets the <i>position</i> of this buffer unit to the specified
	 * {@code position}.
	 * 
	 * @param position
	 *            the position to set
	 */
	public void position(int position);

	/**
	 * Returns the <i>size</i> of this buffer unit.
	 * 
	 * @return the <i>size</i>
	 */
	public int size();

	/**
	 * Sets the <i>size</i> of this buffer unit to the specified {@code size}.
	 * 
	 * @param size
	 *            the size to set
	 */
	public void size(int size);

	/**
	 * Returns the number of bytes remaining in this buffer unit to be read.
	 * 
	 * @return the number of bytes remaining in this buffer unit
	 */
	public int remaining();

	/**
	 * Returns the number of bytes available in this buffer unit to be written.
	 * 
	 * @return the number of bytes available in this buffer unit
	 */
	public int available();

	/**
	 * Returns the number of bytes that this buffer unit can hold.
	 * 
	 * @return the capacity of this buffer unit
	 */
	public int capacity();

	/**
	 * Returns the <i>mark</i> of this buffer unit.
	 * 
	 * @return the mark of this buffer unit
	 */
	public int mark();

	/**
	 * Sets the <i>mark</i> of this buffer unit to the given position.
	 * 
	 * @param mark
	 *            the mark to set
	 */
	public void mark(int mark);

	/**
	 * Tests if this buffer unit can be appended more.
	 * 
	 * @return true if appendable, otherwise false
	 */
	public boolean appendable();

	/**
	 * Tests if this buffer unit can be prepended more.
	 * 
	 * @return true if prependable, otherwise false
	 */
	public boolean prependable();

	/**
	 * Tests if this buffer unit is empty.
	 * 
	 * @return true if empty, otherwise false
	 */
	public boolean isEmpty();

	/**
	 * Resets this buffer unit's position to the previously-marked position.
	 */
	public void reset();

	/**
	 * Rewinds this buffer unit. The position and mark are set to 0.
	 */
	public void rewind();

	/**
	 * Clears this buffer unit. The start, position, mark and size are all set
	 * to 0.
	 */
	public void clear();

	/**
	 * Compacts this buffer unit by dropping all the data before the current
	 * <i>position</i>.
	 * 
	 * <p>
	 * This buffer unit's start is set to {@code start + position}. Its size is
	 * set to {@code remaining()}. Its position and mark are set to zero.
	 */
	public void compact();

	/**
	 * Skips over the next {@code Math.min(n, remaining())} bytes. If {@code n}
	 * is negative, no bytes are skipped.
	 * 
	 * <p>
	 * If m is the actual number of bytes are skipped, then <i>position</i> is
	 * incremented by m.
	 * 
	 * @param n
	 *            the number of bytes to be skipped
	 * @return the actual number of bytes skipped
	 */
	public int skip(int n);

	/**
	 * Returns the {@code ByteBuffer} associated to this buffer unit, with limit
	 * being this unit's {@code (start + size)} and position being this unit's
	 * {@code (start + position)}.
	 * 
	 * @return the {@code ByteBuffer} as described
	 */
	public ByteBuffer getByteBufferForRead();

	/**
	 * Returns the {@code ByteBuffer} associated to this buffer unit, with limit
	 * being this unit's {@code (start + Math.min((offset + length), size))} and
	 * position being this unit's {@code (start + offset)}.
	 * 
	 * @param offset
	 *            the offset, to buffer unit's start, of the first byte to be
	 *            read in the returned {@code ByteBuffer}
	 * @param length
	 *            the number of bytes available to be read in the returned
	 *            {@code ByteBuffer}
	 * @return the {@code ByteBuffer} as described
	 */
	public ByteBuffer getByteBufferForRead(int offset, int length);

	/**
	 * Returns the {@code ByteBuffer} associated to this buffer unit, with limit
	 * being this buffer unit's {@code capacity} and position being this buffer
	 * unit's {@code (start + size)}.
	 * 
	 * @return the {@code ByteBuffer} as described
	 */
	public ByteBuffer getByteBufferForWrite();

	/**
	 * Copies the requested sequence of bytes to the given {@code dst}.
	 *
	 * @param srcBegin
	 *            start copying at this offset.
	 * @param srcEnd
	 *            stop copying at this offset.
	 * @param dst
	 *            the {@code ByteBuffer} to copy the data into.
	 * @throws NullPointerException
	 *             if {@code dst} is {@code null}.
	 * @since 2.0
	 */
	public void getBytes(int srcBegin, int srcEnd, ByteBuffer dst);

	/**
	 * Sets {@code length} bytes starting at the specified {@code index} to the
	 * ones contained in the given byte buffer {@code src}.
	 *
	 * @param index
	 *            the offset of the first byte to be set
	 * @param length
	 *            the number of bytes from {@code src} to set
	 * @param src
	 *            the bytes from which to set
	 * @return this buffer unit
	 * @throws NullPointerException
	 *             if {@code src} is {@code null}.
	 */
	public IUnit set(int index, int length, ByteBuffer src);
}
