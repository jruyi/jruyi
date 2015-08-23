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
 * A buffer factory to create and manage buffers.
 *
 * @since 2.2
 */
public interface IBufferFactory extends org.jruyi.io.IBufferFactory {

	/**
	 * This interface defines methods to configure a buffer factory.
	 * 
	 * @since 2.2
	 */
	interface IConfiguration {

		/**
		 * Configures the capacity of a buffer unit.
		 *
		 * @param unitCapacity
		 *            the capacity to configure
		 * @return this object
		 * @throws IllegalArgumentException
		 *             if {@code unitCapacity < 8}
		 */
		IConfiguration unitCapacity(int unitCapacity);

		/**
		 * Returns the capacity of a buffer unit.
		 * 
		 * @return the capacity of a buffer unit
		 */
		int unitCapacity();

		/**
		 * Applies this configuration to the associated buffer factory.
		 */
		void apply();
	}

	/**
	 * Returns the name of this buffer factory, or {@code null} if this one is
	 * the default buffer factory of RuyiCore.
	 * 
	 * @return the name of this buffer factory
	 */
	String name();

	/**
	 * Returns the configuration of this buffer factory.
	 * 
	 * @return the configuration of this buffer factory
	 */
	IConfiguration configuration();
}
