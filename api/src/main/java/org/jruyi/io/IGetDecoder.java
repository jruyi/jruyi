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
 * @see IGetLimitedDecoder
 */
public interface IGetDecoder<T> {

	/**
	 * Decodes the bytes from the specified {@code unitChain} starting at the
	 * specified {@code index} in the current unit and returns the resultant
	 * object.
	 *
	 * @param unitChain
	 *            the bytes from which to be decoded
	 * @param index
	 *            the offset of the first byte in the current unit to be decoded
	 * @return the decoded object
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is out of bounds
	 */
	T get(IUnitChain unitChain, int index);
}
