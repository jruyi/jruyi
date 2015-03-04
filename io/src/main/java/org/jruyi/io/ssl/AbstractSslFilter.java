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

package org.jruyi.io.ssl;

import java.net.InetSocketAddress;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;

import org.jruyi.common.StrUtil;
import org.jruyi.io.IBuffer;
import org.jruyi.io.IFilter;
import org.jruyi.io.IFilterOutput;
import org.jruyi.io.ISession;
import org.jruyi.io.ISslContextParameters;
import org.jruyi.io.ShortCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSslFilter implements IFilter<IBuffer, IBuffer> {

	private static final Logger c_logger = LoggerFactory.getLogger(AbstractSslFilter.class);

	private static final int HEADER_SIZE = 5;
	private static final Object SSL_CODEC = new Object();

	private SSLContext m_sslContext;
	private Configuration m_conf;

	@Override
	public int msgMinSize() {
		return HEADER_SIZE;
	}

	// Byte 0 = SSL record type, if type >= 0x80, SSLv2
	// Bytes 1-2 = SSL version (major/minor)
	// Bytes 3-4 = Length of data in the record (excluding the header itself).
	// The maximum SSL supports is 16384 (16K).
	@Override
	public final int tellBoundary(ISession session, IBuffer in) {
		int type = in.byteAt(0) & 0xFF;
		if (type > 0x18) { // SSLv2
			final int mask;
			final int headerSize;
			if (type < 0x80) {
				mask = 0x3F;
				headerSize = 3;
			} else {
				mask = 0x7F;
				headerSize = 2;
			}
			return ((type & mask) << 8 | (in.byteAt(1) & 0xFF)) + headerSize;
		}

		return in.getUnsignedShort(3, ShortCodec.bigEndian()) + HEADER_SIZE;
	}

	@Override
	public final boolean onMsgArrive(ISession session, IBuffer netData, IFilterOutput output) {

		SslCodec sslCodec = (SslCodec) session.inquiry(SSL_CODEC);
		if (sslCodec == null) {
			// server mode
			sslCodec = new SslCodec(createEngine(session.remoteAddress(), false));
			session.deposit(SSL_CODEC, sslCodec);
		}

		IBuffer appBuf = netData.newBuffer();
		try {
			final SSLEngine engine = sslCodec.engine();
			for (;;) {
				appBuf.write(netData, sslCodec);
				final SSLEngineResult result = sslCodec.unwrapResult();
				final Status status = result.getStatus();
				if (status == Status.OK) {
					final HandshakeStatus hs = result.getHandshakeStatus();
					if (hs == HandshakeStatus.NOT_HANDSHAKING) {
						output.add(appBuf);
						return true;
					}

					if (hs == HandshakeStatus.NEED_TASK) {
						runDelegatedTask(engine);
						continue;
					}

					if (hs == HandshakeStatus.NEED_UNWRAP) {
						appBuf.close();
						return true;
					}

					if (hs == HandshakeStatus.FINISHED) {
						appBuf.close();
						final IBuffer inception = sslCodec.inception();
						if (inception == null)
							return true;

						sslCodec.inception(null);
						appBuf = inception;
					}

					break;
				}

				appBuf.close();
				// BUFFER_UNDERFLOW, CLOSED
				return true;
			}

			output.add(appBuf);
			return false;
		} catch (Throwable t) {
			c_logger.error(StrUtil.join(session, " failed to unwrap"), t);
			appBuf.close();
			return false;
		} finally {
			netData.close();
		}
	}

	@Override
	public final boolean onMsgDepart(ISession session, IBuffer appData, IFilterOutput output) {
		SslCodec sslCodec = (SslCodec) session.inquiry(SSL_CODEC);
		if (sslCodec == null) {
			// client mode
			sslCodec = new SslCodec(createEngine(session.remoteAddress(), true));
			session.deposit(SSL_CODEC, sslCodec);

			if (!appData.isEmpty())
				sslCodec.inception(appData.split(appData.size()));
		}

		final IBuffer netBuf = appData.newBuffer();
		try {
			final SSLEngine engine = sslCodec.engine();
			for (;;) {
				appData.read(netBuf, sslCodec);
				final SSLEngineResult result = sslCodec.wrapResult();
				final Status status = result.getStatus();
				if (status == Status.OK) {
					final HandshakeStatus hs = result.getHandshakeStatus();
					if (hs == HandshakeStatus.NOT_HANDSHAKING)
						break;

					if (hs == HandshakeStatus.NEED_TASK) {
						runDelegatedTask(engine);
						continue;
					}

					if (hs == HandshakeStatus.NEED_WRAP)
						continue;

					if (hs == HandshakeStatus.FINISHED) {
						final IBuffer inception = sslCodec.inception();
						if (inception != null) {
							sslCodec.inception(null);
							appData.close();
							appData = inception;
							continue;
						}
					}

					break;
				}

				netBuf.close();
				return status == Status.BUFFER_UNDERFLOW;
			}

			output.add(netBuf);
			return true;
		} catch (Throwable t) {
			c_logger.error(StrUtil.join(session, " failed to wrap"), t);
			netBuf.close();
			return false;
		} finally {
			appData.close();
		}
	}

	protected void updatedSslContextParameters(ISslContextParameters sslcp) throws Exception {
		m_sslContext = createSslContext(m_conf);
	}

	protected void modified(Map<String, ?> properties) throws Exception {
		final Configuration newConf = Configuration.create(properties);
		if (m_conf.isMandatoryChanged(newConf))
			m_sslContext = createSslContext(newConf);

		m_conf = newConf;
	}

	protected void activate(Map<String, ?> properties) throws Exception {
		final Configuration conf = Configuration.create(properties);
		m_sslContext = createSslContext(conf);
		m_conf = conf;
	}

	protected void deactivate() {
		m_conf = null;
		m_sslContext = null;
	}

	protected abstract ISslContextParameters sslcp();

	private SSLContext createSslContext(Configuration conf) throws Exception {
		String protocol = conf.protocol();
		if (protocol == null || protocol.isEmpty())
			protocol = "TLS";

		final String provider = conf.provider();
		final SSLContext sslContext = (provider == null || provider.isEmpty())
				? SSLContext.getInstance(protocol)
				: SSLContext.getInstance(protocol, provider);

		final ISslContextParameters sslcp = sslcp();
		sslContext.init(sslcp.getKeyManagers(), sslcp.getCertManagers(), sslcp.getSecureRandom());
		return sslContext;
	}

	private SSLEngine createEngine(Object remoteAddr, boolean clientMode) {
		final Configuration conf = m_conf;
		final SSLEngine engine;
		String hostname = conf.hostname();
		if (hostname == null)
			engine = m_sslContext.createSSLEngine();
		else if (remoteAddr instanceof InetSocketAddress) {
			final InetSocketAddress socketAddress = (InetSocketAddress) remoteAddr;
			if (hostname.isEmpty())
				hostname = socketAddress.getHostName();
			engine = m_sslContext.createSSLEngine(hostname, socketAddress.getPort());
		} else if (hostname.isEmpty())
			engine = m_sslContext.createSSLEngine();
		else
			engine = m_sslContext.createSSLEngine(hostname, -1);
		engine.setSSLParameters(conf.sslParameters());
		engine.setUseClientMode(clientMode);
		return engine;
	}

	private void runDelegatedTask(SSLEngine engine) {
		Runnable task;
		while ((task = engine.getDelegatedTask()) != null)
			task.run();
	}
}
