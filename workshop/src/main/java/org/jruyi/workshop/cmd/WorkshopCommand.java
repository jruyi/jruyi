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
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.service.command.CommandProcessor;
import org.jruyi.workshop.IWorkshopProfiler;

@Service(WorkshopCommand.class)
@Component(name = "jruyi.workshop.cmd", policy = ConfigurationPolicy.IGNORE, createPid = false)
@Properties({
		@Property(name = CommandProcessor.COMMAND_SCOPE, value = "workshop"),
		@Property(name = CommandProcessor.COMMAND_FUNCTION, value = { "info",
				"list", "profiling" }) })
public final class WorkshopCommand {

	@Reference(name = "profiler", referenceInterface = IWorkshopProfiler.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
	private final ArrayList<IWorkshopProfiler> m_profilers = new ArrayList<IWorkshopProfiler>();

	protected synchronized void bindProfiler(IWorkshopProfiler profiler) {
		m_profilers.add(profiler);
	}

	protected synchronized void unbindProfiler(IWorkshopProfiler profiler) {
		final ArrayList<IWorkshopProfiler> profilers = m_profilers;
		for (int i = 0, n = profilers.size(); i < n; ++i) {
			if (profilers.get(i) == profiler) {
				profilers.remove(i);
				break;
			}
		}
	}

	public void list() {
		final ArrayList<IWorkshopProfiler> profilers = m_profilers;
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

	public void profiling(String action, long[] profilerIds) {
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

	public void info(long[] profilerIds) {

		final List<IWorkshopProfiler> profilers = getWorkshopProfilers(profilerIds);

		for (IWorkshopProfiler profiler : profilers) {
			System.out.println("============================================================");
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

	private List<IWorkshopProfiler> getWorkshopProfilers(long[] profilerIds) {
		if (profilerIds == null || profilerIds.length < 1)
			return m_profilers;

		final ArrayList<IWorkshopProfiler> profilers = m_profilers;
		ArrayList<IWorkshopProfiler> profilerList = new ArrayList<IWorkshopProfiler>(
				profilerIds.length);
		idLoop: for (long profilerId : profilerIds) {
			for (IWorkshopProfiler profiler : profilers) {
				if (profiler.getProfilerId() == profilerId) {
					profilerList.add(profiler);
					continue idLoop;
				}
			}
			throw new RuntimeException("WorkshopProfiler NOT Found:"
					+ profilerId);
		}
		return profilerList;
	}
}
