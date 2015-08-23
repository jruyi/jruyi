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
 * This interface defines methods to configure a TCP server service.
 *
 * @since 2.2
 */
public interface ITcpServerConfiguration {

	/**
	 * Returns the bound address or {@code null} if bound to ANY addresses.
	 *
	 * @return the bound address
	 */
	String bindAddr();

	/**
	 * Returns the port.
	 * 
	 * @return the port
	 */
	int port();

	/**
	 * Returns the maximum length of the queue of incoming connections or
	 * {@code null} if not configured.
	 *
	 * @return the maximum length of the queue of incoming connections
	 */
	Integer backlog();

	/**
	 * Returns whether to reuse addresses.
	 * 
	 * @return true if reusing addresses, otherwise false
	 */
	boolean reuseAddr();

	/**
	 * Returns the socket buffer size for receiving, or {@code null} if not
	 * configured.
	 * 
	 * @return the socket buffer size for receiving
	 */
	Integer recvBufSize();

	/**
	 * Returns the performance preferences(connection-time, latency and
	 * bandwidth in order) to set, or {@code null} if not configured.
	 * 
	 * @return the performance preferences to set
	 */
	int[] performancePreferences();

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
	ITcpServerConfiguration throttle(long throttle);

	/**
	 * Returns the timeout in seconds for idle connections.
	 * 
	 * @return the idle timeout in seconds
	 */
	int sessionIdleTimeoutInSeconds();

	/**
	 * Sets the timeout in seconds for idle connections. Sets 0 to not allow
	 * idle connections. Sets -1 to never time out.
	 *
	 * @param sessionIdleTimeoutInSeconds
	 *            the idle timeout in seconds to set
	 * @return this configuration
	 * @throws IllegalArgumentException
	 *             if the specified {@code idleTimeoutInSeconds} is less than -1
	 */
	ITcpServerConfiguration sessionIdleTimeoutInSeconds(int sessionIdleTimeoutInSeconds);

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
	ITcpServerConfiguration keepAlive(Boolean keepAlive);

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
	ITcpServerConfiguration soLinger(Integer soLinger);

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
	ITcpServerConfiguration sendBufSize(Integer sendBufSize);

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
	ITcpServerConfiguration tcpNoDelay(Boolean tcpNoDelay);

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
	ITcpServerConfiguration trafficClass(Integer trafficClass);

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
	ITcpServerConfiguration oobInline(Boolean oobInline);

	/**
	 * Applies this configuration to the associated TCP server service.
	 *
	 * @throws Throwable
	 *             if any error happens
	 */
	void apply() throws Throwable;
}
