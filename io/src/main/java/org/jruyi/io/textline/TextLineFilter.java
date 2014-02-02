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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.jruyi.common.CharsetCodec;
import org.jruyi.common.ICharsetCodec;
import org.jruyi.io.Filter;
import org.jruyi.io.IBuffer;
import org.jruyi.io.ISession;
import org.jruyi.io.IoConstants;

@Service
@Component(name = "jruyi.io.textline.filter", createPid = false, specVersion = "1.1.0")
@Property(name = IoConstants.FILTER_ID, value = "jruyi.io.textline.filter")
public final class TextLineFilter extends Filter<Object, Object> {

	private byte[] m_lineTerminator;

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

	protected void activate(Map<String, ?> properties) throws Exception {
		String v = (String) properties.get("charset");
		if (v == null)
			v = CharsetCodec.UTF_8;

		ICharsetCodec codec = CharsetCodec.get(v);

		v = (String) properties.get("lineTerminator");
		LineTerminator lineTerminator = v == null ? LineTerminator.CRLF
				: LineTerminator.valueOf(v);

		m_lineTerminator = codec.encode(lineTerminator.getValue());
	}

	protected void deactivate() {
		m_lineTerminator = null;
	}
}

enum LineTerminator {

	CR(new char[] { '\r' }), LF(new char[] { '\n' }), CRLF(new char[] { '\r',
			'\n' });

	private final char[] m_value;

	LineTerminator(char[] value) {
		m_value = value;
	}

	public final char[] getValue() {
		return m_value;
	}
}
