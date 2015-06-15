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

import java.nio.ByteBuffer
import java.nio.ByteOrder

class BufferSpec extends Specification {

	def "read/write int"() {
		def size = 4;
		def i = 0x12345678I
		def bytesLe = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN).putInt(i).array()
		def bytesBe = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN).putInt(i).array()
		def bf = new BufferFactory()
		bf.activate([unitCapacity: size * 6 - 1])
		def buf = bf.create()
		buf.reserveHead(size * 4)

		buf.write(bytesLe, Codec.byteArray())
		buf.write(bytesBe, Codec.byteArray())
		buf.write(i, IntCodec.littleEndian())
		buf.write(i, IntCodec.bigEndian())

		buf.prepend(bytesLe, Codec.byteArray())
		buf.prepend(bytesBe, Codec.byteArray())
		buf.prepend(i, IntCodec.littleEndian())
		buf.prepend(i, IntCodec.bigEndian())

		expect:
		buf.read(size, Codec.byteArray()) == bytesBe
		buf.read(size, Codec.byteArray()) == bytesLe
		buf.read(IntCodec.bigEndian()) == i
		buf.read(IntCodec.littleEndian()) == i

		buf.read(IntCodec.littleEndian()) == i
		buf.read(IntCodec.bigEndian()) == i
		buf.read(size, Codec.byteArray()) == bytesLe
		buf.read(size, Codec.byteArray()) == bytesBe

		buf.get(0, size, Codec.byteArray()) == bytesBe
		buf.get(size, size, Codec.byteArray()) == bytesLe
		buf.get(size * 2, IntCodec.bigEndian()) == i
		buf.get(size * 3, IntCodec.littleEndian()) == i
		buf.get(size * 4, IntCodec.littleEndian()) == i
		buf.get(size * 5, IntCodec.bigEndian()) == i
		buf.get(size * 6, size, Codec.byteArray()) == bytesLe
		buf.get(size * 7, size, Codec.byteArray()) == bytesBe
	}

	def "read/write long"() {
		def size = 8;
		def l = 0x1234567890abcdefI
		def bytesLe = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN).putLong(l).array()
		def bytesBe = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN).putLong(l).array()
		def bf = new BufferFactory()
		bf.activate([unitCapacity: size * 6 - 1])
		def buf = bf.create()
		buf.reserveHead(size * 4)

		buf.write(bytesLe, Codec.byteArray())
		buf.write(bytesBe, Codec.byteArray())
		buf.write(l, LongCodec.littleEndian())
		buf.write(l, LongCodec.bigEndian())

		buf.prepend(bytesLe, Codec.byteArray())
		buf.prepend(bytesBe, Codec.byteArray())
		buf.prepend(l, LongCodec.littleEndian())
		buf.prepend(l, LongCodec.bigEndian())

		expect:
		buf.read(size, Codec.byteArray()) == bytesBe
		buf.read(size, Codec.byteArray()) == bytesLe
		buf.read(LongCodec.bigEndian()) == l
		buf.read(LongCodec.littleEndian()) == l

		buf.read(LongCodec.littleEndian()) == l
		buf.read(LongCodec.bigEndian()) == l
		buf.read(size, Codec.byteArray()) == bytesLe
		buf.read(size, Codec.byteArray()) == bytesBe

		buf.get(0, size, Codec.byteArray()) == bytesBe
		buf.get(size, size, Codec.byteArray()) == bytesLe
		buf.get(size * 2, LongCodec.bigEndian()) == l
		buf.get(size * 3, LongCodec.littleEndian()) == l
		buf.get(size * 4, LongCodec.littleEndian()) == l
		buf.get(size * 5, LongCodec.bigEndian()) == l
		buf.get(size * 6, size, Codec.byteArray()) == bytesLe
		buf.get(size * 7, size, Codec.byteArray()) == bytesBe
	}

	def "read/write float"() {
		def size = 4;
		def f = 212324.123F
		def bytesLe = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN).putFloat(f).array()
		def bytesBe = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN).putFloat(f).array()
		def bf = new BufferFactory()
		bf.activate([unitCapacity: size * 6 - 1])
		def buf = bf.create()
		buf.reserveHead(size * 4)

		buf.write(bytesLe, Codec.byteArray())
		buf.write(bytesBe, Codec.byteArray())
		buf.write(f, FloatCodec.littleEndian())
		buf.write(f, FloatCodec.bigEndian())

		buf.prepend(bytesLe, Codec.byteArray())
		buf.prepend(bytesBe, Codec.byteArray())
		buf.prepend(f, FloatCodec.littleEndian())
		buf.prepend(f, FloatCodec.bigEndian())

		expect:
		buf.read(size, Codec.byteArray()) == bytesBe
		buf.read(size, Codec.byteArray()) == bytesLe
		buf.read(FloatCodec.bigEndian()) == f
		buf.read(FloatCodec.littleEndian()) == f

		buf.read(FloatCodec.littleEndian()) == f
		buf.read(FloatCodec.bigEndian()) == f
		buf.read(size, Codec.byteArray()) == bytesLe
		buf.read(size, Codec.byteArray()) == bytesBe

		buf.get(0, size, Codec.byteArray()) == bytesBe
		buf.get(size, size, Codec.byteArray()) == bytesLe
		buf.get(size * 2, FloatCodec.bigEndian()) == f
		buf.get(size * 3, FloatCodec.littleEndian()) == f
		buf.get(size * 4, FloatCodec.littleEndian()) == f
		buf.get(size * 5, FloatCodec.bigEndian()) == f
		buf.get(size * 6, size, Codec.byteArray()) == bytesLe
		buf.get(size * 7, size, Codec.byteArray()) == bytesBe
	}

	def "read/write double"() {
		def size = 8
		def d = 834955223.13323D
		def bytesLe = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN).putDouble(d).array()
		def bytesBe = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN).putDouble(d).array()
		def bf = new BufferFactory()
		bf.activate([unitCapacity: size * 6 - 1])
		def buf = bf.create()
		buf.reserveHead(size * 4)

		buf.write(bytesLe, Codec.byteArray())
		buf.write(bytesBe, Codec.byteArray())
		buf.write(d, DoubleCodec.littleEndian())
		buf.write(d, DoubleCodec.bigEndian())

		buf.prepend(bytesLe, Codec.byteArray())
		buf.prepend(bytesBe, Codec.byteArray())
		buf.prepend(d, DoubleCodec.littleEndian())
		buf.prepend(d, DoubleCodec.bigEndian())

		expect:
		buf.read(size, Codec.byteArray()) == bytesBe
		buf.read(size, Codec.byteArray()) == bytesLe
		buf.read(DoubleCodec.bigEndian()) == d
		buf.read(DoubleCodec.littleEndian()) == d

		buf.read(DoubleCodec.littleEndian()) == d
		buf.read(DoubleCodec.bigEndian()) == d
		buf.read(size, Codec.byteArray()) == bytesLe
		buf.read(size, Codec.byteArray()) == bytesBe

		buf.get(0, size, Codec.byteArray()) == bytesBe
		buf.get(size, size, Codec.byteArray()) == bytesLe
		buf.get(size * 2, DoubleCodec.bigEndian()) == d
		buf.get(size * 3, DoubleCodec.littleEndian()) == d
		buf.get(size * 4, DoubleCodec.littleEndian()) == d
		buf.get(size * 5, DoubleCodec.bigEndian()) == d
		buf.get(size * 6, size, Codec.byteArray()) == bytesLe
		buf.get(size * 7, size, Codec.byteArray()) == bytesBe
	}

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
		buf.write(bytes, Codec.byteArray())
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

	def "splitting a buffer into 3 pieces should not change the sequence"() {
		given: "a buffer with unitCapacity = 32 and size = 70"
		def bf = new BufferFactory()
		bf.activate([unitCapacity: 32])
		def buf = bf.create()
		def bytes1 = createBytes(33)
		def bytes2 = createBytes(13)
		def bytes3 = createBytes(24)
		buf.write(bytes1, Codec.byteArray()).write(bytes2, Codec.byteArray()).write(bytes3, Codec.byteArray())

		when: "split into 3 pieces: the first with 33 bytes, the second with 13 and the third with 24 bytes"
		def firstPiece = buf.split(33).read(Codec.byteArray())
		def secondPiece = buf.split(13).read(Codec.byteArray())
		def thirdPiece = buf.read(Codec.byteArray())
		then:
		firstPiece == bytes1
		secondPiece == bytes2
		thirdPiece == bytes3
	}

	private static def createBytes(int length) {
		byte[] bytes = new byte[length];
		for (int i = 0; i < length; ++i)
			bytes[i] = (byte) i;
		return bytes;
	}
}
