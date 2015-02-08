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

/**
 * A character sequence to be searched for using the Knuth-Morris-Pratt
 * algorithm.
 */
public final class CharKmp {

	private final char[] m_pattern;
	private int[] m_kmp;
	private int[] m_pmk;

	/**
	 * Constructs a KMP pattern with the given {@code pattern} as the sequence
	 * to be searched for.
	 * 
	 * @param pattern
	 *            the character sequence to be searched for
	 */
	public CharKmp(char[] pattern) {
		this(pattern, 0, pattern.length);
	}

	/**
	 * Constructs a KMP pattern with the given {@code pattern} starting at
	 * {@code offset} ending at {@code (offset + length)} to be searched for.
	 * 
	 * @param pattern
	 *            the char sequence to be searched for
	 * @param offset
	 *            the index of the first character of the sequence to be
	 *            searched for
	 * @param length
	 *            the number of characters to be searched for
	 */
	public CharKmp(char[] pattern, int offset, int length) {
		char[] copy = new char[length];
		System.arraycopy(pattern, offset, copy, 0, length);
		m_pattern = copy;
	}

	/**
	 * Constructs a KMP pattern with the given {@code pattern} as the sequence
	 * to be searched for.
	 * 
	 * @param pattern
	 *            the character sequence to be searched for
	 */
	public CharKmp(String pattern) {
		this(pattern, 0, pattern.length());
	}

	/**
	 * Constructs a KMP pattern with the given {@code pattern} starting at
	 * {@code offset} ending at {@code (offset + length)} to be searched for.
	 * 
	 * @param pattern
	 *            the character sequence to be searched for
	 * @param offset
	 *            the index of the first character of the sequence to be
	 *            searched for
	 * @param length
	 *            the number of the characters to be searched for
	 */
	public CharKmp(String pattern, int offset, int length) {
		char[] copy = new char[length];
		pattern.getChars(offset, offset + length, copy, 0);
		m_pattern = copy;
	}

	/**
	 * Returns the index of the first occurrence of this KMP sequence in the
	 * given sequence {@code target} starting at {@code offset} ending at
	 * {@code (offset + length)}.
	 * 
	 * @param target
	 *            the sequence to be searched
	 * @param offset
	 *            the index to be searched from
	 * @param length
	 *            the number of characters to be searched
	 * @return the index of the first matched subsequence, or {@code -1} if not
	 *         found
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} or {@code length} doesn't hold the
	 *             condition
	 */
	public int findIn(char[] target, int offset, int length) {
		if (offset < 0 || length < 0 || (length += offset) > target.length)
			throw new IndexOutOfBoundsException();

		char[] pattern = m_pattern;
		int n = pattern.length;
		if (n == 0)
			return offset;

		length -= n;
		--n;

		int[] table = getKmpTable();
		int i = 0;
		while (offset <= length) {
			if (pattern[i] == target[offset + i]) {
				if (i == n)
					return offset;
				++i;
			} else {
				offset += i;
				i = table[i];
				offset -= i;
				if (i < 0)
					i = 0;
			}
		}

		return -1;
	}

	/**
	 * Returns the index of the rightmost occurrence of this KMP sequence in the
	 * given sequence {@code target} starting at {@code offset} ending at
	 * {@code (offset + length)}.
	 * 
	 * @param target
	 *            the sequence to be searched
	 * @param offset
	 *            the index to be searched from
	 * @param length
	 *            the number of characters to be searched
	 * @return the index of the rightmost occurrence of this KMP, or {@code -1}
	 *         if not found
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} or {@code length} doesn't hold the
	 *             condition
	 */
	public int rfindIn(char[] target, int offset, int length) {

		if (offset < 0 || length < 0 || (length += offset) > target.length)
			throw new IndexOutOfBoundsException();

		char[] pattern = m_pattern;
		int n = pattern.length;
		if (n == 0)
			return length;

		--n;
		offset += n;
		--length;

		int[] table = getPmkTable();
		int i = 0;
		while (length >= offset) {
			if (pattern[n - i] == target[length - i]) {
				if (i == n)
					return length - n;
				++i;
			} else {
				length -= i;
				i = table[i];
				length += i;
				if (i < 0)
					i = 0;
			}
		}

		return -1;
	}

	/**
	 * Returns the length of this sequence.
	 * 
	 * @return the length of this sequence
	 */
	public int length() {
		return m_pattern.length;
	}

	private int[] getKmpTable() {
		if (m_kmp == null)
			m_kmp = buildKmpTable(m_pattern);
		return m_kmp;
	}

	private int[] getPmkTable() {
		if (m_pmk == null)
			m_pmk = buildPmkTable(m_pattern);
		return m_pmk;
	}

	private static int[] buildKmpTable(char[] w) {
		int n = w.length;
		if (n == 0)
			return null;

		int[] t = new int[n];
		t[0] = -1;
		if (n == 1)
			return t;

		// t[1] = 0;
		int pos = 1;
		int cnd = 0;
		--n;
		while (pos < n) {
			if (w[pos] == w[cnd])
				t[++pos] = ++cnd;
			else if (cnd > 0)
				cnd = t[cnd];
			else
				++pos; // t[++pos] = 0;
		}

		return t;
	}

	private static int[] buildPmkTable(char[] w) {
		int n = w.length;
		if (n == 0)
			return null;

		int[] t = new int[n];
		t[0] = -1;
		if (n == 1)
			return t;

		--n;
		// t[1] = 0;
		int pos = n - 1;
		int cnd = n;
		while (pos > 0) {
			if (w[pos] == w[cnd])
				t[n - (--pos)] = n - (--cnd);
			else if (cnd < n)
				cnd = n - t[n - cnd];
			else
				--pos; // t[n - pos--] = 0;
		}

		return t;
	}
}
