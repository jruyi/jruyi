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
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;

import jline.console.ConsoleReader;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.FileNameCompleter;
import jline.console.completer.StringsCompleter;

public final class RuyiCli {

	static final RuyiCli INST = new RuyiCli();
	private static final int BUFFER_LEN = 1024 * 8;
	private static final String JRUYI_PREFIX = "jruyi:";
	private Session m_session;
	private String m_host = "localhost";
	private int m_port = 6060;
	private int m_timeout;
	private int m_status;
	private ConsoleReader m_console;

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

	void open() throws Exception {
		m_session = new Session();
		m_console = new ConsoleReader(null, new FileInputStream(
				FileDescriptor.in), System.out, null, "UTF-8");
	}

	void close() {
		if (m_session != null) {
			m_session.close();
			m_session = null;
		}

		if (m_console != null) {
			m_console.shutdown();
			m_console = null;
		}
	}

	void start() throws Throwable {
		Thread thread = null;
		final ConsoleReader reader = m_console;
		final Session session = m_session;
		try {
			session.open(m_host, m_port, m_timeout);

			String welcome;
			StringWriter sw = new StringWriter(512);
			try {
				session.recv(sw);
				welcome = sw.toString();
			} finally {
				sw.close();
			}

			Writer writer = reader.getOutput();
			if (session.isClosed()) {
				writer.write(welcome);
				return;
			}

			int i = welcome.lastIndexOf('\n');
			String prompt = welcome.substring(i + 1);
			int j = welcome.lastIndexOf('\r', i - 1);
			String commandStr = welcome.substring(j + 2, i);
			welcome = welcome.substring(0, j + 2);

			reader.setPrompt(prompt);

			addCompleter(reader, commandStr);

			writer.write(welcome);

			session.console(reader);
			thread = new Thread(session);
			thread.start();

			String cmdLine;
			for (;;) {
				cmdLine = reader.readLine();
				if (cmdLine == null)
					break;
				cmdLine = cmdLine.trim();
				if (cmdLine.equalsIgnoreCase("quit")
						|| cmdLine.equalsIgnoreCase("exit"))
					break;

				if (!session.send(cmdLine))
					break;

				session.await();
			}
		} finally {
			if (thread != null) {
				session.close();
				thread.join(30 * 1000);
			}
		}
	}

	void run(String command) throws Throwable {
		m_status = -1;
		Session session = m_session;
		session.open(m_host, m_port, m_timeout);
		try {
			if (!session.recv(null))
				return;
			Writer writer = m_console.getOutput();
			if (!session.send(command, writer))
				return;
			m_status = session.status();
		} finally {
			session.close();
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

		Writer writer = m_console.getOutput();
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
		HashSet<String> cmdSet = new HashSet<String>();
		int n = JRUYI_PREFIX.length();
		for (String command : commands) {
			if (command.startsWith(JRUYI_PREFIX))
				cmdSet.add(command.substring(n));
		}
		cmdSet.add("quit");
		cmdSet.add("exit");

		reader.addCompleter(new StringsCompleter(commands));
		ArgumentCompleter completer = new ArgumentCompleter(
				new WhitespaceArgumentDelimiter(),
				new StringsCompleter(cmdSet), new FileNameCompleter());
		completer.setStrict(false);
		reader.addCompleter(completer);
	}
}
