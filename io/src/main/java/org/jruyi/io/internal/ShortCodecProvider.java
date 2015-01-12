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

import org.jruyi.io.IShortCodec;
import org.jruyi.io.ShortCodec.IShortCodecProvider;
import org.jruyi.io.buffer.shortcodec.BigEndian;
import org.jruyi.io.buffer.shortcodec.LittleEndian;
import org.jruyi.io.buffer.shortcodec.Varint;

final class ShortCodecProvider implements IShortCodecProvider {

	static final ShortCodecProvider INST = new ShortCodecProvider();

	private ShortCodecProvider() {
	}

	@Override
	public IShortCodec bigEndianShortCodec() {
		return BigEndian.INST;
	}

	@Override
	public IShortCodec littleEndianShortCodec() {
		return LittleEndian.INST;
	}

	@Override
	public IShortCodec varintShortCodec() {
		return Varint.INST;
	}
}
