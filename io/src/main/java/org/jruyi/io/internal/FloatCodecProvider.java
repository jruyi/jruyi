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

import org.jruyi.io.FloatCodec.IFloatCodecProvider;
import org.jruyi.io.IFloatCodec;
import org.jruyi.io.buffer.floatcodec.BigEndian;
import org.jruyi.io.buffer.floatcodec.LittleEndian;

final class FloatCodecProvider implements IFloatCodecProvider {

	static final FloatCodecProvider INST = new FloatCodecProvider();

	private FloatCodecProvider() {
	}

	@Override
	public IFloatCodec bigEndianFloatCodec() {
		return BigEndian.INST;
	}

	@Override
	public IFloatCodec littleEndianFloatCodec() {
		return LittleEndian.INST;
	}
}
