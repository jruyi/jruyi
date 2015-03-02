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

package org.jruyi.common.internal;

import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.jruyi.common.IServiceHolder;
import org.jruyi.common.StrUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

final class ServiceHolder<T> implements IServiceHolder<T> {

	private final String m_name;
	private final BundleContext m_context;
	private final PriorityQueue<ServiceReference<T>> m_references;
	private final ReentrantLock m_lock;
	private int m_count;
	private T m_service;
	private State m_state = Unresolved.INST;

	static abstract class State {

		<T> void add(ServiceHolder<T> holder, ServiceReference<T> reference) {
		}

		<T> void remove(ServiceHolder<T> holder, ServiceReference<T> reference) {
		}

		<T> T activate(ServiceHolder<T> holder) {
			throw new RuntimeException(StrUtil.join(holder.getId(), " is unavailable"));
		}

		<T> T getService(ServiceHolder<T> holder) {
			throw new RuntimeException(StrUtil.join(holder.getId(), " is unavailable"));
		}

		<T> void deactivate(ServiceHolder<T> holder) {
		}
	}

	static final class Unresolved extends State {

		static final State INST = new Unresolved();

		private Unresolved() {
		}

		@Override
		<T> void add(ServiceHolder<T> holder, ServiceReference<T> reference) {
			holder.addInternal(reference);
			holder.changeState(Resolved.INST);
		}
	}

	static final class Resolved extends State {

		static final State INST = new Resolved();

		private Resolved() {
		}

		@Override
		<T> void add(ServiceHolder<T> holder, ServiceReference<T> reference) {
			holder.addInternal(reference);
		}

		@Override
		<T> void remove(ServiceHolder<T> holder, ServiceReference<T> reference) {
			holder.removeInternal(reference);
			if (holder.getServiceReference() == null)
				holder.changeState(Unresolved.INST);
		}

		@Override
		<T> T activate(ServiceHolder<T> holder) {
			holder.activateInternal();
			holder.changeState(Activated.INST);
			return holder.getServiceInternal();
		}

		@Override
		<T> T getService(ServiceHolder<T> holder) {
			return holder.activate();
		}
	}

	static final class Activated extends State {

		static final State INST = new Activated();

		private Activated() {
		}

		@Override
		<T> void add(ServiceHolder<T> holder, ServiceReference<T> reference) {
			holder.addInternal(reference);
			if (holder.getServiceReference() == reference)
				holder.changeState(Resolved.INST);
		}

		@Override
		<T> void remove(ServiceHolder<T> holder, ServiceReference<T> reference) {
			if (holder.getServiceReference() == reference) {
				holder.changeState(Resolved.INST);
				holder.deactivateInternal();
			}

			holder.removeInternal(reference);
			if (holder.getServiceReference() == null)
				holder.changeState(Unresolved.INST);
		}

		@Override
		<T> T activate(ServiceHolder<T> holder) {
			return holder.getServiceInternal();
		}

		@Override
		<T> T getService(ServiceHolder<T> holder) {
			return holder.getServiceInternal();
		}

		@Override
		<T> void deactivate(ServiceHolder<T> holder) {
			holder.deactivateInternal();
			holder.changeState(Resolved.INST);
		}
	}

	ServiceHolder(String name, BundleContext context) {
		m_name = name;
		m_context = context;
		m_references = new PriorityQueue<>(3);
		m_lock = new ReentrantLock();
	}

	@Override
	public String getId() {
		return m_name;
	}

	@Override
	public T getService() {
		return m_state.getService(this);
	}

	int incRef() {
		return ++m_count;
	}

	int decRef() {
		return --m_count;
	}

	void add(ServiceReference<T> reference) {
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			m_state.add(this, reference);
		} finally {
			lock.unlock();
		}
	}

	void remove(ServiceReference<T> reference) {
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			m_state.remove(this, reference);
		} finally {
			lock.unlock();
		}
	}

	T activate() {
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			return m_state.activate(this);
		} finally {
			lock.unlock();
		}
	}

	void deactivate() {
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			m_state.deactivate(this);
		} finally {
			lock.unlock();
		}
	}

	T getServiceInternal() {
		return m_service;
	}

	void activateInternal() {
		m_service = m_context.getService(m_references.peek());
	}

	void deactivateInternal() {
		m_context.ungetService(m_references.peek());
		m_service = null;
	}

	void addInternal(ServiceReference<T> reference) {
		m_references.add(reference);
	}

	void removeInternal(ServiceReference<T> reference) {
		m_references.remove(reference);
	}

	ServiceReference<T> getServiceReference() {
		return m_references.peek();
	}

	void changeState(State state) {
		m_state = state;
	}
}
