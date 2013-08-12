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
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.jruyi.cmd.internal.RuyiCmd;
import org.jruyi.cmd.util.Util;
import org.jruyi.common.StringBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;

@Service(Obr.class)
@Component(name = "jruyi.cmd.obr", policy = ConfigurationPolicy.IGNORE, createPid = false)
@Properties({
		@Property(name = CommandProcessor.COMMAND_SCOPE, value = "obr"),
		@Property(name = CommandProcessor.COMMAND_FUNCTION, value = { "deploy",
				"info", "list", "repos" }) })
@Reference(name = Obr.REPO_ADMIN, referenceInterface = RepositoryAdmin.class, strategy = ReferenceStrategy.LOOKUP)
public final class Obr {

	public static final String REPO_ADMIN = "repositoryAdmin";
	private static final char VERSION_SEPARATOR = '@';
	private ComponentContext m_context;

	@Descriptor("Manage OSGi bundle repositories")
	public void repos(
			@Descriptor("( add | list | refresh | remove )") String action,
			@Descriptor("space-delimited list of repository URLs") String[] args)
			throws Exception {
		RepositoryAdmin ra = (RepositoryAdmin) m_context
				.locateService(REPO_ADMIN);

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

	@Descriptor("Retrieve resource description from repository")
	public void info(
			@Descriptor("( <bundle-name> | <symbolic-name> | <bundle-id> )[@<version>] ...") String[] args)
			throws Exception {
		if (args == null || args.length == 0) {
			RuyiCmd.INST.help("obr:info");
			return;
		}

		RepositoryAdmin ra = (RepositoryAdmin) m_context
				.locateService(REPO_ADMIN);
		for (String targetName : args) {
			// Find the target's bundle resource.
			String targetVersion = null;
			int i = targetName.indexOf(VERSION_SEPARATOR);
			if (i > 0) {
				targetVersion = targetName.substring(i + 1);
				targetName = targetName.substring(0, i);
			}
			Resource[] resources = searchRepository(ra, targetName,
					targetVersion);
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

	@Descriptor("List repository resources")
	public void list(
			@Parameter(names = { "-v", "--verbose" }, presentValue = "true", absentValue = "false") boolean verbose,
			@Descriptor("Optional strings used for name matching") String[] args)
			throws Exception {
		RepositoryAdmin ra = (RepositoryAdmin) m_context
				.locateService(REPO_ADMIN);
		Resource[] resources;
		// Create a filter that will match presentation name or symbolic name.
		StringBuilder builder = StringBuilder.get();
		try {
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
		} finally {
			builder.close();
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

	public void deploy(
			@Parameter(names = { "-s", "--start" }, presentValue = "true", absentValue = "false") boolean start,
			@Parameter(names = { "-ro", "--required-only" }, presentValue = "true", absentValue = "false") boolean requiredOnly,
			@Parameter(names = { "-f", "--force" }, presentValue = "true", absentValue = "false") boolean force,
			@Descriptor("( <bundle-name> | <symbolic-name> | <bundle-id> )[@<version>] ...") String[] args)
			throws Exception {
		RepositoryAdmin ra = (RepositoryAdmin) m_context
				.locateService(REPO_ADMIN);
		Resolver resolver = ra.resolver();
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
				Resource resource = selectNewestVersion(searchRepository(ra,
						targetName, targetVersion));
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
					bundlesToUninstall = addToUninstall(bundlesToUninstall,
							resources);
			}
			if (!requiredOnly) {
				resources = resolver.getOptionalResources();
				if (resources != null && resources.length > 0) {
					System.out.println();
					System.out.println("Optional resource(s):");
					printUnderlineString(21);
					printResources(resources);
					if (force)
						bundlesToUninstall = addToUninstall(bundlesToUninstall,
								resources);
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

		Reason[] reasons = resolver.getUnsatisfiedRequirements();
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

	protected void activate(ComponentContext context) {
		m_context = context;
	}

	protected void deactivate() {
		m_context = null;
	}

	private static void append(StringBuilder builder, String[] args) {
		builder.append(args[0]);
		for (int i = 1, n = args.length; i < n; ++i)
			builder.append(' ').append(args[i]);
	}

	private Resource[] searchRepository(RepositoryAdmin ra, String targetId,
			String targetVersion) throws Exception {
		// Try to see if the targetId is a bundle ID.
		if (Util.isBundleId(targetId)) {
			Bundle bundle = m_context.getBundleContext().getBundle(
					Long.parseLong(targetId));
			if (bundle == null)
				return null;

			targetId = bundle.getSymbolicName();
		}

		// The targetId may be a bundle name or a bundle symbolic name,
		// so create the appropriate LDAP query.
		StringBuilder builder = StringBuilder.get();
		try {
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
		} finally {
			builder.close();
		}
	}

	private static void printResource(Resource resource) {
		int n = resource.getPresentationName().length();
		printUnderlineString(n);
		System.out.println(resource.getPresentationName());
		printUnderlineString(n);

		@SuppressWarnings("unchecked")
		Map<Object, Object> map = resource.getProperties();
		for (Map.Entry<Object, Object> entry : map.entrySet()) {
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

		Requirement[] reqs = resource.getRequirements();
		if (reqs != null && reqs.length > 0) {
			System.out.println("Requires:");
			n = reqs.length;
			for (int i = 0; i < n; ++i) {
				System.out.print("   ");
				System.out.println(reqs[i].getFilter());
			}
		}

		Capability[] caps = resource.getCapabilities();
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
			Resource resource = resources[i];
			Version vtmp = resource.getVersion();
			if (vtmp.compareTo(v) > 0) {
				r = resource;
				v = vtmp;
			}
		}

		return r;
	}

	private static void printResources(Resource[] resources) {
		for (Resource resource : resources) {
			System.out.print("   ");
			System.out.print(resource.getPresentationName());
			System.out.print(" (");
			System.out.print(resource.getVersion());
			System.out.println(")");
		}
	}

	private static HashSet<String> addToUninstall(
			HashSet<String> bundlesToUninstall, Resource[] resources) {
		if (bundlesToUninstall == null)
			bundlesToUninstall = new HashSet<String>();
		for (Resource resource : resources)
			bundlesToUninstall.add(resource.getSymbolicName());
		return bundlesToUninstall;
	}

	private void uninstall(HashSet<String> bundlesToUninstall) {
		Bundle[] bundles = m_context.getBundleContext().getBundles();
		if (bundles == null)
			return;

		for (Bundle bundle : bundles) {
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
