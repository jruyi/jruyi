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

import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.jruyi.io.ISslContextParameters;

public final class DefaultSslContextParameters implements ISslContextParameters {

	public static final ISslContextParameters INST;

	static {
		try {
			INST = new DefaultSslContextParameters();
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	private final TrustManager[] m_trustManagers;

	private DefaultSslContextParameters() throws Exception {
		final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init((KeyStore) null);
		m_trustManagers = tmf.getTrustManagers();
	}

	@Override
	public KeyManager[] getKeyManagers() throws Exception {
		return null;
	}

	@Override
	public TrustManager[] getCertManagers() throws Exception {
		return m_trustManagers;
	}

	@Override
	public SecureRandom getSecureRandom() throws Exception {
		return null;
	}
}
