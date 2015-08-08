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
 * This class is a factory class for float codecs.
 * 
 * @see IFloatCodec
 */
public final class FloatCodec {

	private static final IFloatCodecProvider c_provider = CodecProvider.getInstance().getFloatCodecProvider();

	/**
	 * This interface defines all the methods that a float codec provider has to
	 * implement. It is used to separate the implementation provider from the
	 * API module.
	 */
	public interface IFloatCodecProvider {

		/**
		 * Returns a big-endian float codec.
		 * 
		 * @return a big-endian float codec
		 */
		IFloatCodec bigEndianFloatCodec();

		/**
		 * Returns a little-endian float codec.
		 * 
		 * @return a little-endian float codec
		 */
		IFloatCodec littleEndianFloatCodec();
	}

	/**
	 * Returns a big-endian float codec.
	 * 
	 * @return a big-endian float codec
	 */
	public static IFloatCodec bigEndian() {
		return c_provider.bigEndianFloatCodec();
	}

	/**
	 * Returns a little-endian float codec.
	 * 
	 * @return a little-endian float codec
	 */
	public static IFloatCodec littleEndian() {
		return c_provider.littleEndianFloatCodec();
	}

	private FloatCodec() {
	}
}
