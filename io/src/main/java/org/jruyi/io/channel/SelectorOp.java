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

import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;

enum SelectorOp {

	REGISTER {
		@Override
		public void run(ISelector selector, ISelectableChannel channel) {
			try {
				channel.register(selector, SelectionKey.OP_READ);
			} catch (ClosedSelectorException | ClosedChannelException e) {
			} catch (Throwable t) {
				channel.onException(t);
			}
		}
	},
	CONNECT {
		@Override
		public void run(ISelector selector, ISelectableChannel channel) {
			try {
				channel.register(selector, SelectionKey.OP_CONNECT);
			} catch (ClosedSelectorException | ClosedChannelException e) {
			} catch (Throwable t) {
				channel.onException(t);
			}
		}
	},
	READ {
		@Override
		public void run(ISelector selector, ISelectableChannel channel) {
			try {
				channel.interestOps(SelectionKey.OP_READ);
			} catch (CancelledKeyException e) {
			} catch (Throwable t) {
				channel.onException(t);
			}
		}
	},
	WRITE {
		@Override
		public void run(ISelector selector, ISelectableChannel channel) {
			try {
				channel.interestOps(SelectionKey.OP_WRITE);
			} catch (CancelledKeyException e) {
			} catch (Throwable t) {
				channel.onException(t);
			}
		}
	};

	public abstract void run(ISelector selector, ISelectableChannel channel);
}
