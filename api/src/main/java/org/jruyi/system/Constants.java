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

package org.jruyi.system;

/**
 * <p>
 * Defines standard names for the JRuyi framework properties.
 * 
 * <p>
 * The values associated with these keys are of type {@code java.lang.String},
 * unless otherwise indicated.
 */
public final class Constants {

	/**
	 * Framework property (named "jruyi.name") identifying jruyi's name.
	 */
	public static final String JRUYI_NAME = "jruyi.name";
	/**
	 * Framework property (named "jruyi.version") identifying jruyi's version.
	 */
	public static final String JRUYI_VERSION = "jruyi.version";
	/**
	 * Framework property (named "jruyi.url") identifying jruyi's URL.
	 */
	public static final String JRUYI_URL = "jruyi.url";
	/**
	 * Framework property (named "jruyi.vendor") identifying the vendor.
	 */
	public static final String JRUYI_VENDOR = "jruyi.vendor";
	/**
	 * Framework property (named "jruyi.vendor.url") identifying the vendor's
	 * URL.
	 */
	public static final String JRUYI_VENDOR_URL = "jruyi.vendor.url";
	/**
	 * Framework property (named "jruyi.home.dir") identifying the home
	 * directory.
	 */
	public static final String JRUYI_HOME_DIR = "jruyi.home.dir";
	/**
	 * Framework property (named "jruyi.inst.name") identifying the current
	 * running instance's name.
	 */
	public static final String JRUYI_INST_NAME = "jruyi.inst.name";
	/**
	 * Framework property (named "jruyi.inst.base.dir") identifying the base
	 * directory of the current running instance.
	 */
	public static final String JRUYI_INST_BASE_DIR = "jruyi.inst.base.dir";
	/**
	 * Framework property (named "jruyi.inst.home.dir") identifying the home
	 * directory of the current running instance.
	 */
	public static final String JRUYI_INST_HOME_DIR = "jruyi.inst.home.dir";
	/**
	 * Framework property (named "jruyi.inst.conf.dir") identifying the conf
	 * directory of the current running instance.
	 */
	public static final String JRUYI_INST_CONF_DIR = "jruyi.inst.conf.dir";
	/**
	 * Framework property (named "jruyi.inst.data.dir") identifying the data
	 * directory of the current running instance.
	 */
	public static final String JRUYI_INST_DATA_DIR = "jruyi.inst.data.dir";
	/**
	 * Framework property (named "jruyi.inst.prov.dir") identifying the
	 * provisioning directory of the current running instance.
	 * 
	 * @since 2.0
	 */
	public static final String JRUYI_INST_PROV_DIR = "jruyi.inst.prov.dir";
	/**
	 * Framework property (named "jruyi.home.url") identifying the home URL.
	 */
	public static final String JRUYI_HOME_URL = "jruyi.home.url";
	/**
	 * Framework property (named "jruyi.bundle.base.url") identifying the bundle
	 * base URL.
	 */
	public static final String JRUYI_BUNDLE_BASE_URL = "jruyi.bundle.base.url";
	/**
	 * Framework property (named "jruyi.inst.base.url") identifying the base URL
	 * of the current running instance.
	 */
	public static final String JRUYI_INST_BASE_URL = "jruyi.inst.base.url";
	/**
	 * Framework property (named "jruyi.inst.home.url") identifying the home URL
	 * of the current running instance.
	 */
	public static final String JRUYI_INST_HOME_URL = "jruyi.inst.home.url";
	/**
	 * Framework property (named "jruyi.inst.conf.url") identifying the conf URL
	 * of the current running instance.
	 */
	public static final String JRUYI_INST_CONF_URL = "jruyi.inst.conf.url";
	/**
	 * Framework property (named "jruyi.inst.bootstrap.url") identifying the URL
	 * of the bootstrap configuration of the current running instance.
	 */
	public static final String JRUYI_INST_BOOTSTRAP_URL = "jruyi.inst.bootstrap.url";

	private Constants() {
	}
}
