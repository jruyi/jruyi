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

package org.jruyi.core.internal;

import java.util.ArrayList;
import java.util.Arrays;

import org.jruyi.core.IFilterChain;
import org.jruyi.io.IBuffer;
import org.jruyi.io.IFilter;
import org.jruyi.io.IFilterOutput;
import org.jruyi.io.ISession;
import org.jruyi.io.filter.IFilterList;
import org.jruyi.io.filter.IFilterManager;

final class FilterChain implements IFilterChain, IFilterManager, IFilterList {

	private static final FilterWrapper<?, ?>[] EMPTY = new FilterWrapper[0];
	private ArrayList<FilterWrapper<?, ?>> m_filterList = new ArrayList<>();
	private FilterWrapper<?, ?>[] m_filters = EMPTY;

	static final class FilterWrapper<I, O> implements IFilter<I, O> {

		private final String m_name;
		private final IFilter<I, O> m_filter;

		FilterWrapper(String name, IFilter<I, O> filter) {
			if (name == null)
				throw new NullPointerException("Argument name cannot be null");
			if (filter == null)
				throw new NullPointerException(("Argument filter cannot be null"));

			m_name = name;
			m_filter = filter;
		}

		@Override
		public int msgMinSize() {
			return m_filter.msgMinSize();
		}

		@Override
		public int tellBoundary(ISession session, IBuffer in) {
			return m_filter.tellBoundary(session, in);
		}

		@Override
		public boolean onMsgArrive(ISession session, I msg, IFilterOutput output) {
			return m_filter.onMsgArrive(session, msg, output);
		}

		@Override
		public boolean onMsgDepart(ISession session, O msg, IFilterOutput output) {
			return m_filter.onMsgDepart(session, msg, output);
		}

		public boolean isName(String name) {
			return m_name.equals(name);
		}

		public IFilter<I, O> filter() {
			return m_filter;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || !(obj.getClass() != FilterWrapper.class))
				return false;
			return m_name.equals(((FilterWrapper<?, ?>) obj).m_name);
		}

		@Override
		public int hashCode() {
			return m_name.hashCode();
		}
	}

	@Override
	public IFilterChain addAfter(String targetFilterName, String name, IFilter<?, ?> filter) {
		final ArrayList<FilterWrapper<?, ?>> filterList = m_filterList;
		if (filterList != null) {
			final int i = findFilter(filterList, targetFilterName);
			if (i < 0)
				throw new IllegalArgumentException("Filter not found: " + targetFilterName);
			filterList.add(i + 1, new FilterWrapper<>(name, filter));
		} else {
			final FilterWrapper<?, ?>[] filters = m_filters;
			int i = findFilter(filters, targetFilterName);
			if (i < 0)
				throw new IllegalArgumentException("Filter not found: " + targetFilterName);
			final int n = filters.length + 1;
			final FilterWrapper<?, ?>[] newFilters = new FilterWrapper[n];
			System.arraycopy(filters, 0, newFilters, 0, ++i);
			newFilters[i] = new FilterWrapper<>(name, filter);
			if (i < n - 1)
				System.arraycopy(filters, i, newFilters, i + 1, n - i);
		}
		return this;
	}

	@Override
	public IFilterChain addBefore(String targetFilterName, String name, IFilter<?, ?> filter) {
		final ArrayList<FilterWrapper<?, ?>> filterList = m_filterList;
		if (filterList != null) {
			final int i = findFilter(filterList, targetFilterName);
			if (i < 0)
				throw new IllegalArgumentException("Filter not found: " + targetFilterName);
			filterList.add(i, new FilterWrapper<>(name, filter));
		} else {
			final FilterWrapper<?, ?>[] filters = m_filters;
			final int i = findFilter(filters, targetFilterName);
			if (i < 0)
				throw new IllegalArgumentException("Filter not found: " + targetFilterName);
			final int n = filters.length;
			final FilterWrapper<?, ?>[] newFilters = new FilterWrapper[n + 1];
			System.arraycopy(filters, 0, newFilters, 0, i);
			newFilters[i] = new FilterWrapper<>(name, filter);
			System.arraycopy(filters, i, newFilters, i + 1, n - i);
		}
		return this;
	}

	@Override
	public IFilterChain addFirst(String name, IFilter<?, ?> filter) {
		final ArrayList<FilterWrapper<?, ?>> filterList = m_filterList;
		if (filterList != null)
			filterList.add(0, new FilterWrapper<>(name, filter));
		else {
			final FilterWrapper<?, ?>[] filters = m_filters;
			final int n = filters.length;
			final FilterWrapper<?, ?>[] newFilters = new FilterWrapper[n + 1];
			System.arraycopy(filters, 0, newFilters, 1, n);
			newFilters[0] = new FilterWrapper<>(name, filter);
			m_filters = newFilters;
		}
		return this;
	}

	@Override
	public IFilterChain addLast(String name, IFilter<?, ?> filter) {
		final ArrayList<FilterWrapper<?, ?>> filterList = m_filterList;
		if (filterList != null)
			filterList.add(new FilterWrapper<>(name, filter));
		else {
			final FilterWrapper<?, ?>[] filters = m_filters;
			final int n = filters.length;
			final FilterWrapper<?, ?>[] newFilters = Arrays.copyOf(filters, n + 1);
			newFilters[n] = new FilterWrapper<>(name, filter);
			m_filters = newFilters;
		}
		return this;
	}

	@Override
	public IFilter<?, ?> replace(String name, IFilter<?, ?> newFilter) {
		IFilter<?, ?> oldFilter = null;
		final ArrayList<FilterWrapper<?, ?>> filterList = m_filterList;
		if (filterList == null) {
			final FilterWrapper<?, ?>[] filters = m_filters;
			final int i = findFilter(filters, name);
			if (i >= 0) {
				oldFilter = filters[i].filter();
				filters[i] = new FilterWrapper<>(name, newFilter);
			}
		} else {
			final int i = findFilter(filterList, name);
			if (i >= 0)
				oldFilter = filterList.set(i, new FilterWrapper<>(name, newFilter)).filter();
		}
		return oldFilter;
	}

	@Override
	public IFilter<?, ?> remove(String name) {
		final ArrayList<FilterWrapper<?, ?>> filterList = m_filterList;
		if (filterList == null) {
			final FilterWrapper<?, ?>[] filters = m_filters;
			final int i = findFilter(filters, name);
			if (i < 0)
				return null;
			else {
				final int n = filters.length - 1;
				final FilterWrapper<?, ?>[] newFilters = new FilterWrapper<?, ?>[n];
				System.arraycopy(filters, 0, newFilters, 0, i);
				if (n > i)
					System.arraycopy(filters, i + 1, newFilters, i, n - i);
				m_filters = newFilters;
				return filters[i].filter();
			}
		} else {
			final int i = findFilter(filterList, name);
			return i < 0 ? null : filterList.remove(i).filter();
		}
	}

	@Override
	public boolean contains(String name) {
		final ArrayList<FilterWrapper<?, ?>> filterList = m_filterList;
		if (filterList == null)
			return findFilter(m_filters, name) >= 0;
		else
			return findFilter(filterList, name) >= 0;
	}

	@Override
	public IFilter<?, ?> get(String name) {
		final ArrayList<FilterWrapper<?, ?>> filterList = m_filterList;
		if (filterList == null) {
			final FilterWrapper<?, ?>[] filters = m_filters;
			final int i = findFilter(filters, name);
			return i < 0 ? null : filters[i].filter();
		} else {
			final int i = findFilter(filterList, name);
			return i < 0 ? null : filterList.get(i).filter();
		}
	}

	@Override
	public void clear() {
		final ArrayList<FilterWrapper<?, ?>> filterList = m_filterList;
		if (filterList != null)
			filterList.clear();
		m_filters = EMPTY;
	}

	@Override
	public IFilter<?, ?>[] filters() {
		return m_filters;
	}

	@Override
	public IFilterList getFilters(String[] filterIds) {
		final ArrayList<FilterWrapper<?, ?>> filterList = m_filterList;
		m_filters = filterList.toArray(new FilterWrapper[filterList.size()]);
		m_filterList = null;
		return this;
	}

	@Override
	public void ungetFilters(String[] filterIds) {
		m_filters = EMPTY;
		m_filterList = new ArrayList<>();
	}

	private static int findFilter(FilterWrapper<?, ?>[] filters, String name) {
		final int n = filters.length;
		for (int i = 0; i < n; ++i) {
			if (filters[i].isName(name))
				return i;
		}
		return -1;
	}

	private static int findFilter(ArrayList<FilterWrapper<?, ?>> filterList, String name) {
		final int n = filterList.size();
		for (int i = 0; i < n; ++i) {
			final FilterWrapper<?, ?> filter = filterList.get(i);
			if (filter.isName(name))
				return i;
		}
		return -1;
	}
}
