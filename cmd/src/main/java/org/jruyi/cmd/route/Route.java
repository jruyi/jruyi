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
package org.jruyi.cmd.route;

import java.util.ArrayList;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Descriptor;
import org.jruyi.me.IRoute;
import org.jruyi.me.IRouteSet;
import org.jruyi.me.IRoutingTable;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;

@Service(Route.class)
@Component(name = "jruyi.cmd.route", policy = ConfigurationPolicy.IGNORE, createPid = false)
@Properties({
		@Property(name = CommandProcessor.COMMAND_SCOPE, value = "route"),
		@Property(name = CommandProcessor.COMMAND_FUNCTION, value = { "clear",
				"delete", "list", "set" }) })
public final class Route {

	@Reference(name = "routingTable")
	private IRoutingTable m_rt;

	@Descriptor("Create/Update a route")
	public void set(@Descriptor("source endpoint") String from,
			@Descriptor("destination endpoint") String to,
			@Descriptor("[filter]") String[] args) throws Exception {

		Filter filter = args != null && args.length > 0 ? FrameworkUtil
				.createFilter(args[0]) : null;
		IRouteSet router = m_rt.getRouteSet(from);
		if (filter == null)
			router.setRoute(to);
		else
			router.setRoute(to, filter);

		router.save();
	}

	@Descriptor("List route(s)")
	public void list(@Descriptor("[from] [to]") String[] args) throws Exception {
		if (args == null || args.length < 1) {
			IRouteSet[] routeSets = m_rt.getAllRouteSets();
			ArrayList<IRoute[]> routeSetList = new ArrayList<IRoute[]>(
					routeSets.length);
			IRoute[] routes = null;
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
			IRouteSet routeSet = m_rt.queryRouteSet(args[0]);
			if (routeSet == null) {
				System.err.print("No Route(s) Found: from=");
				System.err.println(args[0]);
				return;
			}

			printRoutes(routeSet.getRoutes());
		} else {
			IRouteSet routeSet = m_rt.queryRouteSet(args[0]);
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

	@Descriptor("Delete route(s)")
	public void delete(@Descriptor("<from>") String from,
			@Descriptor("[to]") String[] args) throws Exception {
		IRouteSet routeSet = m_rt.queryRouteSet(from);
		if (routeSet == null)
			return;

		if (args.length < 1)
			routeSet.clear();
		else
			routeSet.removeRoute(args[1]);

		routeSet.save();
	}

	@Descriptor("Clear routing table")
	public void clear() throws Exception {
		IRouteSet[] routeSets = m_rt.getAllRouteSets();
		for (IRouteSet routeSet : routeSets) {
			routeSet.clear();
			routeSet.save();
		}
	}

	protected void bindRoutingTable(IRoutingTable rt) {
		m_rt = rt;
	}

	protected void unbindRoutingTable(IRoutingTable rt) {
		m_rt = null;
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
