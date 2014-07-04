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
import java.net.URL;
import java.nio.BufferUnderflowException;
import java.util.Map;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.jruyi.common.CharsetCodec;
import org.jruyi.common.IntStack;
import org.jruyi.common.Properties;
import org.jruyi.common.StrUtil;
import org.jruyi.common.StringBuilder;
import org.jruyi.io.Codec;
import org.jruyi.io.IBuffer;
import org.jruyi.io.IFilter;
import org.jruyi.io.IFilterOutput;
import org.jruyi.io.ISession;
import org.jruyi.io.ISessionService;
import org.jruyi.io.IoConstants;
import org.jruyi.io.SessionListener;
import org.jruyi.system.Constants;
import org.jruyi.timeoutadmin.ITimeoutAdmin;
import org.jruyi.timeoutadmin.ITimeoutNotifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = CliProcessor.SERVICE_ID, //
immediate = true, //
property = { IoConstants.FILTER_ID + "=jruyi.clid.filter" }, //
xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class CliProcessor extends SessionListener implements
		IFilter<IBuffer, Object> {

	public static final String SERVICE_ID = "jruyi.clid";

	private static final Logger c_logger = LoggerFactory
			.getLogger(CliProcessor.class);

	private static final String BRANDING_URL = "jruyi.clid.branding.url";
	private static final String BINDADDR = "jruyi.clid.bindAddr";
	private static final String PORT = "jruyi.clid.port";
	private static final String SESSIONIDLETIMEOUT = "jruyi.clid.sessionIdleTimeout";

	private static final String WELCOME = "welcome";
	private static final String SCOPE = "SCOPE";

	private static final String P_BIND_ADDR = "bindAddr";
	private static final String P_PORT = "port";

	private static final byte COLON = ':';

	private static final String[] FILTERS = { "jruyi.clid.filter" };

	private static final String P_SESSION_IDLE_TIMEOUT = "sessionIdleTimeout";
	private static final String P_FLUSH_THRESHOLD = "flushThreshold";

	private ComponentFactory m_tsf;
	private CommandProcessor m_cp;
	private ITimeoutAdmin m_ta;

	private BundleContext m_context;
	private ComponentInstance m_tcpServer;
	private Properties m_conf;
	private byte[] m_welcome;

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
	public boolean onMsgArrive(ISession session, IBuffer msg,
			IFilterOutput output) {
		msg.compact();
		output.add(msg);
		return true;
	}

	@Override
	public boolean onMsgDepart(ISession session, Object msg,
			IFilterOutput output) {
		output.add(msg);
		return true;
	}

	@Override
	public void onSessionOpened(ISession session) {
		ISessionService ss = (ISessionService) m_tcpServer.getInstance();
		ITimeoutNotifier tn = m_ta.createNotifier(null);
		OutBufferStream outBufferStream = new OutBufferStream(ss, session, tn);
		ErrBufferStream errBufferStream = new ErrBufferStream(outBufferStream);
		PrintStream out = new PrintStream(outBufferStream, true);
		PrintStream err = new PrintStream(errBufferStream, true);
		CommandSession cs = m_cp.createSession(null, out, err);
		cs.put(SCOPE, "builtin:*");

		session.deposit(this, new Context(cs, errBufferStream));

		outBufferStream.write(m_welcome);
		outBufferStream.write(ClidConstants.CR[0]);
		outBufferStream.write(ClidConstants.LF[0]);
		writeCommands(outBufferStream);
		outBufferStream.writeOut(System.getProperty(Constants.JRUYI_INST_NAME)
				+ "> ");
	}

	@Override
	public void onSessionClosed(ISession session) {
		Context context = (Context) session.withdraw(this);
		if (context != null) {
			CommandSession cs = context.commandSession();
			cs.getConsole().close();
			cs.close();
		}
	}

	@Override
	public void onMessageReceived(ISession session, Object message) {
		IBuffer buffer = (IBuffer) message;
		String cmdline;
		try {
			cmdline = buffer.remaining() > 0 ? buffer.read(Codec.utf_8())
					: null;
		} finally {
			buffer.close();
		}

		Context context = (Context) session.inquiry(this);
		CommandSession cs = context.commandSession();
		ErrBufferStream err = context.errBufferStream();
		OutBufferStream out = err.outBufferStream();
		out.reset();

		int status = 0;
		if (cmdline != null) {
			cmdline = filterProps(cmdline, cs, m_context);
			try {
				Object result = cs.execute(cmdline);
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

	@Reference(name = "tsf", target = "(component.name="
			+ IoConstants.CN_TCPSERVER_FACTORY + ")")
	protected void setTcpServerFactory(ComponentFactory tsf) {
		m_tsf = tsf;
	}

	protected void unsetTcpServerFactory(ComponentFactory tsf) {
		m_tsf = null;
	}

	@Reference(name = "cp")
	protected void setCommandProcessor(CommandProcessor cp) {
		m_cp = cp;
	}

	protected void unsetCommandProcessor(CommandProcessor cp) {
		m_cp = null;
	}

	@Reference(name = "ta")
	protected void setTimeoutAdmin(ITimeoutAdmin ta) {
		m_ta = ta;
	}

	protected void unsetTimeoutAdmin(ITimeoutAdmin ta) {
		m_ta = null;
	}

	@Modified
	protected void modified(Map<String, ?> properties) throws Exception {
		ISessionService inst = (ISessionService) m_tcpServer.getInstance();
		inst.update(normalizeConf(properties));

		OutBufferStream.flushThreshold((Integer) properties
				.get(P_FLUSH_THRESHOLD));
	}

	protected void activate(ComponentContext context, Map<String, ?> properties)
			throws Exception {

		final BundleContext bundleContext = context.getBundleContext();
		loadBrandingInfo(bundleContext.getProperty(BRANDING_URL), bundleContext);

		OutBufferStream.flushThreshold((Integer) properties
				.get(P_FLUSH_THRESHOLD));

		Properties conf = normalizeConf(properties);
		if (conf.get(P_BIND_ADDR) == null) {
			String bindAddr = context.getBundleContext().getProperty(BINDADDR);
			if (bindAddr == null || (bindAddr = bindAddr.trim()).length() < 1)
				bindAddr = "localhost";
			conf.put(P_BIND_ADDR, bindAddr);
		}

		if (conf.get(P_PORT) == null) {
			String v = context.getBundleContext().getProperty(PORT);
			Integer port = v == null ? 6060 : Integer.valueOf(v);
			conf.put(P_PORT, port);
		}

		if (conf.get(P_SESSION_IDLE_TIMEOUT) == null) {
			String v = context.getBundleContext().getProperty(
					SESSIONIDLETIMEOUT);
			Integer sessionIdleTimeout = v == null ? 300 : Integer.valueOf(v);
			conf.put(P_SESSION_IDLE_TIMEOUT, sessionIdleTimeout);
		}

		final ComponentInstance tcpServer = m_tsf.newInstance(conf);
		ISessionService ss = (ISessionService) tcpServer.getInstance();
		ss.setSessionListener(this);
		try {
			ss.start();
		} catch (Exception e) {
			ss.setSessionListener(null);
			tcpServer.dispose();
			throw e;
		}
		m_tcpServer = tcpServer;
		m_context = bundleContext;
	}

	protected void deactivate(ComponentContext context) {
		m_context = null;
		m_tcpServer.dispose();
		m_tcpServer = null;
		m_conf = null;
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

	private void loadBrandingInfo(String url, BundleContext context)
			throws Exception {
		java.util.Properties branding = loadBrandingProps(CliProcessor.class
				.getResourceAsStream("branding.properties"));
		if (url != null)
			branding.putAll(loadBrandingProps(new URL(url).openStream()));

		m_welcome = CharsetCodec.get(CharsetCodec.UTF_8).toBytes(
				StrUtil.filterProps(branding.getProperty(WELCOME), context));
	}

	private static java.util.Properties loadBrandingProps(InputStream in)
			throws Exception {
		java.util.Properties props = new java.util.Properties();
		try {
			props.load(new BufferedInputStream(in));
		} finally {
			in.close();
		}
		return props;
	}

	private static String filterProps(String target, CommandSession cs,
			BundleContext context) {
		if (target.length() < 2)
			return target;

		StringBuilder builder = StringBuilder.get();
		IntStack stack = IntStack.get();
		String propValue = null;
		int j = target.length();
		for (int i = 0; i < j; ++i) {
			char c = target.charAt(i);
			switch (c) {
			case '$':
				builder.append(c);
				if (++i < j && (c = target.charAt(i)) == '{')
					stack.push(builder.length() - 1);
				break;
			case '}':
				if (!stack.isEmpty()) {
					int index = stack.pop();
					propValue = getPropValue(builder.substring(index + 2), cs,
							context);
					if (propValue != null) {
						builder.setLength(index);
						builder.append(propValue);
						continue;
					}
				}
			}

			builder.append(c);
		}
		stack.close();

		if (propValue != null || builder.length() != j)
			target = builder.toString();

		builder.close();

		return target;
	}

	private static String getPropValue(String name, CommandSession cs,
			BundleContext context) {
		Object value = cs.get(name);
		if (value != null)
			return value.toString();

		return context.getProperty(name);
	}

	private static int parseErrorMessage(String errorMessage) {
		int len = errorMessage.length();
		int i = 0;
		for (; i < len; ++i) {
			char c = errorMessage.charAt(i);
			if (c < '0' || c > '9')
				break;
		}

		return i;
	}

	private void writeCommands(OutBufferStream bs) {
		ServiceReference<?>[] references;
		try {
			references = m_context.getAllServiceReferences(null, "(&("
					+ CommandProcessor.COMMAND_SCOPE + "=*)(!("
					+ CommandProcessor.COMMAND_SCOPE + "=builtin)))");
		} catch (Throwable t) {
			// should never go here
			c_logger.error("Failed to get commands", t);
			return;
		}

		for (ServiceReference<?> reference : references) {
			String scope = String.valueOf(reference
					.getProperty(CommandProcessor.COMMAND_SCOPE));
			Object v = reference.getProperty(CommandProcessor.COMMAND_FUNCTION);
			if (v instanceof String[]) {
				String[] funcs = (String[]) v;
				for (String func : funcs)
					writeCommand(bs, scope, func);
			} else
				writeCommand(bs, scope, String.valueOf(v));
		}
	}

	private static void writeCommand(OutBufferStream bs, String scope,
			String func) {
		bs.write(scope);
		bs.write(COLON);
		bs.write(func);
		bs.write(ClidConstants.LF[0]);
	}
}
