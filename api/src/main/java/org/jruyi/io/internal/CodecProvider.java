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
package org.jruyi.io.internal;

import org.jruyi.io.CharCodec.ICharCodecProvider;
import org.jruyi.io.Codec.ICodecProvider;
import org.jruyi.io.DoubleCodec.IDoubleCodecProvider;
import org.jruyi.io.FloatCodec.IFloatCodecProvider;
import org.jruyi.io.IntCodec.IIntCodecProvider;
import org.jruyi.io.LongCodec.ILongCodecProvider;
import org.jruyi.io.ShortCodec.IShortCodecProvider;

public final class CodecProvider {

	private CodecProvider() {
	}

	public static CodecProvider getInstance() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public ICharCodecProvider getCharCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public IShortCodecProvider getShortCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public IIntCodecProvider getIntCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public ILongCodecProvider getLongCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public IFloatCodecProvider getFloatCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public IDoubleCodecProvider getDoubleCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public ICodecProvider getCodecProvider() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
