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

package org.jruyi.common;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class provides a default implementation for state transition of a
 * service.
 */
public abstract class Service implements IService {

	private final ReentrantLock m_lock = new ReentrantLock();
	private State m_state = Stopped.INST;

	static abstract class State {

		private final int m_state;

		State(int state) {
			m_state = state;
		}

		public void start(Service service) throws Exception {
		}

		public void start(Service service, int options) throws Exception {
		}

		public void stop(Service service) {
		}

		public void stop(Service service, int options) {
		}

		public void update(Service service, Map<String, ?> properties) throws Exception {
		}

		public final int state() {
			return m_state;
		}
	}

	static final class Active extends State {

		static final State INST = new Active(ACTIVE);

		private Active(int state) {
			super(state);
		}

		@Override
		public void stop(Service service) {
			service.changeState(Stopping.INST);
			service.stopInternal();
			service.changeState(Stopped.INST);
		}

		@Override
		public void stop(Service service, int options) {
			service.changeState(Stopping.INST);
			service.stopInternal(options);
			service.changeState(Stopped.INST);
		}

		@Override
		public void update(Service service, Map<String, ?> properties) throws Exception {
			if (service.updateInternal(properties)) {
				service.stop();
				service.start();
			}
		}

		@Override
		public String toString() {
			return "Active";
		}
	}

	static final class Inactive extends State {

		static final State INST = new Inactive(INACTIVE);

		private Inactive(int state) {
			super(state);
		}

		@Override
		public void start(Service service) throws Exception {
			service.changeState(Starting.INST);
			try {
				service.startInternal();
			} catch (Exception e) {
				service.changeState(Inactive.INST);
				throw e;
			}

			service.changeState(Active.INST);
		}

		@Override
		public void start(Service service, int options) throws Exception {
			service.changeState(Starting.INST);
			try {
				service.startInternal(options);
			} catch (Exception e) {
				service.changeState(Inactive.INST);
				throw e;
			}

			service.changeState(Active.INST);
		}

		@Override
		public void stop(Service service) {
			service.changeState(Stopped.INST);
		}

		@Override
		public void stop(Service service, int options) {
			service.changeState(Stopped.INST);
		}

		@Override
		public void update(Service service, Map<String, ?> properties) throws Exception {
			if (service.updateInternal(properties))
				service.start();
		}

		@Override
		public String toString() {
			return "Inactive";
		}
	}

	static final class Stopped extends State {

		static final State INST = new Stopped(STOPPED);

		private Stopped(int state) {
			super(state);
		}

		@Override
		public void start(Service service) throws Exception {
			service.changeState(Starting.INST);
			try {
				service.startInternal();
			} catch (Exception e) {
				service.changeState(Inactive.INST);
				throw e;
			}

			service.changeState(Active.INST);
		}

		@Override
		public void start(Service service, int options) throws Exception {
			service.changeState(Starting.INST);
			try {
				service.startInternal(options);
			} catch (Exception e) {
				service.changeState(Inactive.INST);
				throw e;
			}

			service.changeState(Active.INST);
		}

		@Override
		public void update(Service service, Map<String, ?> properties) throws Exception {
			service.updateInternal(properties);
		}

		@Override
		public String toString() {
			return "Stopped";
		}
	}

	static final class Starting extends State {

		static final State INST = new Starting(STARTING);

		private Starting(int state) {
			super(state);
		}

		@Override
		public String toString() {
			return "Starting";
		}
	}

	static final class Stopping extends State {

		static final State INST = new Stopping(STOPPING);

		private Stopping(int state) {
			super(state);
		}

		@Override
		public String toString() {
			return "Stopping";
		}
	}

	/**
	 * Starts this service and the service state changes to {@code STARTED}.
	 * 
	 * @throws Exception
	 *             If this service failed to start.
	 */
	@Override
	public final void start() throws Exception {
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			m_state.start(this);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Starts this service with the specified {@code options} and the service
	 * state changes to {@code STARTED}.
	 * 
	 * @param options
	 *            start options
	 * @throws Exception
	 *             If this service failed to start.
	 */
	@Override
	public final void start(int options) throws Exception {
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			m_state.start(this, options);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Stops this service and the state changes to {@code STOPPED}.
	 */
	@Override
	public final void stop() {
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			m_state.stop(this);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Stops this service with the specified {@code options} and the service
	 * state changes to {@code STOPPED}.
	 * 
	 * @param options
	 *            stop options
	 */
	@Override
	public final void stop(int options) {
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			m_state.stop(this, options);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Updates the properties of this service. This service may restart if it's
	 * required to take the new properties.
	 * 
	 * @param properties
	 *            the properties to be updated to
	 * @throws Exception
	 *             if any error occurs
	 */
	@Override
	public void update(Map<String, ?> properties) throws Exception {
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			m_state.update(this, properties);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Returns the current state of this service.
	 * 
	 * @return the current state
	 */
	@Override
	public final int state() {
		return m_state.state();
	}

	/**
	 * A callback method for a derived class to do some customized work for
	 * starting.
	 * 
	 * @throws Exception
	 *             If any error occurs
	 */
	protected abstract void startInternal() throws Exception;

	/**
	 * A callback method for a derived class to do some customized work for
	 * starting with options. This default implementation just calls
	 * {@code startInternal()}.
	 * 
	 * @param options
	 *            start options
	 * @throws Exception
	 *             If any error occurs
	 */
	protected void startInternal(int options) throws Exception {
		startInternal();
	}

	/**
	 * A callback method for a derived class to do some customized work for
	 * stopping.
	 */
	protected abstract void stopInternal();

	/**
	 * A callback method for a derived class to do some customized work for
	 * stopping with options. This default implementation just calls
	 * {@code stopInternal()}.
	 * 
	 * @param options
	 *            stop options
	 */
	protected void stopInternal(int options) {
		stopInternal();
	}

	/**
	 * A callback method for a derived class to handle properties to be updated.
	 * 
	 * @param properties
	 *            the new properties to be updated to
	 * @return true if need restart, otherwise false
	 * @throws Exception
	 *             If any error occurs
	 */
	protected boolean updateInternal(Map<String, ?> properties) throws Exception {
		return false;
	}

	final void changeState(State state) {
		m_state = state;
	}
}
