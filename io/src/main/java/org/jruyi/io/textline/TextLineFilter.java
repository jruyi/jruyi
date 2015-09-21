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

package org.jruyi.io.textline;

import java.util.Map;

import org.jruyi.common.CharsetCodec;
import org.jruyi.common.ICharsetCodec;
import org.jruyi.common.LineTerminator;
import org.jruyi.io.Filter;
import org.jruyi.io.IBuffer;
import org.jruyi.io.IFilter;
import org.jruyi.io.ISession;
import org.jruyi.io.IoConstants;
import org.osgi.service.component.annotations.Component;

@Component(name = "jruyi.io.textline.filter", //
service = { IFilter.class }, //
property = { IoConstants.FILTER_ID + "=" + "jruyi.io.textline.filter" }, //
xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class TextLineFilter extends Filter<Object, Object> {

	private byte[] m_lineTerminator;

	@Override
	public int msgMinSize() {
		return m_lineTerminator.length;
	}

	@Override
	public int tellBoundary(ISession session, IBuffer in) {
		byte[] lineTerminator = m_lineTerminator;
		int i = in.indexOf(lineTerminator, in.position());
		if (i >= 0) {
			in.rewind();
			return i + lineTerminator.length;
		}

		i = in.remaining() - lineTerminator.length + 1;
		if (i > 0)
			in.skip(i);
		return E_UNDERFLOW;
	}

	public void activate(Map<String, ?> properties) {
		String v = (String) properties.get("charset");
		if (v == null || (v = v.trim()).isEmpty())
			v = CharsetCodec.UTF_8;

		final ICharsetCodec codec = CharsetCodec.get(v);

		v = (String) properties.get("lineTerminator");
		final LineTerminator lineTerminator = v == null || (v = v.trim()).isEmpty() ? LineTerminator.CRLF
				: LineTerminator.valueOf(v);

		m_lineTerminator = codec.toBytes(lineTerminator.getValue());
	}

	public void deactivate() {
		m_lineTerminator = null;
	}
}
