/**
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
 * This class is a factory class for long codecs.
 * 
 * @see ILongCodec
 */
public final class LongCodec {

	private static final ILongCodecProvider c_provider = CodecProvider
			.getInstance().getLongCodecProvider();

	/**
	 * This interface defines all the methods that a long codec provider has to
	 * implement. It is used to separate the implementation provider from the
	 * API module.
	 */
	public interface ILongCodecProvider {

		/**
		 * Returns a big-endian long codec.
		 * 
		 * @return a big-endian long codec
		 */
		public ILongCodec bigEndianLongCodec();

		/**
		 * Returns a little-endian long codec.
		 * 
		 * @return a little-endian long codec
		 */
		public ILongCodec littleEndianLongCodec();
	}

	/**
	 * Returns a big-endian long codec.
	 * 
	 * @return a big-endian long codec
	 */
	public static ILongCodec bigEndian() {
		return c_provider.bigEndianLongCodec();
	}

	/**
	 * Returns a little-endian long codec.
	 * 
	 * @return a little-endian long codec
	 */
	public static ILongCodec littleEndian() {
		return c_provider.littleEndianLongCodec();
	}

	private LongCodec() {
	}
}
