/**
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

import java.nio.BufferUnderflowException;

/**
 * This interface defines all the methods a long codec has to implement.
 * 
 * @see LongCodec
 */
public interface ILongCodec {

	/**
	 * Decodes the bytes from the specified {@code unitChain}, starting at
	 * <i>position</i> in the current unit, to a {@code long} value and returns
	 * the resultant {@code long} value.
	 * 
	 * @param unitChain
	 *            the bytes from which to be decoded
	 * @return the decoded {@code long} value
	 * @throws BufferUnderflowException
	 *             if there's not enough data remaining in the {@code unitChain}
	 */
	public long read(IUnitChain unitChain);

	/**
	 * Encodes the specified {@code long} value {@code l} and writes the
	 * resultant bytes to the end of the specified {@code unitChain}.
	 * 
	 * @param l
	 *            the {@code long} value to be encoded
	 * @param unitChain
	 *            the unit chain where the encoded bytes to be written to
	 */
	public void write(long l, IUnitChain unitChain);

	/**
	 * Decodes the bytes from the specified {@code unitChain}, starting at the
	 * specified {@code index} in the current unit, to a {@code long} value and
	 * returns the resultant {@code long} value.
	 * 
	 * @param unitChain
	 *            the bytes from which to be decoded
	 * @param index
	 *            the offset of the first byte in the current unit to be decoded
	 * @return the decoded {@code long} value
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is out of bounds
	 */
	public long get(IUnitChain unitChain, int index);

	/**
	 * Encodes the specified {@code long} value {@code l} and sets the resultant
	 * bytes to the specified {@code unitChain} starting from at the specified
	 * {@code index} in the current unit.
	 * 
	 * @param l
	 *            the {@code long} value to be encoded
	 * @param unitChain
	 *            the unit chain where the encoded bytes to be set to
	 * @param index
	 *            the offset of the first byte in the current unit to be set
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is out of bounds
	 */
	public void set(long l, IUnitChain unitChain, int index);

	/**
	 * Encodes the specified {@code long} value {@code l} and writes the
	 * resultant bytes to the head of the specified {@code unitChain}.
	 * 
	 * @param l
	 *            the {@code long} value to be encoded
	 * @param unitChain
	 *            the unit chain where the encoded bytes to be prepended to
	 */
	public void prepend(long l, IUnitChain unitChain);
}
