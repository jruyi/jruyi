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

import org.jruyi.common.IIdentifiable;

/**
 * A builder to build an NIO service of TCP client. There are five types of TCP
 * client: Conn, MuxConn, ShortConn, ConnPool and MuxConnPool.
 * <ul>
 * <li>Conn - A TCP client service that creates TCP connections to be managed by
 * consumer code</li>
 * <li>MuxConn - A TCP client that creates TCP connections to be
 * multiplexed/managed by consumer code</li>
 * <li>ShortConn - A TCP client that initiates and closes a TCP connection for
 * every single transaction</li>
 * <li>ConnPool - A TCP client that pools TCP connections to reuse</li>
 * <li>MuxConnPool - A TCP client that pools TCP connections able to be
 * multiplexed</li>
 * </ul>
 * 
 * @since 2.2
 */
public interface ITcpClientBuilder {

	/**
	 * Sets the service ID of the TCP client to be built.
	 *
	 * @param serviceId
	 *            the service ID to set
	 * @return this builder
	 */
	ITcpClientBuilder serviceId(String serviceId);

	/**
	 * Sets the address of the remote peer to connect to. This is a mandatory
	 * property to build a TCP client.
	 * 
	 * @param host
	 *            the address of the remote peer to set
	 * @return this builder
	 */
	ITcpClientBuilder host(String host);

	/**
	 * Sets the port of the remote peer to connect to. This is a mandatory
	 * property to build a TCP client.
	 * 
	 * @param port
	 *            the port to set
	 * @return this builder
	 * @throws IllegalArgumentException
	 *             if the specified {@code port} is negative or greater than
	 *             65535
	 */
	ITcpClientBuilder port(int port);

	/**
	 * Sets the initial capacity of the map holding TCP connections.
	 * 
	 * @param initCapacityOfChannelMap
	 *            initial capacity to set
	 * @return this builder
	 */
	ITcpClientBuilder initCapacityOfChannelMap(int initCapacityOfChannelMap);

	/**
	 * Builds a TCP client of type Conn.
	 * 
	 * @param <I>
	 *            the type of inbound data
	 * @param <O>
	 *            the type of outbound data
	 * @return a new TCP client
	 */
	<I, O> INioService<I, O, ? extends ITcpClientConfiguration> buildConn();

	/**
	 * Builds a TCP client of type MuxConn.
	 * 
	 * @param <I>
	 *            the type of the inbound data
	 * @param <O>
	 *            the type of the outbound data
	 * @return a new TCP client
	 */
	<I extends IIdentifiable<?>, O extends IIdentifiable<?>> INioService<I, O, ? extends ITcpClientConfiguration> buildMuxConn();

	/**
	 * Builds a TCP client of type ShortConn.
	 * 
	 * @param <I>
	 *            the type of the inbound data
	 * @param <O>
	 *            the type of the outbound data
	 * @return a new TCP client
	 */
	<I, O> INioService<I, O, ? extends ITcpClientConfiguration> buildShortConn();

	/**
	 * Builds a TCP client of type ConnPool.
	 * 
	 * @param <I>
	 *            the type of inbound data
	 * @param <O>
	 *            the type of outbound data
	 * @return a new TCP client
	 */
	<I, O> INioService<I, O, ? extends ITcpConnPoolConfiguration> buildConnPool();

	/**
	 * Builds a TCP client of type MuxConnPool.
	 * 
	 * @param <I>
	 *            the type of the inbound data
	 * @param <O>
	 *            the type of the outbound data
	 * @return a new TCP client
	 */
	<I extends IIdentifiable<?>, O extends IIdentifiable<?>> INioService<I, O, ? extends ITcpConnPoolConfiguration> buildMuxConnPool();
}
