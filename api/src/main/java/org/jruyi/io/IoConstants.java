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

/**
 * Defines standard names for IO constants.
 */
public final class IoConstants {

	/**
	 * The service property name of I/O service id.
	 */
	public static final String SERVICE_ID = "jruyi.io.service.id";
	/**
	 * The service property name of filter ID.
	 */
	public static final String FILTER_ID = "jruyi.io.filter.id";
	/**
	 * The service property name of {@code ISslContextParameters} ID.
	 */
	public static final String SSLCP_ID = "jruyi.io.sslcp.id";
	/**
	 * The component name of tcpserver factory component.
	 */
	public static final String CN_TCPSERVER_FACTORY = "jruyi.io.tcpserver.factory";
	/**
	 * The component name of udpserver factory component.
	 */
	public static final String CN_UDPSERVER_FACTORY = "jruyi.io.udpserver.factory";
	/**
	 * The component name of tcpclient shortconn factory component.
	 */
	public static final String CN_TCPCLIENT_SHORTCONN_FACTORY = "jruyi.io.tcpclient.shortconn.factory";
	/**
	 * The component name of tcpclient connpool factory component.
	 */
	public static final String CN_TCPCLIENT_CONNPOOL_FACTORY = "jruyi.io.tcpclient.connpool.factory";
	/**
	 * The component name of tcpclient multiplexing connpool factory component.
	 * 
	 * @since 2.0
	 */
	public static final String CN_TCPCLIENT_MUX_CONNPOOL_FACTORY = "jruyi.io.tcpclient.mux.connpool.factory";
	/**
	 * The component name of tcpclient factory component.
	 */
	public static final String CN_TCPCLIENT_FACTORY = "jruyi.io.tcpclient.factory";
	/**
	 * The component name of tcpclient multiplexing factory component.
	 * 
	 * @since 2.0
	 */
	public static final String CN_TCPCLIENT_MUX_FACTORY = "jruyi.io.tcpclient.mux.factory";
	/**
	 * The component name of udpclient factory component.
	 */
	public static final String CN_UDPCLIENT_FACTORY = "jruyi.io.udpclient.factory";
	/**
	 * The filter ID of msglog filter.
	 */
	public static final String FID_MSGLOG = "jruyi.io.msglog.filter";
	/**
	 * The filter ID of the default SSL filter.
	 *
	 * @since 2.2
	 */
	public static final String FID_DEFAULT_SSL = "jruyi.io.ssl.default.filter";
	/**
	 * The component name of filekeystore ssl filter factory component.
	 *
	 * @since 2.2
	 */
	public static final String CN_SSL_FKS_FILTER_FACTORY = "jruyi.io.ssl.fks.filter.factory";
	/**
	 * The component name of ssl filter factory component.
	 * 
	 * @since 2.2
	 */
	public static final String CN_SSL_FILTER_FACTORY = "jruyi.io.ssl.filter.factory";

	private IoConstants() {
	}
}
