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
import org.jruyi.io.Codec.ICodecProvider;
import org.jruyi.io.DoubleCodec.IDoubleCodecProvider;
import org.jruyi.io.FloatCodec.IFloatCodecProvider;
import org.jruyi.io.ICodec;
import org.jruyi.io.IDoubleCodec;
import org.jruyi.io.IFloatCodec;
import org.jruyi.io.IIntCodec;
import org.jruyi.io.ILongCodec;
import org.jruyi.io.IShortCodec;
import org.jruyi.io.IntCodec.IIntCodecProvider;
import org.jruyi.io.LongCodec.ILongCodecProvider;
import org.jruyi.io.ShortCodec.IShortCodecProvider;
import org.jruyi.io.buffer.BigEndianDoubleCodec;
import org.jruyi.io.buffer.BigEndianFloatCodec;
import org.jruyi.io.buffer.BigEndianIntCodec;
import org.jruyi.io.buffer.BigEndianLongCodec;
import org.jruyi.io.buffer.BigEndianShortCodec;
import org.jruyi.io.buffer.ByteArrayCodec;
import org.jruyi.io.buffer.ByteBufferCodec;
import org.jruyi.io.buffer.ByteSequenceCodec;
import org.jruyi.io.buffer.CharArrayCodec;
import org.jruyi.io.buffer.CharSequenceCodec;
import org.jruyi.io.buffer.LittleEndianDoubleCodec;
import org.jruyi.io.buffer.LittleEndianFloatCodec;
import org.jruyi.io.buffer.LittleEndianIntCodec;
import org.jruyi.io.buffer.LittleEndianLongCodec;
import org.jruyi.io.buffer.LittleEndianShortCodec;
import org.jruyi.io.buffer.StringCodec;
import org.jruyi.io.buffer.VarintIntCodec;
import org.jruyi.io.buffer.VarintLongCodec;
import org.jruyi.io.buffer.VarintShortCodec;

public final class CodecProvider implements IShortCodecProvider, IIntCodecProvider, ILongCodecProvider,
		IFloatCodecProvider, IDoubleCodecProvider, ICodecProvider {

	private static final CodecProvider INST = new CodecProvider();

	private CodecProvider() {
	}

	public static CodecProvider getInstance() {
		return INST;
	}

	public IShortCodecProvider getShortCodecProvider() {
		return this;
	}

	public IIntCodecProvider getIntCodecProvider() {
		return this;
	}

	public ILongCodecProvider getLongCodecProvider() {
		return this;
	}

	public IFloatCodecProvider getFloatCodecProvider() {
		return this;
	}

	public IDoubleCodecProvider getDoubleCodecProvider() {
		return this;
	}

	public ICodecProvider getCodecProvider() {
		return this;
	}

	@Override
	public IShortCodec bigEndianShortCodec() {
		return BigEndianShortCodec.INST;
	}

	@Override
	public IShortCodec littleEndianShortCodec() {
		return LittleEndianShortCodec.INST;
	}

	@Override
	public IShortCodec varintShortCodec() {
		return VarintShortCodec.INST;
	}

	@Override
	public IIntCodec bigEndianIntCodec() {
		return BigEndianIntCodec.INST;
	}

	@Override
	public IIntCodec littleEndianIntCodec() {
		return LittleEndianIntCodec.INST;
	}

	@Override
	public IIntCodec varintIntCodec() {
		return VarintIntCodec.INST;
	}

	@Override
	public ILongCodec bigEndianLongCodec() {
		return BigEndianLongCodec.INST;
	}

	@Override
	public ILongCodec littleEndianLongCodec() {
		return LittleEndianLongCodec.INST;
	}

	@Override
	public ILongCodec varintLongCodec() {
		return VarintLongCodec.INST;
	}

	@Override
	public IDoubleCodec bigEndianDoubleCodec() {
		return BigEndianDoubleCodec.INST;
	}

	@Override
	public IDoubleCodec littleEndianDoubleCodec() {
		return LittleEndianDoubleCodec.INST;
	}

	@Override
	public IFloatCodec bigEndianFloatCodec() {
		return BigEndianFloatCodec.INST;
	}

	@Override
	public IFloatCodec littleEndianFloatCodec() {
		return LittleEndianFloatCodec.INST;
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

	@Override
	public ICodec<String> utf_8() {
		return StringCodec.UTF_8;
	}

	@Override
	public ICodec<String> utf_16() {
		return StringCodec.UTF_16;
	}

	@Override
	public ICodec<String> utf_16le() {
		return StringCodec.UTF_16LE;
	}

	@Override
	public ICodec<String> utf_16be() {
		return StringCodec.UTF_16BE;
	}

	@Override
	public ICodec<String> iso_8859_1() {
		return StringCodec.ISO_8859_1;
	}

	@Override
	public ICodec<String> us_ascii() {
		return StringCodec.US_ASCII;
	}

	@Override
	public ICodec<char[]> utf_8_array() {
		return CharArrayCodec.UTF_8;
	}

	@Override
	public ICodec<char[]> utf_16_array() {
		return CharArrayCodec.UTF_16;
	}

	@Override
	public ICodec<char[]> utf_16le_array() {
		return CharArrayCodec.UTF_16LE;
	}

	@Override
	public ICodec<char[]> utf_16be_array() {
		return CharArrayCodec.UTF_16BE;
	}

	@Override
	public ICodec<char[]> iso_8859_1_array() {
		return CharArrayCodec.ISO_8859_1;
	}

	@Override
	public ICodec<char[]> us_ascii_array() {
		return CharArrayCodec.US_ASCII;
	}

	@Override
	public ICodec<CharSequence> utf_8_sequence() {
		return CharSequenceCodec.UTF_8;
	}

	@Override
	public ICodec<CharSequence> utf_16_sequence() {
		return CharSequenceCodec.UTF_16;
	}

	@Override
	public ICodec<CharSequence> utf_16le_sequence() {
		return CharSequenceCodec.UTF_16LE;
	}

	@Override
	public ICodec<CharSequence> utf_16be_sequence() {
		return CharSequenceCodec.UTF_16BE;
	}

	@Override
	public ICodec<CharSequence> iso_8859_1_sequence() {
		return CharSequenceCodec.ISO_8859_1;
	}

	@Override
	public ICodec<CharSequence> us_ascii_sequence() {
		return CharSequenceCodec.US_ASCII;
	}

	@Override
	public ICodec<String> charset(String charsetName) {
		return new StringCodec(charsetName);
	}

	@Override
	public ICodec<char[]> charset_array(String charsetName) {
		return new CharArrayCodec(charsetName);
	}

	@Override
	public ICodec<CharSequence> charset_sequence(String charsetName) {
		return new CharSequenceCodec(charsetName);
	}
}
