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

package org.jruyi.core.internal;

import org.jruyi.io.tcpserver.TcpAcceptor;

final class TcpAcceptorWrapper {

	private final TcpAcceptor m_acceptor = new TcpAcceptor();
	private int m_count;

	TcpAcceptor unwrap() {
		return m_acceptor;
	}

	synchronized void start() throws Throwable {
		if (m_count == 0)
			m_acceptor.activate();
		++m_count;
	}

	synchronized void stop() {
		if (m_count == 0)
			return;

		if (--m_count == 0)
			m_acceptor.deactivate();
	}
}
