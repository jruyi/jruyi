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
 * Encodes data into a chain of buffer units.
 *
 * @param <T>
 *            the type of the data object to be encoded
 * @since 2.5
 * @see IPrependEncoder
 */
public interface IPrependRangedEncoder<T> {

	/**
	 * Encodes the specified {@code length} of elements in the specified
	 * sequence {@code src} starting at {@code offset}, and writes the resultant
	 * bytes to the head of the specified {@code unitChain}.
	 *
	 * @param src
	 *            the array/list to be encoded
	 * @param offset
	 *            the index of the first element in the specified {@code src} to
	 *            be encoded
	 * @param length
	 *            the number of elements to be encoded
	 * @param unitChain
	 *            the unit chain where the encoded bytes to be prepended to
	 */
	void prepend(T src, int offset, int length, IUnitChain unitChain);
}
