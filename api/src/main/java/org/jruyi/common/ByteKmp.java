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
 * A byte sequence to be searched for using the Knuth-Morris-Pratt algorithm.
 */
public final class ByteKmp {

	private final byte[] m_pattern;
	private int[] m_kmp;
	private int[] m_pmk;

	/**
	 * Constructs a KMP pattern with the given {@code pattern} as the sequence
	 * to be searched for.
	 * 
	 * @param pattern
	 *            the byte sequence to be searched for
	 */
	public ByteKmp(byte[] pattern) {
		this(pattern, 0, pattern.length);
	}

	/**
	 * Constructs a KMP pattern with the given {@code pattern} starting at
	 * {@code offset} ending at {@code (offset + length)} to be searched for.
	 * 
	 * @param pattern
	 *            the byte sequence to be searched for
	 * @param offset
	 *            the index of the first byte of the {@code pattern} to be
	 *            searched for
	 * @param length
	 *            the number of bytes to be searched for
	 */
	public ByteKmp(byte[] pattern, int offset, int length) {
		byte[] copy = new byte[length];
		System.arraycopy(pattern, offset, copy, 0, length);
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
	 *            the number of bytes to be searched
	 * @return the index of the first matched subsequence, or {@code -1} if not
	 *         found
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} or {@code length} doesn't hold the
	 *             condition
	 */
	public int findIn(byte[] target, int offset, int length) {
		if (offset < 0 || length < 0 || (length += offset) > target.length)
			throw new IndexOutOfBoundsException();

		byte[] pattern = m_pattern;
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
	 *            the number of bytes to be searched
	 * @return the index of the right most matched subsequence, or {@code -1} if
	 *         not found
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} or {@code length} doesn't hold the
	 *             condition
	 */
	public int rfindIn(byte[] target, int offset, int length) {

		if (offset < 0 || length < 0 || (length += offset) > target.length)
			throw new IndexOutOfBoundsException();

		byte[] pattern = m_pattern;
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

	int findIn(IByteSequence[] target, int[] offsets, int[] lengths, int size) {
		byte[] pattern = m_pattern;
		int i = pattern.length;
		if (i == 0)
			return 0;

		int n = i - 1;
		i = 0;
		int[] table = getKmpTable();
		int k = 0;
		int a = 0;
		while (a < size) {
			IByteSequence sequence = target[a];
			int b = offsets[a];
			int y = b + lengths[a];
			while (b < y) {
				if (pattern[i] == sequence.byteAt(b)) {
					if (i == n)
						return k;
					++i;
					++b;
				} else {
					k += i;
					i = table[i];
					k -= i;

					b -= i;
					while (b < 0)
						b += lengths[--a];

					sequence = target[a];
					y = offsets[a] + lengths[a];

					if (i < 0)
						i = 0;
				}
			}

			++a;
		}

		return -1;
	}

	int rfindIn(IByteSequence[] target, int[] offsets, int[] lengths, int size) {
		int k = 0;
		for (int i = 0; i < size; ++i)
			k += lengths[i];

		byte[] pattern = m_pattern;
		int i = pattern.length;
		if (i == 0)
			return k;

		if (i > k)
			return -1;

		--k;
		int n = i - 1;
		i = 0;
		int[] table = getPmkTable();
		int a = size;
		while (--a >= 0) {
			IByteSequence sequence = target[a];
			int y = offsets[a];
			int len = lengths[a];
			int b = y + len - 1;
			while (b >= y) {
				if (pattern[n - i] == sequence.byteAt(b)) {
					if (i == n)
						return k - n;
					++i;
					--b;
				} else {
					k -= i;
					i = table[i];
					k += i;

					b += i;
					if (b >= (len += y)) {
						do {
							b -= len;
							++a;
							len = lengths[a];
						} while (b >= len);
						sequence = target[a];
						y = offsets[a];
						b += y;
					}

					if (i < 0)
						i = 0;
				}
			}
		}

		return -1;
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

	private static int[] buildKmpTable(byte[] w) {
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

	private static int[] buildPmkTable(byte[] w) {
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
