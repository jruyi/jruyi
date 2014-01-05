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

import java.util.Arrays;

import org.jruyi.common.IDumpable;
import org.jruyi.common.StrUtil;
import org.jruyi.common.StringBuilder;
import org.jruyi.me.IConsumer;
import org.jruyi.me.IMessage;
import org.jruyi.me.IPostHandler;
import org.jruyi.me.IPreHandler;
import org.jruyi.me.IProducer;
import org.jruyi.me.IRoute;
import org.jruyi.me.route.IRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Endpoint implements IProducer, IConsumer, IDumpable {

	private static final Logger c_logger = LoggerFactory
			.getLogger(Endpoint.class);
	private String m_id;
	private IRouter m_router;
	private final MessageQueue m_mq;
	private String[] m_preHandlerIds;
	private String[] m_postHandlerIds;
	private IPreHandler[] m_preHandlers;
	private IPostHandler[] m_postHandlers;
	private Producer m_producer = Producer.OPENED;

	enum Producer {

		CLOSED, OPENED {

			@Override
			public final IMessage createMessage(Endpoint endpoint) {
				return Message.get();
			}

			@Override
			public final void send(IMessage message, Endpoint endpoint) {
				if (message == null)
					return;

				Message msg = (Message) message;
				msg.from(endpoint.id());

				if (!endpoint.onEnqueue(message)) {
					message.close();
					return;
				}

				if (msg.to() == null) {
					IRoute entry = endpoint.router().route(msg);
					if (entry == null) {
						c_logger.warn(StrUtil.join("Route Not Found:", msg));
						msg.close();
						return;
					}
					msg.to(entry.to());
				}

				endpoint.mq().dispatch(msg);
			}
		};

		public IMessage createMessage(Endpoint endpoint) {
			throw new IllegalStateException();
		}

		public void send(IMessage message, Endpoint endpoint) {
			throw new IllegalStateException();
		}
	}

	Endpoint(String id, MessageQueue mq) {
		m_id = id;
		m_mq = mq;
		m_router = mq.getRouter(id);

		String[] preHandlerIds = StrUtil.getEmptyStringArray();
		String[] postHandlerIds = StrUtil.getEmptyStringArray();

		m_preHandlerIds = preHandlerIds;
		m_postHandlerIds = postHandlerIds;
		m_preHandlers = mq.getPreHandlers(preHandlerIds);
		m_postHandlers = mq.getPostHandlers(postHandlerIds);
	}

	final String id() {
		return m_id;
	}

	void id(String id) {
		m_id = id;
		m_router = m_mq.getRouter(id);
	}

	final MessageQueue mq() {
		return m_mq;
	}

	final IRouter router() {
		return m_router;
	}

	final void closeProducer() {
		m_producer = Producer.CLOSED;
	}

	@Override
	public final IMessage createMessage() {
		return m_producer.createMessage(this);
	}

	@Override
	public final void send(IMessage message) {
		m_producer.send(message, this);
	}

	final void consume(Message message) {
		if (!onDequeue(message)) {
			message.close();
			return;
		}

		message.to(null);
		try {
			getConsumer().onMessage(message);
		} catch (Throwable t) {
			c_logger.error(
					StrUtil.join(this, " failed to consume message: ", message),
					t);
		}
	}

	IConsumer getConsumer() {
		return this;
	}

	@Override
	public void onMessage(IMessage message) {
		c_logger.error(StrUtil
				.join(this, " doesn't consume message: ", message));
		message.close();
	}

	@Override
	public int hashCode() {
		return m_id.hashCode();
	}

	@Override
	public void dump(StringBuilder builder) {
		builder.append("Endpoint[").append(m_id).append(']');
	}

	final void setPreHandlers(String[] names) {
		if (names == null)
			names = StrUtil.getEmptyStringArray();

		String[] oldNames = m_preHandlerIds;
		if (Arrays.equals(oldNames, names))
			return;

		MessageQueue mq = m_mq;
		m_preHandlerIds = names;
		m_preHandlers = mq.getPreHandlers(names);

		mq.ungetPreHandlers(oldNames);
	}

	final void setPostHandlers(String[] names) {
		if (names == null)
			names = StrUtil.getEmptyStringArray();

		String[] oldNames = m_postHandlerIds;
		if (Arrays.equals(oldNames, names))
			return;

		MessageQueue mq = m_mq;
		m_postHandlerIds = names;
		m_postHandlers = mq.getPostHandlers(names);

		mq.ungetPostHandlers(oldNames);
	}

	private boolean onEnqueue(IMessage message) {
		IPostHandler[] postHandlers = m_postHandlers;
		try {
			for (IPostHandler handler : postHandlers) {
				if (!handler.postHandle(message))
					return false;
			}
		} catch (Throwable t) {
			// TODO: notify?
			c_logger.error(
					StrUtil.join(this, " unexpected error on post-handling"), t);
			return false;
		}

		return true;
	}

	private boolean onDequeue(IMessage message) {
		IPreHandler[] preHandlers = m_preHandlers;
		try {
			for (IPreHandler handler : preHandlers) {
				if (!handler.preHandle(message))
					return false;
			}
		} catch (Throwable t) {
			// TODO: notify?
			c_logger.error(
					StrUtil.join(this, " unexpected error on pre-handling"), t);
			return false;
		}

		return true;
	}
}
