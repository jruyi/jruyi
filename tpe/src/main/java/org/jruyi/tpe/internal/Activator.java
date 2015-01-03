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
package org.jruyi.tpe.internal;

import org.apache.felix.service.command.CommandProcessor;
import org.jruyi.common.Properties;
import org.jruyi.tpe.cmd.ExecutorCommand;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public final class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		final Properties properties = new Properties();
		properties.put(CommandProcessor.COMMAND_SCOPE, "tpe");
		properties.put(CommandProcessor.COMMAND_FUNCTION, ExecutorCommand.commands());
		context.registerService(ExecutorCommand.class.getName(), new ExecutorCommand(context), properties);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}
}
