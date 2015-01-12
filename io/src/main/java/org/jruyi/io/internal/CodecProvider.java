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
package org.jruyi.io.internal;

import java.nio.ByteBuffer;

import org.jruyi.common.IByteSequence;
import org.jruyi.io.CharArrayCodec;
import org.jruyi.io.CharSequenceCodec;
import org.jruyi.io.Codec;
import org.jruyi.io.DoubleArrayCodec;
import org.jruyi.io.DoubleCodec;
import org.jruyi.io.FloatArrayCodec;
import org.jruyi.io.FloatCodec;
import org.jruyi.io.ICodec;
import org.jruyi.io.IntArrayCodec;
import org.jruyi.io.IntCodec;
import org.jruyi.io.LongArrayCodec;
import org.jruyi.io.LongCodec;
import org.jruyi.io.ShortArrayCodec;
import org.jruyi.io.ShortCodec;
import org.jruyi.io.StringCodec;
import org.jruyi.io.buffer.codec.ByteArrayCodec;
import org.jruyi.io.buffer.codec.ByteBufferCodec;
import org.jruyi.io.buffer.codec.ByteSequenceCodec;

public final class CodecProvider implements Codec.ICodecProvider {

	private static final CodecProvider INST = new CodecProvider();

	private CodecProvider() {
	}

	public Codec.ICodecProvider getCodecProvider() {
		return this;
	}

	public static CodecProvider getInstance() {
		return INST;
	}

	public ShortCodec.IShortCodecProvider getShortCodecProvider() {
		return ShortCodecProvider.INST;
	}

	public IntCodec.IIntCodecProvider getIntCodecProvider() {
		return IntCodecProvider.INST;
	}

	public LongCodec.ILongCodecProvider getLongCodecProvider() {
		return LongCodecProvider.INST;
	}

	public LongArrayCodec.ILongArrayCodecProvider getLongArrayCodecProvider() {
		return LongArrayCodecProvider.INST;
	}

	public IntArrayCodec.IIntArrayCodecProvider getIntArrayCodecProvider() {
		return IntArrayCodecProvider.INST;
	}

	public ShortArrayCodec.IShortArrayCodecProvider getShortArrayCodecProvider() {
		return ShortArrayCodecProvider.INST;
	}

	public FloatArrayCodec.IFloatArrayCodecProvider getFloatArrayCodecProvider() {
		return FloatArrayCodecProvider.INST;
	}

	public DoubleArrayCodec.IDoubleArrayCodecProvider getDoubleArrayCodecProvider() {
		return DoubleArrayCodecProvider.INST;
	}

	public FloatCodec.IFloatCodecProvider getFloatCodecProvider() {
		return FloatCodecProvider.INST;
	}

	public DoubleCodec.IDoubleCodecProvider getDoubleCodecProvider() {
		return DoubleCodecProvider.INST;
	}

	public StringCodec.IStringCodecProvider getStringCodecProvider() {
		return StringCodecProvider.INST;
	}

	public CharArrayCodec.ICharArrayCodecProvider getCharArrayCodecProvider() {
		return CharArrayCodecProvider.INST;
	}

	public CharSequenceCodec.ICharSequenceCodecProvider getCharSequenceCodecProvider() {
		return CharSequenceCodecProvider.INST;
	}

	@Override
	public ICodec<byte[]> byteArray() {
		return ByteArrayCodec.INST;
	}

	@Override
	public ICodec<IByteSequence> byteSequence() {
		return ByteSequenceCodec.INST;
	}

	@Override
	public ICodec<ByteBuffer> byteBuffer() {
		return ByteBufferCodec.INST;
	}
}
