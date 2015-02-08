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
 * Defines standard names for Message Engine's constants.
 */
public final class MeConstants {

	/**
	 * Property name of end point ID.
	 */
	public static final String EP_ID = "jruyi.me.endpoint.id";
	/**
	 * Name of the property specifying whether to lazy activate endpoint.
	 */
	public static final String EP_LAZY = "jruyi.me.endpoint.lazy";
	/**
	 * Property name of prehandler chain.
	 */
	public static final String EP_PREHANDLERS = "jruyi.me.endpoint.prehandlers";
	/**
	 * Property name of posthandler chain.
	 */
	public static final String EP_POSTHANDLERS = "jruyi.me.endpoint.posthandlers";

	/**
	 * Property name of handler ID.
	 */
	public static final String HANDLER_ID = "jruyi.me.handler.id";

	/**
	 * The handler ID of handler msglog.
	 */
	public static final String HID_MSGLOG = "jruyi.me.msglog.handler";

	private MeConstants() {
	}
}
