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
 * @see IGetDecoder
 */
public interface IGetLimitedDecoder<T> {

	/**
	 * Decodes the specified {@code length} of bytes from the specified
	 * {@code unitChain} starting at the specified {@code index} to an object of
	 * type {@code T} and returns the resultant object.
	 *
	 * @param unitChain
	 *            the bytes from which to be decoded
	 * @param index
	 *            the offset of the first byte to be decoded
	 * @param length
	 *            the number of bytes to be decoded
	 * @return the resultant object
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} or {@code length} does not hold the
	 *             condition
	 */
	T get(IUnitChain unitChain, int index, int length);
}
