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

import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;

public final class Util {

	private Util() {
	}

	public static IUnit lastUnit(IUnitChain unitChain) {
		final IUnit unit = unitChain.lastUnit();
		if (unit.appendable())
			return unit;
		return appendNewUnit(unitChain);
	}

	public static IUnit appendNewUnit(IUnitChain unitChain) {
		final IUnit unit = unitChain.create();
		unitChain.append(unit);
		return unit;
	}

	public static IUnit firstUnit(IUnitChain unitChain) {
		final IUnit unit = unitChain.firstUnit();
		if (unit.prependable())
			return unit;
		return prependNewUnit(unitChain);
	}

	public static IUnit prependNewUnit(IUnitChain unitChain) {
		final IUnit unit = unitChain.create();
		unit.start(unit.capacity());
		unitChain.prepend(unit);
		return unit;
	}
}
