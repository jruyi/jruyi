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

import java.nio.ByteBuffer;

import org.jruyi.common.IByteSequence;
import org.jruyi.io.internal.CodecProvider;

/**
 * This class provides a few useful {@code ICodec}s.
 * 
 * @see ICodec
 */
public final class Codec {

	private static final ICodecProvider c_provider = CodecProvider.getInstance().getCodecProvider();

	/**
	 * {@code ICodec} provider. It is used to separate the implementation
	 * provider from the API module.
	 */
	public interface ICodecProvider {

		/**
		 * Returns a byte array codec.
		 * 
		 * @return a byte array codec
		 */
		public ICodec<byte[]> byteArray();

		/**
		 * Returns a byte sequence codec.
		 * 
		 * @return a byte sequence codec
		 */
		public ICodec<IByteSequence> byteSequence();

		/**
		 * Returns a {@code ByteBuffer} codec.
		 * 
		 * @return a {@code ByteBuffer} codec
		 * @since 1.4
		 */
		public ICodec<ByteBuffer> byteBuffer();

		/**
		 * Returns a utf-8 string codec.
		 * 
		 * @return a utf-8 string codec
		 */
		public ICodec<String> utf_8();

		/**
		 * Returns a utf-16 string codec.
		 * 
		 * @return a utf-16 string codec
		 */
		public ICodec<String> utf_16();

		/**
		 * Returns a utf-16le string codec.
		 * 
		 * @return a utf-16le string codec
		 */
		public ICodec<String> utf_16le();

		/**
		 * Returns a utf-16be string codec.
		 * 
		 * @return a utf-16be string codec
		 */
		public ICodec<String> utf_16be();

		/**
		 * Returns an iso-8859-1 string codec.
		 * 
		 * @return an iso-8859-1 string codec
		 */
		public ICodec<String> iso_8859_1();

		/**
		 * Returns a us-ascii string codec.
		 * 
		 * @return a us-ascii string codec
		 */
		public ICodec<String> us_ascii();

		/**
		 * Returns a utf-8 char array codec.
		 * 
		 * @return a utf-8 char array codec
		 */
		public ICodec<char[]> utf_8_array();

		/**
		 * Returns a utf-16 char array codec.
		 * 
		 * @return a utf-16 char array codec
		 */
		public ICodec<char[]> utf_16_array();

		/**
		 * Returns a utf-16le char array codec.
		 * 
		 * @return a utf-16le char array codec
		 */
		public ICodec<char[]> utf_16le_array();

		/**
		 * Returns a utf-16be char array codec.
		 * 
		 * @return a utf-16be char array codec
		 */
		public ICodec<char[]> utf_16be_array();

		/**
		 * Returns an iso-8859-1 char array codec.
		 * 
		 * @return an iso-8859-1 char array codec
		 */
		public ICodec<char[]> iso_8859_1_array();

		/**
		 * Returns a us-ascii char array codec.
		 * 
		 * @return a us-ascii char array codec
		 */
		public ICodec<char[]> us_ascii_array();

		/**
		 * Returns a utf-8 char sequence codec.
		 * 
		 * @return a utf-8 char sequence codec
		 */
		public ICodec<CharSequence> utf_8_sequence();

		/**
		 * Returns a utf-16 char sequence codec.
		 * 
		 * @return a utf-16 char sequence codec
		 */
		public ICodec<CharSequence> utf_16_sequence();

		/**
		 * Returns a utf-16le char sequence codec.
		 * 
		 * @return a utf-16le char sequence codec
		 */
		public ICodec<CharSequence> utf_16le_sequence();

		/**
		 * Returns a utf-16be char sequence codec.
		 * 
		 * @return a utf-16be char sequence codec
		 */
		public ICodec<CharSequence> utf_16be_sequence();

		/**
		 * Returns an iso-8859-1 char sequence codec.
		 * 
		 * @return an iso-8859-1 char sequence codec
		 */
		public ICodec<CharSequence> iso_8859_1_sequence();

		/**
		 * Returns a us-ascii char sequence codec.
		 * 
		 * @return a us-ascii char sequence codec
		 */
		public ICodec<CharSequence> us_ascii_sequence();

		/**
		 * Returns a codec to encode/decode string in the specified charset.
		 * 
		 * @param charsetName
		 *            the charset to encode/decode
		 * @return a codec to encode/decode string in the specified charset
		 */
		public ICodec<String> charset(String charsetName);

		/**
		 * Returns a codec to encode/decode char array in the specified charset.
		 * 
		 * @param charsetName
		 *            the charset to encode/decode
		 * @return a codec to encode/decode char array in the specified charset
		 */
		public ICodec<char[]> charset_array(String charsetName);

		/**
		 * Returns a codec to encode/decode char sequence in the specified
		 * charset.
		 * 
		 * @param charsetName
		 *            the charset to encode/decode
		 * @return a codec to encode/decode char sequence in the specified
		 *         charset
		 */
		public ICodec<CharSequence> charset_sequence(String charsetName);
	}

	private Codec() {
	}

	/**
	 * Returns a byte array codec.
	 * 
	 * @return a byte array codec
	 */
	public static ICodec<byte[]> byteArray() {
		return c_provider.byteArray();
	}

	/**
	 * Returns a byte sequence codec.
	 * 
	 * @return a byte sequence codec
	 */
	public static ICodec<IByteSequence> byteSequence() {
		return c_provider.byteSequence();
	}

	/**
	 * Returns a {@code ByteBuffer} codec.
	 *
	 * @return a {@code ByteBuffer} codec
	 * @since 1.4
	 */
	public static ICodec<ByteBuffer> byteBuffer() {
		return c_provider.byteBuffer();
	}

	/**
	 * Returns a utf-8 string codec.
	 * 
	 * @return a utf-8 string codec
	 */
	public static ICodec<String> utf_8() {
		return c_provider.utf_8();
	}

	/**
	 * Returns a utf-16 string codec.
	 * 
	 * @return a utf-16 string codec
	 */
	public static ICodec<String> utf_16() {
		return c_provider.utf_16();
	}

	/**
	 * Returns a utf-16le string codec.
	 * 
	 * @return a utf-16le string codec
	 */
	public static ICodec<String> utf_16le() {
		return c_provider.utf_16le();
	}

	/**
	 * Returns a utf-16be string codec.
	 * 
	 * @return a utf-16be string codec
	 */
	public static ICodec<String> utf_16be() {
		return c_provider.utf_16be();
	}

	/**
	 * Returns an iso-8859-1 string codec.
	 * 
	 * @return an iso-8859-1 string codec
	 */
	public static ICodec<String> iso_8859_1() {
		return c_provider.iso_8859_1();
	}

	/**
	 * Returns a us-ascii string codec.
	 * 
	 * @return a us-ascii string codec
	 */
	public static ICodec<String> us_ascii() {
		return c_provider.us_ascii();
	}

	/**
	 * Returns a utf-8 char array codec.
	 * 
	 * @return a utf-8 char array codec
	 */
	public static ICodec<char[]> utf_8_array() {
		return c_provider.utf_8_array();
	}

	/**
	 * Returns a utf-16 char array codec.
	 * 
	 * @return a utf-16 char array codec
	 */
	public static ICodec<char[]> utf_16_array() {
		return c_provider.utf_16_array();
	}

	/**
	 * Returns a utf-16le char array codec.
	 * 
	 * @return a utf-16le char array codec
	 */
	public static ICodec<char[]> utf_16le_array() {
		return c_provider.utf_16le_array();
	}

	/**
	 * Returns a utf-16be char array codec.
	 * 
	 * @return a utf-16be char array codec
	 */
	public static ICodec<char[]> utf_16be_array() {
		return c_provider.utf_16be_array();
	}

	/**
	 * Returns an iso-8859-1 char array codec.
	 * 
	 * @return an iso-8859-1 char array codec
	 */
	public static ICodec<char[]> iso_8859_1_array() {
		return c_provider.iso_8859_1_array();
	}

	/**
	 * Returns a us-ascii char array codec.
	 * 
	 * @return a us-ascii char array codec
	 */
	public static ICodec<char[]> us_ascii_array() {
		return c_provider.us_ascii_array();
	}

	/**
	 * Returns a utf-8 char sequence codec.
	 * 
	 * @return a utf-8 char sequence codec
	 */
	public static ICodec<CharSequence> utf_8_sequence() {
		return c_provider.utf_8_sequence();
	}

	/**
	 * Returns a utf-16 char sequence codec.
	 * 
	 * @return a utf-16 char sequence codec
	 */
	public static ICodec<CharSequence> utf_16_sequence() {
		return c_provider.utf_16_sequence();
	}

	/**
	 * Returns a utf-16le char sequence codec.
	 * 
	 * @return a utf-16le char sequence codec
	 */
	public static ICodec<CharSequence> utf_16le_sequence() {
		return c_provider.utf_16le_sequence();
	}

	/**
	 * Returns a utf-16be char sequence codec.
	 * 
	 * @return a utf-16be char sequence codec
	 */
	public static ICodec<CharSequence> utf_16be_sequence() {
		return c_provider.utf_16be_sequence();
	}

	/**
	 * Returns an iso-8859-1 char sequence codec.
	 * 
	 * @return an iso-8859-1 char sequence codec
	 */
	public static ICodec<CharSequence> iso_8859_1_sequence() {
		return c_provider.iso_8859_1_sequence();
	}

	/**
	 * Returns a us-ascii char sequence codec.
	 * 
	 * @return a us-ascii char sequence codec
	 */
	public static ICodec<CharSequence> us_ascii_sequence() {
		return c_provider.us_ascii_sequence();
	}

	/**
	 * Returns a codec to encode/decode string in the specified charset.
	 * 
	 * @param charsetName
	 *            the charset to encode/decode
	 * @return a codec to encode/decode string in the specified charset
	 */
	public static ICodec<String> charset(String charsetName) {
		return c_provider.charset(charsetName);
	}

	/**
	 * Returns a codec to encode/decode char array in the specified charset.
	 * 
	 * @param charsetName
	 *            the charset to encode/decode
	 * @return a codec to encode/decode char array in the specified charset
	 */
	public static ICodec<char[]> charset_array(String charsetName) {
		return c_provider.charset_array(charsetName);
	}

	/**
	 * Returns a codec to encode/decode char sequence in the specified charset.
	 * 
	 * @param charsetName
	 *            the charset to encode/decode
	 * @return a codec to encode/decode char sequence in the specified charset
	 */
	public static ICodec<CharSequence> charset_sequence(String charsetName) {
		return c_provider.charset_sequence(charsetName);
	}
}
