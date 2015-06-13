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

import java.util.Map;

import org.jruyi.common.ICloseable;

/**
 * A message is a data carrier to be routed in the message queue.
 * <p>
 * The message properties are used for routing.
 */
public interface IMessage extends ICloseable {

	/**
	 * Returns the message ID.
	 * 
	 * @return the message ID
	 */
	long id();

	/**
	 * Returns the sender ID of the message.
	 * 
	 * @return the sender ID
	 */
	String from();

	/**
	 * Returns the receiver ID of the message.
	 * 
	 * @return the receiver ID
	 */
	String to();

	/**
	 * Sets the receiver of the message.
	 * 
	 * @param to
	 *            the receiver ID
	 */
	void to(String to);

	/**
	 * Sets the receiver to be null to drop the message.
	 */
	void toNull();

	/**
	 * Tests whether the message is to be dropped
	 * 
	 * @return true if yes, otherwise false.
	 */
	boolean isToNull();

	/**
	 * Returns the value of the property with the specified {@code name}. This
	 * method returns {@code null} if the property is not found.
	 * 
	 * @param name
	 *            the name of the property
	 * @return the property value, or {@code null} if no such property
	 */
	Object getProperty(String name);

	/**
	 * Sets property {@code name} to {@code value}.
	 * 
	 * @param name
	 *            the name of the property
	 * @param value
	 *            the value of the property to be set
	 * @return the previous property value, or {@code null} if there was no such
	 *         property
	 */
	Object putProperty(String name, Object value);

	/**
	 * Puts all the mappings in the specified {@code properties} to this
	 * message's properties.
	 * 
	 * @param properties
	 *            a map containing the properties to be put
	 */
	void putProperties(Map<String, ?> properties);

	/**
	 * Removes the property with the specified {@code name}.
	 * 
	 * @param name
	 *            the name of the property to be removed
	 * @return the value of the removed property, or {@code null} if no such
	 *         property
	 */
	Object removeProperty(String name);

	/**
	 * Gets all the message's properties.
	 * 
	 * @return a map containing all the message's properties
	 */
	Map<String, ?> getProperties();

	/**
	 * Clears all the message properties.
	 */
	void clearProperties();

	/**
	 * Deposits the specified {@code stuff} to this message with the specified
	 * {@code id} reference as the key.
	 * 
	 * @param id
	 *            the reference to which is used as the key
	 * @param stuff
	 *            the object to be deposited
	 * @return the previous object deposited with key {@code id}, or
	 *         {@code null} if there was no such deposition
	 */
	Object deposit(Object id, Object stuff);

	/**
	 * Withdraws the deposited object with the key {@code id}.
	 * 
	 * @param id
	 *            the reference to which is used as the key
	 * @return the deposited object, or {@code null} if no such deposition
	 */
	Object withdraw(Object id);

	/**
	 * Returns the object deposited with the specified {@code id} as the
	 * reference-equality key.
	 * 
	 * @param id
	 *            the reference-equality key
	 * @return the deposited object
	 */
	Object inquiry(Object id);

	/**
	 * Attaches the specified {@code attachment} to this message.
	 * 
	 * @param attachment
	 *            the data this message carries
	 * @return the previous attachment, or {@code null} if there was no
	 *         attachment.
	 */
	Object attach(Object attachment);

	/**
	 * Gets the current attachment.
	 * 
	 * @return the current attachment
	 */
	Object attachment();

	/**
	 * Detaches the current attachment off the message.
	 * 
	 * @return the attachment that is detached
	 */
	Object detach();

	/**
	 * Returns a new message instance with the same properties and storage as
	 * this one.
	 * 
	 * @return the new message as described
	 */
	IMessage duplicate();
}
