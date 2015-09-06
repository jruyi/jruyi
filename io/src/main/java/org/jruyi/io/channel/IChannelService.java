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

import org.jruyi.common.ITimeoutNotifier;
import org.jruyi.io.IBufferFactory;
import org.jruyi.io.filter.IFilterList;

public interface IChannelService<I, O> {

	Object getConfiguration();

	IChannelAdmin getChannelAdmin();

	IBufferFactory getBufferFactory();

	long throttle();

	IFilterList getFilterChain();

	<S> ITimeoutNotifier<S> createTimeoutNotifier(S channel);

	void onChannelOpened(IChannel channel);

	void onChannelClosed(IChannel channel);

	void onMessageReceived(IChannel channel, I inMsg);

	void beforeSendMessage(IChannel channel, O outMsg);

	// The given {@code data} will be closed right after this method returns.
	void onMessageSent(IChannel channel, O outMsg);

	// channel will be closed
	void onChannelException(IChannel channel, Throwable t);

	void onChannelIdleTimedOut(IChannel channel);

	void onChannelConnectTimedOut(IChannel channel);

	void onChannelReadTimedOut(IChannel channel);
}
