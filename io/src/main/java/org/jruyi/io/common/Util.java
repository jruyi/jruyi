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

package org.jruyi.io.common;

import java.util.Map;

import org.jruyi.common.StringBuilder;
import org.jruyi.io.IoConstants;

public final class Util {

	private Util() {
	}

	public static int max(int m1, int m2) {
		return m1 > m2 ? m1 : m2;
	}

	public static String genServiceId(Map<String, ?> properties, String addr, Integer port, String type) {
		try (StringBuilder builder = StringBuilder.get(64)) {
			builder.append(type).append('(');
			String id = (String) properties.get(IoConstants.SERVICE_ID);
			if (id != null && !(id = id.trim()).isEmpty())
				builder.append(id);
			else {
				if (addr != null) {
					if (addr.indexOf(':') >= 0)
						builder.append('[').append(addr).append(']');
					else
						builder.append(addr);
				}
				builder.append(':').append(port);
			}
			return builder.append(')').toString();
		}
	}

	public static int ceilingNextPowerOfTwo(int x) {
		return 1 << (32 - Integer.numberOfLeadingZeros(x - 1));
	}
}
