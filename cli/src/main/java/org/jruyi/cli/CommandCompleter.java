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

package org.jruyi.cli;

import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import jline.console.completer.Completer;

public final class CommandCompleter implements Completer {

	private final Session m_session;
	private SortedSet<String> m_lastCommandSet;

	public CommandCompleter(Session session, String commandStr) {
		m_session = session;
		m_lastCommandSet = createCommandSet(commandStr);
	}

	@Override
	public int complete(String buffer, int cursor, List<CharSequence> candidates) {
		SortedSet<String> cmdSet;
		final StringWriter writer = new StringWriter(RuyiCli.WRITER_INIT_SIZE);
		final Session session = m_session;
		session.writer(writer);
		try {
			if (session.send(RuyiCli.CMD_HELP)) {
				session.await();
				String commandStr = writer.toString().trim();
				cmdSet = createCommandSet(commandStr);
				m_lastCommandSet = cmdSet;
			} else
				cmdSet = m_lastCommandSet;
		} catch (Throwable t) {
			cmdSet = m_lastCommandSet;
		} finally {
			session.writer(null);
		}

		if (buffer == null)
			candidates.addAll(cmdSet);
		else {
			Collection<String> tailSet = cmdSet.tailSet(buffer);
			for (String match : tailSet) {
				if (!match.startsWith(buffer))
					break;
				candidates.add(match);
			}
		}

		if (candidates.size() == 1)
			candidates.set(0, candidates.get(0) + " ");

		return candidates.isEmpty() ? -1 : 0;
	}

	private static SortedSet<String> createCommandSet(String commandStr) {
		final String[] commands = commandStr.split("\n");
		final SortedSet<String> cmdSet = new TreeSet<>();
		for (String command : commands) {
			command = command.trim();
			if (!command.isEmpty()) {
				cmdSet.add(command);
			}
		}

		return cmdSet;
	}
}
