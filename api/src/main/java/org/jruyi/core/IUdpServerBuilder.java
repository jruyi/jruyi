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
 * A builder to build an NIO service of UDP server.
 * 
 * @since 2.2
 */
public interface IUdpServerBuilder {

	/**
	 * Sets the service ID of the UDP server to be built.
	 *
	 * @param serviceId
	 *            the service ID to set
	 * @return this builder
	 */
	IUdpServerBuilder serviceId(String serviceId);

	/**
	 * Sets the address to bind. Sets {@code null} to bind ANY addresses.
	 *
	 * @param bindAddr
	 *            the address to bind
	 * @return this builder
	 */
	IUdpServerBuilder bindAddr(String bindAddr);

	/**
	 * Sets the port to listen on. This is a mandatory property to build a UDP
	 * server.
	 *
	 * @param port
	 *            the port to listen on
	 * @return this builder
	 * @throws IllegalArgumentException
	 *             if the specified {@code port} is negative or greater than
	 *             65535
	 */
	IUdpServerBuilder port(int port);

	/**
	 * Sets the initial capacity of the map holding UDP channels.
	 *
	 * @param initCapacityOfChannelMap
	 *            initial capacity to set
	 * @return this builder
	 * @throws IllegalArgumentException
	 *             if the specified {@code initCapacityOfChannelMap} is less
	 *             than 4
	 */
	IUdpServerBuilder initCapacityOfChannelMap(int initCapacityOfChannelMap);

	/**
	 * Builds a new UDP server.
	 *
	 * @param <I>
	 *            the type of the inbound data
	 * @param <O>
	 *            the type of the outbound data
	 * @return a new UDP client
	 */
	<I, O> INioService<I, O, ? extends IUdpServerConfiguration> build();
}
