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

package org.jruyi.me.msglog;

import org.jruyi.common.StringBuilder;
import org.jruyi.me.IMessage;
import org.jruyi.me.IPostHandler;
import org.jruyi.me.IPreHandler;
import org.jruyi.me.MeConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "jruyi.me.msglog.handler", //
configurationPolicy = ConfigurationPolicy.IGNORE, //
property = { MeConstants.HANDLER_ID + "=" + MeConstants.HID_MSGLOG }, //
xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class MsgLogHandler implements IPreHandler, IPostHandler {

	private static final Logger c_logger = LoggerFactory.getLogger(MsgLogHandler.class);

	@Override
	public Boolean preHandle(IMessage message) {
		final String s;
		try (StringBuilder builder = StringBuilder.get()) {
			s = builder.append("Endpoint[").append(message.to()).append("], dequeue:").append(message).toString();
		}
		c_logger.info(s);
		return Boolean.TRUE;
	}

	@Override
	public Boolean postHandle(IMessage message) {
		final String s;
		try (StringBuilder builder = StringBuilder.get()) {
			s = builder.append("Endpoint[").append(message.from()).append("], enqueue:").append(message).toString();
		}
		c_logger.info(s);
		return Boolean.TRUE;
	}
}
