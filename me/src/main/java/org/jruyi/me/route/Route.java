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
package org.jruyi.me.route;

import java.util.Dictionary;
import java.util.Map;

import org.jruyi.common.IDumpable;
import org.jruyi.common.StringBuilder;
import org.jruyi.common.StrUtil;
import org.jruyi.me.IRoute;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

final class Route implements IRoute, IDumpable {

	static final String FILTER_ALL = "ALL";
	private final Router m_router;
	private final String m_to;
	private Filter m_filter = AlwaysTrueFilter.getInstance();

	static final class AlwaysTrueFilter implements Filter {

		private static final Filter c_inst = new AlwaysTrueFilter();

		static Filter getInstance() {
			return c_inst;
		}

		@Override
		public String toString() {
			return FILTER_ALL;
		}

		@Override
		public boolean match(ServiceReference<?> reference) {
			return true;
		}

		@Override
		public boolean matches(Map<String, ?> map) {
			return true;
		}

		@Override
		public boolean match(@SuppressWarnings("rawtypes") Dictionary dictionary) {
			return true;
		}

		@Override
		public boolean matchCase(
				@SuppressWarnings("rawtypes") Dictionary dictionary) {
			return true;
		}
	}

	Route(Router router, String to) {
		m_router = router;
		m_to = to.intern();
	}

	Route(Router router, String to, String filter)
			throws InvalidSyntaxException {
		this(router, to);
		setFilter(filter);
	}

	Route(Router router, String to, Filter filter) {
		this(router, to);
		filter(filter);
	}

	@Override
	public String filter() {
		return m_filter.toString();
	}

	@Override
	public String from() {
		return m_router.from();
	}

	@Override
	public String to() {
		return m_to;
	}

	@Override
	public boolean isFilterAll() {
		return m_filter == AlwaysTrueFilter.getInstance();
	}

	@Override
	public String toString() {
		return StrUtil.join("Route[(", m_router.from(), ")->(", m_to, "):",
				m_filter, "]");
	}

	@Override
	public void dump(StringBuilder builder) {
		builder.append("Route[(").append(m_router.from()).append(")->(")
				.append(m_to).append("):").append(m_filter).append(']');
	}

	boolean matches(Map<String, ?> routingInfo) {
		return m_filter.matches(routingInfo);
	}

	void setFilter(String filter) throws InvalidSyntaxException {
		if (filter.equals(FILTER_ALL))
			m_filter = AlwaysTrueFilter.getInstance();
		else
			m_filter = FrameworkUtil.createFilter(filter);
	}

	void filter(Filter filter) {
		m_filter = filter;
	}

	Filter getFilter() {
		return m_filter;
	}
}
