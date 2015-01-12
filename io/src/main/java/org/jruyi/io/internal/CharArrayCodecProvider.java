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

import org.jruyi.io.CharArrayCodec.ICharArrayCodecProvider;
import org.jruyi.io.ICodec;
import org.jruyi.io.buffer.codec.CharArrayCodec;

final class CharArrayCodecProvider implements ICharArrayCodecProvider {

	static final CharArrayCodecProvider INST = new CharArrayCodecProvider();

	private CharArrayCodecProvider() {
	}

	@Override
	public ICodec<char[]> utf_8() {
		return CharArrayCodec.UTF_8;
	}

	@Override
	public ICodec<char[]> utf_16() {
		return CharArrayCodec.UTF_16;
	}

	@Override
	public ICodec<char[]> utf_16le() {
		return CharArrayCodec.UTF_16LE;
	}

	@Override
	public ICodec<char[]> utf_16be() {
		return CharArrayCodec.UTF_16BE;
	}

	@Override
	public ICodec<char[]> iso_8859_1() {
		return CharArrayCodec.ISO_8859_1;
	}

	@Override
	public ICodec<char[]> us_ascii() {
		return CharArrayCodec.US_ASCII;
	}

	@Override
	public ICodec<char[]> charset(String charsetName) {
		return new CharArrayCodec(charsetName);
	}
}
