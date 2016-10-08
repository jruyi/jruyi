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

package org.jruyi.clid;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.BufferUnderflowException;
import java.util.Map;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.jruyi.common.CharsetCodec;
import org.jruyi.common.Properties;
import org.jruyi.common.StrUtil;
import org.jruyi.io.IBuffer;
import org.jruyi.io.IFilter;
import org.jruyi.io.IFilterOutput;
import org.jruyi.io.ISession;
import org.jruyi.io.ISessionService;
import org.jruyi.io.IoConstants;
import org.jruyi.io.SessionListener;
import org.jruyi.io.StringCodec;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = CliServer.SERVICE_ID, //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		service = IFilter.class, //
		property = { IoConstants.FILTER_ID + "=jruyi.clid.filter" }, //
		xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class CliServer extends SessionListener<IBuffer, IBuffer> implements IFilter<IBuffer, IBuffer> {

	public static final String SERVICE_ID = "jruyi.clid";

	private static final Logger c_logger = LoggerFactory.getLogger(CliServer.class);

	private static final String WELCOME = "welcome";
	private static final String SCOPE = "SCOPE";

	private static final byte COLON = ':';

	private static final String[] FILTERS = { "jruyi.clid.filter" };

	private static final String P_BRANDING_URL = "jruyi.clid.branding.url";

	private ComponentFactory m_tsf;
	private CommandProcessor m_cp;

	private BundleContext m_context;
	private ComponentInstance m_tcpServer;
	private Properties m_conf;
	private String m_brandingUrl;

	private byte[] m_welcome;

	private static java.util.Properties loadBrandingProps(InputStream in) throws Throwable {
		final java.util.Properties props = new java.util.Properties();
		try {
			props.load(new BufferedInputStream(in));
		} finally {
			in.close();
		}
		return props;
	}

	private static int parseErrorMessage(String errorMessage) {
		final int len = errorMessage.length();
		int i = 0;
		for (; i < len; ++i) {
			char c = errorMessage.charAt(i);
			if (c < '0' || c > '9')
				break;
		}

		return i;
	}

	private static void writeCommand(OutBufferStream bs, String scope, String func) {
		bs.write(scope);
		bs.write(COLON);
		bs.write(func);
		bs.write(ClidConstants.LF);
	}

	@Override
	public int msgMinSize() {
		return 0;
	}

	@Override
	public int tellBoundary(ISession session, IBuffer in) {
		int b;
		int n = 0;
		try {
			do {
				b = in.read();
				n = (n << 7) | (b & 0x7F);
			} while (b < 0);
			return n + in.position();
		} catch (BufferUnderflowException e) {
			in.rewind();
			return IFilter.E_UNDERFLOW;
		}
	}

	@Override
	public boolean onMsgArrive(ISession session, IBuffer msg, IFilterOutput output) {
		msg.compact();
		output.add(msg);
		return true;
	}

	@Override
	public boolean onMsgDepart(ISession session, IBuffer msg, IFilterOutput output) {
		output.add(msg);
		return true;
	}

	@Override
	public void onSessionOpened(ISession session) {
		@SuppressWarnings("unchecked")
		final ISessionService<IBuffer, IBuffer> ss = (ISessionService<IBuffer, IBuffer>) m_tcpServer.getInstance();
		final OutBufferStream outBufferStream = new OutBufferStream(ss, session);
		final ErrBufferStream errBufferStream = new ErrBufferStream(outBufferStream);
		final PrintStream out = new PrintStream(outBufferStream, true);
		final PrintStream err = new PrintStream(errBufferStream, true);
		final CommandSession cs = m_cp.createSession(null, out, err);
		cs.put(SCOPE, "builtin:*");

		final Context context = new Context(cs, errBufferStream);
		session.deposit(this, context);

		outBufferStream.write(m_welcome);
		outBufferStream.write(ClidConstants.CR);
		outBufferStream.write(ClidConstants.LF);
		writeCommands(outBufferStream);
		outBufferStream.writeOut(getPrompt(session));
	}

	@Override
	public void onSessionClosed(ISession session) {
		final Context context = (Context) session.withdraw(this);
		if (context != null) {
			final CommandSession cs = context.commandSession();
			cs.getConsole().close();
			cs.close();
		}
	}

	@Override
	public void onMessageReceived(ISession session, IBuffer inMsg) {
		final Context context = (Context) session.inquiry(this);

		String cmdline;
		try {
			cmdline = inMsg.remaining() > 0 ? inMsg.read(StringCodec.utf_8()) : null;
		} finally {
			inMsg.close();
		}

		final CommandSession cs = context.commandSession();
		final ErrBufferStream err = context.errBufferStream();
		final OutBufferStream out = err.outBufferStream();
		out.reset();

		int status = 0;
		if (cmdline != null) {
			cmdline = Util.filterProps(cmdline, cs, m_context);
			try {
				final Object result = cs.execute(cmdline);
				if (result != null)
					out.write(String.valueOf(result));
			} catch (Throwable t) {
				c_logger.warn(cmdline, t);
				status = 1;
				String msg = t.getMessage();
				if (msg == null)
					msg = t.toString();
				else {
					msg = msg.trim();
					int i = parseErrorMessage(msg);
					if (i > 0) {
						try {
							status = Integer.parseInt(msg.substring(0, i));
							msg = msg.substring(i);
						} catch (Exception e) {
							// Ignore
						}
					}
				}
				err.write(msg);
			}
		}
		out.writeOut(status);
	}

	@Reference(name = "tsf", target = "(component.name=" + IoConstants.CN_TCPSERVER_FACTORY + ")")
	synchronized void setTcpServerFactory(ComponentFactory tsf) throws Throwable {
		if (m_tcpServer != null)
			stopTcpServer();

		m_tsf = tsf;

		if (m_conf != null)
			startTcpServer(tsf);
	}

	synchronized void unsetTcpServerFactory(ComponentFactory tsf) {
		if (m_tsf == tsf) {
			m_tsf = null;
			stopTcpServer();
		}
	}

	@Reference(name = "cp")
	void setCommandProcessor(CommandProcessor cp) {
		m_cp = cp;
	}

	void unsetCommandProcessor(CommandProcessor cp) {
		m_cp = null;
	}

	@Modified
	void modified(Map<String, ?> properties) throws Throwable {
		final Properties conf = normalizeConf(properties);
		loadBrandingInfo(conf, m_context);

		@SuppressWarnings("unchecked")
		final ISessionService<IBuffer, IBuffer> ss = (ISessionService<IBuffer, IBuffer>) m_tcpServer.getInstance();
		ss.update(conf);
	}

	void activate(BundleContext context, Map<String, ?> properties) throws Throwable {

		final Properties conf = normalizeConf(properties);
		loadBrandingInfo(conf, context);

		if (m_tsf != null)
			startTcpServer(m_tsf);

		m_context = context;
	}

	void deactivate() {
		stopTcpServer();
		m_context = null;
		m_conf = null;
		m_brandingUrl = null;
	}

	private Properties normalizeConf(Map<String, ?> properties) {
		Properties conf = m_conf;
		if (conf == null) {
			conf = new Properties(properties);
			m_conf = conf;
		} else
			conf.putAll(properties);

		conf.put(IoConstants.SERVICE_ID, SERVICE_ID);
		conf.put("initCapacityOfChannelMap", 8);
		conf.put("filters", FILTERS);
		conf.put("reuseAddr", Boolean.TRUE);
		return conf;
	}

	private synchronized void startTcpServer(ComponentFactory tsf) throws Throwable {
		final ComponentInstance tcpServer = tsf.newInstance(m_conf);
		@SuppressWarnings("unchecked")
		final ISessionService<IBuffer, IBuffer> ss = (ISessionService<IBuffer, IBuffer>) tcpServer.getInstance();
		ss.setSessionListener(this);
		try {
			ss.start();
		} catch (Throwable t) {
			ss.setSessionListener(null);
			tcpServer.dispose();
			throw t;
		}
		m_tcpServer = tcpServer;
	}

	private synchronized void stopTcpServer() {
		final ComponentInstance tcpServer = m_tcpServer;
		if (tcpServer != null) {
			m_tcpServer = null;
			tcpServer.dispose();
		}
	}

	private void loadBrandingInfo(Properties conf, BundleContext context) throws Throwable {
		String url = (String) conf.remove(P_BRANDING_URL);
		if (url == null)
			url = "";
		if (url.equals(m_brandingUrl))
			return;

		java.util.Properties branding = loadBrandingProps(CliServer.class.getResourceAsStream("branding.properties"));
		if (!url.isEmpty())
			branding.putAll(loadBrandingProps(new URL(url).openStream()));

		m_welcome = CharsetCodec.get(CharsetCodec.UTF_8)
				.toBytes(StrUtil.filterProps(branding.getProperty(WELCOME), context));

		m_brandingUrl = url;
	}

	private void writeCommands(OutBufferStream bs) {
		final ServiceReference<?>[] references;
		try {
			references = m_context.getAllServiceReferences(null,
					"(&(" + CommandProcessor.COMMAND_SCOPE + "=*)(!(" + CommandProcessor.COMMAND_SCOPE + "=builtin)))");
		} catch (Throwable t) {
			// should never go here
			c_logger.error("Failed to get commands", t);
			return;
		}

		for (ServiceReference<?> reference : references) {
			final String scope = String.valueOf(reference.getProperty(CommandProcessor.COMMAND_SCOPE));
			final Object v = reference.getProperty(CommandProcessor.COMMAND_FUNCTION);
			if (v instanceof String[]) {
				final String[] funcs = (String[]) v;
				for (String func : funcs)
					writeCommand(bs, scope, func);
			} else
				writeCommand(bs, scope, String.valueOf(v));
		}
	}

	private String getPrompt(ISession session) {
		final InetSocketAddress localAddr = (InetSocketAddress) session.localAddress();
		return StrUtil.join(localAddr.getHostName(), ':', localAddr.getPort(), "> ");
	}
}
