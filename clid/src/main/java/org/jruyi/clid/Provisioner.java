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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.jruyi.common.BytesBuilder;
import org.jruyi.common.CharsetCodec;
import org.jruyi.common.ICharsetCodec;
import org.jruyi.common.StrUtil;
import org.jruyi.system.Constants;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "jruyi.clid.provisioner", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.IGNORE, //
		xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class Provisioner {

	private static final Logger c_logger = LoggerFactory.getLogger(Provisioner.class);

	private CommandProcessor m_cp;

	@Reference(name = "cp")
	void setCommandProcessor(CommandProcessor cp) {
		m_cp = cp;
	}

	void unsetCommandProcessor(CommandProcessor cp) {
		m_cp = null;
	}

	void activate(BundleContext context) throws Throwable {
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

			c_logger.info("Start provisioning...");

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
			final CommandSession cs = m_cp.createSession(Util.DUMMY_INPUT, new LoggerOutStream(builder, codec),
					new LoggerErrStream(builder, codec));
			try {
				script = Util.filterProps(script, cs, context);
				cs.execute(script);
			} finally {
				cs.close();
			}
		}
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
}
