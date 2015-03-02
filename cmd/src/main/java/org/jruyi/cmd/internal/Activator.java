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

package org.jruyi.cmd.internal;

import org.apache.felix.service.command.CommandProcessor;
import org.jruyi.cmd.IManual;
import org.jruyi.cmd.conf.Conf;
import org.jruyi.cmd.obr.Obr;
import org.jruyi.common.Properties;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public final class Activator implements BundleActivator {

	private Obr m_obr;

	@Override
	public void start(BundleContext context) throws Exception {
		final Properties properties = new Properties();

		// builtin command
		properties.put(CommandProcessor.COMMAND_SCOPE, "builtin");
		properties.put(CommandProcessor.COMMAND_FUNCTION, Builtin.commands());
		context.registerService(Builtin.class.getName(), Builtin.INST, properties);

		// ruyi command
		RuyiCmd.INST.context(context);
		properties.put(CommandProcessor.COMMAND_SCOPE, "jruyi");
		properties.put(CommandProcessor.COMMAND_FUNCTION, RuyiCmd.commands());
		context.registerService(IManual.class.getName(), RuyiCmd.INST, properties);

		// bundle command
		properties.put(CommandProcessor.COMMAND_SCOPE, "bundle");
		properties.put(CommandProcessor.COMMAND_FUNCTION, BundleCmd.commands());
		context.registerService(BundleCmd.class.getName(), new BundleCmd(context), properties);

		// conf command
		properties.put(CommandProcessor.COMMAND_SCOPE, "conf");
		properties.put(CommandProcessor.COMMAND_FUNCTION, Conf.commands());
		context.registerService(Conf.class.getName(), new Conf(context), properties);

		// Obr command
		final Obr obr = new Obr(context);
		obr.open();
		m_obr = obr;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		final Obr obr = m_obr;
		if (obr != null) {
			m_obr = null;
			obr.close();
		}

		RuyiCmd.INST.context(null);
	}
}
