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

package org.jruyi.io.msglog;

import org.jruyi.common.StrUtil;
import org.jruyi.io.Filter;
import org.jruyi.io.IFilter;
import org.jruyi.io.IFilterOutput;
import org.jruyi.io.ISession;
import org.jruyi.io.IoConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = IoConstants.FID_MSGLOG, //
configurationPolicy = ConfigurationPolicy.IGNORE, //
service = { IFilter.class }, //
property = { IoConstants.FILTER_ID + "=" + IoConstants.FID_MSGLOG }, //
xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class MsgLogFilter extends Filter<Object, Object> {

	private static final Logger c_logger = LoggerFactory.getLogger(MsgLogFilter.class);

	public static String[] getInterfaces() {
		return new String[] { IFilter.class.getName() };
	}

	@Override
	public boolean onMsgArrive(ISession session, Object msg, IFilterOutput output) {
		c_logger.info(StrUtil.join(session, " inbound >>", StrUtil.getLineSeparator(), msg));
		output.add(msg);
		return true;
	}

	@Override
	public boolean onMsgDepart(ISession session, Object msg, IFilterOutput output) {
		c_logger.info(StrUtil.join(session, " outbound <<", StrUtil.getLineSeparator(), msg));
		output.add(msg);
		return true;
	}
}
