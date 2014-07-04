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
package org.jruyi.workshop.cmd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jruyi.workshop.IWorkshopProfiler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public final class WorkshopCommand {

	private final BundleContext m_context;

	public WorkshopCommand(BundleContext context) {
		m_context = context;
	}

	public static String[] commands() {
		return new String[] { "info", "list", "profiling" };
	}

	public void list() throws Exception {
		final List<IWorkshopProfiler> profilers = getWorkshopProfilers(null);
		System.out
				.println(" ProfilerID Core/MaxPoolSize KeepAliveTime QueueCapacity Profiling ThreadPrefix");

		for (IWorkshopProfiler profiler : profilers) {

			// ProfilerID
			String s = String.valueOf(profiler.getProfilerId());
			int n = 9 - s.length();
			printFill(' ', n);
			System.out.print(s);

			// Core/MaxPoolSize
			s = String.valueOf(profiler.getCorePoolSize());
			n = 10 - s.length();
			printFill(' ', n);
			System.out.print(s);
			System.out.print('/');
			s = String.valueOf(profiler.getMaxPoolSize());
			System.out.print(s);
			n = 8 - s.length();
			printFill(' ', n);

			// KeepAliveTime
			s = String.valueOf(profiler.getKeepAliveTime());
			n = 14 - s.length();
			printFill(' ', n);
			System.out.print(s);

			// QueueCapacity
			s = String.valueOf(profiler.getQueueCapacity());
			n = 14 - s.length();
			printFill(' ', n);
			System.out.print(s);

			// Profiling
			printFill(' ', 5);
			System.out.print(profiler.isProfiling() ? 'Y' : 'N');

			// ThreadPrefix
			printFill(' ', 5);
			System.out.println(profiler.getThreadPrefix());
		}
	}

	public void profiling(String action, long[] profilerIds) throws Exception {
		boolean start = false;
		if (action.equals("start"))
			start = true;
		else if (!action.equals("stop")) {
			System.err.println("Unknown action: " + action);
			return;
		}

		final List<IWorkshopProfiler> profilers = getWorkshopProfilers(profilerIds);
		for (IWorkshopProfiler profiler : profilers) {
			if (start)
				profiler.startProfiling();
			else
				profiler.stopProfiling();
		}
	}

	public void info(long[] profilerIds) throws Exception {

		final List<IWorkshopProfiler> profilers = getWorkshopProfilers(profilerIds);

		for (IWorkshopProfiler profiler : profilers) {
			System.out
					.println("============================================================");
			System.out.print("                      ProfilerID: ");
			System.out.println(profiler.getProfilerId());

			System.out.print("                    ThreadPrefix: ");
			System.out.println(profiler.getThreadPrefix());

			System.out.print("                    CorePoolSize: ");
			System.out.println(profiler.getCorePoolSize());

			System.out.print("                     MaxPoolSize: ");
			System.out.println(profiler.getMaxPoolSize());

			System.out.print("                   KeepAliveTime: ");
			System.out.print(profiler.getKeepAliveTime());
			System.out.println('s');

			System.out.print("                   QueueCapacity: ");
			System.out.println(profiler.getQueueCapacity());

			System.out.print("                 CurrentPoolSize: ");
			System.out.println(profiler.getCurrentPoolSize());

			System.out.print("              CurrentQueueLength: ");
			System.out.println(profiler.getCurrentQueueLength());

			System.out.print("                     InProfiling: ");
			if (!profiler.isProfiling()) {
				System.out.println('N');
				return;
			} else
				System.out.println('Y');

			System.out.print("         NumberOfRequestsRetired: ");
			System.out.println(profiler.getNumberOfRequestsRetired());

			System.out.print("   AverageNumberOfActiveRequests: ");
			System.out.println(profiler
					.getEstimatedAverageNumberOfActiveRequests());

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
	}

	private static void printFill(char c, int count) {
		while (--count >= 0)
			System.out.print(c);
	}

	private List<IWorkshopProfiler> getWorkshopProfilers(long[] profilerIds)
			throws Exception {
		final BundleContext context = m_context;
		final Collection<ServiceReference<IWorkshopProfiler>> references = context
				.getServiceReferences(IWorkshopProfiler.class, null);

		if (references == null || references.size() < 1)
			return Collections.emptyList();

		final ArrayList<IWorkshopProfiler> profilers;
		if (profilerIds == null || profilerIds.length < 1) {
			profilers = new ArrayList<IWorkshopProfiler>(references.size());
			for (ServiceReference<IWorkshopProfiler> reference : references) {
				final IWorkshopProfiler profiler = context
						.getService(reference);
				if (profiler != null)
					profilers.add(profiler);
			}
		} else {
			profilers = new ArrayList<IWorkshopProfiler>(profilerIds.length);
			for (ServiceReference<IWorkshopProfiler> reference : references) {
				final IWorkshopProfiler profiler = context
						.getService(reference);
				if (profiler != null
						&& in(profilerIds, profiler.getProfilerId()))
					profilers.add(profiler);
			}
		}
		return profilers;
	}

	private static boolean in(long[] profilerIds, long profilerId) {
		for (long id : profilerIds) {
			if (id == profilerId)
				return true;
		}

		return false;
	}
}
