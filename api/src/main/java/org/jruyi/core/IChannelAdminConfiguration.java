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
 * This interface defines methods to configure channel admin.
 *
 * @since 2.2
 */
public interface IChannelAdminConfiguration {

	/**
	 * Configures the number of selector threads.
	 * 
	 * @param numberOfSelectorThreads
	 *            the number of selector threads to configure
	 * @return this object
	 * @throws IllegalArgumentException
	 *             if {@code numberOfSelectorThreads < 0}
	 */
	IChannelAdminConfiguration numberOfSelectorThreads(Integer numberOfSelectorThreads);

	/**
	 * Configures the number of IO threads.
	 * 
	 * @param numberOfIoThreads
	 *            the number of IO threads to configure
	 * @return this object
	 * @throws IllegalArgumentException
	 *             if {@code numberOfIoThreads < 0}
	 */
	IChannelAdminConfiguration numberOfIoThreads(Integer numberOfIoThreads);

	/**
	 * Configures the capacity of the IO ring buffer.
	 *
	 * @param capacityOfIoRingBuffer
	 *            the capacity of IO ring buffer to configure
	 * @return this object
	 * @throws IllegalArgumentException
	 *             if {@code capacityOfIoRingBuffer < 1}
	 */
	IChannelAdminConfiguration capacityOfIoRingBuffer(int capacityOfIoRingBuffer);

	/**
	 * Returns the number of selector threads, or {@code null} which means the
	 * number is calculated based on the number of CPU cores.
	 * 
	 * @return the number of selector threads
	 */
	Integer numberOfSelectorThreads();

	/**
	 * Returns the number of IO threads, or {@code null} which means the number
	 * is calculated based on the number of CPU cores.
	 * 
	 * @return the number of IO threads
	 */
	Integer numberOfIoThreads();

	/**
	 * Returns the capacity of IO ring buffer.
	 * 
	 * @return the capacity of IO ring buffer
	 */
	int capacityOfIoRingBuffer();
}
