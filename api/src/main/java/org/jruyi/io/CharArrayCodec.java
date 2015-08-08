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

import org.jruyi.io.internal.CodecProvider;

/**
 * This class is a factory class for char array codecs.
 *
 * @see Codec
 * @see ShortArrayCodec
 * @see IntArrayCodec
 * @see LongArrayCodec
 * @see FloatArrayCodec
 * @see DoubleArrayCodec
 * @see StringCodec
 * @see CharSequenceCodec
 * @since 2.0
 */
public final class CharArrayCodec {

	private static final ICharArrayCodecProvider c_provider = CodecProvider.getInstance().getCharArrayCodecProvider();

	/**
	 * This interface defines all the methods that a char array codec provider
	 * has to implement. It is used to separate the implementation of the
	 * provider from the API module.
	 */
	public interface ICharArrayCodecProvider {

		/**
		 * Returns a utf-8 char array codec.
		 *
		 * @return a utf-8 char array codec
		 */
		ICodec<char[]> utf_8();

		/**
		 * Returns a utf-16 char array codec.
		 *
		 * @return a utf-16 char array codec
		 */
		ICodec<char[]> utf_16();

		/**
		 * Returns a utf-16le char array codec.
		 *
		 * @return a utf-16le char array codec
		 */
		ICodec<char[]> utf_16le();

		/**
		 * Returns a utf-16be char array codec.
		 *
		 * @return a utf-16be char array codec
		 */
		ICodec<char[]> utf_16be();

		/**
		 * Returns an iso-8859-1 char array codec.
		 *
		 * @return an iso-8859-1 char array codec
		 */
		ICodec<char[]> iso_8859_1();

		/**
		 * Returns a us-ascii char array codec.
		 *
		 * @return a us-ascii char array codec
		 */
		ICodec<char[]> us_ascii();

		/**
		 * Returns a codec to encode/decode char array in the specified charset.
		 *
		 * @param charsetName
		 *            the charset to encode/decode
		 * @return a codec to encode/decode char array in the specified charset
		 */
		ICodec<char[]> charset(String charsetName);
	}

	private CharArrayCodec() {
	}

	/**
	 * Returns a utf-8 char array codec.
	 *
	 * @return a utf-8 char array codec
	 */
	public static ICodec<char[]> utf_8() {
		return c_provider.utf_8();
	}

	/**
	 * Returns a utf-16 char array codec.
	 *
	 * @return a utf-16 char array codec
	 */
	public static ICodec<char[]> utf_16() {
		return c_provider.utf_16();
	}

	/**
	 * Returns a utf-16le char array codec.
	 *
	 * @return a utf-16le char array codec
	 */
	public static ICodec<char[]> utf_16le() {
		return c_provider.utf_16le();
	}

	/**
	 * Returns a utf-16be char array codec.
	 *
	 * @return a utf-16be char array codec
	 */
	public static ICodec<char[]> utf_16be() {
		return c_provider.utf_16be();
	}

	/**
	 * Returns an iso-8859-1 char array codec.
	 *
	 * @return an iso-8859-1 char array codec
	 */
	public static ICodec<char[]> iso_8859_1() {
		return c_provider.iso_8859_1();
	}

	/**
	 * Returns a us-ascii char array codec.
	 *
	 * @return a us-ascii char array codec
	 */
	public static ICodec<char[]> us_ascii() {
		return c_provider.us_ascii();
	}

	/**
	 * Returns a codec to encode/decode char array in the specified charset.
	 *
	 * @param charsetName
	 *            the charset to encode/decode
	 * @return a codec to encode/decode char array in the specified charset
	 */
	public static ICodec<char[]> charset(String charsetName) {
		return c_provider.charset(charsetName);
	}
}
