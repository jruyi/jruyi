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

import java.lang.reflect.Method;
import java.util.Map;

import javax.net.ssl.SSLParameters;

final class Configuration {

	private static final String[] M_PROPS = { "protocol", "provider" };
	private static final Method[] c_mProps;
	private String m_protocol;
	private String m_provider;
	private String m_clientAuth;
	private String m_hostname;
	private SSLParameters m_sslParameters;

	static {
		final Class<Configuration> clazz = Configuration.class;
		final int n = M_PROPS.length;
		c_mProps = new Method[n];
		try {
			for (int i = 0; i < n; ++i)
				c_mProps[i] = clazz.getMethod(M_PROPS[i]);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private Configuration() {
	}

	public static Configuration create(Map<String, ?> properties) {
		final Configuration conf = new Configuration();
		conf.protocol((String) properties.get("protocol"));
		conf.provider((String) properties.get("provider"));
		conf.clientAuth((String) properties.get("clientAuth"));
		conf.hostname((String) properties.get("hostname"));
		conf.initSslParameters(properties);
		return conf;
	}

	private void protocol(String protocol) {
		m_protocol = protocol;
	}

	public String protocol() {
		return m_protocol;
	}

	private void provider(String provider) {
		m_provider = provider;
	}

	public String provider() {
		return m_provider;
	}

	private void clientAuth(String clientAuth) {
		m_clientAuth = clientAuth;
	}

	public String clientAuth() {
		return m_clientAuth;
	}

	private void hostname(String hostname) {
		m_hostname = hostname;
	}

	public String hostname() {
		return m_hostname;
	}

	public String endpointIdentificationAlgorithm() {
		return m_sslParameters.getEndpointIdentificationAlgorithm();
	}

	public String[] enabledProtocols() {
		return m_sslParameters.getProtocols();
	}

	public String[] enabledCipherSuites() {
		return m_sslParameters.getCipherSuites();
	}

	public SSLParameters sslParameters() {
		return m_sslParameters;
	}

	public boolean isMandatoryChanged(Configuration conf) throws Exception {
		for (Method m : c_mProps) {
			final Object v1 = m.invoke(this);
			final Object v2 = m.invoke(conf);
			if (v1 == v2)
				continue;

			if (!(v1 == null ? v2.equals(v1) : v1.equals(v2)))
				return true;
		}

		return false;
	}

	private void initSslParameters(Map<String, ?> properties) {
		final SSLParameters sslParameters = new SSLParameters();
		final String clientAuth = m_clientAuth;
		if ("want".equals(clientAuth))
			sslParameters.setWantClientAuth(true);
		else if ("need".equals(clientAuth))
			sslParameters.setNeedClientAuth(true);
		sslParameters.setProtocols((String[]) properties.get("enabledProtocols"));
		sslParameters.setCipherSuites((String[]) properties.get("enabledCipherSuites"));
		final String v = (String) properties.get("endpointIdentificationAlgorithm");
		if (!"NONE".equals(v))
			sslParameters.setEndpointIdentificationAlgorithm(v);
		m_sslParameters = sslParameters;
	}
}
