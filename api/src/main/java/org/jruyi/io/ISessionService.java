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

package org.jruyi.io;

import org.jruyi.common.IService;

/**
 * This interface defines all the methods that an IO session service need
 * implement.
 * <p>
 * A session service provides an event-driven programming model to deal with IO
 * and hides the detailed IO work from developer.
 * 
 * @param <I>
 *            type of incoming message
 * @param <O>
 *            type of outgoing message
 */
public interface ISessionService<I, O> extends IService {

	/**
	 * Sets the session listener that is interested in IO session events.
	 * 
	 * @param listener
	 *            the session listener to set
	 */
	void setSessionListener(ISessionListener<I, O> listener);

	/**
	 * Requests to open a session.
	 */
	void openSession();

	/**
	 * Requests to write the specified {@code msg} to the specified
	 * {@code session}.
	 * 
	 * @param session
	 *            the IO session to write to
	 * @param msg
	 *            the message to be written
	 */
	void write(ISession session, O msg);

	/**
	 * Requests to close the specified {@code session}.
	 * 
	 * @param session
	 *            the IO session to close
	 */
	void closeSession(ISession session);
}
