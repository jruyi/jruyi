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
import org.jruyi.io.AbstractCodec;
import org.jruyi.io.ICodec;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;

public final class StringCodec extends AbstractCodec<String> {

	public static final ICodec<String> UTF_8 = new StringCodec(
			CharsetCodec.UTF_8);
	public static final ICodec<String> UTF_16 = new StringCodec(
			CharsetCodec.UTF_16);
	public static final ICodec<String> UTF_16LE = new StringCodec(
			CharsetCodec.UTF_16LE);
	public static final ICodec<String> UTF_16BE = new StringCodec(
			CharsetCodec.UTF_16BE);
	public static final ICodec<String> US_ASCII = new StringCodec(
			CharsetCodec.US_ASCII);
	public static final ICodec<String> ISO_8859_1 = new StringCodec(
			CharsetCodec.ISO_8859_1);

	private final String m_charsetName;

	public StringCodec(String charsetName) {
		m_charsetName = charsetName;
	}

	@Override
	public String read(IUnitChain unitChain) {
		final ByteBufferArray bba = ByteBufferArray.get();
		try {
			IUnit unit = unitChain.currentUnit();
			bba.add(unit.getByteBufferForRead(unit.position(), unit.remaining()));
			unit.position(unit.size());
			while ((unit = unitChain.nextUnit()) != null) {
				bba.add(unit.getByteBufferForRead(unit.position(),
						unit.remaining()));
				unit.position(unit.size());
			}
			final ICharsetCodec cc = CharsetCodec.get(m_charsetName);
			return cc.toString(bba.array(), 0, bba.size());
		} finally {
			bba.clear();
		}
	}

	@Override
	public String read(IUnitChain unitChain, int length) {
		if (length < 0)
			throw new IllegalArgumentException();

		if (length == 0)
			return "";

		final ByteBufferArray bba = ByteBufferArray.get();
		try {
			IUnit unit = unitChain.currentUnit();
			bba.add(unit.getByteBufferForRead(unit.position(), length));
			while ((length -= unit.skip(length)) > 0) {
				unit = unitChain.nextUnit();
				bba.add(unit.getByteBufferForRead(unit.position(), length));
			}
			final ICharsetCodec cc = CharsetCodec.get(m_charsetName);
			return cc.toString(bba.array(), 0, bba.size());
		} finally {
			bba.clear();
		}
	}

	@Override
	public void write(String str, IUnitChain unitChain) {
		final ICharsetCodec cc = CharsetCodec.get(m_charsetName);
		Helper.write(cc, CharBuffer.wrap(str), unitChain);
	}

	@Override
	public void write(String str, int offset, int length, IUnitChain unitChain) {
		final ICharsetCodec cc = CharsetCodec.get(m_charsetName);
		Helper.write(cc, CharBuffer.wrap(str, offset, offset + length),
				unitChain);
	}

	@Override
	public String get(IUnitChain unitChain, int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException();
		final ByteBufferArray bba = ByteBufferArray.get();
		try {
			IUnit unit = unitChain.currentUnit();
			bba.add(unit.getByteBufferForRead(index, unit.size() - index));
			while ((unit = unitChain.nextUnit()) != null)
				bba.add(unit.getByteBufferForRead(0, unit.size()));
			final ICharsetCodec cc = CharsetCodec.get(m_charsetName);
			return cc.toString(bba.array(), 0, bba.size());
		} finally {
			bba.clear();
		}
	}

	@Override
	public String get(IUnitChain unitChain, int index, int length) {
		if (index < 0 || length < 0)
			throw new IndexOutOfBoundsException();
		if (length == 0)
			return "";
		final ByteBufferArray bba = ByteBufferArray.get();
		try {
			IUnit unit = unitChain.currentUnit();
			bba.add(unit.getByteBufferForRead(index, length));
			length -= (unit.size() - index);
			while (length > 0) {
				unit = unitChain.nextUnit();
				bba.add(unit.getByteBufferForRead(0, length));
				length -= unit.size();
			}
			final ICharsetCodec cc = CharsetCodec.get(m_charsetName);
			return cc.toString(bba.array(), 0, bba.size());
		} finally {
			bba.clear();
		}
	}

	@Override
	public void prepend(String str, IUnitChain unitChain) {
		final ICharsetCodec cc = CharsetCodec.get(m_charsetName);
		Helper.prepend(cc, CharBuffer.wrap(str), unitChain);
	}

	@Override
	public void prepend(String str, int offset, int length, IUnitChain unitChain) {
		final ICharsetCodec cc = CharsetCodec.get(m_charsetName);
		Helper.prepend(cc, CharBuffer.wrap(str, offset, offset + length),
				unitChain);
	}
}
