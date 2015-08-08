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

package org.jruyi.core.internal;

import java.util.HashMap;
import java.util.Map;

import org.jruyi.common.LineTerminator;
import org.jruyi.core.ITextLineFilterBuilder;
import org.jruyi.io.IFilter;
import org.jruyi.io.textline.TextLineFilter;

final class TextLineFilterBuilder implements ITextLineFilterBuilder {

	private Map<String, Object> m_properties = new HashMap<>(2);

	@Override
	public TextLineFilterBuilder charset(String charset) {
		if (charset == null || (charset = charset.trim()).isEmpty())
			throw new IllegalArgumentException("Illegal charset: cannot be null or empty");
		m_properties.put("charset", charset);
		return this;
	}

	@Override
	public TextLineFilterBuilder lineTerminator(LineTerminator lineTerminator) {
		if (lineTerminator == null)
			throw new NullPointerException();
		m_properties.put("lineTerminator", lineTerminator.name());
		return this;
	}

	@Override
	public IFilter<?, ?> build() {
		final TextLineFilter textLineFilter = new TextLineFilter();
		textLineFilter.activate(m_properties);
		return textLineFilter;
	}
}
