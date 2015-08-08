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
 * This class is a factory class for char codecs.
 * 
 * @see ICharCodec
 */
public final class CharCodec {

	private static final ICharCodecProvider c_provider = CodecProvider.getInstance().getCharCodecProvider();

	/**
	 * This interface defines all the methods that a char codec provider has to
	 * implement. It is used to separate the implementation provider from the
	 * API module.
	 */
	public interface ICharCodecProvider {

		/**
		 * Returns a big-endian char codec.
		 * 
		 * @return a big-endian char codec
		 */
		ICharCodec bigEndianCharCodec();

		/**
		 * Returns a little-endian char codec.
		 * 
		 * @return a little-endian codec
		 */
		ICharCodec littleEndianCharCodec();
	}

	/**
	 * Returns a big-endian char codec.
	 * 
	 * @return a big-endian char codec
	 */
	public static ICharCodec bigEndian() {
		return c_provider.bigEndianCharCodec();
	}

	/**
	 * Returns a little-endian char codec.
	 * 
	 * @return a little-endian char codec
	 */
	public static ICharCodec littleEndian() {
		return c_provider.littleEndianCharCodec();
	}

	private CharCodec() {
	}
}
