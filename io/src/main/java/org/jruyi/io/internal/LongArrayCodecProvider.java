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
import org.jruyi.io.LongArrayCodec.ILongArrayCodecProvider;
import org.jruyi.io.buffer.codec.longarray.BigEndian;
import org.jruyi.io.buffer.codec.longarray.LittleEndian;

final class LongArrayCodecProvider implements ILongArrayCodecProvider {

	static final LongArrayCodecProvider INST = new LongArrayCodecProvider();

	private LongArrayCodecProvider() {
	}

	@Override
	public ICodec<long[]> bigEndian() {
		return BigEndian.INST;
	}

	@Override
	public ICodec<long[]> littleEndian() {
		return LittleEndian.INST;
	}
}