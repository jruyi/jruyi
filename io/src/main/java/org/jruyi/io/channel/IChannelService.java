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

import org.jruyi.io.IBufferFactory;
import org.jruyi.io.IFilter;

public interface IChannelService {

	public Object getConfiguration();

	public IChannelAdmin getChannelAdmin();

	public IBufferFactory getBufferFactory();

	public int throttle();

	public IFilter<?, ?>[] getFilterChain();

	public void onChannelOpened(IChannel channel);

	public void onChannelClosed(IChannel channel);

	public void onMessageReceived(IChannel channel, Object msg);

	// The given {@code data} will be closed right after this method returns.
	public void onMessageSent(IChannel channel, Object msg);

	public void onChannelException(IChannel channel, Throwable t);

	public void onChannelIdleTimedOut(IChannel channel);

	public void onChannelConnectTimedOut(IChannel channel);

	public void onChannelReadTimedOut(IChannel channel);
}
