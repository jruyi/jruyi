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
package org.jruyi.cli;

import jline.console.completer.ArgumentCompleter.AbstractArgumentDelimiter;

public final class WhitespaceArgumentDelimiter extends
		AbstractArgumentDelimiter {

	private static final CharSequence FILE_URL_PREFIX = "file:";
	private static final int LEN = FILE_URL_PREFIX.length();

	@Override
	public boolean isDelimiterChar(final CharSequence buffer, final int pos) {
		char c = buffer.charAt(pos);
		if (Character.isWhitespace(c))
			return true;

		int end = pos + 1;
		int start = end - LEN;
		if (start < 0)
			return false;

		return (FILE_URL_PREFIX.equals(buffer.subSequence(start, end)) && (start == 0 || Character
				.isWhitespace(buffer.charAt(start - 1))));
	}
}
