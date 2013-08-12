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
package org.jruyi.clid;

import org.apache.felix.service.command.CommandSession;

final class Context {

	private final CommandSession m_cs;
	private final BufferStream m_bs;

	public Context(CommandSession cs, BufferStream bs) {
		m_cs = cs;
		m_bs = bs;
	}

	public CommandSession commandSession() {
		return m_cs;
	}

	public BufferStream bufferStream() {
		return m_bs;
	}
}
