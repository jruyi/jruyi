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
package org.jruyi.cli;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import jline.console.ConsoleReader;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.FileNameCompleter;

public final class RuyiCli {

	static final String JRUYI_PREFIX = "jruyi:";
	static final String CMD_HELP = JRUYI_PREFIX + "help";
	static final int WRITER_INIT_SIZE = 2 * 1024;
	static final RuyiCli INST = new RuyiCli();
	private static final int BUFFER_LEN = 1024 * 8;
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
		final ConsoleReader console = new ConsoleReader(null,
				new FileInputStream(FileDescriptor.in), System.out, null,
				"UTF-8");
		console.setPaginationEnabled(true);
		m_console = console;
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
			StringWriter sw = new StringWriter(WRITER_INIT_SIZE);
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

			final int i = welcome.lastIndexOf('\n');
			final String prompt = welcome.substring(i + 1);
			final int j = welcome.lastIndexOf('\r', i - 1);
			final String commandStr = welcome.substring(j + 2, i);
			welcome = welcome.substring(0, j + 2);

			reader.setPrompt(prompt);

			addCompleter(reader, session, commandStr);

			writer.write(welcome);

			session.console(reader);
			thread = new Thread(session);
			thread.start();

			StringBuilder builder = null;
			String cmdLine;
			for (;;) {
				cmdLine = reader.readLine();
				if (cmdLine == null)
					break;

				if (session.isClosed())
					break;

				final int n = cmdLine.length() - 1;
				if (!cmdLine.isEmpty() && cmdLine.charAt(n) == '\\') {
					int m = n;
					while (--m >= 0 && cmdLine.charAt(m) == '\\')
						;
					m = n - m;
					if ((m & 0x01) != 0) {
						reader.setPrompt("> ");
						cmdLine = cmdLine.substring(0, n);
						if (builder == null)
							builder = new StringBuilder(cmdLine);
						else
							builder.append(cmdLine);
						continue;
					}
				}

				if (builder != null) {
					cmdLine = builder.append(cmdLine).toString();
					builder = null;
					reader.setPrompt(prompt);
				}

				cmdLine = cmdLine.trim();
				if (cmdLine.isEmpty())
					continue;

				if (cmdLine.equalsIgnoreCase("quit")
						|| cmdLine.equalsIgnoreCase("exit"))
					break;

				if (cmdLine.equals("help"))
					cmdLine = CMD_HELP;

				boolean help = CMD_HELP.equals(cmdLine);
				if (help) {
					writer = new StringWriter(WRITER_INIT_SIZE);
					session.writer(writer);
				}

				try {
					if (!session.send(cmdLine))
						break;
					session.await();
				} finally {
					session.writer(null);
				}

				if (help)
					printColumns(reader, writer.toString().trim());
			}
		} finally {
			if (thread != null) {
				session.close();
				thread.join(30 * 1000);
			}
		}
	}

	void run(String command) throws Throwable {
		if (command == null || (command = command.trim()).isEmpty())
			return;

		m_status = -1;
		Session session = m_session;
		session.open(m_host, m_port, m_timeout);
		try {
			if (!session.recv(null))
				return;

			if (command.equals("help"))
				command = CMD_HELP;
			boolean help = CMD_HELP.equals(command);

			final Writer writer;
			if (help)
				writer = new StringWriter(WRITER_INIT_SIZE);
			else
				writer = m_console.getOutput();

			if (!session.send(command, writer))
				return;

			if (help)
				printColumns(m_console, writer.toString().trim());

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

	private static void addCompleter(ConsoleReader reader, Session session,
			String commandStr) {
		final CommandCompleter commandCompleter = new CommandCompleter(session,
				commandStr);
		reader.addCompleter(commandCompleter);
		ArgumentCompleter completer = new ArgumentCompleter(
				new WhitespaceArgumentDelimiter(), new FileNameCompleter());
		completer.setStrict(false);
		reader.addCompleter(completer);
	}

	private static void printColumns(ConsoleReader reader, String commandStr)
			throws IOException {
		final List<CharSequence> commandList = new ArrayList<CharSequence>(128);
		final String[] commands = commandStr.trim().split("\n");
		for (String command : commands) {
			command = command.trim();
			if (!command.isEmpty())
				commandList.add(command);
		}

		reader.printColumns(commandList);
		reader.flush();
	}
}
