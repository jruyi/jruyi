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
 * A byte sequence is a readable sequence of {@code byte} values. This interface
 * provides uniform, read-only access to the byte sequence. It doesn't change
 * any properties of the underlying buffer, such as <i>position</i>, <i>size</i>
 * and so on.
 * 
 * <p>
 * All the methods suffixed with <i>B</i> operate the bytes in big-endian order.
 * All those suffixed with <i>L</i> operate the bytes in little-endian order.
 */
public interface IByteSequence {

	/**
	 * Returns the {@code byte} value at the specified {@code index}.
	 * 
	 * @param index
	 *            the offset of the byte to be got
	 * @return the {@code byte} value at the specified {@code index}
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is negative or not smaller than
	 *             {@code length()}
	 */
	byte byteAt(int index);

	/**
	 * Returns the bytes starting from the specified {@code index} to the end of
	 * the byte sequence.
	 * 
	 * <p>
	 * If {@code index} is equal to {@code length()}, then a zero length byte
	 * array is returned.
	 * 
	 * <p>
	 * The method {@code getBytes(start)} behaves exactly the same way as
	 * 
	 * <pre>
	 * {@code getBytes(start, (length() - start))}
	 * </pre>
	 * 
	 * @param index
	 *            the offset of the first byte to be got
	 * @return a byte array containing the bytes got as required
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative or greater than {@code length()}
	 */
	byte[] getBytes(int index);

	/**
	 * Returns {@code length} bytes starting at the specified {@code index}. If
	 * {@code length} is zero, then a zero length byte array is returned.
	 * 
	 * @param index
	 *            the offset of the first byte to be got
	 * @param length
	 *            the number of bytes to be got
	 * @return a byte array containing the bytes got as required
	 * @throws IndexOutOfBoundsException
	 *             if {@code start} is negative, or length is negative, or
	 *             {@code (start + length) > length()}
	 */
	byte[] getBytes(int index, int length);

	/**
	 * Copies the requested sequence of bytes to the given {@code dst} starting
	 * at {@code dstBegin}.
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
	 *             <li>{@code srcEnd} is greater than {@code this.length()}.
	 *             <li>{@code dstBegin+srcEnd-srcBegin} is greater than
	 *             {@code dst.length}
	 *             </ul>
	 */
	void getBytes(int srcBegin, int srcEnd, byte[] dst, int dstBegin);

	/**
	 * Returns the length of this byte sequence. The length is the number of
	 * {@code byte}s in the sequence.
	 * 
	 * @return the number of {@code byte}s in the sequence
	 */
	int length();
}
