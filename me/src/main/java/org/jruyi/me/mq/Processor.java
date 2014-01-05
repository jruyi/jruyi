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

import org.jruyi.common.StrUtil;
import org.jruyi.me.IMessage;
import org.jruyi.me.IProcessor;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Processor extends Endpoint {

	private static final Logger c_logger = LoggerFactory
			.getLogger(Processor.class);
	private final ServiceReference<IProcessor> m_reference;
	private final ReentrantLock m_lock = new ReentrantLock();
	private IProcessor m_processor;
	private IState m_state = Uninitialized.INST;

	interface IState {

		public IProcessor processor(Processor endpoint);
	}

	static final class Uninitialized implements IState {

		static final IState INST = new Uninitialized();

		private Uninitialized() {
		}

		@Override
		public IProcessor processor(Processor endpoint) {
			IProcessor processor;
			final ReentrantLock lock = endpoint.lock();
			lock.lock();
			try {
				processor = endpoint.processor();
				if (processor != null)
					return processor;

				processor = endpoint.mq().locateService(endpoint.reference());
				if (processor == null)
					throw new RuntimeException(StrUtil.join(endpoint,
							" is unavailable"));
				endpoint.processor(processor);
			} finally {
				lock.unlock();
			}
			endpoint.changeState(Initialized.INST);
			return processor;
		}
	}

	static final class Initialized implements IState {

		static final IState INST = new Initialized();

		private Initialized() {
		}

		@Override
		public IProcessor processor(Processor endpoint) {
			return endpoint.processor();
		}
	}

	Processor(String id, MessageQueue mq, ServiceReference<IProcessor> reference) {
		super(id, mq);
		m_reference = reference;
	}

	@Override
	public void onMessage(IMessage message) {
		m_state.processor(this).process(message);

		try {
			send(message);
		} catch (Throwable t) {
			c_logger.error(
					StrUtil.join(this, " failed to enqueue message: ", message),
					t);
		}
	}

	ServiceReference<IProcessor> reference() {
		return m_reference;
	}

	void processor(IProcessor processor) {
		m_processor = processor;
	}

	IProcessor processor() {
		return m_processor;
	}

	ReentrantLock lock() {
		return m_lock;
	}

	void changeState(IState state) {
		m_state = state;
	}
}
