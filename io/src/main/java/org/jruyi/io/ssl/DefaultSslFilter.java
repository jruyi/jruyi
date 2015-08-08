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

import java.util.HashMap;
import java.util.Map;

import org.jruyi.io.IFilter;
import org.jruyi.io.ISslContextParameters;
import org.jruyi.io.IoConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

/**
 * This default SSL filter uses the JVM default cacerts as the material of trust
 * manager and performs SAN verification. It should be usually used by client.
 */
@Component(name = "jruyi.io.ssl.default.filter", //
service = { IFilter.class }, //
configurationPolicy = ConfigurationPolicy.IGNORE, //
property = { IoConstants.FILTER_ID + "=" + IoConstants.FID_DEFAULT_SSL }, //
xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class DefaultSslFilter extends AbstractSslFilter {

	private ISslContextParameters m_sslcp;

	@Override
	protected ISslContextParameters sslcp() {
		return m_sslcp;
	}

	@Override
	protected void activate(Map<String, ?> properties) throws Exception {
		m_sslcp = DefaultSslContextParameters.INST;
		final Map<String, Object> props = new HashMap<>(properties);
		props.put("hostname", ""); // For SAN verification
		props.put("endpointIdentificationAlgorithm", "HTTPS");
		super.activate(props);
	}

	@Override
	protected void deactivate() {
		super.deactivate();
	}
}
