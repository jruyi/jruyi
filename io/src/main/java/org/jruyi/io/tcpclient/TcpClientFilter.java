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
package org.jruyi.io.tcpclient;

import org.jruyi.io.Filter;
import org.jruyi.io.IFilter;
import org.jruyi.io.IFilterOutput;
import org.jruyi.io.ISession;
import org.jruyi.io.IoConstants;
import org.jruyi.me.IMessage;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@Component(name = IoConstants.FID_TCPCLIENT, //
configurationPolicy = ConfigurationPolicy.IGNORE, //
service = { IFilter.class }, //
property = { IoConstants.FILTER_ID + "=" + IoConstants.FID_TCPCLIENT }, //
xmlns = "http://www.osgi.org/xmlns/scr/v1.1.0")
public final class TcpClientFilter extends Filter<Object, Object> {

	@Override
	public boolean onMsgArrive(ISession session, Object msg,
			IFilterOutput output) {
		IMessage message = (IMessage) session
				.withdraw(IoConstants.FID_TCPCLIENT);
		message.attach(msg);
		output.add(message);
		return true;
	}

	@Override
	public boolean onMsgDepart(ISession session, Object msg,
			IFilterOutput output) {
		IMessage message = (IMessage) msg;
		msg = message.detach();
		session.deposit(IoConstants.FID_TCPCLIENT, message);
		output.add(msg);
		return true;
	}
}
