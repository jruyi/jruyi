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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.jruyi.common.ByteKmp;
import org.jruyi.common.IByteSequence;
import org.jruyi.common.ICloseable;
import org.jruyi.common.IDumpable;

/**
 * A byte buffer, which contains a linear, finite sequence of {@code byte}s and
 * has the following properties: <i>size</i>, <i>position</i> and <i>mark</i>.
 * <ul>
 * <li><i>size</i> is the number of {@code byte}s that the underlying buffer
 * contains. It is never negative.
 * <li><i>position</i> is the index of the next {@code byte} to be read. It is
 * never negative and is never greater than <i>size</i>.
 * <li><i>mark</i> is the index to which <i>position</i> will be reset when the
 * method {@link #reset reset} is invoked. It is never negative and is never
 * greater than <i>position</i>. Its initial value is {@code 0}.
 * </ul>
 * <p>
 * A {@code IBuffer} cannot be used anymore after it's closed. Otherwise, the
 * behavior is undefined.
 * 
 * @see IBufferFactory
 * @see IUnit
 * @see IUnitChain
 */
public interface IBuffer extends Comparable<IBuffer>, IByteSequence, IDumpable, ICloseable {

	/**
	 * Returns the <i>position</i> of the underlying buffer
	 * 
	 * @return the <i>position</i> of the underlying buffer
	 */
	int position();

	/**
	 * Returns the number of bytes contained in the underlying buffer.
	 * 
	 * @return the number of bytes contained in the underlying buffer
	 */
	int size();

	/**
	 * Returns the number of bytes remaining in the underlying buffer.
	 * 
	 * @return the number of bytes remaining in the underlying buffer
	 */
	int remaining();

	/**
	 * Sets <i>position</i> to the previously marked position.
	 * 
	 * @throws java.nio.InvalidMarkException
	 *             If the mark has not been set
	 */
	void reset();

	/**
	 * Sets <i>position</i> and <i>mark</i> to {@code 0}.
	 */
	void rewind();

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
	int skip(int n);

	/**
	 * Sets <i>mark</i> to the current <i>position</i>
	 */
	void mark();

	/**
	 * Tests whether this buffer is empty.
	 * 
	 * @return {@code true} if the buffer is empty, otherwise {@code false}
	 */
	boolean isEmpty();

	/**
	 * Tests whether this buffer is closed.
	 * 
	 * @return {@code true} if the buffer is closed, otherwise {@code false}
	 */
	boolean isClosed();

	/**
	 * Returns the index of the first occurrence of the specified byte {@code b}
	 * in this byte sequence. If no such byte occurs in this byte sequence, then
	 * {@code -1} is returned.
	 * 
	 * <p>
	 * This method behaves exactly as below.
	 * 
	 * <pre>
	 * indexOf(b, 0)
	 * </pre>
	 * 
	 * @param b
	 *            the byte whose index of the first occurrence is to be returned
	 * @return the index of the first occurrence of the given {@code b} in this
	 *         byte sequence, {@code -1} if the byte does not occur
	 */
	int indexOf(byte b);

	/**
	 * Returns the index of the first occurrence of the specified byte {@code b}
	 * in this byte sequence, starting search at the given {@code fromIndex}. If
	 * no such byte occurs after index {@code fromIndex}(inclusive) in this byte
	 * sequence, then {@code -1} is returned.
	 * 
	 * <p>
	 * There is no restriction on the value of {@code fromIndex}. If it is
	 * negative, it has the same effect as if it were {@code 0}. If it's greater
	 * than or equal to the length of this byte sequence, then {@code -1} is
	 * returned.
	 * 
	 * @param b
	 *            the byte whose index of the first occurrence is to be returned
	 * @param fromIndex
	 *            the index to start the search from
	 * @return the index of the first occurrence of the given {@code b} after
	 *         {@code fromIndex}(inclusive) in this byte sequence, {@code - 1}
	 *         if the byte does not occur
	 */
	int indexOf(byte b, int fromIndex);

	/**
	 * Returns the index of the first occurrence of the specified {@code bytes}
	 * in this byte sequence. If no such byte array occurs, then {@code -1} is
	 * returned.
	 * 
	 * <p>
	 * This method behaves exactly as below.
	 * 
	 * <pre>
	 * indexOf(bytes, 0)
	 * </pre>
	 * 
	 * @param bytes
	 *            the target byte array
	 * @return the index of the first occurrence of the given {@code bytes} in
	 *         this byte sequence, {@code -1} if the target byte array does not
	 *         occur
	 */
	int indexOf(byte[] bytes);

	/**
	 * Returns the index of the first occurrence of the specified {@code bytes}
	 * in this byte sequence, starting search at the given {@code fromIndex}. If
	 * no such byte array occurs, then {@code -1} is returned.
	 * 
	 * <p>
	 * There is no restriction on the value of {@code fromIndex}. If it is
	 * negative, it has the same effect as if it were {@code 0}. If it's greater
	 * than or equal to the length of this byte sequence, then {@code -1} is
	 * returned.
	 * 
	 * @param bytes
	 *            the target byte array
	 * @param fromIndex
	 *            the index to start the search from
	 * @return the index of the first occurrence of the given {@code bytes}
	 *         after {@code fromIndex}(inclusive) in this byte sequence,
	 *         {@code -1} if the target byte array does not occur
	 */
	int indexOf(byte[] bytes, int fromIndex);

	/**
	 * Returns the index within this sequence of the first occurrence of the
	 * specified {@code pattern}. If no such subsequence exists, then {@code -1}
	 * is returned.
	 * 
	 * @param pattern
	 *            the KMP pattern holding the subsequence for which to search
	 * @return if the given {@code pattern} occurs as a subsequence within this
	 *         sequence, then the index of the first character of the first such
	 *         subsequence is returned; if it does not occur as a subsequence,
	 *         {@code - 1} is returned
	 * @throws NullPointerException
	 *             if {@code pattern} is {@code null}
	 */
	int indexOf(ByteKmp pattern);

	/**
	 * Returns the index within this sequence of the first occurrence of the
	 * specified {@code pattern}, starting at the specified {@code fromIndex}.
	 * If no such subsequence exists, then {@code -1} is returned.
	 * 
	 * @param pattern
	 *            the KMP pattern holding the subsequence for which to search
	 * @param fromIndex
	 *            the index from which to start the search
	 * @return if the given {@code pattern} occurs as a subsequence within this
	 *         sequence, then the index of the first character of the first such
	 *         subsequence is returned; if it does not occur as a subsequence,
	 *         {@code - 1} is returned
	 * @throws NullPointerException
	 *             if {@code pattern} is {@code null}
	 */
	int indexOf(ByteKmp pattern, int fromIndex);

	/**
	 * Returns the index of the last occurrence of the specified byte {@code b}
	 * in this byte sequence, searching backward starting at
	 * {@code (length() - 1)}. If no such byte occurs, then {@code -1} is
	 * returned.
	 * 
	 * <p>
	 * This method behaves exactly as below.
	 * 
	 * <pre>
	 * lastIndexOf(b, length() - 1)
	 * </pre>
	 * 
	 * @param b
	 *            the byte whose index of the last occurrence is to be returned
	 * @return the index of the last occurrence of the given {@code b} in this
	 *         byte sequence, {@code -1} if the byte does not occur
	 */
	int lastIndexOf(byte b);

	/**
	 * Returns the index of the last occurrence of the specified byte {@code b}
	 * in this byte sequence, searching backward starting at the specified
	 * {@code fromIndex}. If no such byte occurs before index {@code fromIndex}
	 * (inclusive) in this byte sequence, then {@code -1} is returned.
	 * 
	 * <p>
	 * There is no restriction on the value of {@code fromIndex}. If it is
	 * greater than or equal to the length of this byte sequence, then the
	 * entire byte sequence would be searched. If it is negative, then it has
	 * the same effect as if it were {@code -1}: {@code -1} is returned.
	 * 
	 * @param b
	 *            the byte whose index of the last occurrence is to be returned
	 * @param fromIndex
	 *            the index to start the backward search from
	 * @return the index of the last occurrence of the given {@code b} before
	 *         {@code fromIndex}(inclusive) in this byte sequence, {@code -1} if
	 *         the byte does not occur
	 */
	int lastIndexOf(byte b, int fromIndex);

	/**
	 * Returns the index of the last occurrence of the specified byte array
	 * {@code bytes} in this byte sequence, searching backward starting at
	 * {@code (length() - bytes.length)}. If no such byte array occurs, then
	 * {@code -1} is returned.
	 * 
	 * @param bytes
	 *            the byte array whose index of the last occurrence is to be
	 *            returned
	 * @return the index of the last occurrence of the given {@code bytes} in
	 *         this byte sequence, {@code -1} if the given byte sequence does
	 *         not occur
	 */
	int lastIndexOf(byte[] bytes);

	/**
	 * Returns the index of the last occurrence of the specified byte array
	 * {@code bytes} in this byte sequence, searching backward starting at the
	 * specified {@code fromIndex}. If no such byte sequence occurs before index
	 * {@code fromIndex}(inclusive) in this byte sequence, then {@code -1} is
	 * returned.
	 * 
	 * @param bytes
	 *            the byte sequence whose index of the last occurrence is to be
	 *            returned
	 * @param fromIndex
	 *            the index to start the backward search from
	 * @return the index of the last occurrence of the given {@code bytes}
	 *         before {@code fromIndex}(inclusive) in this byte sequence,
	 *         {@code -1} if the given byte sequence does not occur
	 */
	int lastIndexOf(byte[] bytes, int fromIndex);

	/**
	 * Returns the index within this sequence of the rightmost occurrence of the
	 * specified subsequence by searching with the KMP algorithm.
	 * 
	 * @param pattern
	 *            the subsequence to search for
	 * @return if the given {@code pattern} occurs one or more times as a
	 *         subsequence within this object, then the index of the first
	 *         character of the last such subsequence is returned. If it does
	 *         not occur as a subsequence, {@code -1} is returned.
	 * @throws NullPointerException
	 *             if {@code pattern} is {@code null}
	 */
	int lastIndexOf(ByteKmp pattern);

	/**
	 * Returns the index of the last occurrence of the specified {@code pattern}
	 * in this byte sequence, searching backward starting at the specified
	 * {@code fromIndex} using the KMP algorithm. If no such byte sequence
	 * occurs in this byte sequence, then {@code -1} is returned.
	 * 
	 * @param pattern
	 *            the subsequence to search for
	 * @param fromIndex
	 *            the index to start the backward search from
	 * @return the index of the last occurrence of the given {@code pattern},
	 *         {@code -1} if the given subsequence does not occur
	 * @throws NullPointerException
	 *             if {@code pattern} is {@code null}
	 */
	int lastIndexOf(ByteKmp pattern, int fromIndex);

	/**
	 * Tests whether this byte sequence starts with the specified {@code bytes}.
	 * 
	 * @param bytes
	 *            the byte sequence to test
	 * @return true if this byte sequence starts with the specified
	 *         {@code bytes}, otherwise false
	 */
	boolean startsWith(byte[] bytes);

	/**
	 * Tests whether this byte sequence ends with the specified {@code bytes}.
	 * 
	 * @param bytes
	 *            the byte sequence to test
	 * @return true if this byte sequence ends with the specified {@code bytes},
	 *         otherwise false
	 */
	boolean endsWith(byte[] bytes);

	/**
	 * Compacts this buffer by dropping all the data before the current
	 * <i>position</i>.
	 * 
	 * <p>
	 * This buffer's position is set to zero. Its size is set to
	 * {@code remaining()}. And its mark is discarded if defined.
	 * 
	 * @return this buffer
	 */
	IBuffer compact();

	/**
	 * Creates a new empty buffer using the same buffer factory instance that
	 * created this buffer.
	 * 
	 * @return a new empty buffer
	 */
	IBuffer newBuffer();

	/**
	 * Splits this buffer into two pieces and returns the first piece. This
	 * buffer holds the rest second piece. The two pieces may share some data,
	 * which means that changes to the content of one of them may be visible to
	 * another one, though they have independent position, size and mark value.
	 * So better use them as read only.
	 * 
	 * @param size
	 *            number of bytes of the first piece
	 * @return the first piece
	 * @throws IllegalArgumentException
	 *             if {@code size} is negative or greater than {@link #length()}
	 */
	IBuffer split(int size);

	/**
	 * Adjusts the length of the buffer to the specified {@code newLength}. If
	 * {@code newLength} is greater than the current length, then the last
	 * {@code newLength - length()} bytes will be filled with zeroes. If
	 * {@code newLength} is smaller than the current length, then the last
	 * {@code length() - newLength} bytes will be dropped.
	 * 
	 * <p>
	 * If the buffer's position is larger than {@code newLength}, then it is set
	 * to {@code newLength}. If the buffer's mark is larger than
	 * {@code newLength}, then it is discarded.
	 * 
	 * @param newLength
	 *            the new length of the buffer to be adjusted to
	 * @return this buffer
	 * @throws IllegalArgumentException
	 *             if {@code newLength} is negative
	 */
	IBuffer setLength(int newLength);

	/**
	 * Empties this buffer.
	 */
	void drain();

	/**
	 * Writes all the remaining data into the end of the given {@code dst}. Then
	 * empties this buffer.
	 * 
	 * @param dst
	 *            the buffer where the data to be transfered to
	 * @throws NullPointerException
	 *             if {@code dst} is null
	 * @throws IllegalArgumentException
	 *             if {@code dst} is this buffer
	 */
	void drainTo(IBuffer dst);

	/**
	 * Reserves the given {@code size} bytes in the head of the buffer for the
	 * efficiency of prepending data. The actual effective reserved size is
	 * returned and may be smaller.
	 * 
	 * @param size
	 *            the number of bytes to be reserved in the head
	 * @return the actual effective size that has been reserved
	 */
	int reserveHead(int size);

	/**
	 * Returns the number of bytes currently reserved for head.
	 * 
	 * @return the current number of bytes reserved for head
	 */
	int headReserved();

	/**
	 * Writes the data read from the specified channel {@code in} to this
	 * buffer.
	 * 
	 * @param in
	 *            the channel to read
	 * @return the number of bytes read, possibly zero
	 * @throws IOException
	 *             if an I/O error occur
	 */
	int readIn(ReadableByteChannel in) throws IOException;

	/**
	 * Writes the data read from this buffer to the given channel {@code out} .
	 * 
	 * @param out
	 *            the channel to write
	 * @return the number of bytes written, possibly zero
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	int writeOut(WritableByteChannel out) throws IOException;

	/**
	 * Writes {@code count} {@code b} values into the underlying buffer. The
	 * <i>size</i> is incremented by {@code count}.
	 * 
	 * @param b
	 *            the byte value to write
	 * @param count
	 *            the number of {@code b} values to write
	 * @return this buffer
	 * @throws IllegalArgumentException
	 *             if {@code count} is negative
	 */
	IBuffer writeFill(byte b, int count);

	/**
	 * Sets all {@code count} bytes starting at the specified {@code start} to
	 * the specified byte {@code b}.
	 * 
	 * @param index
	 *            the offset of the first byte to be set
	 * @param b
	 *            the byte to set
	 * @param count
	 *            the number of bytes to be set
	 * @return this buffer
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than the
	 *             underlying buffer's <i>size</i>, minus {@code count - 1}, or
	 *             {@code count} is negative
	 */
	IBuffer setFill(int index, byte b, int count);

	/**
	 * Writes {@code count} {@code b} values to the head of the underlying
	 * buffer. The <i>size</i> is incremented by {@code count}.
	 * 
	 * @param b
	 *            the byte to write
	 * @param count
	 *            the number of {@code b} values to write
	 * @return this buffer
	 * @throws IllegalArgumentException
	 *             if {@code count} is negative
	 */
	IBuffer prependFill(byte b, int count);

	/**
	 * Zero-extends the {@code byte} value at the specified index to type
	 * {@code int} and returns the result, which is therefore in the range
	 * {@code 0} through {@code 255}.
	 * 
	 * <p>
	 * This method behaves exactly the same way as:
	 * 
	 * <pre>
	 * {@code byteAt(index) & 0xFF}
	 * </pre>
	 * 
	 * @param index
	 *            the index of the byte value
	 * @return the byte at the specified index, interpreted as an unsigned 8-bit
	 *         value
	 * @throws IndexOutOfBoundsException
	 *             if {@code index < 0} or {@code index >= length()}
	 */
	int getUnsignedByte(int index);

	/**
	 * Uses the specified {@code codec} to decode the bytes starting from the
	 * specified {@code index} as an {@code int} value, in the range {@code 0}
	 * through {@code 65535} and returns the result.
	 * 
	 * <p>
	 * This method behaves exactly the same way as:
	 * 
	 * <pre>
	 * {@code get(index, codec) & 0xFFFF}
	 * </pre>
	 * 
	 * @param index
	 *            the starting index of the bytes to be decoded
	 * @param codec
	 *            the codec to decode
	 * @return the decoded unsigned 16-bit value
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is out of bounds
	 */
	int getUnsignedShort(int index, IShortCodec codec);

	/**
	 * Returns the next byte from the underlying buffer as an unsigned 8-bit
	 * value. <i>position</i> is incremented by {@code 1}.
	 * 
	 * <p>
	 * This method behaves exactly the same way as:
	 * 
	 * <pre>
	 * {@code read() & 0xFF}
	 * </pre>
	 * 
	 * @return the next byte read as an unsigned 8-bit value
	 * @throws BufferUnderflowException
	 *             if there are no bytes remaining in the underlying buffer
	 */
	int readUnsignedByte();

	/**
	 * Uses the specified {@code codec} to decode the bytes starting at the
	 * current <i>position</i> from the underlying buffer into an unsigned
	 * 16-bit {@code int} value and returns the result.
	 * 
	 * <p>
	 * This method behaves exactly the same way as:
	 * 
	 * <pre>
	 * {@code read(codec) & 0xFFFF}
	 * </pre>
	 * 
	 * @param codec
	 *            the codec to decode
	 * @return the decoded result
	 * @throws BufferUnderflowException
	 *             if there are not enough remaining bytes to decode
	 */
	int readUnsignedShort(IShortCodec codec);

	/**
	 * Uses the specified {@code decoder} to decode the bytes starting at the
	 * specified {@code index} from the underlying buffer into a {@code char}
	 * value and returns the result.
	 * 
	 * @param index
	 *            the index of the first byte to be interpreted
	 * @param decoder
	 *            the decoder to decode
	 * @return the resultant {@code char} value
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is out of bounds
	 * @since 2.5
	 */
	char get(int index, IGetCharDecoder decoder);

	/**
	 * Uses the specified {@code decoder} to decode the bytes starting at the
	 * specified {@code index} from the underlying buffer into a {@code short}
	 * value and returns the resultant {@code short} value.
	 * 
	 * @param index
	 *            the index of the first byte to be decoded
	 * @param decoder
	 *            the decoder to decode
	 * @return the resultant {@code short} value
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is out of bounds
	 * @since 2.5
	 */
	short get(int index, IGetShortDecoder decoder);

	/**
	 * Uses the specified {@code decoder} to decode the bytes starting at the
	 * specified {@code index} from the underlying buffer into an {@code int}
	 * value and returns the resultant {@code int} value.
	 * 
	 * @param index
	 *            the index of the first byte to be decoded
	 * @param decoder
	 *            the decoder to decode
	 * @return the resultant {@code int} value
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is out of bounds
	 * @since 2.5
	 */
	int get(int index, IGetIntDecoder decoder);

	/**
	 * Uses the specified {@code decoder} to decode the bytes starting at the
	 * specified {@code index} from the underlying buffer into a {@code long}
	 * value and returns the resultant {@code long} value.
	 * 
	 * @param index
	 *            the index of the first byte to be decoded
	 * @param decoder
	 *            the decoder to decode
	 * @return the resultant {@code long} value
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is out of bounds
	 * @since 2.5
	 */
	long get(int index, IGetLongDecoder decoder);

	/**
	 * Uses the specified {@code decoder} to decode the bytes starting at the
	 * specified {@code index} from the underlying buffer into a {@code float}
	 * value and returns the resultant {@code float} value.
	 * 
	 * @param index
	 *            the index of the first byte to be decoded
	 * @param decoder
	 *            the decoder to decode
	 * @return the resultant {@code float} value
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is out of bounds
	 * @since 2.5
	 */
	float get(int index, IGetFloatDecoder decoder);

	/**
	 * Uses the specified {@code decoder} to decode the bytes starting at the
	 * specified {@code index} from the underlying buffer into a {@code double}
	 * value and returns the resultant {@code double} value.
	 * 
	 * @param index
	 *            the index of the first byte to be decoded
	 * @param decoder
	 *            the decoder to decode
	 * @return the resultant {@code double} value
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is out of bounds
	 * @since 2.5
	 */
	double get(int index, IGetDoubleDecoder decoder);

	/**
	 * Uses the specified {@code decoder} to decode the bytes starting at the
	 * specified {@code index} from the underlying buffer into an object of type
	 * {@code T} and returns the resultant object.
	 * 
	 * @param <T>
	 *            the type of the result to decode to
	 * @param index
	 *            the index of the first byte to be decoded
	 * @param decoder
	 *            the decoder to decode
	 * @return the resultant object of type {@code T}
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is out of bounds
	 * @since 2.5
	 */
	<T> T get(int index, IGetDecoder<T> decoder);

	/**
	 * Uses the specified {@code decoder} to decode the bytes, starting at the
	 * specified {@code index} and ending at {@code (index + length)}, from the
	 * underlying buffer into an object of type {@code T} and returns the
	 * resultant object.
	 * 
	 * @param <T>
	 *            the type of the result to decode to
	 * @param index
	 *            the index of the first byte to be decoded
	 * @param length
	 *            the number of bytes to be decoded
	 * @param decoder
	 *            the decoder to decode
	 * @return the resultant object of type {@code T}
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is out of bounds
	 * @since 2.5
	 */
	<T> T get(int index, int length, IGetLimitedDecoder<T> decoder);

	/**
	 * Uses the specified {@code decoder} to decode the bytes starting at the
	 * specified {@code index} from the underlying buffer into the specified
	 * {@code dst}.
	 * 
	 * @param <T>
	 *            the type of result to decode to
	 * @param index
	 *            the index of the first byte to be decoded
	 * @param dst
	 *            the object to hold the decoded result
	 * @param decoder
	 *            the decoder to decode
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index} is out of bounds
	 * @since 2.5
	 */
	<T> void get(int index, T dst, IGetToDstDecoder<T> decoder);

	/**
	 * Uses the specified {@code decoder} to decode the bytes starting at the
	 * specified {@code index} from the underlying buffer into the specified
	 * {@code dst} starting at the specified {@code offset} and ending at
	 * {@code (offset + length)}.
	 * 
	 * @param <T>
	 *            the type of the result to decode to
	 * @param index
	 *            the index of the first byte to be decoded
	 * @param offset
	 *            the start index of the destination
	 * @param length
	 *            the capacity of the destination
	 * @param dst
	 *            the object to hold the decoded result
	 * @param decoder
	 *            the decoder to decode
	 * @throws IndexOutOfBoundsException
	 *             if the specified {@code index, offset or length} is out of
	 *             bounds
	 * @since 2.5
	 */
	<T> void get(int index, T dst, int offset, int length, IGetToRangedDstDecoder<T> decoder);

	/**
	 * Returns the next byte from the underlying buffer. <i>position</i> is
	 * incremented by {@code 1}.
	 * 
	 * @return the next byte read from the underlying buffer
	 * @throws BufferUnderflowException
	 *             if there are no bytes remaining in the underlying buffer
	 */
	byte read();

	/**
	 * Uses the specified {@code decoder} to decode the bytes starting at
	 * <i>position</i> from the underlying buffer into a {@code char} value and
	 * returns the resultant {@code char} value.
	 * 
	 * @param decoder
	 *            the decoder to decode
	 * @return the resultant {@code char} value
	 * @throws BufferUnderflowException
	 *             if there are not enough bytes remained in the underlying
	 *             buffer
	 * @since 2.5
	 */
	char read(IReadCharDecoder decoder);

	/**
	 * Uses the specified {@code decoder} to decode the bytes starting at
	 * <i>position</i> from the underlying buffer into a {@code short} value and
	 * returns the resultant {@code short} value.
	 * 
	 * @param decoder
	 *            the decoder to decode
	 * @return the resultant {@code short} value
	 * @throws BufferUnderflowException
	 *             if there are not enough bytes remained in the underlying
	 *             buffer
	 * @since 2.5
	 */
	short read(IReadShortDecoder decoder);

	/**
	 * Uses the specified {@code decoder} to decode the bytes starting at
	 * <i>position</i> from the underlying buffer into an {@code int} value and
	 * returns the resultant {@code int} value.
	 * 
	 * @param decoder
	 *            the decoder to decode
	 * @return the resultant {@code int} value
	 * @throws BufferUnderflowException
	 *             if there are not enough bytes remained in the underlying
	 *             buffer
	 * @since 2.5
	 */
	int read(IReadIntDecoder decoder);

	/**
	 * Uses the specified {@code decoder} to decode the bytes starting at
	 * <i>position</i> from the underlying buffer into a {@code long} value and
	 * returns the resultant {@code long} value.
	 * 
	 * @param decoder
	 *            the decoder to decode
	 * @return the resultant {@code long} value
	 * @throws BufferUnderflowException
	 *             if there are not enough bytes remained in the underlying
	 *             buffer
	 * @since 2.5
	 */
	long read(IReadLongDecoder decoder);

	/**
	 * Uses the specified {@code decoder} to decode the bytes starting at
	 * <i>position</i> from the underlying buffer into a {@code float} value and
	 * returns the resultant {@code float} value.
	 * 
	 * @param decoder
	 *            the decoder to decode
	 * @return the resultant {@code float} value
	 * @throws BufferUnderflowException
	 *             if there are not enough bytes remained in the underlying
	 *             buffer
	 * @since 2.5
	 */
	float read(IReadFloatDecoder decoder);

	/**
	 * Uses the specified {@code decoder} to decode the bytes starting at
	 * <i>position</i> from the underlying buffer into a {@code double} value
	 * and returns the resultant {@code double} value.
	 * 
	 * @param decoder
	 *            the decoder to decode
	 * @return the resultant {@code double} value
	 * @throws BufferUnderflowException
	 *             if there are not enough bytes remained in the underlying
	 *             buffer
	 * @since 2.5
	 */
	double read(IReadDoubleDecoder decoder);

	/**
	 * Uses the specified {@code decoder} to decode the bytes starting at
	 * <i>position</i> from the underlying buffer into an object of type
	 * {@code T} and returns the resultant object.
	 * 
	 * @param <T>
	 *            the type of the result to decode to
	 * @param decoder
	 *            the decoder to decode
	 * @return the resultant object of type {@code T}
	 * @throws BufferUnderflowException
	 *             if there are not enough bytes remained in the underlying
	 *             buffer
	 * @since 2.5
	 */
	<T> T read(IReadDecoder<T> decoder);

	/**
	 * Uses the specified {@code decoder} to decode the bytes, starting at
	 * <i>position</i> and ending at {@code (position() + length)}, from the
	 * underlying buffer into an object of type {@code T} and returns the
	 * resultant object.
	 * 
	 * @param <T>
	 *            the type of the result to decode to
	 * @param length
	 *            the number bytes to be decoded
	 * @param decoder
	 *            the decoder to decode
	 * @return the resultant object of type {@code T}
	 * @throws IllegalArgumentException
	 *             if the given {@code length} is negative
	 * @throws BufferUnderflowException
	 *             if there are not enough bytes remained in the underlying
	 *             buffer
	 * @since 2.5
	 */
	<T> T read(int length, IReadLimitedDecoder<T> decoder);

	/**
	 * Uses the specified {@code decoder} to decode the bytes, starting at
	 * <i>position</i>, from the underlying buffer into the specified
	 * {@code dst} and returns the actual number of bytes decoded.
	 * 
	 * @param <T>
	 *            the type of the result to decode to
	 * @param dst
	 *            the object to hold the decoded result
	 * @param decoder
	 *            the decoder to decode
	 * @return the actual number of bytes decoded
	 * @since 2.5
	 */
	<T> int read(T dst, IReadToDstDecoder<T> decoder);

	/**
	 * Uses the specified {@code decoder} to decode the bytes, starting at
	 * <i>position</i>, from the underlying buffer into the specified
	 * {@code dst} starting from {@code offset}(inclusive) to
	 * {@code (offset + length)}(exclusive) and returns the actual number of
	 * bytes decoded.
	 * 
	 * @param <T>
	 *            the type of the result to decode to
	 * @param dst
	 *            the object to hold the decoded result
	 * @param offset
	 *            the start index of the destination
	 * @param length
	 *            the capacity of the destination
	 * @param decoder
	 *            the decoder to decode
	 * @return the actual number of bytes decoded
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} or {@code length} doesn't hold the
	 *             condition
	 * @since 2.5
	 */
	<T> int read(T dst, int offset, int length, IReadToRangedDstDecoder<T> decoder);

	/**
	 * Sets the byte at the specified position to the given byte {@code b}.
	 * 
	 * @param index
	 *            the index of the byte to be set
	 * @param b
	 *            the {@code byte} value to set
	 * @return this buffer
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or not smaller than the buffer's
	 *             <i>size</i>
	 */
	IBuffer set(int index, byte b);

	/**
	 * Sets the bytes starting at the specified {@code index} to the ones
	 * encoded from the specified character {@code c} with the specified
	 * {@code encoder}.
	 * 
	 * @param index
	 *            the index of the first byte to be set
	 * @param c
	 *            the character to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is out of bounds
	 * @since 2.5
	 */
	IBuffer set(int index, char c, ISetCharEncoder encoder);

	/**
	 * Sets the bytes starting at the specified {@code index} to the ones
	 * encoded from the specified short value {@code s} with the specified
	 * {@code encoder}.
	 * 
	 * @param index
	 *            the index of the first byte to be set
	 * @param s
	 *            the short value to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is out of bounds
	 * @since 2.5
	 */
	IBuffer set(int index, short s, ISetShortEncoder encoder);

	/**
	 * Sets the bytes starting at the specified {@code index} to the ones
	 * encoded from the specified int value {@code i} with the specified
	 * {@code encoder} .
	 * 
	 * @param index
	 *            the index of the first byte to be set
	 * @param i
	 *            the int value to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is out of bounds
	 * @since 2.5
	 */
	IBuffer set(int index, int i, ISetIntEncoder encoder);

	/**
	 * Sets the bytes starting at the specified {@code index} to the ones
	 * encoded from the specified long value {@code l} with the specified
	 * {@code encoder}.
	 * 
	 * @param index
	 *            the index of the first byte to be set
	 * @param l
	 *            the long value to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is out of bounds
	 * @since 2.5
	 */
	IBuffer set(int index, long l, ISetLongEncoder encoder);

	/**
	 * Sets the bytes starting at the specified {@code index} to the ones
	 * encoded from the specified float value {@code f} with the specified
	 * {@code encoder}.
	 * 
	 * @param index
	 *            the index of the the first byte to be set
	 * @param f
	 *            the float value to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is out of bounds
	 * @since 2.5
	 */
	IBuffer set(int index, float f, ISetFloatEncoder encoder);

	/**
	 * Sets the bytes starting at the specified {@code index} to the ones
	 * encoded from the specified double value {@code d} with the specified
	 * {@code encoder}.
	 * 
	 * @param index
	 *            the index of the the first byte to be set
	 * @param d
	 *            the double value to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is out of bounds
	 * @since 2.5
	 */
	IBuffer set(int index, double d, ISetDoubleEncoder encoder);

	/**
	 * Sets the bytes starting at the specified {@code index} to the ones
	 * encoded from the specified {@code src} of type {@code T} with the
	 * specified {@code encoder}.
	 * 
	 * @param <T>
	 *            the type of the target to be encoded
	 * @param index
	 *            the index of the the first byte to be set
	 * @param src
	 *            the object of type {@code T} to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is out of bounds
	 * @since 2.5
	 */
	<T> IBuffer set(int index, T src, ISetEncoder<T> encoder);

	/**
	 * Sets the bytes starting at the specified {@code index} to the ones
	 * encoded from the specified collection {@code src}, starting at the
	 * specified {@code offset}, ending at {@code (offset + length)} with the
	 * specified {@code encoder}.
	 * 
	 * @param <T>
	 *            the type of the target to be encoded
	 * @param index
	 *            the index of the first byte to be set
	 * @param src
	 *            the object to be encoded
	 * @param offset
	 *            the offset of the first element to be encoded
	 * @param length
	 *            the number of elements to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is out of bounds
	 * @since 2.5
	 */
	<T> IBuffer set(int index, T src, int offset, int length, ISetRangedEncoder<T> encoder);

	/**
	 * Writes the given byte {@code b} to the end of this buffer. The
	 * <i>size</i> is incremented by {@code 1}.
	 * 
	 * @param b
	 *            the {@code byte} to be written
	 * @return this buffer
	 */
	IBuffer write(byte b);

	/**
	 * Writes the bytes, encoded from the specified char value {@code c} with
	 * the specified {@code encoder}, to the end of this buffer.
	 * 
	 * @param c
	 *            the char value to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @since 2.5
	 */
	IBuffer write(char c, IWriteCharEncoder encoder);

	/**
	 * Writes the bytes, encoded from the specified short value {@code s} with
	 * the specified {@code encoder}, to the end of this buffer.
	 * 
	 * @param s
	 *            the short value to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @since 2.5
	 */
	IBuffer write(short s, IWriteShortEncoder encoder);

	/**
	 * Writes the bytes, encoded from the specified int value {@code i} with the
	 * specified {@code encoder}, to the end of this buffer.
	 * 
	 * @param i
	 *            the int value to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @since 2.5
	 */
	IBuffer write(int i, IWriteIntEncoder encoder);

	/**
	 * Writes the bytes, encoded from the specified long value {@code l} with
	 * the specified {@code encoder}, to the end of this buffer.
	 * 
	 * @param l
	 *            the long value to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @since 2.5
	 */
	IBuffer write(long l, IWriteLongEncoder encoder);

	/**
	 * Writes the bytes, encoded from the specified float value {@code f} with
	 * the specified {@code encoder}, to the end of this buffer.
	 * 
	 * @param f
	 *            the float value to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @since 2.5
	 */
	IBuffer write(float f, IWriteFloatEncoder encoder);

	/**
	 * Writes the bytes, encoded from the specified double value {@code d} with
	 * the specified {@code encoder}, to the end of this buffer.
	 * 
	 * @param d
	 *            the double value to be encoded
	 * @param encoder
	 *            the encoder to encoded
	 * @return this buffer
	 * @since 2.5
	 */
	IBuffer write(double d, IWriteDoubleEncoder encoder);

	/**
	 * Writes the bytes, encoded from the specified object {@code src} with the
	 * specified {@code encoder}, to the end of this buffer.
	 * 
	 * @param <T>
	 *            the type of the target to be encoded
	 * @param src
	 *            the object to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @since 2.5
	 */
	<T> IBuffer write(T src, IWriteEncoder<T> encoder);

	/**
	 * Writes the bytes, encoded from the specified collection {@code src}
	 * starting at the specified {@code offset} ending at the specified
	 * {@code (offset + length)} with the specified {@code encoder}, to the end
	 * of this buffer.
	 * 
	 * @param <T>
	 *            the type of the target to be encoded
	 * @param src
	 *            the collection to be encoded
	 * @param offset
	 *            the offset of the first element to be encoded
	 * @param length
	 *            the number of elements to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @since 2.5
	 */
	<T> IBuffer write(T src, int offset, int length, IWriteRangedEncoder<T> encoder);

	/**
	 * Writes the given byte {@code b} to the head of the buffer.
	 * 
	 * <p>
	 * The <i>size</i> is incremented by {@code 1}.
	 * 
	 * @param b
	 *            the {@code byte} value to write
	 * @return this buffer
	 */
	IBuffer prepend(byte b);

	/**
	 * Writes the bytes, encoded from the specified character {@code c} with the
	 * specified {@code encoder}, to the head of this buffer.
	 * 
	 * @param c
	 *            the character to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @since 2.5
	 */
	IBuffer prepend(char c, IPrependCharEncoder encoder);

	/**
	 * Writes the bytes, encoded from the specified short value {@code s} with
	 * the specified {@code encoder}, to the head of this buffer.
	 * 
	 * @param s
	 *            the short value to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @since 2.5
	 */
	IBuffer prepend(short s, IPrependShortEncoder encoder);

	/**
	 * Writes the bytes, encoded from the specified int value {@code i} with the
	 * specified {@code encoder}, to the head of this buffer.
	 * 
	 * @param i
	 *            the int value to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @since 2.5
	 */
	IBuffer prepend(int i, IPrependIntEncoder encoder);

	/**
	 * Writes the bytes, encoded from the specified long value {@code l} with
	 * the specified {@code encoder}, to the head of this buffer.
	 * 
	 * @param l
	 *            the long value to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @since 2.5
	 */
	IBuffer prepend(long l, IPrependLongEncoder encoder);

	/**
	 * Writes the bytes, encoded from the specified float value {@code f} with
	 * the specified {@code encoder}, to the head of this buffer.
	 * 
	 * @param f
	 *            the float value to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @since 2.5
	 */
	IBuffer prepend(float f, IPrependFloatEncoder encoder);

	/**
	 * Writes the bytes, encoded from the specified double value {@code d} with
	 * the specified {@code encoder}, to the head of this buffer.
	 * 
	 * @param d
	 *            the double value to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @since 2.5
	 */
	IBuffer prepend(double d, IPrependDoubleEncoder encoder);

	/**
	 * Writes the bytes, encoded from the specified object {@code src} with the
	 * specified {@code encoder}, to the head of this buffer.
	 * 
	 * @param <T>
	 *            the type of the target to be encoded
	 * @param src
	 *            the object to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @since 2.5
	 */
	<T> IBuffer prepend(T src, IPrependEncoder<T> encoder);

	/**
	 * Writes the bytes, encoded from the specified collection {@code src} with
	 * the specified {@code encoder} starting at {@code offset} ending at
	 * {@code (offset + length)}, to the head of this buffer.
	 * 
	 * @param <T>
	 *            the type of the target to be encoded
	 * @param src
	 *            the collection to be encoded
	 * @param offset
	 *            the index of the first element to be encoded
	 * @param length
	 *            the number of elements to be encoded
	 * @param encoder
	 *            the encoder to encode
	 * @return this buffer
	 * @since 2.5
	 */
	<T> IBuffer prepend(T src, int offset, int length, IPrependRangedEncoder<T> encoder);

	/**
	 * Returns an {@code OutputStream} object that represents this buffer.
	 * Operations made on either one of them will be reflected in another one.
	 * 
	 * @return an {@code OutputStream} object that represents this buffer
	 */
	OutputStream getOutputStream();

	/**
	 * Returns an {@code InputStream} object that represents this buffer.
	 * Operations made on either one of them will be reflected in another one.
	 * 
	 * @return an {@code InputStream} object that represents this buffer
	 */
	InputStream getInputStream();

	/**
	 * Creates a new buffer that shares this buffer's content.
	 *
	 * <p>
	 * The content of the new buffer will start at this buffer's current
	 * position. Changes to this buffer's content may be visible in the new
	 * buffer, and vice versa. The two buffers' size, position and mark values
	 * will be independent.
	 *
	 * <p>
	 * The new buffer's position will be zero, its size will be the number of
	 * bytes remaining in this buffer, and its mark will be undefined.
	 *
	 * @return the new buffer
	 * @since 2.2
	 */
	IBuffer slice();

	/**
	 * Creates a new buffer that shares this buffer's content.
	 *
	 * <p>
	 * The content of the new buffer will be the same as this buffer. Changes to
	 * this buffer's content may be visible in the new buffer, and vice versa.
	 * The two buffers' size, position and mark values will be independent.
	 *
	 * <p>
	 * The new buffer's size, position and mark values will be identical to
	 * those of this buffer.
	 *
	 * @return the new buffer
	 * @since 2.2
	 */
	IBuffer duplicate();
}
