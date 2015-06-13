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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Defines methods that all element handlers must implement. All the three
 * methods {@code start}, {@code setText} and {@code end} are callback methods.
 * They will be invoked when the element which this handler maps to is
 * processed.
 * 
 * @see DefaultElementHandler
 * @see XmlParser
 */
public interface IElementHandler {

	/**
	 * Receive notification of the beginning of the element to be handled by
	 * this handler.
	 * 
	 * @param attributes
	 *            the attributes attached to the element handled by this
	 *            handler. If there are no attributes, it shall be an empty
	 *            {@code Attributes} object. The value of this object after
	 *            {@code start} returns is undefined.
	 * @throws SAXException
	 *             any SAX exception, possibly wrapping another exception
	 */
	void start(Attributes attributes) throws SAXException;

	/**
	 * Receive notification of the text value of the element to be handled by
	 * this handler.
	 * 
	 * @param text
	 *            the text value with leading and trailing whitespace omitted
	 *            and properties substituted.
	 * 
	 * @throws SAXException
	 *             any SAX exception, possibly wrapping another exception.
	 */
	void setText(String text) throws SAXException;

	/**
	 * Receive notification of the end of the element to be handled by this
	 * handler.
	 * 
	 * @throws SAXException
	 *             any SAX exception, possibly wrapping another exception.
	 */
	void end() throws SAXException;
}
