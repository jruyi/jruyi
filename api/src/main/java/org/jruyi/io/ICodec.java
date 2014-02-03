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

import java.nio.BufferUnderflowException;

/**
 * A codec is used to encode/decode data to/from a chain of buffer units.
 * 
 * @param <T>
 *            the type of the data object to be encoded/decoded
 * 
 * @see Codec
 */
public interface ICodec<T> {

	/**
	 * Decodes the bytes from the specified {@code unitChain}, starting at
	 * <i>position</i> in the current unit, to an object of type {@code T} and
	 * returns the resultant object.
	 * 
	 * @param unitChain
	 *            the bytes from which to be decoded
	 * @return the decoded object
	 * @throws BufferUnderflowException
	 *             if there's not enough data remaining in the {@code unitChain}
	 */
	public T read(IUnitChain unitChain);

	/**
	 * Decodes the specified {@code length} of bytes from the specified
	 * {@code unitChain}, starting at <i>position</i> in the current unit, to an
	 * object of type {@code T} and returns the resultant object.
	 * 
	 * @param unitChain
	 *            the bytes from which to be decoded
	 * @param length
	 *            the number of bytes to be decoded
	 * @return the resultant object
	 * @throws BufferUnderflowException
	 *             if there's not enough data remaining in the {@code unitChain}
	 */
	public T read(IUnitChain unitChain, int length);

	/**
	 * Decodes the bytes from the specified {@code unitChain}, starting at
	 * <i>position</i> in the current unit, to the specified {@code dst} and
	 * returns the actual number of bytes decoded.
	 * 
	 * @param dst
	 *            the object to hold the decoded result
	 * @param unitChain
	 *            the bytes from which to be decoded
	 * @return the actual number of bytes decoded
	 */
	public int read(T dst, IUnitChain unitChain);

	/**
	 * Decodes the bytes from the specified {@code unitChain}, starting at
	 * <i>position</i> in the current unit, to the specified collection
	 * {@code dst} from the (inclusive) element at the specified {@code offset}
	 * to the (exclusive) element at {@code (offset + length)}.
	 * 
	 * @param dst
	 *            the collection to hold the decoded elements
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
	public int read(T dst, int offset, int length, IUnitChain unitChain);

	/**
	 * Encodes the specified {@code src} and writes the resultant bytes to the
	 * end of the specified {@code unitChain}.
	 * 
	 * @param src
	 *            the object to be encoded
	 * @param unitChain
	 *            the unit chain where the encoded bytes to be written to
	 */
	public void write(T src, IUnitChain unitChain);

	/**
	 * Encodes the specified {@code length} of elements in the specified
	 * collection {@code src} starting at {@code offset}, and writes the
	 * resultant bytes to the end of the specified {@code unitChain}.
	 * 
	 * @param src
	 *            the collection to be encoded
	 * @param offset
	 *            the index of the first element in {@code src} to be encoded
	 * @param length
	 *            the number of elements to be encoded
	 * @param unitChain
	 *            the unit chain where the encoded bytes to be written to
	 */
	public void write(T src, int offset, int length, IUnitChain unitChain);

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
	public T get(IUnitChain unitChain, int index);

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
	public T get(IUnitChain unitChain, int index, int length);

	/**
	 * Decodes the bytes from the specified {@code unitChain} starting at the
	 * specified {@code index} to the specified {@code dst}.
	 * 
	 * @param dst
	 *            the object to hold the decoded result
	 * @param unitChain
	 *            the bytes from which to be decoded
	 * @param index
	 *            the offset of the first byte to be decoded
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is out of bounds
	 */
	public void get(T dst, IUnitChain unitChain, int index);

	/**
	 * Decodes the bytes from the specified {@code unitChain} starting at the
	 * specified {@code index} to the specified collection {@code dst} from the
	 * (inclusive) element at the specified {@code offset} to the (exclusive)
	 * element at {@code (offset + length)}.
	 * 
	 * @param dst
	 *            the collection to hold the decoded elements
	 * @param offset
	 *            the offset of the first decoded element in {@code unitChain}
	 * @param length
	 *            the number of decoded elements
	 * @param unitChain
	 *            the bytes from which to be decoded
	 * @param index
	 *            the offset of the first byte to be decoded
	 * @throws IndexOutOfBoundsException
	 *             if {@code offset} or {@code length} does not hold the
	 *             condition, or {@code index} is out of bounds
	 */
	public void get(T dst, int offset, int length, IUnitChain unitChain,
			int index);

	/**
	 * Encodes the specified {@code src} and sets the resultant bytes to the
	 * specified {@code unitChain} starting at the specified {@code index}.
	 * 
	 * @param src
	 *            the object to be encoded
	 * @param unitChain
	 *            the unit chain where the encoded bytes to be set to
	 * @param index
	 *            the offset of the first byte in the current unit to be set
	 * @throws IndexOutOfBoundsException
	 *             if {@code index} is out of bounds
	 */
	public void set(T src, IUnitChain unitChain, int index);

	/**
	 * Encodes the specified {@code length} of elements in the specified
	 * collection {@code src} starting at the specified {@code offset} and sets
	 * the resultant bytes to the specified {@code unitChain} starting at
	 * {@code index}.
	 * 
	 * @param src
	 *            the collection whose elements to be encoded
	 * @param offset
	 *            the index of the first element in the {@code src} to be
	 *            encoded
	 * @param length
	 *            the number of elements in the {@code src} to be encoded
	 * @param unitChain
	 *            the unit chain where the encoded bytes to be set to
	 * @param index
	 *            the offset of the first byte in the current unit to be set
	 */
	public void set(T src, int offset, int length, IUnitChain unitChain,
			int index);

	/**
	 * Encodes the specified {@code src} and writes the resultant bytes to the
	 * head of the specified {@code unitChain}.
	 * 
	 * @param src
	 *            the object to be encoded
	 * @param unitChain
	 *            the unit chain where the encoded bytes to be prepended to
	 */
	public void prepend(T src, IUnitChain unitChain);

	/**
	 * Encodes the specified {@code length} of elements in the specified
	 * collection {@code src} starting at {@code offset}, and writes the
	 * resultant bytes to the head of the specified {@code unitChain}.
	 * 
	 * @param src
	 *            the collection to be encoded
	 * @param offset
	 *            the index of the first element in the specified {@code src} to
	 *            be encoded
	 * @param length
	 *            the number of elements to be encoded
	 * @param unitChain
	 *            the unit chain where the encoded bytes to be prepended to
	 */
	public void prepend(T src, int offset, int length, IUnitChain unitChain);
}
