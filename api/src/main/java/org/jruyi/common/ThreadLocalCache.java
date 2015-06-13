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

package org.jruyi.common;

import org.jruyi.common.internal.ThreadLocalCacheProvider;

/**
 * This is the factory class for {@code IThreadLocalCache}.
 * 
 * @see IThreadLocalCache
 */
public final class ThreadLocalCache {

	private static final IFactory c_factory = ThreadLocalCacheProvider.getInstance().getFactory();

	/**
	 * A factory class to create instances of {@code IThreadLocalCache}. It is
	 * used to separate the implementation provider from the API module.
	 */
	public interface IFactory {

		/**
		 * Creates a thread local cache which is softly referenced and backed by
		 * an array with 6 as the initial capacity.
		 * 
		 * @param <E>
		 *            the type of the object to be cached
		 * @return a thread local cache
		 */
		<E> IThreadLocalCache<E> softArrayCache();

		/**
		 * Creates a thread local cache which is softly referenced and backed by
		 * an array with the specified initial capacity.
		 * 
		 * @param <E>
		 *            the type of the object to be cached
		 * @param initialCapacity
		 *            the initial capacity of the backing array
		 * @return a thread local cache
		 */
		<E> IThreadLocalCache<E> softArrayCache(int initialCapacity);

		/**
		 * Creates a thread local cache which is softly reference and backed by
		 * a linked list.
		 * 
		 * @param <E>
		 *            the type of the object to be cached
		 * @return a thread local cache
		 */
		<E> IThreadLocalCache<E> softLinkedCache();

		/**
		 * Creates a thread local cache which is weakly referenced and backed by
		 * an array with 6 as the initial capacity.
		 * 
		 * @param <E>
		 *            the type of the object to be cached
		 * @return a thread local cache
		 */
		<E> IThreadLocalCache<E> weakArrayCache();

		/**
		 * Creates a thread local cache which is weakly referenced and backed by
		 * an array with the specified initial capacity.
		 * 
		 * @param <E>
		 *            the type of the object to be cached
		 * @param initialCapacity
		 *            the initial capacity of the backing array
		 * @return a thread local cache
		 */
		<E> IThreadLocalCache<E> weakArrayCache(int initialCapacity);

		/**
		 * Creates a thread local cache which is weakly referenced and backed by
		 * a linked list.
		 * 
		 * @param <E>
		 *            the type of the object to be cached
		 * @return a thread local cache
		 */
		<E> IThreadLocalCache<E> weakLinkedCache();
	}

	private ThreadLocalCache() {
	}

	/**
	 * Creates a thread local cache which is softly referenced and backed by an
	 * array with 6 as the initial capacity.
	 * 
	 * @param <E>
	 *            the type of the object to be cached
	 * @return a thread local cache
	 */
	public static <E> IThreadLocalCache<E> softArrayCache() {
		return c_factory.softArrayCache();
	}

	/**
	 * Creates a thread local cache which is softly referenced and backed by an
	 * array with the specified initial capacity.
	 * 
	 * @param <E>
	 *            the type of the object to be cached
	 * @param initialCapacity
	 *            the initial capacity of the backing array
	 * @return a thread local cache
	 */
	public static <E> IThreadLocalCache<E> softArrayCache(int initialCapacity) {
		return c_factory.softArrayCache(initialCapacity);
	}

	/**
	 * Creates a thread local cache which is softly reference and backed by a
	 * linked list.
	 * 
	 * @param <E>
	 *            the type of the object to be cached
	 * @return a thread local cache
	 */
	public static <E> IThreadLocalCache<E> softLinkedCache() {
		return c_factory.softLinkedCache();
	}

	/**
	 * Creates a thread local cache which is weakly referenced and backed by an
	 * array with 6 as the initial capacity.
	 * 
	 * @param <E>
	 *            the type of the object to be cached
	 * @return a thread local cache
	 */
	public static <E> IThreadLocalCache<E> weakArrayCache() {
		return c_factory.weakArrayCache();
	}

	/**
	 * Creates a thread local cache which is weakly referenced and backed by an
	 * array with the specified initial capacity.
	 * 
	 * @param <E>
	 *            the type of the object to be cached
	 * @param initialCapacity
	 *            the initial capacity of the backing array
	 * @return a thread local cache
	 */
	public static <E> IThreadLocalCache<E> weakArrayCache(int initialCapacity) {
		return c_factory.weakArrayCache(initialCapacity);
	}

	/**
	 * Creates a thread local cache which is weakly referenced and backed by a
	 * linked list.
	 * 
	 * @param <E>
	 *            the type of the object to be cached
	 * @return a thread local cache
	 */
	public static <E> IThreadLocalCache<E> weakLinkedCache() {
		return c_factory.weakLinkedCache();
	}
}
