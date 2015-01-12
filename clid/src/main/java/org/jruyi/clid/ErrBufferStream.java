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

import java.io.IOException;
import java.io.OutputStream;

import org.jruyi.io.Codec;
import org.jruyi.io.IBuffer;
import org.jruyi.io.StringCodec;

final class ErrBufferStream extends OutputStream {

	private static final String RED = "\u001B[31;1m";
	private static final String RESET = "\u001B[0m";
	private final OutBufferStream m_out;

	public ErrBufferStream(OutBufferStream out) {
		m_out = out;
	}

	public OutBufferStream outBufferStream() {
		return m_out;
	}

	public void write(String str) {
		final OutBufferStream out = m_out;
		synchronized (out) {
			if (out.isClosed())
				return;

			IBuffer buffer = out.buffer();
			buffer.write(RED, StringCodec.utf_8());
			buffer.write(str, StringCodec.utf_8());
			buffer.write(RESET, StringCodec.utf_8());
		}
	}

	@Override
	public void close() {
	}

	@Override
	public void flush() {
		m_out.flush();
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		final OutBufferStream out = m_out;
		synchronized (out) {
			if (out.isClosed())
				return;

			IBuffer buffer = out.buffer();
			buffer.write(RED, StringCodec.utf_8());
			buffer.write(b, off, len, Codec.byteArray());
			buffer.write(RESET, StringCodec.utf_8());
		}
	}

	@Override
	public void write(byte[] b) throws IOException {
		final OutBufferStream out = m_out;
		synchronized (out) {
			if (out.isClosed())
				return;

			IBuffer buffer = out.buffer();
			buffer.write(RED, StringCodec.utf_8());
			buffer.write(b, Codec.byteArray());
			buffer.write(RESET, StringCodec.utf_8());
		}
	}

	@Override
	public void write(int b) throws IOException {
		final OutBufferStream out = m_out;
		synchronized (out) {
			if (out.isClosed())
				return;

			IBuffer buffer = out.buffer();
			buffer.write(RED, StringCodec.utf_8());
			buffer.write((byte) b);
			buffer.write(RESET, StringCodec.utf_8());
		}
	}
}
