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

package org.jruyi.tpe.cmd;

import org.jruyi.tpe.IExecutorProfiler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public final class ExecutorCommand {

	private final BundleContext m_context;

	public ExecutorCommand(BundleContext context) {
		m_context = context;
	}

	public static String[] commands() {
		return new String[] { "info", "profiling" };
	}

	public void profiling(String action) throws Exception {
		boolean start = false;
		if (action.equals("start"))
			start = true;
		else if (!action.equals("stop")) {
			System.err.println("Unknown action: " + action);
			return;
		}

		final IExecutorProfiler profiler = getExecutorProfiler();
		if (profiler == null) {
			System.err.println("Thread pool executor is not up");
			return;
		}

		if (start)
			profiler.startProfiling();
		else
			profiler.stopProfiling();
	}

	public void info() throws Exception {

		final IExecutorProfiler profiler = getExecutorProfiler();
		if (profiler == null) {
			System.err.println("Thread pool executor is not up");
			return;
		}

		boolean isProfiling = profiler.isProfiling();
		printPropName("          CorePoolSize: ", isProfiling);
		System.out.println(profiler.getCorePoolSize());

		printPropName("           MaxPoolSize: ", isProfiling);
		System.out.println(profiler.getMaxPoolSize());

		printPropName("KeepAliveTimeInSeconds: ", isProfiling);
		System.out.println(profiler.getKeepAliveTime());

		printPropName("         QueueCapacity: ", isProfiling);
		System.out.println(profiler.getQueueCapacity());

		printPropName("       CurrentPoolSize: ", isProfiling);
		System.out.println(profiler.getCurrentPoolSize());

		printPropName("    CurrentQueueLength: ", isProfiling);
		System.out.println(profiler.getCurrentQueueLength());

		printPropName("           InProfiling: ", isProfiling);

		if (!isProfiling) {
			System.out.println('N');
			return;
		} else
			System.out.println('Y');

		System.out.print("         NumberOfRequestsRetired: ");
		System.out.println(profiler.getNumberOfRequestsRetired());

		System.out.print("   AverageNumberOfActiveRequests: ");
		System.out.println(profiler.getEstimatedAverageNumberOfActiveRequests());

		System.out.print("             AverageResponseTime: ");
		System.out.println(profiler.getAverageResponseTime());

		System.out.print("              AverageServiceTime: ");
		System.out.println(profiler.getAverageServiceTime());

		System.out.print("        AverageTimeWaitingInPool: ");
		System.out.println(profiler.getAverageTimeWaitingInPool());

		System.out.print("RatioOfActiveRequestsToCoreCount: ");
		System.out.println(profiler.getRatioOfActiveRequestsToCoreCount());

		System.out.print("   RatioOfDeadTimeToResponseTime: ");
		System.out.println(profiler.getRatioOfDeadTimeToResponseTime());

		System.out.print("  RequestPerSecondRetirementRate: ");
		System.out.println(profiler.getRequestPerSecondRetirementRate());
	}

	private IExecutorProfiler getExecutorProfiler() throws Exception {
		final BundleContext context = m_context;
		final ServiceReference<IExecutorProfiler> reference = context.getServiceReference(IExecutorProfiler.class);
		if (reference == null)
			return null;

		return context.getService(reference);
	}

	private static void printPropName(String name, boolean isProfiling) {
		if (isProfiling)
			System.out.print("          ");
		System.out.print(name);
	}
}
