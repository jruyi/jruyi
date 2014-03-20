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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.jruyi.common.BiListNode;
import org.jruyi.common.IServiceHolderManager;
import org.jruyi.common.ServiceHolderManager;
import org.jruyi.common.StrUtil;
import org.jruyi.me.IEndpoint;
import org.jruyi.me.IPostHandler;
import org.jruyi.me.IPreHandler;
import org.jruyi.me.IProcessor;
import org.jruyi.me.MeConstants;
import org.jruyi.me.route.IRouter;
import org.jruyi.me.route.IRouterManager;
import org.jruyi.timeoutadmin.ITimeoutAdmin;
import org.jruyi.timeoutadmin.ITimeoutEvent;
import org.jruyi.timeoutadmin.ITimeoutListener;
import org.jruyi.timeoutadmin.ITimeoutNotifier;
import org.jruyi.workshop.IWorkshop;
import org.jruyi.workshop.WorkshopConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "jruyi.me.mq", createPid = false)
@References({
		@Reference(name = "endpoint", referenceInterface = IEndpoint.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, updated = "updatedEndpoint"),
		@Reference(name = "processor", referenceInterface = IProcessor.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, updated = "updatedProcessor") })
@Properties({
		@Property(name = "endpoint.target", value = "(" + MeConstants.EP_ID
				+ "=*)"),
		@Property(name = "processor.target", value = "(" + MeConstants.EP_ID
				+ "=*)") })
public final class MessageQueue implements ITimeoutListener {

	static final PreHandlerDelegator[] EMPTY_PREHANDLERS = new PreHandlerDelegator[0];
	static final PostHandlerDelegator[] EMPTY_POSTHANDLERS = new PostHandlerDelegator[0];
	private static final Logger c_logger = LoggerFactory
			.getLogger(MessageQueue.class);

	@Property(intValue = 10)
	private static final String P_MSG_TIMEOUT = "msgTimeout";

	private final ConcurrentHashMap<String, Endpoint> m_endpoints;
	private final ReentrantLock m_lock;
	private final HashMap<Object, Endpoint> m_refEps;
	private ConcurrentHashMap<String, BiListNode<MsgNotifier>> m_nodes;
	private IServiceHolderManager<IPreHandler> m_preHandlerManager;
	private IServiceHolderManager<IPostHandler> m_postHandlerManager;

	@Reference(name = "routerManager", policy = ReferencePolicy.DYNAMIC)
	private IRouterManager m_rm;

	@Reference(name = "workshop", policy = ReferencePolicy.DYNAMIC, target = WorkshopConstants.DEFAULT_WORKSHOP_TARGET)
	private IWorkshop m_workshop;

	@Reference(name = "timeoutAdmin", policy = ReferencePolicy.DYNAMIC)
	private ITimeoutAdmin m_ta;

	private volatile ComponentContext m_context;
	private int m_msgTimeout = 10;

	static final class MsgNotifier {

		private Message m_msg;
		private ITimeoutNotifier m_notifier;

		MsgNotifier(Message msg, ITimeoutNotifier notifier) {
			m_msg = msg;
			m_notifier = notifier;
		}

		void msg(Message msg) {
			m_msg = msg;
		}

		Message msg() {
			return m_msg;
		}

		void notifier(ITimeoutNotifier notifier) {
			m_notifier = notifier;
		}

		ITimeoutNotifier notifier() {
			return m_notifier;
		}
	}

	public MessageQueue() {
		m_endpoints = new ConcurrentHashMap<String, Endpoint>();
		m_refEps = new HashMap<Object, Endpoint>();
		m_lock = new ReentrantLock();
	}

	@Override
	public void onTimeout(ITimeoutEvent event) {
		@SuppressWarnings("unchecked")
		BiListNode<MsgNotifier> node = (BiListNode<MsgNotifier>) event
				.getSubject();
		Message msg = removeNode(node);
		c_logger.warn(StrUtil.join("Message timed out:", msg));
		msg.close();
	}

	protected synchronized void bindRouterManager(IRouterManager rm) {
		m_rm = rm;
	}

	protected synchronized void unbindRouterManager(IRouterManager rm) {
		if (m_rm == rm)
			m_rm = null;
	}

	protected synchronized void bindWorkshop(IWorkshop workshop) {
		m_workshop = workshop;
	}

	protected synchronized void unbindWorkshop(IWorkshop workshop) {
		if (m_workshop == workshop)
			m_workshop = null;
	}

	protected synchronized void bindTimeoutAdmin(ITimeoutAdmin ta) {
		m_ta = ta;
	}

	protected synchronized void unbindTimeoutAdmin(ITimeoutAdmin ta) {
		if (m_ta == ta)
			m_ta = null;
	}

	protected synchronized void bindEndpoint(
			ServiceReference<IEndpoint> reference) throws Exception {
		String id = getId(reference);
		if (id == null)
			return;

		final Map<String, Endpoint> endpoints = m_endpoints;
		Endpoint endpoint = endpoints.get(id);
		if (endpoint != null) {
			c_logger.error(StrUtil.join(endpoint,
					" has already been registered"));
			return;
		}

		endpoint = new LazyEndpoint(id, this, reference);
		m_refEps.put(reference, endpoint);
		endpoints.put(id, endpoint);

		// if MQ has not been activated yet
		if (m_context == null)
			return;

		if (!isLazy(reference))
			endpoint.getConsumer();

		wakeMsgs(endpoint);
	}

	protected synchronized void unbindEndpoint(
			ServiceReference<IEndpoint> reference) {
		Endpoint endpoint = m_refEps.remove(reference);
		if (endpoint != null) {
			m_endpoints.remove(endpoint.id());
			endpoint.closeProducer();
		}
	}

	protected synchronized void updatedEndpoint(
			ServiceReference<IEndpoint> reference) throws Exception {
		Endpoint endpoint = m_refEps.get(reference);
		if (endpoint != null) {
			updated(endpoint, reference);
			return;
		}

		bindEndpoint(reference);
	}

	protected synchronized void bindProcessor(
			ServiceReference<IProcessor> reference) {
		String id = getId(reference);
		if (id == null)
			return;

		Map<String, Endpoint> endpoints = m_endpoints;
		Endpoint endpoint = endpoints.get(id);
		if (endpoint != null) {
			c_logger.error(StrUtil.join(endpoint,
					" has already been registered"));
			return;
		}

		endpoint = new Processor(id, this, reference);
		m_refEps.put(reference, endpoint);
		endpoints.put(id, endpoint);

		// if MQ has not been activated yet
		if (m_context == null)
			return;

		wakeMsgs(endpoint);
	}

	protected synchronized void unbindProcessor(
			ServiceReference<IProcessor> reference) {
		Endpoint endpoint = m_refEps.remove(reference);
		if (endpoint != null) {
			m_endpoints.remove(endpoint.id());
			endpoint.closeProducer();
		}
	}

	protected synchronized void updatedProcessor(
			ServiceReference<IProcessor> reference) {
		Endpoint endpoint = m_refEps.get(reference);
		if (endpoint != null) {
			updated(endpoint, reference);
			return;
		}

		bindProcessor(reference);
	}

	@Modified
	protected void modified(Map<String, ?> properties) {
		m_msgTimeout = (Integer) properties.get(P_MSG_TIMEOUT);
	}

	protected void activate(ComponentContext context, Map<String, ?> properties) {
		m_context = context;

		final BundleContext bc = context.getBundleContext();
		IServiceHolderManager<IPreHandler> preHandlerManager = ServiceHolderManager
				.newInstance(bc, IPreHandler.class, MeConstants.HANDLER_ID);
		IServiceHolderManager<IPostHandler> postHandlerManager = ServiceHolderManager
				.newInstance(bc, IPostHandler.class, MeConstants.HANDLER_ID);

		preHandlerManager.open();
		postHandlerManager.open();

		m_preHandlerManager = preHandlerManager;
		m_postHandlerManager = postHandlerManager;

		modified(properties);

		final Collection<Endpoint> endpoints = m_endpoints.values();
		for (Endpoint endpoint : endpoints) {
			if (!isLazy(endpoint.reference()))
				endpoint.getConsumer();
		}

		m_nodes = new ConcurrentHashMap<String, BiListNode<MsgNotifier>>();

		c_logger.info("MessageQueue activated");
	}

	@SuppressWarnings("resource")
	protected void deactivate() {
		Collection<BiListNode<MsgNotifier>> nodes = m_nodes.values();
		for (BiListNode<MsgNotifier> head : nodes) {
			BiListNode<MsgNotifier> node = head.next();
			while (node != head) {
				node.get().notifier().close();
				node = node.next();
			}
		}
		m_nodes = null;

		m_postHandlerManager.close();
		m_preHandlerManager.close();

		m_context = null;

		c_logger.info("MessageQueue deactivated");
	}

	<T> T locateService(ServiceReference<T> reference) {
		return m_context.getBundleContext().getService(reference);
	}

	void dispatch(Message message) {
		if (message.isToNull()) {
			message.close();
			return;
		}

		String dst = message.to();
		try {
			Endpoint mqProxy = m_endpoints.get(dst);
			if (mqProxy != null) {
				message.setEndpoint(mqProxy);
				m_workshop.run(message);
			} else {
				BiListNode<MsgNotifier> node = schedule(message);
				mqProxy = m_endpoints.get(dst);
				if (mqProxy != null && node.get().notifier().cancel()) {
					removeNode(node);
					message.setEndpoint(mqProxy);
					m_workshop.run(message);
				}
			}
		} catch (Throwable t) {
			c_logger.error(StrUtil.join("Endpoint[", dst,
					"] failed to consume: ", message), t);
			message.close();
		}
	}

	IRouter getRouter(String id) {
		return m_rm.getRouter(id);
	}

	IPreHandler[] getPreHandlers(String[] preHandlerIds) {
		int n = preHandlerIds.length;
		if (n < 1)
			return EMPTY_PREHANDLERS;

		final IServiceHolderManager<IPreHandler> manager = m_preHandlerManager;
		IPreHandler[] preHandlers = new IPreHandler[n];
		for (int i = 0; i < n; ++i)
			preHandlers[i] = new PreHandlerDelegator(
					manager.getServiceHolder(preHandlerIds[i]));

		return preHandlers;
	}

	void ungetPreHandlers(String[] preHandlerIds) {
		final IServiceHolderManager<IPreHandler> manager = m_preHandlerManager;
		for (String preHandlerId : preHandlerIds)
			manager.ungetServiceHolder(preHandlerId);
	}

	IPostHandler[] getPostHandlers(String[] postHandlerIds) {
		int n = postHandlerIds.length;
		if (n < 1)
			return EMPTY_POSTHANDLERS;

		final IServiceHolderManager<IPostHandler> manager = m_postHandlerManager;
		IPostHandler[] postHandlers = new IPostHandler[n];
		for (int i = 0; i < n; ++i)
			postHandlers[i] = new PostHandlerDelegator(
					manager.getServiceHolder(postHandlerIds[i]));

		return postHandlers;
	}

	void ungetPostHandlers(String[] postHandlerIds) {
		final IServiceHolderManager<IPostHandler> manager = m_postHandlerManager;
		for (String postHandlerId : postHandlerIds)
			manager.ungetServiceHolder(postHandlerId);
	}

	boolean isLazy(ServiceReference<?> reference) {
		Boolean lazy = (Boolean) reference.getProperty(MeConstants.EP_LAZY);
		return (lazy == null || lazy.booleanValue());
	}

	private String getId(ServiceReference<?> reference) {
		String id = (String) reference.getProperty(MeConstants.EP_ID);
		if (id == null) {
			c_logger.error(StrUtil.join(
					reference.getProperty(Constants.SERVICE_PID), ": Missing "
							+ MeConstants.EP_ID));
			return null;
		}

		id = id.trim();
		if (id.length() < 1) {
			c_logger.error(StrUtil.join(
					reference.getProperty(Constants.SERVICE_PID), ": Empty "
							+ MeConstants.EP_ID));
			return null;
		}

		return id.intern();
	}

	private void unregister(Endpoint endpoint, Object ref) {
		endpoint.closeProducer();
		m_endpoints.remove(endpoint.id());
		m_refEps.remove(ref);
	}

	private void updated(Endpoint endpoint, ServiceReference<?> reference) {
		if (m_context != null)
			endpoint.setHandlers(reference);

		String id = getId(reference);
		if (id == null) {
			unregister(endpoint, reference);
			c_logger.error(StrUtil.join(endpoint, " is unregistered: Illegal "
					+ MeConstants.EP_ID));
		} else if (!endpoint.id().equals(id)) {
			if (m_endpoints.containsKey(id)) {
				unregister(endpoint, reference);
				c_logger.error(StrUtil
						.join(endpoint, " is unregistered: Existing "
								+ MeConstants.EP_ID + "=", id));
			} else {
				String oldId = endpoint.id();
				endpoint.id(id);
				m_endpoints.put(id, endpoint);
				m_endpoints.remove(oldId);

				c_logger.info(StrUtil.join(endpoint, " is reregistered from ",
						oldId));
			}
		}
	}

	private BiListNode<MsgNotifier> schedule(Message message) {
		BiListNode<MsgNotifier> node = BiListNode.create();
		ITimeoutNotifier notifier = m_ta.createNotifier(node);
		notifier.setListener(this);
		MsgNotifier mn = new MsgNotifier(message, notifier);
		node.set(mn);

		BiListNode<MsgNotifier> head = getHead(message.to());
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			BiListNode<MsgNotifier> previous = head.previous();
			previous.next(node);
			node.previous(previous);
			node.next(head);
			head.previous(node);
		} finally {
			lock.unlock();
		}

		notifier.schedule(m_msgTimeout);
		return node;
	}

	private BiListNode<MsgNotifier> getHead(String endpointId) {
		BiListNode<MsgNotifier> head = m_nodes.get(endpointId);
		if (head == null) {
			head = BiListNode.<MsgNotifier> create();
			head.previous(head);
			head.next(head);
			BiListNode<MsgNotifier> node = m_nodes
					.putIfAbsent(endpointId, head);
			if (node != null) {
				head.close();
				head = node;
			}
		}

		return head;
	}

	private Message removeNode(BiListNode<MsgNotifier> node) {
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			final BiListNode<MsgNotifier> previous = node.previous();
			final BiListNode<MsgNotifier> next = node.next();
			previous.next(next);
			next.previous(previous);
		} finally {
			lock.unlock();
		}
		Message msg = node.get().msg();
		node.close();
		return msg;
	}

	private void wakeMsgs(Endpoint endpoint) {
		final BiListNode<MsgNotifier> head = m_nodes.get(endpoint.id());
		if (head == null)
			return;

		final IWorkshop workshop = m_workshop;
		final ReentrantLock lock = m_lock;
		lock.lock();
		try {
			BiListNode<MsgNotifier> node = head.next();
			while (node != head) {
				if (!node.get().notifier().cancel())
					continue;

				final BiListNode<MsgNotifier> previous = node.previous();
				final BiListNode<MsgNotifier> next = node.next();
				previous.next(next);
				next.previous(previous);

				Message msg = node.get().msg();
				msg.setEndpoint(endpoint);
				workshop.run(msg);

				node.close();
				node = next;
			}
		} finally {
			lock.unlock();
		}
	}
}
