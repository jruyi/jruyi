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
 * This class is a factory class for double array codecs.
 *
 * @see Codec
 * @see ShortArrayCodec
 * @see IntArrayCodec
 * @see LongArrayCodec
 * @see FloatArrayCodec
 * @see StringCodec
 * @see CharArrayCodec
 * @see CharSequenceCodec
 * @since 2.0
 */
public final class DoubleArrayCodec {

	private static final IDoubleArrayCodecProvider c_provider = CodecProvider.getInstance()
			.getDoubleArrayCodecProvider();

	/**
	 * This interface defines all the methods that a double array codec provider
	 * has to implement. It is used to separate the implementation provider from
	 * the API module.
	 */
	public interface IDoubleArrayCodecProvider {

		/**
		 * Returns a big-endian double array codec.
		 *
		 * @return a big-endian double array codec
		 */
		ICodec<double[]> bigEndian();

		/**
		 * Returns a little-endian double array codec.
		 *
		 * @return a little-endian double array codec
		 */
		ICodec<double[]> littleEndian();

	}

	private DoubleArrayCodec() {
	}

	/**
	 * Returns a big-endian double array codec.
	 *
	 * @return a big-endian double array codec
	 */
	public static ICodec<double[]> bigEndian() {
		return c_provider.bigEndian();
	}

	/**
	 * Returns a little-endian double array codec.
	 *
	 * @return a little-endian double array codec
	 */
	public static ICodec<double[]> littleEndian() {
		return c_provider.littleEndian();
	}
}
