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

import org.jruyi.common.ITimerAdmin;
import org.jruyi.core.internal.RuyiCoreProvider;
import org.jruyi.io.IFilter;

/**
 * A holder for Scheduler and TimeoutAdmin services. It also provides factory
 * methods for builders of NIO services.
 * 
 * @since 2.2
 */
public final class RuyiCore {

	private static final IRuyiCore c_ruyiCore = RuyiCoreProvider.getInstance().ruyiCore();

	/**
	 * This interface defines all the methods that a RuyiCore provider has to
	 * implement. It is used to separate the implementation of the provider from
	 * the API module.
	 * 
	 * @since 2.2
	 */
	public interface IRuyiCore {

		/**
		 * Returns the configuration to configure the ChannalAdmin.
		 * 
		 * @return the configuration to configure the ChannelAdmin
		 */
		IChannelAdminConfiguration channelAdminConfiguration();

		/**
		 * Returns the default buffer factory service.
		 * 
		 * @return the default buffer factory service
		 */
		IBufferFactory defaultBufferFactory();

		/**
		 * Creates a new buffer factory with the specified {@code name}.
		 * 
		 * @param name
		 *            the name of the buffer factory to be created
		 * @return a new buffer factory
		 * @throws IllegalArgumentException
		 *             if the specified {@code name} is {@code null} or empty
		 */
		IBufferFactory newBufferFactory(String name);

		/**
		 * Returns the Scheduler service.
		 * 
		 * @return the Scheduler service
		 */
		IScheduler getScheduler();

		/**
		 * Returns the TimerAdmin service.
		 * 
		 * @return the TimerAdmin service
		 * @since 2.3
		 */
		ITimerAdmin getTimerAdmin();

		/**
		 * Returns the filter of MsgLog.
		 * 
		 * @return the filter of MsgLog
		 */
		IFilter<?, ?> getMsgLogFilter();

		/**
		 * Returns a builder to build a text line filter.
		 * 
		 * @return a builder of text line filter
		 */
		ITextLineFilterBuilder newTextLineFilterBuilder();

		/**
		 * Returns a builder of ssl filter.
		 * 
		 * @return a builder of ssl filter
		 */
		ISslFilterBuilder newSslFilterBuilder();

		/**
		 * Returns a builder to build an {@code ISslContextParameters} from key
		 * store files.
		 * 
		 * @return a builder of {@code ISslContextParameters}
		 */
		IFileKeyStoreBuilder newFileKeyStoreBuilder();

		/**
		 * Returns a builder of TCP server.
		 * 
		 * @return a builder of TCP server
		 */
		ITcpServerBuilder newTcpServerBuilder();

		/**
		 * Returns a builder of TCP client.
		 * 
		 * @return a builder of TCP client
		 */
		ITcpClientBuilder newTcpClientBuilder();

		/**
		 * Returns a builder of UDP server.
		 * 
		 * @return a builder of UDP server
		 */
		IUdpServerBuilder newUdpServerBuilder();

		/**
		 * Returns a builder of UDP client.
		 * 
		 * @return a builder of UDP client
		 */
		IUdpClientBuilder newUdpClientBuilder();
	}

	private RuyiCore() {
	}

	/**
	 * Returns the configuration to configure the ChannalAdmin.
	 *
	 * @return the configuration to configure the ChannelAdmin
	 */
	public static IChannelAdminConfiguration channelAdminConfiguration() {
		return c_ruyiCore.channelAdminConfiguration();
	}

	/**
	 * Returns the default buffer factory service.
	 *
	 * @return the default buffer factory service
	 */
	public static IBufferFactory defaultBufferFactory() {
		return c_ruyiCore.defaultBufferFactory();
	}

	/**
	 * Creates a new buffer factory with the specified {@code name}.
	 *
	 * @param name
	 *            the name of the buffer factory to be created
	 * @return a new buffer factory
	 * @throws IllegalArgumentException
	 *             if the specified {@code name} is {@code null} or empty
	 */
	public static IBufferFactory newBufferFactory(String name) {
		return c_ruyiCore.newBufferFactory(name);
	}

	/**
	 * Returns the Scheduler service.
	 *
	 * @return the Scheduler service
	 */
	public static IScheduler getScheduler() {
		return c_ruyiCore.getScheduler();
	}

	/**
	 * Returns the TimerAdmin service.
	 * 
	 * @return the TimerAdmin service
	 * @since 2.3
	 */
	public static ITimerAdmin getTimerAdmin() {
		return c_ruyiCore.getTimerAdmin();
	}

	/**
	 * Returns the filter of MsgLog.
	 *
	 * @return the filter of MsgLog
	 */
	public static IFilter<?, ?> getMsgLogFilter() {
		return c_ruyiCore.getMsgLogFilter();
	}

	/**
	 * Returns a builder to build a text line filter.
	 *
	 * @return a builder of text line filter
	 */
	public static ITextLineFilterBuilder newTextLineFilterBuilder() {
		return c_ruyiCore.newTextLineFilterBuilder();
	}

	/**
	 * Returns a builder of ssl filter.
	 *
	 * @return a builder of ssl filter
	 */
	public static ISslFilterBuilder newSslFilterBuilder() {
		return c_ruyiCore.newSslFilterBuilder();
	}

	/**
	 * Returns a builder to build {@code ISslContextParameters} from key store
	 * files.
	 *
	 * @return a builder of {@code ISslContextParameters}
	 */
	public static IFileKeyStoreBuilder newFileKeyStoreBuilder() {
		return c_ruyiCore.newFileKeyStoreBuilder();
	}

	/**
	 * Returns a builder of TCP server.
	 *
	 * @return a builder of TCP server
	 */
	public static ITcpServerBuilder newTcpServerBuilder() {
		return c_ruyiCore.newTcpServerBuilder();
	}

	/**
	 * Returns a builder of TCP client.
	 *
	 * @return a builder of TCP client
	 */
	public static ITcpClientBuilder newTcpClientBuilder() {
		return c_ruyiCore.newTcpClientBuilder();
	}

	/**
	 * Returns a builder of UDP server.
	 *
	 * @return a builder of UDP server
	 */
	public static IUdpServerBuilder newUdpServerBuilder() {
		return c_ruyiCore.newUdpServerBuilder();
	}

	/**
	 * Returns a builder of UDP client.
	 *
	 * @return a builder of UDP client
	 */
	public static IUdpClientBuilder newUdpClientBuilder() {
		return c_ruyiCore.newUdpClientBuilder();
	}
}
