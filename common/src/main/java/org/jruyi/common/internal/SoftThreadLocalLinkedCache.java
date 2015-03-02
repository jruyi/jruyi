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

package org.jruyi.common.internal;

import org.jruyi.common.IThreadLocalCache;

/**
 * This class provides skeletal implementations of providing a thread local
 * cache that is softly reachable.
 *
 * <p>
 * The underlying cache container is backed by a linked list.
 *
 * @see SoftThreadLocalArrayCache
 * @see WeakThreadLocalLinkedCache
 * @see WeakThreadLocalArrayCache
 * @see SoftThreadLocal
 * @see WeakThreadLocal
 */
final class SoftThreadLocalLinkedCache<E> extends SoftThreadLocal<LinkedStack<E>> implements IThreadLocalCache<E> {

	/**
	 * Gets an object of {@code E} from the local cache of the current thread if
	 * the cache is not empty. Otherwise a new object is created and returned.
	 *
	 * @return an object of {@code E}.
	 */
	@Override
	public E take() {
		LinkedStack<E> cache = deref();
		return cache.isEmpty() ? null : cache.popInternal();
	}

	/**
	 * Puts the given {@code e} to the local cache of the current thread. Be
	 * aware of clearing {@code e} before calling this method.
	 *
	 * @param e
	 *            the element to be put to the local cache of the current
	 *            thread.
	 */
	@Override
	public void put(E e) {
		deref().push(e);
	}

	/**
	 * Creates a new {@code LinkedStack<E>} as the container of the thread local
	 * cache.
	 *
	 * @return a new {@code LinkedStack<E>}.
	 */
	@Override
	protected final LinkedStack<E> newValue() {
		return new LinkedStack<>();
	}
}
