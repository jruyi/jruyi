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
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.jruyi.io.ISslContextParameters;

final class FileKeyStore implements ISslContextParameters {

	private static final String KS_TYPE = "keyStoreType";
	private static final String KS_PROVIDER = "keyStoreProvider";
	private static final String KS_URL = "keyStoreUrl";
	private static final String KS_PASSWORD = "keyStorePassword";
	private static final String KEY_PASSWORD = "keyPassword";
	private static final String KMF_ALG = "keyManagerFactoryAlgorithm";
	private static final String KMF_PROVIDER = "keyManagerFactoryProvider";

	private static final String CERT_VALIDATION = "certValidation";
	private static final String TS_TYPE = "trustStoreType";
	private static final String TS_PROVIDER = "trustStoreProvider";
	private static final String TS_URL = "trustStoreUrl";
	private static final String TS_PASSWORD = "trustStorePassword";
	private static final String TMF_ALG = "trustManagerFactoryAlgorithm";
	private static final String TMF_PROVIDER = "trustManagerFactoryProvider";

	private static final String SR_ALG = "secureRandomAlgorithm";
	private static final String SR_PROVIDER = "secureRandomProvider";

	private static final char[] EMPTY_CHARARRAY = new char[0];
	private Configuration m_conf;
	private KeyManager[] m_keyManagers;
	private TrustManager[] m_trustManagers;
	private SecureRandom m_secureRandom;

	static final class Configuration {

		private static final String[] KEYPROPS = { KS_TYPE, KS_PROVIDER, KS_URL, KS_PASSWORD, KEY_PASSWORD };
		private static final String[] TRUSTPROPS = { CERT_VALIDATION, TS_TYPE, TS_PROVIDER, TS_URL, TS_PASSWORD };
		private static final String[] SRPROPS = { SR_ALG, SR_PROVIDER };
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
			keyStoreType((String) properties.get(KS_TYPE));
			keyStoreProvider((String) properties.get(KS_PROVIDER));
			keyStoreUrl((String) properties.get(KS_URL));
			keyStorePassword((String) properties.get(KS_PASSWORD));
			keyPassword((String) properties.get(KEY_PASSWORD));
			keyManagerFactoryAlgorithm((String) properties.get(KMF_ALG));
			keyManagerFactoryProvider((String) properties.get(KMF_PROVIDER));

			certValidation((Boolean) properties.get(CERT_VALIDATION));
			trustStoreType((String) properties.get(TS_TYPE));
			trustStoreProvider((String) properties.get(TS_PROVIDER));
			trustStoreUrl((String) properties.get(TS_URL));
			trustStorePassword((String) properties.get(TS_PASSWORD));
			trustManagerFactoryAlgorithm((String) properties.get(TMF_ALG));
			trustManagerFactoryProvider((String) properties.get(TMF_PROVIDER));

			secureRandomAlgorithm((String) properties.get(SR_ALG));
			secureRandomProvider((String) properties.get(SR_PROVIDER));
		}

		public void keyStoreType(String keyStoreType) {
			if (keyStoreType == null || (keyStoreType = keyStoreType.trim()).length() < 1)
				keyStoreType = KeyStore.getDefaultType();
			m_keyStoreType = keyStoreType;
		}

		public String keyStoreType() {
			return m_keyStoreType;
		}

		public void keyStoreProvider(String keyStoreProvider) {
			if (keyStoreProvider != null && (keyStoreProvider = keyStoreProvider.trim()).length() < 1)
				keyStoreProvider = null;
			m_keyStoreProvider = keyStoreProvider;
		}

		public String keyStoreProvider() {
			return m_keyStoreProvider;
		}

		public void keyStoreUrl(String keyStoreUrl) {
			if (keyStoreUrl != null && (keyStoreUrl = keyStoreUrl.trim()).length() < 1)
				keyStoreUrl = null;
			m_keyStoreUrl = keyStoreUrl;
		}

		public String keyStoreUrl() {
			return m_keyStoreUrl;
		}

		public void keyStorePassword(String keyStorePassword) {
			if (keyStorePassword != null && (keyStorePassword = keyStorePassword.trim()).length() < 1)
				keyStorePassword = null;
			m_keyStorePassword = keyStorePassword;
		}

		public String keyStorePassword() {
			return m_keyStorePassword;
		}

		public void keyPassword(String keyPassword) {
			if (keyPassword != null && (keyPassword = keyPassword.trim()).length() < 1)
				keyPassword = null;
			m_keyPassword = keyPassword;
		}

		public String keyPassword() {
			return m_keyPassword;
		}

		public void keyManagerFactoryAlgorithm(String keyManagerFactoryAlgorithm) {
			if (keyManagerFactoryAlgorithm == null
					|| (keyManagerFactoryAlgorithm = keyManagerFactoryAlgorithm.trim()).length() < 1)
				keyManagerFactoryAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
			m_keyManagerFactoryAlgorithm = keyManagerFactoryAlgorithm;
		}

		public String keyManagerFactoryAlgorithm() {
			return m_keyManagerFactoryAlgorithm;
		}

		public void keyManagerFactoryProvider(String keyManagerFactoryProvider) {
			if (keyManagerFactoryProvider != null
					&& (keyManagerFactoryProvider = keyManagerFactoryProvider.trim()).length() < 1)
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
			if (trustStoreType == null || (trustStoreType = trustStoreType.trim()).length() < 1)
				trustStoreType = KeyStore.getDefaultType();
			m_trustStoreType = trustStoreType;
		}

		public String trustStoreType() {
			return m_trustStoreType;
		}

		public void trustStoreProvider(String trustStoreProvider) {
			if (trustStoreProvider != null && (trustStoreProvider = trustStoreProvider.trim()).length() < 1)
				trustStoreProvider = null;
			m_trustStoreProvider = trustStoreProvider;
		}

		public String trustStoreProvider() {
			return m_trustStoreProvider;
		}

		public void trustStoreUrl(String trustStoreUrl) {
			if (trustStoreUrl != null && (trustStoreUrl = trustStoreUrl.trim()).length() < 1)
				trustStoreUrl = null;
			m_trustStoreUrl = trustStoreUrl;
		}

		public String trustStoreUrl() {
			return m_trustStoreUrl;
		}

		public void trustStorePassword(String trustStorePassword) {
			if (trustStorePassword != null && (trustStorePassword = trustStorePassword.trim()).length() < 1)
				trustStorePassword = null;
			m_trustStorePassword = trustStorePassword;
		}

		public String trustStorePassword() {
			return m_trustStorePassword;
		}

		public void trustManagerFactoryAlgorithm(String trustManagerFactoryAlgorithm) {
			if (trustManagerFactoryAlgorithm == null
					|| (trustManagerFactoryAlgorithm = trustManagerFactoryAlgorithm.trim()).length() < 1)
				trustManagerFactoryAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
			m_trustManagerFactoryAlgorithm = trustManagerFactoryAlgorithm;
		}

		public String trustManagerFactoryAlgorithm() {
			return m_trustManagerFactoryAlgorithm;
		}

		public void trustManagerFactoryProvider(String trustManagerFactoryProvider) {
			if (trustManagerFactoryProvider != null
					&& (trustManagerFactoryProvider = trustManagerFactoryProvider.trim()).length() < 1)
				trustManagerFactoryProvider = null;
			m_trustManagerFactoryProvider = trustManagerFactoryProvider;
		}

		public String trustManagerFactoryProvider() {
			return m_trustManagerFactoryProvider;
		}

		public void secureRandomAlgorithm(String secureRandomAlgorithm) {
			if (secureRandomAlgorithm != null && (secureRandomAlgorithm = secureRandomAlgorithm.trim()).length() < 1)
				secureRandomAlgorithm = null;
			m_secureRandomAlgorithm = secureRandomAlgorithm;
		}

		public String secureRandomAlgorithm() {
			return m_secureRandomAlgorithm;
		}

		public void secureRandomProvider(String secureRandomProvider) {
			if (secureRandomProvider != null && (secureRandomProvider = secureRandomProvider.trim()).length() < 1)
				secureRandomProvider = null;
			m_secureRandomProvider = secureRandomProvider;
		}

		public String secureRandomProvider() {
			return m_secureRandomProvider;
		}

		public boolean isChanged(Configuration conf, Method[] mProps) throws Exception {
			for (Method m : mProps) {
				final Object v1 = m.invoke(this);
				final Object v2 = m.invoke(conf);
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

	void modified(Map<String, ?> properties) throws Exception {
		final Configuration newConf = new Configuration();
		newConf.initialize(properties);
		final Configuration conf = m_conf;
		final KeyManager[] keyManagers = newConf.isChanged(conf, Configuration.c_keyProps) ? getKeyManagers(newConf)
				: m_keyManagers;
		final TrustManager[] trustManagers = newConf.isChanged(conf, Configuration.c_trustProps) ? getTrustManagers(newConf)
				: m_trustManagers;
		final SecureRandom secureRandom = newConf.isChanged(conf, Configuration.c_srProps) ? getSecureRandom(newConf)
				: m_secureRandom;

		m_keyManagers = keyManagers;
		m_trustManagers = trustManagers;
		m_secureRandom = secureRandom;

		m_conf = newConf;
	}

	void activate(Map<String, ?> properties) throws Exception {
		final Configuration conf = new Configuration();
		conf.initialize(properties);

		final KeyManager[] keyManagers = getKeyManagers(conf);
		final TrustManager[] trustManagers = getTrustManagers(conf);
		final SecureRandom secureRandom = getSecureRandom(conf);

		m_keyManagers = keyManagers;
		m_trustManagers = trustManagers;
		m_secureRandom = secureRandom;

		m_conf = conf;
	}

	void deactivate() {
		m_conf = null;

		m_keyManagers = null;
		m_trustManagers = null;
		m_secureRandom = null;
	}

	private KeyManager[] getKeyManagers(Configuration conf) throws Exception {
		final String keyStoreUrl = conf.keyStoreUrl();

		String v = conf.keyStorePassword();
		char[] password = v == null ? EMPTY_CHARARRAY : v.toCharArray();

		final KeyStore ks;
		if (keyStoreUrl == null || keyStoreUrl.length() < 1)
			ks = null;
		else {
			v = conf.keyStoreProvider();
			ks = v == null ? KeyStore.getInstance(conf.keyStoreType()) : KeyStore.getInstance(conf.keyStoreType(), v);

			final File file = new File(keyStoreUrl);
			try (InputStream in = file.exists() ? new FileInputStream(file) : new URL(keyStoreUrl).openStream()) {
				ks.load(new BufferedInputStream(in), password);
			}
		}

		v = conf.keyManagerFactoryProvider();
		KeyManagerFactory kmf = v == null ? KeyManagerFactory.getInstance(conf.keyManagerFactoryAlgorithm())
				: KeyManagerFactory.getInstance(conf.keyManagerFactoryAlgorithm(), v);
		kmf.init(ks, password);

		return kmf.getKeyManagers();
	}

	private TrustManager[] getTrustManagers(Configuration conf) throws Exception {
		final Boolean certValidation = conf.certValidation();
		if (certValidation == null || !certValidation)
			return TrustAll.TRUST_ALL;

		String v;
		final KeyStore ks;
		final String trustStoreUrl = conf.trustStoreUrl();
		if (trustStoreUrl == null || trustStoreUrl.length() < 1)
			ks = null;
		else {
			v = conf.trustStorePassword();
			final char[] password = v == null ? EMPTY_CHARARRAY : v.toCharArray();

			v = conf.trustStoreProvider();
			ks = v == null ? KeyStore.getInstance(conf.trustStoreType()) : KeyStore.getInstance(conf.trustStoreType(),
					v);

			final File file = new File(trustStoreUrl);
			try (InputStream in = file.exists() ? new FileInputStream(file) : new URL(trustStoreUrl).openStream()) {
				ks.load(new BufferedInputStream(in), password);
			}
		}

		v = conf.trustManagerFactoryProvider();
		TrustManagerFactory tmf = v == null ? TrustManagerFactory.getInstance(conf.trustManagerFactoryAlgorithm())
				: TrustManagerFactory.getInstance(conf.trustManagerFactoryAlgorithm(), v);
		tmf.init(ks);

		return tmf.getTrustManagers();
	}

	private SecureRandom getSecureRandom(Configuration conf) throws Exception {
		if (conf.secureRandomAlgorithm() == null)
			return (conf.secureRandomProvider() == null) ? null : SecureRandom.getInstance(null,
					conf.secureRandomProvider());
		else if (conf.secureRandomProvider() == null)
			return SecureRandom.getInstance(conf.secureRandomAlgorithm());
		else
			return SecureRandom.getInstance(conf.secureRandomAlgorithm(), conf.secureRandomProvider());
	}
}
