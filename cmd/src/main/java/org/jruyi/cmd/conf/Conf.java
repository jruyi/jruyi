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

package org.jruyi.cmd.conf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;

import org.jruyi.cmd.internal.RuyiCmd;
import org.jruyi.common.Properties;
import org.jruyi.common.StrUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

public final class Conf {

	private static final String MASK = "****";

	private static final int PID = 0x01;
	private static final int FACTORYPID = 0x02;
	private static final int BOTH = PID | FACTORYPID;

	private final BundleContext m_context;

	public Conf(BundleContext context) {
		m_context = context;
	}

	public static String[] commands() {
		return new String[] { "create", "delete", "list", "update", "exists" };
	}

	/**
	 * Creates a configuration
	 * 
	 * @param id
	 *            <pid|factoryPid>
	 * @param args
	 *            [name=value] ...
	 * @throws Exception
	 */
	public void create(String id, String[] args) throws Exception {
		Properties props;
		if (args == null || args.length < 1) {
			props = new Properties(1);
		} else {
			props = new Properties(args.length);
			for (String arg : args) {
				int i = arg.indexOf('=');
				if (i < 1 || i > arg.length() - 1)
					continue;

				props.put(arg.substring(0, i).trim(), arg.substring(i + 1).trim());
			}
		}

		final boolean[] factory = new boolean[1];
		final ObjectClassDefinition ocd = getOcd(id, BOTH, factory);
		if (ocd == null)
			throw new Exception("Metatype NOT Found: " + id);

		props = PropUtil.normalize(props, ocd);

		final ConfigurationAdmin ca = (ConfigurationAdmin) ca();
		Configuration conf = factory[0] ? ca.createFactoryConfiguration(id, null) : ca.getConfiguration(id, null);

		conf.update(props);
	}

	/**
	 * Updates configuration(s)
	 * 
	 * @param filter
	 *            <pid | filter>
	 * @param args
	 *            [name[=value]] ...
	 * @throws Exception
	 */
	public void update(String filter, String[] args) throws Exception {
		if (args == null || args.length < 1) {
			RuyiCmd.INST.help("conf:update");
			return;
		}

		final ConfigurationAdmin ca = (ConfigurationAdmin) ca();
		final Configuration[] confs = ca.listConfigurations(normalizeFilter(filter));

		if (confs == null || confs.length < 1)
			throw new Exception("Configuration(s) NOT Found: " + filter);

		final int n = args.length;
		ArrayList<String> removedProps = null;
		final HashMap<String, Object> props = new HashMap<>(n);
		for (String arg : args) {
			int i = arg.indexOf('=');
			if (i < 0) {
				removedProps = getList(removedProps);
				removedProps.add(arg);
			} else
				props.put(arg.substring(0, i).trim(), arg.substring(i + 1).trim());
		}
		final Set<String> keys = props.keySet();

		for (final Configuration conf : confs) {
			final String factoryPid = conf.getFactoryPid();
			final String id;
			final ObjectClassDefinition ocd;
			if (factoryPid == null) {
				id = conf.getPid();
				ocd = getOcd(id, PID, null);
			} else {
				id = factoryPid;
				ocd = getOcd(id, FACTORYPID, null);
			}
			if (ocd == null)
				throw new Exception("Metatype NOT Found: " + id);

			boolean modified = false;
			final Dictionary<String, Object> oldProps = conf.getProperties();
			Properties newProps = new Properties(props);
			for (Enumeration<String> e = oldProps.keys(); e.hasMoreElements();) {
				final String key = (String) e.nextElement();
				if (removedProps == null || !removedProps.contains(key)) {
					if (!props.containsKey(key))
						newProps.put(key, oldProps.get(key));
				} else
					modified = true;
			}
			newProps = PropUtil.normalize(newProps, ocd);

			if (modified)
				conf.update(newProps);
			else {
				for (String key : keys) {
					if (!equals(newProps.get(key), oldProps.get(key))) {
						conf.update(newProps);
						break;
					}
				}
			}
		}
	}

	/**
	 * Deletes configuration(s)
	 * 
	 * @param filter
	 *            pid | filter
	 * @throws Exception
	 */
	public void delete(String filter) throws Exception {
		final ConfigurationAdmin ca = (ConfigurationAdmin) ca();
		final Configuration[] confs = ca.listConfigurations(normalizeFilter(filter));
		if (confs == null || confs.length == 0) {
			System.err.print("Configuration(s) NOT Found: ");
			System.err.println(filter);
			return;
		}

		for (Configuration conf : confs)
			conf.delete();
	}

	/**
	 * Lists configuration(s)
	 * 
	 * @param args
	 *            [pid | filter]
	 * @throws Exception
	 */
	public void list(String[] args) throws Exception {
		String filter = null;
		if (args != null) {
			if (args.length > 1) {
				RuyiCmd.INST.help("conf:list");
				return;
			} else if (args.length > 0)
				filter = normalizeFilter(args[0]);
		}

		final ConfigurationAdmin ca = (ConfigurationAdmin) ca();
		final Configuration[] confs = ca.listConfigurations(filter);
		if (confs == null || confs.length == 0)
			return;

		if (confs.length == 1) {
			inspect(confs[0]);
			return;
		}

		for (Configuration conf : confs) {
			System.out.print("    ");
			System.out.print(conf.getPid());
			System.out.print(": {");
			line(conf);
			System.out.println('}');
		}
	}

	/**
	 * Tests if configuration(s) exists
	 * 
	 * @param filter
	 *            [pid | filter]
	 * @return
	 * @throws Exception
	 */
	public boolean exists(String filter) throws Exception {
		final ConfigurationAdmin ca = (ConfigurationAdmin) ca();
		final Configuration[] confs = ca.listConfigurations(normalizeFilter(filter));
		return (confs != null && confs.length > 0);
	}

	private static String normalizeFilter(String filter) {
		if (filter == null)
			return null;

		filter = filter.trim();
		if (filter.charAt(0) != '(')
			filter = StrUtil.join("(" + Constants.SERVICE_PID + "=", filter, ')');

		return filter;
	}

	private ObjectClassDefinition getOcd(final String id, final int type, boolean[] factory) {

		final MetaTypeService mts = (MetaTypeService) mts();
		final Bundle[] bundles = m_context.getBundles();
		for (Bundle bundle : bundles) {
			MetaTypeInformation mti = mts.getMetaTypeInformation(bundle);
			if (mti == null)
				continue;

			if ((type & FACTORYPID) != 0) {
				String[] factoryPids = mti.getFactoryPids();
				for (String factoryPid : factoryPids) {
					if (factoryPid.equals(id)) {
						if (factory != null)
							factory[0] = true;
						return mti.getObjectClassDefinition(id, null);
					}
				}
			}

			if ((type & PID) != 0) {
				String[] pids = mti.getPids();
				for (String pid : pids) {
					if (pid.equals(id))
						return mti.getObjectClassDefinition(id, null);
				}
			}
		}

		return null;
	}

	private void line(Configuration conf) {
		Dictionary<String, ?> props = conf.getProperties();

		String id = conf.getFactoryPid();
		ObjectClassDefinition ocd = id == null ? getOcd(conf.getPid(), PID, null) : getOcd(id, FACTORYPID, null);
		if (ocd != null) {
			AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
			int n;
			if (ads != null && (n = ads.length) > 0) {
				boolean first = true;
				for (int i = 0; i < n; ++i) {
					final AttributeDefinition ad = ads[i];
					id = ad.getID();
					Object value = props.get(id);
					if (value != null) {
						if (!first)
							System.out.print(", ");
						else
							first = false;
						if (ad.getType() == AttributeDefinition.PASSWORD)
							value = MASK;
						lineProperty(id, value);
					}
				}
				return;
			}
		}

		Enumeration<String> keys = props.keys();
		if (keys.hasMoreElements()) {
			id = keys.nextElement();
			lineProperty(id, props.get(id));
		}

		while (keys.hasMoreElements()) {
			id = keys.nextElement();
			System.out.print(", ");
			lineProperty(id, props.get(id));
		}
	}

	private void inspect(Configuration conf) {
		String pid = conf.getPid();
		String factoryPid = conf.getFactoryPid();
		System.out.print("pid: ");
		System.out.println(pid);

		ObjectClassDefinition ocd;
		if (factoryPid != null) {
			System.out.print("factoryPid: ");
			System.out.println(factoryPid);
			ocd = getOcd(factoryPid, FACTORYPID, null);
		} else
			ocd = getOcd(pid, PID, null);

		System.out.print("bundleLocation: ");
		System.out.println(conf.getBundleLocation());

		System.out.println("properties: ");
		Dictionary<String, ?> props = conf.getProperties();
		if (ocd != null) {
			AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
			if (ads != null) {
				for (AttributeDefinition ad : ads) {
					String id = ad.getID();
					Object value = props.get(id);
					if (value != null) {
						if (ad.getType() == AttributeDefinition.PASSWORD)
							value = MASK;
						inspectProperty(id, value);
					}
				}
				return;
			}
		}

		Enumeration<String> keys = props.keys();
		while (keys.hasMoreElements()) {
			String id = keys.nextElement();
			inspectProperty(id, props.get(id));
		}
	}

	private static void lineProperty(String id, Object value) {
		System.out.print(id);
		System.out.print('=');
		printValue(value);
	}

	private static void inspectProperty(String id, Object value) {
		System.out.print('\t');
		System.out.print(id);
		System.out.print('=');
		printValue(value);
		System.out.println();
	}

	private static void printValue(Object value) {
		if (!value.getClass().isArray()) {
			System.out.print(value);
			return;
		}

		Object[] values = (Object[]) value;
		System.out.print('[');
		int n = values.length;
		if (n > 0) {
			System.out.print(values[0]);
			for (int i = 1; i < n; ++i) {
				System.out.print(", ");
				System.out.print(values[i]);
			}
		}
		System.out.print(']');
	}

	private static ArrayList<String> getList(ArrayList<String> list) {
		if (list == null)
			list = new ArrayList<>();
		return list;
	}

	private static boolean equals(Object o1, Object o2) {
		final Class<?> clazz = o1.getClass();
		if (!clazz.isArray())
			return o1.equals(o2);

		if (clazz == byte[].class)
			return Arrays.equals((byte[]) o1, (byte[]) o2);
		if (clazz == boolean[].class)
			return Arrays.equals((boolean[]) o1, (boolean[]) o2);
		if (clazz == short[].class)
			return Arrays.equals((short[]) o1, (short[]) o2);
		if (clazz == int[].class)
			return Arrays.equals((int[]) o1, (int[]) o2);
		if (clazz == long[].class)
			return Arrays.equals((long[]) o1, (long[]) o2);
		if (clazz == float[].class)
			return Arrays.equals((float[]) o1, (float[]) o2);
		if (clazz == double[].class)
			return Arrays.equals((double[]) o1, (double[]) o2);

		return Arrays.equals((Object[]) o1, (Object[]) o2);
	}

	private Object ca() {
		final BundleContext context = m_context;
		final ServiceReference<ConfigurationAdmin> reference = context.getServiceReference(ConfigurationAdmin.class);
		return context.getService(reference);
	}

	private Object mts() {
		final BundleContext context = m_context;
		final ServiceReference<MetaTypeService> reference = context.getServiceReference(MetaTypeService.class);
		return context.getService(reference);
	}
}
