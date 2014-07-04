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
package org.jruyi.io.filter;

import org.jruyi.common.IServiceHolderManager;
import org.jruyi.common.ServiceHolderManager;
import org.jruyi.io.IFilter;
import org.jruyi.io.IoConstants;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@Component(name = "jruyi.io.filter", //
configurationPolicy = ConfigurationPolicy.IGNORE, //
xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class FilterManager implements IFilterManager {

	private static final IFilter<?, ?>[] EMPTY = new IFilter[0];

	@SuppressWarnings("rawtypes")
	private IServiceHolderManager<IFilter> m_manager;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public IFilter<?, ?>[] getFilters(String[] filterIds) {
		int n = filterIds.length;
		if (n < 1)
			return EMPTY;

		final IServiceHolderManager<IFilter> manager = m_manager;
		IFilter<?, ?>[] filters = new IFilter[n];
		for (int i = 0; i < n; ++i)
			filters[i] = new FilterDelegator(
					manager.getServiceHolder(filterIds[i]));

		return filters;
	}

	@Override
	public void ungetFilters(String[] filterIds) {
		@SuppressWarnings("rawtypes")
		final IServiceHolderManager<IFilter> manager = m_manager;
		for (String filterId : filterIds)
			manager.ungetServiceHolder(filterId);
	}

	protected void activate(BundleContext context) {
		@SuppressWarnings("rawtypes")
		final IServiceHolderManager<IFilter> manager = ServiceHolderManager
				.newInstance(context, IFilter.class, IoConstants.FILTER_ID);
		manager.open();
		m_manager = manager;
	}

	protected void deactivate() {
		m_manager.close();
	}
}
