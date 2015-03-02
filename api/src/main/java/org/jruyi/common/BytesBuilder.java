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

package org.jruyi.common;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * A utility class for building a byte array.
 * <p>
 * The static methods {@code get} are used to get the instance of this class. If
 * there are some instances currently available in the thread local cache, one
 * will be returned. Otherwise, a new instance will be created and returned. The
 * method {@code release} is used to recycle this instance into the local cache
 * of the current thread for it can be reused in this thread before being GC'ed.
 */
public final class BytesBuilder implements Serializable, IByteSequence, ICloseable {

	private static final long serialVersionUID = -3381784872368302825L;
	private static final int DEFAULT_CAPACITY = 256;
	private static final IThreadLocalCache<BytesBuilder> c_cache = ThreadLocalCache.weakArrayCache();
	private byte[] m_value;
	private int m_length;
	private transient ByteBuffer m_byteBuffer;

	private BytesBuilder() {
		this(DEFAULT_CAPACITY);
	}

	private BytesBuilder(int capacity) {
		m_value = new byte[capacity];
	}

	/**
	 * Gets a bytes builder with a minimum capacity of {@code 256}.
	 * 
	 * @return a {@code BytesBuilder} object.
	 * @see #capacity()
	 */
	public static BytesBuilder get() {
		BytesBuilder builder = c_cache.take();
		if (builder == null)
			builder = new BytesBuilder();
		return builder;
	}

	/**
	 * Gets a bytes builder with the specified minimum capacity.
	 * 
	 * @param capacity
	 *            the desired minimum capacity.
	 * @return a {@code BytesBuilder} object.
	 * @throws NegativeArraySizeException
	 *             if the {@code capacity} argument is less than {@code 0}.
	 */
	public static BytesBuilder get(int capacity) {
		BytesBuilder builder = c_cache.take();
		if (builder == null)
			builder = new BytesBuilder(capacity);
		else
			builder.ensureCapacity(capacity);

		return builder;
	}

	/**
	 * Releases this bytes builder to the thread local cache so that it can be
	 * reused before being GC'ed.
	 * 
	 * <p>
	 * The reference to this byte builder must not be used anymore after this
	 * method is called. Otherwise, the behavior is undefined.
	 */
	@Override
	public void close() {
		m_length = 0;
		c_cache.put(this);
	}

	/**
	 * Returns the current length.
	 * 
	 * @return the number of bytes currently held in this {@code BytesBuilder}
	 */
	@Override
	public int length() {
		return m_length;
	}

	/**
	 * Returns the number of bytes that can be held without growing.
	 * 
	 * @return the current capacity
	 */
	public int capacity() {
		return m_value.length;
	}

	/**
	 * Ensures that this object has a minimum capacity available before
	 * requiring the underlying buffer to be expanded. The general policy of
	 * this method is that if the {@code minCapacity} is greater than the
	 * current {@link #capacity()}, then the capacity will be expanded to the
	 * larger value of either the {@code minCapacity} or the current capacity
	 * multiplied by two plus two. Although this is the general policy, there is
	 * no guarantee that the capacity will change.
	 * 
	 * @param minCapacity
	 *            the minimum desired capacity
	 */
	public void ensureCapacity(int minCapacity) {
		if (minCapacity > m_value.length)
			expandCapacity(minCapacity);
	}

	/**
	 * Trims off any extra capacity beyond the current length.
	 */
	public void trimToSize() {
		if (m_length < m_value.length) {
			byte[] value = new byte[m_length];
			System.arraycopy(m_value, 0, value, 0, value.length);
			m_value = value;
		}
	}

	/**
	 * Sets the current length to the specified {@code newLength}. If the new
	 * length is greater than the current length, then the new bytes at the end
	 * of this object are filled with {@code 0}.
	 * 
	 * @param newLength
	 *            the new length
	 * @throws IndexOutOfBoundsException
	 *             if the {@code newLength} argument is negative
	 */
	public void setLength(int newLength) {
		if (newLength < 0)
			throw new IndexOutOfBoundsException();

		if (newLength > m_value.length)
			expandCapacity(newLength);

		m_length = newLength;
	}

	@Override
	public byte byteAt(int index) {
		if (index >= m_length)
			throw new IndexOutOfBoundsException();

		return m_value[index];
	}

	/**
	 * Tests whether this byte sequence ends with the specified {@code bytes}.
	 * 
	 * @param bytes
	 *            the byte sequence to test
	 * @return true if this byte sequence ends with the specified {@code bytes},
	 *         otherwise false
	 */
	public boolean endsWith(byte[] bytes) {
		int i = bytes.length;
		int j = m_length;
		if (i > j)
			return false;

		byte[] value = m_value;
		while (i > 0) {
			if (bytes[--i] != value[--j])
				return false;
		}

		return true;
	}

	@Override
	public byte[] getBytes(int index) {
		int length = m_length;
		if (index > length)
			throw new IndexOutOfBoundsException();

		length -= index;
		byte[] copy = new byte[length];
		System.arraycopy(m_value, index, copy, 0, length);
		return copy;
	}

	@Override
	public byte[] getBytes(int index, int length) {
		if (length < 0 || index + length > m_length)
			throw new IndexOutOfBoundsException();

		byte[] copy = new byte[length];
		System.arraycopy(m_value, index, copy, 0, length);
		return copy;
	}

	/**
	 * Interprets the 8 bytes starting at the specified {@code start} into a
	 * {@code double} value in the big-endian byte order and returns the
	 * resultant {@code double} value.
	 * 
	 * @param start
	 *            the index of the first byte to be interpreted
	 * @return the resultant {@code double} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than
	 *             {@code length()}, minus {@code 8}
	 */
	public double getDoubleB(int start) {
		return Double.longBitsToDouble(getLongB(start));
	}

	/**
	 * Interprets the 8 bytes starting at the specified {@code start} into a
	 * {@code double} value in the little-endian byte order and returns the
	 * resultant {@code double} value.
	 * 
	 * @param start
	 *            the index of the first byte to be interpreted
	 * @return the resultant {@code double} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than
	 *             {@code length()}, minus {@code 8}
	 */
	public double getDoubleL(int start) {
		return Double.longBitsToDouble(getLongL(start));
	}

	/**
	 * Interprets the 4 bytes starting at the specified {@code start} into a
	 * {@code float} value in the big-endian byte order and returns the
	 * resultant {@code float} value.
	 * 
	 * @param start
	 *            the index of the first byte to be interpreted
	 * @return the resultant {@code float} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than
	 *             {@code length()}, minus {@code 4}
	 */
	public float getFloatB(int start) {
		return Float.intBitsToFloat(getIntB(start));
	}

	/**
	 * Interprets the 4 bytes starting at the specified {@code start} into a
	 * {@code float} value in the little-endian byte order and returns the
	 * resultant {@code float} value.
	 * 
	 * @param start
	 *            the index of the first byte to be interpreted
	 * @return the resultant {@code float} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than
	 *             {@code length()}, minus {@code 4}
	 */
	public float getFloatL(int start) {
		return Float.intBitsToFloat(getIntL(start));
	}

	/**
	 * Interprets the 4 bytes starting at the specified {@code start} into an
	 * {@code int} value in the big-endian byte order and returns the resultant
	 * {@code int} value.
	 * 
	 * @param start
	 *            the index of the first byte to be interpreted
	 * @return the resultant {@code int} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than
	 *             {@code length()}, minus {@code 4}
	 */
	public int getIntB(int start) {
		if (start > m_length - 4)
			throw new IndexOutOfBoundsException();

		byte[] v = m_value;
		int i = v[start] << 24;
		i |= ((v[++start] & 0xFF) << 16);
		i |= ((v[++start] & 0xFF) << 8);
		i |= (v[++start] & 0xFF);

		return i;
	}

	/**
	 * Interprets the 4 bytes starting at the specified {@code start} into an
	 * {@code int} value in the little-endian byte order and returns the
	 * resultant {@code int} value.
	 * 
	 * @param start
	 *            the index of the first byte to be interpreted
	 * @return the resultant {@code int} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than
	 *             {@code length()}, minus {@code 4}
	 */
	public int getIntL(int start) {
		if (start > m_length - 4)
			throw new IndexOutOfBoundsException();

		byte[] v = m_value;
		int i = v[start] & 0xFF;
		i |= ((v[++start] & 0xFF) << 8);
		i |= ((v[++start] & 0xFF) << 16);
		i |= ((v[++start] & 0xFF) << 24);

		return i;
	}

	/**
	 * Interprets the 8 bytes starting at the specified {@code start} into a
	 * {@code long} value in the big-endian byte order and returns the resultant
	 * {@code long} value.
	 * 
	 * @param start
	 *            the index of the first byte to be interpreted
	 * @return the resultant {@code long} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than
	 *             {@code length()}, minus {@code 8}
	 */
	public long getLongB(int start) {
		if (start > m_length - 8)
			throw new IndexOutOfBoundsException();

		byte[] v = m_value;
		long l = ((long) v[start]) << 56;
		l |= (((long) v[++start] & 0xFF) << 48);
		l |= (((long) v[++start] & 0xFF) << 40);
		l |= (((long) v[++start] & 0xFF) << 32);
		l |= (((long) v[++start] & 0xFF) << 24);
		l |= (((long) v[++start] & 0xFF) << 16);
		l |= (((long) v[++start] & 0xFF) << 8);
		l |= ((long) v[++start] & 0xFF);

		return l;
	}

	/**
	 * Interprets the 8 bytes starting at the specified {@code start} into a
	 * {@code long} value in the little-endian byte order and returns the
	 * resultant {@code long} value.
	 * 
	 * @param start
	 *            the index of the first byte to be interpreted
	 * @return the resultant {@code long} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than
	 *             {@code length()}, minus {@code 8}
	 */
	public long getLongL(int start) {
		if (start > m_length - 8)
			throw new IndexOutOfBoundsException();

		byte[] v = m_value;
		long l = v[start] & 0xFF;
		l |= (((long) v[++start] & 0xFF) << 8);
		l |= (((long) v[++start] & 0xFF) << 16);
		l |= (((long) v[++start] & 0xFF) << 24);
		l |= (((long) v[++start] & 0xFF) << 32);
		l |= (((long) v[++start] & 0xFF) << 40);
		l |= (((long) v[++start] & 0xFF) << 48);
		l |= (((long) v[++start] & 0xFF) << 56);

		return l;
	}

	/**
	 * Interprets the 2 bytes starting at the specified {@code start} into a
	 * {@code short} value in the big-endian byte order and returns the
	 * resultant {@code short} value.
	 * 
	 * @param start
	 *            the index of the first byte to be interpreted
	 * @return the resultant {@code short} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than
	 *             {@code length()}, minus {@code 2}
	 */
	public short getShortB(int start) {
		return (short) getUShortB(start);
	}

	/**
	 * Interprets the 2 bytes starting at the specified {@code start} into a
	 * {@code short} value in the little-endian byte order and returns the
	 * resultant {@code short} value.
	 * 
	 * @param start
	 *            the index of the first byte to be interpreted
	 * @return the resultant {@code short} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or not smaller than
	 *             {@code length()}, minus {@code 2}
	 */
	public short getShortL(int start) {
		return (short) getUShortL(start);
	}

	/**
	 * Decodes the sub-bytesequence starting from {@code start}(inclusive) to
	 * the end into a {@code String} using the platform's default charset, and
	 * returns the resultant {@code String}. The length of the resultant
	 * {@code String} is a function of the charset, and hence may not be equal
	 * to {@code (length() - start)}.
	 * 
	 * <p>
	 * The returned {@code String} is constructed in the way below.
	 * 
	 * <pre>
	 * {@code new String(getBytes(start))}
	 * </pre>
	 * 
	 * @param start
	 *            the starting index of the sub-bytesquence to be decoded
	 * @return the resultant {@code String} as specified
	 * @throws IndexOutOfBoundsException
	 *             if {@code start < 0} or {@code start > length()}
	 */
	public String getString(int start) {
		int length = m_length;
		if (start > length)
			throw new IndexOutOfBoundsException();

		if (start == length)
			return "";

		try (StringBuilder out = StringBuilder.get()) {
			CharsetCodec.get().decode(this, start, length - start, out);
			return out.toString();
		}
	}

	/**
	 * Decodes the sub-bytesquence starting from {@code start}(inclusive) to the
	 * end into a {@code String} using the charset whose name is the given
	 * {@code charsetName}, and returns the resultant {@code String}. The length
	 * of the resultant {@code String} is a function of the charset, and hence
	 * may not be equal to {@code (length() - start)}.
	 * 
	 * <p>
	 * The returned {@code String} is constructed in the way below.
	 * 
	 * <pre>
	 * {@code new String(getBytes(start), charsetName)}
	 * </pre>
	 * 
	 * @param start
	 *            the starting index of the sub-bytesquence to be decoded
	 * @param charsetName
	 *            the name of the charset to be used to decode the bytes
	 * @return the resultant {@code String} as specified
	 * @throws IllegalCharsetNameException
	 *             if the given charset name is illegal
	 * @throws IllegalArgumentException
	 *             if the {@code charsetName} is null
	 * @throws UnsupportedCharsetException
	 *             if the named charset is not supported
	 * @throws IndexOutOfBoundsException
	 *             if {@code start < 0} or {@code start > length()}
	 */
	public String getString(int start, String charsetName) {
		return getString(start, CharsetCodec.get(charsetName));
	}

	/**
	 * Decodes the sub-bytesquence starting from {@code start}(inclusive) to the
	 * end into a {@code String} using the given {@code charset}, and returns
	 * the resultant {@code String}. The length of the resultant {@code String}
	 * is a function of the charset, and hence may not be equal to
	 * {@code (length() - start)}.
	 * 
	 * @param start
	 *            the starting index of the sub-bytesquence to be decoded
	 * @param charset
	 *            the charset to be used to decode the bytes
	 * @return the resultant {@code String} as specified
	 * @throws IndexOutOfBoundsException
	 *             if {@code start < 0} or {@code start > length()}
	 */
	public String getString(int start, Charset charset) {
		return getString(start, CharsetCodec.get(charset));
	}

	/**
	 * Decodes the sub-bytesquence starting from {@code start}(inclusive) to the
	 * end into a {@code String} using the given {@code charsetCodec}, and
	 * returns the resultant {@code String}. The length of the resultant
	 * {@code String} is a function of the charset, and hence may not be equal
	 * to {@code (length() - start)}.
	 * 
	 * @param start
	 *            the starting index of the sub-bytesquence to be decoded
	 * @param charsetCodec
	 *            the charset codec to be used to decode the bytes
	 * @return the resultant {@code String} as specified
	 * @throws IndexOutOfBoundsException
	 *             if {@code start < 0} or {@code start > length()}
	 */
	public String getString(int start, ICharsetCodec charsetCodec) {
		int length = m_length;
		if (start > length)
			throw new IndexOutOfBoundsException();

		if (start == length)
			return "";

		try (StringBuilder out = StringBuilder.get()) {
			charsetCodec.decode(this, start, length - start, out);
			return out.toString();
		}
	}

	/**
	 * Decodes the sub-bytesquence starting from {@code start}(inclusive) to
	 * {@code (start + length)}(exclusive) into a {@code String} using the
	 * platform's default charset, and returns the resultant {@code String}. The
	 * length of the resultant {@code String} is a function of the charset, and
	 * hence may not be equal to {@code length}.
	 * 
	 * @param start
	 *            the starting index of the sub-bytesequence to be decoded
	 * @param length
	 *            the number of bytes to be decoded
	 * @return the resultant {@code String} as specified
	 * @throws IndexOutOfBoundsException
	 *             if the {@code start} and {@code length} arguments index
	 *             characters outside this byte sequence
	 */
	public String getString(int start, int length) {
		return getString(start, length, CharsetCodec.get());
	}

	/**
	 * Decodes the sub-bytesequence starting from {@code start}(inclusive) to
	 * {@code (start + length)}(exclusive) into a {@code String} using the
	 * charset whose name is the given {@code charsetName}, and returns the
	 * resultant {@code String}. The length of the resultant {@code String} is a
	 * function of the charset, and hence may not be equal to {@code length}.
	 * 
	 * @param start
	 *            the starting index of the sub-bytesequence to be decoded
	 * @param length
	 *            the number of bytes to be decoded
	 * @param charsetName
	 *            the name of the charset to be used to decode the bytes
	 * @return the resultant {@code String} as specified
	 * @throws IllegalCharsetNameException
	 *             if the given charset name is illegal
	 * @throws IllegalArgumentException
	 *             if the {@code charsetName} is null
	 * @throws UnsupportedCharsetException
	 *             if the named charset is not supported
	 * @throws IndexOutOfBoundsException
	 *             if the {@code start} and {@code length} arguments index
	 *             characters outside this byte sequence
	 */
	public String getString(int start, int length, String charsetName) {
		return getString(start, length, CharsetCodec.get(charsetName));
	}

	/**
	 * Decodes the sub-bytesequence starting from {@code start}(inclusive) to
	 * {@code (start + length)}(exclusive) into a {@code String} using the given
	 * {@code charset}, and returns the resultant {@code String}. The length of
	 * the resultant {@code String} is a function of the charset, and hence may
	 * not be equal to {@code length}.
	 * 
	 * @param start
	 *            the starting index of the sub-bytesequence to be decoded
	 * @param length
	 *            the number of bytes to be decoded
	 * @param charset
	 *            the charset to be used to decode the bytes
	 * @return the resultant {@code String} as specified
	 * @throws IndexOutOfBoundsException
	 *             if the {@code start} and {@code length} arguments index
	 *             characters outside this byte sequence
	 */
	public String getString(int start, int length, Charset charset) {
		return getString(start, length, CharsetCodec.get(charset));
	}

	/**
	 * Decodes the sub-bytesequence starting from {@code start}(inclusive) to
	 * {@code (start + length)}(exclusive) into a {@code String} using the given
	 * {@code charsetCodec}, and returns the resultant {@code String}. The
	 * length of the resultant {@code String} is a function of the charset, and
	 * hence may not be equal to {@code length}.
	 * 
	 * @param start
	 *            the starting index of the sub-bytesequence to be decoded
	 * @param length
	 *            the number of bytes to be decoded
	 * @param charsetCodec
	 *            the charset codec to be used to decode the bytes
	 * @return the resultant {@code String} as specified
	 * @throws IndexOutOfBoundsException
	 *             if the {@code start} and {@code length} arguments index
	 *             characters outside this byte sequence
	 */
	public String getString(int start, int length, ICharsetCodec charsetCodec) {
		if (length < 0)
			throw new IndexOutOfBoundsException();

		try (StringBuilder out = StringBuilder.get()) {
			charsetCodec.decode(this, start, length, out);
			return out.toString();
		}
	}

	/**
	 * Zero-extends the {@code byte} value at the specified index to type
	 * {@code int} and returns the result, which is therefore in the range
	 * {@code 0} through {@code 255}.
	 * 
	 * @param index
	 *            the index of the byte value
	 * @return the byte at the specified index, interpreted as an unsigned 8-bit
	 *         value
	 * @throws IndexOutOfBoundsException
	 *             if {@code index < 0} or {@code index >= length()}
	 */
	public int getUByte(int index) {
		return byteAt(index) & 0xFF;
	}

	/**
	 * Interprets the 2 bytes starting from the specified {@code start} index as
	 * an {@code int} value in the big-endian byte order, in the range {@code 0}
	 * through {@code 65535} and returns.
	 * 
	 * <p>
	 * Let {@code a} be the first byte and {@code b} be the second byte. The
	 * value returned is:
	 * 
	 * <pre>
	 * {@code ((a &amp; 0xff) &lt;&lt; 8) | (b &amp; 0xff)}
	 * </pre>
	 * 
	 * @param start
	 *            the starting index of the 2 bytes
	 * @return the 2 bytes starting from the specified {@code start} index,
	 *         interpreted as an unsigned 16-bit value in the big-endian byte
	 *         order
	 * @throws IndexOutOfBoundsException
	 *             if {@code start < 0} or {@code (start + 2) > length()}
	 */
	public int getUShortB(int start) {
		if (start > m_length - 2)
			throw new IndexOutOfBoundsException();

		byte[] v = m_value;
		int i = (v[start] & 0xFF) << 8;
		i |= (v[++start] & 0xFF);

		return i;
	}

	/**
	 * Interprets the 2 bytes starting from the specified {@code start} index as
	 * an {@code int} value in the little-endian byte order, in the range
	 * {@code 0} through {@code 65535} and returns.
	 * 
	 * <p>
	 * Let {@code a} be the first byte and {@code b} be the second byte. The
	 * value returned is:
	 * 
	 * <pre>
	 * {@code ((b &amp; 0xff) &lt;&lt; 8) | (a &amp; 0xff)}
	 * </pre>
	 * 
	 * @param start
	 *            the starting index of the 2 bytes
	 * @return the 2 bytes starting from the specified {@code start} index,
	 *         interpreted as an unsigned 16-bit value in the little-endian byte
	 *         order
	 * @throws IndexOutOfBoundsException
	 *             if {@code start < 0} or {@code (start + 2) > length()}
	 */
	public int getUShortL(int start) {
		if (start > m_length - 2)
			throw new IndexOutOfBoundsException();

		byte[] v = m_value;
		int i = v[start] & 0xFF;
		i |= ((v[++start] & 0xFF) << 8);

		return i;
	}

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
	public int indexOf(byte b) {
		byte[] v = m_value;
		int n = m_length;
		for (int i = 0; i < n; ++i) {
			if (v[i] == b)
				return i;
		}

		return -1;
	}

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
	public int indexOf(byte b, int fromIndex) {
		if (fromIndex < 0)
			fromIndex = 0;

		byte[] v = m_value;
		for (int n = m_length; fromIndex < n; ++fromIndex) {
			if (v[fromIndex] == b)
				return fromIndex;
		}

		return -1;
	}

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
	public int indexOf(byte[] bytes) {
		int n = bytes.length;
		if (n < 1)
			return 0;

		byte[] v = m_value;
		byte first = bytes[0];
		int fromIndex = 0;

		next: for (int endIndex = m_length - n; fromIndex <= endIndex; ++fromIndex) {
			if (v[fromIndex] == first)
				continue;

			for (int i = 1; i < n; ++i) {
				if (v[fromIndex + i] != bytes[i])
					continue next;
			}

			return fromIndex;
		}

		return -1;
	}

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
	public int indexOf(byte[] bytes, int fromIndex) {
		int n = bytes.length;
		int endIndex = m_length - n;
		if (fromIndex > endIndex)
			return n < 1 ? endIndex : -1;

		if (fromIndex < 0)
			fromIndex = 0;

		if (n < 1)
			return fromIndex;

		byte[] v = m_value;
		next: for (byte first = bytes[0]; fromIndex <= endIndex; ++fromIndex) {
			if (v[fromIndex] != first)
				continue;

			for (int i = 1; i < n; ++i) {
				if (v[fromIndex + i] != bytes[i])
					continue next;
			}

			return fromIndex;
		}

		return -1;
	}

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
	public int indexOf(ByteKmp pattern) {
		return pattern.findIn(m_value, 0, m_value.length);
	}

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
	public int indexOf(ByteKmp pattern, int fromIndex) {
		return pattern.findIn(m_value, fromIndex, m_value.length - fromIndex);
	}

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
	public int lastIndexOf(byte b) {
		byte[] v = m_value;
		for (int i = m_length; i > 0;) {
			if (v[--i] == b)
				return i;
		}

		return -1;
	}

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
	public int lastIndexOf(byte b, int fromIndex) {
		if (fromIndex < 0)
			return -1;

		if (fromIndex > m_length)
			fromIndex = m_length;

		byte[] v = m_value;
		while (fromIndex > 0) {
			if (v[--fromIndex] == b)
				return fromIndex;
		}

		return -1;
	}

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
	public int lastIndexOf(byte[] bytes) {
		int n = bytes.length;
		int fromIndex = m_length - n;
		if (n < 1)
			return fromIndex;

		byte[] v = m_value;
		next: for (byte first = bytes[0]; fromIndex >= 0; --fromIndex) {
			if (v[fromIndex] != first)
				continue;

			for (int i = 1; i < n; ++i) {
				if (v[fromIndex + i] != bytes[i])
					continue next;
			}

			return fromIndex;
		}

		return -1;
	}

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
	public int lastIndexOf(byte[] bytes, int fromIndex) {
		int n = bytes.length;
		int maxIndex = m_length - n;
		if (fromIndex > maxIndex)
			fromIndex = maxIndex;

		if (fromIndex < 0)
			return -1;

		if (n < 1)
			return fromIndex;

		byte[] v = m_value;
		next: for (byte first = bytes[0]; fromIndex >= 0; --fromIndex) {
			if (v[fromIndex] != first)
				continue;

			for (int i = 1; i < n; ++i) {
				if (v[fromIndex + i] != bytes[i])
					continue next;
			}

			return fromIndex;
		}

		return -1;
	}

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
	public int lastIndexOf(ByteKmp pattern) {
		return pattern.findIn(m_value, 0, m_value.length);
	}

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
	public int lastIndexOf(ByteKmp pattern, int fromIndex) {
		return pattern.rfindIn(m_value, 0, fromIndex + pattern.length());
	}

	/**
	 * Tests whether this byte sequence starts with the specified {@code bytes}.
	 * 
	 * @param bytes
	 *            the byte sequence to test
	 * @return true if this byte sequence starts with the specified
	 *         {@code bytes}, otherwise false
	 */
	public boolean startsWith(byte[] bytes) {
		int i = bytes.length;
		if (i > m_length)
			return false;

		byte[] value = m_value;
		while (i > 0) {
			--i;
			if (bytes[i] != value[i])
				return false;
		}

		return true;
	}

	@Override
	public void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin) {
		System.arraycopy(m_value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
	}

	/**
	 * Appends the given byte {@code b} into this {@code BytesBuilder}.
	 * 
	 * @param b
	 *            the byte to append
	 * @return this object
	 */
	public BytesBuilder append(byte b) {
		int newLength = m_length + 1;
		if (newLength > m_value.length)
			expandCapacity(newLength);

		m_value[m_length] = b;
		m_length = newLength;
		return this;
	}

	/**
	 * Appends the data in the given {@code bytes} into this
	 * {@code BytesBuilder} .
	 * 
	 * @param bytes
	 *            data of which to append
	 * @return this object
	 */
	public BytesBuilder append(byte[] bytes) {
		return append(bytes, 0, bytes.length);
	}

	/**
	 * Appends {@code length} bytes of the given {@code bytes} starting at
	 * {@code offset} into this {@code BytesBuilder}.
	 * 
	 * @param bytes
	 *            data of which to append
	 * @param offset
	 *            the index of the first byte in {@code bytes} to append
	 * @param length
	 *            the number of bytes to append
	 * @return this object
	 * @throws NullPointerException
	 *             if {@code bytes} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code bytes.length - length}
	 */
	public BytesBuilder append(byte[] bytes, int offset, int length) {
		int newLength = m_length + length;
		if (newLength > m_value.length)
			expandCapacity(newLength);
		System.arraycopy(bytes, offset, m_value, m_length, length);
		m_length = newLength;
		return this;
	}

	/**
	 * Appends the data in the given {@code bs} into this {@code BytesBuilder}.
	 * 
	 * @param bs
	 *            the byte sequence to append
	 * @return this object
	 */
	public BytesBuilder append(IByteSequence bs) {
		int length = bs.length();
		int newLength = m_length + length;
		if (newLength > m_value.length)
			expandCapacity(newLength);

		bs.getBytes(0, length, m_value, m_length);
		m_length = newLength;
		return this;
	}

	/**
	 * Appends {@code length} bytes of the given {@code bs} starting at
	 * {@code offset} into this {@code BytesBuilder}.
	 * 
	 * @param bs
	 *            bytes of which to append
	 * @param offset
	 *            the index of the first byte in {@code bs} to append
	 * @param length
	 *            the number of bytes to append
	 * @return this object
	 * @throws NullPointerException
	 *             if {@code bs} is {@code null}
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code bs.length() - length}
	 */
	public BytesBuilder append(IByteSequence bs, int offset, int length) {
		int newLength = m_length + length;
		if (newLength > m_value.length)
			expandCapacity(newLength);

		bs.getBytes(offset, offset + length, m_value, m_length);
		m_length = newLength;
		return this;
	}

	/**
	 * Appends the bytes, encoded from the given {@code str} using the
	 * platform's default charset, into this {@code BytesBuilder}. If
	 * {@code str} is {@code null}, then the string {@code "null"} will be
	 * encoded and appended into this {@code BytesBuilder}.
	 * 
	 * @param str
	 *            the string to append
	 * @return this object
	 */
	public BytesBuilder append(String str) {
		return append(str, CharsetCodec.get());
	}

	/**
	 * Appends the bytes, encoded from the given {@code str} using the specified
	 * charset, into this {@code BytesBuilder}. If {@code str} is {@code null},
	 * then the string {@code "null"} will be encoded and appended into this
	 * {@code BytesBuilder}.
	 * 
	 * @param str
	 *            the string to append
	 * @param charset
	 *            the charset to encode the given {@code str}
	 * @return this object
	 * @throws NullPointerException
	 *             if {@code charset} is {@code null}
	 */
	public BytesBuilder append(String str, Charset charset) {
		return append(str, CharsetCodec.get(charset));
	}

	/**
	 * Appends the bytes, encoded from the given {@code str} using the specified
	 * charset, into this {@code BytesBuilder}. If {@code str} is {@code null},
	 * then the string {@code "null"} will be encoded and appended into this
	 * {@code BytesBuilder}.
	 * 
	 * @param str
	 *            the string to append
	 * @param charsetName
	 *            the name of the charset to encode the given {@code str}
	 * @return this object
	 * @throws NullPointerException
	 *             if {@code charsetName} is {@code null}
	 * @throws IllegalCharsetNameException
	 *             if the given charset name is illegal
	 * @throws UnsupportedCharsetException
	 *             if the named charset is not supported
	 */
	public BytesBuilder append(String str, String charsetName) {
		return append(str, CharsetCodec.get(charsetName));
	}

	/**
	 * Appends the bytes, encoded from the given {@code str} using the specified
	 * charset codec, into this {@code BytesBuilder}. If {@code str} is
	 * {@code null}, then the string {@code "null"} will be encoded and appended
	 * into this {@code BytesBuilder}.
	 * 
	 * @param str
	 *            the string to append
	 * @param charsetCodec
	 *            the charset codec to encode the given {@code str}
	 * @return this object
	 * @throws NullPointerException
	 *             if {@code charsetCodec} is {@code null}
	 */
	public BytesBuilder append(String str, ICharsetCodec charsetCodec) {
		try (StringBuilder builder = str == null ? StringBuilder.get() : StringBuilder.get(str.length())) {
			builder.append(str);
			charsetCodec.encode(builder, this);
		}
		return this;
	}

	/**
	 * Appends the bytes, encoded from the given {@code str} starting at
	 * {@code offset} ending at {@code (offset + length)} using the platform's
	 * default charset, into this {@code BytesBuilder}. If {@code str} is
	 * {@code null}, then the string {@code "null"} will be encoded and appended
	 * into this {@code BytesBuilder}.
	 * 
	 * @param str
	 *            the string to append
	 * @param offset
	 *            the index of the first character in {@code str} to encode
	 * @param length
	 *            the number of characters to encode
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (str.length() - length)}
	 */
	public BytesBuilder append(String str, int offset, int length) {
		return append(str, offset, length, CharsetCodec.get());
	}

	/**
	 * Appends the bytes, encoded from the given {@code str} starting at
	 * {@code offset} ending at {@code (offset + length)} using the specified
	 * charset, into this {@code BytesBuilder}. If {@code str} is {@code null},
	 * then the string {@code "null"} will be encoded and appended into this
	 * {@code BytesBuilder}.
	 * 
	 * @param str
	 *            the string to append
	 * @param offset
	 *            the index of the first character in {@code str} to encode
	 * @param length
	 *            the number of characters to encode
	 * @param charset
	 *            the charset to encode
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (str.length() - length)}
	 * @throws NullPointerException
	 *             if {@code charset} is {@code null}
	 */
	public BytesBuilder append(String str, int offset, int length, Charset charset) {
		return append(str, offset, length, CharsetCodec.get(charset));
	}

	/**
	 * Appends the bytes, encoded from the given {@code str} starting at
	 * {@code offset} ending at {@code (offset + length)} using the specified
	 * charset, into this {@code BytesBuilder}. If {@code str} is {@code null},
	 * then the string {@code "null"} will be encoded and appended into this
	 * {@code BytesBuilder}.
	 * 
	 * @param str
	 *            the string to append
	 * @param offset
	 *            the index of the first character in {@code str} to encode
	 * @param length
	 *            the number of characters to encode
	 * @param charsetName
	 *            the name of the charset to encode
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (str.length() - length)}
	 * @throws NullPointerException
	 *             if {@code charsetName} is {@code null}
	 */
	public BytesBuilder append(String str, int offset, int length, String charsetName) {
		return append(str, offset, length, CharsetCodec.get(charsetName));
	}

	/**
	 * Appends the bytes, encoded from the given {@code str} starting at
	 * {@code offset} ending at {@code (offset + length)} using the specified
	 * charset, into this {@code BytesBuilder}. If {@code str} is {@code null},
	 * then the string {@code "null"} will be encoded and appended into this
	 * {@code BytesBuilder}.
	 * 
	 * @param str
	 *            the string to append
	 * @param offset
	 *            the index of the first character in {@code str} to encode
	 * @param length
	 *            the number of characters to encode
	 * @param charsetCodec
	 *            the charset codec to encode
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code offset} is greater than
	 *             {@code (str.length() - length)}
	 * @throws NullPointerException
	 *             if {@code charsetCodec} is {@code null}
	 */
	public BytesBuilder append(String str, int offset, int length, ICharsetCodec charsetCodec) {
		try (StringBuilder builder = StringBuilder.get(length)) {
			builder.append(str, offset, offset + length);
			charsetCodec.encode(builder, this);
		}
		return this;
	}

	/**
	 * Appends the bytes, encoded from the given {@code chars} using the
	 * platform's default charset, into this {@code BytesBuilder}.
	 * 
	 * @param chars
	 *            the char array to append
	 * @return this object
	 */
	public BytesBuilder append(char[] chars) {
		return append(chars, CharsetCodec.get());
	}

	/**
	 * Appends the bytes, encoded from the given {@code chars} using the given
	 * charset, into this {@code BytesBuilder}.
	 * 
	 * @param chars
	 *            the char array to append
	 * @param charset
	 *            the charset to encode the given {@code chars}
	 * @return this object
	 */
	public BytesBuilder append(char[] chars, Charset charset) {
		return append(chars, CharsetCodec.get(charset));
	}

	/**
	 * Appends the bytes, encoded from the given {@code chars} using the
	 * specified charset, into this {@code BytesBuilder}.
	 * 
	 * @param chars
	 *            the char array to append
	 * @param charsetName
	 *            the name of the charset to encode the given {@code chars}
	 * @return this object
	 */
	public BytesBuilder append(char[] chars, String charsetName) {
		return append(chars, CharsetCodec.get(charsetName));
	}

	/**
	 * Appends the bytes, encoded from the given {@code chars} using the
	 * specified charset codec, into this {@code BytesBuilder}.
	 * 
	 * @param chars
	 *            the char array to append
	 * @param charsetCodec
	 *            the charset codec to encode the given {@code chars}
	 * @return this object
	 */
	public BytesBuilder append(char[] chars, ICharsetCodec charsetCodec) {
		charsetCodec.encode(chars, this);
		return this;
	}

	/**
	 * Appends the bytes, encoded from the given {@code chars} starting at
	 * {@code offset} ending at {@code (offset + length)} using the platform's
	 * default charset, into this {@code BytesBuilder}.
	 * 
	 * @param chars
	 *            the char array to append
	 * @param offset
	 *            the index of the first character in {@code chars} to encode
	 * @param length
	 *            the number of characters to encode
	 * @return this object
	 */
	public BytesBuilder append(char[] chars, int offset, int length) {
		return append(chars, offset, length, CharsetCodec.get());
	}

	/**
	 * Appends the bytes, encoded from the given {@code chars} starting at
	 * {@code offset} ending at {@code (offset + length)} using the specified
	 * charset, into this {@code BytesBuilder}.
	 * 
	 * @param chars
	 *            the char array to append
	 * @param offset
	 *            the index of the first character in {@code chars} to encode
	 * @param length
	 *            the number of characters to encode
	 * @param charset
	 *            the charset to encode the given {@code chars}
	 * @return this object
	 */
	public BytesBuilder append(char[] chars, int offset, int length, Charset charset) {
		return append(chars, offset, length, CharsetCodec.get(charset));
	}

	/**
	 * Appends the bytes, encoded from the given {@code chars} starting at
	 * {@code offset} ending at {@code (offset + length)} using the specified
	 * charset, into this {@code BytesBuilder}.
	 * 
	 * @param chars
	 *            the char array to append
	 * @param offset
	 *            the index of the first character in {@code chars} to encode
	 * @param length
	 *            the number of characters to encode
	 * @param charsetName
	 *            the name of the charset to encode the given {@code chars}
	 * @return this object
	 */
	public BytesBuilder append(char[] chars, int offset, int length, String charsetName) {
		return append(chars, offset, length, CharsetCodec.get(charsetName));
	}

	/**
	 * Appends the bytes, encoded from the given {@code chars} starting at
	 * {@code offset} ending at {@code (offset + length)} using the specified
	 * charset codec, into this {@code BytesBuilder}.
	 * 
	 * @param chars
	 *            the char array to append
	 * @param offset
	 *            the index of the first character in {@code chars} to encode
	 * @param length
	 *            the number of characters to encode
	 * @param charsetCodec
	 *            the charset codec to encode
	 * @return this object
	 */
	public BytesBuilder append(char[] chars, int offset, int length, ICharsetCodec charsetCodec) {
		charsetCodec.encode(chars, offset, length, this);
		return this;
	}

	/**
	 * Appends {@code count} bytes of {@code b} to this {@code BytesBuilder}.
	 * 
	 * @param b
	 *            the byte to append
	 * @param count
	 *            the number of the specified byte to append
	 * @return this object
	 */
	public BytesBuilder appendFill(byte b, int count) {
		if (count < 0)
			throw new IllegalArgumentException();

		int i = m_length;
		int newLength = i + count;
		if (newLength > m_value.length)
			expandCapacity(newLength);

		byte[] v = m_value;
		for (; i < newLength; ++i)
			v[i] = b;

		m_length = newLength;
		return this;
	}

	/**
	 * Encodes the specified {@code i} into 4 bytes in big-endian order and
	 * appends them to this {@code BytesBuilder}.
	 * 
	 * @param i
	 *            the {@code int} value to be encoded and appended
	 * @return this object
	 */
	public BytesBuilder appendIntB(int i) {
		int newLength = m_length + 4;
		if (newLength > m_value.length)
			expandCapacity(newLength);

		m_length = newLength;
		byte[] v = m_value;
		v[--newLength] = (byte) i;
		v[--newLength] = (byte) (i >> 8);
		v[--newLength] = (byte) (i >> 16);
		v[--newLength] = (byte) (i >> 24);
		return this;
	}

	/**
	 * Encodes the specified {@code i} into 4 bytes in little-endian order and
	 * appends them to this {@code BytesBuilder}.
	 * 
	 * @param i
	 *            the {@code int} value to be encoded and appended
	 * @return this object
	 */
	public BytesBuilder appendIntL(int i) {
		int newLength = m_length + 4;
		if (newLength > m_value.length)
			expandCapacity(newLength);

		m_length = newLength;
		byte[] v = m_value;
		v[--newLength] = (byte) (i >> 24);
		v[--newLength] = (byte) (i >> 16);
		v[--newLength] = (byte) (i >> 8);
		v[--newLength] = (byte) i;
		return this;
	}

	/**
	 * Encodes the specified {@code l} into 8 bytes in big-endian order and
	 * appends them to this {@code BytesBuilder}.
	 * 
	 * @param l
	 *            the {@code long} value to be encoded and appended
	 * @return this object
	 */
	public BytesBuilder appendLongB(long l) {
		int newLength = m_length + 8;
		if (newLength > m_value.length)
			expandCapacity(newLength);

		m_length = newLength;
		byte[] v = m_value;
		v[--newLength] = (byte) l;
		v[--newLength] = (byte) (l >> 8);
		v[--newLength] = (byte) (l >> 16);
		v[--newLength] = (byte) (l >> 24);
		v[--newLength] = (byte) (l >> 32);
		v[--newLength] = (byte) (l >> 40);
		v[--newLength] = (byte) (l >> 48);
		v[--newLength] = (byte) (l >> 56);
		return this;
	}

	/**
	 * Encodes the specified {@code l} into 8 bytes in little-endian order and
	 * appends them to this {@code BytesBuilder}.
	 * 
	 * @param l
	 *            the {@code long} value to be encoded and appended
	 * @return this object
	 */
	public BytesBuilder appendLongL(long l) {
		int newLength = m_length + 8;
		if (newLength > m_value.length)
			expandCapacity(newLength);

		m_length = newLength;
		byte[] v = m_value;
		v[--newLength] = (byte) (l >> 56);
		v[--newLength] = (byte) (l >> 48);
		v[--newLength] = (byte) (l >> 40);
		v[--newLength] = (byte) (l >> 32);
		v[--newLength] = (byte) (l >> 24);
		v[--newLength] = (byte) (l >> 16);
		v[--newLength] = (byte) (l >> 8);
		v[--newLength] = (byte) l;
		return this;
	}

	/**
	 * Encodes the specified {@code s} into 2 bytes in big-endian order and
	 * appends them to this {@code BytesBuilder}.
	 * 
	 * @param s
	 *            the {@code short} value to be encoded and appended
	 * @return this object
	 */
	public BytesBuilder appendShortB(short s) {
		int newLength = m_length + 2;
		if (newLength > m_value.length)
			expandCapacity(newLength);

		m_length = newLength;
		byte[] v = m_value;
		v[--newLength] = (byte) s;
		v[--newLength] = (byte) (s >> 8);
		return this;
	}

	/**
	 * Encodes the specified {@code s} into 2 bytes in little-endian order and
	 * appends them to this {@code BytesBuilder}.
	 * 
	 * @param s
	 *            the {@code short} value to be encoded and appended
	 * @return this object
	 */
	public BytesBuilder appendShortL(short s) {
		int newLength = m_length + 2;
		if (newLength > m_value.length)
			expandCapacity(newLength);

		m_length = newLength;
		byte[] v = m_value;
		v[--newLength] = (byte) (s >> 8);
		v[--newLength] = (byte) s;
		return this;
	}

	/**
	 * Encodes the specified {@code f} into 4 bytes in big-endian order and
	 * appends them to this {@code BytesBuilder}.
	 * 
	 * @param f
	 *            the {@code float} value to be encoded and appended
	 * @return this object
	 */
	public BytesBuilder appendFloatB(float f) {
		return appendIntB(Float.floatToRawIntBits(f));
	}

	/**
	 * Encodes the specified {@code f} into 4 bytes in little-endian order and
	 * appends them to this {@code BytesBuilder}.
	 * 
	 * @param f
	 *            the {@code float} value to be encoded and appended
	 * @return this object
	 */
	public BytesBuilder appendFloatL(float f) {
		return appendIntL(Float.floatToRawIntBits(f));
	}

	/**
	 * Encodes the specified {@code d} into 8 bytes in big-endian order and
	 * appends them to this {@code BytesBuilder}.
	 * 
	 * @param d
	 *            the {@code double} value to be encoded and appended
	 * @return this object
	 */
	public BytesBuilder appendDoubleB(double d) {
		return appendLongB(Double.doubleToRawLongBits(d));
	}

	/**
	 * Encodes the specified {@code d} into 8 bytes in little-endian order and
	 * appends them to this {@code BytesBuilder}.
	 * 
	 * @param d
	 *            the {@code double} value to be encoded and appended
	 * @return this object
	 */
	public BytesBuilder appendDoubleL(double d) {
		return appendLongL(Double.doubleToRawLongBits(d));
	}

	/**
	 * Inserts the specified byte {@code b} to this {@code BytesBuilder} at
	 * {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param b
	 *            the byte to insert
	 * @return this object
	 */
	public BytesBuilder insert(int index, byte b) {
		int newLength = m_length;
		if (index > newLength)
			throw new IndexOutOfBoundsException();

		if (++newLength > m_value.length)
			expandCapacityForInsertion(index, 1, newLength);

		m_value[index] = b;
		m_length = newLength;
		return this;
	}

	/**
	 * Inserts the specified {@code bytes} to this {@code BytesBuilder} at
	 * {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param bytes
	 *            the byte array to insert
	 * @return this object
	 */
	public BytesBuilder insert(int index, byte[] bytes) {
		int newLength = m_length;
		if (index > newLength)
			throw new IndexOutOfBoundsException();

		int length = bytes.length;
		newLength += length;
		if (newLength > m_value.length)
			expandCapacityForInsertion(index, length, newLength);

		System.arraycopy(bytes, 0, m_value, index, length);
		m_length = newLength;
		return this;
	}

	/**
	 * Inserts the specified {@code bytes} starting at {@code offset} ending at
	 * {@code (offset + length)} to this {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param bytes
	 *            the byte array to insert
	 * @param offset
	 *            the index of the first byte of {@code bytes} to insert
	 * @param length
	 *            the number of bytes to insert
	 * @return this object
	 */
	public BytesBuilder insert(int index, byte[] bytes, int offset, int length) {
		int newLength = m_length;
		if (index > newLength || length < 0)
			throw new IndexOutOfBoundsException();

		newLength += length;
		if (newLength > m_value.length)
			expandCapacityForInsertion(index, length, newLength);

		System.arraycopy(bytes, offset, m_value, index, length);
		m_length = newLength;
		return this;
	}

	/**
	 * Inserts the specified {@code bs} to this {@code BytesBuilder} at
	 * {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param bs
	 *            the byte sequence to insert
	 * @return this object
	 */
	public BytesBuilder insert(int index, IByteSequence bs) {
		int newLength = m_length;
		if (index > newLength)
			throw new IndexOutOfBoundsException();

		int length = bs.length();
		newLength += length;
		if (newLength > m_value.length)
			expandCapacityForInsertion(index, length, newLength);

		bs.getBytes(0, length, m_value, index);
		m_length = newLength;
		return this;
	}

	/**
	 * Inserts the specified {@code bs} starting at {@code offset} ending at
	 * {@code (offset + length)} to this {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param bs
	 *            the byte sequence to insert
	 * @param offset
	 *            the index of the first byte in the specified {@code bs} to
	 *            insert
	 * @param length
	 *            the number of bytes to insert
	 * @return this object
	 */
	public BytesBuilder insert(int index, IByteSequence bs, int offset, int length) {
		int newLength = m_length;
		if (index > newLength || length < 0)
			throw new IndexOutOfBoundsException();

		newLength += length;
		if (newLength > m_value.length)
			expandCapacityForInsertion(index, length, newLength);

		bs.getBytes(offset, offset + length, m_value, index);
		m_length = newLength;
		return this;
	}

	/**
	 * Encodes the specified {@code i} into 4 bytes in big-endian order and
	 * inserts them to this {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param i
	 *            the {@code int} value to be encoded and inserted
	 * @return this object
	 */
	public BytesBuilder insertIntB(int index, int i) {
		int newLength = m_length;
		if (index > newLength)
			throw new IndexOutOfBoundsException();

		newLength += 4;
		if (newLength > m_value.length)
			expandCapacityForInsertion(index, 4, newLength);

		byte[] v = m_value;
		v[index] = (byte) (i >> 24);
		v[++index] = (byte) (i >> 16);
		v[++index] = (byte) (i >> 8);
		v[++index] = (byte) i;
		m_length = newLength;
		return this;
	}

	/**
	 * Encodes the specified {@code i} into 4 bytes in little-endian order and
	 * inserts them into this {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param i
	 *            the {@code int} value to be encoded and inserted
	 * @return this object
	 */
	public BytesBuilder insertIntL(int index, int i) {
		int newLength = m_length;
		if (index > newLength)
			throw new IndexOutOfBoundsException();

		newLength += 4;
		if (newLength > m_value.length)
			expandCapacityForInsertion(index, 4, newLength);

		byte[] v = m_value;
		v[index] = (byte) i;
		v[++index] = (byte) (i >> 8);
		v[++index] = (byte) (i >> 16);
		v[++index] = (byte) (i >> 24);
		m_length = newLength;
		return this;
	}

	/**
	 * Encodes the specified {@code l} into 8 bytes in big-endian order and
	 * inserts them into this {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param l
	 *            the {@code long} value to be encoded and inserted
	 * @return this object
	 */
	public BytesBuilder insertLongB(int index, long l) {
		int newLength = m_length;
		if (index > newLength)
			throw new IndexOutOfBoundsException();

		newLength += 8;
		if (newLength > m_value.length)
			expandCapacityForInsertion(index, 8, newLength);

		byte[] v = m_value;
		v[index] = (byte) (l >> 56);
		v[++index] = (byte) (l >> 48);
		v[++index] = (byte) (l >> 40);
		v[++index] = (byte) (l >> 32);
		v[++index] = (byte) (l >> 24);
		v[++index] = (byte) (l >> 16);
		v[++index] = (byte) (l >> 8);
		v[++index] = (byte) l;
		m_length = newLength;
		return this;
	}

	/**
	 * Encodes the specified {@code l} into 8 bytes in little-endian order and
	 * inserts them into this {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param l
	 *            the {@code long} value to be encoded and inserted
	 * @return this object
	 */
	public BytesBuilder insertLongL(int index, long l) {
		int newLength = m_length;
		if (index > newLength)
			throw new IndexOutOfBoundsException();

		newLength += 8;
		if (newLength > m_value.length)
			expandCapacityForInsertion(index, 8, newLength);

		byte[] v = m_value;
		v[index] = (byte) l;
		v[++index] = (byte) (l >> 8);
		v[++index] = (byte) (l >> 16);
		v[++index] = (byte) (l >> 24);
		v[++index] = (byte) (l >> 32);
		v[++index] = (byte) (l >> 40);
		v[++index] = (byte) (l >> 48);
		v[++index] = (byte) (l >> 56);
		m_length = newLength;
		return this;
	}

	/**
	 * Encodes the specified {@code s} into 2 bytes in big-endian order and
	 * inserts them into this {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param s
	 *            the {@code short} value to be encoded and inserted
	 * @return this object
	 */
	public BytesBuilder insertShortB(int index, short s) {
		int newLength = m_length;
		if (index < 0 || index > newLength)
			throw new IndexOutOfBoundsException();

		newLength += 2;
		if (newLength > m_value.length)
			expandCapacityForInsertion(index, 2, newLength);

		byte[] v = m_value;
		v[index] = (byte) (s >> 8);
		v[++index] = (byte) s;
		m_length = newLength;
		return this;
	}

	/**
	 * Encodes the specified {@code s} into 2 bytes in little-endian order and
	 * inserts them into this {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param s
	 *            the {@code short} value to be encoded and inserted
	 * @return this object
	 */
	public BytesBuilder insertShortL(int index, short s) {
		int newLength = m_length;
		if (index < 0 || index > newLength)
			throw new IndexOutOfBoundsException();

		newLength += 2;
		if (newLength > m_value.length)
			expandCapacityForInsertion(index, 2, newLength);

		byte[] v = m_value;
		v[index] = (byte) s;
		v[++index] = (byte) (s >> 8);
		m_length = newLength;
		return this;
	}

	/**
	 * Encodes the specified {@code f} into 4 bytes in big-endian order and
	 * inserts them into this {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param f
	 *            the {@code float} value to be encoded and inserted
	 * @return this object
	 */
	public BytesBuilder insertFloatB(int index, float f) {
		return insertIntB(index, Float.floatToRawIntBits(f));
	}

	/**
	 * Encodes the specified {@code f} into 4 bytes in little-endian order and
	 * inserts them into this {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param f
	 *            the {@code float} value to be encoded and inserted
	 * @return this object
	 */
	public BytesBuilder insertFloatL(int index, float f) {
		return insertIntL(index, Float.floatToRawIntBits(f));
	}

	/**
	 * Encodes the specified {@code d} into 8 bytes in big-endian order and
	 * inserts them into this {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param d
	 *            the {@code double} value to be encoded and inserted
	 * @return this object
	 */
	public BytesBuilder insertDoubleB(int index, double d) {
		return insertLongB(index, Double.doubleToRawLongBits(d));
	}

	/**
	 * Encodes the specified {@code d} into 8 bytes in little-endian order and
	 * inserts them into this {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param d
	 *            the {@code double} value to be encoded and inserted
	 * @return this object
	 */
	public BytesBuilder insertDoubleL(int index, double d) {
		return insertLongL(index, Double.doubleToRawLongBits(d));
	}

	/**
	 * Encodes the specified {@code str} using the platform's default charset
	 * and inserts the result into this {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param str
	 *            the {@code String} value to be encoded and inserted
	 * @return this object
	 */
	public BytesBuilder insert(int index, String str) {
		return insert(index, str, CharsetCodec.get());
	}

	/**
	 * Encodes the specified {@code str} using the specified charset and inserts
	 * the result into this {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param str
	 *            the {@code String} value to be encoded and inserted
	 * @param charset
	 *            the charset to encode the specified string
	 * @return this object
	 */
	public BytesBuilder insert(int index, String str, Charset charset) {
		return insert(index, str, CharsetCodec.get(charset));
	}

	/**
	 * Encodes the specified {@code str} using the specified charset and inserts
	 * the result into this {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param str
	 *            the {@code String} value to be encoded and inserted
	 * @param charsetName
	 *            the name of the charset to encode the specified string
	 * @return this object
	 */
	public BytesBuilder insert(int index, String str, String charsetName) {
		return insert(index, str, CharsetCodec.get(charsetName));
	}

	/**
	 * Encodes the specified {@code str} using the specified charset codec and
	 * inserts the result into this {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param str
	 *            the {@code String} value to be encoded and inserted
	 * @param charsetCodec
	 *            the charset codec to encode the specified string
	 * @return this object
	 */
	public BytesBuilder insert(int index, String str, ICharsetCodec charsetCodec) {
		try (BytesBuilder bb = BytesBuilder.get(); StringBuilder sb = StringBuilder.get()) {
			sb.append(str);
			charsetCodec.encode(sb, bb);
			return insert(index, bb);
		}
	}

	/**
	 * Encodes the substring of the specified {@code str} starting at
	 * {@code offset} ending at {@code (offset + length)} using the platform's
	 * default charset, and inserts the result into this {@code BytesBuilder} at
	 * {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param str
	 *            the {@code String} value to be encoded and inserted
	 * @param offset
	 *            the index of the first character of the specified string to be
	 *            encoded and inserted
	 * @param length
	 *            the number of characters to be encoded and inserted
	 * @return this object
	 */
	public BytesBuilder insert(int index, String str, int offset, int length) {
		return insert(index, str, CharsetCodec.get());
	}

	/**
	 * Encodes the substring of the specified {@code str} starting at
	 * {@code offset} ending at {@code (offset + length)} using the specified
	 * charset, and inserts the result into this {@code BytesBuilder} at
	 * {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param str
	 *            the {@code String} value to be encoded and inserted
	 * @param offset
	 *            the index of the first character of the specified string to be
	 *            encoded and inserted
	 * @param length
	 *            the number of characters to be encoded and inserted
	 * @param charset
	 *            the charset to encode
	 * @return this object
	 */
	public BytesBuilder insert(int index, String str, int offset, int length, Charset charset) {
		return insert(index, str, CharsetCodec.get(charset));
	}

	/**
	 * Encodes the substring of the specified {@code str} starting at
	 * {@code offset} ending at {@code (offset + length)} using the specified
	 * charset, and inserts the result into this {@code BytesBuilder} at
	 * {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param str
	 *            the {@code String} value to be encoded and inserted
	 * @param offset
	 *            the index of the first character of the specified string to be
	 *            encoded and inserted
	 * @param length
	 *            the number of characters to be encoded and inserted
	 * @param charsetName
	 *            the name of the charset to encode
	 * @return this object
	 */
	public BytesBuilder insert(int index, String str, int offset, int length, String charsetName) {
		return insert(index, str, CharsetCodec.get(charsetName));
	}

	/**
	 * Encodes the substring of the specified {@code str} starting at
	 * {@code offset} ending at {@code (offset + length)} using the specified
	 * charset codec, and inserts the result into this {@code BytesBuilder} at
	 * {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param str
	 *            the {@code String} value to be encoded and inserted
	 * @param offset
	 *            the index of the first character of the specified string to be
	 *            encoded and inserted
	 * @param length
	 *            the number of characters to be encoded and inserted
	 * @param charsetCodec
	 *            the charset codec to encode
	 * @return this object
	 */
	public BytesBuilder insert(int index, String str, int offset, int length, ICharsetCodec charsetCodec) {
		try (BytesBuilder bb = BytesBuilder.get(); StringBuilder sb = StringBuilder.get()) {
			sb.append(str, offset, offset + length);
			charsetCodec.encode(sb, bb);
			return insert(index, bb);
		}
	}

	/**
	 * Encodes the specified {@code chars} using the platform's default charset,
	 * and inserts the result into this {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param chars
	 *            the char array to be encoded and inserted
	 * @return this object
	 */
	public BytesBuilder insert(int index, char[] chars) {
		return insert(index, chars, CharsetCodec.get());
	}

	/**
	 * Encodes the specified {@code chars} using the specified charset, and
	 * inserts the result into this {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param chars
	 *            the char array to be encoded and inserted
	 * @param charset
	 *            the charset to encode
	 * @return this object
	 */
	public BytesBuilder insert(int index, char[] chars, Charset charset) {
		return insert(index, chars, CharsetCodec.get(charset));
	}

	/**
	 * Encodes the specified {@code chars} using the specified charset, and
	 * inserts the result into this {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param chars
	 *            the char array to be encoded and inserted
	 * @param charsetName
	 *            the name of the charset to encode
	 * @return this object
	 */
	public BytesBuilder insert(int index, char[] chars, String charsetName) {
		return insert(index, chars, CharsetCodec.get(charsetName));
	}

	/**
	 * Encodes the specified {@code chars} using the specified charset codec,
	 * and inserts the result into this {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param chars
	 *            the char array to be encoded and inserted
	 * @param charsetCodec
	 *            the charset codec to encode
	 * @return this object
	 */
	public BytesBuilder insert(int index, char[] chars, ICharsetCodec charsetCodec) {
		try (BytesBuilder bb = BytesBuilder.get()) {
			charsetCodec.encode(chars, bb);
			return insert(index, bb);
		}
	}

	/**
	 * Encodes the subsequence of the specified {@code chars} starting at
	 * {@code offset} ending at {@code (offset + length)} using the platform's
	 * default charset, and inserts the result into this {@code BytesBuilder} at
	 * {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param chars
	 *            the char array to be encoded and inserted
	 * @param offset
	 *            the index of the first character in the specified
	 *            {@code chars} to be encoded and inserted
	 * @param length
	 *            the number of the characters to be encoded and inserted
	 * @return this object
	 */
	public BytesBuilder insert(int index, char[] chars, int offset, int length) {
		return insert(index, chars, offset, length, CharsetCodec.get());
	}

	/**
	 * Encodes the subsequence of the specified {@code chars} starting at
	 * {@code offset} ending at {@code (offset + length)} using the specified
	 * charset, and inserts the result into this {@code BytesBuilder} at
	 * {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param chars
	 *            the char array to be encoded and inserted
	 * @param offset
	 *            the index of the first character in the specified
	 *            {@code chars} to be encoded and inserted
	 * @param length
	 *            the number of the characters to be encoded and inserted
	 * @param charset
	 *            the charset to encode
	 * @return this object
	 */
	public BytesBuilder insert(int index, char[] chars, int offset, int length, Charset charset) {
		return insert(index, chars, offset, length, CharsetCodec.get(charset));
	}

	/**
	 * Encodes the subsequence of the specified {@code chars} starting at
	 * {@code offset} ending at {@code (offset + length)} using the specified
	 * charset, and inserts the result into this {@code BytesBuilder} at
	 * {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param chars
	 *            the char array to be encoded and inserted
	 * @param offset
	 *            the index of the first character in the specified
	 *            {@code chars} to be encoded and inserted
	 * @param length
	 *            the number of the characters to be encoded and inserted
	 * @param charsetName
	 *            the name of the charset to encode
	 * @return this object
	 */
	public BytesBuilder insert(int index, char[] chars, int offset, int length, String charsetName) {
		return insert(index, chars, offset, length, CharsetCodec.get(charsetName));
	}

	/**
	 * Encodes the subsequence of the specified {@code chars} starting at
	 * {@code offset} ending at {@code (offset + length)} using the specified
	 * charset codec, and inserts the result into this {@code BytesBuilder} at
	 * {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param chars
	 *            the char array to be encoded and inserted
	 * @param offset
	 *            the index of the first character in the specified
	 *            {@code chars} to be encoded and inserted
	 * @param length
	 *            the number of the characters to be encoded and inserted
	 * @param charsetCodec
	 *            the charset codec to encode
	 * @return this object
	 */
	public BytesBuilder insert(int index, char[] chars, int offset, int length, ICharsetCodec charsetCodec) {
		try (BytesBuilder bb = BytesBuilder.get()) {
			charsetCodec.encode(chars, offset, length, bb);
			return insert(index, bb);
		}
	}

	/**
	 * Inserts the specified {@code count} bytes of byte {@code b} into this
	 * {@code BytesBuilder} at {@code index}.
	 * 
	 * @param index
	 *            the offset where to insert
	 * @param b
	 *            the byte to insert
	 * @param count
	 *            the number of the specified byte to insert
	 * @return this object
	 * @throws IllegalArgumentException
	 *             if {@code count} is negative
	 */
	public BytesBuilder insertFill(int index, byte b, int count) {
		if (count < 0)
			throw new IllegalArgumentException();

		int n = m_length;
		int newLength = n + count;
		if (newLength > m_value.length)
			expandCapacity(newLength);

		byte[] v = m_value;
		count += index;
		System.arraycopy(v, index, v, count, n - index);

		for (; index < count; ++index)
			v[index] = b;

		m_length = newLength;
		return this;
	}

	/**
	 * Removes the bytes in a subsequence of this sequence. The subsequence
	 * begins at the specified {@code start} and extends to the byte at index
	 * {@code end - 1} or to the end of the sequence if no such byte exists. If
	 * {@code start} is equal to {@code end}, no changes are made.
	 * 
	 * @param start
	 *            the beginning index, inclusive
	 * @param end
	 *            the ending index, exclusive
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative, greater than {@code length()},
	 *             or greater than {@code end}
	 */
	public BytesBuilder delete(int start, int end) {
		int count = m_length;
		if (end > count)
			end = count;
		if (start > end)
			throw new IndexOutOfBoundsException();

		int length = end - start;
		if (length > 0) {
			byte[] v = m_value;
			System.arraycopy(v, end, v, start, count - end);
			m_length = count - length;
		}
		return this;
	}

	/**
	 * Removes the byte at the specified position in this sequence.
	 * 
	 * @param index
	 *            the index of the byte to remove
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the {@code index} is negative or greater than or equal to
	 *             {@code length()}
	 */
	public BytesBuilder deleteByteAt(int index) {
		int length = m_length - 1;
		System.arraycopy(m_value, index + 1, m_value, index, length - index);
		m_length = length;
		return this;
	}

	/**
	 * Replaces the bytes in a subsequence of this sequence with bytes in the
	 * specified {@code bs}. The subsequence begins at the specified
	 * {@code start} and extends to the byte at index {@code end - 1} or to the
	 * end of the sequence if no such byte exists. First the bytes in the
	 * subsequence are removed and then the specified {@code bs} is inserted at
	 * {@code start}. (This sequence will be lengthened to accommodate the
	 * specified sequence if necessary.)
	 * 
	 * @param start
	 *            the beginning index, inclusive
	 * @param end
	 *            the ending index, exclusive
	 * @param bs
	 *            the byte sequence that will replace previous contents
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative, greater than {@code length()},
	 *             or greater than {@code end}
	 */
	public BytesBuilder replace(int start, int end, IByteSequence bs) {
		int newLength = m_length;

		if (end > newLength)
			end = newLength;

		if (start > end)
			throw new IndexOutOfBoundsException();

		int length = bs.length();
		newLength += length - (end - start);
		if (newLength > m_value.length)
			expandCapacity(newLength);

		byte[] v = m_value;
		System.arraycopy(v, end, v, start + length, m_length - end);
		bs.getBytes(0, length, v, start);
		m_length = newLength;
		return this;
	}

	/**
	 * Causes this byte sequence to be replaced by the reverse of the sequence.
	 * 
	 * @return this object.
	 */
	public BytesBuilder reverse() {
		byte[] v = m_value;
		for (int i = 0, j = m_length; i < j; ++i, --j) {
			byte b = v[i];
			v[i] = v[j];
			v[j] = b;
		}

		return this;
	}

	/**
	 * Sets the byte {@code b} at the specified {@code index}.
	 * 
	 * @param index
	 *            the index of the byte to set
	 * @param b
	 *            the new byte
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or greater than or equal to
	 *             {@link #length()}
	 */
	public void setByteAt(int index, byte b) {
		if (index >= m_length)
			throw new IndexOutOfBoundsException();

		m_value[index] = b;
	}

	/**
	 * Sets the byte {@code b} at the specified {@code index}. This method
	 * behaves exactly the same as {@link #setByteAt(int, byte)}.
	 * 
	 * @param index
	 *            the index of the byte to set
	 * @param b
	 *            the new byte
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or greater than or equal to
	 *             {@link #length()}
	 */
	public void update(int index, byte b) {
		m_value[index] = b;
	}

	/**
	 * Returns a byte array containing the data that this {@code BytesBuilder}
	 * currently holds.
	 * 
	 * @return a copy of the data currently held in this {@code BytesBuilder}
	 */
	public byte[] toBytes() {
		int length = m_length;
		byte[] copy = new byte[length];
		System.arraycopy(m_value, 0, copy, 0, length);
		return copy;
	}

	/**
	 * Returns a {@code ByteBuffer} backed by the byte array in this
	 * {@code BytesBuilder}. Its capacity is {@link #capacity()}, its position
	 * is {@code offset}, its limit is {@code (offset + length)}, its mark is
	 * undefined.
	 * 
	 * @param offset
	 *            the offset of the subarray of the backing byte array
	 * @param length
	 *            the length of the subarray of the backing byte array
	 * @return a {@code ByteBuffer}
	 * @throws IndexOutOfBoundsException
	 *             if the preconditions on the offset and length arguments do
	 *             not hold
	 */
	public ByteBuffer getByteBuffer(int offset, int length) {
		ByteBuffer byteBuffer = m_byteBuffer;
		if (byteBuffer == null) {
			byteBuffer = ByteBuffer.wrap(m_value, offset, length);
			m_byteBuffer = byteBuffer;
		} else {
			byteBuffer.clear();
			byteBuffer.position(offset);
			byteBuffer.limit(offset + length);
		}

		return byteBuffer;
	}

	/**
	 * Writes all bytes in this {@code BytesBuilder} to the specified
	 * {@code out}.
	 * 
	 * @param out
	 *            the {@code OutputStream} to write to
	 * @throws IOException
	 *             if any IO error occurs
	 * @since 1.2
	 */
	public void write(OutputStream out) throws IOException {
		out.write(m_value, 0, m_length);
	}

	/**
	 * Writes {@code length} number of bytes in this {@code BytesBuilder},
	 * starting at the specified {@code offset}, to the specified {@code out}.
	 * 
	 * @param out
	 *            the {@code OutputStream} to write to
	 * @param offset
	 *            the index of the first byte to write
	 * @param length
	 *            the number of bytes to write
	 * @throws IndexOutOfBoundsException
	 *             if the preconditions on the offset and length arguments do
	 *             not hold
	 * @throws IOException
	 *             if any IO error occurs
	 * @since 1.2
	 */
	public void write(OutputStream out, int offset, int length) throws IOException {
		if (offset < 0 || offset + length > m_length)
			throw new IndexOutOfBoundsException();

		out.write(m_value, offset, length);
	}

	/**
	 * Reads {@code length} number of bytes from the specified {@code in} into
	 * this {@code  BytesBuilder}, or fails if there are not enough left (EOF
	 * reached).
	 * 
	 * @param in
	 *            the {@code InputStream} to read from
	 * @param length
	 *            the number of bytes to read
	 * @throws IOException
	 *             if any IO error occurs
	 * @throws IllegalArgumentException
	 *             if {@code length} is negative
	 * @throws EOFException
	 *             if EOF is reached before the requested number of bytes is
	 *             read
	 * @since 1.2
	 */
	public void read(InputStream in, int length) throws IOException {
		if (length < 0)
			throw new IllegalArgumentException("Length must not be negative: " + length);
		int offset = m_length;
		ensureCapacity(offset + length);
		final byte[] value = m_value;
		int n;
		while (length > 0 && (n = in.read(value, offset, length)) > 0) {
			offset += n;
			length -= n;
		}

		if (length > 0)
			throw new EOFException();

		m_length = offset;
	}

	/**
	 * Reads all bytes from the specified {@code in} into this
	 * {@code  BytesBuilder}.
	 *
	 * @param in
	 *            the {@code InputStream} to read from
	 * @return number of bytes read from the given {@code in}
	 * @throws IOException
	 *             if any IO error occurs
	 * @since 2.0
	 */
	public int read(InputStream in) throws IOException {
		byte[] value = m_value;
		final int count = m_length;
		int offset = count;
		int length = value.length - offset;
		for (;;) {
			if (length < 1) {
				ensureCapacity(offset + 512);
				value = m_value;
				length = value.length - offset;
			}

			final int n = in.read(value, offset, length);
			offset += n;
			if (n < 1)
				break;

			length -= n;
		}
		m_length = offset;

		return offset - count;
	}

	private void expandCapacity(int minCapacity) {
		int newCapacity = (m_value.length + 1) << 1;
		if (newCapacity < 0)
			newCapacity = Integer.MAX_VALUE;
		else if (minCapacity > newCapacity)
			newCapacity = minCapacity;

		byte[] value = new byte[newCapacity];
		System.arraycopy(m_value, 0, value, 0, m_length);
		m_value = value;
		m_byteBuffer = null;
	}

	private void expandCapacityForInsertion(int index, int len, int minCapacity) {
		int newCapacity = (m_value.length + 1) << 1;
		if (newCapacity < 0)
			newCapacity = Integer.MAX_VALUE;
		else if (minCapacity > newCapacity)
			newCapacity = minCapacity;

		byte[] value = new byte[newCapacity];
		System.arraycopy(m_value, 0, value, 0, index);
		if (index < m_length)
			System.arraycopy(m_value, index, value, index + len, m_length - index);
		m_value = value;
		m_byteBuffer = null;
	}
}
