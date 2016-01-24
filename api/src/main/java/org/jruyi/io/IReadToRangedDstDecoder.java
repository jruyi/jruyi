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

/**
 * Decodes data from a chain of buffer units.
 *
 * @param <T>
 *            the type of the data object to be decoded
 * @since 2.5
 * @see IReadToDstDecoder
 */
public interface IReadToRangedDstDecoder<T> {

	/**
	 * Decodes the bytes from the specified {@code unitChain}, starting at
	 * <i>position</i> in the current unit, to the specified array/list
	 * {@code dst} from the (inclusive) element at the specified {@code offset}
	 * to the (exclusive) element at {@code (offset + length)}.
	 *
	 * @param dst
	 *            the array/list to hold the decoded elements
	 * @param offset
	 *            the offset of the first decoded element in {@code unitChain}
	 * @param length
	 *            the number of decoded elements
	 * @param unitChain
	 *            the bytes from which to be decoded
	 * @return the actual number of bytes decoded
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} or {@code length} doesn't hold the
	 *             condition
	 */
	int read(T dst, int offset, int length, IUnitChain unitChain);
}
