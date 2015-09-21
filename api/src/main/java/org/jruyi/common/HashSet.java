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
 * An {@code Object} hash set.
 * <p>
 * This class uses a simple thread local cache mechanism for {@code HashSet
 * <Object>}
 */
public final class HashSet extends java.util.HashSet<Object>implements ICloseable {

	private static final long serialVersionUID = -8986434770383785065L;
	private static final IThreadLocalCache<HashSet> c_cache = ThreadLocalCache.weakArrayCache();

	/**
	 * Gets an {@code HashSet} object from the local cache of the current thread
	 * if the cache is not empty. Otherwise a new object is created and
	 * returned.
	 * 
	 * @return an {@code HashSet} object.
	 */
	public static HashSet get() {
		HashSet set = c_cache.take();
		if (set == null)
			set = new HashSet();
		return set;
	}

	/**
	 * Clears this {@code HashSet} and puts it to the local cache of the current
	 * thread.
	 */
	@Override
	public void close() {
		clear();
		c_cache.put(this);
	}
}
