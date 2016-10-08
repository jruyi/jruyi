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

package org.jruyi.io.common;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public final class IoEventQueue<E> {

    private final ReentrantLock m_lock;
    private WeakReference<List<E>>  m_cachedQueue;
    private List<E> m_queue;

    public IoEventQueue() {
        m_lock = new ReentrantLock();
        m_cachedQueue = new WeakReference<>(null);
        m_queue = new ArrayList<>();
    }

    public void put(E e) {
        final ReentrantLock lock = m_lock;
        lock.lock();
        try {
            m_queue.add(e);
        } finally {
            lock.unlock();
        }
    }

    public List<E> elements() {
        final List<E> queue = m_queue;
        if (queue.isEmpty())
            return null;

        List<E> cachedQueue = m_cachedQueue.get();
        if (cachedQueue == null)
            cachedQueue = new ArrayList<>(queue.size() << 1);
        m_queue = cachedQueue;
        return queue;
    }

    public void cache(List<E> queue) {
        queue.clear();
        m_cachedQueue = new WeakReference<>(queue);
    }
}
