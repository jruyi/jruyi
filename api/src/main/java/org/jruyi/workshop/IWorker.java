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
package org.jruyi.workshop;

import java.util.concurrent.RejectedExecutionException;

import org.jruyi.common.IArgList;

/**
 * Service for running jobs concurrently.
 */
public interface IWorker {

	/**
	 * Assigns the given {@code job} to a worker thread to run at some time in
	 * the future.
	 * 
	 * @param job
	 *            the runnable task
	 * @throws NullPointerException
	 *             if the given {@code job} is null
	 * @throws RejectedExecutionException
	 *             if the worker is shutdown or the current thread is
	 *             interrupted
	 */
	public void run(Runnable job);

	/**
	 * Assigns the given {@code job} to a worker thread to run at some time in
	 * the future. The given {@code argList} will be passed to {@code job.run}.
	 * 
	 * @param job
	 *            the runnable task
	 * @param argList
	 *            the arguments to be passed to {@code job.run}
	 * @throws NullPointerException
	 *             if the given {@code job} is null
	 * @throws RejectedExecutionException
	 *             if the worker is shutdown or the current thread is
	 *             interrupted
	 */
	public void run(IRunnable job, IArgList argList);
}
