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
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.service.command.CommandProcessor;
import org.jruyi.common.StrUtil;
import org.jruyi.me.IRoute;
import org.jruyi.me.IRouteSet;
import org.jruyi.me.IRoutingTable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;

@Service
@Component(name = "jruyi.me.route", policy = ConfigurationPolicy.IGNORE, createPid = false)
@Properties({
		@Property(name = CommandProcessor.COMMAND_SCOPE, value = "route"),
		@Property(name = CommandProcessor.COMMAND_FUNCTION, value = { "clear",
				"delete", "list", "set" }) })
public final class RoutingTable implements IRoutingTable, IRouterManager {

	private static final String ROUTINGTABLE_DIR = "jruyi.me.routingtable.dir";
	private static final String DEFAULT_ROUTINGTABLE_LOCATION = "routingtable";
	private ConcurrentHashMap<String, Router> m_routers;

	static final class NonDirFileFilter implements FileFilter {

		@Override
		public boolean accept(File pathname) {
			return pathname.isFile();
		}
	}

	@Override
	public IRouteSet getRouteSet(String from) {
		return getRouter(from);
	}

	@Override
	public IRouteSet queryRouteSet(String from) {
		if ((from = from.trim()).length() < 1)
			throw new IllegalArgumentException();

		return m_routers.get(from);
	}

	@Override
	public IRouteSet[] getAllRouteSets() {
		return m_routers.values().toArray(new IRouteSet[m_routers.size()]);
	}

	@Override
	public IRouter getRouter(String id) {
		if (id == null || (id = id.trim()).length() < 1)
			throw new IllegalArgumentException();

		Router router = m_routers.get(id);
		if (router != null)
			return router;

		router = new Router(id);
		Router oldRouter = m_routers.putIfAbsent(router.from(), router);
		if (oldRouter != null)
			router = oldRouter;

		return router;
	}

	/**
	 * Creates/Updates a route
	 * 
	 * @param from
	 *            source endpoint
	 * @param to
	 *            destination endpoint
	 * @param args
	 *            [filter]
	 * @throws Exception
	 */
	public void set(String from, String to, String[] args) throws Exception {
		final Filter filter = args != null && args.length > 0 ? FrameworkUtil
				.createFilter(args[0]) : null;
		final IRouteSet router = getRouteSet(from);
		if (filter == null)
			router.setRoute(to);
		else
			router.setRoute(to, filter);

		router.save();
	}

	/**
	 * Lists routes
	 * 
	 * @param args
	 *            [from] [to]
	 * @throws Exception
	 */
	public void list(String[] args) throws Exception {
		if (args == null || args.length < 1) {
			final IRouteSet[] routeSets = getAllRouteSets();
			final ArrayList<IRoute[]> routeSetList = new ArrayList<IRoute[]>(
					routeSets.length);
			IRoute[] routes;
			int n = 0;
			for (IRouteSet routeSet : routeSets) {
				routes = routeSet.getRoutes();
				routeSetList.add(routes);
				n += routes.length;
			}

			routes = new IRoute[n];
			n = 0;
			for (IRoute[] array : routeSetList) {
				System.arraycopy(array, 0, routes, n, array.length);
				n += array.length;
			}

			printRoutes(routes);
		} else if (args.length < 2) {
			IRouteSet routeSet = queryRouteSet(args[0]);
			if (routeSet == null) {
				System.err.print("No Route(s) Found: from=");
				System.err.println(args[0]);
				return;
			}

			printRoutes(routeSet.getRoutes());
		} else {
			IRouteSet routeSet = queryRouteSet(args[0]);
			if (routeSet == null) {
				System.err.print("No Route(s) Found: from=");
				System.err.println(args[0]);
				return;
			}

			IRoute route = routeSet.getRoute(args[1]);
			if (route == null) {
				System.err.print("Route Not Found: from=");
				System.err.print(args[0]);
				System.err.print(", to=");
				System.err.println(args[1]);
				return;
			}

			printRoute(route);
		}
	}

	/**
	 * Deletes route(s)
	 * 
	 * @param from
	 * @param args
	 *            [to]
	 * @throws Exception
	 */
	public void delete(String from, String[] args) throws Exception {
		final IRouteSet routeSet = queryRouteSet(from);
		if (routeSet == null)
			return;

		if (args.length < 1)
			routeSet.clear();
		else
			routeSet.removeRoute(args[0]);

		routeSet.save();
	}

	/**
	 * Clears routing table
	 * 
	 * @throws Exception
	 */
	public void clear() throws Exception {
		IRouteSet[] routeSets = getAllRouteSets();
		for (IRouteSet routeSet : routeSets) {
			routeSet.clear();
			routeSet.save();
		}
	}

	protected void activate(BundleContext bundleContext) throws Exception {
		String location = bundleContext.getProperty(ROUTINGTABLE_DIR);
		File routeTableDir = location == null ? bundleContext
				.getDataFile(DEFAULT_ROUTINGTABLE_LOCATION)
				: new File(location);

		// Fall back to the current working directory
		// if the platform does not have file system support
		if (routeTableDir == null)
			routeTableDir = new File(bundleContext.getProperty("user.dir")
					+ File.separator + DEFAULT_ROUTINGTABLE_LOCATION);

		routeTableDir = routeTableDir.getCanonicalFile();
		if (routeTableDir.exists()) {
			if (!routeTableDir.isDirectory())
				throw new Exception(StrUtil.join(routeTableDir,
						" is not a directory"));
		} else if (!routeTableDir.mkdirs())
			throw new Exception(StrUtil.join("Cannot create directory ",
					routeTableDir));

		Router.setRoutingTableDir(routeTableDir);
		m_routers = load(routeTableDir);
	}

	protected void deactivate() {
		Router.setRoutingTableDir(null);
		m_routers = null;
	}

	private static ConcurrentHashMap<String, Router> load(File routeTableDir)
			throws IOException {
		File[] files = routeTableDir.listFiles(new NonDirFileFilter());
		int size = files.length < 1024 ? 1024 : files.length;
		ConcurrentHashMap<String, Router> routers = new ConcurrentHashMap<String, Router>(
				size);
		for (File file : files) {
			Router router = Router.load(file);
			routers.put(router.from(), router);
		}

		return routers;
	}

	private static void printRoute(IRoute route) {
		System.out.print('[');
		System.out.print(route.from());
		System.out.print("] -> [");
		System.out.print(route.to());
		System.out.print("]: ");
		System.out.println(route.filter());
	}

	private static void printRoutes(IRoute[] routes) {
		for (IRoute route : routes)
			printRoute(route);
	}
}
