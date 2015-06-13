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

package org.jruyi.io;

import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

/**
 * Service for providing parameters needed to initialize an {@code SSLContext}.
 */
public interface ISslContextParameters {

	/**
	 * Returns key managers for initializing an {@code SSLContext}.
	 * 
	 * @return key managers
	 * @throws Exception
	 *             If any error happens
	 */
	KeyManager[] getKeyManagers() throws Exception;

	/**
	 * Returns trust managers for initializing an {@code SSLContext}.
	 * 
	 * @return trust managers
	 * @throws Exception
	 *             If any error happens
	 */
	TrustManager[] getCertManagers() throws Exception;

	/**
	 * Returns a {@code SecureRandom} for initializing an {@code SSLContext}.
	 * 
	 * @return a {@code SecureRandom} object
	 * @throws Exception
	 *             If any error happens
	 */
	SecureRandom getSecureRandom() throws Exception;
}
