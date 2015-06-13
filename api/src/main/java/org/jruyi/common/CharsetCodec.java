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

import java.nio.charset.Charset;

import org.jruyi.common.internal.CharsetCodecProvider;

/**
 * This is the factory class for {@code ICharsetCodec}.
 * 
 * @see ICharsetCodec
 */
public final class CharsetCodec {

	/**
	 * The standard name for charset US-ASCII.
	 */
	public static final String US_ASCII = "US-ASCII";
	/**
	 * The standard name for charset ISO-8859-1.
	 */
	public static final String ISO_8859_1 = "ISO-8859-1";
	/**
	 * The standard name for charset UTF-8.
	 */
	public static final String UTF_8 = "UTF-8";
	/**
	 * The standard name for charset UTF-16BE.
	 */
	public static final String UTF_16BE = "UTF-16BE";
	/**
	 * The standard name for charset UTF-16LE.
	 */
	public static final String UTF_16LE = "UTF-16LE";
	/**
	 * The standard name for charset UTF-16.
	 */
	public static final String UTF_16 = "UTF-16";

	private static final IFactory c_factory = CharsetCodecProvider.getInstance().getFactory();

	/**
	 * A factory class to create instances of {@code ICharsetCodec}. It is used
	 * to separate the implementation provider from the API module.
	 */
	public interface IFactory {

		/**
		 * Gets an instance of {@code ICharsetCodec} using the platform's
		 * default charset.
		 * 
		 * @return an instance of {@code ICharsetCodec}
		 */
		ICharsetCodec get();

		/**
		 * Gets an instance of {@code ICharsetCodec} using the specified
		 * charset.
		 * 
		 * @param charsetName
		 *            the name of the charset to codec
		 * @return an instance of {@code ICharsetCodec}
		 */
		ICharsetCodec get(String charsetName);

		/**
		 * Gets an instance of {@code ICharsetCodec} using the specified
		 * {@code charset}.
		 * 
		 * @param charset
		 *            the charset to codec
		 * @return an instance of {@code ICharsetCodec}
		 */
		ICharsetCodec get(Charset charset);
	}

	private CharsetCodec() {
	}

	/**
	 * Gets an instance of {@code ICharsetCodec} using the platform's default
	 * charset.
	 * 
	 * @return an instance of {@code ICharsetCodec}
	 */
	public static ICharsetCodec get() {
		return c_factory.get();
	}

	/**
	 * Gets an instance of {@code ICharsetCodec} using the specified charset.
	 * 
	 * @param charsetName
	 *            the name of the charset to codec
	 * @return an instance of {@code ICharsetCodec}
	 */
	public static ICharsetCodec get(String charsetName) {
		return c_factory.get(charsetName);
	}

	/**
	 * Gets an instance of {@code ICharsetCodec} using the specified
	 * {@code charset}.
	 * 
	 * @param charset
	 *            the charset to codec
	 * @return an instance of {@code ICharsetCodec}
	 */
	public static ICharsetCodec get(Charset charset) {
		return c_factory.get(charset);
	}
}
