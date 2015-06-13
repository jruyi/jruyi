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

package org.jruyi.common;

/**
 * Service for tracking and managing services of type {@code T}.
 * 
 * @param <T>
 *            the type of the tracked service
 * 
 * @see ServiceHolderManager
 */
public interface IServiceHolderManager<T> {

	/**
	 * Opens this manager to start tracking services.
	 */
	void open();

	/**
	 * Gets the service holder holding the service identified by the specified
	 * {@code id}. The use count for this holder is incremented by one.
	 * 
	 * @param id
	 *            the ID of the held service
	 * @return the service holder holding the requested service
	 */
	IServiceHolder<T> getServiceHolder(String id);

	/**
	 * Ungets the service holder holding the service identified by the specified
	 * {@code id}. The use count for this holder is decremented by one.
	 * 
	 * @param id
	 *            the ID of the held service
	 * @return the service holder that used to hold the service named the
	 *         specified {@code name}
	 */
	IServiceHolder<T> ungetServiceHolder(String id);

	/**
	 * Closes this manager to stop tracking services.
	 */
	void close();
}
