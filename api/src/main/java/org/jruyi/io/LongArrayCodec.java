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
 * This class is a factory class for long array codecs.
 *
 * @see Codec
 * @see ShortArrayCodec
 * @see IntArrayCodec
 * @see FloatArrayCodec
 * @see DoubleArrayCodec
 * @see StringCodec
 * @see CharArrayCodec
 * @see CharSequenceCodec
 * @since 2.0
 */
public final class LongArrayCodec {

	private static final ILongArrayCodecProvider c_provider = CodecProvider.getInstance().getLongArrayCodecProvider();

	/**
	 * This interface defines all the methods that a long array codec provider
	 * has to implement. It is used to separate the implementation provider from
	 * the API module.
	 */
	public interface ILongArrayCodecProvider {

		/**
		 * Returns a big-endian long array codec.
		 *
		 * @return a big-endian long array codec
		 */
		ICodec<long[]> bigEndian();

		/**
		 * Returns a little-endian long array codec.
		 *
		 * @return a little-endian long array codec
		 */
		ICodec<long[]> littleEndian();
	}

	private LongArrayCodec() {
	}

	/**
	 * Returns a big-endian long array codec.
	 *
	 * @return a big-endian long array codec
	 */
	public static ICodec<long[]> bigEndian() {
		return c_provider.bigEndian();
	}

	/**
	 * Returns a little-endian long array codec.
	 *
	 * @return a little-endian long array codec
	 */
	public static ICodec<long[]> littleEndian() {
		return c_provider.littleEndian();
	}
}
