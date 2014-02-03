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
package org.jruyi.io;

/**
 * A session represents a context of a connection.
 */
public interface ISession {

	/**
	 * Returns the session ID.
	 * 
	 * @return the session ID
	 */
	public Long id();

	/**
	 * Returns the address of the local peer.
	 * 
	 * @return the local address
	 */
	public Object localAddress();

	/**
	 * Returns the address of the remote peer.
	 * 
	 * @return the remote address
	 */
	public Object remoteAddress();

	/**
	 * Puts an attribute, which maps the specified {@code name} to the specified
	 * {@code value}, into this session.
	 * 
	 * @param name
	 *            the name to be associated with the specified {@code value}
	 * @param value
	 *            the value to be associated with the specified {@code name}
	 * @return the previous value associated with the specified {@code name}, or
	 *         {@code null} if there was no mapping for {@code name}
	 */
	public Object put(String name, Object value);

	/**
	 * Gets the value of the session attribute whose name is the specified
	 * {@code name}.
	 * 
	 * @param name
	 *            the name of the attribute
	 * @return the value of the attribute, or {@code null} if no such attribute
	 */
	public Object get(String name);

	/**
	 * Removes the attribute whose name is the specified {@code name} from this
	 * session.
	 * 
	 * @param name
	 *            the name of the attribute
	 * @return the value of the attribute, or {@code null} if no such attribute
	 */
	public Object remove(String name);

	/**
	 * Deposits the specified {@code something} to this session with the
	 * specified {@code id} as the reference-equality key.
	 * 
	 * @param id
	 *            the reference-equality key
	 * @param something
	 *            the object to deposit
	 * @return the previous deposited object, or {@code null} if none
	 */
	public Object deposit(Object id, Object something);

	/**
	 * Withdraws the object deposited with the specified {@code id} as the
	 * reference-equality key.
	 * 
	 * @param id
	 *            the reference-equality key
	 * @return the deposited object with the specified {@code id} as the
	 *         reference-equality key, or {@code null} if no such deposit
	 */
	public Object withdraw(Object id);

	/**
	 * Returns the object deposited with the specified {@code id} as the
	 * reference-equality key.
	 * 
	 * @param id
	 *            the reference-equality key
	 * @return the deposited object
	 */
	public Object inquiry(Object id);

	/**
	 * Creates an empty buffer using the buffer factory associated with this
	 * session.
	 * 
	 * @return an empty buffer
	 */
	public IBuffer createBuffer();

	/**
	 * Attaches the specified {@code attachment} to this session.
	 * 
	 * @param attachment
	 *            the attachment
	 * @return the previous attachment, or {@code null} if there was no
	 *         attachment
	 */
	public Object attach(Object attachment);

	/**
	 * Gets the current attachment.
	 * 
	 * @return the current attachment
	 */
	public Object attachment();

	/**
	 * Detaches the current attachment off the session.
	 * 
	 * @return the attachment that is detached
	 */
	public Object detach();
}
