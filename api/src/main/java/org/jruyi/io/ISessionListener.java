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

/**
 * This interface defines callback methods for being called on session events.
 * 
 * @param <I>
 *            type of incoming message
 * @param <O>
 *            type of outgoing message
 */
public interface ISessionListener<I, O> {

	/**
	 * Callback method on session opened.
	 * 
	 * @param session
	 *            the opened session
	 */
	public void onSessionOpened(ISession session);

	/**
	 * Callback method on session closed.
	 * 
	 * @param session
	 *            the closed session
	 */
	public void onSessionClosed(ISession session);

	/**
	 * Callback method on message sent.
	 * 
	 * @param session
	 *            the session sent the message
	 * @param outMsg
	 *            the message sent
	 */
	public void onMessageSent(ISession session, O outMsg);

	/**
	 * Callback method on message received.
	 * 
	 * @param session
	 *            the session received the message
	 * @param inMsg
	 *            the message received
	 */
	public void onMessageReceived(ISession session, I inMsg);

	/**
	 * Callback method on session exception.
	 * 
	 * @param session
	 *            the session got exception
	 * @param t
	 *            the exception
	 */
	public void onSessionException(ISession session, Throwable t);

	/**
	 * Callback method on session idle timed out.
	 * 
	 * @param session
	 *            the idle timed out session
	 */
	public void onSessionIdleTimedOut(ISession session);

	/**
	 * Callback method on session connect timed out.
	 * 
	 * @param session
	 *            the connect timed out session
	 */
	public void onSessionConnectTimedOut(ISession session);

	/**
	 * Callback method on response timed out for the specified {@code msg}.
	 * 
	 * @param session
	 *            the session in which the response timed out
	 * @param outMsg
	 *            the request for which the response timed out
	 */
	public void onSessionReadTimedOut(ISession session, O outMsg);
}
