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
	void onSessionOpened(ISession session);

	/**
	 * Callback method on session closed.
	 * 
	 * @param session
	 *            the closed session
	 */
	void onSessionClosed(ISession session);

	/**
	 * Callback method right before sending message.
	 *
	 * @param session
	 *            the session to send the message
	 * @param outMsg
	 *            the message to be sent
	 * @since 2.1
	 */
	void beforeSendMessage(ISession session, O outMsg);

	/**
	 * Callback method on message sent.
	 * 
	 * @param session
	 *            the session sent the message
	 * @param outMsg
	 *            the message sent
	 */
	void onMessageSent(ISession session, O outMsg);

	/**
	 * Callback method on message received.
	 * 
	 * @param session
	 *            the session received the message
	 * @param inMsg
	 *            the message received
	 */
	void onMessageReceived(ISession session, I inMsg);

	/**
	 * Callback method on session exception.
	 * 
	 * @param session
	 *            the session got exception
	 * @param cause
	 *            the exception
	 */
	void onSessionException(ISession session, Throwable cause);

	/**
	 * Callback method on session idle timed out.
	 * 
	 * @param session
	 *            the idle timed out session
	 */
	void onSessionIdleTimedOut(ISession session);

	/**
	 * Callback method on session connect timed out.
	 * 
	 * @param session
	 *            the connect timed out session
	 */
	void onSessionConnectTimedOut(ISession session);

	/**
	 * Callback method on response timed out for the specified {@code msg}.
	 * 
	 * @param session
	 *            the session in which the response timed out
	 * @param outMsg
	 *            the request for which the response timed out
	 */
	void onSessionReadTimedOut(ISession session, O outMsg);
}
