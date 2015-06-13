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

package org.jruyi.me;

/**
 * An entity produces/consumes messages. A producer representing this endpoint
 * will be passed in on its being registered to JRuyi message queue. Likewise, a
 * consumer must be provided to JRuyi message queue on registering if this
 * endpoint wants to consume messages.
 */
public interface IEndpoint {

	/**
	 * Injects an {@code IProducer} representing this endpoint to produce
	 * messages when this endpoint is being registered to the message queue.
	 * 
	 * @param producer
	 *            a {@code IProducer} representing this endpoint.
	 */
	void producer(IProducer producer);

	/**
	 * Outjects an {@code IConsumer} representing this endpoint to consume
	 * messages when this endpoint is being registered to the message queue.
	 * 
	 * @return an {@code IConsumer} representing this endpoint.
	 */
	IConsumer consumer();
}
