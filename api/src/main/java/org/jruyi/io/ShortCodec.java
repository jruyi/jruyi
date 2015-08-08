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
 * This class is a factory class for short codecs.
 * 
 * @see IShortCodec
 */
public final class ShortCodec {

	private static final IShortCodecProvider c_provider = CodecProvider.getInstance().getShortCodecProvider();

	/**
	 * This interface defines all the methods that a short codec provider has to
	 * implement. It is used to separate the implementation provider from the
	 * API module.
	 */
	public interface IShortCodecProvider {

		/**
		 * Returns a big-endian short codec.
		 * 
		 * @return a big-endian short codec
		 */
		IShortCodec bigEndianShortCodec();

		/**
		 * Returns a little-endian short codec.
		 * 
		 * @return a little-endian short codec
		 */
		IShortCodec littleEndianShortCodec();

		/**
		 * Returns a varint short codec.
		 * 
		 * @return a varint short codec
		 * @since 1.2
		 */
		IShortCodec varintShortCodec();
	}

	/**
	 * Returns a big-endian short codec.
	 * 
	 * @return a big-endian short codec
	 */
	public static IShortCodec bigEndian() {
		return c_provider.bigEndianShortCodec();
	}

	/**
	 * Returns a little-endian short codec.
	 * 
	 * @return a little-endian short codec
	 */
	public static IShortCodec littleEndian() {
		return c_provider.littleEndianShortCodec();
	}

	/**
	 * Returns a varint short codec.
	 * 
	 * @return a varint short codec
	 * @since 1.2
	 */
	public static IShortCodec varint() {
		return c_provider.varintShortCodec();
	}

	private ShortCodec() {
	}
}
