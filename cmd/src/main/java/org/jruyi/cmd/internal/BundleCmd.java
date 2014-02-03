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

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;

import org.jruyi.cmd.util.Util;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;

public final class BundleCmd {

	private final BundleContext m_context;

	public static String[] commands() {
		return new String[] { "list", "inspect", "start", "stop", "install",
				"uninstall", "update", "refresh" };
	}

	BundleCmd(BundleContext context) {
		m_context = context;
	}

	public void list() {
		Bundle[] bundles = m_context.getBundles();
		FrameworkStartLevel fsl = m_context.getBundle(0).adapt(
				FrameworkStartLevel.class);
		System.out.print("START LEVEL ");
		System.out.println(fsl.getStartLevel());
		System.out.println("[ ID ][  State  ][Level][     Name     ]");
		for (Bundle bundle : bundles) {
			// ID
			System.out.print(' ');
			String s = String.valueOf(bundle.getBundleId());
			int n = 4 - s.length();
			if (n > 0)
				Util.printFill(' ', n);
			System.out.print(s);
			System.out.print("  ");

			// State
			s = bundleStateName(bundle.getState());
			n = 9 - s.length();
			if (n > 0)
				Util.printFill(' ', n);
			System.out.print(s);
			System.out.print("   ");

			// Start Level
			n = bundle.adapt(BundleStartLevel.class).getStartLevel();
			if (n < 10)
				System.out.print(' ');

			System.out.print(n);
			System.out.print("    ");

			// Name & Version
			System.out.print(bundle.getSymbolicName());
			System.out.print('-');
			System.out.println(bundle.getVersion().toString());
		}
	}

	public void inspect(String bundleId) {
		Bundle bundle = m_context.getBundle(Long.parseLong(bundleId));
		Dictionary<String, String> headers = bundle.getHeaders();

		System.out.print("     Bundle ID: ");
		System.out.println(bundle.getBundleId());

		System.out.print(" Symbolic Name: ");
		System.out.println(bundle.getSymbolicName());

		System.out.print("       Version: ");
		System.out.println(bundle.getVersion().toString());

		System.out.print("   Bundle Name: ");
		System.out.println(normalize(headers.get("Bundle-Name")));

		System.out.print("   Description: ");
		System.out.println(normalize(headers.get("Bundle-Description")));

		System.out.print(" Bundle Vendor: ");
		System.out.println(normalize(headers.get("Bundle-Vendor")));

		System.out.print("Bundle License: ");
		System.out.println(normalize(headers.get("Bundle-License")));

		System.out.print(" Last Modified: ");
		System.out.println(bundle.getLastModified());

		System.out.print("         State: ");
		System.out.println(bundleStateName(bundle.getState()));

		System.out.print("   Start Level: ");
		System.out
				.println(bundle.adapt(BundleStartLevel.class).getStartLevel());

		System.out.print("Export-Package: ");
		System.out.println(normalize(headers.get("Export-Package")));

		System.out.print("Import-Package: ");
		System.out.println(normalize(headers.get("Import-Package")));

		System.out.print("Offer Services: ");
		printServiceReferences(bundle.getRegisteredServices());
		System.out.print("  Use Services: ");
		printServiceReferences(bundle.getServicesInUse());

		System.out.print("      Location: ");
		System.out.println(bundle.getLocation());
	}

	public void start(String[] bundles) throws Exception {
		if (bundles == null || bundles.length == 0) {
			RuyiCmd.INST.help("bundle:start");
			return;
		}

		BundleContext context = m_context;
		for (String s : bundles) {
			Bundle bundle = Util.isBundleId(s) ? context.getBundle(Long
					.parseLong(s)) : context.installBundle(s);
			if (bundle == null) {
				System.err.print("Failed to start bundle: ");
				System.err.println(s);
				return;
			}

			bundle.start();
		}
	}

	public void stop(String[] bundles) throws Exception {
		if (bundles == null || bundles.length == 0) {
			RuyiCmd.INST.help("bundle:stop");
			return;
		}

		BundleContext context = m_context;
		for (String s : bundles) {
			Bundle bundle;
			if (Util.isBundleId(s))
				bundle = context.getBundle(Long.parseLong(s));
			else
				bundle = context.getBundle(s);

			if (bundle == null) {
				System.err.print("Bundle Not Found: ");
				System.err.println(s);
				return;
			}

			bundle.stop();
		}
	}

	public void install(String[] urls) throws Exception {
		if (urls == null || urls.length == 0) {
			RuyiCmd.INST.help("bundle:install");
			return;
		}

		BundleContext context = m_context;
		for (String url : urls)
			context.installBundle(url);
	}

	public void uninstall(String[] bundles) throws Exception {
		if (bundles == null || bundles.length == 0) {
			RuyiCmd.INST.help("bundle:uninstall");
			return;
		}

		BundleContext context = m_context;
		for (String s : bundles) {
			Bundle bundle;
			if (Util.isBundleId(s))
				bundle = context.getBundle(Long.parseLong(s));
			else
				bundle = context.getBundle(s);

			if (bundle != null)
				bundle.uninstall();
			else {
				System.err.print("Bundle Not Found: ");
				System.err.println(s);
			}
		}
	}

	public void update(String[] bundles) throws Exception {
		BundleContext context = m_context;
		if (bundles == null || bundles.length < 1) {
			RuyiCmd.INST.help("bundle:update");
			return;
		}

		for (String s : bundles) {
			Bundle bundle;
			if (Util.isBundleId(s))
				bundle = context.getBundle(Long.parseLong(s));
			else
				bundle = context.getBundle(s);

			if (bundle != null)
				bundle.update();
			else {
				System.err.print("Bundle Not Found: ");
				System.err.println(s);
			}
		}
	}

	public void update(String bundleId, String arg) throws Exception {
		Bundle bundle = m_context.getBundle(Long.parseLong(bundleId));
		if (bundle == null) {
			System.err.print("Bundle Not Found: ");
			System.err.println(bundleId);
			return;
		}

		if (Util.isBundleId(arg)) {
			bundle.update();
			bundle = m_context.getBundle(Long.parseLong(arg));
			if (bundle == null) {
				System.err.print("Bundle Not Found: ");
				System.err.println(arg);
				return;
			}
			bundle.update();
			return;
		}

		InputStream in = new URL(arg).openStream();
		try {
			bundle.update(in);
		} finally {
			in.close();
		}
	}

	public void refresh(String[] bundleIds) throws Exception {
		BundleContext context = m_context;
		Collection<Bundle> bundles = null;
		if (bundleIds != null && bundleIds.length > 0) {
			bundles = new ArrayList<Bundle>(bundleIds.length);
			for (String s : bundleIds) {
				Bundle bundle;
				if (Util.isBundleId(s))
					bundle = context.getBundle(Long.parseLong(s));
				else
					bundle = context.getBundle(s);
				if (bundle != null)
					bundles.add(bundle);
				else {
					System.err.print("Bundle Not Found: ");
					System.err.println(s);
					return;
				}
			}
		}

		context.getBundle(0).adapt(FrameworkWiring.class)
				.refreshBundles(bundles);
	}

	private static String normalize(String s) {
		return s == null ? "" : s;
	}

	private static void printServiceReferences(ServiceReference<?>[] references) {
		if (references != null) {
			System.out.print(references[0].toString());
			for (int i = 1, n = references.length; i < n; ++i) {
				System.out.print(", ");
				System.out.print(references[i].toString());
			}
		}
		System.out.println();
	}

	private static String bundleStateName(int state) {
		switch (state) {
		case Bundle.ACTIVE:
			return "Active";
		case Bundle.INSTALLED:
			return "Installed";
		case Bundle.RESOLVED:
			return "Resolved";
		case Bundle.STARTING:
			return "Starting";
		case Bundle.STOPPING:
			return "Stopping";
		}

		return "Unknown";
	}
}
