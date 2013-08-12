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
package org.jruyi.cmd.conf;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Descriptor;
import org.jruyi.cmd.internal.RuyiCmd;
import org.jruyi.common.Properties;
import org.jruyi.common.StrUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service(Conf.class)
@Component(name = "jruyi.cmd.conf", policy = ConfigurationPolicy.IGNORE, createPid = false)
@org.apache.felix.scr.annotations.Properties({
		@Property(name = CommandProcessor.COMMAND_SCOPE, value = "conf"),
		@Property(name = CommandProcessor.COMMAND_FUNCTION, value = { "create",
				"delete", "list", "update", "exists" }) })
@Reference(name = "mts", referenceInterface = MetaTypeService.class, strategy = ReferenceStrategy.LOOKUP)
public final class Conf {

	private static final Logger c_logger = LoggerFactory.getLogger(Conf.class);
	private static final String MASK = "****";
	private OcdTracker m_tracker;

	@Reference(name = "configurationAdmin")
	private ConfigurationAdmin m_ca;

	static final class IdsPair {

		private String[] m_pids;
		private String[] m_factoryIds;

		void pids(String[] pids) {
			m_pids = pids;
		}

		String[] pids() {
			return m_pids;
		}

		void factoryPids(String[] factoryIds) {
			m_factoryIds = factoryIds;
		}

		String[] factoryPids() {
			return m_factoryIds;
		}
	}

	static final class OcdTracker extends BundleTracker<IdsPair> {

		private final ConcurrentHashMap<String, ObjectClassDefinition> m_pidOcds;
		private final ConcurrentHashMap<String, ObjectClassDefinition> m_factoryPidOcds;
		private final MetaTypeService m_mts;

		public OcdTracker(BundleContext context, MetaTypeService mts) {
			super(context, Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING
					| Bundle.ACTIVE | Bundle.STOPPING, null);

			m_pidOcds = new ConcurrentHashMap<String, ObjectClassDefinition>(
					256);
			m_factoryPidOcds = new ConcurrentHashMap<String, ObjectClassDefinition>(
					256);
			m_mts = mts;
		}

		@Override
		public IdsPair addingBundle(Bundle bundle, BundleEvent event) {
			IdsPair idsPair = new IdsPair();
			setOcds(bundle, idsPair);

			return idsPair;
		}

		@Override
		public void modifiedBundle(Bundle bundle, BundleEvent event,
				IdsPair idsPair) {
			if (event.getType() != BundleEvent.UPDATED)
				return;

			unsetOcds(idsPair);
			setOcds(bundle, idsPair);
		}

		@Override
		public void removedBundle(Bundle bundle, BundleEvent event,
				IdsPair idsPair) {
			unsetOcds(idsPair);
		}

		ObjectClassDefinition pidOcd(String pid) {
			return m_pidOcds.get(pid);
		}

		ObjectClassDefinition factoryPidOcd(String factoryPid) {
			return m_factoryPidOcds.get(factoryPid);
		}

		private void setOcds(Bundle bundle, IdsPair idsPair) {
			MetaTypeInformation mti = m_mts.getMetaTypeInformation(bundle);
			if (mti == null)
				return;

			ConcurrentHashMap<String, ObjectClassDefinition> ocds = m_pidOcds;
			String[] ids = mti.getPids();
			for (String id : ids) {
				if (ocds.putIfAbsent(id, mti.getObjectClassDefinition(id, null)) != null)
					c_logger.error(StrUtil.buildString(
							"ObjectClassDefinition for pid=", id,
							" is not unique"));
			}

			idsPair.pids(ids);

			ocds = m_factoryPidOcds;
			ids = mti.getFactoryPids();
			for (String id : ids) {
				if (ocds.putIfAbsent(id, mti.getObjectClassDefinition(id, null)) != null)
					c_logger.error(StrUtil.buildString(
							"ObjectClassDefinition for factoryPid=", id,
							" is not unique"));
			}

			idsPair.factoryPids(ids);
		}

		private void unsetOcds(IdsPair idsPair) {
			ConcurrentHashMap<String, ObjectClassDefinition> ocds = m_pidOcds;
			String[] ids = idsPair.pids();
			if (ids != null) {
				for (String id : ids)
					ocds.remove(id);
			}

			ocds = m_factoryPidOcds;
			ids = idsPair.factoryPids();
			if (ids != null) {
				for (String id : ids)
					ocds.remove(id);
			}
		}
	}

	@Descriptor("Create a configuration")
	public void create(@Descriptor("<pid|factoryPid>") String id,
			@Descriptor("[name=value] ...") String[] args) throws Exception {

		if (args == null || args.length < 1) {
			RuyiCmd.INST.help("conf:create");
			return;
		}

		Properties props = new Properties(args.length);
		for (String arg : args) {
			int i = arg.indexOf('=');
			if (i < 1 || i >= arg.length() - 1)
				continue;

			props.put(arg.substring(0, i).trim(), arg.substring(i + 1).trim());
		}

		ObjectClassDefinition ocd = m_tracker.factoryPidOcd(id);
		boolean factory = true;
		if (ocd == null) {
			ocd = m_tracker.pidOcd(id);
			if (ocd == null)
				throw new Exception("Metatype NOT Found: " + id);
			factory = false;
		}

		props = PropUtil.normalize(props, ocd);

		Configuration conf = factory ? m_ca
				.createFactoryConfiguration(id, null) : m_ca.getConfiguration(
				id, null);

		conf.update(props);
	}

	@Descriptor("Update configuration(s)")
	public void update(@Descriptor("<pid|filter>") String filter,
			@Descriptor("[name[=value]] ...") String[] args) throws Exception {
		if (args == null || args.length < 1) {
			RuyiCmd.INST.help("conf:update");
			return;
		}

		Configuration[] confs = m_ca
				.listConfigurations(normalizeFilter(filter));

		if (confs == null || confs.length < 1)
			throw new Exception("Configuration(s) NOT Found: " + filter);

		for (Configuration conf : confs) {
			Dictionary<String, Object> props = conf.getProperties();
			boolean modified = false;
			for (String arg : args) {
				int i = arg.indexOf('=');
				String name = i < 0 ? arg : arg.substring(0, i).trim();
				if (i < 0) {
					if (props.remove(name) != null)
						modified = true;
				} else {
					Object newValue = arg.substring(i + 1).trim();
					Object oldValue = props.put(name, newValue);
					if (!newValue.equals(oldValue))
						modified = true;
				}
			}

			if (modified) {
				String id = conf.getFactoryPid();
				ObjectClassDefinition ocd = id == null ? m_tracker.pidOcd(conf
						.getPid()) : m_tracker.factoryPidOcd(id);
				props = PropUtil.normalize(props, ocd);
				conf.update(props);
			}
		}
	}

	@Descriptor("Delete configuration(s)")
	public void delete(@Descriptor("[pid|filter]") String filter)
			throws Exception {
		Configuration[] confs = m_ca
				.listConfigurations(normalizeFilter(filter));
		if (confs == null || confs.length == 0) {
			System.err.print("Configuration(s) NOT Found: ");
			System.err.println(filter);
			return;
		}

		for (Configuration conf : confs)
			conf.delete();
	}

	@Descriptor("List configuration(s)")
	public void list(@Descriptor("[pid|filter]") String[] args)
			throws Exception {
		String filter = null;
		if (args != null) {
			if (args.length > 1) {
				RuyiCmd.INST.help("conf:list");
				return;
			} else if (args.length > 0)
				filter = normalizeFilter(args[0]);
		}

		Configuration[] confs = m_ca.listConfigurations(filter);
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

	@Descriptor("Test if configuration(s) exists")
	public boolean exists(@Descriptor("[pid|filter]") String filter)
			throws Exception {
		Configuration[] confs = m_ca
				.listConfigurations(normalizeFilter(filter));
		return (confs != null && confs.length > 0);
	}

	protected void bindConfigurationAdmin(ConfigurationAdmin ca) {
		m_ca = ca;
	}

	protected void unbindConfigurationAdmin(ConfigurationAdmin ca) {
		m_ca = null;
	}

	protected void activate(ComponentContext context) {
		MetaTypeService mts = (MetaTypeService) context.locateService("mts");
		m_tracker = new OcdTracker(context.getBundleContext(), mts);
		m_tracker.open();
	}

	protected void deactivate() {
		m_tracker.close();
		m_tracker = null;
	}

	private static String normalizeFilter(String filter) {
		if (filter == null)
			return null;

		filter = filter.trim();
		if (filter.charAt(0) != '(')
			filter = StrUtil.buildString("(" + Constants.SERVICE_PID + "=",
					filter, ')');

		return filter;
	}

	private void line(Configuration conf) {
		Dictionary<String, ?> props = conf.getProperties();

		String id = conf.getFactoryPid();
		ObjectClassDefinition ocd = id != null ? m_tracker.factoryPidOcd(id)
				: m_tracker.pidOcd(conf.getPid());
		if (ocd != null) {
			AttributeDefinition[] ads = ocd
					.getAttributeDefinitions(ObjectClassDefinition.ALL);
			int n;
			if (ads != null && (n = ads.length) > 0) {
				AttributeDefinition ad = ads[0];
				id = ad.getID();
				Object value = props.get(id);
				if (value != null) {
					if (ad.getType() == AttributeDefinition.PASSWORD)
						value = MASK;
					lineProperty(id, value);
				}

				for (int i = 1; i < n; ++i) {
					ad = ads[i];
					id = ad.getID();
					value = props.get(id);
					if (value != null) {
						System.out.print(", ");
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
			ocd = m_tracker.factoryPidOcd(factoryPid);
		} else
			ocd = m_tracker.pidOcd(pid);

		System.out.print("bundleLocation: ");
		System.out.println(conf.getBundleLocation());

		System.out.println("properties: ");
		Dictionary<String, ?> props = conf.getProperties();
		if (ocd != null) {
			AttributeDefinition[] ads = ocd
					.getAttributeDefinitions(ObjectClassDefinition.ALL);
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
}
