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

package org.jruyi.io.channel;

import org.jruyi.io.IBuffer;
import org.jruyi.io.ISession;

public interface IChannel extends ISession, ISelectableChannel {

	IChannelService<Object, Object> channelService();

	ISelector selector();

	void connect(int timeout);

	void write(Object data);

	boolean scheduleIdleTimeout(int timeout);

	boolean scheduleConnectTimeout(int timeout);

	boolean scheduleReadTimeout(int timeout);

	boolean cancelTimeout();

	void receive(IBuffer in);

	/**
	 * Tests whether this session is closed.
	 * 
	 * @return {@code true} if this session is closed, otherwise {@code false}
	 */
	boolean isClosed();
}
