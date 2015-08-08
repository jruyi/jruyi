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

import org.jruyi.core.IFileKeyStoreBuilder;
import org.jruyi.io.ISslContextParameters;
import org.jruyi.io.ssl.FileKeyStore;

final class FileKeyStoreBuilder implements IFileKeyStoreBuilder {

	private final HashMap<String, Object> m_properties = new HashMap<>(16);

	public FileKeyStoreBuilder() {
		m_properties.put("certValidation", Boolean.TRUE);
	}

	@Override
	public IFileKeyStoreBuilder keyStoreUrl(String keyStoreUrl) {
		if (keyStoreUrl == null)
			m_properties.remove("keyStoreUrl");
		else
			m_properties.put("keyStoreUrl", keyStoreUrl);
		return this;
	}

	@Override
	public IFileKeyStoreBuilder keyStoreProvider(String keyStoreProvider) {
		if (keyStoreProvider == null)
			m_properties.remove("keyStoreProvider");
		else
			m_properties.put("keyStoreProvider", keyStoreProvider);
		return this;
	}

	@Override
	public IFileKeyStoreBuilder keyStoreType(String keyStoreType) {
		if (keyStoreType == null)
			m_properties.remove("keyStoreType");
		else
			m_properties.put("keyStoreType", keyStoreType);
		return this;
	}

	@Override
	public IFileKeyStoreBuilder keyStorePassword(String keyStorePassword) {
		if (keyStorePassword == null)
			m_properties.remove("keyStorePassword");
		else
			m_properties.put("keyStorePassword", keyStorePassword);
		return this;
	}

	@Override
	public IFileKeyStoreBuilder keyPassword(String keyPassword) {
		if (keyPassword == null)
			m_properties.remove("keyPassword");
		else
			m_properties.put("keyPassword", keyPassword);
		return this;
	}

	@Override
	public IFileKeyStoreBuilder keyManagerFactoryAlgorithm(String keyManagerFactoryAlgorithm) {
		if (keyManagerFactoryAlgorithm == null)
			m_properties.remove("keyManagerFactoryAlgorithm");
		else
			m_properties.put("keyManagerFactoryAlgorithm", keyManagerFactoryAlgorithm);
		return this;
	}

	@Override
	public IFileKeyStoreBuilder keyManagerFactoryProvider(String keyManagerFactoryProvider) {
		if (keyManagerFactoryProvider == null)
			m_properties.remove("keyManagerFactoryProvider");
		else
			m_properties.put("keyManagerFactoryProvider", keyManagerFactoryProvider);
		return this;
	}

	@Override
	public IFileKeyStoreBuilder certValidation(boolean certValidation) {
		m_properties.put("certValidation", certValidation);
		return this;
	}

	@Override
	public IFileKeyStoreBuilder trustStoreUrl(String trustStoreUrl) {
		if (trustStoreUrl == null)
			m_properties.remove("trustStoreUrl");
		else
			m_properties.put("trustStoreUrl", trustStoreUrl);
		return this;
	}

	@Override
	public IFileKeyStoreBuilder trustStoreProvider(String trustStoreProvider) {
		if (trustStoreProvider == null)
			m_properties.remove("trustStoreProvider");
		else
			m_properties.put("trustStoreProvider", trustStoreProvider);
		return this;
	}

	@Override
	public IFileKeyStoreBuilder trustStoreType(String trustStoreType) {
		if (trustStoreType == null)
			m_properties.remove("trustStoreType");
		else
			m_properties.put("trustStoreType", trustStoreType);
		return this;
	}

	@Override
	public IFileKeyStoreBuilder trustStorePassword(String trustStorePassword) {
		if (trustStorePassword == null)
			m_properties.remove("trustStorePassword");
		else
			m_properties.put("trustStorePassword", trustStorePassword);
		return null;
	}

	@Override
	public IFileKeyStoreBuilder trustManagerFactoryAlgorithm(String trustManagerFactoryAlgorithm) {
		if (trustManagerFactoryAlgorithm == null)
			m_properties.remove("trustManagerFactoryAlgorithm");
		else
			m_properties.put("trustManagerFactoryAlgorithm", trustManagerFactoryAlgorithm);
		return this;
	}

	@Override
	public IFileKeyStoreBuilder trustManagerFactoryProvider(String trustManagerFactoryProvider) {
		if (trustManagerFactoryProvider == null)
			m_properties.remove("trustManagerFactoryProvider");
		else
			m_properties.put("trustManagerFactoryProvider", trustManagerFactoryProvider);
		return this;
	}

	@Override
	public ISslContextParameters build() throws Throwable {
		final FileKeyStore fks = new FileKeyStore();
		fks.activate(m_properties);
		return fks;
	}
}
