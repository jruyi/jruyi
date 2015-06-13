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

import org.jruyi.common.internal.ServiceHolderManagerProvider;
import org.osgi.framework.BundleContext;

/**
 * This is the factory class for {@code IServiceHolderManager}.
 * 
 * @see IServiceHolderManager
 */
public final class ServiceHolderManager {

	private static final IFactory c_factory = ServiceHolderManagerProvider.getInstance().getFactory();

	/**
	 * A factory to create {@code IServiceHolderManager} objects. It is used to
	 * separate the implementation provider from the API module.
	 */
	public interface IFactory {

		/**
		 * Creates a new {@code IServiceHolderManager} object.
		 * 
		 * @param <T>
		 *            the type of the service to be hold
		 * @param context
		 *            a bundle context bound to the service holder manager for
		 *            getting services.
		 * @param clazz
		 *            the class of services to be managed
		 * @param nameOfId
		 *            the name of the service property used as the service ID
		 * @return a service holder manager
		 */
		<T> IServiceHolderManager<T> create(BundleContext context, Class<T> clazz, String nameOfId);
	}

	private ServiceHolderManager() {
	}

	/**
	 * Creates a new {@code IServiceHolderManager} object.
	 * 
	 * @param <T>
	 *            the type of the service to be hold
	 * @param context
	 *            a bundle context bound to the service holder manager for
	 *            getting services.
	 * @param clazz
	 *            the class of services to be managed
	 * @param nameOfId
	 *            the name of the service property used as the service ID
	 * @return a service holder manager
	 */
	public static <T> IServiceHolderManager<T> newInstance(BundleContext context, Class<T> clazz, String nameOfId) {
		return c_factory.create(context, clazz, nameOfId);
	}
}
