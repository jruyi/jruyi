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

package org.jruyi.io.buffer

import org.jruyi.common.StringBuilder
import org.jruyi.io.*
import spock.lang.Specification

class BufferSpec extends Specification {

	def "minimum unit capacity should be 8 bytes"() {
		given: "a buffer with unit capacity specified to 7"
		def bf = new BufferFactory()
		bf.activate([unitCapacity: 7])

		expect: "unit capacity is actually 8"
		bf.getUnit().capacity() == 8
		bf.getUnit(7).capacity() == 8
	}

	def "write a short array then read it, should be same"() {
		given: "3 buffers with unit capacity being 9, 9 and 32 respectively"
		def bf = new BufferFactory()
		bf.activate([unitCapacity: 9])
		def buf = bf.create()
		def buf2 = bf.create()

		bf.modified([unitCapacity: 32])
		def buf3 = bf.create()

		when: "write 8 shorts to a buffer with two 9-byte units"
		buf.write(array, ShortArrayCodec.bigEndian())
		then:
		buf.get(0, ShortArrayCodec.bigEndian()) == array
		buf.get(5, 8, ShortArrayCodec.bigEndian()) == array52
		buf.get(0, ShortArrayCodec.littleEndian()) == arrayR
		buf.read(ShortArrayCodec.bigEndian()) == array

		when: "write 16 bytes to a buffer with two 9-byte units"
		buf2.write(barray, Codec.byteArray())
		then:
		buf2.get(0, ShortArrayCodec.bigEndian()) == array
		buf2.get(5, 8, ShortArrayCodec.bigEndian()) == array52
		buf.get(0, ShortArrayCodec.littleEndian()) == arrayR
		buf2.read(ShortArrayCodec.bigEndian()) == array

		when: "write 16 bytes to a buffer with a single 32-byte unit"
		buf3.write(barray, Codec.byteArray())
		then:
		buf3.get(0, ShortArrayCodec.bigEndian()) == array
		buf3.get(5, 8, ShortArrayCodec.bigEndian()) == array52
		buf.get(0, ShortArrayCodec.littleEndian()) == arrayR
		buf3.read(ShortArrayCodec.bigEndian()) == array

		where:
		array = [0x0123, 0x4567, 0xfedc, 0xba98, 0xabcd, 0xef98, 0x1032, 0x5476] as short[]
		barray = [0x01, 0x23, 0x45, 0x67, 0xfe, 0xdc, 0xba, 0x98, 0xab, 0xcd, 0xef, 0x98, 0x10, 0x32, 0x54, 0x76] as byte[]
		array52 = [0xdcba, 0x98ab, 0xcdef, 0x9810] as short[]
		arrayR = [0x2301, 0x6745, 0xdcfe, 0x98ba, 0xcdab, 0x98ef, 0x3210, 0x7654] as short[]
	}

	def "write an int array then read it, should be same"() {
		given: "3 buffers with unit capacity being 10, 10 and 32 respectively"
		def bf = new BufferFactory()
		bf.activate([unitCapacity: 10])
		def buf = bf.create()
		def buf2 = bf.create()

		bf.modified([unitCapacity: 32])
		def buf3 = bf.create()

		when: "write 4 ints to a buffer with two 10-byte units"
		buf.write(array, IntArrayCodec.bigEndian())
		then:
		buf.get(0, IntArrayCodec.bigEndian()) == array
		buf.get(5, 8, IntArrayCodec.bigEndian()) == array52
		buf.get(0, IntArrayCodec.littleEndian()) == arrayR
		buf.read(IntArrayCodec.bigEndian()) == array

		when: "write 16 bytes to a buffer with two 10-byte units"
		buf2.write(barray, Codec.byteArray())
		then:
		buf2.get(0, IntArrayCodec.bigEndian()) == array
		buf2.get(5, 8, IntArrayCodec.bigEndian()) == array52
		buf.get(0, IntArrayCodec.littleEndian()) == arrayR
		buf2.read(IntArrayCodec.bigEndian()) == array

		when: "write 16 bytes to a buffer with a single 32-byte unit"
		buf3.write(barray, Codec.byteArray())
		then:
		buf3.get(0, IntArrayCodec.bigEndian()) == array
		buf3.get(5, 8, IntArrayCodec.bigEndian()) == array52
		buf.get(0, IntArrayCodec.littleEndian()) == arrayR
		buf3.read(IntArrayCodec.bigEndian()) == array

		where:
		array = [0x01234567, 0xfedcba98, 0xabcdef98, 0x10325476] as int[]
		barray = [0x01, 0x23, 0x45, 0x67, 0xfe, 0xdc, 0xba, 0x98, 0xab, 0xcd, 0xef, 0x98, 0x10, 0x32, 0x54, 0x76] as byte[]
		array52 = [0xdcba98ab, 0xcdef9810] as int[]
		arrayR = [0x67452301, 0x98badcfe, 0x98efcdab, 0x76543210] as int[]
	}

	def "write a long array then read it, should be same"() {
		given: "3 buffers with unit capacity being 12, 12 and 32 respectively"
		def bf = new BufferFactory()
		bf.activate([unitCapacity: 12])
		def buf = bf.create()
		def buf2 = bf.create()

		bf.modified([unitCapacity: 32])
		def buf3 = bf.create()

		when: "write 3 longs to a buffer with three 12-byte units"
		buf.write(array, LongArrayCodec.bigEndian())
		then:
		buf.get(0, LongArrayCodec.bigEndian()) == array
		buf.get(5, 16, LongArrayCodec.bigEndian()) == array52
		buf.get(0, LongArrayCodec.littleEndian()) == arrayR
		buf.read(LongArrayCodec.bigEndian()) == array

		when: "write 24 bytes to a buffer with two 12-byte units"
		buf2.write(barray, Codec.byteArray())
		then:
		buf2.get(0, LongArrayCodec.bigEndian()) == array
		buf2.get(5, 16, LongArrayCodec.bigEndian()) == array52
		buf.get(0, LongArrayCodec.littleEndian()) == arrayR
		buf2.read(LongArrayCodec.bigEndian()) == array

		when: "write 24 bytes to a buffer with a single 32-byte unit"
		buf3.write(barray, Codec.byteArray())
		then:
		buf3.get(0, LongArrayCodec.bigEndian()) == array
		buf3.get(5, 16, LongArrayCodec.bigEndian()) == array52
		buf.get(0, LongArrayCodec.littleEndian()) == arrayR
		buf3.read(LongArrayCodec.bigEndian()) == array

		where:
		array = [0x0123456789abcdefL, 0xfedcba9876543210L, 0xabcdef9876543210L] as long[]
		barray = [0x01, 0x23, 0x45, 0x67, 0x89, 0xab, 0xcd, 0xef, 0xfe, 0xdc, 0xba, 0x98, 0x76, 0x54, 0x32, 0x10, 0xab, 0xcd, 0xef, 0x98, 0x76, 0x54, 0x32, 0x10] as byte[]
		array52 = [0xabcdeffedcba9876, 0x543210abcdef9876] as long[]
		arrayR = [0xefcdab8967452301L, 0x1032547698badcfeL, 0x1032547698efcdabL] as long[]
	}

	def "write a float array then read it, should be same"() {
		given: "2 buffers with unit capacity being 9 and 32 respectively"
		def bf = new BufferFactory()
		bf.activate([unitCapacity: 9])
		def buf = bf.create()

		bf.modified([unitCapacity: 32])
		def buf2 = bf.create()

		when: "write 4 floats to a buffer with two 9-byte units"
		buf.write(array, FloatArrayCodec.bigEndian())
		then:
		buf.get(0, FloatArrayCodec.bigEndian()) == array
		buf.read(FloatArrayCodec.bigEndian()) == array

		when: "write 4 floats to a buffer with a single 32-byte unit"
		buf2.write(array, FloatArrayCodec.littleEndian())
		then:
		buf2.get(0, FloatArrayCodec.littleEndian()) == array
		buf2.read(FloatArrayCodec.littleEndian()) == array

		where:
		array = [3.1415, 43.234, 592.32, 7118.23] as float[]
	}

	def "write a double array then read it, should be same"() {
		given: "2 buffers with unit capacity being 13 and 32 respectively"
		def bf = new BufferFactory()
		bf.activate([unitCapacity: 13])
		def buf = bf.create()

		bf.modified([unitCapacity: 32])
		def buf2 = bf.create()

		when: "write 4 doubles to a buffer with four 13-byte units"
		buf.write(array, DoubleArrayCodec.bigEndian())
		then:
		buf.get(0, DoubleArrayCodec.bigEndian()) == array
		buf.read(DoubleArrayCodec.bigEndian()) == array

		when: "write 4 doubles to a buffer with a single 32-byte unit"
		buf2.write(array, DoubleArrayCodec.littleEndian())
		then:
		buf2.get(0, DoubleArrayCodec.littleEndian()) == array
		buf2.read(DoubleArrayCodec.littleEndian()) == array

		where:
		array = [31.415, 432.34, 5923.2, 71189.23] as double[]
	}

	def "write a byte array then read it, should be same"() {
		def bf = new BufferFactory()
		bf.activate([unitCapacity: 13])
		def buf = bf.create()
		def bytes = createBytes(50)
		def length = bytes.length

		when: "write 50 bytes"
		buf.write(bytes, Codec.byteArray());
		then:
		buf.position() == 0
		buf.size() == length

		when: "make a hex dump"
		def hexDump1 = StringBuilder.get().appendHexDump(bytes).toStringAndClose()
		def hexDump2 = StringBuilder.get().append(buf).toStringAndClose()
		then:
		hexDump1 == hexDump2

		when: "read 50 bytes"
		def bytes2 = buf.read(50, Codec.byteArray())
		then:
		bytes == bytes2
		buf.remaining() == 0

		when: "get bytes starting at 7"
		def bytes3 = Arrays.copyOfRange(bytes, 7, 50)
		def bytes4 = buf.get(7, Codec.byteArray())
		then:
		bytes3 == bytes4

		when: "rewind buffer, prepend 50 bytes, then read first 50 bytes"
		buf.rewind()
		buf.prepend(bytes, Codec.byteArray())
		def bytes5 = buf.read(50, Codec.byteArray())
		then:
		bytes == bytes5
	}

	private def createBytes(int length) {
		byte[] bytes = new byte[length];
		for (int i = 0; i < length; ++i)
			bytes[i] = (byte) i;
		return bytes;
	}
}
