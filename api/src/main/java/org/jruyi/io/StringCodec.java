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
 * This class is a factory class for string codecs.
 *
 * @see Codec
 * @see ShortArrayCodec
 * @see IntArrayCodec
 * @see LongArrayCodec
 * @see FloatArrayCodec
 * @see DoubleArrayCodec
 * @see CharArrayCodec
 * @see CharSequenceCodec
 * @since 2.0
 */
public final class StringCodec {

	private static final IStringCodecProvider c_provider = CodecProvider.getInstance().getStringCodecProvider();

	/**
	 * This interface defines all the methods that a string codec provider has
	 * to implement. It is used to separate the implementation provider from the
	 * API module.
	 */
	public interface IStringCodecProvider {

		/**
		 * Returns a utf-8 string codec.
		 *
		 * @return a utf-8 string codec
		 */
		ICodec<String> utf_8();

		/**
		 * Returns a utf-16 string codec.
		 *
		 * @return a utf-16 string codec
		 */
		ICodec<String> utf_16();

		/**
		 * Returns a utf-16le string codec.
		 *
		 * @return a utf-16le string codec
		 */
		ICodec<String> utf_16le();

		/**
		 * Returns a utf-16be string codec.
		 *
		 * @return a utf-16be string codec
		 */
		ICodec<String> utf_16be();

		/**
		 * Returns an iso-8859-1 string codec.
		 *
		 * @return an iso-8859-1 string codec
		 */
		ICodec<String> iso_8859_1();

		/**
		 * Returns a us-ascii string codec.
		 *
		 * @return a us-ascii string codec
		 */
		ICodec<String> us_ascii();

		/**
		 * Returns a codec to encode/decode string in the specified charset.
		 *
		 * @param charsetName
		 *            the charset to encode/decode
		 * @return a codec to encode/decode string in the specified charset
		 */
		ICodec<String> charset(String charsetName);
	}

	private StringCodec() {
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
	 * Returns a codec to encode/decode string in the specified charset.
	 *
	 * @param charsetName
	 *            the charset to encode/decode
	 * @return a codec to encode/decode string in the specified charset
	 */
	public static ICodec<String> charset(String charsetName) {
		return c_provider.charset(charsetName);
	}
}
