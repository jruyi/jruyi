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

import java.lang.ref.SoftReference;

/**
 * This class provides skeletal implementations of providing thread local
 * variables that are softly reachable.
 * 
 * @see WeakThreadLocal
 * @see SoftThreadLocalArrayCache
 * @see SoftThreadLocalLinkedCache
 * @see WeakThreadLocalArrayCache
 * @see WeakThreadLocalLinkedCache
 */
public abstract class SoftThreadLocal<T> extends ThreadLocal<SoftReference<T>> {

	/**
	 * Creates a {@code SoftReference} instance referring to a newly created
	 * value for this thread local variable. This method will be invoked the
	 * first time a thread accesses this variable.
	 * 
	 * @return a {@code SoftReference} referring to a newly created value for
	 *         this thread local variable.
	 */
	@Override
	protected final SoftReference<T> initialValue() {
		return new SoftReference<T>(null);
	}

	/**
	 * Returns the value in the current thread's copy of this thread local
	 * variable. If the variable currently has no value for the current thread,
	 * it is first assigned to the value returned by an invocation of the
	 * {@link #newValue} method.
	 * 
	 * @return the current thread's value of this thread local variable.
	 */
	public T deref() {
		T t = get().get();
		if (t == null) {
			t = newValue();
			set(new SoftReference<T>(t));
		}
		return t;
	}

	/**
	 * Creates a new value for this thread local variable. This method will be
	 * invoked the first time a thread accesses this variable, or the previous
	 * one is GC'ed, via the {@link #deref} method.
	 * 
	 * @return a new value for this thread local variable.
	 */
	protected abstract T newValue();
}
