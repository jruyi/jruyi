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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import org.jruyi.common.BytesBuilder;
import org.jruyi.common.IByteSequence;
import org.jruyi.common.ICharsetCodec;
import org.jruyi.common.StringBuilder;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;

public final class Helper {

	public static final char[] EMPTY_CHARS = new char[0];

	private Helper() {
	}

	static void write(ICharsetCodec cc, CharBuffer cb, IUnitChain unitChain) {
		final CharsetEncoder encoder = cc.getEncoder();
		try {
			IUnit unit = Util.lastUnit(unitChain);
			ByteBuffer bb = unit.getByteBufferForWrite();

			boolean flush = false;
			encoder.reset();
			for (;;) {
				CoderResult cr = flush ? encoder.flush(bb) : encoder.encode(cb,
						bb, true);
				unit.size(bb.position() - unit.start());
				if (cr.isOverflow()) {
					unit = Util.appendNewUnit(unitChain);
					bb = unit.getByteBufferForWrite();
					continue;
				}

				if (!cr.isUnderflow())
					cr.throwException();

				if (flush)
					break;
				else
					flush = true;
			}
		} catch (CharacterCodingException e) {
			throw new RuntimeException(e);
		} finally {
			cc.releaseEncoder(encoder);
		}
	}

	static int prepend(IByteSequence src, int offset, int length, IUnit unit) {
		int start = unit.start();
		if (length > start) {
			offset += length - start;
			length = start;
		}
		start -= length;
		unit.set(start, src, offset, offset + length);
		unit.start(start);
		unit.size(unit.size() + length);
		return length;
	}

	static void prepend(ICharsetCodec cc, CharBuffer cb, IUnitChain unitChain) {
		final BytesBuilder bb = BytesBuilder.get();
		try {
			cc.encode(cb, bb);
			int length = bb.length();
			IUnit unit = Util.firstUnit(unitChain);
			while ((length -= prepend(bb, 0, length, unit)) > 0)
				unit = Util.prependNewUnit(unitChain);
		} finally {
			bb.close();
		}
	}

	static void prepend(ICharsetCodec cc, StringBuilder sb, IUnitChain unitChain) {
		final BytesBuilder bb = BytesBuilder.get();
		try {
			cc.encode(sb, bb);
			int length = bb.length();
			IUnit unit = Util.firstUnit(unitChain);
			while ((length -= prepend(bb, 0, length, unit)) > 0)
				unit = Util.prependNewUnit(unitChain);
		} finally {
			bb.close();
		}
	}

	static void prepend(ICharsetCodec cc, StringBuilder sb, int offset,
			int len, IUnitChain unitChain) {
		final BytesBuilder bb = BytesBuilder.get();
		try {
			cc.encode(sb, offset, len, bb);
			int length = bb.length();
			IUnit unit = Util.firstUnit(unitChain);
			while ((length -= prepend(bb, 0, length, unit)) > 0)
				unit = Util.prependNewUnit(unitChain);
		} finally {
			bb.close();
		}
	}
}
