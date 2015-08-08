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

package org.jruyi.core.internal;

import java.util.HashMap;

import org.jruyi.core.ISslFilterBuilder;
import org.jruyi.io.IBuffer;
import org.jruyi.io.IFilter;
import org.jruyi.io.ISslContextParameters;
import org.jruyi.io.ssl.DefaultSslContextParameters;
import org.jruyi.io.ssl.SslFilter;

final class SslFilterBuilder implements ISslFilterBuilder {

	private final HashMap<String, Object> m_properties;
	private ISslContextParameters m_sslcp;

	public SslFilterBuilder() {
		final HashMap<String, Object> properties = new HashMap<>(8);
		properties.put("protocol", "TLS");
		properties.put("hostname", "");
		properties.put("endpointIdentificationAlgorithm", "NONE");
		properties.put("enableSessionCreation", Boolean.TRUE);
		m_properties = properties;
	}

	@Override
	public SslFilterBuilder protocol(String protocol) {
		if (protocol == null || (protocol = protocol.trim()).isEmpty())
			m_properties.remove("protocol");
		else
			m_properties.put("protocol", protocol);
		return this;
	}

	@Override
	public SslFilterBuilder provider(String provider) {
		if (provider == null || (provider = provider.trim()).isEmpty())
			m_properties.remove("provider");
		else
			m_properties.put("provider", provider);
		return this;
	}

	@Override
	public SslFilterBuilder clientAuth(ClientAuth clientAuth) {
		if (clientAuth == null)
			m_properties.remove("clientAuth");
		else
			m_properties.put("clientAuth", clientAuth.name());
		return this;
	}

	@Override
	public SslFilterBuilder hostname(String hostname) {
		if (hostname == null)
			m_properties.remove("hostname");
		else
			m_properties.put("hostname", hostname.trim());
		return this;
	}

	@Override
	public SslFilterBuilder endpointIdentificationAlgorithm(EndpointIdAlg alg) {
		final String algName = alg == null ? "NONE" : alg.name();
		m_properties.put("endpointIdentificationAlgorithm", algName);
		return this;
	}

	@Override
	public SslFilterBuilder enabledProtocols(String[] protocols) {
		if (protocols == null)
			m_properties.remove("enabledProtocols");
		else
			m_properties.put("enabledProtocols", protocols);
		return this;
	}

	@Override
	public SslFilterBuilder enabledCipherSuites(String[] cipherSuites) {
		if (cipherSuites == null)
			m_properties.remove("enabledCipherSuites");
		else
			m_properties.put("enabledCipherSuites", cipherSuites);
		return this;
	}

	@Override
	public SslFilterBuilder enableSessionCreation(boolean enableSessionCreation) {
		m_properties.put("enableSessionCreation", enableSessionCreation);
		return this;
	}

	@Override
	public SslFilterBuilder sslContextParameters(ISslContextParameters sslcp) {
		m_sslcp = sslcp;
		return this;
	}

	@Override
	public IFilter<IBuffer, IBuffer> build() throws Throwable {
		final SslFilter sslFilter = new SslFilter();
		ISslContextParameters sslcp = m_sslcp;
		if (sslcp == null)
			sslcp = defaultSslContextParameters();
		sslFilter.setSslContextParameters(sslcp);
		sslFilter.activate(m_properties);
		return sslFilter;
	}

	private ISslContextParameters defaultSslContextParameters() {
		return DefaultSslContextParameters.INST;
	}
}
