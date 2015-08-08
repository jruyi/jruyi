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
 * A filter is in charge of splitting the incoming data into messages, and
 * performs filtering tasks on both incoming and outgoing messages.
 * 
 * @see IFilterOutput
 * @see Filter
 * 
 * @param <I>
 *            the type of the in-bound message passed to {@link #onMsgArrive}
 * @param <O>
 *            the type of the out-bound message passed to {@link #onMsgDepart}
 */
public interface IFilter<I, O> {

	/**
	 * Indicates that more data is needed to parse out the length of a message.
	 */
	int E_UNDERFLOW = 0;
	/**
	 * Fail to parse the length of a message
	 */
	int E_ERROR = -1;

	/**
	 * Returns the minimum size of a message, usually the number of bytes that
	 * are adequate to tell the length of the whole message.
	 * 
	 * @return the minimum size of a message
	 * @since 2.0
	 */
	int msgMinSize();

	/**
	 * Returns the message length by reading and parsing the specified
	 * {@code bufferReader}.
	 * 
	 * @param session
	 *            the current IO session
	 * @param in
	 *            a byte sequence to be parsed
	 * @return {@link #E_ERROR} to discard the data due to any errors. The
	 *         specified {@code session} will be closed after this method
	 *         returns.<br>
	 *         {@link #E_UNDERFLOW} if the underlying raw data of the given
	 *         {@code in} is not sufficient to parse out the message length,
	 *         which means that more data needs to be read<br>
	 */
	int tellBoundary(ISession session, IBuffer in);

	/**
	 * Filters the incoming data which is passed from the left most filter to
	 * the right most filter in the chain in order. The filtered output is
	 * passed onto the next filter via the given {@code output}.
	 * <p>
	 * If this method returns true and the output is not empty, then the output
	 * will be passed to the next filter's {@code onMsgArrive} as input
	 * {@code msg}.
	 * <p>
	 * If this method returns true and the output is empty, then the
	 * filter-chain's {@code onMsgArrive} routine will abort.
	 * <p>
	 * If this method returns false and the output is empty, then the
	 * filter-chain's {@code onMsgArrive} routine will abort and the given
	 * {@code session} will be closed.
	 * <p>
	 * If this method returns false but the output is returned, then the inbound
	 * filter routine stops here and the output will go backward doing outbound
	 * filtering.
	 * 
	 * @param session
	 *            the current IO session
	 * @param msg
	 *            the incoming message
	 * @param output
	 *            an object used to pass the output to the next filter in the
	 *            filter chain
	 * @return true if going to next filter otherwise false.
	 */
	boolean onMsgArrive(ISession session, I msg, IFilterOutput output);

	/**
	 * Filters the outgoing data which is passed from the right most filter to
	 * the left most filter in the chain in order. The filtered output is passed
	 * onto the next filter via the given {@code output}.
	 * <p>
	 * If this method returns true and the output is not empty, then the output
	 * will be passed to the next filter's {@code onMsgDepart} as {@code msg}.
	 * <p>
	 * If this method returns true and the output is empty, then the
	 * filter-chain's {@code onMsgDepart} routine will abort.
	 * <p>
	 * If this method returns false, then the filter-chain's {@code onMsgDepart}
	 * routine will abort.
	 * 
	 * @param session
	 *            the current IO session
	 * @param msg
	 *            the outgoing message
	 * @param output
	 *            an object used to pass the output to the next filter in the
	 *            filter chain
	 * @return true if going to the next filter otherwise false
	 */
	boolean onMsgDepart(ISession session, O msg, IFilterOutput output);
}
