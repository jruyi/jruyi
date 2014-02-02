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
package org.jruyi.io.buffer;

import org.jruyi.io.IDoubleCodec;
import org.jruyi.io.IUnitChain;

public final class BigEndianDoubleCodec implements IDoubleCodec {

	public static final IDoubleCodec INST = new BigEndianDoubleCodec();

	private BigEndianDoubleCodec() {
	}

	@Override
	public double read(IUnitChain unitChain) {
		return Double.longBitsToDouble(BigEndianLongCodec.INST.read(unitChain));
	}

	@Override
	public void write(double d, IUnitChain unitChain) {
		BigEndianLongCodec.INST.write(Double.doubleToRawLongBits(d), unitChain);
	}

	@Override
	public double get(IUnitChain unitChain, int index) {
		return Double.longBitsToDouble(BigEndianLongCodec.INST.get(unitChain,
				index));
	}

	@Override
	public void set(double d, IUnitChain unitChain, int index) {
		BigEndianLongCodec.INST.set(Double.doubleToRawLongBits(d), unitChain,
				index);
	}

	@Override
	public void prepend(double d, IUnitChain unitChain) {
		BigEndianLongCodec.INST.prepend(Double.doubleToRawLongBits(d),
				unitChain);
	}
}
