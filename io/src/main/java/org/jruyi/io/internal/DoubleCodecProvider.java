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

import org.jruyi.io.DoubleCodec.IDoubleCodecProvider;
import org.jruyi.io.IDoubleCodec;
import org.jruyi.io.buffer.doublecodec.BigEndian;
import org.jruyi.io.buffer.doublecodec.LittleEndian;

final class DoubleCodecProvider implements IDoubleCodecProvider {

	static final DoubleCodecProvider INST = new DoubleCodecProvider();

	private DoubleCodecProvider() {
	}

	@Override
	public IDoubleCodec bigEndianDoubleCodec() {
		return BigEndian.INST;
	}

	@Override
	public IDoubleCodec littleEndianDoubleCodec() {
		return LittleEndian.INST;
	}
}
