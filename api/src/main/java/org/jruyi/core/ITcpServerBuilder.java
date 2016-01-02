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
 * A builder to build an NIO service of TCP server.
 *
 * @since 2.2
 */
public interface ITcpServerBuilder {

	/**
	 * Sets the service ID of the TCP server to be built.
	 *
	 * @param serviceId
	 *            the service ID to set
	 * @return this builder
	 */
	ITcpServerBuilder serviceId(String serviceId);

	/**
	 * Sets the address to bind. Sets {@code null} to bind ANY addresses.
	 *
	 * @param bindAddr
	 *            the address to bind
	 * @return this builder
	 */
	ITcpServerBuilder bindAddr(String bindAddr);

	/**
	 * Sets the port to listen on. This is a mandatory property to build a TCP
	 * server.
	 *
	 * @param port
	 *            the port to listen on
	 * @return this builder
	 * @throws IllegalArgumentException
	 *             if the specified {@code port} is negative or greater than
	 *             65535
	 */
	ITcpServerBuilder port(int port);

	/**
	 * Sets the initial capacity of the map holding TCP connections.
	 * 
	 * @param initCapacityOfChannelMap
	 *            initial capacity to set
	 * @return this builder
	 * @throws IllegalArgumentException
	 *             if the specified {@code initCapacityOfChannelMap} is less
	 *             than 4
	 */
	ITcpServerBuilder initCapacityOfChannelMap(int initCapacityOfChannelMap);

	/**
	 * Sets the maximum length of the queue of incoming connections.
	 * 
	 * @param backlog
	 *            the maximum length of the queue of incoming connections to set
	 * @return this builder
	 * @throws IllegalArgumentException
	 *             if the specified {@code backlog} is less than 1
	 */
	ITcpServerBuilder backlog(Integer backlog);

	/**
	 * Sets {@code true} to reuse address, {@code false} to not.
	 * 
	 * @param reuseAddr
	 *            indicates whether to reuse address
	 * @return this builder
	 */
	ITcpServerBuilder reuseAddr(boolean reuseAddr);

	/**
	 * Sets the number of bytes used to set the socket option SO_RCVBUF. Sets
	 * {@code null} to not set the socket option SO_RCVBUF.
	 *
	 * @param recvBufSize
	 *            the maximum socket receive buffer in bytes
	 * @return this builder
	 * @throws IllegalArgumentException
	 *             if the specified {@code recvBufSize} is less than 1
	 */
	ITcpServerBuilder recvBufSize(Integer recvBufSize);

	/**
	 * Sets the performance preferences.
	 *
	 * @param connectionTime
	 *            indicates the relative importance of short connection time
	 * @param latency
	 *            indicates the relative importance of low latency
	 * @param bandwidth
	 *            indicates the relative importance of high bandwidth
	 * @return this builder
	 */
	ITcpServerBuilder performancePreferences(int connectionTime, int latency, int bandwidth);

	/**
	 * Builds a new TCP server.
	 * 
	 * @param <I>
	 *            the type of the inbound data
	 * @param <O>
	 *            the type of the outbound data
	 * @return a new TCP client
	 */
	<I, O> INioService<I, O, ? extends ITcpServerConfiguration> build();
}
