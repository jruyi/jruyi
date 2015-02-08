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

import java.util.Map;

/**
 * This class is used to start/stop the JRuyi system.
 * 
 * @see Ruyi
 */
public final class Ruyi {

	private static final Ruyi INST = new Ruyi();

	/**
	 * Gets the singleton instance of this class.
	 * 
	 * @return the {@code Ruyi} singleton instance
	 */
	public static Ruyi getInstance() {
		return INST;
	}

	/**
	 * Sets the JRuyi system's properties to the given {@code properties}. The
	 * new properties will take effect after a new start.
	 * 
	 * @param properties
	 *            the properties to set
	 */
	public void setProperties(Map<String, String> properties) {
	}

	/**
	 * Starts the JRuyi system.
	 * 
	 * @throws Exception
	 *             if any error happens
	 */
	public void start() throws Exception {
	}

	/**
	 * Stops the JRuyi system.
	 * 
	 * @throws Exception
	 *             if any error happens
	 */
	public void stop() throws Exception {
	}

	/**
	 * Starts the JRuyi system and wait for stopping.
	 * 
	 * @throws Exception
	 *             if any error happens
	 */
	public void startAndWait() throws Exception {
	}

	/**
	 * Gets the value of the given JRuyi property {@code name}.
	 * 
	 * @return the property value
	 * @param name
	 *            the name of the property
	 */
	public String getProperty(String name) {
		return null;
	}
}
