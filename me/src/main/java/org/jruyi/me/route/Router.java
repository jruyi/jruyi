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
package org.jruyi.me.route;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.jruyi.common.StrUtil;
import org.jruyi.me.IRoute;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

final class Router implements IRouter {

	private static final Route[] NONE = new Route[0];
	private static File s_routingTableDir;
	private final ArrayList<Route> m_routeList;
	private final String m_from;
	private final ReentrantLock m_lock;
	private Route[] m_routes = NONE;
	private boolean m_modified;

	Router(String from) {
		m_from = from.intern();
		m_routeList = new ArrayList<Route>();
		m_lock = new ReentrantLock();
	}

	static Router load(File from) throws IOException {
		Router router = new Router(from.getName());
		ArrayList<Route> routeList = router.m_routeList;
		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(new FileInputStream(from));
			for (int size = in.readInt(); size > 0; --size) {
				String to = (String) in.readObject();
				String filter = (String) in.readObject();
				routeList.add(new Route(router, to, filter));
			}
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		} catch (InvalidSyntaxException e) {
			throw new IOException(e);
		} finally {
			if (in != null)
				in.close();
		}

		return router;
	}

	static void setRoutingTableDir(File routingTableDir) {
		s_routingTableDir = routingTableDir;
	}

	@Override
	public IRoute route(IRoutable routable) {
		Route[] res = routes();
		Map<String, ?> routingInfo = routable.getRoutingInfo();
		for (Route entry : res) {
			if (entry.matches(routingInfo))
				return entry;
		}

		return null;
	}

	@Override
	public void clear() {
		final ArrayList<Route> routeList = m_routeList;
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			if (routeList.size() < 1)
				return;

			routeList.clear();
			m_routes = NONE;
			m_modified = true;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public IRoute[] getRoutes() {
		return routes();
	}

	@Override
	public IRoute getRoute(String to) {
		if (to == null || (to = to.trim()).length() < 1)
			throw new IllegalArgumentException();

		return getRouteInternal(to);
	}

	@Override
	public IRoute setRoute(String to, String filter)
			throws InvalidSyntaxException {
		return setRoute(to, FrameworkUtil.createFilter(filter));
	}

	@Override
	public IRoute setRoute(String to) {
		return setRoute(to, Route.AlwaysTrueFilter.getInstance());
	}

	@Override
	public IRoute setRoute(String to, Filter filter) {
		if (to == null || (to = to.trim()).length() < 1)
			throw new IllegalArgumentException();

		Route route = null;
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			route = getRouteInternal(to);
			if (route == null) {
				route = new Route(this, to, filter);
				m_routeList.add(route);
				m_routes = NONE;
				m_modified = true;
			} else if (!route.filter().equals(filter)) {
				route.filter(filter);
				m_modified = true;
			}
		} finally {
			lock.unlock();
		}

		return route;
	}

	@Override
	public String from() {
		return m_from;
	}

	@Override
	public void removeRoute(String to) {
		if (to == null || (to = to.trim()).length() < 1)
			throw new IllegalArgumentException();

		ArrayList<Route> routeList = m_routeList;
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			int n = routeList.size();
			for (int i = 0; i < n; ++i) {
				if (to.equals(routeList.get(i).to())) {
					m_routeList.remove(i);
					m_routes = NONE;
					m_modified = true;
					break;
				}
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void save() throws IOException {
		final ArrayList<Route> routeList = m_routeList;
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			if (!m_modified)
				return;

			int size = routeList.size();
			if (m_routes == NONE)
				m_routes = routeList.toArray(new Route[size]);

			File file = new File(s_routingTableDir, m_from);
			if (size < 1) {
				if (file.exists() && !file.delete())
					throw new IOException(StrUtil.join(
							"Failed to delete file: ", file.getCanonicalPath()));
				return;
			}

			ObjectOutputStream out = new ObjectOutputStream(
					new FileOutputStream(file));
			try {
				out.writeInt(size);
				Route[] routes = m_routes;
				for (Route route : routes) {
					out.writeObject(route.to());
					out.writeObject(route.filter());
				}
			} finally {
				out.close();
			}

			m_modified = false;
		} finally {
			lock.unlock();
		}
	}

	private Route[] routes() {
		Route[] routes = m_routes;
		if (routes != NONE)
			return routes;

		final ArrayList<Route> routeList = m_routeList;
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			if (m_routes == NONE)
				m_routes = routeList.toArray(new Route[routeList.size()]);

			return m_routes;
		} finally {
			lock.unlock();
		}
	}

	private Route getRouteInternal(String to) {
		ArrayList<Route> routeList = m_routeList;
		int n = routeList.size();
		for (int i = 0; i < n; ++i) {
			Route route = routeList.get(i);
			if (to.equals(route.to()))
				return route;
		}

		return null;
	}
}
