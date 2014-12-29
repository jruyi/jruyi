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

import java.nio.ByteBuffer;

import org.jruyi.io.ISession;

public interface IChannel extends ISession, ISelectableChannel {

	public IChannelService channelService();

	public void connect(int timeout);

	public void receive(ByteBuffer data);

	public void write(Object data);

	public void onReadRequired();

	public void onWriteRequired();

	public boolean scheduleIdleTimeout(int timeout);

	public boolean scheduleConnectTimeout(int timeout);

	public boolean scheduleReadTimeout(int timeout);

	public boolean cancelTimeout();

	/**
	 * Tests whether this session is closed.
	 * 
	 * @return {@code true} if this session is closed, otherwise {@code false}
	 */
	public boolean isClosed();
}
