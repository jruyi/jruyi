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

import java.lang.reflect.Method;
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

	private static final Logger c_logger = LoggerFactory
			.getLogger(AbstractSslFilter.class);

	private static final int HEADER_SIZE = 5;
	private static final Object SSL_CODEC = new Object();

	private SSLContext m_sslContext;
	private Configuration m_conf;

	static final class Configuration {

		private static final String[] M_PROPS = { "protocol", "provider" };
		private static final Method[] c_mProps;
		private String m_protocol;
		private String m_provider;
		private String m_clientAuth;
		private String[] m_enabledProtocols;
		private String[] m_enabledCipherSuites;

		static {
			Class<Configuration> clazz = Configuration.class;
			c_mProps = new Method[M_PROPS.length];
			try {
				for (int i = 0; i < M_PROPS.length; ++i)
					c_mProps[i] = clazz.getMethod(M_PROPS[i]);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}

		public void initialize(Map<String, ?> properties) {
			protocol((String) properties.get("protocol"));
			provider((String) properties.get("provider"));
			clientAuth((String) properties.get("clientAuth"));
			enabledProtocols((String[]) properties.get("enabledProtocols"));
			enabledCipherSuites((String[]) properties
					.get("enabledCipherSuites"));
		}

		public void protocol(String protocol) {
			m_protocol = protocol;
		}

		public String protocol() {
			return m_protocol;
		}

		public void provider(String provider) {
			m_provider = provider;
		}

		public String provider() {
			return m_provider;
		}

		public void clientAuth(String clientAuth) {
			m_clientAuth = clientAuth;
		}

		public String clientAuth() {
			return m_clientAuth;
		}

		public void enabledProtocols(String[] enabledProtocols) {
			m_enabledProtocols = enabledProtocols;
		}

		public String[] enabledProtocols() {
			return m_enabledProtocols;
		}

		public void enabledCipherSuites(String[] enabledCipherSuites) {
			m_enabledCipherSuites = enabledCipherSuites;
		}

		public String[] enabledCipherSuites() {
			return m_enabledCipherSuites;
		}

		public boolean isMandatoryChanged(Configuration conf) throws Exception {
			for (Method m : c_mProps) {
				Object v1 = m.invoke(this);
				Object v2 = m.invoke(conf);
				if (v1 == v2)
					continue;

				if (!(v1 == null ? v2.equals(v1) : v1.equals(v2)))
					return true;
			}

			return false;
		}
	}

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
		if (type > 0x17) { // SSLv2
			int mask = type < 0x80 ? 0x3F : 0x7F;
			return ((type & mask) << 8 | (in.byteAt(1) & 0xFF)) + 2;
		}

		return in.getUnsignedShort(3, ShortCodec.bigEndian()) + HEADER_SIZE;
	}

	@Override
	public final boolean onMsgArrive(ISession session, IBuffer netData,
			IFilterOutput output) {

		SslCodec sslCodec = (SslCodec) session.inquiry(SSL_CODEC);
		if (sslCodec == null) {
			// server mode
			sslCodec = new SslCodec(createEngine(false));
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
					} else if (hs == HandshakeStatus.FINISHED) {
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
	public final boolean onMsgDepart(ISession session, IBuffer appData,
			IFilterOutput output) {
		SslCodec sslCodec = (SslCodec) session.inquiry(SSL_CODEC);
		if (sslCodec == null) {
			// client mode
			sslCodec = new SslCodec(createEngine(true));
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

	protected void updatedSslContextParameters(ISslContextParameters sslcp)
			throws Exception {
		m_sslContext = createSslContext(m_conf);
	}

	protected void modified(Map<String, ?> properties) throws Exception {
		final Configuration newConf = getConf(properties);
		if (m_conf.isMandatoryChanged(newConf))
			m_sslContext = createSslContext(newConf);

		m_conf = newConf;
	}

	protected void activate(Map<String, ?> properties) throws Exception {
		Configuration conf = getConf(properties);
		m_sslContext = createSslContext(conf);
		m_conf = conf;
	}

	protected void deactivate() {
		m_conf = null;
		m_sslContext = null;
	}

	protected abstract ISslContextParameters sslcp();

	private synchronized SSLContext createSslContext(Configuration conf)
			throws Exception {
		String protocol = conf.protocol();
		if (protocol == null || protocol.isEmpty())
			protocol = "TLS";

		final String provider = conf.provider();
		final SSLContext sslContext = (provider == null || provider.isEmpty()) ? SSLContext
				.getInstance(protocol) : SSLContext.getInstance(protocol,
				provider);

		final ISslContextParameters sslcp = sslcp();
		sslContext.init(sslcp.getKeyManagers(), sslcp.getCertManagers(),
				sslcp.getSecureRandom());
		return sslContext;
	}

	private Configuration getConf(Map<String, ?> properties) throws Exception {
		Configuration conf = new Configuration();
		conf.initialize(properties);
		return conf;
	}

	private SSLEngine createEngine(boolean clientMode) {
		SSLEngine engine = m_sslContext.createSSLEngine();
		Configuration conf = m_conf;
		if (conf.enabledProtocols() != null)
			engine.setEnabledProtocols(conf.enabledProtocols());

		if (conf.enabledCipherSuites() != null)
			engine.setEnabledCipherSuites(conf.enabledCipherSuites());

		final String clientAuth = conf.clientAuth();
		if ("want".equals(clientAuth))
			engine.setWantClientAuth(true);
		else if ("need".equals(clientAuth))
			engine.setNeedClientAuth(true);
		engine.setUseClientMode(clientMode);

		return engine;
	}

	private void runDelegatedTask(SSLEngine engine) {
		Runnable task;
		while ((task = engine.getDelegatedTask()) != null)
			task.run();
	}
}
