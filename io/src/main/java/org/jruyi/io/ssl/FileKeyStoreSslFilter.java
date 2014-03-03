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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Service;
import org.jruyi.io.ISslContextParameters;

@Service
@Component(name = "jruyi.io.ssl.fks.filter", policy = ConfigurationPolicy.REQUIRE, createPid = false)
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
