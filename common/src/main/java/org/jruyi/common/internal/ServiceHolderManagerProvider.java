/**
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
package org.jruyi.common.internal;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.jruyi.common.IServiceHolderManager;
import org.jruyi.common.ServiceHolderManager.IFactory;
import org.jruyi.common.StrUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public final class ServiceHolderManagerProvider implements IFactory {

	private static final ServiceHolderManagerProvider c_inst = new ServiceHolderManagerProvider();

	static final class NameHolder {

		String m_name;

		NameHolder(String name) {
			m_name = name;
		}
	}

	static final class ServiceHolderTracker<T> extends
			ServiceTracker<T, NameHolder> implements IServiceHolderManager<T> {

		private final String m_nameOfId;
		private final ReentrantLock m_lock;
		private final HashMap<String, ServiceHolder<T>> m_holders;

		ServiceHolderTracker(BundleContext context, Filter filter,
				String nameOfId) {
			super(context, filter, null);
			m_nameOfId = nameOfId;
			m_lock = new ReentrantLock();
			m_holders = new HashMap<String, ServiceHolder<T>>(128);
		}

		@Override
		public NameHolder addingService(ServiceReference<T> reference) {
			String name = getName(reference);
			ServiceHolder<T> holder = getServiceHolder(name);
			holder.add(reference);
			return new NameHolder(name);
		}

		@Override
		public void modifiedService(ServiceReference<T> reference,
				NameHolder nameHolder) {
			String name = getName(reference);
			if (nameHolder.m_name.equals(name))
				return;

			ServiceHolder<T> holder = ungetServiceHolder(nameHolder.m_name);
			holder.remove(reference);

			holder = getServiceHolder(name);
			holder.add(reference);
			nameHolder.m_name = name;
		}

		@Override
		public void removedService(ServiceReference<T> reference,
				NameHolder nameHolder) {
			String name = nameHolder.m_name;
			ServiceHolder<T> holder = ungetServiceHolder(name);
			holder.remove(reference);
		}

		@Override
		public ServiceHolder<T> getServiceHolder(String name) {
			final HashMap<String, ServiceHolder<T>> holders = m_holders;
			final ReentrantLock lock = m_lock;
			lock.lock();
			try {
				ServiceHolder<T> holder = holders.get(name);
				if (holder == null) {
					holder = new ServiceHolder<T>(name, context);
					holders.put(name, holder);
				}
				holder.incRef();
				return holder;
			} finally {
				lock.unlock();
			}
		}

		@Override
		public ServiceHolder<T> ungetServiceHolder(String name) {
			final HashMap<String, ServiceHolder<T>> holders = m_holders;
			final ReentrantLock lock = m_lock;
			lock.lock();
			try {
				ServiceHolder<T> holder = holders.get(name);
				if (holder == null)
					throw new RuntimeException(StrUtil.join(
							"Unexpected ungetServiceHolder(", name, ')'));
				int count = holder.decRef();
				if (count == 0)
					holders.remove(name);
				else if (count == 1)
					holder.deactivate();

				return holder;

			} finally {
				lock.unlock();
			}
		}

		private String getName(ServiceReference<T> reference) {
			return reference.getProperty(m_nameOfId).toString();
		}
	}

	private ServiceHolderManagerProvider() {
	}

	public static ServiceHolderManagerProvider getInstance() {
		return c_inst;
	}

	public IFactory getFactory() {
		return this;
	}

	@Override
	public <T> IServiceHolderManager<T> create(BundleContext context,
			Class<T> clazz, String nameOfId) {
		try {
			Filter filter = FrameworkUtil.createFilter(StrUtil.join("(&("
					+ Constants.OBJECTCLASS + "=", clazz.getName(), ")(",
					nameOfId, "=*))"));
			return new ServiceHolderTracker<T>(context, filter, nameOfId);
		} catch (InvalidSyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
