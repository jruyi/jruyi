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

import org.jruyi.io.CharArrayCodec;
import org.jruyi.io.CharCodec;
import org.jruyi.io.CharSequenceCodec;
import org.jruyi.io.Codec;
import org.jruyi.io.DoubleArrayCodec;
import org.jruyi.io.DoubleCodec;
import org.jruyi.io.FloatArrayCodec;
import org.jruyi.io.FloatCodec;
import org.jruyi.io.IntArrayCodec;
import org.jruyi.io.IntCodec;
import org.jruyi.io.LongArrayCodec;
import org.jruyi.io.LongCodec;
import org.jruyi.io.ShortArrayCodec;
import org.jruyi.io.ShortCodec;
import org.jruyi.io.StringCodec;

public final class CodecProvider {

	private CodecProvider() {
	}

	public static CodecProvider getInstance() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public CharCodec.ICharCodecProvider getCharCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public ShortCodec.IShortCodecProvider getShortCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public IntCodec.IIntCodecProvider getIntCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public LongCodec.ILongCodecProvider getLongCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public FloatCodec.IFloatCodecProvider getFloatCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public DoubleCodec.IDoubleCodecProvider getDoubleCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public StringCodec.IStringCodecProvider getStringCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public CharArrayCodec.ICharArrayCodecProvider getCharArrayCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public CharSequenceCodec.ICharSequenceCodecProvider getCharSequenceCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public LongArrayCodec.ILongArrayCodecProvider getLongArrayCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public IntArrayCodec.IIntArrayCodecProvider getIntArrayCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public ShortArrayCodec.IShortArrayCodecProvider getShortArrayCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public FloatArrayCodec.IFloatArrayCodecProvider getFloatArrayCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public DoubleArrayCodec.IDoubleArrayCodecProvider getDoubleArrayCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public Codec.ICodecProvider getCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
