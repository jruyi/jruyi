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

import org.jruyi.io.buffer.codec.ByteArrayCodec;

import java.io.IOException;
import java.io.InputStream;

final class BufferInputStream extends InputStream {

	private final Buffer m_buffer;

	public BufferInputStream(Buffer buffer) {
		m_buffer = buffer;
	}

	@Override
	public int available() throws IOException {
		return m_buffer.remaining();
	}

	@Override
	public void close() throws IOException {
		m_buffer.close();
	}

	@Override
	public void mark(int readLimit) {
		m_buffer.mark();
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public int read() throws IOException {
		return m_buffer.readByte();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int n = m_buffer.read(b, off, len, ByteArrayCodec.INST);
		return n == 0 ? -1 : n;
	}

	@Override
	public int read(byte[] b) throws IOException {
		int n = m_buffer.read(b, ByteArrayCodec.INST);
		return n == 0 ? -1 : n;
	}

	@Override
	public void reset() throws IOException {
		m_buffer.reset();
	}

	@Override
	public long skip(long n) throws IOException {
		return m_buffer.skip((int) n);
	}
}
