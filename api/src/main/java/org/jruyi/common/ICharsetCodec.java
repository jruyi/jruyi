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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

/**
 * A charset codec used to encode characters into bytes and decode bytes into
 * characters.
 */
public interface ICharsetCodec {

	/**
	 * Gets the charset of this codec.
	 * 
	 * @return the charset of this codec
	 */
	Charset getCharset();

	/**
	 * Encodes the characters in the specified {@code in} and appends the result
	 * to the specified {@code out}.
	 * 
	 * @param in
	 *            the {@code StrBuilder} holding the characters to be encoded
	 * @param out
	 *            the {@code BytesBuilder} to append the encoded bytes
	 */
	void encode(StringBuilder in, BytesBuilder out);

	/**
	 * Encodes the characters in the specified {@code in} starting at
	 * {@code offset} ending at {@code (offset + length)}, and appends the
	 * result to the specified {@code out}.
	 * 
	 * @param in
	 *            the {@code StrBuilder} holding the characters to be encoded
	 * @param offset
	 *            the index of the first character in the specified {@code in}
	 *            to be encoded
	 * @param length
	 *            the number of characters to be encoded
	 * @param out
	 *            the {@code BytesBuilder} to append the encoded bytes
	 */
	void encode(StringBuilder in, int offset, int length, BytesBuilder out);

	/**
	 * Encodes the characters in specified {@code chars} and appends the result
	 * to the specified {@code out}.
	 * 
	 * @param chars
	 *            the char array to be encoded
	 * @param out
	 *            the {@code BytesBuilder} to append the encoded bytes
	 */
	void encode(char[] chars, BytesBuilder out);

	/**
	 * Encodes the characters in the specified {@code chars} starting at
	 * {@code offset} ending at {@code (offset + length)}, and appends the
	 * result to the specified {@code out}.
	 * 
	 * @param chars
	 *            the char array to be encoded
	 * @param offset
	 *            the index of the first character in the specified
	 *            {@code chars} to be encoded
	 * @param length
	 *            the number of characters to be encoded
	 * @param out
	 *            the {@code BytesBuilder} to append
	 */
	void encode(char[] chars, int offset, int length, BytesBuilder out);

	/**
	 * Encodes characters in the specified char buffer {@code in} and appends
	 * the result to the specified {@code out}.
	 * 
	 * @param in
	 *            the char buffer to be encoded
	 * @param out
	 *            the {@code BytesBuilder} to append the encoded bytes
	 */
	void encode(CharBuffer in, BytesBuilder out);

	/**
	 * Encodes the characters from the given sequence of char buffers and
	 * appends the result to the specified {@code out}.
	 * 
	 * @param in
	 *            an array of {@code CharBuffer}s containing the characters to
	 *            be encoded
	 * @param out
	 *            the {@code BytesBuilder} to append the encoded bytes
	 */
	void encode(CharBuffer[] in, BytesBuilder out);

	/**
	 * Encodes the characters from the subsequence of the specified {@code in}
	 * starting at {@code offset} ending at {@code (offset + length)} and
	 * appends the result to the specified {@code out}.
	 * 
	 * @param in
	 *            an array of {@code CharBuffer}s containing the characters to
	 *            be encoded
	 * @param offset
	 *            the index in the buffer array of the first {@code CharBuffer}
	 *            from which characters to be encoded
	 * @param length
	 *            the number of {@code CharBuffer}s in which characters to be
	 *            encoded
	 * @param out
	 *            the {@code BytesBuilder} to append the encoded bytes
	 */
	void encode(CharBuffer[] in, int offset, int length, BytesBuilder out);

	/**
	 * Encodes the specified {@code chars} and returns the result.
	 * 
	 * @param chars
	 *            the char array to be encoded
	 * @return the encoded bytes
	 */
	byte[] encode(char[] chars);

	/**
	 * Encodes the characters in the specified {@code chars} starting at
	 * {@code offset} ending at {@code (offset + length)}, and returns the
	 * result.
	 * 
	 * @param chars
	 *            the char array to be encoded
	 * @param offset
	 *            the index of the first character in the specified
	 *            {@code chars} to be encoded
	 * @param length
	 *            the number of the characters to be encoded
	 * @return the encoded bytes
	 */
	byte[] encode(char[] chars, int offset, int length);

	/**
	 * Encodes the characters in the specified char buffer {@code in} and
	 * returns the result.
	 * 
	 * @param in
	 *            the char buffer to be encoded
	 * @return the encoded bytes
	 */
	byte[] encode(CharBuffer in);

	/**
	 * Encodes the characters from the given sequence of char buffers and
	 * returns the result.
	 * 
	 * @param in
	 *            an array of {@code CharBuffer}s containing the characters to
	 *            be encoded
	 * @return the encoded bytes
	 */
	byte[] encode(CharBuffer[] in);

	/**
	 * Encodes the characters from the subsequence of the specified {@code in}
	 * starting at {@code offset} ending at {@code (offset + length)} and
	 * returns the result.
	 * 
	 * @param in
	 *            an array of {@code CharBuffer}s containing the characters to
	 *            be encoded
	 * @param offset
	 *            the index in the buffer array of the first {@code CharBuffer}
	 *            from which characters to be encoded
	 * @param length
	 *            the number of {@code CharBuffer}s in which characters to be
	 *            encoded
	 * @return the encoded bytes
	 */
	byte[] encode(CharBuffer[] in, int offset, int length);

	/**
	 * Encodes the specified {@code str} and returns the result.
	 * 
	 * @param str
	 *            the {@code String} to be encoded
	 * @return the encoded bytes
	 */
	byte[] toBytes(String str);

	/**
	 * Encodes the substring of the specified {@code str} starting at
	 * {@code offset} ending at {@code (offset + length)}, and returns the
	 * result.
	 * 
	 * @param str
	 *            the {@code String} to be encoded
	 * @param offset
	 *            the index of the first character in the specified {@code str}
	 *            to be encoded
	 * @param length
	 *            the number of characters to be encoded
	 * @return the encoded bytes
	 */
	byte[] toBytes(String str, int offset, int length);

	/**
	 * Decodes the bytes in the specified {@code in} and appends the result to
	 * the specified {@code out}.
	 * 
	 * @param in
	 *            the {@code BytesBuilder} holding the bytes to be decoded
	 * @param out
	 *            the {@code StrBuilder} to append the decoded characters
	 */
	void decode(BytesBuilder in, StringBuilder out);

	/**
	 * Decodes the subsequence of the specified {@code in} starting at
	 * {@code offset} ending at {@code (offset + length)}, and appends the
	 * result to the specified {@code out}.
	 * 
	 * @param in
	 *            the {@code BytesBuilder} holding the bytes to be decoded
	 * @param offset
	 *            the index of the first byte in the specified {@code in} to be
	 *            decoded
	 * @param length
	 *            the number of bytes to be decoded
	 * @param out
	 *            the {@code StrBuilder} to append the decoded characters
	 */
	void decode(BytesBuilder in, int offset, int length, StringBuilder out);

	/**
	 * Decodes the specified bytes {@code in} and appends the result to the
	 * specified {@code out}.
	 * 
	 * @param in
	 *            the byte array to be decoded
	 * @param out
	 *            the {@code StrBuilder} to append the decoded characters
	 */
	void decode(byte[] in, StringBuilder out);

	/**
	 * Decodes the bytes in the specified byte array {@code in} starting at
	 * {@code offset} ending at {@code (offset + length)}, and appends the
	 * result to the specified {@code out}.
	 * 
	 * @param in
	 *            the byte array to be decoded
	 * @param offset
	 *            the index of the first byte in the specified {@code in} to be
	 *            decoded
	 * @param length
	 *            the number of bytes to be decoded
	 * @param out
	 *            the {@code StrBuilder} to append decoded characters
	 */
	void decode(byte[] in, int offset, int length, StringBuilder out);

	/**
	 * Decodes the specified bytes {@code in} and appends the result to the
	 * specified {@code out}.
	 * 
	 * @param in
	 *            the byte buffer to be decoded
	 * @param out
	 *            the {@code StrBuilder} to append decoded characters
	 */
	void decode(ByteBuffer in, StringBuilder out);

	/**
	 * Decodes the bytes from the given sequence of byte buffers and appends the
	 * result to the specified {@code out}.
	 * 
	 * @param in
	 *            an array of {@code ByteBuffer}s containing the bytes to be
	 *            decoded
	 * @param out
	 *            the {@code StrBuilder} to append the decoded characters
	 */
	void decode(ByteBuffer[] in, StringBuilder out);

	/**
	 * Decodes the bytes from the subsequence of the specified {@code in}
	 * starting at {@code offset} ending at {@code (offset + length)} and
	 * appends the result to the specified {@code out}.
	 * 
	 * @param in
	 *            an array of {@code ByteBuffer}s containing the bytes to be
	 *            decoded
	 * @param offset
	 *            the index in the buffer array of the first {@code ByteBuffer}
	 *            from which bytes to be decoded
	 * @param length
	 *            the number of {@code ByteBuffer}s in which bytes to be decoded
	 * @param out
	 *            the {@code StrBuilder} to append the decoded characters
	 */
	void decode(ByteBuffer[] in, int offset, int length, StringBuilder out);

	/**
	 * Decodes the specified bytes {@code in} and returns the result.
	 * 
	 * @param in
	 *            the byte array to be decoded
	 * @return the decoded char array
	 */
	char[] decode(byte[] in);

	/**
	 * Decodes the bytes in the specified byte array {@code in} starting at
	 * {@code offset} ending at {@code (offset + length)}, and returns the
	 * result.
	 * 
	 * @param in
	 *            the byte array to be decoded
	 * @param offset
	 *            the index of the first byte in the specified {@code in} to be
	 *            decoded
	 * @param length
	 *            the number of bytes to be decoded
	 * @return the decoded char array
	 */
	char[] decode(byte[] in, int offset, int length);

	/**
	 * Decodes the bytes in the specified byte buffer {@code in} and returns the
	 * result.
	 * 
	 * @param in
	 *            the byte buffer to be decoded
	 * @return the decoded char array
	 */
	char[] decode(ByteBuffer in);

	/**
	 * Decodes the bytes from the given sequence of byte buffers and returns the
	 * result.
	 * 
	 * @param in
	 *            an array of {@code ByteBuffer}s containing the bytes to be
	 *            decoded
	 * @return return the decoded characters
	 */
	char[] decode(ByteBuffer[] in);

	/**
	 * Decodes the bytes from the subsequence of the specified {@code in}
	 * starting at {@code offset} ending at {@code (offset + length)} and
	 * returns the result.
	 * 
	 * @param in
	 *            an array of {@code ByteBuffer}s containing the bytes to be
	 *            decoded
	 * @param offset
	 *            the index in the buffer array of the first {@code ByteBuffer}
	 *            from which bytes to be decoded
	 * @param length
	 *            the number of {@code ByteBuffer}s in which bytes to be decoded
	 * @return return the decoded characters
	 */
	char[] decode(ByteBuffer[] in, int offset, int length);

	/**
	 * Decodes the specified bytes {@code in} and returns the resultant
	 * {@code String}.
	 * 
	 * @param in
	 *            the byte array to be decoded
	 * @return the decoded {@code String}
	 */
	String toString(byte[] in);

	/**
	 * Decodes the bytes in the specified byte array {@code in} starting at
	 * {@code offset} ending at {@code (offset + length)}, and returns the
	 * resultant {@code String}.
	 * 
	 * @param in
	 *            the byte array to be decoded
	 * @param offset
	 *            the index of the first byte in the specified {@code in} to be
	 *            decoded
	 * @param length
	 *            the number of the bytes to be decoded
	 * @return the decoded {@code String}
	 */
	String toString(byte[] in, int offset, int length);

	/**
	 * Decodes the bytes in the specified {@code in} and returns the resultant
	 * {@code String}.
	 * 
	 * @param in
	 *            the bytes from which to be decoded
	 * @return the decoded {@code String}
	 */
	String toString(ByteBuffer in);

	/**
	 * Decodes the bytes in the specified byte buffer {@code in} and returns the
	 * resultant {@code String}.
	 * 
	 * @param in
	 *            an array of {@code ByteBuffer}s containing the bytes to be
	 *            decoded
	 * @return the decoded {@code String}
	 */
	String toString(ByteBuffer[] in);

	/**
	 * Decodes the bytes from the subsequence of the specified {@code in}
	 * starting at {@code offset} ending at {@code (offset + length)} and
	 * returns the resultant {@code String}.
	 * 
	 * @param in
	 *            an array of {@code ByteBuffer}s containing the bytes to be
	 *            decoded
	 * @param offset
	 *            the index in the buffer array of the first {@code ByteBuffer}
	 *            from which bytes to be decoded
	 * @param length
	 *            the number of {@code ByteBuffer}s in which bytes to be decoded
	 * @return the decoded {@code String}
	 */
	String toString(ByteBuffer[] in, int offset, int length);

	/**
	 * Returns a charset encoder in the thread local cache, or creates a new one
	 * to return if none in the cache.
	 * 
	 * @return a charset encoder
	 */
	CharsetEncoder getEncoder();

	/**
	 * Releases the charset encoder to the thread local cache.
	 * 
	 * @param encoder
	 *            the charset encoder to be cached
	 */
	void releaseEncoder(CharsetEncoder encoder);

	/**
	 * Returns a charset decoder in the thread local cache, or creates a new one
	 * to return if none in the cache.
	 * 
	 * @return a charset decoder
	 */
	CharsetDecoder getDecoder();

	/**
	 * Releases the charset decoder to the thread local cache.
	 * 
	 * @param decoder
	 *            the charset decoder to be cached
	 */
	void releaseDecoder(CharsetDecoder decoder);
}
