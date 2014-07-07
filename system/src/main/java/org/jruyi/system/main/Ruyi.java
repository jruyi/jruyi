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
package org.jruyi.system.main;

import static org.jruyi.system.Constants.JRUYI_BUNDLE_BASE_URL;
import static org.jruyi.system.Constants.JRUYI_HOME_DIR;
import static org.jruyi.system.Constants.JRUYI_HOME_URL;
import static org.jruyi.system.Constants.JRUYI_INST_BASE_DIR;
import static org.jruyi.system.Constants.JRUYI_INST_BASE_URL;
import static org.jruyi.system.Constants.JRUYI_INST_BOOTSTRAP_URL;
import static org.jruyi.system.Constants.JRUYI_INST_CONF_DIR;
import static org.jruyi.system.Constants.JRUYI_INST_CONF_URL;
import static org.jruyi.system.Constants.JRUYI_INST_DATA_DIR;
import static org.jruyi.system.Constants.JRUYI_INST_HOME_DIR;
import static org.jruyi.system.Constants.JRUYI_INST_HOME_URL;
import static org.jruyi.system.Constants.JRUYI_INST_NAME;
import static org.jruyi.system.Constants.JRUYI_NAME;
import static org.jruyi.system.Constants.JRUYI_URL;
import static org.jruyi.system.Constants.JRUYI_VENDOR;
import static org.jruyi.system.Constants.JRUYI_VENDOR_URL;
import static org.jruyi.system.Constants.JRUYI_VERSION;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Ruyi {

	private static final Ruyi INST = new Ruyi();
	private Map<String, String> m_properties;
	private final ReentrantLock m_lock = new ReentrantLock();
	private Framework m_framework;

	static final class Log {
		static final Logger INST = LoggerFactory.getLogger(Ruyi.class);
	}

	private static Properties getProductProps() throws Exception {
		final InputStream in = Ruyi.class
				.getResourceAsStream("product.properties");
		try {
			final Properties props = new Properties();
			props.load(in);
			return props;
		} finally {
			in.close();
		}
	}

	public static Ruyi getInstance() {
		return INST;
	}

	public void setProperties(Map<String, String> properties) {
		properties = properties == null ? new HashMap<String, String>(32)
				: new HashMap<String, String>(properties);

		String v = properties.get(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
		if (v != null && (v = v.trim()).length() > 0) {
			v = v.replaceAll("[\t\r\n ]", "");
			properties.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, v);
		}

		m_properties = properties;

		initialize();
	}

	public String property(String name) {
		final String value = m_properties.get(name);
		if (value != null)
			return value;

		return System.getProperty(name);
	}

	public void start() throws Exception {
		final ReentrantLock lock = m_lock;
		if (!lock.tryLock())
			return;

		if (m_framework != null)
			return;

		Log.INST.info("Start JRuyi (version={})", property(JRUYI_VERSION));

		try {
			logDir();
			logAllSysProps();

			final Bootstrap bootstrap = loadBootstrap();

			final Map<String, String> osgiProps = bootstrap.getFrameworkProps();
			initOsgiProps(osgiProps);

			final BootLoader loader = new BootLoader();
			m_framework = loader.loadFramework(bootstrap.getFrameworkUrl(),
					osgiProps, bootstrap.getInitialBundleStartLevel());

			loader.startBundles(bootstrap.getBundleInfoList());

			m_framework.start();

		} catch (Exception e) {
			Log.INST.error("Failed to start JRuyi", e);
			throw e;
		} finally {
			lock.unlock();
		}
	}

	public void startAndWait() throws Exception {
		start();
		final Framework framework = m_framework;
		if (framework != null)
			framework.waitForStop(0L);
	}

	public void stop() throws Exception {
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			Framework framework = m_framework;
			if (framework != null) {
				m_framework = null;

				Log.INST.info("Stopping JRuyi...");

				if (framework.getState() != Bundle.STOPPING)
					framework.stop();

				framework.waitForStop(0L);

				Log.INST.info("JRuyi stopped");
			}
		} catch (Exception e) {
			Log.INST.error("Failed to stop JRuyi", e);
			throw e;
		} finally {
			lock.unlock();
		}
	}

	private void initialize() {

		try {
			initDirProps();
			initProductProps();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void initDirProps() throws IOException {

		// Home Dir
		final File homeDir;
		String temp = property(JRUYI_HOME_DIR);
		if (temp == null) {
			temp = Ruyi.class.getProtectionDomain().getCodeSource()
					.getLocation().getFile();
			temp = URLDecoder.decode(temp, "UTF-8");
			homeDir = new File(temp).getParentFile().getParentFile()
					.getCanonicalFile();
		} else
			homeDir = new File(temp).getCanonicalFile();

		// Home URL
		temp = property(JRUYI_HOME_URL);
		final URL homeUrl = (temp == null) ? homeDir.toURI().toURL() : new URL(
				temp);

		// Bundle Base URL
		final URL bundleBaseUrl = getUrl(JRUYI_BUNDLE_BASE_URL, homeUrl,
				"bundles/");

		// Instance Base Dir
		final File instBaseDir = getDir(JRUYI_INST_BASE_DIR, homeDir, "inst");
		// Instance Base URL
		final URL instBaseUrl = getUrl(JRUYI_INST_BASE_URL, homeUrl, "inst/");

		// Instance Name
		String instName = property(JRUYI_INST_NAME);
		if (instName == null)
			instName = "default";

		// Instance Home Dir
		final File instHomeDir = getDir(JRUYI_INST_HOME_DIR, instBaseDir,
				instName);
		// Instance Home URL
		final URL instHomeUrl = getUrl(JRUYI_INST_HOME_URL, instBaseUrl,
				instName + "/");

		// Instance Conf Dir
		final File instConfDir = getDir(JRUYI_INST_CONF_DIR, instHomeDir,
				"conf");
		// Instance Conf URL
		final URL instConfUrl = getUrl(JRUYI_INST_CONF_URL, instHomeUrl,
				"conf/");

		final URL instBootstrapUrl = getUrl(JRUYI_INST_BOOTSTRAP_URL,
				instConfUrl, "bootstrap.xml");

		final File instDataDir = getDir(JRUYI_INST_DATA_DIR, instHomeDir,
				"data");

		final Map<String, String> props = m_properties;
		props.put(JRUYI_HOME_DIR, homeDir.getCanonicalPath());
		props.put(JRUYI_HOME_URL, homeUrl.toString());
		props.put(JRUYI_BUNDLE_BASE_URL, bundleBaseUrl.toString());
		props.put(JRUYI_INST_BASE_DIR, instBaseDir.getCanonicalPath());
		props.put(JRUYI_INST_BASE_URL, instBaseUrl.toString());
		props.put(JRUYI_INST_NAME, instName);
		props.put(JRUYI_INST_HOME_DIR, instHomeDir.getCanonicalPath());
		props.put(JRUYI_INST_HOME_URL, instHomeUrl.toString());
		props.put(JRUYI_INST_CONF_DIR, instConfDir.getCanonicalPath());
		props.put(JRUYI_INST_CONF_URL, instConfUrl.toString());
		props.put(JRUYI_INST_BOOTSTRAP_URL, instBootstrapUrl.toString());
		props.put(JRUYI_INST_DATA_DIR, instDataDir.getCanonicalPath());
	}

	private void initProductProps() throws Exception {
		final Properties productProps = getProductProps();
		System.setProperty(JRUYI_NAME, productProps.getProperty(JRUYI_NAME));
		System.setProperty(JRUYI_VERSION,
				productProps.getProperty(JRUYI_VERSION));
		System.setProperty(JRUYI_URL, productProps.getProperty(JRUYI_URL));
		System.setProperty(JRUYI_VENDOR, productProps.getProperty(JRUYI_VENDOR));
		System.setProperty(JRUYI_VENDOR_URL,
				productProps.getProperty(JRUYI_VENDOR_URL));
	}

	private void logDir() {
		Log.INST.info("JRuyi Home Dir: {}", property(JRUYI_HOME_DIR));
		Log.INST.info("JRuyi Home URL: {}", property(JRUYI_HOME_URL));
		Log.INST.info("Instance Name: {}", property(JRUYI_INST_NAME));
		Log.INST.info("Instance Home Dir: {}", property(JRUYI_INST_HOME_DIR));
		Log.INST.info("Instance Home URL: {}", property(JRUYI_INST_HOME_URL));
	}

	private void logAllSysProps() {

		if (!Log.INST.isDebugEnabled())
			return;

		Log.INST.debug("Print all the system properties");

		for (Entry<Object, Object> entry : System.getProperties().entrySet())
			Log.INST.debug(entry.toString());
	}

	private Bootstrap loadBootstrap() throws Exception {
		String bootstrapUrl = property(JRUYI_INST_BOOTSTRAP_URL);
		if (bootstrapUrl == null)
			bootstrapUrl = "file:bootstrap.xml";

		final Bootstrap bootstrap = new Bootstrap();
		bootstrap.getLocalProps().putAll(m_properties);
		final InputStream in = Ruyi.class.getResourceAsStream("bootstrap.xsd");
		try {
			XmlParser.getInstance(in).parse(bootstrapUrl,
					bootstrap.getHandlers(), bootstrap.getLocalProps());
		} finally {
			try {
				in.close();
			} catch (Exception e) {
			}
		}

		return bootstrap;
	}

	private void initOsgiProps(Map<String, String> osgiProps) {
		final Map<String, String> properties = m_properties;
		String pkgExtra = properties
				.get(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);

		String v = (String) osgiProps
				.get(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
		if (v != null && (v = v.trim()).length() > 0) {
			v = v.replaceAll("[\t\r\n ]", "");
			pkgExtra = pkgExtra == null ? v : pkgExtra + "," + v;
		}

		osgiProps.putAll(properties);
		if (pkgExtra != null)
			osgiProps.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, pkgExtra);
	}

	private File getDir(String key, File parent, String child)
			throws IOException {
		final String pathName = property(key);
		return (pathName == null) ? new File(parent, child)
				: new File(pathName).getCanonicalFile();
	}

	private URL getUrl(String key, URL context, String spec)
			throws MalformedURLException {
		final String str = property(key);
		return (str == null) ? new URL(context, spec) : new URL(spec);
	}
}
