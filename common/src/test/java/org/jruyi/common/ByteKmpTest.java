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

import org.junit.Assert;
import org.junit.Test;

public class ByteKmpTest {

	private static final byte[] EMPTY = new byte[0];
	private static final byte[] m_word = "1231234".getBytes();
	private static final byte[] m_text = "1231234owieur238419824123123420398iooidjoei1231234"
			.getBytes();

	@Test
	public void test_findIn() {
		ByteKmp kmp = new ByteKmp(m_word);
		Assert.assertEquals(kmp.findIn(m_text, 0, m_text.length), 0);
		Assert.assertEquals(kmp.findIn(m_text, 1, m_text.length - 1), 22);
		Assert.assertEquals(kmp.findIn(m_text, 1, 27), -1);
		Assert.assertEquals(kmp.findIn(m_text, 23, m_text.length - 23), 43);

		kmp = new ByteKmp(EMPTY);
		Assert.assertEquals(kmp.findIn(m_text, 0, m_text.length), 0);
	}

	@Test
	public void test_rfindIn() {
		ByteKmp kmp = new ByteKmp(m_word);
		Assert.assertEquals(kmp.rfindIn(m_text, 0, m_text.length), 43);
		Assert.assertEquals(kmp.rfindIn(m_text, 1, m_text.length - 2), 22);
		Assert.assertEquals(kmp.rfindIn(m_text, 1, 27), -1);
		Assert.assertEquals(kmp.rfindIn(m_text, 0, 27), 0);

		kmp = new ByteKmp(EMPTY);
		Assert.assertEquals(kmp.rfindIn(m_text, 0, m_text.length),
				m_text.length);
	}
}
