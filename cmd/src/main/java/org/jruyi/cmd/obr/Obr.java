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

package org.jruyi.cmd.obr;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Parameter;
import org.jruyi.cmd.internal.RuyiCmd;
import org.jruyi.cmd.util.Util;
import org.jruyi.common.Properties;
import org.jruyi.common.StringBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.util.tracker.ServiceTracker;

public final class Obr extends ServiceTracker<RepositoryAdmin, Object> {

	private static final String[] COMMANDS = { "deploy", "info", "list", "repos" };
	private static final char VERSION_SEPARATOR = '@';
	private ServiceRegistration<?> m_registration;

	public Obr(BundleContext context) {
		super(context, "org.apache.felix.bundlerepository.RepositoryAdmin", null);
	}

	@Override
	public Object addingService(ServiceReference<RepositoryAdmin> reference) {
		if (m_registration == null) {
			final Properties properties = new Properties();
			properties.put(CommandProcessor.COMMAND_SCOPE, "obr");
			properties.put(CommandProcessor.COMMAND_FUNCTION, COMMANDS);
			m_registration = context.registerService(Obr.class.getName(), this, properties);
		}
		return super.addingService(reference);
	}

	@Override
	public void removedService(ServiceReference<RepositoryAdmin> reference, Object service) {
		final ServiceRegistration<?> registration = m_registration;
		if (registration != null) {
			m_registration = null;
			registration.unregister();
		}
		super.removedService(reference, service);
	}

	/**
	 * Manages OSGi bundle repositories
	 * 
	 * @param action
	 *            ( add | list | refresh | remove )
	 * @param args
	 *            space-delimited list of repository URLs
	 * @throws Exception
	 */
	public void repos(String action, String[] args) throws Exception {

		final RepositoryAdmin ra = (RepositoryAdmin) getService();

		int n = args.length;
		if (n > 0) {
			if (action.equals("add")) {
				for (int i = 0; i < n; ++i)
					ra.addRepository(args[i]);
			} else if (action.equals("refresh")) {
				for (int i = 0; i < n; ++i) {
					ra.removeRepository(args[i]);
					ra.addRepository(args[i]);
				}
			} else if (action.equals("remove")) {
				for (int i = 0; i < n; ++i)
					ra.removeRepository(args[i]);
			} else {
				System.err.print("Unknown Repository Action: ");
				System.err.println(action);
			}
		} else if (action.equals("list")) {
			Repository[] repos = ra.listRepositories();
			if (repos == null || (n = repos.length) < 1) {
				System.out.println("No repository URLs are set.");
				return;
			}

			for (int i = 0; i < n; ++i)
				System.out.println(repos[i].getURI());
		} else
			RuyiCmd.INST.help("obr:repos");
	}

	/**
	 * Retrieves resource description from repository
	 * 
	 * @param args
	 *            ( <bundle-name> | <symbolic-name> | <bundle-id> )[@<version>]
	 *            ...
	 * @throws Exception
	 */
	public void info(String[] args) throws Exception {
		if (args == null || args.length == 0) {
			RuyiCmd.INST.help("obr:info");
			return;
		}

		final RepositoryAdmin ra = (RepositoryAdmin) getService();
		for (String targetName : args) {
			// Find the target's bundle resource.
			String targetVersion = null;
			int i = targetName.indexOf(VERSION_SEPARATOR);
			if (i > 0) {
				targetVersion = targetName.substring(i + 1);
				targetName = targetName.substring(0, i);
			}
			final Resource[] resources = searchRepository(ra, targetName, targetVersion);
			if ((resources == null) || (resources.length == 0)) {
				System.err.print("Unknown bundle and/or version: ");
				System.err.println(targetName);
			} else {
				printResource(resources[0]);
				int n = resources.length;
				for (i = 1; i < n; ++i) {
					System.out.println();
					printResource(resources[i]);
				}
			}
		}
	}

	/**
	 * Lists repository resources
	 * 
	 * @param verbose
	 *            names = { "-v", "--verbose" }, presentValue = "true",
	 *            absentValue = "false"
	 * @param args
	 *            Optional strings used for name matching
	 * @throws Exception
	 */
	public void list(
			@Parameter(names = { "-v", "--verbose" }, presentValue = "true", absentValue = "false") boolean verbose,
			String[] args) throws Exception {
		final RepositoryAdmin ra = (RepositoryAdmin) getService();
		final Resource[] resources;
		// Create a filter that will match presentation name or symbolic name.
		try (StringBuilder builder = StringBuilder.get()) {
			if (args == null || args.length < 1)
				builder.append("(|(presentationname=*)(symbolicname=*))");
			else {
				builder.append("(|(presentationname=*");
				append(builder, args);
				builder.append("*)(symbolicname=*");
				append(builder, args);
				builder.append("*))");
			}
			// Use filter to get matching resources.
			resources = ra.discoverResources(builder.toString());
		}

		if (resources == null || resources.length == 0) {
			System.out.println("No matching bundles.");
			return;
		}

		// Group the resources by symbolic name in descending version order,
		// but keep them in overall sorted order by presentation name.
		Arrays.sort(resources, ResourceComparator.INST);

		Resource resource = resources[0];
		String name = resource.getSymbolicName();
		String label = resource.getPresentationName();
		if (label == null)
			System.out.print(name);
		else {
			System.out.print(label);
			if (verbose) {
				System.out.print(" [");
				System.out.print(name);
				System.out.print("]");
			}
		}
		System.out.print(" (");
		System.out.print(resource.getVersion());

		boolean dots = false;
		String previous = name;
		for (int i = 1, n = resources.length; i < n; ++i) {
			resource = resources[i];
			name = resource.getSymbolicName();
			if (name.equals(previous)) {
				if (verbose) {
					System.out.print(", ");
					System.out.print(resource.getVersion());
				} else if (!dots) {
					System.out.print(", ...");
					dots = true;
				}
			} else {
				System.out.println(")");
				previous = name;
				dots = false;
				label = resource.getPresentationName();
				if (label == null)
					System.out.print(name);
				else {
					System.out.print(label);
					if (verbose) {
						System.out.print(" [");
						System.out.print(name);
						System.out.print("]");
					}
				}
				System.out.print(" (");
				System.out.print(resource.getVersion());
			}
		}
		System.out.println(")");
	}

	/**
	 * @param start
	 *            names = { "-s", "--start" }, presentValue = "true",
	 *            absentValue = "false"
	 * @param requiredOnly
	 *            names = { "-ro", "--required-only" }, presentValue = "true",
	 *            absentValue = "false"
	 * @param force
	 *            names = { "-f", "--force" }, presentValue = "true",
	 *            absentValue = "false"
	 * @param args
	 *            ( <bundle-name> | <symbolic-name> | <bundle-id> )[@<version>]
	 *            ...
	 * @throws Exception
	 */
	public void deploy(
			@Parameter(names = { "-s", "--start" }, presentValue = "true", absentValue = "false") boolean start,
			@Parameter(names = { "-ro", "--required-only" }, presentValue = "true", absentValue = "false") boolean requiredOnly,
			@Parameter(names = { "-f", "--force" }, presentValue = "true", absentValue = "false") boolean force,
			String[] args) throws Exception {
		final RepositoryAdmin ra = (RepositoryAdmin) getService();
		final Resolver resolver = ra.resolver();
		if (args != null) {
			for (String arg : args) {
				// Find the target's bundle resource.
				String targetName = arg;
				String targetVersion = null;
				int idx = targetName.indexOf(VERSION_SEPARATOR);
				if (idx > 0) {
					targetName = arg.substring(0, idx);
					targetVersion = arg.substring(idx + 1);
				}
				Resource resource = selectNewestVersion(searchRepository(ra, targetName, targetVersion));
				if (resource != null)
					resolver.add(resource);
				else {
					System.err.print("Unknown bundle - ");
					System.err.println(arg);
				}
			}
		}

		Resource[] resources = resolver.getAddedResources();
		if (resources == null || resources.length == 0)
			return;

		if (resolver.resolve()) {
			System.out.println("Target resource(s):");
			printUnderlineString(19);
			printResources(resources);
			HashSet<String> bundlesToUninstall = null;
			resources = resolver.getRequiredResources();
			if (resources != null && resources.length > 0) {
				System.out.println();
				System.out.println("Required resource(s):");
				printUnderlineString(21);
				printResources(resources);
				if (force)
					bundlesToUninstall = addToUninstall(bundlesToUninstall, resources);
			}
			if (!requiredOnly) {
				resources = resolver.getOptionalResources();
				if (resources != null && resources.length > 0) {
					System.out.println();
					System.out.println("Optional resource(s):");
					printUnderlineString(21);
					printResources(resources);
					if (force)
						bundlesToUninstall = addToUninstall(bundlesToUninstall, resources);
				}
			}

			if (bundlesToUninstall != null)
				uninstall(bundlesToUninstall);

			System.out.println();
			System.out.println("Deploying...");
			int options = 0;
			if (start)
				options |= Resolver.START;

			if (requiredOnly)
				options |= Resolver.NO_OPTIONAL_RESOURCES;

			resolver.deploy(options);
			System.out.println("Done.");
			return;
		}

		final Reason[] reasons = resolver.getUnsatisfiedRequirements();
		if (reasons == null || reasons.length == 0) {
			System.out.println("Could not resolve targets.");
			return;
		}

		System.out.println("Unsatisfied requirement(s):");
		printUnderlineString(27);
		for (Reason reason : reasons) {
			System.out.print("   ");
			System.out.println(reason.getRequirement().getFilter());
			System.out.print("      ");
			System.out.println(reason.getResource().getPresentationName());
		}
	}

	private static void append(StringBuilder builder, String[] args) {
		builder.append(args[0]);
		for (int i = 1, n = args.length; i < n; ++i)
			builder.append(' ').append(args[i]);
	}

	private Resource[] searchRepository(RepositoryAdmin ra, String targetId, String targetVersion) throws Exception {
		// Try to see if the targetId is a bundle ID.
		if (Util.isBundleId(targetId)) {
			final Bundle bundle = context.getBundle(Long.parseLong(targetId));
			if (bundle == null)
				return null;

			targetId = bundle.getSymbolicName();
		}

		// The targetId may be a bundle name or a bundle symbolic name,
		// so create the appropriate LDAP query.
		try (StringBuilder builder = StringBuilder.get()) {
			if (targetVersion != null)
				builder.append("(&");

			builder.append("(|(presentationname=");
			builder.append(targetId);
			builder.append(")(symbolicname=");
			builder.append(targetId);
			builder.append("))");

			if (targetVersion != null) {
				builder.append("(version=");
				builder.append(targetVersion);
				builder.append("))");
			}

			return ra.discoverResources(builder.toString());
		}
	}

	private static void printResource(Resource resource) {
		int n = resource.getPresentationName().length();
		printUnderlineString(n);
		System.out.println(resource.getPresentationName());
		printUnderlineString(n);

		final Map<?, ?> map = resource.getProperties();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			System.out.print(entry.getKey());
			System.out.println(":");
			Object value = entry.getValue();
			if (value.getClass().isArray()) {
				for (int i = 0; i < Array.getLength(value); ++i) {
					System.out.print("   ");
					System.out.println(Array.get(value, i));
				}
			} else
				System.out.println(value);
		}

		final Requirement[] reqs = resource.getRequirements();
		if (reqs != null && reqs.length > 0) {
			System.out.println("Requires:");
			n = reqs.length;
			for (int i = 0; i < n; ++i) {
				System.out.print("   ");
				System.out.println(reqs[i].getFilter());
			}
		}

		final Capability[] caps = resource.getCapabilities();
		if (caps != null && caps.length > 0) {
			System.out.println("Capabilities:");
			n = caps.length;
			for (int i = 0; i < n; ++i) {
				System.out.print("   ");
				System.out.println(caps[i].getPropertiesAsMap());
			}
		}
	}

	private static void printUnderlineString(int len) {
		for (int i = 0; i < len; ++i)
			System.out.print('-');
		System.out.println();
	}

	private static Resource selectNewestVersion(Resource[] resources) {
		if (resources == null || resources.length == 0)
			return null;

		Resource r = resources[0];
		Version v = r.getVersion();
		for (int i = 1, n = resources.length; i < n; ++i) {
			final Resource resource = resources[i];
			final Version vtmp = resource.getVersion();
			if (vtmp.compareTo(v) > 0) {
				r = resource;
				v = vtmp;
			}
		}

		return r;
	}

	private static void printResources(Resource[] resources) {
		for (final Resource resource : resources) {
			System.out.print("   ");
			System.out.print(resource.getPresentationName());
			System.out.print(" (");
			System.out.print(resource.getVersion());
			System.out.println(")");
		}
	}

	private static HashSet<String> addToUninstall(HashSet<String> bundlesToUninstall, Resource[] resources) {
		if (bundlesToUninstall == null)
			bundlesToUninstall = new HashSet<>();
		for (Resource resource : resources)
			bundlesToUninstall.add(resource.getSymbolicName());
		return bundlesToUninstall;
	}

	private void uninstall(HashSet<String> bundlesToUninstall) {
		final Bundle[] bundles = context.getBundles();
		if (bundles == null)
			return;

		for (final Bundle bundle : bundles) {
			if (bundlesToUninstall.contains(bundle.getSymbolicName())) {
				try {
					bundle.uninstall();
				} catch (Exception e) {
					System.err.print("Failed to uninstall: ");
					System.err.println(bundle.toString());
					System.err.println(e.toString());
				}
			}
		}
	}
}
