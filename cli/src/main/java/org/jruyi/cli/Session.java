/**
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
package org.jruyi.cli;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public final class Session {

	private final byte[] m_bytes = new byte[8 * 1024];
	private final char[] m_chars = new char[8 * 1024];
	private final CharBuffer m_cb = CharBuffer.wrap(m_chars);
	private final ByteBuffer m_bb = ByteBuffer.wrap(m_bytes);
	private final CharsetDecoder m_decoder = Charset.forName("UTF-8")
			.newDecoder();
	private Socket m_socket;
	private int m_status;
	private InputStream m_in;
	private OutputStream m_out;

	public void open(String host, int port, int timeout) throws Exception {
		close();
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress(host, port), 10000);
		socket.setSoTimeout(timeout);
		m_socket = socket;
		m_in = new BufferedInputStream(socket.getInputStream());
		m_out = new BufferedOutputStream(socket.getOutputStream());
	}

	public void writeLength(int n) throws Exception {
		byte[] bytes = m_bytes;
		int i = bytes.length - 1;
		bytes[i] = (byte) (n & 0x7F);
		while ((n >>= 7) > 0)
			bytes[--i] = (byte) ((n & 0x7F) | 0x80);

		m_out.write(bytes, i, bytes.length - i);
	}

	public void writeChunk(byte[] chunk, int offset, int length)
			throws Exception {
		OutputStream out = m_out;
		out.write(chunk, offset, length);
	}

	public void flush() throws Exception {
		m_out.flush();
	}

	public boolean recv(Writer writer) throws Exception {
		int len = 0;
		while ((len = readLength()) > 0) {
			if (len < 0) {
				closeOnEof();
				return false;
			}

			InputStream in = m_in;
			ByteBuffer bb = m_bb;
			CharBuffer cb = m_cb;
			CharsetDecoder decoder = m_decoder;
			byte[] bytes = m_bytes;
			char[] chars = m_chars;

			decoder.reset();
			int n = len <= bytes.length ? len : bytes.length;
			int offset = 0;
			while (len > 0) {
				if ((n = in.read(bytes, offset, n)) < 0) {
					closeOnEof();
					return false;
				}

				len -= n;
				if (writer != null) {
					bb.position(0);
					bb.limit(offset + n);
					cb.clear();
					decoder.decode(bb, cb, len < 1);
					writer.write(chars, 0, cb.position());
					writer.flush();

					bb.compact();
					offset = bb.position();
				} else
					offset = 0;

				n = bytes.length - offset;
				if (len < n)
					n = len;
			}

			if (writer != null) {
				cb.clear();
				decoder.flush(cb);
				if ((n = cb.position()) > 0) {
					writer.write(chars, 0, n);
					writer.flush();
				}
			}
		}

		m_status = readStatus();
		return true;
	}

	public boolean send(String msg, Writer writer) throws Exception {
		msg = msg.trim();
		byte[] bytes = msg.getBytes(m_decoder.charset());
		writeLength(bytes.length);
		OutputStream out = m_out;
		out.write(bytes);
		out.flush();

		return recv(writer);
	}

	public int status() {
		return m_status;
	}

	public void close() {
		if (m_socket != null) {
			try {
				m_socket.close();
			} catch (Throwable t) {
			}
			m_socket = null;
		}
		if (m_out != null) {
			try {
				m_out.close();
			} catch (Throwable t) {
			}
			m_out = null;
		}
		if (m_in != null) {
			try {
				m_in.close();
			} catch (Throwable t) {
			}
			m_in = null;
		}
	}

	private int readStatus() throws Exception {
		InputStream in = m_in;
		int b = 0;
		int n = 0;
		for (int i = 0; i < 4; ++i) {
			b = in.read();
			if (b < 0)
				return b;

			n = (n << 8) | b;
		}

		return n;
	}

	private int readLength() throws Exception {
		InputStream in = m_in;
		int b = 0;
		int n = 0;
		do {
			b = in.read();
			if (b < 0)
				return b;

			n = (n << 7) | (b & 0x7F);
		} while (b > 0x7F);

		return n;
	}

	private void closeOnEof() throws Exception {
		close();
		System.err.println();
		System.err.println("Remote peer closed the connection.");
	}
}
