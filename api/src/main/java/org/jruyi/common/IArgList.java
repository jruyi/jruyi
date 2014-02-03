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
package org.jruyi.common;

/**
 * A list holding arguments.
 * 
 * @see ArgList
 */
public interface IArgList extends IDumpable, ICloseable {

	/**
	 * Returns the argument at the specified {@code index}.
	 * 
	 * @param index
	 *            the index of the argument to return
	 * @return the argument at {@code index}
	 */
	public Object arg(int index);

	/**
	 * Returns the number of arguments in this list.
	 * 
	 * @return the number of arguments in this list
	 */
	public int size();
}
