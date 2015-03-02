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

import java.util.Iterator;
import java.util.Map;

import org.osgi.framework.BundleContext;

/**
 * A string utility class.
 */
public final class StrUtil {

	private static final char[] c_digits = {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
			'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
			'u', 'v', 'w', 'x', 'y', 'z' };
	private static final char[] c_digitTens = {
			'0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
			'1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
			'2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
			'3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
			'4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
			'5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
			'6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
			'7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
			'8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
			'9', '9', '9', '9', '9', '9', '9', '9', '9', '9' };
	private static final char[] c_digitOnes = {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
	private static final int[] c_intSizeTable = {
			9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE };
	private static final long[] c_longSizeTable = new long[19];

	static {
		long l = 1L;
		int n = c_longSizeTable.length - 1;
		for (int i = 0; i < n; ++i) {
			l *= 10L;
			c_longSizeTable[i] = l - 1;
		}
		c_longSizeTable[n] = Long.MAX_VALUE;
	}

	static final class LineSeparatorHolder {

		static final String LINE_SEPARATOR = System.getProperty("line.separator");
	}

	static final class EmptyStringArrayHolder {

		static final String[] EMPTY = new String[0];
	}

	private StrUtil() {
	}

	/**
	 * Returns a constant empty {@code String} array.
	 * 
	 * @return an empty {@code String} array.
	 */
	public static String[] getEmptyStringArray() {
		return EmptyStringArrayHolder.EMPTY;
	}

	/**
	 * Returns the system property <i>line.separator</i>.
	 * 
	 * @return the line separator of the local system.
	 */
	public static String getLineSeparator() {
		return LineSeparatorHolder.LINE_SEPARATOR;
	}

	/**
	 * Returns the number of the characters that the string value of the given
	 * int {@code i} has.
	 * 
	 * @param i
	 *            the {@code int}.
	 * @return the length of the string value of the given {@code i}.
	 */
	public static int stringSizeOfInt(int i) {
		int n = 0;
		while (i > c_intSizeTable[n])
			++n;
		return ++n;
	}

	/**
	 * Returns the number of the characters that the string value of the given
	 * long {@code l} has.
	 * 
	 * @param l
	 *            the {@code long}
	 * @return the length of the string value of the given {@code l}
	 */
	public static int stringSizeOfLong(long l) {
		int n = 0;
		while (l > c_longSizeTable[n])
			++n;
		return ++n;
	}

	/**
	 * Returns a copy of the given string {@code target}, with the system
	 * properties substituted using syntax <i>${&lt;property name&gt;}</i>. The
	 * property substitution can be nested.
	 * 
	 * <p>
	 * This string itself will be returned, if it has no properties to be
	 * substituted.
	 * 
	 * @param target
	 *            the string to be filtered
	 * @return a copy of the given string that is filtered, or this string if it
	 *         has no properties to be substituted
	 */
	public static String filterProps(String target) {
		return filterProps(target, null, null);
	}

	/**
	 * Returns a copy of the given string {@code target}, with the given
	 * {@code properties} and system properties substituted using syntax
	 * <i>${&lt;property name&gt;}</i>. The property substitution can be nested.
	 * The given {@code properties} have higher precedence than system
	 * properties.
	 * 
	 * <p>
	 * This string itself will be returned, if it has no properties to be
	 * substituted.
	 * 
	 * @param target
	 *            the string to be filtered
	 * @param properties
	 *            the properties besides system properties to be substituted
	 * @return a copy of the given string that is filtered, or this string if it
	 *         has no properties to be substituted
	 */
	public static String fitlerProps(String target, Map<String, String> properties) {
		return filterProps(target, properties, null);
	}

	/**
	 * Returns a copy of the given string {@code target}, with the OSGi
	 * framework properties and system properties substituted using syntax
	 * <i>${&lt;property name&gt;}</i>. The property substitution can be nested.
	 * The OSGi framework properties have higher precedence than system
	 * properties.
	 * 
	 * <p>
	 * This string itself will be returned, if it has no properties to be
	 * substituted.
	 * 
	 * @param target
	 *            the string to be filtered
	 * @param context
	 *            the bundle context to get the OSGi framework properties
	 * @return a copy of the given string that is filtered, or this string if it
	 *         has no properties to be substituted
	 */
	public static String filterProps(String target, BundleContext context) {
		return filterProps(target, null, context);
	}

	/**
	 * Returns a copy of the given string {@code target}, with the given
	 * {@code properties}, OSGi framework properties and system properties
	 * substituted using syntax <i>${&lt;property name&gt;}</i>. The property
	 * substitution can be nested. The precedence of properties to be taken for
	 * substitution from highest to lowest is the given {@code properties}, OSGi
	 * framework properties and system properties.
	 * 
	 * <p>
	 * This string itself will be returned, if it has no properties to be
	 * substituted.
	 * 
	 * @param target
	 *            the string to be filtered
	 * @param properties
	 *            the properties besides OSGi framework properties and system
	 *            properties to be substituted
	 * @param context
	 *            the bundle context to get the OSGi framework properties
	 * @return a copy of the given string that is filtered, or this string if it
	 *         has no properties to be substituted
	 */
	public static String filterProps(String target, Map<String, String> properties, BundleContext context) {
		if (target == null)
			return null;

		if (target.length() < 2)
			return target;

		try (StringBuilder builder = StringBuilder.get(); IntStack stack = IntStack.get()) {
			String propValue = null;
			final int j = target.length();
			for (int i = 0; i < j; ++i) {
				char c = target.charAt(i);
				switch (c) {
				case '\\':
					if (++i < j)
						c = target.charAt(i);
					break;
				case '$':
					builder.append(c);
					if (++i < j && (c = target.charAt(i)) == '{')
						stack.push(builder.length() - 1);
					break;
				case '}':
					if (!stack.isEmpty()) {
						int index = stack.popInternal();
						propValue = getPropValue(builder.substring(index + 2), properties, context);
						if (propValue != null) {
							builder.setLength(index);
							builder.append(propValue);
							continue;
						}
					}
				}

				builder.append(c);
			}

			if (propValue != null || builder.length() != j)
				target = builder.toString();
		}

		return target;
	}

	/**
	 * Concatenates the provided arguments into a single {@code String}. Null
	 * argument is treated as an empty string.
	 * 
	 * @param obj0
	 *            the first {@code Object} to construct the string
	 * @param obj1
	 *            the second {@code Object} to construct the string
	 * @return the {@code String} constructed
	 * @since 1.1
	 */
	public static String join(Object obj0, Object obj1) {
		try (StringBuilder builder = StringBuilder.get()) {
			if (obj0 != null)
				builder.append(obj0);
			if (obj1 != null)
				builder.append(obj1);
			return builder.toString();
		}
	}

	/**
	 * Concatenates the provided arguments into a single {@code String}. Null
	 * argument is treated as an empty string.
	 * 
	 * @param obj0
	 *            the first {@code Object} to construct the string.
	 * @param obj1
	 *            the second {@code Object} to construct the string.
	 * @param obj2
	 *            the third {@code Object} to construct the string.
	 * @return the {@code String} constructed.
	 * @since 1.1
	 */
	public static String join(Object obj0, Object obj1, Object obj2) {
		try (StringBuilder builder = StringBuilder.get()) {
			if (obj0 != null)
				builder.append(obj0);
			if (obj1 != null)
				builder.append(obj1);
			if (obj2 != null)
				builder.append(obj2);
			return builder.toString();
		}
	}

	/**
	 * Concatenates the string values of the 4 given {@code obj}s by using the
	 * {@link StringBuilder} object associated with the current thread, and
	 * returns the constructed {@code String}. Null argument is treated as an
	 * empty string.
	 * 
	 * @param obj0
	 *            the first {@code Object} to construct the string.
	 * @param obj1
	 *            the second {@code Object} to construct the string.
	 * @param obj2
	 *            the third {@code Object} to construct the string.
	 * @param obj3
	 *            the fourth {@code Object} to construct the string.
	 * @return the {@code String} constructed.
	 * @since 1.1
	 */
	public static String join(Object obj0, Object obj1, Object obj2, Object obj3) {
		try (StringBuilder builder = StringBuilder.get()) {
			if (obj0 != null)
				builder.append(obj0);
			if (obj1 != null)
				builder.append(obj1);
			if (obj2 != null)
				builder.append(obj2);
			if (obj3 != null)
				builder.append(obj3);
			return builder.toString();
		}
	}

	/**
	 * Concatenates the string values of the 5 given {@code obj}s by using the
	 * {@link StringBuilder} object associated with the current thread, and
	 * returns the constructed {@code String}. Null argument is treated as an
	 * empty string.
	 * 
	 * @param obj0
	 *            the first {@code Object} to construct the string.
	 * @param obj1
	 *            the second {@code Object} to construct the string.
	 * @param obj2
	 *            the third {@code Object} to construct the string.
	 * @param obj3
	 *            the fourth {@code Object} to construct the string.
	 * @param obj4
	 *            the fifth {@code Object} to construct the string.
	 * @return the {@code String} constructed.
	 * @since 1.1
	 */
	public static String join(Object obj0, Object obj1, Object obj2, Object obj3, Object obj4) {
		try (StringBuilder builder = StringBuilder.get()) {
			if (obj0 != null)
				builder.append(obj0);

			if (obj1 != null)
				builder.append(obj1);

			if (obj2 != null)
				builder.append(obj2);

			if (obj3 != null)
				builder.append(obj3);

			if (obj4 != null)
				builder.append(obj4);

			return builder.toString();
		}
	}

	/**
	 * Concatenates the string values of the 6 given {@code obj}s by using the
	 * {@link StringBuilder} object associated with the current thread, and
	 * returns the constructed {@code String}. Null argument is treated as an
	 * empty string.
	 * 
	 * @param obj0
	 *            the first {@code Object} to construct the string.
	 * @param obj1
	 *            the second {@code Object} to construct the string.
	 * @param obj2
	 *            the third {@code Object} to construct the string.
	 * @param obj3
	 *            the fourth {@code Object} to construct the string.
	 * @param obj4
	 *            the fifth {@code Object} to construct the string.
	 * @param obj5
	 *            the sixth {@code Object} to construct the string.
	 * @return the {@code String} constructed.
	 * @since 1.1
	 */
	public static String join(Object obj0, Object obj1, Object obj2, Object obj3, Object obj4, Object obj5) {
		try (StringBuilder builder = StringBuilder.get()) {
			if (obj0 != null)
				builder.append(obj0);
			if (obj1 != null)
				builder.append(obj1);
			if (obj2 != null)
				builder.append(obj2);
			if (obj3 != null)
				builder.append(obj3);
			if (obj4 != null)
				builder.append(obj4);
			if (obj5 != null)
				builder.append(obj5);
			return builder.toString();
		}
	}

	/**
	 * Concatenates the string values of the 7 given {@code obj}s by using the
	 * {@link StringBuilder} object associated with the current thread, and
	 * returns the constructed {@code String}. Null argument is treated as an
	 * empty string.
	 * 
	 * @param obj0
	 *            the first {@code Object} to construct the string.
	 * @param obj1
	 *            the second {@code Object} to construct the string.
	 * @param obj2
	 *            the third {@code Object} to construct the string.
	 * @param obj3
	 *            the fourth {@code Object} to construct the string.
	 * @param obj4
	 *            the fifth {@code Object} to construct the string.
	 * @param obj5
	 *            the sixth {@code Object} to construct the string.
	 * @param obj6
	 *            the seventh {@code Object} to construct the string.
	 * @return the {@code String} constructed.
	 * @since 1.1
	 */
	public static String join(Object obj0, Object obj1, Object obj2, Object obj3, Object obj4, Object obj5, Object obj6) {
		try (StringBuilder builder = StringBuilder.get()) {
			if (obj0 != null)
				builder.append(obj0);
			if (obj1 != null)
				builder.append(obj1);
			if (obj2 != null)
				builder.append(obj2);
			if (obj3 != null)
				builder.append(obj3);
			if (obj4 != null)
				builder.append(obj4);
			if (obj5 != null)
				builder.append(obj5);
			if (obj6 != null)
				builder.append(obj6);
			return builder.toString();
		}
	}

	/**
	 * Concatenates the string values of the given {@code obj}s by using the
	 * {@link StringBuilder} object associated with the current thread, and
	 * returns the constructed {@code String}. Null argument is treated as an
	 * empty string.
	 * 
	 * @param obj
	 *            the first {@code Object} to construct the string.
	 * @param objs
	 *            the rest {@code Object}s to construct the string.
	 * @return the {@code String} constructed.
	 * @since 1.1
	 */
	public static String join(Object obj, Object... objs) {
		try (StringBuilder builder = StringBuilder.get()) {
			if (obj != null)
				builder.append(obj);
			for (Object o : objs) {
				if (o != null)
					builder.append(o);
			}
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param delimiter
	 *            the delimiter character to use
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(char[] array, char delimiter) {
		if (array == null)
			return null;

		int n = array.length;
		if (n < 1)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[0]);
			for (int i = 1; i < n; ++i)
				builder.append(delimiter).append(array[i]);
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param begin
	 *            start joining at this offset
	 * @param end
	 *            stop joining at this offset
	 * @param delimiter
	 *            the delimiter character to use
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(char[] array, int begin, int end, char delimiter) {
		if (array == null)
			return null;

		if (end <= begin)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[begin]);
			while (++begin < end)
				builder.append(delimiter).append(array[begin]);
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param delimiter
	 *            the delimiter string to use, {@code null} treated as empty
	 *            string
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(char[] array, String delimiter) {
		if (array == null)
			return null;

		int n = array.length;
		if (n < 1)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[0]);
			for (int i = 1; i < n; ++i) {
				if (delimiter != null)
					builder.append(delimiter);
				builder.append(array[i]);
			}
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param begin
	 *            start joining at this offset
	 * @param end
	 *            stop joining at this offset
	 * @param delimiter
	 *            the delimiter string to use, {@code null} treated as empty
	 *            string
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(char[] array, int begin, int end, String delimiter) {
		if (array == null)
			return null;

		if (end <= begin)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[begin]);
			while (++begin < end) {
				if (delimiter != null)
					builder.append(delimiter);
				builder.append(array[begin]);
			}
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param delimiter
	 *            the delimiter character to use
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(byte[] array, char delimiter) {
		if (array == null)
			return null;

		int n = array.length;
		if (n < 1)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[0]);
			for (int i = 1; i < n; ++i)
				builder.append(delimiter).append(array[i]);
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param begin
	 *            start joining at this offset
	 * @param end
	 *            stop joining at this offset
	 * @param delimiter
	 *            the delimiter character to use
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(byte[] array, int begin, int end, char delimiter) {
		if (array == null)
			return null;

		if (end <= begin)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[begin]);
			while (++begin < end)
				builder.append(delimiter).append(array[begin]);
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param delimiter
	 *            the delimiter string to use, {@code null} treated as empty
	 *            string
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(byte[] array, String delimiter) {
		if (array == null)
			return null;

		int n = array.length;
		if (n < 1)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[0]);
			for (int i = 1; i < n; ++i) {
				if (delimiter != null)
					builder.append(delimiter);
				builder.append(array[i]);
			}
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param begin
	 *            start joining at this offset
	 * @param end
	 *            stop joining at this offset
	 * @param delimiter
	 *            the delimiter string to use, {@code null} treated as empty
	 *            string
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(byte[] array, int begin, int end, String delimiter) {
		if (array == null)
			return null;

		if (end <= begin)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[begin]);
			while (++begin < end) {
				if (delimiter != null)
					builder.append(delimiter);
				builder.append(array[begin]);
			}
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param delimiter
	 *            the delimiter character to use
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(short[] array, char delimiter) {
		if (array == null)
			return null;

		int n = array.length;
		if (n < 1)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[0]);
			for (int i = 1; i < n; ++i)
				builder.append(delimiter).append(array[i]);
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param begin
	 *            start joining at this offset
	 * @param end
	 *            stop joining at this offset
	 * @param delimiter
	 *            the delimiter character to use
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(short[] array, int begin, int end, char delimiter) {
		if (array == null)
			return null;

		if (end <= begin)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[begin]);
			while (++begin < end)
				builder.append(delimiter).append(array[begin]);
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param delimiter
	 *            the delimiter string to use, {@code null} treated as empty
	 *            string
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(short[] array, String delimiter) {
		if (array == null)
			return null;

		int n = array.length;
		if (n < 1)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[0]);
			for (int i = 1; i < n; ++i) {
				if (delimiter != null)
					builder.append(delimiter);
				builder.append(array[i]);
			}
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param begin
	 *            start joining at this offset
	 * @param end
	 *            stop joining at this offset
	 * @param delimiter
	 *            the delimiter string to use, {@code null} treated as empty
	 *            string
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(short[] array, int begin, int end, String delimiter) {
		if (array == null)
			return null;

		if (end <= begin)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[begin]);
			while (++begin < end) {
				if (delimiter != null)
					builder.append(delimiter);
				builder.append(array[begin]);
			}
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param delimiter
	 *            the delimiter character to use
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(int[] array, char delimiter) {
		if (array == null)
			return null;

		int n = array.length;
		if (n < 1)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[0]);
			for (int i = 1; i < n; ++i)
				builder.append(delimiter).append(array[i]);
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param begin
	 *            start joining at this offset
	 * @param end
	 *            stop joining at this offset
	 * @param delimiter
	 *            the delimiter character to use
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(int[] array, int begin, int end, char delimiter) {
		if (array == null)
			return null;

		if (end <= begin)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[begin]);
			while (++begin < end)
				builder.append(delimiter).append(array[begin]);
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param delimiter
	 *            the delimiter string to use, {@code null} treated as empty
	 *            string
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(int[] array, String delimiter) {
		if (array == null)
			return null;

		int n = array.length;
		if (n < 1)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[0]);
			for (int i = 1; i < n; ++i) {
				if (delimiter != null)
					builder.append(delimiter);
				builder.append(array[i]);
			}
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param begin
	 *            start joining at this offset
	 * @param end
	 *            stop joining at this offset
	 * @param delimiter
	 *            the delimiter string to use, {@code null} treated as empty
	 *            string
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(int[] array, int begin, int end, String delimiter) {
		if (array == null)
			return null;

		if (end <= begin)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[begin]);
			while (++begin < end) {
				if (delimiter != null)
					builder.append(delimiter);
				builder.append(array[begin]);
			}
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param delimiter
	 *            the delimiter character to use
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(long[] array, char delimiter) {
		if (array == null)
			return null;

		int n = array.length;
		if (n < 1)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[0]);
			for (int i = 1; i < n; ++i)
				builder.append(delimiter).append(array[i]);
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param begin
	 *            start joining at this offset
	 * @param end
	 *            stop joining at this offset
	 * @param delimiter
	 *            the delimiter character to use
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(long[] array, int begin, int end, char delimiter) {
		if (array == null)
			return null;

		if (end <= begin)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[begin]);
			while (++begin < end)
				builder.append(delimiter).append(array[begin]);
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param delimiter
	 *            the delimiter string to use, {@code null} treated as empty
	 *            string
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(long[] array, String delimiter) {
		if (array == null)
			return null;

		int n = array.length;
		if (n < 1)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[0]);
			for (int i = 1; i < n; ++i) {
				if (delimiter != null)
					builder.append(delimiter);
				builder.append(array[i]);
			}
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param begin
	 *            start joining at this offset
	 * @param end
	 *            stop joining at this offset
	 * @param delimiter
	 *            the delimiter string to use, {@code null} treated as empty
	 *            string
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(long[] array, int begin, int end, String delimiter) {
		if (array == null)
			return null;

		if (end <= begin)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[begin]);
			while (++begin < end) {
				if (delimiter != null)
					builder.append(delimiter);
				builder.append(array[begin]);
			}
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param delimiter
	 *            the delimiter character to use
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(float[] array, char delimiter) {
		if (array == null)
			return null;

		int n = array.length;
		if (n < 1)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[0]);
			for (int i = 1; i < n; ++i)
				builder.append(delimiter).append(array[i]);
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param begin
	 *            start joining at this offset
	 * @param end
	 *            stop joining at this offset
	 * @param delimiter
	 *            the delimiter character to use
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(float[] array, int begin, int end, char delimiter) {
		if (array == null)
			return null;

		if (end <= begin)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[begin]);
			while (++begin < end)
				builder.append(delimiter).append(array[begin]);
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param delimiter
	 *            the delimiter string to use, {@code null} treated as empty
	 *            string
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(float[] array, String delimiter) {
		if (array == null)
			return null;

		int n = array.length;
		if (n < 1)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[0]);
			for (int i = 1; i < n; ++i) {
				if (delimiter != null)
					builder.append(delimiter);
				builder.append(array[i]);
			}
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param begin
	 *            start joining at this offset
	 * @param end
	 *            stop joining at this offset
	 * @param delimiter
	 *            the delimiter string to use, {@code null} treated as empty
	 *            string
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(float[] array, int begin, int end, String delimiter) {
		if (array == null)
			return null;

		if (end <= begin)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[begin]);
			while (++begin < end) {
				if (delimiter != null)
					builder.append(delimiter);
				builder.append(array[begin]);
			}
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param delimiter
	 *            the delimiter character to use
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(double[] array, char delimiter) {
		if (array == null)
			return null;

		int n = array.length;
		if (n < 1)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[0]);
			for (int i = 1; i < n; ++i)
				builder.append(delimiter).append(array[i]);
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param begin
	 *            start joining at this offset
	 * @param end
	 *            stop joining at this offset
	 * @param delimiter
	 *            the delimiter character to use
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(double[] array, int begin, int end, char delimiter) {
		if (array == null)
			return null;

		if (end <= begin)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[begin]);
			while (++begin < end)
				builder.append(delimiter).append(array[begin]);

			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param delimiter
	 *            the delimiter string to use, {@code null} treated as empty
	 *            string
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(double[] array, String delimiter) {
		if (array == null)
			return null;

		int n = array.length;
		if (n < 1)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[0]);
			for (int i = 1; i < n; ++i) {
				if (delimiter != null)
					builder.append(delimiter);
				builder.append(array[i]);
			}
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param begin
	 *            start joining at this offset
	 * @param end
	 *            stop joining at this offset
	 * @param delimiter
	 *            the delimiter string to use, {@code null} treated as empty
	 *            string
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(double[] array, int begin, int end, String delimiter) {
		if (array == null)
			return null;

		if (end <= begin)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(array[begin]);
			while (++begin < end) {
				if (delimiter != null)
					builder.append(delimiter);
				builder.append(array[begin]);
			}
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param delimiter
	 *            the delimiter character to use
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(Object[] array, char delimiter) {
		if (array == null)
			return null;

		int n = array.length;
		if (n < 1)
			return "";

		Object obj = null;
		int i = 0;
		while (i < n && (obj = array[i]) == null)
			++i;

		if (obj == null)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(obj);
			while (++i < n) {
				obj = array[i];
				if (obj != null) {
					builder.append(delimiter);
					builder.append(obj);
				}
			}
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param begin
	 *            start joining at this offset
	 * @param end
	 *            stop joining at this offset
	 * @param delimiter
	 *            the delimiter character to use
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(Object[] array, int begin, int end, char delimiter) {
		if (array == null)
			return null;

		if (end <= begin)
			return "";

		Object obj = null;
		while (begin < end && (obj = array[begin]) == null)
			++begin;

		if (obj == null)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(obj);
			while (++begin < end) {
				obj = array[begin];
				if (obj != null) {
					builder.append(delimiter);
					builder.append(obj);
				}
			}
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param delimiter
	 *            the delimiter string to use, {@code null} treated as empty
	 *            string
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(Object[] array, String delimiter) {
		if (array == null)
			return null;

		int n = array.length;
		if (n < 1)
			return "";

		Object obj = null;
		int i = 0;
		while (i < n && (obj = array[i]) == null)
			++i;

		if (obj == null)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(obj);
			while (++i < n) {
				obj = array[i];
				if (obj != null) {
					if (delimiter != null)
						builder.append(delimiter);
					builder.append(obj);
				}
			}
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code array} into a single
	 * {@code String} containing the provided list of elements.
	 * <p>
	 * No delimiter is added before or after the list. A null delimiter is the
	 * same as an empty string.
	 * 
	 * @param array
	 *            the array of values to join together, may be null
	 * @param begin
	 *            start joining at this offset
	 * @param end
	 *            stop joining at this offset
	 * @param delimiter
	 *            the delimiter string to use, {@code null} treated as empty
	 *            string
	 * @return the joined {@code String}, {@code null} if null array input
	 * @since 1.1
	 */
	public static String join(Object[] array, int begin, int end, String delimiter) {
		if (array == null)
			return null;

		if (end <= begin)
			return "";

		Object obj = null;
		while (begin < end && (obj = array[begin]) == null)
			++begin;

		if (obj == null)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(obj);
			while (++begin < end) {
				obj = array[begin];
				if (obj != null) {
					if (delimiter != null)
						builder.append(delimiter);
					builder.append(obj);
				}
			}
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code Iterable} into a single
	 * {@code String} containing the provided elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param iterable
	 *            the {@code Iterable} providing the values to join together,
	 *            may be null
	 * @param delimiter
	 *            the delimiter character to use
	 * @return the joined {@code String}, {@code null} if null iterable input
	 */
	public static String join(Iterable<?> iterable, char delimiter) {
		if (iterable == null)
			return null;

		Object obj = null;
		Iterator<?> iterator = iterable.iterator();
		while (iterator.hasNext() && (obj = iterator.next()) == null)
			;

		if (obj == null)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(obj);
			while (iterator.hasNext()) {
				obj = iterator.next();
				if (obj != null) {
					builder.append(delimiter);
					builder.append(obj);
				}
			}
			return builder.toString();
		}
	}

	/**
	 * Joins the elements of the provided {@code Iterable} into a single
	 * {@code String} containing the provided elements.
	 * <p>
	 * No delimiter is added before or after the list.
	 * 
	 * @param iterable
	 *            the {@code Iterable} providing the values to join together,
	 *            may be null
	 * @param delimiter
	 *            the delimiter string to use, {@code null} treated as empty
	 *            string
	 * @return the joined {@code String}, {@code null} if null iterable input
	 */
	public static String join(Iterable<?> iterable, String delimiter) {
		if (iterable == null)
			return null;

		Object obj = null;
		Iterator<?> iterator = iterable.iterator();
		while (iterator.hasNext() && (obj = iterator.next()) == null)
			;

		if (obj == null)
			return "";

		try (StringBuilder builder = StringBuilder.get()) {
			builder.append(obj);
			while (iterator.hasNext()) {
				obj = iterator.next();
				if (obj != null) {
					if (delimiter != null)
						builder.append(delimiter);
					builder.append(obj);
				}
			}
			return builder.toString();
		}
	}

	/**
	 * Returns a {@code String} constructed as follows.
	 * 
	 * <pre>
	 * {@code StringBuilder.get().deeplyAppend(obj).toStringAndClose()}
	 * </pre>
	 * 
	 * @param obj
	 *            of which the {@code String} value is returned.
	 * @return the {@code String} value of the given {@code obj}.
	 */
	public static String deeplyBuild(Object obj) {
		if (obj == null || !obj.getClass().isArray())
			return String.valueOf(obj);

		try (StringBuilder builder = StringBuilder.get()) {
			return builder.deeplyAppend(obj).toString();
		}
	}

	/**
	 * Returns a {@code String} constructed as follows.
	 * 
	 * <pre>
	 * {@code StringBuilder.get().deeplyAppend(obj0).deeplyAppend(obj1).toStringAndClose()}
	 * </pre>
	 * 
	 * @param obj0
	 *            the first {@code Object} to construct the string.
	 * @param obj1
	 *            the second {@code Object} to construct the string.
	 * @return the {@code String} constructed.
	 */
	public static String deeplyBuild(Object obj0, Object obj1) {
		try (StringBuilder builder = StringBuilder.get()) {
			return builder.deeplyAppend(obj0).deeplyAppend(obj1).toString();
		}
	}

	/**
	 * Returns a {@code String} constructed as follows.
	 * 
	 * <pre>
	 * {@code StringBuilder.get().deeplyAppend(obj0)...deeplyAppend(obj2).toStringAndClose()}
	 * </pre>
	 * 
	 * @param obj0
	 *            the first {@code Object} to construct the string.
	 * @param obj1
	 *            the second {@code Object} to construct the string.
	 * @param obj2
	 *            the third {@code Object} to construct the string.
	 * @return the {@code String} constructed.
	 */
	public static String deeplyBuild(Object obj0, Object obj1, Object obj2) {
		try (StringBuilder builder = StringBuilder.get()) {
			return builder.deeplyAppend(obj0).deeplyAppend(obj1).deeplyAppend(obj2).toString();
		}
	}

	/**
	 * Returns a {@code String} constructed as follows.
	 * 
	 * <pre>
	 * {@code StringBuilder.get().deeplyAppend(obj0)...deeplyAppend(obj3).toStringAndClose()}
	 * </pre>
	 * 
	 * @param obj0
	 *            the first {@code Object} to construct the string.
	 * @param obj1
	 *            the second {@code Object} to construct the string.
	 * @param obj2
	 *            the third {@code Object} to construct the string.
	 * @param obj3
	 *            the fourth {@code Object} to construct the string.
	 * @return the {@code String} constructed.
	 */
	public static String deeplyBuild(Object obj0, Object obj1, Object obj2, Object obj3) {
		try (StringBuilder builder = StringBuilder.get()) {
			return builder.deeplyAppend(obj0).deeplyAppend(obj1).deeplyAppend(obj2).deeplyAppend(obj3).toString();
		}
	}

	/**
	 * Returns a {@code String} constructed as follows.
	 * 
	 * <pre>
	 * {@code StringBuilder.get().deeplyAppend(obj0)...deeplyAppend(obj4).toStringAndClose()}
	 * </pre>
	 * 
	 * @param obj0
	 *            the first {@code Object} to construct the string.
	 * @param obj1
	 *            the second {@code Object} to construct the string.
	 * @param obj2
	 *            the third {@code Object} to construct the string.
	 * @param obj3
	 *            the fourth {@code Object} to construct the string.
	 * @param obj4
	 *            the fifth {@code Object} to construct the string.
	 * @return the {@code String} constructed.
	 */
	public static String deeplyBuild(Object obj0, Object obj1, Object obj2, Object obj3, Object obj4) {
		try (StringBuilder builder = StringBuilder.get()) {
			return builder.deeplyAppend(obj0).deeplyAppend(obj1).deeplyAppend(obj2).deeplyAppend(obj3)
					.deeplyAppend(obj4).toString();
		}
	}

	/**
	 * Returns a {@code String} constructed as follows.
	 * 
	 * <pre>
	 * {@code StringBuilder.get().deeplyAppend(obj0)...deeplyAppend(obj5).toStringAndClose()}
	 * </pre>
	 * 
	 * @param obj0
	 *            the first {@code Object} to construct the string.
	 * @param obj1
	 *            the second {@code Object} to construct the string.
	 * @param obj2
	 *            the third {@code Object} to construct the string.
	 * @param obj3
	 *            the fourth {@code Object} to construct the string.
	 * @param obj4
	 *            the fifth {@code Object} to construct the string.
	 * @param obj5
	 *            the sixth {@code Object} to construct the string.
	 * @return the {@code String} constructed.
	 */
	public static String deeplyBuild(Object obj0, Object obj1, Object obj2, Object obj3, Object obj4, Object obj5) {
		try (StringBuilder builder = StringBuilder.get()) {
			return builder.deeplyAppend(obj0).deeplyAppend(obj1).deeplyAppend(obj2).deeplyAppend(obj3)
					.deeplyAppend(obj4).deeplyAppend(obj5).toString();
		}
	}

	/**
	 * Returns a {@code String} constructed as follows.
	 * 
	 * <pre>
	 * {@code StringBuilder.get().deeplyAppend(obj0)...deeplyAppend(obj6).toStringAndClose()}
	 * </pre>
	 * 
	 * @param obj0
	 *            the first {@code Object} to construct the string.
	 * @param obj1
	 *            the second {@code Object} to construct the string.
	 * @param obj2
	 *            the third {@code Object} to construct the string.
	 * @param obj3
	 *            the fourth {@code Object} to construct the string.
	 * @param obj4
	 *            the fifth {@code Object} to construct the string.
	 * @param obj5
	 *            the sixth {@code Object} to construct the string.
	 * @param obj6
	 *            the seventh {@code Object} to construct the string.
	 * @return the {@code String} constructed.
	 */
	public static String deeplyBuild(Object obj0, Object obj1, Object obj2, Object obj3, Object obj4, Object obj5,
			Object obj6) {
		try (StringBuilder builder = StringBuilder.get()) {
			return builder.deeplyAppend(obj0).deeplyAppend(obj1).deeplyAppend(obj2).deeplyAppend(obj3)
					.deeplyAppend(obj4).deeplyAppend(obj5).deeplyAppend(obj6).toString();
		}
	}

	/**
	 * Returns a {@code String} constructed as follows.
	 * 
	 * <pre>
	 * {@code StringBuilder.get().deeplyAppend(objs[0])...deeplyAppend(objs[objs.length -1]).toString()}
	 * </pre>
	 * 
	 * @param objs
	 *            the rest arguments forming the string to be built.
	 * @return the {@code String} object constructed.
	 */
	public static String deeplyBuild(Object... objs) {
		try (StringBuilder builder = StringBuilder.get()) {
			for (Object o : objs)
				builder.deeplyAppend(o);

			return builder.toString();
		}
	}

	static void getChars(int i, int index, char[] buf) {
		boolean negative = false;
		if (i < 0) {
			negative = true;
			i = -i;
		}

		int q = 0;
		int r = 0;
		while (i >= 65536) {
			q = i / 100;
			r = i - (q * 100);
			i = q;
			buf[--index] = c_digitOnes[r];
			buf[--index] = c_digitTens[r];
		}

		for (;;) {
			q = (i * 52429) >>> (16 + 3);
			r = i - (q * 10);
			buf[--index] = c_digits[r];
			i = q;
			if (i == 0)
				break;
		}

		if (negative)
			buf[--index] = '-';
	}

	static void getChars(long l, int index, char[] buf) {
		boolean negative = false;
		if (l < 0) {
			negative = true;
			l = -l;
		}

		long q = 0L;
		int r = 0;
		while (l > Integer.MAX_VALUE) {
			q = l / 100;
			r = (int) (l - (q * 100));
			l = q;
			buf[--index] = c_digitOnes[r];
			buf[--index] = c_digitTens[r];
		}

		int q2 = 0;
		int i = (int) l;
		while (i >= 65536) {
			q2 = i / 100;
			r = i - (q2 * 100);
			i = q2;
			buf[--index] = c_digitOnes[r];
			buf[--index] = c_digitTens[r];
		}

		for (;;) {
			q2 = (i * 52429) >>> (16 + 3);
			r = i - (q2 * 10);
			buf[--index] = c_digits[r];
			i = q2;
			if (i == 0)
				break;
		}

		if (negative)
			buf[--index] = '-';
	}

	private static String getPropValue(String name, Map<String, String> properties, BundleContext context) {
		String value = null;
		if (properties != null && (value = properties.get(name)) != null)
			return value;

		if (context != null)
			return context.getProperty(name);

		return System.getProperty(name);
	}
}
