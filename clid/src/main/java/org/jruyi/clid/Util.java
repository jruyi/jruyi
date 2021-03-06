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

package org.jruyi.clid;

import java.io.InputStream;

import org.apache.felix.service.command.CommandSession;
import org.jruyi.common.IntStack;
import org.jruyi.common.StringBuilder;
import org.osgi.framework.BundleContext;

final class Util {

	static final InputStream DUMMY_INPUT = new InputStream() {
		@Override
		public int read() {
			return 0;
		}
	};

	private Util() {
	}

	public static String filterProps(String target, CommandSession cs, BundleContext context) {
		if (target.length() < 2)
			return target;

		try (final StringBuilder builder = StringBuilder.get(); final IntStack stack = IntStack.get()) {
			final int j = target.length();
			String propValue = null;
			for (int i = 0; i < j; ++i) {
				char c = target.charAt(i);
				switch (c) {
				case '$':
					builder.append(c);
					if (++i < j && (c = target.charAt(i)) == '{')
						stack.push(builder.length() - 1);
					break;
				case '}':
					if (!stack.isEmpty()) {
						int index = stack.pop();
						propValue = getPropValue(builder.substring(index + 2), cs, context);
						if (propValue != null) {
							builder.setLength(index);
							builder.append(propValue);
							continue;
						}
					}
				}

				builder.append(c);
			}

			if (propValue != null || builder.length() != j)
				target = builder.toString();
		}

		return target;
	}

	private static String getPropValue(String name, CommandSession cs, BundleContext context) {
		final int i = name.indexOf(':');
		final String defaultValue;
		if (i >= 0) {
			defaultValue = name.substring(i + 1).trim();
			name = name.substring(0, i).trim();
		} else
			defaultValue = null;

		Object value = cs.get(name);
		if (value != null)
			return value.toString();

		value = context.getProperty(name);
		if (value != null)
			return value.toString();

		value = System.getenv(name);
		return (value == null) ? defaultValue : value.toString();
	}
}
