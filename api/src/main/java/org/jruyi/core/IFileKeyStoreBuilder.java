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

package org.jruyi.core;

import org.jruyi.io.ISslContextParameters;

/**
 * A builder to build a new {@link ISslContextParameters} backed by file-based
 * key/trust stores which are used to initialize an SSL context.
 *
 * @since 2.2
 */
public interface IFileKeyStoreBuilder {

	/**
	 * Sets the URL of the key store.
	 * 
	 * @param keyStoreUrl
	 *            the URL of the key store
	 * @return this builder
	 */
	IFileKeyStoreBuilder keyStoreUrl(String keyStoreUrl);

	/**
	 * Sets the provider of the key store implementation.
	 *
	 * @param keyStoreProvider
	 *            the provider of the key store implementation
	 * @return this builder
	 */
	IFileKeyStoreBuilder keyStoreProvider(String keyStoreProvider);

	/**
	 * Sets the type of the key store.
	 * 
	 * @param keyStoreType
	 *            the type of the key store
	 * @return this builder
	 */
	IFileKeyStoreBuilder keyStoreType(String keyStoreType);

	/**
	 * Sets the password of the key store.
	 * 
	 * @param keyStorePassword
	 *            the password of the key store
	 * @return this builder
	 */
	IFileKeyStoreBuilder keyStorePassword(String keyStorePassword);

	/**
	 * Sets the password of the key.
	 * 
	 * @param keyPassword
	 *            the password of the key
	 * @return this builder
	 */
	IFileKeyStoreBuilder keyPassword(String keyPassword);

	/**
	 * Sets the algorithm of the key manager factory.
	 * 
	 * @param keyManagerFactoryAlgorithm
	 *            the algorithm of the key manager factory
	 * @return this builder
	 */
	IFileKeyStoreBuilder keyManagerFactoryAlgorithm(String keyManagerFactoryAlgorithm);

	/**
	 * Sets the provider of the key manager factory implementation.
	 * 
	 * @param keyManagerFactoryProvider
	 *            the provider of the key manager factory implementation
	 * @return this builder
	 */
	IFileKeyStoreBuilder keyManagerFactoryProvider(String keyManagerFactoryProvider);

	/**
	 * Sets whether to validate the certificate chain.
	 * 
	 * @param certValidation
	 *            whether to validate the certificate chain
	 * @return this builder
	 */
	IFileKeyStoreBuilder certValidation(boolean certValidation);

	/**
	 * Sets the URL of the trust store.
	 * 
	 * @param trustStoreUrl
	 *            the URL of the trust store
	 * @return this builder
	 */
	IFileKeyStoreBuilder trustStoreUrl(String trustStoreUrl);

	/**
	 * Sets the provider of the trust store implementation
	 * 
	 * @param trustStoreProvider
	 *            the provider of the trust store implementation
	 * @return this builder
	 */
	IFileKeyStoreBuilder trustStoreProvider(String trustStoreProvider);

	/**
	 * Sets the type of the trust store.
	 * 
	 * @param trustStoreType
	 *            the type of the trust store
	 * @return this builder
	 */
	IFileKeyStoreBuilder trustStoreType(String trustStoreType);

	/**
	 * Sets the password of the trust store
	 * 
	 * @param trustStorePassword
	 *            the password of the trust store
	 * @return this builder
	 */
	IFileKeyStoreBuilder trustStorePassword(String trustStorePassword);

	/**
	 * Sets the algorithm of the trust manager factory
	 * 
	 * @param trustManagerFactoryAlgorithm
	 *            the algorithm of the trust manager factory
	 * @return this builder
	 */
	IFileKeyStoreBuilder trustManagerFactoryAlgorithm(String trustManagerFactoryAlgorithm);

	/**
	 * Sets the provider of the trust manager factory
	 * 
	 * @param trustManagerFactoryProvider
	 *            the provider of the trust manager factory
	 * @return this builder
	 */
	IFileKeyStoreBuilder trustManagerFactoryProvider(String trustManagerFactoryProvider);

	/**
	 * Builds and returns a new {@link ISslContextParameters} backed by
	 * file-based key/trust stores.
	 * 
	 * @return a new {@link ISslContextParameters}
	 * @throws Throwable
	 *             if any error happens when building
	 */
	ISslContextParameters build() throws Throwable;
}
