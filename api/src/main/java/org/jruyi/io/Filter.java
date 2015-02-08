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
 * This abstract class provides pass-through implementation of {@link IFilter}.
 * 
 * @see IFilter
 * 
 * @param <I>
 *            the type of the in-bound message passed to {@link #onMsgArrive}
 * @param <O>
 *            the type of the out-bound message passed to {@link #onMsgDepart}
 */
public abstract class Filter<I, O> implements IFilter<I, O> {

	/**
	 * Returns {@code 0} to indicate no min size restriction.
	 * 
	 * @return zero
	 * @since 2.0
	 */
	@Override
	public int msgMinSize() {
		return 0;
	}

	/**
	 * Returns the current length of the given {@code in} as the message
	 * boundary.
	 * 
	 * @param session
	 *            the current IO session.
	 * @param in
	 *            the {@link IBuffer} holding the available data.
	 * @return the message length
	 */
	@Override
	public int tellBoundary(ISession session, IBuffer in) {
		return in.length();
	}

	/**
	 * Passes through the given {@code msg}.
	 * 
	 * @param session
	 *            the current IO session
	 * @param msg
	 *            the incoming message
	 * @param output
	 *            an {@link IFilterOutput} object used to pass the output to the
	 *            next filter in the filter chain
	 * @return true
	 */
	@Override
	public boolean onMsgArrive(ISession session, I msg, IFilterOutput output) {
		output.add(msg);
		return true;
	}

	/**
	 * Passes through the given {@code msg}.
	 * 
	 * @param session
	 *            the current IO session
	 * @param msg
	 *            the outgoing message
	 * @param output
	 *            an {@link IFilterOutput} object used to pass the output to the
	 *            previous filter in the filter chain
	 * @return true
	 */
	@Override
	public boolean onMsgDepart(ISession session, O msg, IFilterOutput output) {
		output.add(msg);
		return true;
	}
}
