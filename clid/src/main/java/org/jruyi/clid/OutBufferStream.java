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

import java.io.OutputStream;

import org.jruyi.io.Codec;
import org.jruyi.io.IBuffer;
import org.jruyi.io.ISession;
import org.jruyi.io.ISessionService;
import org.jruyi.io.IntCodec;
import org.jruyi.io.StringCodec;

final class OutBufferStream extends OutputStream {

	private static final byte[] CR = { '\r' };
	private static final byte[] LF = { '\n' };
	private static final int HEAD_RESERVE_SIZE = 4;
	private final ISessionService<IBuffer, IBuffer> m_ss;
	private final ISession m_session;
	private IBuffer m_buffer;
	private volatile boolean m_closed;

	public OutBufferStream(ISessionService<IBuffer, IBuffer> ss, ISession session) {
		m_ss = ss;
		m_session = session;
	}

	public void reset() {
		m_closed = false;
	}

	@Override
	public void close() {
		m_closed = true;
	}

	@Override
	public void flush() {
		final IBuffer buffer;
		synchronized (this) {
			buffer = m_buffer;
			if (buffer == null || buffer.isEmpty())
				return;

			m_buffer = null;

			prependLength(buffer);
		}
		m_ss.write(m_session, buffer);
	}

	public void write(String str) {
		synchronized (this) {
			if (isClosed())
				return;

			buffer().write(str, StringCodec.utf_8());
		}
	}

	@Override
	public void write(byte[] b, int off, int len) {
		synchronized (this) {
			if (isClosed())
				return;

			buffer().write(b, off, len, Codec.byteArray());
		}
	}

	@Override
	public void write(byte[] b) {
		synchronized (this) {
			if (isClosed())
				return;

			buffer().write(b, Codec.byteArray());
		}
	}

	@Override
	public void write(int b) {
		synchronized (this) {
			if (isClosed())
				return;

			buffer().write((byte) b);
		}
	}

	public void writeOut(int status) {
		IBuffer out;
		synchronized (this) {
			if (isClosed())
				return;

			m_closed = true;

			out = detachBuffer();
			if (!out.isEmpty() && !out.endsWith(CR) && !out.endsWith(LF)) {
				out.write(CR[0]);
				out.write(LF[0]);
			}

			if (!out.isEmpty())
				prependLength(out);

			// EOF
			out.write((byte) 0);
			out.write(status, IntCodec.bigEndian());
		}

		m_ss.write(m_session, out);
	}

	public void writeOut(String prompt) {
		IBuffer out;
		synchronized (this) {
			if (isClosed())
				return;

			m_closed = true;

			out = detachBuffer();
			if (!out.isEmpty() && !out.endsWith(CR) && !out.endsWith(LF)) {
				out.write(CR[0]);
				out.write(LF[0]);
			}

			if (!prompt.isEmpty())
				out.write(prompt, StringCodec.utf_8());

			if (!out.isEmpty())
				prependLength(out);

			// EOF
			out.write((byte) 0);
			out.write(0, IntCodec.bigEndian());
		}

		m_ss.write(m_session, out);
	}

	private static void prependLength(IBuffer buffer) {
		int n = buffer.size();
		buffer.prepend((byte) (n & 0x7F));
		while ((n >>= 7) > 0)
			buffer.prepend((byte) ((n & 0x7F) | 0x80));
	}

	boolean isClosed() {
		return m_closed;
	}

	IBuffer buffer() {
		IBuffer buffer = m_buffer;
		if (buffer == null) {
			buffer = m_session.createBuffer();
			buffer.reserveHead(HEAD_RESERVE_SIZE);
			m_buffer = buffer;
		}

		return buffer;
	}

	private IBuffer detachBuffer() {
		IBuffer buffer = m_buffer;
		if (buffer == null) {
			buffer = m_session.createBuffer();
			buffer.reserveHead(HEAD_RESERVE_SIZE);
		} else
			m_buffer = null;

		return buffer;
	}
}
