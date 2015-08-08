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
 * This class is a factory class for int codecs.
 * 
 * @see IIntCodec
 */
public final class IntCodec {

	private static final IIntCodecProvider c_provider = CodecProvider.getInstance().getIntCodecProvider();

	/**
	 * This interface defines all the methods that an int codec provider has to
	 * implement. It is used to separate the implementation provider from the
	 * API module.
	 */
	public interface IIntCodecProvider {

		/**
		 * Returns a big-endian int codec.
		 * 
		 * @return a big-endian int codec
		 */
		IIntCodec bigEndianIntCodec();

		/**
		 * Returns a little-endian int codec.
		 * 
		 * @return a little-endian int codec
		 */
		IIntCodec littleEndianIntCodec();

		/**
		 * Returns a varint int codec.
		 * 
		 * @return a varint int codec
		 * @since 1.2
		 */
		IIntCodec varintIntCodec();
	}

	/**
	 * Returns a big-endian int codec.
	 * 
	 * @return a big-endian int codec
	 */
	public static IIntCodec bigEndian() {
		return c_provider.bigEndianIntCodec();
	}

	/**
	 * Returns a little-endian int codec.
	 * 
	 * @return a little-endian int codec
	 */
	public static IIntCodec littleEndian() {
		return c_provider.littleEndianIntCodec();
	}

	/**
	 * Returns a varint int codec.
	 * 
	 * @return a varint int codec
	 * @since 1.2
	 */
	public static IIntCodec varint() {
		return c_provider.varintIntCodec();
	}

	private IntCodec() {
	}
}
