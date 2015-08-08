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
 * This class is a factory class for double codecs.
 * 
 * @see IDoubleCodec
 */
public final class DoubleCodec {

	private static final IDoubleCodecProvider c_provider = CodecProvider.getInstance().getDoubleCodecProvider();

	/**
	 * This interface defines all the methods that a double codec provider has
	 * to implement. It is used to separate the implementation provider from the
	 * API module.
	 */
	public interface IDoubleCodecProvider {

		/**
		 * Returns a big-endian double codec.
		 * 
		 * @return a big-endian double codec
		 */
		IDoubleCodec bigEndianDoubleCodec();

		/**
		 * Returns a little-endian double codec.
		 * 
		 * @return a little-endian double codec
		 */
		IDoubleCodec littleEndianDoubleCodec();
	}

	/**
	 * Returns a big-endian double codec.
	 * 
	 * @return a big-endian double codec
	 */
	public static IDoubleCodec bigEndian() {
		return c_provider.bigEndianDoubleCodec();
	}

	/**
	 * Returns a little-endian double codec.
	 * 
	 * @return a little-endian double codec
	 */
	public static IDoubleCodec littleEndian() {
		return c_provider.littleEndianDoubleCodec();
	}

	private DoubleCodec() {
	}
}
