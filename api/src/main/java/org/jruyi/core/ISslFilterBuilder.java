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

import org.jruyi.io.IBuffer;
import org.jruyi.io.IFilter;
import org.jruyi.io.ISslContextParameters;

/**
 * A builder to build an ssl filter.
 * 
 * @since 2.2
 */
public interface ISslFilterBuilder {

	/**
	 * Defines the possible values for property <i>clientAuth</i>.
	 * 
	 * @since 2.2
	 */
	enum ClientAuth {
		/**
		 * Requests client authentication. But the negotiations can continue if
		 * the client chooses not to provide authentication information.
		 */
		WANT, //
		/**
		 * Requires client authentication. The negotiations will stop if the
		 * client chooses not to provide authentication information.
		 */
		NEED;
	}

	/**
	 * Defines the possible values for property
	 * <i>endpointIdentificationAlgorithm</i>.
	 * 
	 * @since 2.2
	 */
	enum EndpointIdAlg {

		/**
		 * HTTP over TLS, rfc2818.
		 */
		HTTPS, //
		/**
		 * LDAP over TLS, rfc2830.
		 */
		LDAPS;
	}

	/**
	 * Sets the requested SSL protocol. "TLS" will be used if not set.
	 * 
	 * @param protocol
	 *            the standard name of the requested protocol.
	 * @return this builder
	 */
	ISslFilterBuilder protocol(String protocol);

	/**
	 * Sets the name of the SSL provider.
	 * 
	 * @param provider
	 *            the name of the provider
	 * @return this builder
	 */
	ISslFilterBuilder provider(String provider);

	/**
	 * Sets if client authentication is requested/required. This property is
	 * only useful in the server mode.
	 * 
	 * @param clientAuth
	 *            client authentication is requested or required
	 * @return this builder
	 */
	ISslFilterBuilder clientAuth(ClientAuth clientAuth);

	/**
	 * Sets the hostname for SAN verification. And may also be used as hints for
	 * internal SSL session reuse strategy.
	 *
	 * <p>
	 * If hostname is set to null, no SAN verification will be made. If hostname
	 * is set to an empty string and the filter is used for a TCPIP based
	 * service, the remote address's hostname will be used.
	 * 
	 * @param hostname
	 *            the name of the peer host
	 * @return this builder
	 */
	ISslFilterBuilder hostname(String hostname);

	/**
	 * Sets the algorithm to be used for endpoint identification.
	 * 
	 * @param alg
	 *            the algorithm to set
	 * @return this builder
	 */
	ISslFilterBuilder endpointIdentificationAlgorithm(EndpointIdAlg alg);

	/**
	 * Sets the SSL protocol versions enabled for use.
	 * 
	 * @param protocols
	 *            names of all the protocols to enable
	 * @return this builder
	 */
	ISslFilterBuilder enabledProtocols(String[] protocols);

	/**
	 * Sets the cipher suites enabled for use.
	 * 
	 * @param cipherSuites
	 *            names of all the cipher suites to enable
	 * @return this builder
	 */
	ISslFilterBuilder enabledCipherSuites(String[] cipherSuites);

	/**
	 * Sets whether new SSL sessions may be established. If session creations
	 * are not allowed, and there are no existing sessions to resume, there will
	 * be no successful handshaking.
	 * 
	 * @param enableSessionCreation
	 *            true indicates that session may be created otherwise false
	 * @return this builder
	 */
	ISslFilterBuilder enableSessionCreation(boolean enableSessionCreation);

	/**
	 * Sets the SSL context parameters used for creating SSL context.
	 * 
	 * @param sslcp
	 *            the SSL context parameters to set
	 * @return this builder
	 */
	ISslFilterBuilder sslContextParameters(ISslContextParameters sslcp);

	/**
	 * Returns a new SSL filter.
	 * 
	 * @return a new SSL filter
	 * @throws Throwable
	 *             if any error happens
	 */
	IFilter<IBuffer, IBuffer> build() throws Throwable;
}
