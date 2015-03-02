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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.CharBuffer;

/**
 * A utility class for building a {@code String}.
 * <p>
 * The static methods {@code get} are used to get the instance of this class. If
 * there are some instances currently available in the thread local cache, one
 * will be returned. Otherwise, a new instance will be created and returned. The
 * method {@code close} is used to recycle this instance into the local cache of
 * the current thread for it can be reused in this thread before being GC'ed.
 */
public final class StringBuilder implements Serializable, Appendable, CharSequence, ICloseable {

	private static final long serialVersionUID = -6951364133799695673L;
	private static final int DEFAULT_CAPACITY = 256;
	private static final int HM_BYTES_PERROW = 16;
	private static final int HM_DISTANCE = 3 * (HM_BYTES_PERROW + 1);
	private static final int LSL = StrUtil.getLineSeparator().length();
	private static final char[] c_ls = StrUtil.getLineSeparator().toCharArray();
	private static final char[] c_bhDigits = new char[256];
	private static final char[] c_blDigits = new char[256];
	private static final char[] c_bhDigitsLower = new char[256];
	private static final char[] c_blDigitsLower = new char[256];
	private static final IThreadLocalCache<StringBuilder> c_cache = ThreadLocalCache.weakArrayCache();
	private char[] m_value;
	private int m_length;
	private transient CharBuffer m_charBuffer;

	static {
		for (int i = 0; i < 256; ++i) {
			int hex = i >> 4;
			if (hex < 10) {
				hex += '0';
				c_bhDigits[i] = (char) hex;
				c_bhDigitsLower[i] = (char) hex;
			} else {
				hex -= 10;
				c_bhDigits[i] = (char) (hex + 'A');
				c_bhDigitsLower[i] = (char) (hex + 'a');
			}

			hex = i & 0x0F;
			if (hex < 10) {
				hex += '0';
				c_blDigits[i] = (char) hex;
				c_blDigitsLower[i] = (char) hex;
			} else {
				hex -= 10;
				c_blDigits[i] = (char) (hex + 'A');
				c_blDigitsLower[i] = (char) (hex + 'a');
			}
		}
	}

	private StringBuilder() {
		this(DEFAULT_CAPACITY);
	}

	private StringBuilder(int capacity) {
		m_value = new char[capacity];
	}

	/**
	 * Gets a string builder with a minimum capacity of {@code 256}.
	 * 
	 * @return a {@code StringBuilder} object.
	 * @see #capacity()
	 */
	public static StringBuilder get() {
		StringBuilder builder = c_cache.take();
		if (builder == null)
			builder = new StringBuilder();

		return builder;
	}

	/**
	 * Gets a string builder with the specified minimum capacity.
	 * 
	 * @param capacity
	 *            the desired minimum capacity.
	 * @return a {@code StringBuilder} object.
	 * @throws NegativeArraySizeException
	 *             if the {@code capacity} argument is negative.
	 */
	public static StringBuilder get(int capacity) {
		StringBuilder builder = c_cache.take();
		if (builder == null)
			builder = new StringBuilder(capacity);
		else
			builder.ensureCapacity(capacity);

		return builder;
	}

	/**
	 * Gets a string builder that is initialized with the contents of the
	 * specified {@code CharSequence}. The minimum capacity of the string
	 * builder is the length of the {@code cs} plus {@code 16}.
	 * 
	 * @param cs
	 *            the sequence to copy.
	 * @return a {@code StringBuilder} object.
	 * @throws NullPointerException
	 *             if {@code csq} is {@code null}
	 */
	public static StringBuilder get(CharSequence cs) {
		StringBuilder builder = c_cache.take();
		if (builder == null)
			builder = new StringBuilder(cs.length() + DEFAULT_CAPACITY);
		else
			builder.ensureCapacity(cs.length() + DEFAULT_CAPACITY);

		builder.append(cs);
		return builder;
	}

	/**
	 * Gets a string builder initialized to the contents of the specified
	 * string. The minimum capacity of the string builder is the length of the
	 * {@code str} plus {@code 16}.
	 * 
	 * @param str
	 *            the {@code String} to be copied into the builder
	 * @return a {@code StringBuilder} object.
	 * @throws NullPointerException
	 *             if {@code str} is {@code null}
	 */
	public static StringBuilder get(String str) {
		StringBuilder builder = c_cache.take();
		if (builder == null)
			builder = new StringBuilder(str.length() + DEFAULT_CAPACITY);
		else
			builder.ensureCapacity(str.length() + DEFAULT_CAPACITY);

		builder.append(str);
		return builder;
	}

	/**
	 * Releases this string builder to the thread local cache so that it can be
	 * reused before being GC'ed.
	 * 
	 * <p>
	 * The reference to this string builder must not be used anymore after this
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
	 * @return the number of characters currently held in this string builder
	 */
	@Override
	public int length() {
		return m_length;
	}

	/**
	 * Returns the number of characters that can be held without growing.
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
	 * Returns a new {@code String} that contains a subsequence of characters
	 * currently contained in this character sequence with leading and trailing
	 * whitespace omitted.
	 * 
	 * @return the new string.
	 */
	public String trim() {
		int i = 0;
		int j = m_length - 1;
		char[] value = m_value;
		for (; j > i && Character.isWhitespace(value[j]); --j)
			;
		for (; i <= j && Character.isWhitespace(value[i]); ++i)
			;

		if (i > j)
			return "";

		return new String(value, i, j - i + 1);
	}

	/**
	 * Trims off any extra capacity beyond the current length.
	 */
	public void trimToSize() {
		if (m_length < m_value.length) {
			char[] value = new char[m_length];
			System.arraycopy(m_value, 0, value, 0, value.length);
			m_value = value;
		}
	}

	/**
	 * Sets the current length to the specified {@code newLength}. If the new
	 * length is greater than the current length, then the new characters at the
	 * end of this object are filled with {@code '\0'}.
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

	/**
	 * Returns the character at the specified {@code index}.
	 * 
	 * @param index
	 *            the index of the desired {@code char} value.
	 * @return the {@code char} value at the specified index.
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or greater than or equal to
	 *             {@link #length()}.
	 */
	@Override
	public char charAt(int index) {
		if (index >= m_length)
			throw new IndexOutOfBoundsException();

		return m_value[index];
	}

	/**
	 * Returns the Unicode code point value at the specified {@code index}.
	 * 
	 * @param index
	 *            the index to the {@code char} value
	 * @return the code point value of the character at the given {@code index}
	 * @throws IndexOutOfBoundsException
	 *             if the {@code index} argument is negative or not less than
	 *             {@link #length()}.
	 */
	public int codePointAt(int index) {
		if (index >= m_length)
			throw new IndexOutOfBoundsException();

		char c1 = m_value[index];
		if (Character.isHighSurrogate(c1) && ++index < m_length) {
			char c2 = m_value[index];
			if (Character.isLowSurrogate(c2))
				return Character.toCodePoint(c1, c2);
		}

		return c1;
	}

	/**
	 * Returns the Unicode code point value preceding the given {@code index}.
	 * 
	 * @param index
	 *            the index following the code point to be returned
	 * @return the Unicode code point value before the given {@code index}.
	 * @throws IndexOutOfBoundsException
	 *             if the {@code index} argument is less than 1 or greater than
	 *             {@link #length()}.
	 */
	public int codePointBefore(int index) {
		if (index > m_length)
			throw new IndexOutOfBoundsException();

		char[] v = m_value;
		char c2 = v[--index];
		if (Character.isLowSurrogate(c2) && index > 0) {
			char c1 = v[--index];
			if (Character.isHighSurrogate(c1))
				return Character.toCodePoint(c1, c2);
		}
		return c2;
	}

	/**
	 * Returns the number of Unicode code points between {@code beginIndex} and
	 * {@code endIndex}.
	 * 
	 * @param beginIndex
	 *            the index to the first {@code char} that is included
	 * @param endIndex
	 *            the index after the last {@code char} that is excluded
	 * @return the number of Unicode code points in the specified text range
	 * @throws IndexOutOfBoundsException
	 *             if the {@code beginIndex} is negative, or {@code endIndex} is
	 *             greater than {@link #length()}, or {@code beginIndex} is
	 *             greater than {@code endIndex}.
	 */
	public int codePointCount(int beginIndex, int endIndex) {
		if (endIndex > m_length || beginIndex > endIndex)
			throw new IndexOutOfBoundsException();

		int n = 0;
		char[] v = m_value;
		while (beginIndex < endIndex) {
			++n;
			if (Character.isHighSurrogate(v[beginIndex]) && ++beginIndex < endIndex
					&& Character.isLowSurrogate(v[beginIndex]))
				++beginIndex;
		}

		return n;
	}

	/**
	 * Returns the index that is offset {@code codePointOffset} code points from
	 * {@code index}.
	 * 
	 * @param index
	 *            the index to be offset
	 * @param codePointOffset
	 *            the offset in code points
	 * @return the index within this sequence
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or greater than
	 *             {@link #length()} or if there aren't enough code points
	 *             before or after {@code index} to match
	 *             {@code codePointOffset}.
	 */
	public int offsetByCodePoints(int index, int codePointOffset) {
		if (index > m_length)
			throw new IndexOutOfBoundsException();

		char[] v = m_value;
		if (codePointOffset >= 0) {
			int length = m_length;
			for (; index < length && codePointOffset > 0; --codePointOffset) {
				if (Character.isHighSurrogate(v[index]) && ++index < length && Character.isLowSurrogate(v[index]))
					++index;
			}

			if (codePointOffset > 0)
				throw new IndexOutOfBoundsException();
		} else {
			for (; index > 0 && codePointOffset < 0; ++codePointOffset) {
				if (Character.isLowSurrogate(v[--index]) && index > 0 && Character.isHighSurrogate(v[index - 1]))
					--index;
			}

			if (codePointOffset < 0)
				throw new IndexOutOfBoundsException();
		}

		return index;
	}

	/**
	 * Copies the requested sequence of characters to the given {@code dst}
	 * starting at {@code dstBegin}.
	 * 
	 * @param srcBegin
	 *            start copying at this offset.
	 * @param srcEnd
	 *            stop copying at this offset.
	 * @param dst
	 *            the array to copy the data into.
	 * @param dstBegin
	 *            offset into {@code dst}.
	 * @throws NullPointerException
	 *             if {@code dst} is {@code null}.
	 * @throws IndexOutOfBoundsException
	 *             if any of the following is true:
	 *             <ul>
	 *             <li>{@code srcBegin} is negative
	 *             <li>{@code dstBegin} is negative
	 *             <li>the {@code srcBegin} argument is greater than the
	 *             {@code srcEnd} argument.
	 *             <li>{@code srcEnd} is greater than {@link #length()}.
	 *             <li>{@code dstBegin+srcEnd-srcBegin} is greater than
	 *             {@code dst.length}
	 *             </ul>
	 */
	public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
		if (srcEnd > m_length || srcBegin > srcEnd)
			throw new IndexOutOfBoundsException();

		System.arraycopy(m_value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
	}

	/**
	 * Sets the character {@code c} at the specified {@code index}.
	 * 
	 * @param index
	 *            the index of the character to set.
	 * @param c
	 *            the new character.
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or greater than or equal to
	 *             {@link #length()}.
	 */
	public void setCharAt(int index, char c) {
		if (index >= m_length)
			throw new IndexOutOfBoundsException();

		m_value[index] = c;
	}

	/**
	 * Appends the string representation of the {@code Object} argument.
	 * <p>
	 * If the argument is an instance of {@code IDumpable}, then {@code append(
	 * IDumpable)} is invoked. Otherwise the argument is converted to a string
	 * as if by the method {@code String.valueOf}, and the characters of that
	 * string are then appended to this sequence.
	 * 
	 * @param obj
	 *            an {@code Object}
	 * @return this object
	 */
	public StringBuilder append(Object obj) {
		if (obj == null)
			return append("null");

		if (obj instanceof IDumpable)
			return append((IDumpable) obj);

		return append(obj.toString());
	}

	/**
	 * Appends the contents of the specified string. If the string is
	 * {@code null}, then the string {@code "null"} is appended.
	 * 
	 * @param str
	 *            a string
	 * @return this object
	 */
	public StringBuilder append(String str) {
		if (str == null)
			str = "null";

		int len = str.length();
		int newLength = m_length + len;
		if (newLength > m_value.length)
			expandCapacity(newLength);
		str.getChars(0, len, m_value, m_length);
		m_length = newLength;
		return this;
	}

	/**
	 * Appends the contents of the specified {@code StringBuffer}. If the
	 * {@code StringBuffer} is {@code null}, then the string {@code "null"} is
	 * appended.
	 * 
	 * @param sb
	 *            the <tt>StringBuffer</tt> to append
	 * @return this object
	 */
	public StringBuilder append(StringBuffer sb) {
		if (sb == null)
			return append("null");

		int len = sb.length();
		int newLength = m_length + len;
		if (newLength > m_value.length)
			expandCapacity(newLength);
		sb.getChars(0, len, m_value, m_length);
		m_length = newLength;
		return this;
	}

	/**
	 * Appends the contents of the specified {@code java.lang.StringBuilder}. If
	 * the string builder is {@code null}, then the string {@code "null"} is
	 * appended.
	 * 
	 * @param sb
	 *            the <tt>java.lang.StringBuilder</tt> to append
	 * @return this object
	 */
	public StringBuilder append(java.lang.StringBuilder sb) {
		if (sb == null)
			return append("null");

		int len = sb.length();
		int newLength = m_length + len;
		if (newLength > m_value.length)
			expandCapacity(newLength);
		sb.getChars(0, len, m_value, m_length);
		m_length = newLength;
		return this;
	}

	/**
	 * Appends the contents of the specified {@code StringBuilder}. If the
	 * string builder is {@code null}, then the string {@code "null"} is
	 * appended.
	 * 
	 * @param sb
	 *            the {@code StringBuilder} to append
	 * @return this object
	 */
	public StringBuilder append(StringBuilder sb) {
		if (sb == null)
			return append("null");

		int len = sb.length();
		int newLength = m_length + len;
		if (newLength > m_value.length)
			expandCapacity(newLength);
		sb.getChars(0, len, m_value, m_length);
		m_length = newLength;
		return this;
	}

	/**
	 * Appends the string representation of the specified {@code CharSequence}.
	 * If the {@code CharSequence} is {@code null}, then the string
	 * {@code "null"} is appended.
	 * 
	 * @param cs
	 *            the {@code CharSequence} to append
	 * 
	 * @return this object
	 */
	@Override
	public StringBuilder append(CharSequence cs) {
		if (cs == null)
			return append("null");

		return append(cs, 0, cs.length());
	}

	/**
	 * Appends the string representation of the specified subsequence of the
	 * {@code CharSequence}. If the {@code CharSequence} is {@code null}, then
	 * the string {@code "null"} is appended.
	 * 
	 * @param cs
	 *            the sequence to append
	 * @param start
	 *            the starting index of the subsequence to be appended
	 * @param end
	 *            the end index of the subsequence to be appended
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} or {@code end} are negative, or
	 *             {@code start} is greater than {@code end} or {@code end} is
	 *             greater than {@code cs.length()}
	 */
	@Override
	public StringBuilder append(CharSequence cs, int start, int end) {
		if (cs == null)
			return append("null");

		if (start > end || end > cs.length())
			throw new IndexOutOfBoundsException();

		int len = end - start;
		if (len == 0)
			return this;

		int n = m_length;
		int newLength = n + len;
		if (newLength > m_value.length)
			expandCapacity(newLength);
		char[] value = m_value;
		for (int i = start; i < end; ++i, ++n)
			value[n] = cs.charAt(i);

		m_length = newLength;
		return this;
	}

	/**
	 * Appends a subsequence of the specified {@code String} to this sequence.
	 * <p>
	 * Characters of the argument {@code str}, starting at index {@code start},
	 * are appended, in order, to the contents of this sequence up to the
	 * (exclusive) index {@code end}. The length of this sequence is increased
	 * by the value of {@code end - start}.
	 * <p>
	 * Let <i>n</i> be the length of this character sequence just prior to
	 * execution of the {@code append} method. Then the character at index
	 * <i>k</i> in this character sequence becomes equal to the character at
	 * index <i>k</i> in this sequence, if <i>k</i> is less than <i>n</i>;
	 * otherwise, it is equal to the character at index <i>k+start-n</i> in the
	 * argument {@code str}.
	 * <p>
	 * If {@code str} is {@code null}, then this method appends characters as if
	 * the str parameter was a sequence containing the four characters
	 * {@code "null"}.
	 * 
	 * @param str
	 *            the string to append
	 * @param start
	 *            the starting index of the subsequence to be appended
	 * @param end
	 *            the end index of the subsequence to be appended
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} or {@code end} are negative, or
	 *             {@code start} is greater than {@code end} or {@code end} is
	 *             greater than {@code str.length()}
	 */
	public StringBuilder append(String str, int start, int end) {
		if (str == null)
			return append("null");

		if (start > end || end > str.length())
			throw new IndexOutOfBoundsException();

		int newLength = m_length + end - start;
		if (newLength > m_value.length)
			expandCapacity(newLength);

		str.getChars(start, end, m_value, m_length);
		m_length = newLength;
		return this;
	}

	/**
	 * Appends the data dumped by the specified {@code dumpable} to this
	 * {@code StringBuilder}.
	 * 
	 * @param dumpable
	 *            the object to dump
	 * @return this object
	 */
	public StringBuilder append(IDumpable dumpable) {
		dumpable.dump(this);
		return this;
	}

	/**
	 * Appends the string representation of the {@code char} array argument to
	 * this sequence.
	 * <p>
	 * The characters of the array argument are appended, in order, to the
	 * contents of this sequence. The length of this sequence increases by the
	 * length of the argument.
	 * <p>
	 * The overall effect is exactly as if the argument were converted to a
	 * string by the method {@code String.valueOf(char[])} and the characters of
	 * that string were then {@link #append(String) appended} to this character
	 * sequence.
	 * 
	 * @param chars
	 *            the characters to be appended
	 * @return this object
	 */
	public StringBuilder append(char[] chars) {
		int newLength = m_length + chars.length;
		if (newLength > m_value.length)
			expandCapacity(newLength);
		System.arraycopy(chars, 0, m_value, m_length, chars.length);
		m_length = newLength;
		return this;
	}

	/**
	 * Appends the string representation of a subarray of the {@code char} array
	 * argument to this sequence.
	 * <p>
	 * Characters of the {@code char} array {@code chars}, starting at index
	 * {@code offset}, are appended, in order, to the contents of this sequence.
	 * The length of this sequence increases by the value of {@code len}.
	 * <p>
	 * The overall effect is exactly as if the arguments were converted to a
	 * string by the method {@code String.valueOf(char[],int,int)} and the
	 * characters of that string were then {@link #append(String) appended} to
	 * this character sequence.
	 * 
	 * @param chars
	 *            the characters to be appended
	 * @param offset
	 *            the index of the first {@code char} to append
	 * @param len
	 *            the number of {@code char}csq to append
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the preconditions on the given {@code offset} and
	 *             {@code len} do not hold
	 */
	public StringBuilder append(char[] chars, int offset, int len) {
		int newLength = m_length + len;
		if (newLength > m_value.length)
			expandCapacity(newLength);
		System.arraycopy(chars, offset, m_value, m_length, len);
		m_length = newLength;
		return this;
	}

	/**
	 * Appends the string representation of the {@code boolean} argument to the
	 * sequence.
	 * <p>
	 * The argument is converted to a string as if by the method
	 * {@code String.valueOf}, and the characters of that string are then
	 * appended to this sequence.
	 * 
	 * @param b
	 *            a {@code boolean}
	 * @return this object
	 */
	public StringBuilder append(boolean b) {
		return append(String.valueOf(b));
	}

	/**
	 * Appends the string representation of the {@code char} argument to this
	 * sequence.
	 * <p>
	 * The argument is appended to the contents of this sequence. The length of
	 * this sequence increases by {@code 1}.
	 * <p>
	 * The overall effect is exactly as if the argument were converted to a
	 * string by the method {@code String.valueOf(char)} and the character in
	 * that string were then {@link #append(String) appended} to this character
	 * sequence.
	 * 
	 * @param c
	 *            a {@code char}
	 * @return this object
	 */
	@Override
	public StringBuilder append(char c) {
		int newLength = m_length + 1;
		if (newLength > m_value.length)
			expandCapacity(newLength);
		m_value[m_length] = c;
		m_length = newLength;
		return this;
	}

	/**
	 * Appends the string representation of the {@code int} argument to this
	 * sequence.
	 * <p>
	 * The argument is converted to a string as if by the method
	 * {@code String.valueOf}, and the characters of that string are then
	 * appended to this sequence.
	 * 
	 * @param i
	 *            an {@code int}
	 * @return this object
	 */
	public StringBuilder append(int i) {
		if (i == Integer.MIN_VALUE)
			return append("-2147483648");

		int len = i < 0 ? StrUtil.stringSizeOfInt(-i) + 1 : StrUtil.stringSizeOfInt(i);
		int newLength = m_length + len;
		if (newLength > m_value.length)
			expandCapacity(newLength);
		StrUtil.getChars(i, newLength, m_value);
		m_length = newLength;
		return this;
	}

	/**
	 * Appends the string representation of the {@code long} argument to this
	 * sequence.
	 * <p>
	 * The argument is converted to a string as if by the method
	 * {@code String.valueOf}, and the characters of that string are then
	 * appended to this sequence.
	 * 
	 * @param l
	 *            a {@code long}
	 * @return this object
	 */
	public StringBuilder append(long l) {
		if (l == Long.MIN_VALUE)
			return append("-9223372036854775808");

		int len = (l < 0L) ? StrUtil.stringSizeOfLong(-l) + 1 : StrUtil.stringSizeOfLong(l);
		int newLength = m_length + len;
		if (newLength > m_value.length)
			expandCapacity(newLength);
		StrUtil.getChars(l, newLength, m_value);
		m_length = newLength;
		return this;
	}

	/**
	 * Appends the string representation of the {@code float} argument to this
	 * sequence.
	 * <p>
	 * The argument is converted to a string as if by the method
	 * {@code String.valueOf}, and the characters of that string are then
	 * appended to this string sequence.
	 * 
	 * @param f
	 *            a {@code float}
	 * @return this object
	 */
	public StringBuilder append(float f) {
		return append(Float.toString(f));
	}

	/**
	 * Appends the string representation of the {@code double} argument to this
	 * sequence.
	 * <p>
	 * The argument is converted to a string as if by the method
	 * {@code String.valueOf}, and the characters of that string are then
	 * appended to this sequence.
	 * 
	 * @param d
	 *            a {@code double}
	 * @return this object
	 */
	public StringBuilder append(double d) {
		return append(Double.toString(d));
	}

	/**
	 * Appends the given byte {@code b} to this string builder as 2 hex
	 * characters.
	 * <p>
	 * If the given {@code lowerCase} is true, then 'a'-'f' is used. Otherwise,
	 * 'A'-'F' is used.
	 * </p>
	 * 
	 * @param b
	 *            the {@code byte} to be interpreted and appended
	 * @param lowerCase
	 *            true to use 'a'-'f'; false to use 'A'-'F'
	 * 
	 * @return this object
	 * @since 2.0
	 */
	public StringBuilder appendHex(byte b, boolean lowerCase) {
		final int length = m_length;
		final int newLength = length + 2;
		if (newLength > m_value.length)
			expandCapacity(newLength);

		final char[] bh;
		final char[] bl;
		if (lowerCase) {
			bh = c_bhDigitsLower;
			bl = c_blDigitsLower;
		} else {
			bh = c_bhDigits;
			bl = c_blDigits;
		}
		final int bt = b & 0xFF;
		m_value[length] = bh[bt];
		m_value[length + 1] = bl[bt];
		m_length = newLength;
		return this;
	}

	/**
	 * Appends the given byte {@code b} to this string builder as 2 hex
	 * characters. Character '0'-'9' and 'A'-'F' are used.
	 *
	 * @param b
	 *            the {@code byte} to be interpreted and appended
	 *
	 * @return this object
	 */
	public StringBuilder appendHex(byte b) {
		return appendHex(b, false);
	}

	/**
	 * Appends the given short {@code s} to this string builder as 4 hex
	 * characters.
	 * <p>
	 * If the given {@code lowerCase} is true, then 'a'-'f' is used. Otherwise,
	 * 'A'-'F' is used.
	 * </p>
	 *
	 * @param s
	 *            the {@code short} to be interpreted and appended
	 * @param lowerCase
	 *            true to use 'a'-'f'; false to use 'A'-'F'
	 * @return this object
	 * @since 2.0
	 */
	public StringBuilder appendHex(short s, boolean lowerCase) {
		int newLength = m_length + 4;
		if (newLength > m_value.length)
			expandCapacity(newLength);

		final char[] bh;
		final char[] bl;
		if (lowerCase) {
			bh = c_bhDigitsLower;
			bl = c_blDigitsLower;
		} else {
			bh = c_bhDigits;
			bl = c_blDigits;
		}
		final char[] value = m_value;
		newLength = m_length;
		int b = (s >> 8) & 0xFF;
		value[newLength] = bh[b];
		value[++newLength] = bl[b];

		b = s & 0xFF;
		value[++newLength] = bh[b];
		value[++newLength] = bl[b];

		m_length = ++newLength;
		return this;
	}

	/**
	 * Appends the given short {@code s} to this string builder as 4 hex
	 * characters. Character '0'-'9' and 'A'-'F' are used.
	 * 
	 * @param s
	 *            the {@code short} to be interpreted and appended
	 * @return this object
	 */
	public StringBuilder appendHex(short s) {
		return appendHex(s, false);
	}

	/**
	 * Appends the given int {@code i} to this string builder as 8 hex
	 * characters.
	 * <p>
	 * If the given {@code lowerCase} is true, then 'a'-'f' is used. Otherwise,
	 * 'A'-'F' is used.
	 * </p>
	 *
	 * @param i
	 *            the {@code int} to be interpreted and appended
	 * @param lowerCase
	 *            true to use 'a'-'f'; false to use 'A'-'F'
	 * @return this object
	 * @since 2.0
	 */
	public StringBuilder appendHex(int i, boolean lowerCase) {
		int newLength = m_length + 8;
		if (newLength > m_value.length)
			expandCapacity(newLength);

		final char[] bh;
		final char[] bl;
		if (lowerCase) {
			bh = c_bhDigitsLower;
			bl = c_blDigitsLower;
		} else {
			bh = c_bhDigits;
			bl = c_blDigits;
		}
		final char[] value = m_value;
		newLength = m_length;
		for (int bits = 24; bits >= 0; bits -= 8, ++newLength) {
			int b = (i >> bits) & 0xFF;
			value[newLength] = bh[b];
			value[++newLength] = bl[b];
		}
		m_length = newLength;
		return this;
	}

	/**
	 * Appends the given int {@code i} to this string builder as 8 hex
	 * characters. Character '0'-'9' and 'A'-'F' are used.
	 * 
	 * @param i
	 *            the {@code int} to be interpreted and appended
	 * @return this object
	 */
	public StringBuilder appendHex(int i) {
		return appendHex(i, false);
	}

	/**
	 * Appends the given long {@code l} to this string builder as 16 hex
	 * characters.
	 * <p>
	 * If the given {@code lowerCase} is true, then 'a'-'f' is used. Otherwise,
	 * 'A'-'F' is used.
	 * </p>
	 *
	 * @param l
	 *            the {@code long} to be interpreted and appended
	 * @param lowerCase
	 *            true to use 'a'-'f'; false to use 'A'-'F'
	 * @return this object
	 * @since 2.0
	 */
	public StringBuilder appendHex(long l, boolean lowerCase) {
		final int newLength = m_length + 16;
		if (newLength > m_value.length)
			expandCapacity(newLength);

		final char[] bh;
		final char[] bl;
		if (lowerCase) {
			bh = c_bhDigitsLower;
			bl = c_blDigitsLower;
		} else {
			bh = c_bhDigits;
			bl = c_blDigits;
		}
		final char[] value = m_value;
		int c1 = m_length;
		int c2 = c1 + 8;
		final int i1 = (int) (l >>> 32);
		final int i2 = (int) l;
		for (int bits = 24; bits >= 0; bits -= 8, ++c1, ++c2) {
			int b = (i1 >> bits) & 0xFF;
			value[c1] = bh[b];
			value[++c1] = bl[b];

			b = (i2 >> bits) & 0xFF;
			value[c2] = bh[b];
			value[++c2] = bl[b];
		}
		m_length = newLength;
		return this;
	}

	/**
	 * Appends the given long {@code l} to this string builder as 16 hex
	 * characters. Character '0'-'9' and 'A'-'F' are used.
	 * 
	 * @param l
	 *            the {@code long} to be interpreted and appended
	 * @return this object
	 */
	public StringBuilder appendHex(long l) {
		return appendHex(l, false);
	}

	/**
	 * Behaves exactly the same way as the following.
	 *
	 * <pre>
	 * {@code appendHex(b, 0, b.length, lowerCase)}
	 * </pre>
	 *
	 * @param b
	 *            the byte array to be interpreted and appended
	 * @param lowerCase
	 *            true to use 'a'-'f'; false to use 'A'-'F'
	 * @return this object
	 * @since 2.0
	 */
	public StringBuilder appendHex(byte[] b, boolean lowerCase) {
		return appendHexInternal(b, 0, b.length, lowerCase);
	}

	/**
	 * Behaves exactly the same way as the following.
	 * 
	 * <pre>
	 * {@code appendHex(b, 0, b.length)}
	 * </pre>
	 * 
	 * @param b
	 *            the byte array to be interpreted and appended
	 * @return this object
	 */
	public StringBuilder appendHex(byte[] b) {
		return appendHexInternal(b, 0, b.length, false);
	}

	/**
	 * Appends the given {@code length} of bytes starting at the given
	 * {@code offset} of the given byte array {@code b} to this string builder
	 * as a hex string.
	 * <p>
	 * If the given {@code lowerCase} is true, then 'a'-'f' is used. Otherwise,
	 * 'A'-'F' is used.
	 * </p>
	 *
	 * @param b
	 *            the byte array containing the bytes to be interpreted and
	 *            appended
	 * @param offset
	 *            the offset of the first byte to be interpreted and appended
	 * @param length
	 *            the number of bytes to be interpreted and appended
	 * @param lowerCase
	 *            true to use 'a'-'f'; false to use 'A'-'F'
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the preconditions on the given {@code offset} and
	 *             {@code length} do not hold
	 * @since 2.0
	 */
	public StringBuilder appendHex(byte[] b, int offset, int length, boolean lowerCase) {
		if (length < 0)
			throw new IndexOutOfBoundsException();
		return appendHexInternal(b, offset, length, lowerCase);
	}

	/**
	 * Appends the given {@code length} of bytes starting at the given
	 * {@code offset} of the given byte array {@code b} to this string builder
	 * as a hex string. Character '0'-'9' and 'A'-'F' are used.
	 * 
	 * @param b
	 *            the byte array containing the bytes to be interpreted and
	 *            appended
	 * @param offset
	 *            the offset of the first byte to be interpreted and appended
	 * @param length
	 *            the number of bytes to be interpreted and appended
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the preconditions on the given {@code offset} and
	 *             {@code length} do not hold
	 */
	public StringBuilder appendHex(byte[] b, int offset, int length) {
		return appendHexInternal(b, offset, length, false);
	}

	/**
	 * Behaves exactly the same way as the following.
	 * 
	 * <pre>
	 * {@code appendHexDump(b, 0, b.length)}
	 * </pre>
	 * 
	 * @param b
	 *            the byte array to be interpreted and appended
	 * @return this object
	 */
	public StringBuilder appendHexDump(byte[] b) {
		return appendHexDumpInternal(b, 0, b.length);
	}

	/**
	 * Appends the given {@code length} of bytes starting at the given
	 * {@code offset} of the given byte array {@code b} to this string builder
	 * as a hex dump with {@code 16} bytes per row.
	 * 
	 * @param b
	 *            the byte array containing the bytes to be interpreted and
	 *            appended
	 * @param offset
	 *            the offset of the first byte to be interpreted and appended
	 * @param length
	 *            the number of bytes to be interpreted and appended
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the preconditions on the given {@code offset} and
	 *             {@code length} do not hold
	 */
	public StringBuilder appendHexDump(byte[] b, int offset, int length) {
		if (length < 0)
			throw new IndexOutOfBoundsException();
		return appendHexDumpInternal(b, offset, length);
	}

	/**
	 * Appends the specified {@code int} array to this sequence.
	 * <p>
	 * The given {@code array} is expanded to a character sequence as follows.
	 * 
	 * <pre>
	 * {@code "[" + array[0] + ", " + array[1] + ", " + ... + "]"}
	 * </pre>
	 * 
	 * And then the resulting character sequence is appended to this sequence.
	 * 
	 * @param array
	 *            the array to be expanded and appended
	 * @return this object
	 */
	public StringBuilder appendArray(int[] array) {
		append('[');
		int n = array.length;
		if (n > 0) {
			append(array[0]);
			for (int i = 1; i < n; ++i)
				append(", ").append(array[i]);
		}
		append(']');

		return this;
	}

	/**
	 * Appends the specified {@code byte} array to this sequence.
	 * <p>
	 * The given {@code array} is expanded to a character sequence as follows.
	 * 
	 * <pre>
	 * {@code "[" + array[0] + ", " + array[1] + ", " + ... + "]"}
	 * </pre>
	 * 
	 * And then the resulting character sequence is appended to this sequence.
	 * 
	 * @param array
	 *            the array to be expanded and appended
	 * @return this object
	 */
	public StringBuilder appendArray(byte[] array) {
		append('[');
		int n = array.length;
		if (n > 0) {
			append(array[0]);
			for (int i = 1; i < n; ++i)
				append(", ").append(array[i]);
		}
		append(']');

		return this;
	}

	/**
	 * Appends the specified {@code char} array to this sequence.
	 * <p>
	 * The given {@code array} is expanded to a character sequence as follows.
	 * 
	 * <pre>
	 * {@code "[" + array[0] + ", " + array[1] + ", " + ... + "]"}
	 * </pre>
	 * 
	 * And then the resulting character sequence is appended to this sequence.
	 * 
	 * @param array
	 *            the array to be expanded and appended
	 * @return this object
	 */
	public StringBuilder appendArray(char[] array) {
		append('[');
		int n = array.length;
		if (n > 0) {
			append(array[0]);
			for (int i = 1; i < n; ++i)
				append(", ").append(array[i]);
		}
		append(']');

		return this;
	}

	/**
	 * Appends the specified {@code short} array to this sequence.
	 * <p>
	 * The given {@code array} is expanded to a character sequence as follows.
	 * 
	 * <pre>
	 * {@code "[" + array[0] + ", " + array[1] + ", " + ... + "]"}
	 * </pre>
	 * 
	 * And then the resulting character sequence is appended to this sequence.
	 * 
	 * @param array
	 *            the array to be expanded and appended
	 * @return this object
	 */
	public StringBuilder appendArray(short[] array) {
		append('[');
		int n = array.length;
		if (n > 0) {
			append(array[0]);
			for (int i = 1; i < n; ++i)
				append(", ").append(array[i]);
		}
		append(']');

		return this;
	}

	/**
	 * Appends the specified {@code boolean} array to this sequence.
	 * <p>
	 * The given {@code array} is expanded to a character sequence as follows.
	 * 
	 * <pre>
	 * {@code "[" + array[0] + ", " + array[1] + ", " + ... + "]"}
	 * </pre>
	 * 
	 * And then the resulting character sequence is appended to this sequence.
	 * 
	 * @param array
	 *            the array to be expanded and appended
	 * @return this object
	 */
	public StringBuilder appendArray(boolean[] array) {
		append('[');
		int n = array.length;
		if (n > 0) {
			append(array[0]);
			for (int i = 1; i < n; ++i)
				append(", ").append(array[i]);
		}
		append(']');

		return this;
	}

	/**
	 * Appends the specified {@code long} array to this sequence.
	 * <p>
	 * The given {@code array} is expanded to a character sequence as follows.
	 * 
	 * <pre>
	 * {@code "[" + array[0] + ", " + array[1] + ", " + ... + "]"}
	 * </pre>
	 * 
	 * And then the resulting character sequence is appended to this sequence.
	 * 
	 * @param array
	 *            the array to be expanded and appended
	 * @return this object
	 */
	public StringBuilder appendArray(long[] array) {
		append('[');
		int n = array.length;
		if (n > 0) {
			append(array[0]);
			for (int i = 1; i < n; ++i)
				append(", ").append(array[i]);
		}
		append(']');

		return this;
	}

	/**
	 * Appends the specified {@code float} array to this sequence.
	 * <p>
	 * The given {@code array} is expanded to a character sequence as follows.
	 * 
	 * <pre>
	 * {@code "[" + array[0] + ", " + array[1] + ", " + ... + "]"}
	 * </pre>
	 * 
	 * And then the resulting character sequence is appended to this sequence.
	 * 
	 * @param array
	 *            the array to be expanded and appended
	 * @return this object
	 */
	public StringBuilder appendArray(float[] array) {
		append('[');
		int n = array.length;
		if (n > 0) {
			append(array[0]);
			for (int i = 1; i < n; ++i)
				append(", ").append(array[i]);
		}
		append(']');

		return this;
	}

	/**
	 * Appends the specified {@code double} array to this sequence.
	 * <p>
	 * The given {@code array} is expanded to a character sequence as follows.
	 * 
	 * <pre>
	 * {@code "[" + array[0] + ", " + array[1] + ", " + ... + "]"}
	 * </pre>
	 * 
	 * And then the resulting character sequence is appended to this sequence.
	 * 
	 * @param array
	 *            the array to be expanded and appended
	 * @return this object
	 */
	public StringBuilder appendArray(double[] array) {
		append('[');
		int n = array.length;
		if (n > 0) {
			append(array[0]);
			for (int i = 1; i < n; ++i)
				append(", ").append(array[i]);
		}
		append(']');

		return this;
	}

	/**
	 * Appends the specified {@code String} array to this sequence.
	 * <p>
	 * The given {@code array} is expanded to a character sequence as follows.
	 * 
	 * <pre>
	 * {@code "[" + array[0] + ", " + array[1] + ", " + ... + "]"}
	 * </pre>
	 * 
	 * And then the resulting character sequence is appended to this sequence.
	 * 
	 * @param array
	 *            the array to be expanded and appended
	 * @return this object
	 */
	public StringBuilder appendArray(String[] array) {
		append('[');
		int n = array.length;
		if (n > 0) {
			append(array[0]);
			for (int i = 1; i < n; ++i)
				append(", ").append(array[i]);
		}
		append(']');

		return this;
	}

	/**
	 * Appends the specified object array to this sequence.
	 * <p>
	 * If all the members of the given {@code array} are not of an array type,
	 * then this array is expanded to a character sequence as follows.
	 * 
	 * <pre>
	 * {@code "[" + array[0] + ", " + array[1] + ", " + ... + "]"}
	 * </pre>
	 * 
	 * If there's a member of this array which itself is an array too, then this
	 * member is expanded recursively. The final resulting character sequence
	 * will be appended to this sequence.
	 * 
	 * @param array
	 *            the array to be expanded and appended
	 * @return this object
	 */
	public StringBuilder appendArray(Object[] array) {
		try (HashSet seen = HashSet.get()) {
			return appendArray(array, seen);
		}
	}

	/**
	 * Appends {@code count} characters of {@code c} to this
	 * {@code StringBuilder}.
	 * 
	 * @param c
	 *            the character to append
	 * @param count
	 *            the number of the specified characters to append
	 * @return this object
	 * @throws IllegalArgumentException
	 *             if {@code count} is negative
	 */
	public StringBuilder appendFill(char c, int count) {
		if (count < 0)
			throw new IllegalArgumentException();

		int i = m_length;
		int newLength = i + count;
		if (newLength > m_value.length)
			expandCapacity(newLength);

		char[] v = m_value;
		for (; i < newLength; ++i)
			v[i] = c;

		m_length = newLength;
		return this;
	}

	/**
	 * Appends the specified object to this sequence deeply.
	 * <p>
	 * If the given {@code arg} is of an array type, then it will be appended by
	 * calling a corresponding {@code appendArray}. Otherwise its string value
	 * is appended.
	 * 
	 * @param arg
	 *            the object to be appended deeply
	 * @return this object
	 */
	public StringBuilder deeplyAppend(Object arg) {
		Class<?> clazz = null;
		if (arg == null || !(clazz = arg.getClass()).isArray())
			append(arg);
		else if (clazz == int[].class)
			appendArray((int[]) arg);
		else if (clazz == byte[].class)
			appendArray((byte[]) arg);
		else if (clazz == long[].class)
			appendArray((long[]) arg);
		else if (clazz == short[].class)
			appendArray((short[]) arg);
		else if (clazz == float[].class)
			appendArray((float[]) arg);
		else if (clazz == double[].class)
			appendArray((double[]) arg);
		else if (clazz == char[].class)
			appendArray((char[]) arg);
		else if (clazz == String[].class)
			appendArray((String[]) arg);
		else
			appendArray((Object[]) arg);

		return this;
	}

	/**
	 * Removes the characters in a substring of this sequence. The substring
	 * begins at the specified {@code start} and extends to the character at
	 * index {@code end - 1} or to the end of the sequence if no such character
	 * exists. If {@code start} is equal to {@code end}, no changes are made.
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
	public StringBuilder delete(int start, int end) {
		int count = m_length;
		if (end > count)
			end = count;
		if (start > end)
			throw new IndexOutOfBoundsException();
		int length = end - start;
		if (length > 0) {
			char[] v = m_value;
			System.arraycopy(v, start + length, v, start, count - end);
			m_length = count - length;
		}
		return this;
	}

	/**
	 * Appends the string representation of the {@code codePoint} argument to
	 * this sequence.
	 * 
	 * <p>
	 * The argument is appended to the contents of this sequence. The length of
	 * this sequence increases by {@code Character.charCount(codePoint)}.
	 * 
	 * <p>
	 * The overall effect is exactly as if the argument were converted to a
	 * {@code char} array by the method {@code Character.toChars(int)} and the
	 * character in that array were then {@link #append(char[]) appended} to
	 * this character sequence.
	 * 
	 * @param codePoint
	 *            a Unicode code point
	 * @return this object.
	 * @throws IllegalArgumentException
	 *             if the specified {@code codePoint} isn't a valid Unicode code
	 *             point
	 */
	public StringBuilder appendCodePoint(int codePoint) {
		if (!Character.isValidCodePoint(codePoint))
			throw new IllegalArgumentException();
		int n = 1;
		if (codePoint >= Character.MIN_SUPPLEMENTARY_CODE_POINT)
			++n;
		int newLength = m_length + n;
		if (newLength > m_value.length)
			expandCapacity(newLength);

		newLength = m_length;
		if (n == 1)
			m_value[newLength] = (char) codePoint;
		else {
			int offset = codePoint - Character.MIN_SUPPLEMENTARY_CODE_POINT;
			m_value[newLength] = (char) ((offset >>> 10) + Character.MIN_HIGH_SURROGATE);
			m_value[++newLength] = (char) ((offset & 0x3FF) + Character.MIN_LOW_SURROGATE);
		}

		m_length = ++newLength;

		return this;
	}

	/**
	 * Removes the character at the specified position in this sequence.
	 * 
	 * @param index
	 *            the index of the character to remove
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the {@code index} is negative or greater than or equal to
	 *             {@code length()}
	 */
	public StringBuilder deleteCharAt(int index) {
		int count = m_length;
		if (index >= count)
			throw new IndexOutOfBoundsException();
		--count;
		System.arraycopy(m_value, index + 1, m_value, index, count - index);
		m_length = count;
		return this;
	}

	/**
	 * Replaces the characters in a substring of this sequence with characters
	 * in the specified {@code String}. The substring begins at the specified
	 * {@code start} and extends to the character at index {@code end - 1} or to
	 * the end of the sequence if no such character exists. First the characters
	 * in the substring are removed and then the specified {@code String} is
	 * inserted at {@code start}. (This sequence will be lengthened to
	 * accommodate the specified string if necessary.)
	 * 
	 * @param start
	 *            the beginning index, inclusive
	 * @param end
	 *            the ending index, exclusive
	 * @param str
	 *            the string that will replace previous contents
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative, greater than {@code length()},
	 *             or greater than {@code end}
	 */
	public StringBuilder replace(int start, int end, String str) {
		int newLength = m_length;
		if (start > newLength || start > end)
			throw new IndexOutOfBoundsException();

		if (end > newLength)
			end = newLength;
		int length = str.length();
		newLength += length - (end - start);
		if (newLength > m_value.length)
			expandCapacity(newLength);

		char[] v = m_value;
		System.arraycopy(v, end, v, start + length, m_length - end);
		str.getChars(0, length, v, start);
		m_length = newLength;
		return this;
	}

	/**
	 * Returns a new {@code String} that contains a subsequence of characters
	 * currently contained in this character sequence. The substring begins at
	 * the specified index and extends to the end of this sequence.
	 * 
	 * @param start
	 *            the beginning index, inclusive
	 * @return the new string
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is less than zero, or greater than the
	 *             length of this object
	 */
	public String substring(int start) {
		return substring(start, m_length);
	}

	/**
	 * Returns a new character sequence that is a subsequence of this sequence.
	 * 
	 * <p>
	 * An invocation of this method of the form
	 * 
	 * <pre>
	 * {@code subSequence(begin, end)}
	 * </pre>
	 * 
	 * behaves in exactly the same way as the invocation
	 * 
	 * <pre>
	 * {@code substring(begin, end)}
	 * </pre>
	 * 
	 * This method is provided so that this class can implement the
	 * {@code CharSequence} interface.
	 * 
	 * @param start
	 *            the start index, inclusive
	 * @param end
	 *            the end index, exclusive
	 * @return the specified subsequence
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if <tt>start</tt> or <tt>end</tt> are negative, if
	 *             <tt>end</tt> is greater than <tt>length()</tt>, or if
	 *             <tt>start</tt> is greater than <tt>end</tt>
	 */
	@Override
	public CharSequence subSequence(int start, int end) {
		return substring(start, end);
	}

	/**
	 * Returns a new {@code String} that contains a subsequence of characters
	 * currently contained in this sequence. The substring begins at the
	 * specified {@code start} and extends to the character at index {@code end
	 * - 1}.
	 * 
	 * @param start
	 *            the beginning index, inclusive
	 * @param end
	 *            the ending index, exclusive
	 * @return the new string
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} or {@code end} are negative or greater than
	 *             {@code length()}, or {@code start} is greater than
	 *             {@code end}
	 */
	public String substring(int start, int end) {
		if (end > m_length | start > end)
			throw new IndexOutOfBoundsException();

		return new String(m_value, start, end - start);
	}

	/**
	 * Inserts the string representation of a subarray of the {@code chars}
	 * array argument into this sequence. The subarray begins at the specified
	 * {@code offset} and extends {@code len} {@code char}i. The characters of
	 * the subarray are inserted into this sequence at the position indicated by
	 * {@code index}. The length of this sequence increases by {@code len}
	 * {@code char}i.
	 * 
	 * @param index
	 *            position at which to insert subarray
	 * @param chars
	 *            a {@code char} array
	 * @param offset
	 *            the index of the first {@code char} in subarray to be inserted
	 * @param len
	 *            the number of {@code char}s in the subarray to be inserted
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or greater than {@code length()}
	 *             , or {@code offset} or {@code len} are negative, or
	 *             {@code (offset+len)} is greater than {@code chars.length}
	 */
	public StringBuilder insert(int index, char[] chars, int offset, int len) {
		if (index > m_length || len < 0)
			throw new IndexOutOfBoundsException();

		int newLength = m_length + len;
		if (newLength > m_value.length)
			expandCapacityForInsertion(index, len, newLength);
		else if (index < m_length)
			System.arraycopy(m_value, index, m_value, index + len, m_length - offset);
		System.arraycopy(chars, offset, m_value, index, len);
		m_length = newLength;
		return this;
	}

	/**
	 * Inserts the string representation of the {@code Object} argument into
	 * this character sequence.
	 * <p>
	 * The second argument is converted to a string as if by the method
	 * {@code String.valueOf}, and the characters of that string are then
	 * inserted into this sequence at the indicated offset.
	 * <p>
	 * The offset argument must be greater than or equal to {@code 0}, and less
	 * than or equal to the length of this sequence.
	 * 
	 * @param offset
	 *            the offset
	 * @param obj
	 *            an {@code Object}
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the offset is invalid
	 */
	public StringBuilder insert(int offset, Object obj) {
		return insert(offset, String.valueOf(obj));
	}

	/**
	 * Inserts the string into this character sequence.
	 * <p>
	 * The characters of the {@code String} argument are inserted, in order,
	 * into this sequence at the indicated offset, moving up any characters
	 * originally above that position and increasing the length of this sequence
	 * by the length of the argument. If {@code str} is {@code null}, then the
	 * four characters {@code "null"} are inserted into this sequence.
	 * <p>
	 * The character at index <i>k</i> in the new character sequence is equal
	 * to:
	 * <ul>
	 * <li>the character at index <i>k</i> in the old character sequence, if
	 * <i>k</i> is less than {@code offset}
	 * <li>the character at index <i>k</i>{@code -offset} in the argument
	 * {@code str}, if <i>k</i> is not less than {@code offset} but is less than
	 * {@code offset+str.length()}
	 * <li>the character at index <i>k</i>{@code -str.length()} in the old
	 * character sequence, if <i>k</i> is not less than
	 * {@code offset+str.length()}
	 * </ul>
	 * <p>
	 * The offset argument must be greater than or equal to {@code 0}, and less
	 * than or equal to the length of this sequence.
	 * 
	 * @param offset
	 *            the offset
	 * @param str
	 *            a string
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the offset is invalid
	 */
	public StringBuilder insert(int offset, String str) {
		if (offset > m_length)
			throw new IndexOutOfBoundsException();

		if (str == null)
			str = "null";
		int len = str.length();
		int newLength = m_length + len;
		if (newLength > m_value.length)
			expandCapacityForInsertion(offset, len, newLength);
		else if (offset < m_length)
			System.arraycopy(m_value, offset, m_value, offset + len, m_length - offset);
		str.getChars(0, len, m_value, offset);
		m_length = newLength;
		return this;
	}

	/**
	 * Inserts the string representation of the {@code char} array argument into
	 * this sequence.
	 * <p>
	 * The characters of the array argument are inserted into the contents of
	 * this sequence at the position indicated by {@code offset}. The length of
	 * this sequence increases by the length of the argument.
	 * <p>
	 * The overall effect is exactly as if the argument were converted to a
	 * string by the method {@code String.valueOf(char[])} and the characters of
	 * that string were then {@link #insert(int,String) inserted} into this
	 * character sequence at the position indicated by {@code offset}.
	 * 
	 * @param offset
	 *            the offset
	 * @param chars
	 *            a character array
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the offset is invalid
	 */
	public StringBuilder insert(int offset, char[] chars) {
		if (offset > m_length)
			throw new IndexOutOfBoundsException();

		int len = chars.length;
		int newLength = m_length + len;
		if (newLength > m_value.length)
			expandCapacityForInsertion(offset, len, newLength);
		else if (offset < m_length)
			System.arraycopy(m_value, offset, m_value, offset + len, m_length - offset);
		System.arraycopy(chars, 0, m_value, offset, len);
		m_length = newLength;
		return this;
	}

	/**
	 * Inserts the specified {@code CharSequence} into this sequence.
	 * <p>
	 * The characters of the {@code CharSequence} argument are inserted, in
	 * order, into this sequence at the indicated offset, moving up any
	 * characters originally above that position and increasing the length of
	 * this sequence by the length of the argument csq.
	 * <p>
	 * The result of this method is exactly the same as if it were an invocation
	 * of this object'csq insert(dstOffset, csq, 0, csq.length()) method.
	 * 
	 * <p>
	 * If {@code csq} is {@code null}, then the four characters {@code "null"}
	 * are inserted into this sequence.
	 * 
	 * @param dstOffset
	 *            the offset
	 * @param csq
	 *            the sequence to be inserted
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the offset is invalid
	 */
	public StringBuilder insert(int dstOffset, CharSequence csq) {
		if (csq == null)
			return insert(dstOffset, "null");

		return insert(dstOffset, csq, 0, csq.length());
	}

	/**
	 * Inserts a subsequence of the specified {@code CharSequence} into this
	 * sequence.
	 * <p>
	 * The subsequence of the argument {@code csq} specified by {@code start}
	 * and {@code end} are inserted, in order, into this sequence at the
	 * specified destination offset, moving up any characters originally above
	 * that position. The length of this sequence is increased by {@code end -
	 * start}.
	 * <p>
	 * The character at index <i>k</i> in this sequence becomes equal to:
	 * <ul>
	 * <li>the character at index <i>k</i> in this sequence, if <i>k</i> is less
	 * than {@code dstOffset}
	 * <li>the character at index <i>k</i>{@code +start-dstOffset} in the
	 * argument {@code csq}, if <i>k</i> is greater than or equal to
	 * {@code dstOffset} but is less than {@code dstOffset+end-start}
	 * <li>the character at index <i>k</i>{@code -(end-start)} in this sequence,
	 * if <i>k</i> is greater than or equal to {@code dstOffset+end-start}
	 * </ul>
	 * <p>
	 * The dstOffset argument must be greater than or equal to {@code 0}, and
	 * less than or equal to the length of this sequence.
	 * <p>
	 * The start argument must be nonnegative, and not greater than {@code end}.
	 * <p>
	 * The end argument must be greater than or equal to {@code start}, and less
	 * than or equal to the length of csq.
	 * 
	 * <p>
	 * If {@code csq} is {@code null}, then this method inserts characters as if
	 * the csq parameter was a sequence containing the four characters
	 * {@code "null"}.
	 * 
	 * @param dstOffset
	 *            the offset in this sequence
	 * @param csq
	 *            the sequence to be inserted
	 * @param start
	 *            the starting index of the subsequence to be inserted
	 * @param end
	 *            the end index of the subsequence to be inserted
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if {@code dstOffset} is negative or greater than
	 *             {@code this.length()}, or {@code start} or {@code end} are
	 *             negative, or {@code start} is greater than {@code end} or
	 *             {@code end} is greater than {@code csq.length()}
	 */
	public StringBuilder insert(int dstOffset, CharSequence csq, int start, int end) {
		if (csq == null)
			return insert(dstOffset, "null");

		int newLength = m_length;
		if (dstOffset > newLength || start > end)
			throw new IndexOutOfBoundsException();

		int len = end - start;
		if (len == 0)
			return this;
		newLength += len;
		if (newLength > m_value.length)
			expandCapacityForInsertion(dstOffset, len, newLength);
		else if (dstOffset < m_length)
			System.arraycopy(m_value, dstOffset, m_value, dstOffset + len, m_length - dstOffset);

		char[] v = m_value;
		for (int i = start; i < end; ++i, ++dstOffset)
			v[dstOffset] = csq.charAt(i);
		m_length = newLength;
		return this;
	}

	/**
	 * Inserts the string representation of the {@code boolean} argument into
	 * this sequence.
	 * <p>
	 * The second argument is converted to a string as if by the method
	 * {@code String.valueOf}, and the characters of that string are then
	 * inserted into this sequence at the indicated offset.
	 * <p>
	 * The offset argument must be greater than or equal to {@code 0}, and less
	 * than or equal to the length of this sequence.
	 * 
	 * @param offset
	 *            the offset
	 * @param b
	 *            a {@code boolean}
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the offset is invalid
	 */
	public StringBuilder insert(int offset, boolean b) {
		return insert(offset, String.valueOf(b));
	}

	/**
	 * Inserts the string representation of the {@code char} argument into this
	 * sequence.
	 * <p>
	 * The second argument is inserted into the contents of this sequence at the
	 * position indicated by {@code offset}. The length of this sequence
	 * increases by one.
	 * <p>
	 * The overall effect is exactly as if the argument were converted to a
	 * string by the method {@code String.valueOf(char)} and the character in
	 * that string were then {@link #insert(int, String) inserted} into this
	 * character sequence at the position indicated by {@code offset}.
	 * <p>
	 * The offset argument must be greater than or equal to {@code 0}, and less
	 * than or equal to the length of this sequence.
	 * 
	 * @param offset
	 *            the offset
	 * @param c
	 *            a {@code char}
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the offset is invalid
	 */
	public StringBuilder insert(int offset, char c) {
		int newLength = m_length + 1;
		if (newLength > m_value.length)
			expandCapacityForInsertion(offset, 1, newLength);
		else if (offset < m_length)
			System.arraycopy(m_value, offset, m_value, offset + 1, m_length - offset);

		m_value[offset] = c;
		m_length = newLength;
		return this;
	}

	/**
	 * Inserts the string representation of the second {@code int} argument into
	 * this sequence.
	 * <p>
	 * The second argument is converted to a string as if by the method
	 * {@code String.valueOf}, and the characters of that string are then
	 * inserted into this sequence at the indicated offset.
	 * <p>
	 * The offset argument must be greater than or equal to {@code 0}, and less
	 * than or equal to the length of this sequence.
	 * 
	 * @param offset
	 *            the offset
	 * @param i
	 *            an {@code int}
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the offset is invalid
	 */
	public StringBuilder insert(int offset, int i) {
		int newLength = m_length;
		if (offset > newLength)
			throw new IndexOutOfBoundsException();

		if (i == Integer.MIN_VALUE)
			return insert(offset, "-2147483648");

		int len = i < 0 ? StrUtil.stringSizeOfInt(-i) + 1 : StrUtil.stringSizeOfInt(i);
		newLength += len;
		if (newLength > m_value.length)
			expandCapacityForInsertion(offset, len, newLength);
		else if (offset < m_length)
			System.arraycopy(m_value, offset, m_value, offset + len, m_length - offset);

		StrUtil.getChars(i, len + offset, m_value);
		m_length = newLength;
		return this;
	}

	/**
	 * Inserts the string representation of the {@code long} argument into this
	 * sequence.
	 * <p>
	 * The second argument is converted to a string as if by the method
	 * {@code String.valueOf}, and the characters of that string are then
	 * inserted into this sequence at the position indicated by {@code offset}.
	 * <p>
	 * The offset argument must be greater than or equal to {@code 0}, and less
	 * than or equal to the length of this sequence.
	 * 
	 * @param offset
	 *            the offset
	 * @param l
	 *            a {@code long}
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the offset is invalid
	 */
	public StringBuilder insert(int offset, long l) {
		int newLength = m_length;
		if (offset > newLength)
			throw new IndexOutOfBoundsException();

		if (l == Long.MIN_VALUE)
			return insert(offset, "-9223372036854775808");

		int len = (l < 0L) ? StrUtil.stringSizeOfLong(-l) + 1 : StrUtil.stringSizeOfLong(l);
		newLength += len;
		if (newLength > m_value.length)
			expandCapacityForInsertion(offset, len, newLength);
		else if (offset < m_length)
			System.arraycopy(m_value, offset, m_value, offset + len, m_length - offset);

		StrUtil.getChars(l, offset + len, m_value);
		m_length = newLength;
		return this;
	}

	/**
	 * Inserts the string representation of the {@code float} argument into this
	 * sequence.
	 * <p>
	 * The second argument is converted to a string as if by the method
	 * {@code String.valueOf}, and the characters of that string are then
	 * inserted into this sequence at the indicated offset.
	 * <p>
	 * The offset argument must be greater than or equal to {@code 0}, and less
	 * than or equal to the length of this sequence.
	 * 
	 * @param offset
	 *            the offset
	 * @param f
	 *            a {@code float}
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the offset is invalid
	 */
	public StringBuilder insert(int offset, float f) {
		return insert(offset, Float.toString(f));
	}

	/**
	 * Inserts the string representation of the {@code double} argument into
	 * this sequence.
	 * 
	 * <p>
	 * The second argument is converted to a string as if by the method
	 * {@code String.valueOf}, and the characters of that string are then
	 * inserted into this sequence at the indicated offset.
	 * 
	 * <p>
	 * The offset argument must be greater than or equal to {@code 0}, and less
	 * than or equal to the length of this sequence.
	 * 
	 * @param offset
	 *            the offset
	 * @param d
	 *            a {@code double}
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the offset is invalid
	 */
	public StringBuilder insert(int offset, double d) {
		return insert(offset, Double.toString(d));
	}

	/**
	 * Inserts the string representation of the {@code codePoint} argument to
	 * this sequence.
	 * 
	 * <p>
	 * The argument is inserted to the contents of this sequence at
	 * {@code offset}. The length of this sequence increases by
	 * {@code Character.charCount(codePoint)}.
	 * 
	 * <p>
	 * The overall effect is exactly as if the argument were converted to a
	 * {@code char} array by the method {@code Character.toChars(int)} and the
	 * character in that array were then {@link #append(char[]) inserted} to
	 * this character sequence at the given {@code offset}.
	 * 
	 * @param offset
	 *            the offset
	 * @param codePoint
	 *            a Unicode code point
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the offset is invalid
	 * @throws IllegalArgumentException
	 *             if the specified {@code codePoint} isn't a valid Unicode code
	 *             point
	 */
	public StringBuilder insertCodePoint(int offset, int codePoint) {
		int newLength = m_length;
		if (offset > newLength)
			throw new IndexOutOfBoundsException();

		if (!Character.isValidCodePoint(codePoint))
			throw new IllegalArgumentException();

		int n = 1;
		if (codePoint >= Character.MIN_SUPPLEMENTARY_CODE_POINT)
			++n;
		newLength += n;
		if (newLength > m_value.length)
			expandCapacityForInsertion(offset, n, newLength);
		else if (offset < m_length)
			System.arraycopy(m_value, offset, m_value, offset + n, m_length - offset);

		if (n == 1)
			m_value[offset] = (char) codePoint;
		else {
			int off = codePoint - Character.MIN_SUPPLEMENTARY_CODE_POINT;
			char[] v = m_value;
			v[offset] = (char) ((off >>> 10) + Character.MIN_HIGH_SURROGATE);
			v[++offset] = (char) ((off & 0x3FF) + Character.MIN_LOW_SURROGATE);
		}

		m_length = newLength;

		return this;
	}

	/**
	 * Inserts the {@code count} character {@code c} to this
	 * {@code StringBuilder} at {@code offset}.
	 * 
	 * @param offset
	 *            the offset
	 * @param c
	 *            the character to insert
	 * @param count
	 *            the number of characters to insert
	 * @return this object
	 */
	public StringBuilder insertFill(int offset, char c, int count) {
		int n = m_length;
		if (offset > n)
			throw new IndexOutOfBoundsException();

		if (count < 0)
			throw new IllegalArgumentException();

		int newLength = n + count;
		if (newLength > m_value.length)
			expandCapacity(newLength);

		char[] v = m_value;
		count += offset;
		System.arraycopy(v, offset, v, count, n - offset);

		for (; offset < count; ++offset)
			v[offset] = c;

		m_length = newLength;
		return this;
	}

	/**
	 * Inserts the given byte {@code b} to this string builder at the given
	 * {@code index} as 2 hex characters.
	 * 
	 * @param index
	 *            the offset at which to be inserted
	 * @param b
	 *            the {@code byte} to be interpreted and inserted
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the index is invalid
	 */
	public StringBuilder insertHex(int index, byte b) {
		int newLength = m_length;
		if (index > newLength)
			throw new IndexOutOfBoundsException();

		newLength += 2;
		if (newLength > m_value.length)
			expandCapacityForInsertion(index, 2, newLength);
		else if (index < m_length)
			System.arraycopy(m_value, index, m_value, index + 2, m_length - index);

		char[] v = m_value;
		int c = b & 0xFF;
		v[index] = c_bhDigits[c];
		v[++index] = c_blDigits[c];

		m_length = newLength;

		return this;
	}

	/**
	 * Inserts the given short {@code s} to this string builder at the given
	 * {@code index} as 4 hex characters.
	 * 
	 * @param index
	 *            the offset at which to be inserted
	 * @param s
	 *            the {@code short} to be interpreted and inserted
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the index is invalid
	 */
	public StringBuilder insertHex(int index, short s) {
		int newLength = m_length;
		if (index > newLength)
			throw new IndexOutOfBoundsException();

		newLength += 4;
		if (newLength > m_value.length)
			expandCapacityForInsertion(index, 4, newLength);
		else if (index < m_length)
			System.arraycopy(m_value, index, m_value, index + 4, m_length - index);

		char[] v = m_value;
		int b = (s >> 8) & 0xFF;
		v[index] = c_bhDigits[b];
		v[++index] = c_blDigits[b];

		b = s & 0xFF;
		v[++index] = c_bhDigits[b];
		v[++index] = c_blDigits[b];

		m_length = newLength;

		return this;
	}

	/**
	 * Inserts the given int {@code i} to this string builder at the given
	 * {@code index} as 8 hex characters.
	 * 
	 * @param index
	 *            the offset at which to be inserted
	 * @param i
	 *            the {@code int} to be interpreted and inserted
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the index is invalid
	 */
	public StringBuilder insertHex(int index, int i) {
		int newLength = m_length;
		if (index > newLength)
			throw new IndexOutOfBoundsException();

		newLength += 8;
		if (newLength > m_value.length)
			expandCapacityForInsertion(index, 8, newLength);
		else if (index < m_length)
			System.arraycopy(m_value, index, m_value, index + 8, m_length - index);

		char[] v = m_value;
		for (int bits = 24; bits >= 0; bits -= 8, ++index) {
			int b = (i >> bits) & 0xFF;
			v[index] = c_bhDigits[b];
			v[++index] = c_blDigits[b];
		}

		m_length = newLength;

		return this;
	}

	/**
	 * Inserts the given long {@code l} to this string builder at the given
	 * {@code index} as 16 hex characters.
	 * 
	 * @param index
	 *            the offset at which to be inserted
	 * @param l
	 *            the {@code long} to be interpreted and inserted
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the index is invalid
	 */
	public StringBuilder insertHex(int index, long l) {
		int newLength = m_length;
		if (index > newLength)
			throw new IndexOutOfBoundsException();

		newLength += 16;
		if (newLength > m_value.length)
			expandCapacityForInsertion(index, 16, newLength);
		else if (index < m_length)
			System.arraycopy(m_value, index, m_value, index + 16, m_length - index);

		char[] v = m_value;
		int offset = index + 8;
		int i1 = (int) (l >>> 32);
		int i2 = (int) l;
		for (int bits = 24; bits >= 0; bits -= 8, ++index, ++offset) {
			int b = (i1 >> bits) & 0xFF;
			v[index] = c_bhDigits[b];
			v[++index] = c_blDigits[b];

			b = (i2 >> bits) & 0xFF;
			v[offset] = c_bhDigits[b];
			v[++offset] = c_blDigits[b];
		}

		m_length = newLength;

		return this;
	}

	/**
	 * Inserts the given byte array {@code b} into this string builder at the
	 * given {@code index} as a hex string.
	 * 
	 * @param index
	 *            the offset at which to insert
	 * @param b
	 *            the byte array to be interpreted and inserted
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the index is invalid
	 */
	public StringBuilder insertHex(int index, byte[] b) {
		return insertHex(index, b, 0, b.length);
	}

	/**
	 * Inserts {@code length} bytes of the given byte array {@code b} starting
	 * at the given {@code offset} into this string builder at the given
	 * {@code index} as a hex string.
	 * 
	 * @param index
	 *            the offset at which to insert
	 * @param b
	 *            the byte array to be interpreted and inserted
	 * @param offset
	 *            the index of the first byte in the byte array {@code b} to be
	 *            inserted
	 * @param length
	 *            the number of bytes in byte array {@code b} to be inserted
	 * @return this object
	 * @throws IndexOutOfBoundsException
	 *             if the index is invalid, or the preconditions on the given
	 *             {@code offset} and {@code length} do not hold
	 */
	public StringBuilder insertHex(int index, byte[] b, int offset, int length) {
		int newLength = m_length;
		if (index > newLength || length < 0)
			throw new IndexOutOfBoundsException();

		length <<= 1;
		newLength += length;
		if (newLength < m_value.length)
			expandCapacityForInsertion(index, length, newLength);
		else if (index < m_length)
			System.arraycopy(m_value, index, m_value, index + length, m_length - index);

		for (final char[] v = m_value; index < newLength; ++offset) {
			int c = b[offset] & 0xFF;
			v[index++] = c_bhDigits[c];
			v[index++] = c_blDigits[c];
		}

		m_length = newLength;

		return this;
	}

	/**
	 * Returns the index within this string of the first occurrence of the
	 * specified substring.
	 * 
	 * @param str
	 *            any string
	 * @return if the string argument occurs as a substring within this object,
	 *         then the index of the first character of the first such substring
	 *         is returned; if it does not occur as a substring, {@code -1} is
	 *         returned
	 * @throws NullPointerException
	 *             if {@code str} is {@code null}
	 */
	public int indexOf(String str) {
		int n = str.length();
		if (n < 1)
			return 0;

		try (StringBuilder builder = get(str)) {
			char[] strV = builder.m_value;
			char first = strV[0];

			char[] v = m_value;
			int fromIndex = 0;

			next: for (int endIndex = m_length - n; fromIndex <= endIndex; ++fromIndex) {
				if (v[fromIndex] != first)
					continue;

				for (int i = 1; i < n; ++i) {
					if (v[fromIndex + i] != strV[i])
						continue next;
				}

				return fromIndex;
			}

			return -1;
		}
	}

	/**
	 * Returns the index within this sequence of the first occurrence of the
	 * specified subsequence, starting at the specified index. If no such
	 * substring exists, then {@code -1} is returned.
	 * 
	 * @param str
	 *            the substring for which to search
	 * @param fromIndex
	 *            the index from which to start the search
	 * @return the index within this string of the first occurrence of the
	 *         specified subsequence, starting at the specified index
	 * @throws NullPointerException
	 *             if {@code str} is {@code null}
	 */
	public int indexOf(String str, int fromIndex) {
		int n = str.length();
		int endIndex = m_length - n;
		if (fromIndex > endIndex)
			return n < 0 ? endIndex : -1;

		if (fromIndex < 0)
			fromIndex = 0;

		if (n < 0)
			return fromIndex;

		try (StringBuilder builder = get(str)) {
			char[] strV = builder.m_value;
			char[] v = m_value;
			next: for (char first = strV[0]; fromIndex <= endIndex; ++fromIndex) {
				if (v[fromIndex] != first)
					continue;

				for (int i = 1; i < n; ++i) {
					if (v[fromIndex + i] != strV[i])
						continue next;
				}

				return fromIndex;
			}

			return -1;
		}
	}

	/**
	 * Returns the index within this char sequence of the first occurrence of
	 * the specified char sequence {@code chars}. If no such subsequence exists,
	 * then {@code -1} is returned.
	 * 
	 * @param chars
	 *            the subsequence for which to search
	 * @return if the char sequence argument occurs as a subsequence within this
	 *         object, then the index of the first character of the first such
	 *         subsequence is returned; if it does not occur as a subsequence,
	 *         {@code -1} is returned
	 * @throws NullPointerException
	 *             if {@code chars} is {@code null}
	 */
	public int indexOf(char[] chars) {
		int n = chars.length;
		if (n < 1)
			return 0;

		char first = chars[0];

		char[] v = m_value;
		int fromIndex = 0;

		next: for (int endIndex = m_length - n; fromIndex <= endIndex; ++fromIndex) {
			if (v[fromIndex] != first)
				continue;

			for (int i = 1; i < n; ++i) {
				if (v[fromIndex + i] != chars[i])
					continue next;
			}

			return fromIndex;
		}

		return -1;
	}

	/**
	 * Returns the index within this char sequence of the first occurrence of
	 * the specified subsequence, starting at the specified index. If no such
	 * subsequence exists, then {@code -1} is returned.
	 * 
	 * @param chars
	 *            the subsequence for which to search
	 * @param fromIndex
	 *            the index from which to start the search
	 * @return the index within this char sequence of the first occurrence of
	 *         the specified subsequence, starting at the specified index
	 * @throws NullPointerException
	 *             if {@code chars} is {@code null}
	 */
	public int indexOf(char[] chars, int fromIndex) {
		int n = chars.length;
		int endIndex = m_length - n;
		if (fromIndex > endIndex)
			return n < 0 ? endIndex : -1;

		if (fromIndex < 0)
			fromIndex = 0;

		if (n < 0)
			return fromIndex;

		char[] v = m_value;
		next: for (char first = chars[0]; fromIndex <= endIndex; ++fromIndex) {
			if (v[fromIndex] != first)
				continue;

			for (int i = 1; i < n; ++i) {
				if (v[fromIndex + i] != chars[i])
					continue next;
			}

			return fromIndex;
		}

		return -1;
	}

	/**
	 * Returns the index within this sequence of the first occurrence of the
	 * specified {@code pattern} by searching with the KMP algorithm. If no such
	 * subsequence exists, then {@code -1} is returned.
	 * 
	 * @param pattern
	 *            the KMP pattern holding the subsequence for which to search
	 * @return if the given {@code pattern} occurs as a subsequence with this
	 *         object, then the index of the first character of the first such
	 *         subsequence is returned; if it does not occur as a subsequence,
	 *         {@code - 1} is returned
	 * @throws NullPointerException
	 *             if {@code pattern} is {@code null}
	 */
	public int indexOf(CharKmp pattern) {
		return pattern.findIn(m_value, 0, m_value.length);
	}

	/**
	 * Returns the index within this sequence of the first occurrence of the
	 * specified {@code pattern}, starting at the specified {@code fromIndex}.
	 * If no such substring exists by searching with KMP algorithm, then
	 * {@code -1} is returned.
	 * 
	 * @param pattern
	 *            the KMP pattern holding the subsequence for which to search
	 * @param fromIndex
	 *            the index from which to start the search
	 * @return if the given {@code pattern} occurs as a subsequence with this
	 *         object, then the index of the first character of the first such
	 *         subsequence is returned; if it does not occur as a subsequence,
	 *         {@code - 1} is returned
	 * @throws NullPointerException
	 *             if {@code pattern} is {@code null}
	 */
	public int indexOf(CharKmp pattern, int fromIndex) {
		return pattern.findIn(m_value, fromIndex, m_value.length - fromIndex);
	}

	/**
	 * Returns the index within this sequence of the rightmost occurrence of the
	 * specified substring. The rightmost empty string "" is considered to occur
	 * at the index value {@code this.length()}. The returned index is the
	 * largest value <i>k</i> such that
	 * 
	 * <pre>
	 * {@code toString().startsWith(str, k)}
	 * </pre>
	 * 
	 * is true.
	 * 
	 * @param str
	 *            the substring to search for.
	 * @return if the string argument occurs one or more times as a substring
	 *         within this object, then the index of the first character of the
	 *         last such substring is returned. If it does not occur as a
	 *         substring, {@code -1} is returned.
	 * @throws NullPointerException
	 *             if {@code str} is {@code null}.
	 */
	public int lastIndexOf(String str) {
		int n = str.length();
		int fromIndex = m_length - n;
		if (n < 1)
			return fromIndex;

		try (StringBuilder builder = get()) {
			builder.append(str);
			char[] strV = builder.m_value;
			char[] v = m_value;

			next: for (char first = strV[0]; fromIndex >= 0; --fromIndex) {
				if (v[fromIndex] != first)
					continue;

				for (int i = 1; i < n; ++i) {
					if (v[fromIndex + i] != strV[i])
						continue next;
				}

				return fromIndex;
			}

			return -1;
		}
	}

	/**
	 * Returns the index within this string of the last occurrence of the
	 * specified substring. The integer returned is the largest value <i>k</i>
	 * such that:
	 * 
	 * <pre>
	 * {@code k &lt;= Math.min(fromIndex, chars.length()) &amp;&amp; this.toString().startsWith(str, k)}
	 * </pre>
	 * 
	 * If no such value of <i>k</i> exists, then -1 is returned.
	 * 
	 * @param str
	 *            the substring to search for.
	 * @param fromIndex
	 *            the index to start the search from.
	 * @return the index within this sequence of the last occurrence of the
	 *         specified substring.
	 * @throws NullPointerException
	 *             if {@code str} is {@code null}.
	 */
	public int lastIndexOf(String str, int fromIndex) {
		int n = str.length();
		int maxIndex = m_length - n;
		if (fromIndex > maxIndex)
			fromIndex = maxIndex;

		if (fromIndex < 0)
			return -1;

		if (n < 1)
			return fromIndex;

		try (StringBuilder builder = get()) {
			builder.append(str);
			char[] strV = builder.m_value;
			char[] v = m_value;
			next: for (char first = strV[0]; fromIndex >= 0; --fromIndex) {
				if (v[fromIndex] != first)
					continue;

				for (int i = 1; i < n; ++i) {
					if (v[fromIndex + i] != strV[i])
						continue next;
				}

				return fromIndex;
			}

			return -1;
		}
	}

	/**
	 * Returns the index within this char sequence of the rightmost occurrence
	 * of the specified subsequence. The rightmost empty char array is
	 * considered to occur at the index value {@code this.length()}.
	 * 
	 * @param chars
	 *            the subsequence to search for.
	 * @return if the char array argument occurs one or more times as a
	 *         subsequence within this object, then the index of the first
	 *         character of the last such subsequence is returned. If it does
	 *         not occur as a subsequence, {@code -1} is returned.
	 * @throws NullPointerException
	 *             if {@code chars} is {@code null}.
	 */
	public int lastIndexOf(char[] chars) {
		int n = chars.length;
		int fromIndex = m_length - n;
		if (n < 1)
			return fromIndex;

		char[] v = m_value;

		next: for (char first = chars[0]; fromIndex >= 0; --fromIndex) {
			if (v[fromIndex] != first)
				continue;

			for (int i = 1; i < n; ++i) {
				if (v[fromIndex + i] != chars[i])
					continue next;
			}

			return fromIndex;
		}

		return -1;
	}

	/**
	 * Returns the index within this char sequence of the last occurrence of the
	 * specified subsequence. If no such subsequence exists, then {@code -1} is
	 * returned.
	 * 
	 * @param chars
	 *            the subsequence to search for
	 * @param fromIndex
	 *            the index to start the search from
	 * @return the index within this sequence of the last occurrence of the
	 *         specified subsequence
	 * @throws NullPointerException
	 *             if {@code str} is {@code null}
	 */
	public int lastIndexOf(char[] chars, int fromIndex) {
		int n = chars.length;
		int maxIndex = m_length - n;
		if (fromIndex > maxIndex)
			fromIndex = maxIndex;

		if (fromIndex < 0)
			return -1;

		if (n < 1)
			return fromIndex;

		char[] v = m_value;
		next: for (char first = chars[0]; fromIndex >= 0; --fromIndex) {
			if (v[fromIndex] != first)
				continue;

			for (int i = 1; i < n; ++i) {
				if (v[fromIndex + i] != chars[i])
					continue next;
			}

			return fromIndex;
		}

		return -1;
	}

	/**
	 * Returns the index within this char sequence of the rightmost occurrence
	 * of the specified subsequence by searching with the KMP algorithm. The
	 * rightmost empty char array is considered to occur at the index value
	 * {@code this.length()}.
	 * 
	 * @param pattern
	 *            the subsequence to search for
	 * @return If the given {@code pattern} occurs one or more times as a
	 *         subsequence within this object, then the index of the first
	 *         character of the last such subsequence is returned. If it does
	 *         not occur as a subsequence, {@code -1} is returned.
	 * @throws NullPointerException
	 *             if {@code pattern} is {@code null}
	 */
	public int lastIndexOf(CharKmp pattern) {
		return pattern.rfindIn(m_value, 0, m_value.length);
	}

	/**
	 * Returns the index within this char sequence starting from the specified
	 * {@code fromIndex} of the rightmost occurrence of the specified
	 * subsequence by searching with the KMP algorithm. If no such subsequence
	 * exists, then {@code -1} is returned.
	 * 
	 * @param pattern
	 *            the subsequence to search for
	 * @param fromIndex
	 *            the index to start the search from
	 * @return the index within this sequence of the last occurrence of the
	 *         specified subsequence
	 * @throws NullPointerException
	 *             if {@code pattern} is {@code null}
	 */
	public int lastIndexOf(CharKmp pattern, int fromIndex) {
		return pattern.rfindIn(m_value, 0, fromIndex + pattern.length());
	}

	/**
	 * Replaces this character sequence with the reverse of the sequence. If
	 * there are any surrogate pairs included in the sequence, these are treated
	 * as single characters for the reverse operation. Thus, the order of the
	 * high-low surrogates is never reversed.
	 * 
	 * Let <i>n</i> be the character length of this character sequence (not the
	 * length in {@code char} values) just prior to execution of the
	 * {@code reverse} method. Then the character at index <i>k</i> in the new
	 * character sequence is equal to the character at index <i>n-k-1</i> in the
	 * old character sequence.
	 * 
	 * <p>
	 * Note that the reverse operation may result in producing surrogate pairs
	 * that were unpaired low-surrogates and high-surrogates before the
	 * operation. For example, reversing "&#92;uDC00&#92;uD800" produces
	 * "&#92;uD800&#92;uDC00" which is a valid surrogate pair.
	 * 
	 * @return this object.
	 */
	public StringBuilder reverse() {
		boolean hasSurrogate = false;
		char[] v = m_value;
		int n = m_length - 1;
		for (int j = (n - 1) >> 1; j >= 0; --j) {
			char c1 = v[j];
			char c2 = v[n - j];
			if (!hasSurrogate) {
				hasSurrogate = (c1 >= Character.MIN_SURROGATE && c1 <= Character.MAX_SURROGATE)
						|| (c2 >= Character.MIN_SURROGATE && c2 <= Character.MAX_SURROGATE);
			}
			v[j] = c2;
			v[n - j] = c1;
		}
		if (hasSurrogate) {
			// Reverse back all valid surrogate pairs
			for (int i = 0; i < n; ++i) {
				char c2 = v[i];
				if (Character.isLowSurrogate(c2)) {
					char c1 = v[i + 1];
					if (Character.isHighSurrogate(c1)) {
						v[i] = c1;
						v[++i] = c2;
					}
				}
			}
		}
		return this;
	}

	/**
	 * Returns a string representing the data in this sequence. A new
	 * {@code String} object is allocated and initialized to contain the
	 * character sequence currently represented by this object. This
	 * {@code String} is then returned. Subsequent changes to this sequence do
	 * not affect the contents of the {@code String}.
	 * 
	 * @return a string representation of this sequence of characters.
	 */
	@Override
	public String toString() {
		return new String(m_value, 0, m_length);
	}

	/**
	 * Returns a string representing the data in this sequnce. A new
	 * {@code String} object is allocated and initialized to contain the
	 * character sequence currently represented by this object. This
	 * {@code String} is then returned. This {@code StringBuilder} is then
	 * closed. So the reference to this object must not be used anymore after
	 * this method returns unless it's got from the cache again.
	 * 
	 * @return a string representation of this sequence of characters.
	 */
	public String toStringAndClose() {
		String str = new String(m_value, 0, m_length);
		close();
		return str;
	}

	/**
	 * Returns a character array that is a copy of the data this
	 * {@code StringBuilder} currently holds.
	 * 
	 * @return a copy of the data this {@code StringBuilder} currently holds
	 */
	public char[] toCharArray() {
		int length = m_length;
		char[] copy = new char[length];
		System.arraycopy(m_value, 0, copy, 0, length);
		return copy;
	}

	/**
	 * Returns a {@code CharBuffer} backed by the char array in this
	 * {@code StringBuilder}. Its capacity will be {@link #capacity()}, its
	 * position will be {@code offset}, its limit will be
	 * {@code (offset + length)}, its mark will be undefined.
	 * 
	 * @param offset
	 *            the offset of the subarray of the backing char array
	 * @param length
	 *            the length of the subarray of the backing char array
	 * @return a {@code CharBuffer}
	 * @throws IndexOutOfBoundsException
	 *             if the preconditions on the offset and length parameters do
	 *             not hold
	 */
	public CharBuffer getCharBuffer(int offset, int length) {
		CharBuffer charBuffer = m_charBuffer;
		if (charBuffer == null) {
			charBuffer = CharBuffer.wrap(m_value, offset, length);
			m_charBuffer = charBuffer;
		} else {
			charBuffer.clear();
			charBuffer.position(offset);
			charBuffer.limit(offset + length);
		}

		return charBuffer;
	}

	StringBuilder deeplyAppend(Object arg, HashSet seen) {
		Class<?> clazz = arg.getClass();
		if (!clazz.isArray())
			append(arg);
		else if (clazz == int[].class)
			appendArray((int[]) arg);
		else if (clazz == byte[].class)
			appendArray((byte[]) arg);
		else if (clazz == long[].class)
			appendArray((long[]) arg);
		else if (clazz == short[].class)
			appendArray((short[]) arg);
		else if (clazz == float[].class)
			appendArray((float[]) arg);
		else if (clazz == double[].class)
			appendArray((double[]) arg);
		else if (clazz == char[].class)
			appendArray((char[]) arg);
		else if (clazz == String[].class)
			appendArray((String[]) arg);
		else
			appendArray((Object[]) arg, seen);

		return this;
	}

	StringBuilder appendArray(Object[] array, HashSet seen) {
		append('[');
		int n = array.length;
		if (n > 0) {
			if (seen.contains(array))
				append("...");
			else {
				seen.add(array);
				deeplyAppend(array[0], seen);
				for (int i = 1; i < n; ++i)
					append(", ").deeplyAppend(array[i], seen);
			}
		}
		append(']');

		return this;
	}

	void writeObject(ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
		oos.writeInt(m_length);
		oos.writeObject(m_value);
	}

	void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		m_length = ois.readInt();
		m_value = (char[]) ois.readObject();
	}

	void appendHexDump(IByteSequence[] data, int[] offsets, int[] lengths, int size) {
		int length = 0;
		int n = 0;
		for (; n < size; ++n)
			length += lengths[n];

		if (length < 1)
			return;

		n = (length + HM_BYTES_PERROW - 1) / HM_BYTES_PERROW;
		int m = (10 + HM_DISTANCE + LSL) * n + length + LSL;
		n = m_length;
		m += n;
		if (m > m_value.length)
			expandCapacity(m);

		char[] v = m_value;
		char[] bhDigits = c_bhDigits;
		char[] blDigits = c_blDigits;

		IByteSequence sequence = null;
		int offset = 0;
		int i = 0;
		while (i < size) {
			sequence = data[i];
			offset = offsets[i];
			length = lengths[i];
			++i;

			if (length > 0)
				break;
		}

		int addr = 0;
		do {
			// dump address
			int c = addr >>> 24;
			v[n] = bhDigits[c];
			v[++n] = blDigits[c];
			c = (addr >> 16) & 0xFF;
			v[++n] = bhDigits[c];
			v[++n] = blDigits[c];
			c = (addr >> 8) & 0xFF;
			v[++n] = bhDigits[c];
			v[++n] = blDigits[c];
			c = addr & 0xFF;
			v[++n] = bhDigits[c];
			v[++n] = blDigits[c];
			v[++n] = 'h';
			v[++n] = ':';

			// dump one row
			int asc = n + HM_DISTANCE;
			eof: for (m = 0; m < HM_BYTES_PERROW;) {
				v[++n] = ' ';
				c = sequence.byteAt(offset) & 0xFF;
				++offset;
				v[++n] = bhDigits[c];
				v[++n] = blDigits[c];

				v[++asc] = Character.isISOControl(c) ? '.' : (char) c;

				++m;
				--length;
				while (length < 1) {
					if (i >= size)
						break eof;
					sequence = data[i];
					offset = offsets[i];
					length = lengths[i];
					++i;
				}
			}

			do {
				v[++n] = ' ';
				v[++n] = ' ';
				v[++n] = ' ';
			} while (++m <= HM_BYTES_PERROW);

			System.arraycopy(c_ls, 0, v, ++asc, LSL);
			n = asc + LSL;

			addr += HM_BYTES_PERROW;

		} while (length > 0);

		m_length = n;
	}

	private void expandCapacity(int minCapacity) {
		int newCapacity = (m_value.length + 1) << 1;
		if (newCapacity < 0)
			newCapacity = Integer.MAX_VALUE;
		else if (minCapacity > newCapacity)
			newCapacity = minCapacity;

		char[] value = new char[newCapacity];
		System.arraycopy(m_value, 0, value, 0, m_length);
		m_value = value;
		m_charBuffer = null;
	}

	private void expandCapacityForInsertion(int index, int len, int minCapacity) {
		int newCapacity = (m_value.length + 1) << 1;
		if (newCapacity < 0)
			newCapacity = Integer.MAX_VALUE;
		else if (minCapacity > newCapacity)
			newCapacity = minCapacity;

		char[] value = new char[newCapacity];
		System.arraycopy(m_value, 0, value, 0, index);
		if (index < m_length)
			System.arraycopy(m_value, index, value, index + len, m_length - index);
		m_value = value;
		m_charBuffer = null;
	}

	private StringBuilder appendHexInternal(byte[] b, int offset, int length, boolean lowerCase) {
		int i = m_length;
		length = i + (length << 1);
		if (length > m_value.length)
			expandCapacity(length);

		final char[] bh;
		final char[] bl;
		if (lowerCase) {
			bh = c_bhDigitsLower;
			bl = c_blDigitsLower;
		} else {
			bh = c_bhDigits;
			bl = c_blDigits;
		}
		for (final char[] v = m_value; i < length; ++offset) {
			int c = b[offset] & 0xFF;
			v[i++] = bh[c];
			v[i++] = bl[c];
		}
		m_length = length;
		return this;
	}

	private StringBuilder appendHexDumpInternal(byte[] b, int offset, int length) {
		int n = (length + HM_BYTES_PERROW - 1) / HM_BYTES_PERROW;
		int m = (10 + HM_DISTANCE + LSL) * n + length + LSL;
		n = m_length;
		m += n;
		if (m > m_value.length)
			expandCapacity(m);

		final char[] v = m_value;
		int addr = 0;
		while (length > 0) {
			int c = addr >>> 24;
			v[n] = c_bhDigits[c];
			v[++n] = c_blDigits[c];
			c = (addr >> 16) & 0xFF;
			v[++n] = c_bhDigits[c];
			v[++n] = c_blDigits[c];
			c = (addr >> 8) & 0xFF;
			v[++n] = c_bhDigits[c];
			v[++n] = c_blDigits[c];
			c = addr & 0xFF;
			v[++n] = c_bhDigits[c];
			v[++n] = c_blDigits[c];

			v[++n] = 'h';
			v[++n] = ':';

			int i = n + HM_DISTANCE;
			for (m = 0; m < HM_BYTES_PERROW && length > 0; ++m, ++offset, --length) {
				v[++n] = ' ';
				c = b[offset] & 0xFF;
				v[++n] = c_bhDigits[c];
				v[++n] = c_blDigits[c];

				v[++i] = Character.isISOControl(c) ? '.' : (char) c;
			}

			do {
				v[++n] = ' ';
				v[++n] = ' ';
				v[++n] = ' ';
			} while (++m <= HM_BYTES_PERROW);

			System.arraycopy(c_ls, 0, v, ++i, LSL);
			n = i + LSL;

			addr += HM_BYTES_PERROW;
		}

		m_length = n;
		return this;
	}
}
