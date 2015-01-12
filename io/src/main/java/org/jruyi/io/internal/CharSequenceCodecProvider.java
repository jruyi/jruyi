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

import org.jruyi.io.CharSequenceCodec.ICharSequenceCodecProvider;
import org.jruyi.io.ICodec;
import org.jruyi.io.buffer.codec.CharSequenceCodec;

final class CharSequenceCodecProvider implements ICharSequenceCodecProvider {

	static final CharSequenceCodecProvider INST = new CharSequenceCodecProvider();

	private CharSequenceCodecProvider() {
	}

	@Override
	public ICodec<CharSequence> utf_8() {
		return CharSequenceCodec.UTF_8;
	}

	@Override
	public ICodec<CharSequence> utf_16() {
		return CharSequenceCodec.UTF_16;
	}

	@Override
	public ICodec<CharSequence> utf_16le() {
		return CharSequenceCodec.UTF_16LE;
	}

	@Override
	public ICodec<CharSequence> utf_16be() {
		return CharSequenceCodec.UTF_16BE;
	}

	@Override
	public ICodec<CharSequence> iso_8859_1() {
		return CharSequenceCodec.ISO_8859_1;
	}

	@Override
	public ICodec<CharSequence> us_ascii() {
		return CharSequenceCodec.US_ASCII;
	}

	@Override
	public ICodec<CharSequence> charset(String charsetName) {
		return new CharSequenceCodec(charsetName);
	}
}
