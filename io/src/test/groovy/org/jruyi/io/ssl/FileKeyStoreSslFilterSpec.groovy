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

package org.jruyi.io.ssl

import org.jruyi.io.*
import org.jruyi.io.buffer.BufferFactory
import spock.lang.Specification
import spock.lang.Unroll

class FileKeyStoreSslFilterSpec extends Specification {

	def m_bf = new BufferFactory()

	@Unroll
	def "verifying hostname should be #result for: #hostname"() {
		expect:
		doSsl(hostname) == result

		where:
		hostname << ["test.jruyi.org", "127.0.0.1", "test.jruyi1.org"]
		result << [true, false, false]
	}

	private def doSsl(String hostname) {
		def clientSession = StubSession(hostname)
		def serverSession = StubSession("127.0.0.1")

		def serverConf = [
				keyStoreUrl     : FileKeyStoreSslFilterSpec.class.getResource("/testkey").toString(),
				keyStorePassword: "changeme",
				keyPassword     : "changeme",
		]
		def serverFilter = new FileKeyStoreSslFilter()
		serverFilter.activate(serverConf)

		def clientConf = [
				trustStoreUrl                  : FileKeyStoreSslFilterSpec.class.getResource("/testcerts").toString(),
				trustStorePassword             : "changeme",
				hostname                       : hostname,
				endpointIdentificationAlgorithm: "HTTPS"
		]
		def clientFilter = new FileKeyStoreSslFilter()
		clientFilter.activate(clientConf)

		def IBufferFactory bf = m_bf
		bf.activate([unitCapacity: 8192])

		def data = bf.create().write(0x12345678, IntCodec.littleEndian());
		def out = bf.create();
		def filter = clientFilter
		def session = clientSession
		while (true) {
			def (result, hasOut) = onMsgDepart(filter, session, data, out)
			if (result && hasOut) {
				if (filter == clientFilter) {
					filter = serverFilter
					session = serverSession
				} else {
					filter = clientFilter
					session = clientSession
				}
				data = out
				out = bf.create()
				(result, hasOut) = onMsgArrive(filter, session, data, out)
				if (result && !out.empty && out.read(IntCodec.littleEndian()) == 0x12345678)
					return true

				if (!result && hasOut) {
					data = out
					out = bf.create()
					continue
				}

				return false
			}
		}
	}

	private def onMsgArrive(IFilter<IBuffer, IBuffer> filter, ISession session, IBuffer data, IBuffer out) {
		def hasOut = false
		def output = Stub(IFilterOutput)
		output.add(_) >> { args ->
			hasOut = true
			((IBuffer) args[0]).drainTo(out)
		}
		while (!data.empty) {
			int msgLen = filter.tellBoundary(session, data)
			int dataLen = data.length()
			if (dataLen < msgLen)
				throw new Exception("Insufficient msg data: msgLen=" + msgLen + ", dataLen=" + dataLen)

			final IBuffer msg = data.split(msgLen)
			def result = filter.onMsgArrive(session, msg, output)
			if (!result)
				return [false, hasOut]
		}
		return [true, hasOut]
	}

	private def onMsgDepart(IFilter<IBuffer, IBuffer> filter, ISession session, IBuffer data, IBuffer out) {
		def hasOut = false;
		def output = Stub(IFilterOutput)
		output.add(_) >> { args ->
			hasOut = true
			((IBuffer) args[0]).drainTo(out)
		}
		def result = filter.onMsgDepart(session, data, output)
		return [result, hasOut]
	}

	private def StubSession(String host) {
		def map = new IdentityHashMap()
		def session = Stub(ISession)
		session.inquiry(_) >> { args -> return map.get(args[0]) }
		session.deposit(_, _) >> { k, v -> return map.put(k, v) }
		session.createBuffer() >> { return m_bf.create() }
		session.remoteAddress() >> { return InetSocketAddress.createUnresolved(host, 8888) }
		return session
	}
}