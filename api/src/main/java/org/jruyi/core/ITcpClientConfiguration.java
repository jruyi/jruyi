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
 * This interface defines methods to configure a TCP client service.
 *
 * @since 2.2
 */
public interface ITcpClientConfiguration {

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
	 * Returns the throttle.
	 * 
	 * @return the throttle
	 */
	long throttle();

	/**
	 * Sets the throttle.
	 * 
	 * @param throttle
	 *            the throttle to set
	 * @return this configuration
	 * @throws IllegalArgumentException
	 *             if the specified {@code throttle} is less than -1
	 */
	ITcpClientConfiguration throttle(long throttle);

	/**
	 * Returns the timeout in seconds on establishing a TCP connection
	 * 
	 * @return the connect timeout in seconds
	 */
	int connectTimeoutInSeconds();

	/**
	 * Sets the timeout in seconds on establishing a TCP connection. Sets 0 to
	 * never time out.
	 * 
	 * @param connectTimeoutInSeconds
	 *            the timeout to set
	 * @return this configuration
	 * @throws IllegalArgumentException
	 *             if the specified {@code connectTimeoutInSeconds} is negative
	 */
	ITcpClientConfiguration connectTimeoutInSeconds(int connectTimeoutInSeconds);

	/**
	 * Returns the timeout in seconds on reading.
	 * 
	 * @return the read timeout in seconds
	 */
	int readTimeoutInSeconds();

	/**
	 * Sets the timeout in seconds on reading. Sets 0 to not expect a response.
	 * Sets -1 to never time out.
	 * 
	 * @param readTimeoutInSeconds
	 *            the timeout to set
	 * @return this configuration
	 * @throws IllegalArgumentException
	 *             if the specified {@code readTimeoutInSeconds} is less than -1
	 */
	ITcpClientConfiguration readTimeoutInSeconds(int readTimeoutInSeconds);

	/**
	 * Returns whether address is reused.
	 * 
	 * @return true if address is reused, otherwise false
	 */
	boolean reuseAddr();

	/**
	 * Sets true to allow to reuse addresses.
	 * 
	 * @param reuseAddr
	 *            indicates whether to reuse addresses
	 * @return this configuration
	 */
	ITcpClientConfiguration reuseAddr(boolean reuseAddr);

	/**
	 * Returns whether to set socket option SO_KEEPALIVE.
	 * 
	 * @return true if SO_KEEPALIVE will be set, false if will not, null if not
	 *         set
	 */
	Boolean keepAlive();

	/**
	 * Sets whether to set socket option SO_KEEPALIVE. Sets {@code null} to not
	 * set the socket option SO_KEEPALIVE.
	 * 
	 * @param keepAlive
	 *            indicates whether to set socket option SO_KEEPALIVE
	 * @return this configuration
	 */
	ITcpClientConfiguration keepAlive(Boolean keepAlive);

	/**
	 * Returns how many seconds to linger for as configured.
	 * 
	 * @return seconds to linger for if configured
	 */
	Integer soLinger();

	/**
	 * Sets the seconds to linger for. Sets {@code null} to not set the socket
	 * option SO_LINGER. behavior.
	 * 
	 * @param soLinger
	 *            the seconds to linger for
	 * @return this configuration
	 * @throws IllegalArgumentException
	 *             if the specified {@code soLinger} is negative
	 */
	ITcpClientConfiguration soLinger(Integer soLinger);

	/**
	 * Returns the socket buffer size for receiving, or {@code null} if not
	 * configured.
	 * 
	 * @return the socket buffer size for receiving
	 */
	Integer recvBufSize();

	/**
	 * Sets the number of bytes used to set the socket option SO_RCVBUF. Sets
	 * {@code null} to not set the socket option SO_RCVBUF.
	 * 
	 * @param recvBufSize
	 *            the maximum socket receive buffer in bytes
	 * @return this configuration
	 * @throws IllegalArgumentException
	 *             if the specified {@code recvBufSize} is less than 1
	 */
	ITcpClientConfiguration recvBufSize(Integer recvBufSize);

	/**
	 * Returns the socket buffer size for sending, or {@code null} if not
	 * configured.
	 * 
	 * @return the socket buffer size for sending
	 */
	Integer sendBufSize();

	/**
	 * Sets the number of bytes used to set the socket option SO_SNDBUF. Sets
	 * {@code null} to not set the socket option SO_SNDBUF.
	 * 
	 * @param sendBufSize
	 *            the maximum socket send buffer in bytes
	 * @return this configuration
	 * @throws IllegalArgumentException
	 *             if the specified {@code sendBufSize} is less than 1
	 */
	ITcpClientConfiguration sendBufSize(Integer sendBufSize);

	/**
	 * Returns whether to disable Nagle's Algorithm, or {@code null} if not
	 * configured
	 * 
	 * @return true if to disable Nagle's Algorithm, false if not to, or
	 *         {@code null} if not configured
	 */
	Boolean tcpNoDelay();

	/**
	 * Sets whether to disable Nagle's Algorithm. Sets {@code null} to not
	 * configure.
	 * 
	 * @param tcpNoDelay
	 *            indicates whether to disable Nagle's Algorithm
	 * @return this configuration
	 */
	ITcpClientConfiguration tcpNoDelay(Boolean tcpNoDelay);

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
	ITcpClientConfiguration trafficClass(Integer trafficClass);

	/**
	 * Returns whether to enable OOBINLINE(receipt of TCP urgent data), or
	 * {@code null} if not configured.
	 * 
	 * @return true if to enable OOBINLINE, false if not to, or {@code null} if
	 *         not configured
	 */
	Boolean oobInline();

	/**
	 * Sets whether to enable OOBINLINE(receipt of TCP urgent data). Sets
	 * {@code null} to not configure.
	 * 
	 * @param oobInline
	 *            indicates whether to enable OOBINLINE
	 * @return this configuration
	 */
	ITcpClientConfiguration oobInline(Boolean oobInline);

	/**
	 * Returns the performance preferences(connection-time, latency and
	 * bandwidth in order) to set, or {@code null} if not configured.
	 * 
	 * @return the performance preferences to set
	 */
	int[] performancePreferences();

	/**
	 * Sets the performance preferences.
	 * 
	 * @param connectionTime
	 *            indicates the relative importance of short connection time
	 * @param latency
	 *            indicates the relative importance of low latency
	 * @param bandwidth
	 *            indicates the relative importance of high bandwidth
	 * @return this configuration
	 */
	ITcpClientConfiguration performancePreferences(int connectionTime, int latency, int bandwidth);

	/**
	 * Unsets the performance preferences.
	 * 
	 * @return this configuration
	 */
	ITcpClientConfiguration unsetPerformancePreferences();

	/**
	 * Applies this configuration to the associated TCP client service.
	 * 
	 * @throws Throwable
	 *             if any error happens
	 */
	void apply() throws Throwable;
}
