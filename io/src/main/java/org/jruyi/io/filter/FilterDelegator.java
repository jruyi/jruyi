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
package org.jruyi.io.filter;

import org.jruyi.common.IServiceHolder;
import org.jruyi.common.StrUtil;
import org.jruyi.io.IBuffer;
import org.jruyi.io.IFilter;
import org.jruyi.io.IFilterOutput;
import org.jruyi.io.ISession;

final class FilterDelegator<I, O> implements IFilter<I, O> {

	private final IServiceHolder<IFilter<I, O>> m_holder;
	private final String m_caption;

	FilterDelegator(IServiceHolder<IFilter<I, O>> holder) {
		m_holder = holder;
		m_caption = StrUtil.join("Filter[", holder.getId(), ']');
	}

	@Override
	public int tellBoundary(ISession session, IBuffer in) {
		return m_holder.getService().tellBoundary(session, in);
	}

	@Override
	public boolean onMsgArrive(ISession session, I msg, IFilterOutput output) {
		return m_holder.getService().onMsgArrive(session, msg, output);
	}

	@Override
	public boolean onMsgDepart(ISession session, O msg, IFilterOutput output) {
		return m_holder.getService().onMsgDepart(session, msg, output);
	}

	@Override
	public String toString() {
		return m_caption;
	}

	IServiceHolder<IFilter<I, O>> serviceHolder() {
		return m_holder;
	}
}
