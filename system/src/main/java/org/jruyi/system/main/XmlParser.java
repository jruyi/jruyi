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
package org.jruyi.system.main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class wraps the {@code SAXParser}. It provides a way to handle the XML
 * element via {@link IElementHandler} when parsing. For each element to be
 * handled, its handler instance must be mapped to its name in a {@code Map}
 * passed to those {@code parse} methods.
 * 
 * @see IElementHandler
 * @see DefaultElementHandler
 */
public final class XmlParser {

	private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
	private static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
	private final SAXParser m_saxParser;

	static final class Stack {

		private int[] m_stack;
		private int m_size;

		Stack() {
			this(3);
		}

		Stack(int capacity) {
			m_stack = new int[capacity];
		}

		void clear() {
			m_size = 0;
		}

		boolean isEmpty() {
			return m_size <= 0;
		}

		void push(int i) {
			final int minCapacity = ++m_size;
			int[] stack = m_stack;
			if (minCapacity > stack.length) {
				int newCapacity = (stack.length * 3) / 2 + 1;
				if (newCapacity < 0)
					newCapacity = Integer.MAX_VALUE;
				else if (minCapacity > newCapacity)
					newCapacity = minCapacity;

				int[] newStack = new int[newCapacity];
				System.arraycopy(stack, 0, newStack, 0, m_size);
				stack = newStack;
				m_stack = stack;
			}

			stack[minCapacity - 1] = i;
		}

		int pop() {
			return m_stack[--m_size];
		}

	}

	static final class Attributes implements org.xml.sax.Attributes {

		private final org.xml.sax.Attributes m_attributes;
		private final Map<String, String> m_properties;
		private final StringBuilder m_builder;
		private final Stack m_stack;

		Attributes(org.xml.sax.Attributes attributes) {
			this(attributes, null);
		}

		Attributes(org.xml.sax.Attributes attributes,
				Map<String, String> properties) {
			m_attributes = attributes;
			m_properties = properties;
			m_builder = new StringBuilder(64);
			m_stack = new Stack();
		}

		@Override
		public int getIndex(String uri, String localName) {
			return m_attributes.getIndex(uri, localName);
		}

		@Override
		public int getIndex(String qName) {
			return m_attributes.getIndex(qName);
		}

		@Override
		public int getLength() {
			return m_attributes.getLength();
		}

		@Override
		public String getLocalName(int index) {
			return m_attributes.getLocalName(index);
		}

		@Override
		public String getQName(int index) {
			return m_attributes.getQName(index);
		}

		@Override
		public String getType(int index) {
			return m_attributes.getType(index);
		}

		@Override
		public String getType(String uri, String localName) {
			return m_attributes.getType(uri, localName);
		}

		@Override
		public String getType(String qName) {
			return m_attributes.getType(qName);
		}

		@Override
		public String getURI(int index) {
			return m_attributes.getURI(index);
		}

		@Override
		public String getValue(int index) {
			return filterProps(m_attributes.getValue(index));
		}

		@Override
		public String getValue(String uri, String localName) {
			return filterProps(m_attributes.getValue(uri, localName));
		}

		@Override
		public String getValue(String qName) {
			return filterProps(m_attributes.getValue(qName));
		}

		private String filterProps(String target) {
			if (target == null)
				return null;

			if (target.length() < 2)
				return target;

			final Map<String, String> properties = m_properties;
			final StringBuilder builder = m_builder;
			final Stack stack = m_stack;
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
						int index = stack.pop();
						propValue = getPropValue(builder.substring(index + 2),
								properties);
						if (propValue != null) {
							builder.setLength(index);
							builder.append(propValue);
							continue;
						}
					}
				}

				builder.append(c);
			}
			stack.clear();

			if (propValue != null || builder.length() != j)
				target = builder.toString();

			builder.setLength(0);

			return target;
		}
	}

	static class XMLHandler extends DefaultHandler {

		private final StringBuilder m_builder;
		private final Map<String, IElementHandler> m_handlers;
		private IElementHandler m_validHandler;

		XMLHandler(Map<String, IElementHandler> handlers) {
			m_builder = new StringBuilder(128);
			m_handlers = handlers;
		}

		@Override
		public final void startElement(String uri, String localName,
				String qName, org.xml.sax.Attributes attributes)
				throws SAXException {
			final IElementHandler handler = m_handlers.get(qName);
			if (handler != null) {
				handler.start(attributes);
				m_validHandler = handler;
			}
		}

		@Override
		public final void characters(char[] ch, int start, int length)
				throws SAXException {
			if (m_validHandler != null)
				m_builder.append(ch, start, length);
		}

		@Override
		public final void endElement(String uri, String localName, String qName)
				throws SAXException {
			final IElementHandler handler = m_handlers.get(qName);
			if (handler != null) {
				if (m_validHandler == handler) {
					handler.setText(m_builder.toString());
					m_builder.setLength(0);
				}

				handler.end();
			}
			m_validHandler = null;
		}

		@Override
		public final void error(SAXParseException e) throws SAXException {
			throw e;
		}

		@Override
		public final void fatalError(SAXParseException e) throws SAXException {
			throw e;
		}

		@Override
		public final void warning(SAXParseException e) throws SAXException {
			throw e;
		}
	}

	static class XMLFilterHandler extends DefaultHandler {

		private final Map<String, IElementHandler> m_handlers;
		private final Map<String, String> m_properties;
		private final StringBuilder m_builder;
		private final Stack m_stack;
		private IElementHandler m_validHandler;

		XMLFilterHandler(Map<String, IElementHandler> handlers) {
			this(handlers, null);
		}

		XMLFilterHandler(Map<String, IElementHandler> handlers,
				Map<String, String> properties) {
			m_handlers = handlers;
			m_properties = properties;
			m_builder = new StringBuilder(128);
			m_stack = new Stack();
		}

		@Override
		public final void startElement(String uri, String localName,
				String qName, org.xml.sax.Attributes attributes)
				throws SAXException {
			final IElementHandler handler = m_handlers.get(qName);
			if (handler != null) {
				handler.start(new Attributes(attributes, m_properties));
				m_validHandler = handler;
			}
		}

		@Override
		public final void characters(char[] ch, int start, int length)
				throws SAXException {
			if (m_validHandler != null)
				filterToBuilder(ch, start, length);
		}

		@Override
		public final void endElement(String uri, String localName, String qName)
				throws SAXException {
			final IElementHandler handler = m_handlers.get(qName);
			if (handler != null) {
				if (m_validHandler == handler) {
					handler.setText(m_builder.toString());
					m_builder.setLength(0);
					m_stack.clear();
				}

				handler.end();
			}
			m_validHandler = null;
		}

		@Override
		public final void error(SAXParseException e) throws SAXException {
			throw e;
		}

		@Override
		public final void fatalError(SAXParseException e) throws SAXException {
			throw e;
		}

		@Override
		public final void warning(SAXParseException e) throws SAXException {
			throw e;
		}

		private void filterToBuilder(char[] target, int start, int length) {
			if (target == null)
				return;

			final StringBuilder builder = m_builder;
			final Map<String, String> properties = m_properties;
			final Stack stack = m_stack;

			final int end = start + length;
			for (; start < end; ++start) {
				char c = target[start];
				switch (c) {
				case '\\':
					if (++start < end)
						c = target[start];
					break;
				case '$':
					builder.append(c);
					if (++start < end && (c = target[start]) == '{')
						stack.push(builder.length() - 1);
					break;
				case '}':
					if (!stack.isEmpty()) {
						int index = stack.pop();
						String propValue = getPropValue(
								builder.substring(index + 2), properties);
						if (propValue != null) {
							builder.setLength(index);
							builder.append(propValue);
							continue;
						}
					}
				}

				builder.append(c);
			}
		}

	}

	private XmlParser(SAXParser saxParser) {
		m_saxParser = saxParser;
	}

	static String getPropValue(String name, Map<String, String> properties) {
		final String value;
		if (properties != null && properties.size() > 0
				&& (value = properties.get(name)) != null)
			return value;

		return System.getProperty(name);
	}

	/**
	 * Parse the XML document loaded from the specified {@code file}. The
	 * corresponding handler in the specified {@code handlers} will be executed
	 * when the mapped name equals to a tag name in XML during parsing. If the
	 * specified {@code filtering} is true, then the nested system property
	 * substitution will be applied. The syntax is {@literal $ <property>} .
	 * 
	 * @param file
	 *            the file from which XML document is loaded
	 * @param handlers
	 *            handlers to be executed on corresponding tags are parsed
	 * @param filtering
	 *            indicate whether to apply system property substitution
	 * @throws IllegalArgumentException
	 *             if the {@code File} object is null
	 * @throws SAXException
	 *             if any SAX errors occur during processing
	 * @throws IOException
	 *             if any IO errors occur
	 */
	public void parse(File file, Map<String, IElementHandler> handlers,
			boolean filtering) throws SAXException, IOException {
		m_saxParser.parse(file, filtering ? new XMLFilterHandler(handlers)
				: new XMLHandler(handlers));
	}

	/**
	 * Parse the XML document loaded from the specified {@code is}. The
	 * corresponding handler in the specified {@code handlers} will be executed
	 * when the mapped name equals to a tag name in XML during parsing. If the
	 * specified {@code filtering} is true, then the nested system property
	 * substitution will be applied. The syntax is ${<property>}.
	 * 
	 * @param is
	 *            the input source from which XML document is loaded
	 * @param handlers
	 *            handlers to be executed on corresponding tags are parsed
	 * @param filtering
	 *            indicate whether to apply system property substitution
	 * @throws IllegalArgumentException
	 *             if the {@code InputSource} object is null
	 * @throws SAXException
	 *             if any SAX errors occur during processing
	 * @throws IOException
	 *             if any IO errors occur
	 */
	public void parse(InputSource is, Map<String, IElementHandler> handlers,
			boolean filtering) throws SAXException, IOException {
		m_saxParser.parse(is, filtering ? new XMLFilterHandler(handlers)
				: new XMLHandler(handlers));
	}

	/**
	 * Parse the XML document loaded from the specified {@code is}. The
	 * corresponding handler in the specified {@code handlers} will be executed
	 * when the mapped name equals to a tag name in XML during parsing. If the
	 * specified {@code filtering} is true, then the nested system property
	 * substitution will be applied. The syntax is ${<property>}.
	 * 
	 * @param is
	 *            the input stream from which XML document is loaded
	 * @param handlers
	 *            handlers to be executed on corresponding tags are parsed
	 * @param filtering
	 *            indicate whether to apply system property substitution
	 * @throws IllegalArgumentException
	 *             if the {@code InputStream} object is null
	 * @throws SAXException
	 *             if any SAX errors occur during processing
	 * @throws IOException
	 *             if any IO errors occur
	 */
	public void parse(InputStream is, Map<String, IElementHandler> handlers,
			boolean filtering) throws SAXException, IOException {
		m_saxParser.parse(is, filtering ? new XMLFilterHandler(handlers)
				: new XMLHandler(handlers));
	}

	/**
	 * Parse the XML document loaded from the specified {@code uri}. The
	 * corresponding handler in the specified {@code handlers} will be executed
	 * when the mapped name equals to a tag name in XML during parsing. If the
	 * specified {@code filtering} is true, then the nested system property
	 * substitution will be applied. The syntax is ${<property>}.
	 * 
	 * @param uri
	 *            the URI from which XML document is loaded
	 * @param handlers
	 *            handlers to be executed on corresponding tags are parsed
	 * @param filtering
	 *            indicate whether to apply system property substitution
	 * @throws IllegalArgumentException
	 *             if the given {@code uri} is null
	 * @throws SAXException
	 *             if any SAX errors occur during processing
	 * @throws IOException
	 *             if any IO errors occur
	 */
	public void parse(String uri, Map<String, IElementHandler> handlers,
			boolean filtering) throws SAXException, IOException {
		m_saxParser.parse(uri, filtering ? new XMLFilterHandler(handlers)
				: new XMLHandler(handlers));
	}

	/**
	 * Parse the XML document loaded from the specified {@code file}. The
	 * corresponding handler in the specified {@code handlers} will be executed
	 * when the mapped name equals to a tag name in XML during parsing. Any
	 * ${<property>} in the XML will be substituted if the property is in the
	 * specified {@code properties} or system properties. Nested substitution is
	 * supported.
	 * 
	 * @param file
	 *            the file from which XML document is loaded
	 * @param handlers
	 *            handlers to be executed on corresponding tags are parsed
	 * @param properties
	 *            properties to be substituted
	 * @throws IllegalArgumentException
	 *             if the given {@code file} is null
	 * @throws SAXException
	 *             if any SAX errors occur during processing
	 * @throws IOException
	 *             if any IO errors occur
	 */
	public void parse(File file, Map<String, IElementHandler> handlers,
			Map<String, String> properties)
			throws ParserConfigurationException, SAXException, IOException {
		m_saxParser.parse(file, properties == null ? new XMLFilterHandler(
				handlers) : new XMLFilterHandler(handlers, properties));
	}

	/**
	 * Parse the XML document loaded from the specified {@code is}. The
	 * corresponding handler in the specified {@code handlers} will be executed
	 * when the mapped name equals to a tag name in XML during parsing. Any
	 * ${<property>} in the XML will be substituted if the property is in the
	 * specified {@code properties} or system properties. Nested substitution is
	 * supported.
	 * 
	 * @param is
	 *            the input source from which XML document is loaded
	 * @param handlers
	 *            handlers to be executed on corresponding tags are parsed
	 * @param properties
	 *            properties to be substituted
	 * @throws IllegalArgumentException
	 *             if the given {@code is} is null
	 * @throws SAXException
	 *             if any SAX errors occur during processing
	 * @throws IOException
	 *             if any IO errors occur
	 */
	public void parse(InputSource is, Map<String, IElementHandler> handlers,
			Map<String, String> properties)
			throws ParserConfigurationException, SAXException, IOException {
		m_saxParser.parse(is, properties == null ? new XMLFilterHandler(
				handlers) : new XMLFilterHandler(handlers, properties));
	}

	/**
	 * Parse the XML document loaded from the specified {@code is}. The
	 * corresponding handler in the specified {@code handlers} will be executed
	 * when the mapped name equals to a tag name in XML during parsing. Any
	 * ${<property>} in the XML will be substituted if the property is in the
	 * specified {@code properties} or system properties. Nested substitution is
	 * supported.
	 * 
	 * @param is
	 *            the input stream from which XML document is loaded
	 * @param handlers
	 *            handlers to be executed on corresponding tags are parsed
	 * @param properties
	 *            properties to be substituted
	 * @throws IllegalArgumentException
	 *             if the given {@code is} is null
	 * @throws SAXException
	 *             if any SAX errors occur during processing
	 * @throws IOException
	 *             if any IO errors occur
	 */
	public void parse(InputStream is, Map<String, IElementHandler> handlers,
			Map<String, String> properties)
			throws ParserConfigurationException, SAXException, IOException {
		m_saxParser.parse(is, properties == null ? new XMLFilterHandler(
				handlers) : new XMLFilterHandler(handlers, properties));
	}

	/**
	 * Parse the XML document loaded from the specified {@code uri}. The
	 * corresponding handler in the specified {@code handlers} will be executed
	 * when the mapped name equals to a tag name in XML during parsing. Any
	 * ${<property>} in the XML will be substituted if the property is in the
	 * specified {@code properties} or system properties. Nested substitution is
	 * supported.
	 * 
	 * @param uri
	 *            the URI from which XML document is loaded
	 * @param handlers
	 *            handlers to be executed on corresponding tags are parsed
	 * @param properties
	 *            properties to be substituted
	 * @throws IllegalArgumentException
	 *             if the given {@code uri} is null
	 * @throws SAXException
	 *             if any SAX errors occur during processing
	 * @throws IOException
	 *             if any IO errors occur
	 */
	public void parse(String uri, Map<String, IElementHandler> handlers,
			Map<String, String> properties)
			throws ParserConfigurationException, SAXException, IOException {
		m_saxParser.parse(uri, properties == null ? new XMLFilterHandler(
				handlers) : new XMLFilterHandler(handlers, properties));
	}

	/**
	 * Create an instance of {@code XmlParser}.
	 * 
	 * @param validating
	 *            indicate whether to validate the document on parsing using a
	 *            validator as defined in the XML recommendation
	 * @return an {@code XmlParser} object
	 * @throws ParserConfigurationException
	 *             if a parser cannot be created which satisfies the requested
	 *             configuration
	 * @throws SAXException
	 *             if any SAX errors occur during processing
	 * @throws IOException
	 *             if any IO errors occur
	 */
	public static XmlParser getInstance(boolean validating)
			throws ParserConfigurationException, SAXException, IOException {
		final SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setValidating(validating);
		return new XmlParser(factory.newSAXParser());
	}

	/**
	 * Create an instance of {@code XmlParser}. The XML document will be
	 * validated on parsing using a validator created from the specified
	 * {@code schema}.
	 * 
	 * @param schema
	 *            the schema from which the validator is created
	 * @return an {@code XmlParser} object
	 * @throws ParserConfigurationException
	 *             if a parser cannot be created which satisfies the requested
	 *             configuration
	 * @throws SAXException
	 *             if any SAX errors occur during processing
	 * @throws IOException
	 *             if any IO errors occur
	 */
	public static XmlParser getInstance(InputStream schema)
			throws ParserConfigurationException, SAXException, IOException {
		final SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setValidating(true);

		final SAXParser parser = factory.newSAXParser();
		parser.setProperty(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
		parser.setProperty(JAXP_SCHEMA_SOURCE, schema);

		return new XmlParser(parser);
	}
}
