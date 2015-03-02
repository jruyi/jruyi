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
 * cache that is weakly reachable.
 *
 * <p>
 * The underlying cache container is backed by an auto-growing array.
 * 
 * @see WeakThreadLocalLinkedCache
 * @see SoftThreadLocalArrayCache
 * @see SoftThreadLocalLinkedCache
 * @see WeakThreadLocal
 * @see SoftThreadLocal
 */
final class WeakThreadLocalArrayCache<E> extends WeakThreadLocal<ArrayStack<E>> implements IThreadLocalCache<E> {

	private final int m_initialCapacity;

	/**
	 * Constructs a {@code WeakThreadLocalCache} with the initial capacity of
	 * the backed array being 6.
	 */
	WeakThreadLocalArrayCache() {
		this(6);
	}

	/**
	 * Constructs a {@code WeakThreadLocalCache} with the initial capacity of
	 * the backed array being the given {@code initialCapacity}.
	 *
	 * @param initialCapacity
	 *            the initial capacity.
	 */
	WeakThreadLocalArrayCache(int initialCapacity) {
		m_initialCapacity = initialCapacity;
	}

	/**
	 * Gets an object of {@code E} from the local cache of the current thread if
	 * the cache is not empty. Otherwise a new object is created and returned.
	 * 
	 * @return an object of {@code E}.
	 */
	@Override
	public E take() {
		ArrayStack<E> cache = deref();
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
	 * Creates a new {@code ArrayStack<E>} as the container of the thread local
	 * cache.
	 * 
	 * @return a new {@code ArrayStack<E>}.
	 */
	@Override
	protected final ArrayStack<E> newValue() {
		return new ArrayStack<>(m_initialCapacity);
	}
}
