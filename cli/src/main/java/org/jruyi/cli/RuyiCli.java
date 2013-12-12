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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;

import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.FileNameCompleter;
import jline.console.completer.StringsCompleter;

public final class RuyiCli {

	static final RuyiCli INST = new RuyiCli();
	private static final int BUFFER_LEN = 1024 * 8;
	private final Session m_session = new Session();
	private String m_host = "localhost";
	private int m_port = 6060;
	private int m_timeout;
	private int m_status;

	private RuyiCli() {
	}

	int status() {
		return m_status;
	}

	void host(String host) {
		m_host = host;
	}

	void port(int port) {
		m_port = port;
	}

	void timeout(int timeout) {
		m_timeout = timeout;
	}

	void shutdown() {
		m_session.close();
	}

	void start() throws Throwable {
		ConsoleReader reader = new ConsoleReader();
		try {
			Session session = m_session;
			session.open(m_host, m_port, m_timeout);

			String welcome;
			StringWriter sw = new StringWriter(256);
			try {
				session.recv(sw);
				welcome = sw.toString();
			} finally {
				sw.close();
			}

			int i = welcome.lastIndexOf('\n');
			String prompt = welcome.substring(i + 1);
			int j = welcome.lastIndexOf('\r', i - 1);
			String commandStr = welcome.substring(j + 2, i);
			welcome = welcome.substring(0, j + 2);

			reader.setPrompt(prompt);

			addCompleter(reader, commandStr);

			Writer writer = reader.getOutput();
			writer.write(welcome);

			String cmdLine;
			do {
				cmdLine = reader.readLine();
				if (cmdLine == null)
					break;
				cmdLine = cmdLine.trim();
				if (cmdLine.equalsIgnoreCase("quit")
						|| cmdLine.equalsIgnoreCase("exit"))
					break;
			} while (session.send(cmdLine, writer));
		} finally {
			TerminalFactory.get().restore();
		}
	}

	void run(String command) throws Throwable {
		m_status = -1;
		Session session = m_session;
		Writer writer = null;
		session.open(m_host, m_port, m_timeout);
		try {
			if (!session.recv(null))
				return;
			writer = new OutputStreamWriter(System.out, "UTF-8");
			if (!session.send(command, writer))
				return;
			m_status = session.status();
		} finally {
			session.close();
			if (writer != null)
				writer.close();
		}
	}

	void run(String[] scripts) throws Throwable {
		m_status = -1;
		Session session = m_session;
		session.open(m_host, m_port, m_timeout);
		try {
			if (!session.recv(null))
				return;

			if (scripts == null || scripts.length < 1)
				return;
		} catch (Throwable t) {
			session.close();
			throw t;
		}

		OutputStreamWriter writer = new OutputStreamWriter(System.out, "UTF-8");
		try {
			StringBuilder builder = new StringBuilder(128);
			byte[] buffer = new byte[BUFFER_LEN];
			for (String name : scripts) {
				File script = new File(name);
				if (!script.exists())
					throw new Exception("File Not Found: " + name);

				if (!script.isFile())
					throw new Exception("Invalid script file: " + name);

				int length = (int) script.length();
				if (length < 1)
					continue;

				String preScript = builder.append("'0'='").append(name)
						.append("'").toString();
				builder.setLength(0);
				if (!session.send(preScript, null))
					return;

				session.writeLength(length);
				InputStream in = new FileInputStream(script);
				try {
					int offset = 0;
					for (;;) {
						int n = in.read(buffer, offset, BUFFER_LEN - offset);
						offset += n;
						length -= n;
						if (length < 1) {
							session.writeChunk(buffer, 0, offset);
							break;
						}
						if (offset >= BUFFER_LEN) {
							session.writeChunk(buffer, 0, BUFFER_LEN);
							offset = 0;
						}
					}
				} finally {
					in.close();
				}

				session.flush();
				session.recv(writer);
				if (session.status() != 0)
					break;
			}
			m_status = session.status();
		} finally {
			session.close();
			writer.close();
		}
	}

	private static void addCompleter(ConsoleReader reader, String commandStr) {
		String[] commands = commandStr.split("\n");
		String[] localCommands = new String[] { "quit", "exit" };

		reader.addCompleter(new StringsCompleter(commands));
		ArgumentCompleter completer = new ArgumentCompleter(
				new WhitespaceArgumentDelimiter(), new StringsCompleter(
						localCommands), new FileNameCompleter());
		completer.setStrict(false);
		reader.addCompleter(completer);
	}
}
