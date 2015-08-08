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
 * This class is a factory class for short array codecs.
 *
 * @see Codec
 * @see IntArrayCodec
 * @see LongArrayCodec
 * @see FloatArrayCodec
 * @see DoubleArrayCodec
 * @see StringCodec
 * @see CharArrayCodec
 * @see CharSequenceCodec
 * @since 2.0
 */
public final class ShortArrayCodec {

	private static final IShortArrayCodecProvider c_provider = CodecProvider.getInstance().getShortArrayCodecProvider();

	/**
	 * This interface defines all the methods that a short array codec provider
	 * has to implement. It is used to separate the implementation provider from
	 * the API module.
	 */
	public interface IShortArrayCodecProvider {

		/**
		 * Returns a big-endian short array codec.
		 *
		 * @return a big-endian short array codec
		 */
		ICodec<short[]> bigEndian();

		/**
		 * Returns a little-endian short array codec.
		 *
		 * @return a little-endian short array codec
		 */
		ICodec<short[]> littleEndian();
	}

	private ShortArrayCodec() {
	}

	/**
	 * Returns a big-endian short array codec.
	 *
	 * @return a big-endian short array codec
	 */
	public static ICodec<short[]> bigEndian() {
		return c_provider.bigEndian();
	}

	/**
	 * Returns a little-endian short array codec.
	 *
	 * @return a little-endian short array codec
	 */
	public static ICodec<short[]> littleEndian() {
		return c_provider.littleEndian();
	}
}
