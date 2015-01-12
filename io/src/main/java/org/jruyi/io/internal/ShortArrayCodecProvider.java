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
import org.jruyi.io.ShortArrayCodec;
import org.jruyi.io.buffer.codec.shortarray.BigEndian;
import org.jruyi.io.buffer.codec.shortarray.LittleEndian;

final class ShortArrayCodecProvider implements ShortArrayCodec.IShortArrayCodecProvider {

	static final ShortArrayCodecProvider INST = new ShortArrayCodecProvider();

	private ShortArrayCodecProvider() {
	}

	@Override
	public ICodec<short[]> bigEndian() {
		return BigEndian.INST;
	}

	@Override
	public ICodec<short[]> littleEndian() {
		return LittleEndian.INST;
	}
}
