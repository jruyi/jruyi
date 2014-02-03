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
package org.jruyi.me.mq;

import java.io.Closeable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.jruyi.common.ICloseable;
import org.jruyi.common.IDumpable;
import org.jruyi.common.IThreadLocalCache;
import org.jruyi.common.Properties;
import org.jruyi.common.StrUtil;
import org.jruyi.common.StringBuilder;
import org.jruyi.common.ThreadLocalCache;
import org.jruyi.me.IMessage;
import org.jruyi.me.route.IRoutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Message implements Runnable, IMessage, IRoutable,
		ICloseable, IDumpable {

	private static final Logger c_logger = LoggerFactory
			.getLogger(Message.class);
	private static final String NULL = "jruyi.me.endpoint.null";
	private static final IThreadLocalCache<Message> c_cache = ThreadLocalCache
			.weakLinkedCache();
	private static final AtomicLong c_counter = new AtomicLong(0L);
	private final Properties m_properties;
	private final IdentityHashMap<Object, Object> m_storage;
	private long m_id;
	private String m_from;
	private String m_to;
	private Object m_attachment;
	private Endpoint m_endpoint;

	static Message get() {
		Message message = c_cache.take();
		if (message == null)
			message = new Message();

		message.m_id = c_counter.incrementAndGet();
		return message;
	}

	private Message get(Map<String, Object> properties,
			Map<Object, Object> storage) {
		Message message = c_cache.take();
		if (message == null)
			message = new Message(properties, storage);
		else {
			message.m_properties.putAll(properties);
			message.m_storage.putAll(storage);
		}

		message.m_id = c_counter.incrementAndGet();
		return message;
	}

	private Message() {
		m_properties = new Properties();
		m_storage = new IdentityHashMap<Object, Object>();
	}

	private Message(Map<String, Object> properties, Map<Object, Object> storage) {
		m_properties = new Properties(properties);
		m_storage = new IdentityHashMap<Object, Object>(storage);
	}

	@Override
	public void run() {
		Endpoint mqProxy = m_endpoint;
		m_endpoint = null;
		mqProxy.consume(this);
	}

	@Override
	public Object attach(Object attachment) {
		Object oldAttachment = m_attachment;
		m_attachment = attachment;
		return oldAttachment;
	}

	@Override
	public Object attachment() {
		return m_attachment;
	}

	@Override
	public void clearProperties() {
		m_properties.clear();
	}

	@Override
	public Object detach() {
		Object attachment = m_attachment;
		m_attachment = null;
		return attachment;
	}

	@Override
	public long id() {
		return m_id;
	}

	@Override
	public String from() {
		return m_from;
	}

	public void from(String from) {
		m_from = from;
	}

	@Override
	public Object withdraw(Object id) {
		return m_storage.remove(id);
	}

	@Override
	public Object deposit(Object id, Object stuff) {
		return m_storage.put(id, stuff);
	}

	@Override
	public Object inquiry(Object id) {
		return m_storage.get(id);
	}

	@Override
	public Map<String, ?> getProperties() {
		return m_properties;
	}

	@Override
	public Object getProperty(String name) {
		return m_properties.get(name);
	}

	@Override
	public String to() {
		return m_to;
	}

	@Override
	public void putProperties(Map<String, ?> properties) {
		m_properties.putAll(properties);
	}

	@Override
	public Object putProperty(String name, Object value) {
		return m_properties.put(name, value);
	}

	@Override
	public Object removeProperty(String name) {
		return m_properties.remove(name);
	}

	@Override
	public void to(String to) {
		m_to = to;
	}

	@Override
	public boolean isToNull() {
		return m_to == NULL;
	}

	@Override
	public void toNull() {
		m_to = NULL;
	}

	@Override
	public Map<String, ?> getRoutingInfo() {
		return m_properties;
	}

	@Override
	public IMessage duplicate() {
		return get(m_properties, m_storage);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass())
			return false;

		return m_id == ((Message) obj).m_id;
	}

	@Override
	public int hashCode() {
		return (int) m_id;
	}

	@Override
	public void dump(StringBuilder builder) {
		String lineSeparator = StrUtil.getLineSeparator();

		builder.append(lineSeparator).append("Message ID: ").append(m_id)
				.append(lineSeparator).append("From: ").append(m_from)
				.append(lineSeparator).append("Properties: ");

		Iterator<Entry<String, Object>> iter = m_properties.entrySet()
				.iterator();
		while (iter.hasNext()) {
			Entry<String, ?> entry = iter.next();
			builder.append(lineSeparator).append('\t').append(entry.getKey())
					.append('=').append(entry.getValue());
		}

		builder.append(lineSeparator).append("Attachment:")
				.append(lineSeparator).append(m_attachment)
				.append(lineSeparator);
	}

	@Override
	public String toString() {
		StringBuilder builder = StringBuilder.get();
		try {
			dump(builder);
			return builder.toString();
		} finally {
			builder.close();
		}
	}

	@Override
	public void close() {
		m_endpoint = null;
		m_properties.clear();
		m_from = null;
		m_to = null;
		m_storage.clear();
		Object attachment = m_attachment;
		try {
			if (attachment != null) {
				m_attachment = null;
				if (attachment instanceof Closeable) {
					try {
						((Closeable) attachment).close();
					} catch (Throwable t) {
						c_logger.error(StrUtil.join(
								"Failed to close attachment: ", attachment), t);
					}
				}
			}
		} finally {
			c_cache.put(this);
		}
	}

	void setEndpoint(Endpoint endpoint) {
		m_endpoint = endpoint;
	}
}
