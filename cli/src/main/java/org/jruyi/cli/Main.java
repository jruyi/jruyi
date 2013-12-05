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
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import jline.TerminalFactory;
import jline.console.ConsoleReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

public final class Main {

	public static final Main INST = new Main();
	private static final int BUFFER_LEN = 1024 * 8;
	private final Session m_session = new Session();
	private String m_host = "localhost";
	private int m_port = 6060;
	private int m_timeout;
	private int m_status;

	static final class JarFileFilter implements FilenameFilter {

		private static String[] LIBS = { "commons-cli", "jline" };

		@Override
		public boolean accept(File dir, String name) {
			for (String lib : LIBS) {
				if (name.startsWith(lib) && name.endsWith(".jar"))
					return true;
			}
			return false;
		}
	}

	static final class ShutdownHook extends Thread {

		ShutdownHook() {
			super("JRuyi-CLI Shutdown Hook");
		}

		@Override
		public void run() {
			try {
				INST.shutdown();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	private Main() {
	}

	public static void main(String[] args) {
		try {
			init();

			if (args.length > 0 && !INST.processCommandLines(args)) {
				System.exit(INST.m_status);
				return;
			}
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
			return;
		}

		try {
			Runtime.getRuntime().addShutdownHook(new ShutdownHook());

			INST.start();
		} catch (InterruptedException e) {
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			INST.shutdown();
		}
	}

	void shutdown() {
		m_session.close();
	}

	private static void init() throws Throwable {
		ClassLoader classLoader = Main.class.getClassLoader();
		Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL",
				URL.class);
		boolean accessible = addUrl.isAccessible();
		if (!accessible)
			addUrl.setAccessible(true);

		File[] jars = getLibJars();
		for (File jar : jars)
			addUrl.invoke(classLoader, jar.getCanonicalFile().toURI().toURL());

		if (!accessible)
			addUrl.setAccessible(false);
	}

	private static File[] getLibJars() throws Throwable {
		File homeDir;
		String temp = System.getProperty("jruyi.home.dir");
		if (temp == null) {
			String classpath = System.getProperty("java.class.path");
			int index = classpath.toLowerCase().indexOf("jruyi-cli");
			int start = classpath.lastIndexOf(File.pathSeparator, index) + 1;
			if (index >= start) {
				temp = classpath.substring(start, index);
				homeDir = new File(temp).getCanonicalFile().getParentFile();
			} else
				// use current dir
				homeDir = new File(System.getProperty("user.dir"));
		} else
			homeDir = new File(temp);

		homeDir = homeDir.getCanonicalFile();
		return new File(homeDir, "lib").listFiles(new JarFileFilter());
	}

	private void start() throws Throwable {
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

			int i = welcome.lastIndexOf('\n') + 1;
			String prompt = welcome.substring(i);
			welcome = welcome.substring(0, i);

			reader.setPrompt(prompt);

			Writer writer = reader.getOutput();
			writer.write(welcome);

			String cmdLine;
			do {
				cmdLine = reader.readLine();
				if (cmdLine == null || cmdLine.equalsIgnoreCase("quit")
						|| cmdLine.equalsIgnoreCase("exit"))
					break;
			} while (session.send(cmdLine, writer));
		} finally {
			TerminalFactory.get().restore();
		}
	}

	// Exit if false is returned.
	private boolean processCommandLines(String[] args) throws Throwable {

		Options options = new Options();
		options.addOption("?", "help", false, null);
		options.addOption("h", "host", true, null);
		options.addOption("p", "port", true, null);
		options.addOption("t", "timeout", true, null);
		options.addOption("f", "file", false, null);

		CommandLine line = new PosixParser().parse(options, args);

		Option[] opts = line.getOptions();
		for (Option option : opts) {
			String opt = option.getOpt();
			if (opt.equals("?")) {
				printHelp();
				return false;
			} else if (opt.equals("h")) {
				String v = option.getValue();
				if (v != null)
					m_host = v;
			} else if (opt.equals("p")) {
				String v = option.getValue();
				if (v != null)
					m_port = Integer.parseInt(v);
			} else if (opt.equals("t")) {
				String v = option.getValue();
				if (v != null)
					m_timeout = Integer.parseInt(v) * 1000;
			} else if (opt.equals("f")) {
				args = line.getArgs();
				if (args == null || args.length < 1)
					System.out.println("Please specify SCRIPT.");
				else
					run(args);

				return false;
			} else
				throw new Exception("Unknown option: " + option);
		}

		args = line.getArgs();
		if (args == null || args.length < 1)
			return true;

		String command = args[0];
		int n = args.length;
		if (n > 1) {
			StringBuilder builder = new StringBuilder(256);
			builder.append(command);
			for (int i = 1; i < n; ++i)
				builder.append(' ').append(args[i]);
			command = builder.toString();
		}

		run(command);
		return false;
	}

	private void printHelp() {
		String programName = System.getProperty("program.name");
		System.out.println();
		System.out.println("Usage:");
		System.out.println("    " + programName
				+ " [options] [COMMAND | SCRIPT ...]");
		System.out.println();
		System.out.println("options:");
		System.out
				.println("    -?, --help                print this help message");
		System.out
				.println("    -h, --host=<host_name>    the remote host to connect");
		System.out
				.println("    -p, --port=<port_num>     the remote port to connect");
		System.out
				.println("    -t, --timeout=<seconds>   the time to wait for response");
		System.out
				.println("    -f, --file                execute ruyi script file");
		System.out.println();
	}

	private void run(String command) throws Throwable {
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

	private void run(String[] scripts) throws Throwable {
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
}
