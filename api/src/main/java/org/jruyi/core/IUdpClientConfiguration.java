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

/**
 * This interface defines methods to configure a UDP client service.
 * 
 * @since 2.2
 */
public interface IUdpClientConfiguration {

	/**
	 * Returns the hostname or address of remote peer.
	 * 
	 * @return the hostname or address of remote peer
	 */
	String host();

	/**
	 * Returns the port.
	 * 
	 * @return the port
	 */
	int port();

	/**
	 * Returns the traffic class or type-of-service octet to be set in the IP
	 * header, or {@code null} if not configured.
	 *
	 * @return the traffic class or type-of-service octet
	 */
	Integer trafficClass();

	/**
	 * Sets the traffic class or type-of-service octet to be set in the IP
	 * header. Sets {@code null} to not configure.
	 *
	 * @param trafficClass
	 *            the traffic class or type-of-service octet to be set in the IP
	 *            header
	 * @return this configuration
	 */
	IUdpClientConfiguration trafficClass(Integer trafficClass);

	/**
	 * Returns whether to enable SO_BROADCAST.
	 * 
	 * @return true if to enable SO_BROADCAST, otherwise false
	 */
	boolean broadcast();

	/**
	 * Sets {@code true} to enable SO_BROADCAST; set {@code false} to disable
	 * SO_BROADCAST.
	 * 
	 * @param broadcast
	 *            indicates whether to enable SO_BROADCAST
	 * @return this configuration
	 */
	IUdpClientConfiguration broadcast(boolean broadcast);

	/**
	 * Applies this configuration to the associated UDP client service.
	 * 
	 * @throws Throwable
	 *             if any error happens
	 */
	void apply() throws Throwable;
}
