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
package org.jruyi.io.ssl;

import java.util.Map;

import org.jruyi.io.IFilter;
import org.jruyi.io.ISslContextParameters;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;

@Component(name = "jruyi.io.ssl.fks.filter", //
configurationPolicy = ConfigurationPolicy.REQUIRE, //
service = { IFilter.class }, //
xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class FileKeyStoreSslFilter extends AbstractSslFilter {

	private final FileKeyStore m_fks = new FileKeyStore();

	@Modified
	@Override
	protected void modified(Map<String, ?> properties) throws Exception {
		m_fks.modified(properties);
		super.modified(properties);
	}

	@Override
	protected void activate(Map<String, ?> properties) throws Exception {
		m_fks.activate(properties);
		super.activate(properties);
	}

	@Override
	protected void deactivate() {
		super.deactivate();
		m_fks.deactivate();
	}

	@Override
	protected ISslContextParameters sslcp() {
		return m_fks;
	}
}
