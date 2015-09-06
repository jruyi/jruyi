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

import static org.osgi.framework.Constants.FRAMEWORK_STORAGE;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.jruyi.common.BytesBuilder;
import org.jruyi.common.CharsetCodec;
import org.jruyi.common.ICharsetCodec;
import org.jruyi.common.IntStack;
import org.jruyi.common.Properties;
import org.jruyi.common.StrUtil;
import org.jruyi.common.StringBuilder;
import org.jruyi.io.IBuffer;
import org.jruyi.io.IFilter;
import org.jruyi.io.IFilterOutput;
import org.jruyi.io.ISession;
import org.jruyi.io.ISessionService;
import org.jruyi.io.IoConstants;
import org.jruyi.io.SessionListener;
import org.jruyi.io.StringCodec;
import org.jruyi.system.Constants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = CliServer.SERVICE_ID, //
immediate = true, //
property = { IoConstants.FILTER_ID + "=jruyi.clid.filter" }, //
xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class CliServer extends SessionListener<IBuffer, IBuffer>implements IFilter<IBuffer, IBuffer> {

	public static final String SERVICE_ID = "jruyi.clid";

	private static final Logger c_logger = LoggerFactory.getLogger(CliServer.class);

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

	private ComponentFactory m_tsf;
	private CommandProcessor m_cp;

	private BundleContext m_context;
	private ComponentInstance m_tcpServer;
	private Properties m_conf;
	private byte[] m_welcome;

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

		session.deposit(this, new Context(cs, errBufferStream));

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
			CommandSession cs = context.commandSession();
			cs.getConsole().close();
			cs.close();
		}
	}

	@Override
	public void onMessageReceived(ISession session, IBuffer inMsg) {
		String cmdline;
		try {
			cmdline = inMsg.remaining() > 0 ? inMsg.read(StringCodec.utf_8()) : null;
		} finally {
			inMsg.close();
		}

		final Context context = (Context) session.inquiry(this);
		final CommandSession cs = context.commandSession();
		final ErrBufferStream err = context.errBufferStream();
		final OutBufferStream out = err.outBufferStream();
		out.reset();

		int status = 0;
		if (cmdline != null) {
			cmdline = filterProps(cmdline, cs, m_context);
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

	@Reference(name = "tsf", //
	policy = ReferencePolicy.DYNAMIC, //
	cardinality = ReferenceCardinality.OPTIONAL, //
	target = "(component.name=" + IoConstants.CN_TCPSERVER_FACTORY + ")")
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
	void modified(Map<String, ?> properties) throws Exception {
		@SuppressWarnings("unchecked")
		final ISessionService<IBuffer, IBuffer> ss = (ISessionService<IBuffer, IBuffer>) m_tcpServer.getInstance();
		ss.update(normalizeConf(properties));
	}

	void activate(ComponentContext context, Map<String, ?> properties) throws Throwable {

		final BundleContext bundleContext = context.getBundleContext();

		provision(bundleContext);
		loadBrandingInfo(bundleContext.getProperty(BRANDING_URL), bundleContext);

		final Properties conf = normalizeConf(properties);
		if (conf.get(P_BIND_ADDR) == null) {
			String bindAddr = bundleContext.getProperty(BINDADDR);
			if (bindAddr != null && !(bindAddr = bindAddr.trim()).isEmpty())
				conf.put(P_BIND_ADDR, bindAddr);
		}

		if (conf.get(P_PORT) == null) {
			String v = bundleContext.getProperty(PORT);
			Integer port = v == null ? 6060 : Integer.valueOf(v);
			conf.put(P_PORT, port);
		}

		if (conf.get(P_SESSION_IDLE_TIMEOUT) == null) {
			String v = bundleContext.getProperty(SESSIONIDLETIMEOUT);
			Integer sessionIdleTimeout = v == null ? 300 : Integer.valueOf(v);
			conf.put(P_SESSION_IDLE_TIMEOUT, sessionIdleTimeout);
		}

		if (m_tsf != null)
			startTcpServer(m_tsf);

		m_context = bundleContext;
	}

	void deactivate() {
		stopTcpServer();
		m_context = null;
		m_conf = null;
	}

	static class LoggerOutStream extends OutputStream {

		private final BytesBuilder m_builder;
		private final ICharsetCodec m_codec;

		public LoggerOutStream(BytesBuilder builder, ICharsetCodec codec) {
			m_builder = builder;
			m_codec = codec;
		}

		@Override
		public void write(int b) {
			m_builder.append((byte) b);
		}

		@Override
		public void write(byte[] b) {
			m_builder.append(b);
		}

		@Override
		public void write(byte[] b, int off, int len) {
			m_builder.append(b, off, len);
		}

		@Override
		public void flush() {
			String msg = toMsg();
			if (msg != null && !(msg = msg.trim()).isEmpty())
				c_logger.info(msg);
		}

		@Override
		public void close() {
			flush();
		}

		protected String toMsg() {
			final BytesBuilder builder = m_builder;
			final int n = builder.length();
			if (n < 1)
				return null;

			try {
				return m_codec.toString(builder.getByteBuffer(0, n));
			} finally {
				builder.setLength(0);
			}
		}
	}

	static final class LoggerErrStream extends LoggerOutStream {

		public LoggerErrStream(BytesBuilder builder, ICharsetCodec codec) {
			super(builder, codec);
		}

		@Override
		public void flush() {
			String msg = toMsg();
			if (msg != null && !(msg = msg.trim()).isEmpty())
				c_logger.error(msg);
		}
	}

	static final class ScriptFilter implements FileFilter {

		private final Matcher m_matcher;
		private final ArrayList<String> m_provisioned;

		public ScriptFilter(ArrayList<String> provisioned) {
			m_matcher = Pattern.compile("\\d\\d-\\w+\\.ry").matcher("");
			m_provisioned = provisioned;
		}

		@Override
		public boolean accept(File pathname) {
			final String name = pathname.getName();
			return pathname.isFile() && !m_provisioned.contains(name) && m_matcher.reset(name).matches();
		}
	}

	private void provision(BundleContext context) throws Throwable {
		File provisioned = context.getBundle().getDataFile(".provisioned");
		if (provisioned == null)
			provisioned = new File(context.getProperty(FRAMEWORK_STORAGE), ".provisioned");
		final ArrayList<String> provisionedScripts = new ArrayList<>();
		try (RandomAccessFile raf = new RandomAccessFile(provisioned, "rw")) {
			if (provisioned.exists()) {
				String line;
				while ((line = raf.readLine()) != null)
					provisionedScripts.add(line);
			}

			final File provDir = new File(context.getProperty(Constants.JRUYI_INST_PROV_DIR));
			if (!provDir.exists())
				return;

			final File[] files = provDir.listFiles(new ScriptFilter(provisionedScripts));
			if (files == null || files.length < 1)
				return;

			c_logger.info("Provisioning...");

			Arrays.sort(files);
			for (File file : files) {
				c_logger.info("Execute script: {}", file.getCanonicalPath());
				execute(file, context);
				raf.writeBytes(file.getName());
				raf.writeBytes(StrUtil.getLineSeparator());
			}
		}
		c_logger.info("Done provisioning");
	}

	private void execute(File file, BundleContext context) throws Throwable {
		try (BytesBuilder builder = BytesBuilder.get((int) file.length() + 1)) {
			try (InputStream in = new FileInputStream(file)) {
				builder.read(in);
			}

			final int len = builder.length();
			if (len < 1)
				return;

			final ICharsetCodec codec = CharsetCodec.get("UTF-8");
			String script = codec.toString(builder.getByteBuffer(0, len));
			builder.setLength(0);
			final CommandSession cs = m_cp.createSession(null, new PrintStream(new LoggerOutStream(builder, codec)),
					new PrintStream(new LoggerErrStream(builder, codec)));
			try {
				script = filterProps(script, cs, context);
				cs.execute(script);
			} finally {
				cs.close();
			}
		}
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

	private void startTcpServer(ComponentFactory tsf) throws Throwable {
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

	private void stopTcpServer() {
		final ComponentInstance tcpServer = m_tcpServer;
		if (tcpServer != null) {
			m_tcpServer = null;
			tcpServer.dispose();
		}
	}

	private void loadBrandingInfo(String url, BundleContext context) throws Throwable {
		java.util.Properties branding = loadBrandingProps(CliServer.class.getResourceAsStream("branding.properties"));
		if (url != null)
			branding.putAll(loadBrandingProps(new URL(url).openStream()));

		m_welcome = CharsetCodec.get(CharsetCodec.UTF_8)
				.toBytes(StrUtil.filterProps(branding.getProperty(WELCOME), context));
	}

	private static java.util.Properties loadBrandingProps(InputStream in) throws Throwable {
		java.util.Properties props = new java.util.Properties();
		try {
			props.load(new BufferedInputStream(in));
		} finally {
			in.close();
		}
		return props;
	}

	private static String filterProps(String target, CommandSession cs, BundleContext context) {
		if (target.length() < 2)
			return target;

		try (StringBuilder builder = StringBuilder.get(); IntStack stack = IntStack.get()) {
			final int j = target.length();
			String propValue = null;
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
						propValue = getPropValue(builder.substring(index + 2), cs, context);
						if (propValue != null) {
							builder.setLength(index);
							builder.append(propValue);
							continue;
						}
					}
				}

				builder.append(c);
			}

			if (propValue != null || builder.length() != j)
				target = builder.toString();
		}

		return target;
	}

	private static String getPropValue(String name, CommandSession cs, BundleContext context) {
		final Object value = cs.get(name);
		if (value != null)
			return value.toString();

		return context.getProperty(name);
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
