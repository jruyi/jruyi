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

import org.jruyi.common.internal.ArgListProvider;

/**
 * This is the factory class for {@code IArgList}.
 * 
 * @see IArgList
 */
public final class ArgList {

	private static final IFactory c_factory = ArgListProvider.getInstance()
			.getFactory();

	/**
	 * A factory class to create instances of {@code IArgList}. It is used to
	 * separate the implementation provider from the API module.
	 */
	public interface IFactory {

		/**
		 * Creates an argument list with the specified {@code arg} as the only
		 * one element.
		 * 
		 * @param arg
		 *            the element to be put into the argument list
		 * @return an argument list
		 */
		public IArgList create(Object arg);

		/**
		 * Creates an argument list with the given 2 arguments as the elements in
		 * order.
		 * 
		 * @param arg0
		 *            the first element to be put into the argument list
		 * @param arg1
		 *            the second element to be put into the argument list
		 * @return an argument list
		 */
		public IArgList create(Object arg0, Object arg1);

		/**
		 * Creates an argument list with the given 3 arguments as the elements in
		 * order.
		 * 
		 * @param arg0
		 *            the first element to be put into the argument list
		 * @param arg1
		 *            the second element to be put into the argument list
		 * @param arg2
		 *            the third element to be put into the argument list
		 * @return an argument list
		 */
		public IArgList create(Object arg0, Object arg1, Object arg2);

		/**
		 * Creates an argument list with the given 4 arguments as the elements in
		 * order.
		 * 
		 * @param arg0
		 *            the first element to be put into the argument list
		 * @param arg1
		 *            the second element to be put into the argument list
		 * @param arg2
		 *            the third element to be put into the argument list
		 * @param arg3
		 *            the fourth element to be put into the argument list
		 * @return an argument list
		 */
		public IArgList create(Object arg0, Object arg1, Object arg2,
				Object arg3);

		/**
		 * Creates an argument list with the given 5 arguments as the elements in
		 * order.
		 * 
		 * @param arg0
		 *            the first element to be put into the argument list
		 * @param arg1
		 *            the second element to be put into the argument list
		 * @param arg2
		 *            the third element to be put into the argument list
		 * @param arg3
		 *            the fourth element to be put into the argument list
		 * @param arg4
		 *            the fifth element to be put into the argument list
		 * @return an argument list
		 */
		public IArgList create(Object arg0, Object arg1, Object arg2,
				Object arg3, Object arg4);

		/**
		 * Creates an argument list with the given 6 arguments as the elements in
		 * order.
		 * 
		 * @param arg0
		 *            the first element to be put into the argument list
		 * @param arg1
		 *            the second element to be put into the argument list
		 * @param arg2
		 *            the third element to be put into the argument list
		 * @param arg3
		 *            the fourth element to be put into the argument list
		 * @param arg4
		 *            the fifth element to be put into the argument list
		 * @param arg5
		 *            the sixth element to be put into the argument list
		 * @return an argument list
		 */
		public IArgList create(Object arg0, Object arg1, Object arg2,
				Object arg3, Object arg4, Object arg5);

		/**
		 * Creates an argument list with the given 7 arguments as the elements in
		 * order.
		 * 
		 * @param arg0
		 *            the first element to be put into the argument list
		 * @param arg1
		 *            the second element to be put into the argument list
		 * @param arg2
		 *            the third element to be put into the argument list
		 * @param arg3
		 *            the fourth element to be put into the argument list
		 * @param arg4
		 *            the fifth element to be put into the argument list
		 * @param arg5
		 *            the sixth element to be put into the argument list
		 * @param arg6
		 *            the seventh element to be put into the argument list
		 * @return an argument list
		 */
		public IArgList create(Object arg0, Object arg1, Object arg2,
				Object arg3, Object arg4, Object arg5, Object arg6);

		/**
		 * Creates an argument list with the given 8 arguments as the elements in
		 * order.
		 * 
		 * @param arg0
		 *            the first element to be put into the argument list
		 * @param arg1
		 *            the second element to be put into the argument list
		 * @param arg2
		 *            the third element to be put into the argument list
		 * @param arg3
		 *            the fourth element to be put into the argument list
		 * @param arg4
		 *            the fifth element to be put into the argument list
		 * @param arg5
		 *            the sixth element to be put into the argument list
		 * @param arg6
		 *            the seventh element to be put into the argument list
		 * @param arg7
		 *            the eighth element to be put into the argument list
		 * @return an argument list
		 */
		public IArgList create(Object arg0, Object arg1, Object arg2,
				Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);

		/**
		 * Creates an argument list with the given 1+ arguments as the elements
		 * in order.
		 * 
		 * @param arg0
		 *            the first element to be put into the argument list
		 * @param args
		 *            the following elements to be put into the argument list
		 * @return an argument list
		 */
		public IArgList create(Object arg0, Object... args);
	}

	private ArgList() {
	}

	/**
	 * Creates an argument list with the specified {@code arg} as the only one
	 * element.
	 * 
	 * @param arg
	 *            the element to be put into the argument list
	 * @return an argument list
	 */
	public static IArgList create(Object arg) {
		return c_factory.create(arg);
	}

	/**
	 * Creates an argument list with the given 2 arguments as the elements in
	 * order.
	 * 
	 * @param arg0
	 *            the first element to be put into the argument list
	 * @param arg1
	 *            the second element to be put into the argument list
	 * @return an argument list
	 */
	public static IArgList create(Object arg0, Object arg1) {
		return c_factory.create(arg0, arg1);
	}

	/**
	 * Creates an argument list with the given 3 arguments as the elements in
	 * order.
	 * 
	 * @param arg0
	 *            the first element to be put into the argument list
	 * @param arg1
	 *            the second element to be put into the argument list
	 * @param arg2
	 *            the third element to be put into the argument list
	 * @return an argument list
	 */
	public static IArgList create(Object arg0, Object arg1, Object arg2) {
		return c_factory.create(arg0, arg1, arg2);
	}

	/**
	 * Creates an argument list with the given 4 arguments as the elements in
	 * order.
	 * 
	 * @param arg0
	 *            the first element to be put into the argument list
	 * @param arg1
	 *            the second element to be put into the argument list
	 * @param arg2
	 *            the third element to be put into the argument list
	 * @param arg3
	 *            the fourth element to be put into the argument list
	 * @return an argument list
	 */
	public static IArgList create(Object arg0, Object arg1, Object arg2,
			Object arg3) {
		return c_factory.create(arg0, arg1, arg2, arg3);
	}

	/**
	 * Creates an argument list with the given 5 arguments as the elements in
	 * order.
	 * 
	 * @param arg0
	 *            the first element to be put into the argument list
	 * @param arg1
	 *            the second element to be put into the argument list
	 * @param arg2
	 *            the third element to be put into the argument list
	 * @param arg3
	 *            the fourth element to be put into the argument list
	 * @param arg4
	 *            the fifth element to be put into the argument list
	 * @return an argument list
	 */
	public static IArgList create(Object arg0, Object arg1, Object arg2,
			Object arg3, Object arg4) {
		return c_factory.create(arg0, arg1, arg2, arg3, arg4);
	}

	/**
	 * Creates an argument list with the given 6 arguments as the elements in
	 * order.
	 * 
	 * @param arg0
	 *            the first element to be put into the argument list
	 * @param arg1
	 *            the second element to be put into the argument list
	 * @param arg2
	 *            the third element to be put into the argument list
	 * @param arg3
	 *            the fourth element to be put into the argument list
	 * @param arg4
	 *            the fifth element to be put into the argument list
	 * @param arg5
	 *            the sixth element to be put into the argument list
	 * @return an argument list
	 */
	public static IArgList create(Object arg0, Object arg1, Object arg2,
			Object arg3, Object arg4, Object arg5) {
		return c_factory.create(arg0, arg1, arg2, arg3, arg4, arg5);
	}

	/**
	 * Creates an argument list with the given 7 arguments as the elements in
	 * order.
	 * 
	 * @param arg0
	 *            the first element to be put into the argument list
	 * @param arg1
	 *            the second element to be put into the argument list
	 * @param arg2
	 *            the third element to be put into the argument list
	 * @param arg3
	 *            the fourth element to be put into the argument list
	 * @param arg4
	 *            the fifth element to be put into the argument list
	 * @param arg5
	 *            the sixth element to be put into the argument list
	 * @param arg6
	 *            the seventh element to be put into the argument list
	 * @return an argument list
	 */
	public static IArgList create(Object arg0, Object arg1, Object arg2,
			Object arg3, Object arg4, Object arg5, Object arg6) {
		return c_factory.create(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
	}

	/**
	 * Creates an argument list with the given 8 arguments as the elements in
	 * order.
	 * 
	 * @param arg0
	 *            the first element to be put into the argument list
	 * @param arg1
	 *            the second element to be put into the argument list
	 * @param arg2
	 *            the third element to be put into the argument list
	 * @param arg3
	 *            the fourth element to be put into the argument list
	 * @param arg4
	 *            the fifth element to be put into the argument list
	 * @param arg5
	 *            the sixth element to be put into the argument list
	 * @param arg6
	 *            the seventh element to be put into the argument list
	 * @param arg7
	 *            the eighth element to be put into the argument list
	 * @return an argument list
	 */
	public static IArgList create(Object arg0, Object arg1, Object arg2,
			Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
		return c_factory.create(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
	}

	/**
	 * Creates an argument list with the given 1+ arguments as the elements in
	 * order.
	 * 
	 * @param arg0
	 *            the first element to be put into the argument list
	 * @param args
	 *            the following elements to be put into the argument list
	 * @return an argument list
	 */
	public static IArgList create(Object arg0, Object... args) {
		return c_factory.create(arg0, args);
	}
}
