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
	Long id();

	/**
	 * Returns the address of the local peer.
	 * 
	 * @return the local address
	 */
	Object localAddress();

	/**
	 * Returns the address of the remote peer.
	 * 
	 * @return the remote address
	 */
	Object remoteAddress();

	/**
	 * Tests if an attribute with the specified {@code key} exists.
	 *
	 * @param key
	 *            the key to be tested
	 * @return {@code true} if and only if an attribute with the specified
	 *         {@code key} exists, as determined by the {@code equals} method;
	 *         {@code false} otherwise.
	 * @throws NullPointerException
	 *             if the specified {@code key} is {@code null}
	 * @since 2.0
	 */
	boolean contains(Object key);

	/**
	 * Puts an attribute, which maps the specified {@code key} to the specified
	 * {@code value}, into this session.
	 * 
	 * @param key
	 *            the key to be associated with the specified {@code value}
	 * @param value
	 *            the value to be associated with the specified {@code key}
	 * @return the previous value associated with the specified {@code key}, or
	 *         {@code null} if there was no mapping for {@code key}
	 * @throws NullPointerException
	 *             if the specified {@code key} or {@code value} is {@code null}
	 */
	Object put(Object key, Object value);

	/**
	 * Puts an attribute, which maps the specified {@code key} to the specified
	 * {@code value}, into this session if the attribute with the specified
	 * {@code key} does not exist yet. The logic is equivalent to
	 * 
	 * <pre>
	 * {@code
	 * if (!contains(key))
	 *     return put(key, value);
	 * else
	 *     return get(key);
	 * }
	 * </pre>
	 * 
	 * except that the operation is performed atomically.
	 * 
	 * @param key
	 *            the key to be associated with the specified {@code value}
	 * @param value
	 *            the value to be associated with the specified {@code key}
	 * @return the previous value associated with the specified {@code key}, or
	 *         {@code null} if there was no mapping for {@code key}
	 * @throws NullPointerException
	 *             if the specified {@code key} or {@code value} is {@code null}
	 * @since 2.0
	 */
	Object putIfAbsent(Object key, Object value);

	/**
	 * Gets the value of the session attribute with the specified {@code key}.
	 * 
	 * @param key
	 *            the key of the attribute
	 * @return the value of the attribute, or {@code null} if no such attribute
	 * @throws NullPointerException
	 *             if the specified {@code key} is {@code null}
	 */
	Object get(Object key);

	/**
	 * Removes the attribute with the specified {@code key} from this session.
	 * 
	 * @param key
	 *            the key of the attribute
	 * @return the value of the attribute, or {@code null} if no such attribute
	 * @throws NullPointerException
	 *             if the specified {@code key} is {@code null}
	 */
	Object remove(Object key);

	/**
	 * Removes the attribute with the specified {@code key} from this session if
	 * and only if currently mapped to the given {@code value}. The logic is
	 * equivalent to
	 * 
	 * <pre>
	 * {@code
	 * if (contains(key) && value.equals(get(key))) {
	 *     remove(key);
	 *     return true;
	 * } else
	 *     return false;
	 * }
	 * </pre>
	 * 
	 * except that the operation is performed atomically.
	 * 
	 * @param key
	 *            the key of the attribute to be removed
	 * @param value
	 *            the expected value of the attribute to be removed
	 * @return {@code true} if the attribute is removed
	 * @throws NullPointerException
	 *             if the specified {@code key} or {@code value} is {@code null}
	 * @since 2.0
	 */
	boolean remove(Object key, Object value);

	/**
	 * Replaces the value of the attribute only if currently exists. The logic
	 * is equivalent to
	 *
	 * <pre>
	 * {@code
	 * if (contains(key))
	 *     return put(key, value);
	 * else
	 *     return null;
	 * }
	 * </pre>
	 *
	 * except that the operation is performed atomically.
	 * 
	 * @param key
	 *            the key of the attribute to be associated.
	 * @param value
	 *            the value to be associated with the specified {@code key}
	 * @return the previous value associated with the specified {@code key}, or
	 *         {@code null} if there was no mapping for {@code key}
	 * @throws NullPointerException
	 *             if the specified {@code key} or {@code value} is {@code null}
	 * @since 2.0
	 */
	Object replace(Object key, Object value);

	/**
	 * Replaces the value of the attribute with the given {@code newValue} only
	 * if exists and mapped to the given {@code oldValue}. The logic is
	 * equivalent to
	 * 
	 * <pre>
	 * {@code
	 * if (contains(key) && oldValue.equals(get(key))) {
	 *     put(key, newValue);
	 *     return true;
	 * } else
	 *     return false;
	 * }
	 * </pre>
	 * 
	 * except that the operation is performed atomically.
	 * 
	 * @param key
	 *            the key of the attribute whose value is to be replaced
	 * @param oldValue
	 *            the expected value currently associated with the specified
	 *            {@code key}
	 * @param newValue
	 *            the value to be associated with the specified {@code key}
	 * @return {@code true} if the value is replaced
	 * @throws NullPointerException
	 *             if the specified {@code key}, {@code oldValue} or
	 *             {@code newValue} is {@code null}
	 * @since 2.0
	 */
	boolean replace(Object key, Object oldValue, Object newValue);

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
	Object deposit(Object id, Object something);

	/**
	 * Withdraws the object deposited with the specified {@code id} as the
	 * reference-equality key.
	 * 
	 * @param id
	 *            the reference-equality key
	 * @return the deposited object with the specified {@code id} as the
	 *         reference-equality key, or {@code null} if no such deposit
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
	 * Creates an empty buffer using the buffer factory associated with this
	 * session.
	 * 
	 * @return an empty buffer
	 */
	IBuffer createBuffer();

	/**
	 * Attaches the specified {@code attachment} to this session.
	 * 
	 * @param attachment
	 *            the attachment
	 * @return the previous attachment, or {@code null} if there was no
	 *         attachment
	 */
	Object attach(Object attachment);

	/**
	 * Gets the current attachment.
	 * 
	 * @return the current attachment
	 */
	Object attachment();

	/**
	 * Detaches the current attachment off the session.
	 * 
	 * @return the attachment that is detached
	 */
	Object detach();
}
