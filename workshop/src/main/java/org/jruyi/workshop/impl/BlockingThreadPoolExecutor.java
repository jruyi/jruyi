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
package org.jruyi.workshop.impl;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

final class BlockingThreadPoolExecutor extends ThreadPoolExecutor {

	static final class PooledThread extends Thread {

		PooledThread(Runnable r) {
			super(r);
		}
	}

	static final class PooledThreadFactory implements ThreadFactory {

		@Override
		public Thread newThread(Runnable r) {
			return new PooledThread(r);
		}
	}

	static final class NoOfferSynchronousQueue extends SynchronousQueue<Runnable> {

		private static final long serialVersionUID = -5865387484672577740L;

		@Override
		public boolean offer(Runnable r) {
			return false;
		}
	}

	static final class NoOfferArrayBlockingQueue extends ArrayBlockingQueue<Runnable> {

		private static final long serialVersionUID = -1198203241217503456L;

		NoOfferArrayBlockingQueue(int capacity) {
			super(capacity);
		}

		@Override
		public boolean offer(Runnable r) {
			return false;
		}
	}

	static final class NoOfferLinkedBlockingQueue extends LinkedBlockingQueue<Runnable> {

		private static final long serialVersionUID = 1437201513532500949L;

		@Override
		public boolean offer(Runnable e) {
			return false;
		}
	}

	static final class BlockPolicy implements RejectedExecutionHandler {

		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			if (executor.isShutdown())
				throw new RejectedExecutionException("Workshop has been shutdown");

			// If the current thread is PooledThread, then use the CallerRunsPolicy.
			// Otherwise deadlock might be introduced if the PooledThread blocks here.
			if (Thread.currentThread() instanceof PooledThread)
				r.run();
			else { // put to queue, may block
				try {
					executor.getQueue().put(r);
				} catch (InterruptedException e) {
					throw new RejectedExecutionException(e);
				}
			}
		}
	}

	BlockingThreadPoolExecutor(int corePoolSize, int maxPoolSize,
			long keepAliveTime, int queueCapacity) {
		super(corePoolSize, maxPoolSize,
				keepAliveTime, TimeUnit.SECONDS,
				queueCapacity < 0 ? new NoOfferLinkedBlockingQueue()
				: (queueCapacity > 0
				? new NoOfferArrayBlockingQueue(queueCapacity)
				: new NoOfferSynchronousQueue()),
				new PooledThreadFactory(),
				new BlockPolicy());
	}
}
