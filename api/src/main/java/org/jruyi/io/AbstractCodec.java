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
 * A default {@link ICodec} implementation with all methods throwing
 * {@code UnsupportedOperationException}.
 * 
 * @param <T>
 *            the type of the object to be encoded/decoded
 */
public abstract class AbstractCodec<T> implements ICodec<T> {

	/**
	 * Empty implementation. Throws UnsupportedOperationException.
	 */
	@Override
	public T read(IUnitChain unitChain) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Empty implementation. Throws UnsupportedOperationException.
	 */
	@Override
	public T read(IUnitChain unitChain, int length) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Empty implementation. Throws UnsupportedOperationException.
	 */
	@Override
	public int read(T dst, IUnitChain unitChain) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Empty implementation. Throws UnsupportedOperationException.
	 */
	@Override
	public int read(T dst, int offset, int length, IUnitChain unitChain) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Empty implementation. Throws UnsupportedOperationException.
	 */
	@Override
	public void write(T src, IUnitChain unitChain) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Empty implementation. Throws UnsupportedOperationException.
	 */
	@Override
	public void write(T src, int offset, int length, IUnitChain unitChain) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Empty implementation. Throws UnsupportedOperationException.
	 */
	@Override
	public T get(IUnitChain unitChain, int index) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Empty implementation. Throws UnsupportedOperationException.
	 */
	@Override
	public T get(IUnitChain unitChain, int index, int length) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Empty implementation. Throws UnsupportedOperationException.
	 */
	@Override
	public void get(T dst, IUnitChain unitChain, int index) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Empty implementation. Throws UnsupportedOperationException.
	 */
	@Override
	public void get(T dst, int offset, int length, IUnitChain unitChain, int index) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Empty implementation. Throws UnsupportedOperationException.
	 */
	@Override
	public void set(T src, IUnitChain unitChain, int index) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Empty implementation. Throws UnsupportedOperationException.
	 */
	@Override
	public void set(T src, int offset, int length, IUnitChain unitChain, int index) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Empty implementation. Throws UnsupportedOperationException.
	 */
	@Override
	public void prepend(T src, IUnitChain unitChain) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Empty implementation. Throws UnsupportedOperationException.
	 */
	@Override
	public void prepend(T src, int offset, int length, IUnitChain unitChain) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
