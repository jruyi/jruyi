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

import java.nio.CharBuffer;

import org.jruyi.common.CharsetCodec;
import org.jruyi.common.ICharsetCodec;
import org.jruyi.common.StringBuilder;
import org.jruyi.io.AbstractCodec;
import org.jruyi.io.ICodec;
import org.jruyi.io.IUnitChain;

public final class CharSequenceCodec extends AbstractCodec<CharSequence> {

	public static final ICodec<CharSequence> UTF_8 = new CharSequenceCodec(
			CharsetCodec.UTF_8);
	public static final ICodec<CharSequence> UTF_16 = new CharSequenceCodec(
			CharsetCodec.UTF_16);
	public static final ICodec<CharSequence> UTF_16LE = new CharSequenceCodec(
			CharsetCodec.UTF_16LE);
	public static final ICodec<CharSequence> UTF_16BE = new CharSequenceCodec(
			CharsetCodec.UTF_16BE);
	public static final ICodec<CharSequence> US_ASCII = new CharSequenceCodec(
			CharsetCodec.US_ASCII);
	public static final ICodec<CharSequence> ISO_8859_1 = new CharSequenceCodec(
			CharsetCodec.ISO_8859_1);

	private final String m_charsetName;

	public CharSequenceCodec(String charsetName) {
		m_charsetName = charsetName;
	}

	@Override
	public void write(CharSequence cs, IUnitChain unitChain) {
		ICharsetCodec cc = CharsetCodec.get(m_charsetName);
		Helper.write(cc, CharBuffer.wrap(cs), unitChain);
	}

	@Override
	public void write(CharSequence cs, int offset, int length,
			IUnitChain unitChain) {
		ICharsetCodec cc = CharsetCodec.get(m_charsetName);
		Helper.write(cc, CharBuffer.wrap(cs, offset, offset + length),
				unitChain);
	}

	@Override
	public void prepend(CharSequence cs, IUnitChain unitChain) {
		StringBuilder sb = StringBuilder.get(cs);
		try {
			ICharsetCodec cc = CharsetCodec.get(m_charsetName);
			Helper.prepend(cc, sb, unitChain);
		} finally {
			sb.close();
		}
	}

	@Override
	public void prepend(CharSequence cs, int offset, int length,
			IUnitChain unitChain) {
		StringBuilder sb = StringBuilder.get(length);
		try {
			sb.append(cs, offset, offset + length);
			ICharsetCodec cc = CharsetCodec.get(m_charsetName);
			Helper.prepend(cc, sb, unitChain);
		} finally {
			sb.close();
		}
	}
}
