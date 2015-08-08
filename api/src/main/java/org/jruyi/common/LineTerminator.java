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

package org.jruyi.common;

/**
 * Enumeration for line terminators.
 *
 * @since 2.2
 */
public enum LineTerminator {

	/**
	 * Represents a newline with CR.
	 */
	CR("\r"), //
	/**
	 * Represents a newline with LF.
	 */
	LF("\n"), //
	/**
	 * Represents a newline with CR followed by LF.
	 */
	CRLF("\r\n"), //
	/**
	 * Represents a newline with LF followed by CR.
	 */
	LFCR("\n\r");

	private final String m_value;

	LineTerminator(String value) {
		m_value = value;
	}

	/**
	 * Returns a character sequence that represents the newline.
	 * 
	 * @return a newline String
	 */
	public final String getValue() {
		return m_value;
	}
}
