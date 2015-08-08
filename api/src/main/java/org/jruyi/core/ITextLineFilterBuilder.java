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

package org.jruyi.core;

import org.jruyi.common.LineTerminator;
import org.jruyi.io.IFilter;

/**
 * A builder to build a textline filter.
 * 
 * @since 2.2
 */
public interface ITextLineFilterBuilder {

	/**
	 * Sets the charset to interpret the line terminator. Default is "UTF-8".
	 * 
	 * @param charset
	 *            the charset to encode the line terminator
	 * @return this builder
	 * @throws IllegalArgumentException
	 *             if the specified {@code charset} is null or empty
	 */
	ITextLineFilterBuilder charset(String charset);

	/**
	 * Sets the line terminator. Default is {@code LineTerminator.CRLF}.
	 *
	 * @param lineTerminator
	 *            the terminator of a text line to set
	 * @return this builder
	 * @throws NullPointerException
	 *             if the specified {@code lineTerminator} is null
	 */
	ITextLineFilterBuilder lineTerminator(LineTerminator lineTerminator);

	/**
	 * Returns a new text line filter.
	 * 
	 * @return a new text line filter
	 */
	IFilter<?, ?> build();
}
