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
 * A {@code Blob} consists of a list of byte sequences. The content of a
 * {@code Blob} can be looked at as a linear byte array.
 */
public final class Blob implements IDumpable, ICloseable {

	private static final int DEFAULT_CAPACITY = 16;
	private static final IThreadLocalCache<Blob> c_cache = ThreadLocalCache.weakArrayCache();
	private IByteSequence[] m_data;
	private int[] m_offsets;
	private int[] m_lengths;
	private int m_size;

	private Blob() {
		m_data = new IByteSequence[DEFAULT_CAPACITY];
		m_offsets = new int[DEFAULT_CAPACITY];
		m_lengths = new int[DEFAULT_CAPACITY];
	}

	private Blob(int capacity) {
		m_data = new IByteSequence[capacity];
		m_offsets = new int[capacity];
		m_lengths = new int[capacity];
	}

	/**
	 * Returns a {@code Blob} instance fetched from the current thread's local
	 * cache if it is not empty. Otherwise, a new instance will be created and
	 * returned.
	 * 
	 * @return an instance of {@code Blob}
	 */
	public static Blob get() {
		Blob blob = c_cache.take();
		if (blob == null)
			blob = new Blob();

		return blob;
	}

	/**
	 * Returns a {@code Blob} instance fetched from the current thread's local
	 * cache if it is not empty. Otherwise, a new instance will be created and
	 * returned. The initial capacity of the returned instance is ensured to be
	 * the specified {@code capacity}.
	 * 
	 * @param capacity
	 *            the initial capacity
	 * @return an instance of {@code Blob}
	 */
	public static Blob get(int capacity) {
		Blob blob = c_cache.take();
		if (blob == null)
			blob = new Blob(capacity);
		else
			blob.ensureCapacity(capacity);

		return blob;
	}

	/**
	 * Recycles this instance into the local cache of the current thread. Any
	 * further operation made to this instance is undefined.
	 */
	@Override
	public void close() {
		int n = m_size;
		IByteSequence[] data = m_data;
		for (int i = 0; i < n; ++i)
			data[i] = null;

		m_size = 0;

		c_cache.put(this);
	}

	/**
	 * Inserts the given {@code sequence} into this blob at the given
	 * {@code index}.
	 * 
	 * <p>
	 * This method behaves exactly the same as follows.
	 * 
	 * <pre>
	 * {@code insert(index, sequence, 0, sequence.length())}
	 * </pre>
	 * 
	 * @param index
	 *            the index at which the given {@code b} is to be inserted
	 * @param sequence
	 *            the byte sequence to be inserted.
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or greater than {@code size()}
	 */
	public void insert(int index, IByteSequence sequence) {
		insert(index, sequence, 0, sequence.length());
	}

	/**
	 * Inserts {@code length} bytes of the given {@code sequence} starting at
	 * the given {@code offset} into this blob at the given {@code index}.
	 * 
	 * @param index
	 *            the index at which the given {@code b} is to be inserted.
	 * @param sequence
	 *            the byte sequence to be inserted
	 * @param offset
	 *            the index of the first byte to be inserted
	 * @param length
	 *            the number of bytes to be inserted
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or greater than {@code size()}
	 * @throws IllegalArgumentException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code (offset + length)} is greater than
	 *             {@code sequence.length()}
	 */
	public void insert(int index, IByteSequence sequence, int offset, int length) {
		int size = m_size;
		if (index > size)
			throw new IndexOutOfBoundsException();

		if (offset < 0 || length < 0 || offset > sequence.length() - length)
			throw new IllegalArgumentException();

		ensureCapacity(size + 1);

		size -= index;
		int toIndex = index + 1;
		IByteSequence[] data = m_data;
		int[] offsets = m_offsets;
		int[] lengths = m_lengths;
		System.arraycopy(data, index, data, toIndex, size);
		System.arraycopy(offsets, index, offsets, toIndex, size);
		System.arraycopy(lengths, index, lengths, toIndex, size);
		data[index] = sequence;
		offsets[index] = offset;
		lengths[index] = length;

		++m_size;
	}

	/**
	 * Adds the whole given byte sequence {@code sequence} into this blob.
	 * 
	 * <p>
	 * This method behaves exactly the same as follows.
	 * 
	 * <pre>
	 * {@code add(sequence, 0, sequence.length())}
	 * </pre>
	 * 
	 * @param sequence
	 *            the byte sequence to be added
	 */
	public void add(IByteSequence sequence) {
		add(sequence, 0, sequence.length());
	}

	/**
	 * Adds {@code length} bytes of the given byte sequence {@code sequence}
	 * starting at {@code offset} into this blob.
	 * 
	 * @param sequence
	 *            the byte sequence to be added
	 * @param offset
	 *            the index of the first byte to be added
	 * @param length
	 *            the number of bytes of the given {@code sequence} to be added
	 * @throws IllegalArgumentException
	 *             if {@code offset} is negative, or {@code length} is negative,
	 *             or {@code (offset + length)} is greater than
	 *             {@code sequence.length()}
	 */
	public void add(IByteSequence sequence, int offset, int length) {
		if (offset < 0 || length < 0 || offset > sequence.length() - length)
			throw new IllegalArgumentException();

		if (length < 1)
			return;

		int n = m_size++;
		ensureCapacity(m_size);
		m_data[n] = sequence;
		m_offsets[n] = offset;
		m_lengths[n] = length;
	}

	/**
	 * Appends a hex dump to the given {@code builder}.
	 * 
	 * @param builder
	 *            the {@code StringBuilder} to be appended to
	 */
	@Override
	public void dump(StringBuilder builder) {
		builder.appendHexDump(m_data, m_offsets, m_lengths, m_size);
	}

	/**
	 * Increases the capacity if necessary, to ensure that at least the number
	 * of byte arrays specified by the given {@code minCapacity} can be hold.
	 * 
	 * @param minCapacity
	 *            the desired minimum capacity
	 */
	public void ensureCapacity(int minCapacity) {
		int oldCapacity = m_data.length;
		if (minCapacity > oldCapacity) {
			int newCapacity = (oldCapacity * 3) / 2 + 1;
			IByteSequence[] oldData = m_data;
			int[] oldOffsets = m_offsets;
			int[] oldLengths = m_lengths;

			IByteSequence[] data = new IByteSequence[newCapacity];
			int[] offsets = new int[newCapacity];
			int[] lengths = new int[newCapacity];

			System.arraycopy(oldData, 0, data, 0, oldCapacity);
			System.arraycopy(oldOffsets, 0, offsets, 0, oldCapacity);
			System.arraycopy(oldLengths, 0, lengths, 0, oldCapacity);

			m_data = data;
			m_offsets = offsets;
			m_lengths = lengths;
		}
	}

	/**
	 * Returns the index of the first occurrence of the specified byte sequence
	 * {@code kmp}.
	 * 
	 * @param kmp
	 *            the byte sequence to be searched for
	 * @return the index of the first occurrence of the specified byte sequence
	 *         {@code kmp}
	 */
	public int indexOf(ByteKmp kmp) {
		return kmp.findIn(m_data, m_offsets, m_lengths, m_size);
	}

	/**
	 * Returns the index within this sequence of the rightmost occurrence of the
	 * specified byte sequence {@code kmp}.
	 * 
	 * @param kmp
	 *            the byte sequence to be searched for
	 * @return the index of the rightmost occurrence of the specified byte
	 *         sequence {@code kmp}
	 */
	public int lastIndexOf(ByteKmp kmp) {
		return kmp.rfindIn(m_data, m_offsets, m_lengths, m_size);
	}
}
