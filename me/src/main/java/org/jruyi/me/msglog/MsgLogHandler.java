/**
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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.jruyi.common.StringBuilder;
import org.jruyi.me.IMessage;
import org.jruyi.me.IPostHandler;
import org.jruyi.me.IPreHandler;
import org.jruyi.me.MeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Component(name = "jruyi.me.msglog.handler", policy = ConfigurationPolicy.IGNORE, createPid = false)
@Property(name = MeConstants.HANDLER_ID, value = MeConstants.HID_MSGLOG)
public final class MsgLogHandler implements IPreHandler, IPostHandler {

	private static final Logger c_logger = LoggerFactory
			.getLogger(MsgLogHandler.class);

	@Override
	public boolean preHandle(IMessage message) {
		String s;
		StringBuilder builder = StringBuilder.get();
		try {
			s = builder.append("Endpoint[").append(message.to())
					.append("], dequeue:").append(message).toString();
		} finally {
			builder.close();
		}

		c_logger.info(s);

		return true;
	}

	@Override
	public boolean postHandle(IMessage message) {

		String s;
		StringBuilder builder = StringBuilder.get();
		try {
			s = builder.append("Endpoint[").append(message.from())
					.append("], enqueue:").append(message).toString();
		} finally {
			builder.close();
		}

		c_logger.info(s);

		return true;
	}
}
