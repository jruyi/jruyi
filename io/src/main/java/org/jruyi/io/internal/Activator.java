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

package org.jruyi.io.internal;

import org.apache.felix.service.command.CommandProcessor;
import org.jruyi.common.Properties;
import org.jruyi.io.IFilter;
import org.jruyi.io.cmd.IoCommand;
import org.jruyi.io.msglog.MsgLogFilter;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public final class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		final Properties properties = new Properties();
		properties.put(CommandProcessor.COMMAND_SCOPE, "io");
		properties.put(CommandProcessor.COMMAND_FUNCTION, IoCommand.commands());
		context.registerService(IoCommand.class.getName(), new IoCommand(context), properties);

		context.registerService(IFilter.class.getName(), MsgLogFilter.INST, MsgLogFilter.getProperties());
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}
}
