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
package org.jruyi.cmd.conf;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.jruyi.common.Properties;
import org.jruyi.common.StrUtil;
import org.jruyi.common.StringBuilder;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

public final class PropUtil {

	enum Type {

		STRING {

			@Override
			public Object convert(String str) {
				return str;
			}

			@Override
			public Object convert(String[] strs) {
				return strs;
			}

			@Override
			public Object convertToVector(String[] strs) {
				Vector<String> vec = new Vector<String>(strs.length);
				for (String str : strs)
					vec.add(str);
				return vec;
			}

			@Override
			public boolean checkType(Class<?> clazz) {
				return clazz == String.class;
			}
		},
		LONG {

			@Override
			public Object convert(String str) {
				return Long.valueOf(str);
			}

			@Override
			public Object convert(String[] strs) {
				int n = strs.length;
				long[] longs = new long[n];
				for (int i = 0; i < n; ++i)
					longs[i] = Long.parseLong(strs[i]);

				return longs;
			}

			@Override
			public Object convertToVector(String[] strs) {
				Vector<Long> vec = new Vector<Long>(strs.length);
				for (String str : strs)
					vec.add(Long.valueOf(str));
				return vec;
			}

			@Override
			public boolean checkType(Class<?> clazz) {
				return clazz == Long.class || clazz == long.class;
			}
		},
		INTEGER {

			@Override
			public Object convert(String str) {
				return Integer.valueOf(str);
			}

			@Override
			public Object convert(String[] strs) {
				int n = strs.length;
				int[] ints = new int[n];
				for (int i = 0; i < n; ++i)
					ints[i] = Integer.parseInt(strs[i]);

				return ints;
			}

			@Override
			public Object convertToVector(String[] strs) {
				Vector<Integer> vec = new Vector<Integer>(strs.length);
				for (String str : strs)
					vec.add(Integer.valueOf(str));
				return vec;
			}

			@Override
			public boolean checkType(Class<?> clazz) {
				return clazz == Integer.class || clazz == int.class;
			}
		},
		SHORT {

			@Override
			public Object convert(String str) {
				return Short.valueOf(str);
			}

			@Override
			public Object convert(String[] strs) {
				int n = strs.length;
				short[] shorts = new short[n];
				for (int i = 0; i < n; ++i)
					shorts[i] = Short.parseShort(strs[i]);

				return shorts;
			}

			@Override
			public Object convertToVector(String[] strs) {
				Vector<Short> vec = new Vector<Short>(strs.length);
				for (String str : strs)
					vec.add(Short.valueOf(str));
				return vec;
			}

			@Override
			public boolean checkType(Class<?> clazz) {
				return clazz == Short.class || clazz == short.class;
			}
		},
		CHARACTER {

			@Override
			public Object convert(String str) {
				return str.charAt(0);
			}

			@Override
			public Object convert(String[] strs) {
				int n = strs.length;
				char[] chars = new char[n];
				for (int i = 0; i < n; ++i)
					chars[i] = strs[i].charAt(0);

				return chars;
			}

			@Override
			public Object convertToVector(String[] strs) {
				Vector<Character> vec = new Vector<Character>(strs.length);
				for (String str : strs)
					vec.add(str.charAt(0));
				return vec;
			}

			@Override
			public boolean checkType(Class<?> clazz) {
				return clazz == Character.class || clazz == char.class;
			}
		},
		BYTE {

			@Override
			public Object convert(String str) {
				return Byte.valueOf(str);
			}

			@Override
			public Object convert(String[] strs) {
				int n = strs.length;
				byte[] bytes = new byte[n];
				for (int i = 0; i < n; ++i)
					bytes[i] = Byte.parseByte(strs[i]);

				return bytes;
			}

			@Override
			public Object convertToVector(String[] strs) {
				Vector<Byte> vec = new Vector<Byte>(strs.length);
				for (String str : strs)
					vec.add(Byte.valueOf(str));
				return vec;
			}

			@Override
			public boolean checkType(Class<?> clazz) {
				return clazz == Byte.class || clazz == byte.class;
			}
		},
		DOUBLE {

			@Override
			public Object convert(String str) {
				return Double.valueOf(str);
			}

			@Override
			public Object convert(String[] strs) {
				int n = strs.length;
				double[] doubles = new double[n];
				for (int i = 0; i < n; ++i)
					doubles[i] = Double.parseDouble(strs[i]);

				return doubles;
			}

			@Override
			public Object convertToVector(String[] strs) {
				Vector<Double> vec = new Vector<Double>(strs.length);
				for (String str : strs)
					vec.add(Double.valueOf(str));
				return vec;
			}

			@Override
			public boolean checkType(Class<?> clazz) {
				return clazz == Double.class || clazz == double.class;
			}
		},
		FLOAT {

			@Override
			public Object convert(String str) {
				return Float.valueOf(str);
			}

			@Override
			public Object convert(String[] strs) {
				int n = strs.length;
				float[] floats = new float[n];
				for (int i = 0; i < n; ++i)
					floats[i] = Float.parseFloat(strs[i]);

				return floats;
			}

			@Override
			public Object convertToVector(String[] strs) {
				Vector<Float> vec = new Vector<Float>(strs.length);
				for (String str : strs)
					vec.add(Float.valueOf(str));
				return vec;
			}

			@Override
			public boolean checkType(Class<?> clazz) {
				return clazz == Float.class || clazz == float.class;
			}
		},
		BOOLEAN {

			@Override
			public Object convert(String str) {
				return Boolean.valueOf(str);
			}

			@Override
			public boolean[] convert(String[] strs) {
				int n = strs.length;
				boolean[] booleans = new boolean[n];
				for (int i = 0; i < n; ++i)
					booleans[i] = Boolean.parseBoolean(strs[i]);

				return booleans;
			}

			@Override
			public Object convertToVector(String[] strs) {
				Vector<Boolean> vec = new Vector<Boolean>(strs.length);
				for (String str : strs)
					vec.add(Boolean.valueOf(str));
				return vec;
			}

			@Override
			public boolean checkType(Class<?> clazz) {
				return clazz == Boolean.class || clazz == boolean.class;
			}
		};

		public abstract Object convert(String str);

		public abstract Object convert(String[] strs);

		public abstract Object convertToVector(String[] strs);

		public abstract boolean checkType(Class<?> clazz);

		public static Type valueOf(int type) {

			switch (type) {
			case AttributeDefinition.STRING:
			case AttributeDefinition.PASSWORD:
				return STRING;
			case AttributeDefinition.INTEGER:
				return INTEGER;
			case AttributeDefinition.LONG:
				return LONG;
			case AttributeDefinition.BOOLEAN:
				return BOOLEAN;
			case AttributeDefinition.SHORT:
				return SHORT;
			case AttributeDefinition.FLOAT:
				return FLOAT;
			case AttributeDefinition.DOUBLE:
				return DOUBLE;
			case AttributeDefinition.BYTE:
				return BYTE;
			case AttributeDefinition.CHARACTER:
				return CHARACTER;
			}

			throw new RuntimeException(StrUtil.join("Unknown Type: ", type));
		}
	}

	private PropUtil() {
	}

	public static Properties normalize(Dictionary<String, ?> props,
			ObjectClassDefinition ocd) throws Exception {
		AttributeDefinition[] ads = ocd
				.getAttributeDefinitions(ObjectClassDefinition.REQUIRED);
		if (ads != null && ads.length > 0 && props == null)
			throw new RuntimeException(StrUtil.join("Property[",
					ads[0].getID(), "] is required"));

		Properties conf = new Properties();
		if (ads != null)
			normalize(props, conf, ads, true);

		ads = ocd.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL);
		if (ads != null)
			normalize(props, conf, ads, false);

		return conf;
	}

	private static void normalize(Dictionary<String, ?> props,
			Map<String, Object> conf, AttributeDefinition[] ads,
			boolean required) throws Exception {
		for (AttributeDefinition ad : ads) {
			String id = ad.getID();
			Object value = props.get(id);
			if (value == null) {
				handleDefaultValues(ad, conf, required);
				continue;
			}

			Class<?> clazz = value.getClass();
			if (clazz == String.class) {
				handleStringValue(ad, (String) value, conf, required);
				continue;
			}

			Type type = Type.valueOf(ad.getType());
			if (clazz.isArray()) {
				Object[] values = (Object[]) value;
				if (values.length > 0) {
					if (!type.checkType(values[0].getClass()))
						throw new Exception(StrUtil.join("Property[", id,
								"] should be of type: ", type));
					for (Object obj : values) {
						String message = ad.validate(String.valueOf(obj));
						if (message != null && message.length() > 0)
							throw new Exception(StrUtil.join("Error Property[",
									id, "]: ", message));
					}
				}
			} else if (!Iterable.class.isAssignableFrom(clazz)) {
				if (!type.checkType(clazz))
					throw new Exception(StrUtil.join("Property[", id,
							"] should be of type: ", type));
				String message = ad.validate(String.valueOf(value));
				if (message != null && message.length() > 0)
					throw new Exception(StrUtil.join("Error Property[", id,
							"]: ", message));
			} else {
				@SuppressWarnings("unchecked")
				Iterator<Object> iter = ((Iterable<Object>) value).iterator();
				for (int i = 0; iter.hasNext(); ++i) {
					Object obj = iter.next();
					if (!type.checkType(obj.getClass()))
						throw new Exception(StrUtil.join("Property[", id, "](",
								i, ") should be of type: ", type));

					String message = ad.validate(String.valueOf(obj));
					if (message != null && message.length() > 0)
						throw new Exception(StrUtil.join("Error Property[", id,
								"](", i, "): ", message));
				}
			}

			conf.put(id, value);
		}
	}

	private static void handleStringValue(AttributeDefinition ad, String value,
			Map<String, Object> conf, boolean required) throws Exception {
		String id = ad.getID();
		String[] values = ad.getCardinality() != 0 ? split(value) : null;
		String[] optionValues = ad.getOptionValues();
		if (optionValues != null && optionValues.length > 0) {
			if (values != null) {
				for (String v : values)
					validate(id, v, optionValues);
			} else
				validate(id, value, optionValues);
		} else {
			if (values != null) {
				for (String v : values)
					validate(id, v, ad);
			} else
				validate(id, value, ad);
		}

		Type type = Type.valueOf(ad.getType());
		if (values == null)
			conf.put(id, type.convert(value));
		else if (ad.getCardinality() > 0)
			conf.put(id, type.convert(values));
		else
			conf.put(id, type.convertToVector(values));
	}

	private static void handleDefaultValues(AttributeDefinition ad,
			Map<String, Object> conf, boolean required) throws Exception {
		String id = ad.getID();
		String[] defaultValues = ad.getDefaultValue();
		if (defaultValues != null) {
			Type type = Type.valueOf(ad.getType());
			int cardinality = ad.getCardinality();
			if (cardinality == 0)
				conf.put(id, type.convert(defaultValues[0]));
			else if (cardinality > 0)
				conf.put(id, type.convert(defaultValues));
			else
				conf.put(id, type.convertToVector(defaultValues));
		} else if (required)
			throw new Exception(StrUtil.join("Property[", id, "] is required"));
	}

	private static String[] split(String value) {
		ArrayList<String> list = new ArrayList<String>();
		StringBuilder builder = StringBuilder.get();
		try {
			int n = value.length();
			boolean filter = true;
			for (int i = 0; i < n; ++i) {
				char c = value.charAt(i);
				if (filter) {
					if (Character.isWhitespace(c))
						continue;

					filter = false;
				}

				if (c == '\\') {
					if (++i >= n)
						break;

					builder.append(value.charAt(i));
				} else if (c != ',')
					builder.append(c);
				else {
					addString(list, builder);
					filter = true;
				}
			}

			addString(list, builder);

			return list.toArray(new String[list.size()]);
		} finally {
			builder.close();
		}
	}

	private static void addString(ArrayList<String> list, StringBuilder builder) {
		int j = builder.length() - 1;
		while (j >= 0 && Character.isWhitespace(builder.charAt(j)))
			--j;

		if (j >= 0)
			list.add(builder.substring(0, ++j));
		builder.setLength(0);
	}

	private static void makeStringTo(StringBuilder builder, String[] values) {
		builder.append(values[0]);
		int n = values.length;
		for (int i = 1; i < n; ++i)
			builder.append(',').append(values[i]);
	}

	private static void validate(String id, String value, String[] optionValues)
			throws Exception {
		for (String optionValue : optionValues) {
			if (optionValue.equals(value))
				return;
		}

		StringBuilder builder = StringBuilder.get();
		try {
			builder.append("Illegal Property[").append(id).append('=')
					.append(value).append("]: {");
			makeStringTo(builder, optionValues);
			builder.append('}');
			throw new Exception(builder.toString());
		} finally {
			builder.close();
		}
	}

	private static void validate(String id, String value, AttributeDefinition ad)
			throws Exception {
		String message = ad.validate(value);
		if (message != null && message.length() > 0)
			throw new Exception(StrUtil.join("Error Property[", id, "]: ",
					message));
	}
}
