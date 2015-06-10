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
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public final class Main {

	static final Main INST = new Main();

	static final class JarFileFilter implements FilenameFilter {

		private static final String[] LIBS = { "commons-cli", "jline" };

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
				RuyiCli.INST.close();
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

			RuyiCli.INST.open();

			if (args.length > 0 && !INST.processCommandLines(args)) {
				System.exit(RuyiCli.INST.status());
				return;
			}
		} catch (Throwable t) {
			RuyiCli.INST.close();
			t.printStackTrace();
			System.exit(1);
			return;
		}

		try {
			Runtime.getRuntime().addShutdownHook(new ShutdownHook());
			RuyiCli.INST.start();
		} catch (InterruptedException e) {
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			RuyiCli.INST.close();
		}
	}

	private static void init() throws Throwable {
		ClassLoader classLoader = Main.class.getClassLoader();
		Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
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

	// Exit if false is returned.
	private boolean processCommandLines(String[] args) throws Throwable {

		Options options = new Options();
		options.addOption("?", "help", false, null);
		options.addOption("h", "host", true, null);
		options.addOption("p", "port", true, null);
		options.addOption("t", "timeout", true, null);
		options.addOption("f", "file", false, null);

		CommandLine line = new DefaultParser().parse(options, args);

		Option[] opts = line.getOptions();
		for (Option option : opts) {
			String opt = option.getOpt();
			if (opt.equals("?")) {
				printHelp();
				return false;
			} else if (opt.equals("h")) {
				String v = option.getValue();
				if (v != null)
					RuyiCli.INST.host(v);
			} else if (opt.equals("p")) {
				String v = option.getValue();
				if (v != null)
					RuyiCli.INST.port(Integer.parseInt(v));
			} else if (opt.equals("t")) {
				String v = option.getValue();
				if (v != null)
					RuyiCli.INST.timeout(Integer.parseInt(v) * 1000);
			} else if (opt.equals("f")) {
				args = line.getArgs();
				if (args == null || args.length < 1)
					System.out.println("Please specify SCRIPT.");
				else
					RuyiCli.INST.run(args);

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

		RuyiCli.INST.run(command);
		return false;
	}

	private void printHelp() {
		String programName = System.getProperty("program.name");
		System.out.println();
		System.out.println("Usage: " + programName + " [options] [COMMAND | SCRIPT ...]");
		System.out.println();
		System.out.println("options:");
		System.out.println("    -?, --help                print this help message");
		System.out.println("    -h, --host=<host_name>    the remote host to connect");
		System.out.println("    -p, --port=<port_num>     the remote port to connect");
		System.out.println("    -t, --timeout=<seconds>   the time to wait for response");
		System.out.println("    -f, --file                execute ruyi script file");
		System.out.println();
	}
}
