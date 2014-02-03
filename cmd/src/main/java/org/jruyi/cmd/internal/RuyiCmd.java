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
package org.jruyi.cmd.internal;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.service.command.CommandProcessor;
import org.jruyi.cmd.IManual;
import org.jruyi.common.ListNode;
import org.jruyi.common.StrUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public final class RuyiCmd implements IManual {

	public static final RuyiCmd INST = new RuyiCmd();
	private BundleContext m_context;

	private RuyiCmd() {
	}

	public static String[] commands() {
		return new String[] { "echo", "gc", "grep", "help", "shutdown",
				"sysinfo" };
	}

	public void context(BundleContext context) {
		m_context = context;
	}

	public void sysinfo() {

		// Java Runtime
		System.out.print("        Java Runtime: ");
		System.out.print(System.getProperty("java.runtime.name"));
		System.out.print("(build ");
		System.out.print(System.getProperty("java.runtime.version"));
		System.out.println(')');

		// Java VM
		System.out.print("Java Virtual Machine: ");
		System.out.print(System.getProperty("java.vm.name"));
		System.out.print("(build ");
		System.out.print(System.getProperty("java.vm.version"));
		String v = System.getProperty("java.vm.info");
		if (v != null && !v.isEmpty()) {
			System.out.print(", ");
			System.out.print(System.getProperty("java.vm.info"));
		}
		System.out.println(')');

		// OS Name
		System.out.print("             OS Name: ");
		System.out.println(System.getProperty("os.name"));

		// OS Version
		System.out.print("          OS Version: ");
		System.out.println(System.getProperty("os.version"));

		// OS Arch
		System.out.print("             OS Arch: ");
		System.out.println(System.getProperty("os.arch"));

		// Default Locale
		System.out.print("      Default Locale: ");
		System.out.println(Locale.getDefault());

		// Default Charset
		System.out.print("     Default Charset: ");
		System.out.println(Charset.defaultCharset());

		// Default Time Zone
		System.out.print("   Default Time Zone: ");
		System.out.println(TimeZone.getDefault().getID());

		Runtime runtime = Runtime.getRuntime();

		// Available Processors
		System.out.print("Available Processors: ");
		System.out.println(runtime.availableProcessors());

		// Used Memory
		long totalMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();
		System.out.print("         Used Memory: ");
		System.out.println(totalMemory - freeMemory);

		// Free Memory
		System.out.print("         Free Memory: ");
		System.out.println(freeMemory);

		// Total Memory
		System.out.print("        Total Memory: ");
		System.out.println(totalMemory);

		// Max Memory
		System.out.print("          Max Memory: ");
		System.out.println(runtime.maxMemory());
	}

	public void help() throws Exception {
		ServiceReference<?>[] references = m_context.getAllServiceReferences(
				null, "(&(" + CommandProcessor.COMMAND_SCOPE + "=*)(!("
						+ CommandProcessor.COMMAND_SCOPE + "=builtin)))");
		final ListNode<String> head = ListNode.create();
		for (ServiceReference<?> reference : references) {
			String scope = String.valueOf(reference
					.getProperty(CommandProcessor.COMMAND_SCOPE));
			Object v = reference.getProperty(CommandProcessor.COMMAND_FUNCTION);
			if (v instanceof String[]) {
				String[] funcs = (String[]) v;
				for (String func : funcs)
					add(head, StrUtil.join(scope, ":", func));
			} else
				add(head, StrUtil.join(scope, ":", v));
		}

		ListNode<String> node = head.next();
		while (node != null) {
			System.out.println(node.get());
			head.next(node.next());
			node.close();
			node = head.next();
		}

		head.close();
	}

	public void help(String command) throws Exception {
		int i = command.indexOf(':');
		if (i == command.length() - 1) {
			System.err.print("Illegal Command: ");
			System.err.println(command);
			return;
		}

		String scope = "";
		String function = command;
		if (i >= 0) {
			scope = command.substring(0, i);
			function = command.substring(i + 1);
		}

		if (scope.length() < 1)
			scope = "*";

		String filter = StrUtil.join("(&(" + CommandProcessor.COMMAND_SCOPE
				+ "=", scope, ")(" + CommandProcessor.COMMAND_FUNCTION + "=",
				function, "))");

		BundleContext context = m_context;
		ServiceReference<?>[] references = context.getAllServiceReferences(
				null, filter);
		if (references == null || references.length < 1) {
			System.err.print("Command Not Found: ");
			System.err.println(command);
			return;
		}

		ServiceReference<?> reference = references[0];
		scope = (String) reference.getProperty(CommandProcessor.COMMAND_SCOPE);
		Bundle bundle = reference.getBundle();
		URL url = bundle.getEntry(StrUtil.join("/HELP-INF/", scope, "/",
				function));
		if (url == null) {
			if (bundle.equals(context.getBundle()))
				return;
			bundle = context.getBundle();
			url = bundle.getEntry(StrUtil.join("/HELP-INF/", scope, "/",
					function));
			if (url == null)
				return;
		}

		byte[] buffer = new byte[512];
		int n = 0;
		InputStream in = url.openStream();
		try {
			while ((n = in.read(buffer)) > 0)
				System.out.write(buffer, 0, n);
		} finally {
			in.close();
		}
	}

	public void echo(Object[] args) {
		if (args == null || args.length < 1)
			return;

		System.out.print(args[0]);
		int n = args.length;
		for (int i = 1; i < n; ++i) {
			System.out.print(' ');
			System.out.print(args[i]);
		}

		System.out.println();
	}

	public void gc() {
		Runtime.getRuntime().gc();
	}

	/**
	 * @param ignoreCase
	 *            ignore case distinctions, names = { "-i", "--ignore-case" },
	 *            presentValue = "true", absentValue = "false"
	 * @param invertMatch
	 *            select non-matching lines, names = { "-v", "--invert-match" },
	 *            presentValue = "true", absentValue = "false")
	 * @param regex
	 * @throws Exception
	 */
	public void grep(boolean ignoreCase, boolean invertMatch, String regex)
			throws Exception {

		if (ignoreCase)
			regex = StrUtil.join("(?i)", regex);

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher("");
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (matcher.reset(line).find() ^ invertMatch)
					System.out.println(line);
			}
		} finally {
			reader.close();
		}
	}

	public void shutdown() throws Exception {
		m_context.getBundle(0).stop();
	}

	private void add(ListNode<String> node, String cmd) {
		ListNode<String> prev = node;
		while ((node = prev.next()) != null && node.get().compareTo(cmd) < 0)
			prev = node;

		ListNode<String> newNode = ListNode.create();
		newNode.set(cmd);
		prev.next(newNode);
		newNode.next(node);
	}
}
