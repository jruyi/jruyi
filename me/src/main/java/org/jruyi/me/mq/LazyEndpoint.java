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
package org.jruyi.me.mq;

import java.util.concurrent.locks.ReentrantLock;

import org.jruyi.common.IService;
import org.jruyi.common.StrUtil;
import org.jruyi.me.IConsumer;
import org.jruyi.me.IEndpoint;
import org.jruyi.me.IMessage;
import org.osgi.framework.ServiceReference;

final class LazyEndpoint extends Endpoint {

	private final ServiceReference<IEndpoint> m_reference;
	private final ReentrantLock m_lock = new ReentrantLock();
	private IConsumer m_consumer;
	private IState m_state = Uninitialized.INST;

	interface IState {

		public IConsumer consumer(LazyEndpoint endpoint);
	}

	static final class Uninitialized implements IState {

		static final IState INST = new Uninitialized();

		private Uninitialized() {
		}

		@Override
		public IConsumer consumer(LazyEndpoint endpoint) {
			IConsumer consumer;
			final ReentrantLock lock = endpoint.lock();
			lock.lock();
			try {
				consumer = endpoint.consumer();
				if (consumer != null)
					return consumer;

				IEndpoint original = endpoint.mq().locateService(
						endpoint.reference());
				if (original == null)
					throw new RuntimeException(StrUtil.buildString(endpoint,
							" is unavailable"));
				original.producer(endpoint);
				if (original instanceof IService) {
					try {
						((IService) original).start();
					} catch (Throwable t) {
						// ignore
					}
				}
				consumer = original.consumer();
				if (consumer == null)
					consumer = endpoint;
				endpoint.consumer(consumer);
			} finally {
				lock.unlock();
			}
			endpoint.changeState(Initialized.INST);
			return consumer;
		}
	}

	static final class Initialized implements IState {

		static final IState INST = new Initialized();

		private Initialized() {
		}

		@Override
		public IConsumer consumer(LazyEndpoint endpoint) {
			return endpoint.consumer();
		}
	}

	public LazyEndpoint(String id, MessageQueue mq,
			ServiceReference<IEndpoint> reference) {
		super(id, mq);
		m_reference = reference;
	}

	@Override
	public void onMessage(IMessage message) {
		m_state.consumer(this).onMessage(message);
	}

	@Override
	IConsumer getConsumer() {
		return m_state.consumer(this);
	}

	IConsumer consumer() {
		return m_consumer;
	}

	void consumer(IConsumer consumer) {
		m_consumer = consumer;
	}

	ServiceReference<IEndpoint> reference() {
		return m_reference;
	}

	ReentrantLock lock() {
		return m_lock;
	}

	void changeState(IState state) {
		m_state = state;
	}
}
