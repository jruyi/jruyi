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

/**
 * A local cache bound to the current thread.
 * 
 * @param <E>
 *            the type of the object to cache
 * 
 * @see ThreadLocalCache
 */
public interface IThreadLocalCache<E> {

	/**
	 * Gets a cached object from the local cache of the current thread.
	 * 
	 * @return a cached object or {@code null} if no cached objects
	 */
	E take();

	/**
	 * Puts the specified {@code e} into the local cache of the current thread.
	 * 
	 * @param e
	 *            the object to cache
	 */
	void put(E e);
}
