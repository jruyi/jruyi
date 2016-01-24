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

import java.nio.ByteBuffer;

import org.jruyi.common.IByteSequence;
import org.jruyi.io.internal.CodecProvider;

/**
 * This class provides several useful {@code ICodec}s.
 *
 * @see ShortArrayCodec
 * @see IntArrayCodec
 * @see LongArrayCodec
 * @see StringCodec
 * @see CharArrayCodec
 * @see CharSequenceCodec
 */
public final class Codec {

	private static final ICodecProvider c_provider = CodecProvider.getInstance().getCodecProvider();

	/**
	 * {@code ICodec} provider. It is used to separate the implementation
	 * provider from the API module.
	 */
	public interface ICodecProvider {

		/**
		 * Returns a byte array codec.
		 * 
		 * @return a byte array codec
		 */
		ICodec<byte[]> byteArray();

		/**
		 * Returns a byte sequence codec.
		 * 
		 * @return a byte sequence codec
		 */
		ICodec<IByteSequence> byteSequence();

		/**
		 * Returns a {@code ByteBuffer} codec.
		 * 
		 * @return a {@code ByteBuffer} codec
		 * @since 2.0
		 */
		ICodec<ByteBuffer> byteBuffer();
	}

	private Codec() {
	}

	/**
	 * Returns a byte array codec.
	 * 
	 * @return a byte array codec
	 */
	public static ICodec<byte[]> byteArray() {
		return c_provider.byteArray();
	}

	/**
	 * Returns a byte sequence codec.
	 * 
	 * @return a byte sequence codec
	 */
	public static ICodec<IByteSequence> byteSequence() {
		return c_provider.byteSequence();
	}

	/**
	 * Returns a {@code ByteBuffer} codec.
	 *
	 * @return a {@code ByteBuffer} codec
	 * @since 2.0
	 */
	public static ICodec<ByteBuffer> byteBuffer() {
		return c_provider.byteBuffer();
	}
}
