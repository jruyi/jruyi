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
 * This class is a factory class for char sequence codecs.
 *
 * @see Codec
 * @see ShortArrayCodec
 * @see IntArrayCodec
 * @see LongArrayCodec
 * @see FloatArrayCodec
 * @see DoubleArrayCodec
 * @see StringCodec
 * @see CharArrayCodec
 * @since 2.0
 */
public final class CharSequenceCodec {

	private static final ICharSequenceCodecProvider c_provider = CodecProvider.getInstance()
			.getCharSequenceCodecProvider();

	/**
	 * This interface defines all the methods that a char sequence codec
	 * provider has to implement. It is used to separate the implementation
	 * provider from the API module.
	 */
	public interface ICharSequenceCodecProvider {

		/**
		 * Returns a utf-8 char sequence codec.
		 *
		 * @return a utf-8 char sequence codec
		 */
		ICodec<CharSequence> utf_8();

		/**
		 * Returns a utf-16 char sequence codec.
		 *
		 * @return a utf-16 char sequence codec
		 */
		ICodec<CharSequence> utf_16();

		/**
		 * Returns a utf-16le char sequence codec.
		 *
		 * @return a utf-16le char sequence codec
		 */
		ICodec<CharSequence> utf_16le();

		/**
		 * Returns a utf-16be char sequence codec.
		 *
		 * @return a utf-16be char sequence codec
		 */
		ICodec<CharSequence> utf_16be();

		/**
		 * Returns an iso-8859-1 char sequence codec.
		 *
		 * @return an iso-8859-1 char sequence codec
		 */
		ICodec<CharSequence> iso_8859_1();

		/**
		 * Returns a us-ascii char sequence codec.
		 *
		 * @return a us-ascii char sequence codec
		 */
		ICodec<CharSequence> us_ascii();

		/**
		 * Returns a codec to encode/decode char sequence in the specified
		 * charset.
		 *
		 * @param charsetName
		 *            the charset to encode/decode
		 * @return a codec to encode/decode char sequence in the specified
		 *         charset
		 */
		ICodec<CharSequence> charset(String charsetName);
	}

	private CharSequenceCodec() {
	}

	/**
	 * Returns a utf-8 char sequence codec.
	 *
	 * @return a utf-8 char sequence codec
	 */
	public static ICodec<CharSequence> utf_8() {
		return c_provider.utf_8();
	}

	/**
	 * Returns a utf-16 char sequence codec.
	 *
	 * @return a utf-16 char sequence codec
	 */
	public static ICodec<CharSequence> utf_16() {
		return c_provider.utf_16();
	}

	/**
	 * Returns a utf-16le char sequence codec.
	 *
	 * @return a utf-16le char sequence codec
	 */
	public static ICodec<CharSequence> utf_16le() {
		return c_provider.utf_16le();
	}

	/**
	 * Returns a utf-16be char sequence codec.
	 *
	 * @return a utf-16be char sequence codec
	 */
	public static ICodec<CharSequence> utf_16be() {
		return c_provider.utf_16be();
	}

	/**
	 * Returns an iso-8859-1 char sequence codec.
	 *
	 * @return an iso-8859-1 char sequence codec
	 */
	public static ICodec<CharSequence> iso_8859_1() {
		return c_provider.iso_8859_1();
	}

	/**
	 * Returns a us-ascii char sequence codec.
	 *
	 * @return a us-ascii char sequence codec
	 */
	public static ICodec<CharSequence> us_ascii() {
		return c_provider.us_ascii();
	}

	/**
	 * Returns a codec to encode/decode char sequence in the specified charset.
	 *
	 * @param charsetName
	 *            the charset to encode/decode
	 * @return a codec to encode/decode char sequence in the specified charset
	 */
	public static ICodec<CharSequence> charset(String charsetName) {
		return c_provider.charset(charsetName);
	}
}
