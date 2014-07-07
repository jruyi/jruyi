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

import static org.jruyi.system.Constants.JRUYI_INST_DATA_DIR;
import static org.osgi.framework.Constants.FRAGMENT_HOST;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BootLoader {

	private final Logger m_logger = LoggerFactory.getLogger(BootLoader.class);
	private BundleContext m_bundleContext;
	private Map<String, Bundle> m_installedBundles;
	private ArrayList<Bundle> m_bundles;

	Framework loadFramework(String frameworkUrl, Map<String, String> osgiProps,
			int initialBundleStartLevel) throws Exception {
		final URL url = new URL(frameworkUrl);
		frameworkUrl = url.toString();

		m_logger.info("OSGi Framework: {}", frameworkUrl);
		m_logger.info("Loading bundles...");

		final ClassLoader classLoader = Ruyi.class.getClassLoader();
		final Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL",
				URL.class);

		final boolean accessible = addUrl.isAccessible();
		if (!accessible)
			addUrl.setAccessible(true);

		addUrl.invoke(classLoader, url);

		if (!accessible)
			addUrl.setAccessible(false);

		if (osgiProps.get(FRAMEWORK_STORAGE) == null)
			osgiProps.put(FRAMEWORK_STORAGE, osgiProps.get(JRUYI_INST_DATA_DIR)
					+ File.separator + "osgi");

		final Framework framework = getFrameworkFactory(classLoader)
				.newFramework(osgiProps);
		framework.init();

		final BundleContext bundleContext = framework.getBundleContext();

		final FrameworkStartLevel fsl = bundleContext.getBundle().adapt(
				FrameworkStartLevel.class);
		fsl.setInitialBundleStartLevel(initialBundleStartLevel);

		final Bundle[] bundles = bundleContext.getBundles();
		final HashMap<String, Bundle> installedBundles = new HashMap<String, Bundle>(
				bundles.length);
		for (final Bundle bundle : bundles) {
			if (bundle.getBundleId() != 0)
				installedBundles.put(bundle.getLocation(), bundle);
		}

		m_bundleContext = bundleContext;
		m_installedBundles = installedBundles;

		return framework;
	}

	void startBundles(List<BundleInfo> bundleInfos) throws Exception {
		for (BundleInfo bundleInfo : bundleInfos)
			loadBundle(bundleInfo.getBundleUrl(), bundleInfo.getStartLevel());

		// deployBundles();

		m_bundleContext = null;
		m_installedBundles = null;

		m_logger.info("Done loading bundles");

		// Start all installed bundles
		final ArrayList<Bundle> bundles = m_bundles;
		for (Bundle bundle : bundles)
			bundle.start();

		m_bundles = null;
	}

	private void loadBundle(String bundleUrl, int startLevel) throws Exception {
		final Bundle bundle = loadBundle(new URL(bundleUrl));
		bundle.adapt(BundleStartLevel.class).setStartLevel(startLevel);
	}

	private Bundle loadBundle(URL url) throws Exception {
		final String bundleUrl = url.toString();
		Bundle bundle = m_installedBundles.remove(bundleUrl);
		if (bundle == null) {
			m_logger.debug("Install bundle: {}", bundleUrl);
			bundle = m_bundleContext.installBundle(bundleUrl);
		} else {
			long lastModified = url.openConnection().getLastModified();
			if (bundle.getLastModified() < lastModified) {
				m_logger.debug("Update bundle: {}", bundleUrl);
				bundle.update();
			}
		}

		if (!isFragment(bundle))
			getBundles().add(bundle);

		return bundle;
	}

	private FrameworkFactory getFrameworkFactory(ClassLoader classLoader)
			throws Exception {
		final ServiceLoader<FrameworkFactory> serviceLoader = ServiceLoader
				.load(FrameworkFactory.class, classLoader);
		final Iterator<FrameworkFactory> iter = serviceLoader.iterator();
		if (!iter.hasNext())
			throw new Exception("FrameworkFactory Not Found");

		return iter.next();
	}

	private ArrayList<Bundle> getBundles() {
		if (m_bundles == null)
			m_bundles = new ArrayList<Bundle>(50);

		return m_bundles;
	}

	private boolean isFragment(Bundle bundle) {
		return bundle.getHeaders().get(FRAGMENT_HOST) != null;
	}
}
