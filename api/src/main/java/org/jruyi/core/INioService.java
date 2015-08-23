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

package org.jruyi.core;

import org.jruyi.io.ISession;
import org.jruyi.io.ISessionListener;

/**
 * An event based IO service using Java NIO.
 *
 * @param <I>
 *            The type of the data to be received
 * @param <O>
 *            The type of the data to be sent
 * @param <C>
 *            The type of the configuration
 * @since 2.2
 */
public interface INioService<I, O, C> {

	/**
	 * Returns the ID of this NIO service.
	 * 
	 * @return the service ID
	 */
	String id();

	/**
	 * Returns the configuration used to configure this NIO service.
	 * 
	 * @return the service configuration
	 */
	C configuration();

	/**
	 * Returns the filter chain associated with this NIO service.
	 * 
	 * @return the filter chain
	 */
	IFilterChain filterChain();

	/**
	 * Returns the buffer factory associated with this NIO service.
	 * 
	 * @return the buffer factory
	 */
	IBufferFactory bufferFactory();

	/**
	 * Associates the specified {@code bufferFactory} with this NIO service.
	 * 
	 * @param bufferFactory
	 *            the buffer factory to associate
	 * @return this NIO service
	 */
	INioService<I, O, C> bufferFactory(IBufferFactory bufferFactory);

	/**
	 * Sets the specified listener to this NIO service to handle all the IO
	 * events.
	 * 
	 * @param listener
	 *            the listener to set
	 * @return this NIO service
	 */
	INioService<I, O, C> sessionListener(ISessionListener<I, O> listener);

	/**
	 * Establishes a connection to the remote peer.
	 */
	void openSession();

	/**
	 * Closes the specified {@code session}.
	 * 
	 * @param session
	 *            the session to be closed
	 */
	void closeSession(ISession session);

	/**
	 * Writes the specified {@code msg} the specified {@code session}.
	 * 
	 * @param session
	 *            the session to be written to
	 * @param msg
	 *            the message to write
	 */
	void write(ISession session, O msg);

	/**
	 * Starts this NIO service.
	 * 
	 * @throws Throwable
	 *             if any error happens
	 */
	void start() throws Throwable;

	/**
	 * Stops this NIO service.
	 */
	void stop();
}
