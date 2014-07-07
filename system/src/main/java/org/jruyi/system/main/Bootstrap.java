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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Constants;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

final class Bootstrap {

	static final int PT_NONE = 0;
	static final int PT_LOCAL = 1;
	static final int PT_SYSTEM = 2;
	static final int PT_FRAMEWORK = 3;
	private int m_propType = PT_NONE;
	private String m_frameworkUrl;
	private int m_initialBundleStartLevel = 6;
	private int m_startLevel = -1;
	private final Map<String, String> m_localProps;
	private final Map<String, String> m_frameworkProps;
	private final ArrayList<BundleInfo> m_bundleInfos;

	final class PropertyHandler implements IElementHandler {

		private String m_name = null;

		@Override
		public void start(Attributes attributes) throws SAXException {
			m_name = attributes.getValue("name");
		}

		@Override
		public void setText(String text) throws SAXException {
			putProperty(m_name, text);
		}

		@Override
		public void end() throws SAXException {
			m_name = null;
		}
	}

	final class SystemHandler extends DefaultElementHandler {

		@Override
		public void start(Attributes attributes) throws SAXException {
			changePropType(PT_SYSTEM);
		}

		@Override
		public void end() throws SAXException {
			changePropType(PT_NONE);
		}
	}

	final class LocalHandler extends DefaultElementHandler {

		@Override
		public void start(Attributes attributes) throws SAXException {
			changePropType(PT_LOCAL);
		}

		@Override
		public void end() throws SAXException {
			changePropType(PT_NONE);
		}
	}

	final class FrameworkHandler extends DefaultElementHandler {

		@Override
		public void start(Attributes attributes) throws SAXException {
			setFrameworkUrl(attributes.getValue("url"));
			setInitialBundleStartLevel(Integer.parseInt(attributes
					.getValue("initialBundleStartLevel")));
			getFrameworkProps().put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL,
					attributes.getValue("startLevel"));
			changePropType(PT_FRAMEWORK);
		}

		@Override
		public void end() throws SAXException {
			changePropType(PT_NONE);
		}
	}

	final class BundlesHandler extends DefaultElementHandler {

		@Override
		public void start(Attributes attributes) throws SAXException {
			final String v = attributes.getValue("startLevel");
			changeStartLevel(Integer.parseInt(v));
		}

		@Override
		public void end() throws SAXException {
			changeStartLevel(-1);
		}
	}

	final class BundleHandler extends DefaultElementHandler {

		@Override
		public void start(Attributes attributes) throws SAXException {
			try {
				final String bundleUrl = attributes.getValue("url");
				final String v = attributes.getValue("startLevel");
				final int startLevel = v == null ? getStartLevel() : Integer
						.parseInt(v);
				getBundleInfoList().add(new BundleInfo(bundleUrl, startLevel));

				changePropType(PT_FRAMEWORK);
			} catch (Exception e) {
				throw new SAXException(e);
			}
		}

		@Override
		public void end() throws SAXException {
			changePropType(PT_NONE);
		}
	}

	Bootstrap() {
		m_localProps = new HashMap<String, String>();
		m_frameworkProps = new LinkedHashMap<String, String>();
		m_bundleInfos = new ArrayList<BundleInfo>(64);
	}

	Map<String, IElementHandler> getHandlers() {
		final HashMap<String, IElementHandler> handlers = new HashMap<String, IElementHandler>();
		handlers.put("property", new PropertyHandler());
		handlers.put("local", new LocalHandler());
		handlers.put("system", new SystemHandler());
		handlers.put("framework", new FrameworkHandler());
		handlers.put("bundle", new BundleHandler());
		handlers.put("bundles", new BundlesHandler());
		return handlers;
	}

	void changePropType(int propType) {
		m_propType = propType;
	}

	void changeStartLevel(int startLevel) {
		m_startLevel = startLevel;
	}

	int getStartLevel() {
		return m_startLevel;
	}

	void putProperty(String name, String value) {
		switch (m_propType) {
		case PT_LOCAL:
			m_localProps.put(name, value);
			break;
		case PT_SYSTEM:
			System.setProperty(name, value);
			break;
		case PT_FRAMEWORK:
			m_frameworkProps.put(name, value);
			break;
		}
	}

	String getFrameworkUrl() {
		return m_frameworkUrl;
	}

	void setFrameworkUrl(String frameworkUrl) {
		m_frameworkUrl = frameworkUrl;
	}

	int getInitialBundleStartLevel() {
		return m_initialBundleStartLevel;
	}

	void setInitialBundleStartLevel(int initialBundleStartLevel) {
		m_initialBundleStartLevel = initialBundleStartLevel;
	}

	Map<String, String> getLocalProps() {
		return m_localProps;
	}

	Map<String, String> getFrameworkProps() {
		return m_frameworkProps;
	}

	List<BundleInfo> getBundleInfoList() {
		return m_bundleInfos;
	}
}
