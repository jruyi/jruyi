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
 * A builder to build an NIO serivce of UDP client.
 * 
 * @since 2.2
 */
public interface IUdpClientBuilder {

	/**
	 * Sets the service ID of the UDP client to be built.
	 *
	 * @param serviceId
	 *            the service ID to set
	 * @return this builder
	 */
	IUdpClientBuilder serviceId(String serviceId);

	/**
	 * Sets the address of the remote peer to communicate to. This is a
	 * mandatory property to build a UDP client.
	 *
	 * @param host
	 *            the address of the remote peer to set
	 * @return this builder
	 */
	IUdpClientBuilder host(String host);

	/**
	 * Sets the port of the remote peer to communicate to. This is a mandatory
	 * property to build a UDP client.
	 *
	 * @param port
	 *            the port to set
	 * @return this builder
	 * @throws IllegalArgumentException
	 *             if the specified {@code port} is negative or greater than
	 *             65535
	 * 
	 */
	IUdpClientBuilder port(int port);

	/**
	 * Builds a new UDP client.
	 * 
	 * @param <I>
	 *            the type of the inbound data
	 * @param <O>
	 *            the type of the outbound data
	 * @return a new UDP client
	 */
	<I, O> INioService<I, O, ? extends IUdpClientConfiguration> build();
}
