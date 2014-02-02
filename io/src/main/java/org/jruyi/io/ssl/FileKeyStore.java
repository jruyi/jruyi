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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Service;
import org.jruyi.io.ISslContextParameters;
import org.osgi.service.component.ComponentContext;

@Service
@Component(name = "jruyi.io.ssl.filekeystore", createPid = false)
public final class FileKeyStore implements ISslContextParameters {

	private static final char[] EMPTY_CHARARRAY = new char[0];
	private static final TrustManager[] TRUST_ALL_CERTS;
	private Configuration m_conf;
	private KeyManager[] m_keyManagers;
	private TrustManager[] m_trustManagers;
	private SecureRandom m_secureRandom;

	static {
		TRUST_ALL_CERTS = new TrustManager[] { new X509TrustManager() {

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(X509Certificate[] certs,
					String authType) {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] certs,
					String authType) {
			}
		} };
	}

	static final class Configuration {

		private static final String[] KEYPROPS = { "keyStoreType",
				"keyStoreProvider", "keyStoreUrl", "keyStorePassword",
				"keyPassword" };
		private static final String[] TRUSTPROPS = { "certValidation",
				"trustStoreType", "trustStoreProvider", "trustStoreUrl",
				"trustStorePassword" };
		private static final String[] SRPROPS = { "secureRandomAlgorithm",
				"secureRandomProvider" };
		static final Method[] c_keyProps;
		static final Method[] c_srProps;
		static final Method[] c_trustProps;
		private String m_keyStoreType;
		private String m_keyStoreProvider;
		private String m_keyStoreUrl;
		private String m_keyStorePassword;
		private String m_keyPassword;
		private String m_keyManagerFactoryAlgorithm;
		private String m_keyManagerFactoryProvider;
		private Boolean m_certValidation;
		private String m_trustStoreType;
		private String m_trustStoreProvider;
		private String m_trustStoreUrl;
		private String m_trustStorePassword;
		private String m_trustManagerFactoryAlgorithm;
		private String m_trustManagerFactoryProvider;
		private String m_secureRandomAlgorithm;
		private String m_secureRandomProvider;

		static {
			Class<Configuration> clazz = Configuration.class;
			try {
				c_keyProps = new Method[KEYPROPS.length];
				for (int i = 0; i < KEYPROPS.length; ++i)
					c_keyProps[i] = clazz.getMethod(KEYPROPS[i]);

				c_trustProps = new Method[TRUSTPROPS.length];
				for (int i = 0; i < TRUSTPROPS.length; ++i)
					c_trustProps[i] = clazz.getMethod(TRUSTPROPS[i]);

				c_srProps = new Method[SRPROPS.length];
				for (int i = 0; i < SRPROPS.length; ++i)
					c_srProps[i] = clazz.getMethod(SRPROPS[i]);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}

		public void initialize(Map<String, ?> properties) {
			keyStoreType((String) properties.get("keyStoreType"));
			keyStoreProvider((String) properties.get("keyStoreProvider"));
			keyStoreUrl((String) properties.get("keyStoreUrl"));
			keyStorePassword((String) properties.get("keyStorePassword"));
			keyPassword((String) properties.get("keyPassword"));
			keyManagerFactoryAlgorithm((String) properties
					.get("keyManagerFactoryAlgorithm"));
			keyManagerFactoryProvider((String) properties
					.get("keyManagerFactoryProvider"));

			certValidation((Boolean) properties.get("certValidation"));
			trustStoreType((String) properties.get("trustStoreType"));
			trustStoreProvider((String) properties.get("trustStoreProvider"));
			trustStoreUrl((String) properties.get("trustStoreUrl"));
			trustStorePassword((String) properties.get("trustStorePassword"));
			trustManagerFactoryAlgorithm((String) properties
					.get("trustManagerFactoryAlgorithm"));
			trustManagerFactoryProvider((String) properties
					.get("trustManagerFactoryProvider"));

			secureRandomAlgorithm((String) properties
					.get("secureRandomAlgorithm"));
			secureRandomProvider((String) properties
					.get("secureRandomProvider"));
		}

		public void keyStoreType(String keyStoreType) {
			if (keyStoreType == null
					|| (keyStoreType = keyStoreType.trim()).length() < 1)
				keyStoreType = KeyStore.getDefaultType();
			m_keyStoreType = keyStoreType;
		}

		public String keyStoreType() {
			return m_keyStoreType;
		}

		public void keyStoreProvider(String keyStoreProvider) {
			if (keyStoreProvider != null
					&& (keyStoreProvider = keyStoreProvider.trim()).length() < 1)
				keyStoreProvider = null;
			m_keyStoreProvider = keyStoreProvider;
		}

		public String keyStoreProvider() {
			return m_keyStoreProvider;
		}

		public void keyStoreUrl(String keyStoreUrl) {
			if (keyStoreUrl != null
					&& (keyStoreUrl = keyStoreUrl.trim()).length() < 1)
				keyStoreUrl = null;
			m_keyStoreUrl = keyStoreUrl;
		}

		public String keyStoreUrl() {
			return m_keyStoreUrl;
		}

		public void keyStorePassword(String keyStorePassword) {
			if (keyStorePassword != null
					&& (keyStorePassword = keyStorePassword.trim()).length() < 1)
				keyStorePassword = null;
			m_keyStorePassword = keyStorePassword;
		}

		public String keyStorePassword() {
			return m_keyStorePassword;
		}

		public void keyPassword(String keyPassword) {
			if (keyPassword != null
					&& (keyPassword = keyPassword.trim()).length() < 1)
				keyPassword = null;
			m_keyPassword = keyPassword;
		}

		public String keyPassword() {
			return m_keyPassword;
		}

		public void keyManagerFactoryAlgorithm(String keyManagerFactoryAlgorithm) {
			if (keyManagerFactoryAlgorithm == null
					|| (keyManagerFactoryAlgorithm = keyManagerFactoryAlgorithm
							.trim()).length() < 1)
				keyManagerFactoryAlgorithm = KeyManagerFactory
						.getDefaultAlgorithm();
			m_keyManagerFactoryAlgorithm = keyManagerFactoryAlgorithm;
		}

		public String keyManagerFactoryAlgorithm() {
			return m_keyManagerFactoryAlgorithm;
		}

		public void keyManagerFactoryProvider(String keyManagerFactoryProvider) {
			if (keyManagerFactoryProvider != null
					&& (keyManagerFactoryProvider = keyManagerFactoryProvider
							.trim()).length() < 1)
				keyManagerFactoryProvider = null;
			m_keyManagerFactoryProvider = keyManagerFactoryProvider;
		}

		public String keyManagerFactoryProvider() {
			return m_keyManagerFactoryProvider;
		}

		public void certValidation(Boolean certValidation) {
			m_certValidation = certValidation;
		}

		public Boolean certValidation() {
			return m_certValidation;
		}

		public void trustStoreType(String trustStoreType) {
			if (trustStoreType == null
					|| (trustStoreType = trustStoreType.trim()).length() < 1)
				trustStoreType = KeyStore.getDefaultType();
			m_trustStoreType = trustStoreType;
		}

		public String trustStoreType() {
			return m_trustStoreType;
		}

		public void trustStoreProvider(String trustStoreProvider) {
			if (trustStoreProvider != null
					&& (trustStoreProvider = trustStoreProvider.trim())
							.length() < 1)
				trustStoreProvider = null;
			m_trustStoreProvider = trustStoreProvider;
		}

		public String trustStoreProvider() {
			return m_trustStoreProvider;
		}

		public void trustStoreUrl(String trustStoreUrl) {
			if (trustStoreUrl != null
					&& (trustStoreUrl = trustStoreUrl.trim()).length() < 1)
				trustStoreUrl = null;
			m_trustStoreUrl = trustStoreUrl;
		}

		public String trustStoreUrl() {
			return m_trustStoreUrl;
		}

		public void trustStorePassword(String trustStorePassword) {
			if (trustStorePassword != null
					&& (trustStorePassword = trustStorePassword.trim())
							.length() < 1)
				trustStorePassword = null;
			m_trustStorePassword = trustStorePassword;
		}

		public String trustStorePassword() {
			return m_trustStorePassword;
		}

		public void trustManagerFactoryAlgorithm(
				String trustManagerFactoryAlgorithm) {
			if (trustManagerFactoryAlgorithm == null
					|| (trustManagerFactoryAlgorithm = trustManagerFactoryAlgorithm
							.trim()).length() < 1)
				trustManagerFactoryAlgorithm = TrustManagerFactory
						.getDefaultAlgorithm();
			m_trustManagerFactoryAlgorithm = trustManagerFactoryAlgorithm;
		}

		public String trustManagerFactoryAlgorithm() {
			return m_trustManagerFactoryAlgorithm;
		}

		public void trustManagerFactoryProvider(
				String trustManagerFactoryProvider) {
			if (trustManagerFactoryProvider != null
					&& (trustManagerFactoryProvider = trustManagerFactoryProvider
							.trim()).length() < 1)
				trustManagerFactoryProvider = null;
			m_trustManagerFactoryProvider = trustManagerFactoryProvider;
		}

		public String trustManagerFactoryProvider() {
			return m_trustManagerFactoryProvider;
		}

		public void secureRandomAlgorithm(String secureRandomAlgorithm) {
			if (secureRandomAlgorithm != null
					&& (secureRandomAlgorithm = secureRandomAlgorithm.trim())
							.length() < 1)
				secureRandomAlgorithm = null;
			m_secureRandomAlgorithm = secureRandomAlgorithm;
		}

		public String secureRandomAlgorithm() {
			return m_secureRandomAlgorithm;
		}

		public void secureRandomProvider(String secureRandomProvider) {
			if (secureRandomProvider != null
					&& (secureRandomProvider = secureRandomProvider.trim())
							.length() < 1)
				secureRandomProvider = null;
			m_secureRandomProvider = secureRandomProvider;
		}

		public String secureRandomProvider() {
			return m_secureRandomProvider;
		}

		public boolean isChanged(Configuration conf, Method[] mProps)
				throws Exception {
			for (Method m : mProps) {
				Object v1 = m.invoke(this);
				Object v2 = m.invoke(conf);
				if (v1 == v2)
					continue;

				if (!(v1 == null ? v2.equals(v1) : v1.equals(v2)))
					return true;
			}

			return false;
		}
	}

	@Override
	public KeyManager[] getKeyManagers() throws Exception {
		return m_keyManagers;
	}

	@Override
	public TrustManager[] getCertManagers() throws Exception {
		return m_trustManagers;
	}

	@Override
	public SecureRandom getSecureRandom() throws Exception {
		return m_secureRandom;
	}

	@Modified
	protected void modified(Map<String, ?> properties) throws Exception {
		Configuration newConf = new Configuration();
		newConf.initialize(properties);
		Configuration conf = m_conf;
		KeyManager[] keyManagers = newConf.isChanged(conf,
				Configuration.c_keyProps) ? getKeyManagers(newConf)
				: m_keyManagers;
		TrustManager[] trustManagers = newConf.isChanged(conf,
				Configuration.c_trustProps) ? getTrustManagers(newConf)
				: m_trustManagers;
		SecureRandom secureRandom = newConf.isChanged(conf,
				Configuration.c_srProps) ? getSecureRandom(newConf)
				: m_secureRandom;

		m_keyManagers = keyManagers;
		m_trustManagers = trustManagers;
		m_secureRandom = secureRandom;

		m_conf = newConf;
	}

	protected void activate(ComponentContext context, Map<String, ?> properties)
			throws Exception {
		Configuration conf = new Configuration();
		conf.initialize(properties);

		KeyManager[] keyManagers = getKeyManagers(conf);
		TrustManager[] trustManagers = getTrustManagers(conf);
		SecureRandom secureRandom = getSecureRandom(conf);

		m_keyManagers = keyManagers;
		m_trustManagers = trustManagers;
		m_secureRandom = secureRandom;

		m_conf = conf;
	}

	protected void deactivate() {
		m_conf = null;

		m_keyManagers = null;
		m_trustManagers = null;
		m_secureRandom = null;
	}

	private KeyManager[] getKeyManagers(Configuration conf) throws Exception {
		String keyStoreUrl = conf.keyStoreUrl();
		if (keyStoreUrl == null || keyStoreUrl.length() < 1)
			return null;

		String v = conf.keyStorePassword();
		char[] password = v == null ? EMPTY_CHARARRAY : v.toCharArray();

		v = conf.keyStoreProvider();
		KeyStore ks = v == null ? KeyStore.getInstance(conf.keyStoreType())
				: KeyStore.getInstance(conf.keyStoreType(), v);

		File file = new File(keyStoreUrl);
		@SuppressWarnings("resource")
		InputStream in = file.exists() ? new FileInputStream(file) : new URL(
				keyStoreUrl).openStream();
		try {
			in = new BufferedInputStream(in);
			ks.load(in, password);
		} finally {
			in.close();
		}

		v = conf.keyManagerFactoryProvider();
		KeyManagerFactory kmf = v == null ? KeyManagerFactory.getInstance(conf
				.keyManagerFactoryAlgorithm()) : KeyManagerFactory.getInstance(
				conf.keyManagerFactoryAlgorithm(), v);
		kmf.init(ks, password);

		return kmf.getKeyManagers();
	}

	private TrustManager[] getTrustManagers(Configuration conf)
			throws Exception {
		if (!conf.certValidation())
			return TRUST_ALL_CERTS;

		String trustStoreUrl = conf.trustStoreUrl();
		if (trustStoreUrl == null || trustStoreUrl.length() < 1)
			return null;

		String v = conf.trustStorePassword();
		char[] password = v == null ? EMPTY_CHARARRAY : v.toCharArray();

		v = conf.trustStoreProvider();
		KeyStore ks = v == null ? KeyStore.getInstance(conf.trustStoreType())
				: KeyStore.getInstance(conf.trustStoreType(), v);

		File file = new File(trustStoreUrl);
		@SuppressWarnings("resource")
		InputStream in = file.exists() ? new FileInputStream(file) : new URL(
				trustStoreUrl).openStream();
		try {
			in = new BufferedInputStream(in);
			ks.load(in, password);
		} finally {
			in.close();
		}

		v = conf.trustManagerFactoryProvider();
		TrustManagerFactory tmf = v == null ? TrustManagerFactory
				.getInstance(conf.trustManagerFactoryAlgorithm())
				: TrustManagerFactory.getInstance(
						conf.trustManagerFactoryAlgorithm(), v);
		tmf.init(ks);

		return tmf.getTrustManagers();
	}

	private SecureRandom getSecureRandom(Configuration conf) throws Exception {
		if (conf.secureRandomAlgorithm() == null)
			return (conf.secureRandomProvider() == null) ? null : SecureRandom
					.getInstance(null, conf.secureRandomProvider());
		else if (conf.secureRandomProvider() == null)
			return SecureRandom.getInstance(conf.secureRandomAlgorithm());
		else
			return SecureRandom.getInstance(conf.secureRandomAlgorithm(),
					conf.secureRandomProvider());
	}
}
