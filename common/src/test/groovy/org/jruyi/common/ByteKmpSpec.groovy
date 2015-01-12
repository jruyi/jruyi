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

package org.jruyi.common

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class ByteKmpSpec extends Specification {

	@Shared
			s_word = "1231234".bytes
	@Shared
			s_text = "1231234or2a38D241231234203a8i9%iL1231234".bytes
	@Shared
			s_emptyWord = new byte[0]
	@Shared
			kmp = new ByteKmp(s_word)
	@Shared
			emptyKmp = new ByteKmp(s_emptyWord)

	@Unroll
	def "should return #index, find #word in #bytes starting at #begin ending at #end"() {
		expect:
		kmp.findIn(bytes, begin, end - begin) == index

		where:
		word = s_word
		bytes = s_text

		begin | end           || index
		0     | s_text.length || 0
		1     | s_text.length || 16
		1     | 22            || -1
		23    | s_text.length || 33
		0     | s_text.length || 0
	}

	@Unroll
	def "should return 0, find #word in #bytes"() {
		expect:
		emptyKmp.findIn(bytes, 0, s_text.length) == 0

		where:
		word = s_emptyWord
		bytes = s_text
	}

	@Unroll
	def "should return #index, rfind #word in #bytes starting at #begin ending at #end"() {
		expect:
		kmp.rfindIn(bytes, begin, end - begin) == index

		where:
		word = s_word
		bytes = s_text

		begin | end               || index
		0     | s_text.length     || 33
		1     | s_text.length - 2 || 16
		1     | 22                || -1
		0     | 22                || 0
	}

	@Unroll
	def "should return #index, rfind #word in #bytes"() {
		expect:
		emptyKmp.rfindIn(bytes, 0, s_text.length) == index

		where:
		word = s_emptyWord
		bytes = s_text
		index = s_text.length
	}
}