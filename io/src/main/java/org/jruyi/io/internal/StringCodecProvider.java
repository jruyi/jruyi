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

import org.jruyi.io.ICodec;
import org.jruyi.io.StringCodec.IStringCodecProvider;
import org.jruyi.io.buffer.codec.StringCodec;

final class StringCodecProvider implements IStringCodecProvider {

	static final StringCodecProvider INST = new StringCodecProvider();

	private StringCodecProvider() {
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
	public ICodec<String> charset(String charsetName) {
		return new StringCodec(charsetName);
	}
}
