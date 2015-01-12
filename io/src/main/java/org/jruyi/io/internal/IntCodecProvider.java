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

import org.jruyi.io.IIntCodec;
import org.jruyi.io.IntCodec.IIntCodecProvider;
import org.jruyi.io.buffer.intcodec.BigEndian;
import org.jruyi.io.buffer.intcodec.LittleEndian;
import org.jruyi.io.buffer.intcodec.Varint;

final class IntCodecProvider implements IIntCodecProvider {

	static final IntCodecProvider INST = new IntCodecProvider();

	private IntCodecProvider() {
	}

	@Override
	public IIntCodec bigEndianIntCodec() {
		return BigEndian.INST;
	}

	@Override
	public IIntCodec littleEndianIntCodec() {
		return LittleEndian.INST;
	}

	@Override
	public IIntCodec varintIntCodec() {
		return Varint.INST;
	}
}
