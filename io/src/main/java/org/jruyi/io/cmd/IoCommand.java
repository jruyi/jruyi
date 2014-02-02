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
package org.jruyi.io.cmd;

import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.service.command.CommandProcessor;
import org.jruyi.common.IService;
import org.jruyi.common.StrUtil;
import org.jruyi.io.IoConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

@Service(IoCommand.class)
@Component(name = "jruyi.io.cmd", policy = ConfigurationPolicy.IGNORE, createPid = false)
@Properties({
		@Property(name = CommandProcessor.COMMAND_SCOPE, value = "io"),
		@Property(name = CommandProcessor.COMMAND_FUNCTION, value = { "list",
				"start", "stop" }) })
public final class IoCommand {

	private BundleContext m_context;

	public void start(String serviceId) throws Exception {
		IService service = getService(serviceId);
		if (service == null) {
			System.err.print("IO Service Not Found: ");
			System.err.println(serviceId);
			return;
		}

		service.start();
	}

	public void start(String serviceId, int options) throws Exception {
		IService service = getService(serviceId);
		if (service == null) {
			System.err.print("IO Service Not Found: ");
			System.err.println(serviceId);
			return;
		}

		service.start(options);
	}

	public void stop(String serviceId) throws Exception {
		IService service = getService(serviceId);
		if (service == null) {
			System.err.print("IO Service Not Found: ");
			System.err.println(serviceId);
			return;
		}

		service.stop();
	}

	public void stop(String serviceId, int options) throws Exception {
		IService service = getService(serviceId);
		if (service == null) {
			System.err.print("IO Service Not Found: ");
			System.err.println(serviceId);
			return;
		}

		service.stop(options);
	}

	public void list() throws Exception {
		System.out.println("[        Address        ][  State  ][ "
				+ IoConstants.SERVICE_ID + " ]");
		final BundleContext context = m_context;
		Collection<ServiceReference<IService>> references = context
				.getServiceReferences(IService.class, "("
						+ IoConstants.SERVICE_ID + "=*)");
		for (ServiceReference<IService> reference : references) {
			IService service = context.getService(reference);
			String addr = (String) reference.getProperty("addr");
			if (addr == null) {
				addr = (String) reference.getProperty("bindAddr");
				if (addr == null)
					addr = "*";
			}
			String port = String.valueOf(reference.getProperty("port"));
			// Address
			int n = 25 - addr.length() - port.length() - 1;
			int x = n >> 1;
			printFill(' ', x);
			System.out.print(addr);
			System.out.print('.');
			System.out.print(port);
			x = n - x;
			printFill(' ', x);
			// State
			System.out.print(getState(service.state()));
			// jruyi.io.service.id
			System.out.println(reference.getProperty(IoConstants.SERVICE_ID));
		}
	}

	public void list(String arg) throws Exception {
		final BundleContext context = m_context;
		Collection<ServiceReference<IService>> references = context
				.getServiceReferences(IService.class, StrUtil.join("("
						+ IoConstants.SERVICE_ID + "=", arg, ")"));
		if (references.isEmpty())
			return;

		final ServiceReference<IService> reference = references.iterator()
				.next();
		IService service = context.getService(reference);

		// service id
		System.out.print(IoConstants.SERVICE_ID);
		System.out.print(": ");
		System.out.println(reference.getProperty(IoConstants.SERVICE_ID));

		// state
		System.out.print("State: ");
		System.out.println(state(service.state()));

		// other properties
		String[] keys = reference.getPropertyKeys();
		for (String key : keys) {
			if (key.equals(IoConstants.SERVICE_ID))
				continue;
			System.out.print(key);
			System.out.print(": ");
			Object v = reference.getProperty(key);
			if (v.getClass().isArray())
				v = StrUtil.deeplyBuild(v);
			System.out.println(v);
		}
	}

	protected void activate(BundleContext context) {
		m_context = context;
	}

	protected void deactivate() {
		m_context = null;
	}

	private static String state(int state) {
		switch (state) {
		case IService.ACTIVE:
			return "Active";
		case IService.INACTIVE:
			return "Inactive";
		case IService.STOPPED:
			return "Stopped";
		case IService.STARTING:
			return "Starting";
		case IService.STOPPING:
			return "Stopping";
		default:
			return "Unknown";
		}
	}

	private static String getState(int state) {
		switch (state) {
		case IService.ACTIVE:
			return " Active     ";
		case IService.INACTIVE:
			return " Inactive   ";
		case IService.STOPPED:
			return " Stopped    ";
		case IService.STARTING:
			return " Starting   ";
		case IService.STOPPING:
			return " Stopping   ";
		default:
			return " Unknown    ";
		}
	}

	private IService getService(String serviceId) throws Exception {
		final BundleContext context = m_context;
		Collection<ServiceReference<IService>> references = context
				.getServiceReferences(IService.class, StrUtil.join("("
						+ IoConstants.SERVICE_ID + "=", serviceId, ")"));
		if (references.isEmpty())
			return null;

		final ServiceReference<IService> reference = references.iterator()
				.next();
		IService service = context.getService(reference);
		return service;
	}

	private static void printFill(char c, int count) {
		while (--count >= 0)
			System.out.print(c);
	}
}
