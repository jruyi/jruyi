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
package org.jruyi.io.buffer;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.jruyi.common.ByteKmp;
import org.jruyi.common.StringBuilder;
import org.jruyi.io.Codec;
import org.jruyi.io.DoubleCodec;
import org.jruyi.io.FloatCodec;
import org.jruyi.io.IBuffer;
import org.jruyi.io.IntCodec;
import org.jruyi.io.LongCodec;
import org.jruyi.io.ShortCodec;
import org.jruyi.io.StringCodec;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class BufferTest {

	private static final String UNIT_CAPACITY = "unitCapacity";
	private static BufferFactory s_factory;
	private static Map<String, Object> s_props;

	@BeforeClass
	public static void setUp() {
		System.out.println("Testing Buffer...");
		s_factory = new BufferFactory();
		s_props = new HashMap<>();
		s_props.put(UNIT_CAPACITY, 8192);
		s_factory.modified(s_props);
	}

	@AfterClass
	public static void tearDown() {
	}

	@Test
	public void test_writeReadBytes() {
		byte[] bytes = createBytes();
		StringBuilder builder = StringBuilder.get();
		String hexDump1;
		String hexDump2;
		try {
			builder.appendHexDump(bytes);

			hexDump1 = builder.toString();
			builder.setLength(0);

			final int n = bytes.length;
			for (int i = 1; i < n + 11; i += 10) {
				BufferFactory factory = initializeFactory(i);
				final IBuffer buffer = factory.create();
				buffer.write(bytes, Codec.byteArray());

				Assert.assertEquals(0, buffer.position());
				Assert.assertEquals(n, buffer.size());

				buffer.dump(builder);
				hexDump2 = builder.toString();

				builder.setLength(0);

				Assert.assertEquals(hexDump1, hexDump2);

				byte[] bytes2 = buffer.read(n, Codec.byteArray());
				Assert.assertArrayEquals(bytes, bytes2);

				int index = new Random().nextInt(n);
				byte[] bytes3 = Arrays.copyOfRange(bytes, index, n);
				byte[] bytes4 = buffer.get(index, Codec.byteArray());
				Assert.assertArrayEquals(bytes3, bytes4);

				Assert.assertEquals(0, buffer.remaining());

				buffer.rewind();
				buffer.prepend(bytes, Codec.byteArray());
				byte[] bytes5 = buffer.read(n, Codec.byteArray());
				Assert.assertArrayEquals(bytes, bytes5);
			}
		} finally {
			builder.close();
		}
	}

	@Test
	public void test_writeReadFloat() {
		final byte[] bytes = createBytes();
		final float v = 3.14F;
		int t = Float.floatToIntBits(v);
		final byte[] r1 = { (byte) (t >> 24), (byte) (t >> 16), (byte) (t >> 8), (byte) t };
		final byte[] r2 = { (byte) t, (byte) (t >> 8), (byte) (t >> 16), (byte) (t >> 24) };
		final int n = bytes.length;
		for (int i = 1; i < n + 10; ++i) {
			BufferFactory factory = initializeFactory(i);
			IBuffer buffer = factory.create();
			buffer.write(bytes, Codec.byteArray());

			buffer.write(v, FloatCodec.bigEndian());
			buffer.write(v, FloatCodec.littleEndian());

			Assert.assertEquals(n + 8, buffer.size());

			buffer.skip(n);

			float read = buffer.read(FloatCodec.bigEndian());
			Assert.assertEquals(v, read, 0);
			read = buffer.get(n, FloatCodec.bigEndian());
			Assert.assertEquals(v, read, 0);

			read = buffer.read(FloatCodec.littleEndian());
			Assert.assertEquals(v, read, 0);
			read = buffer.get(n + 4, FloatCodec.littleEndian());
			Assert.assertEquals(v, read, 0);

			buffer.set(n, v, FloatCodec.littleEndian());
			read = buffer.get(n, FloatCodec.littleEndian());
			Assert.assertEquals(v, read, 0);

			buffer.set(n, v, FloatCodec.bigEndian());
			read = buffer.get(n, FloatCodec.bigEndian());
			Assert.assertEquals(v, read, 0);

			Assert.assertEquals(buffer.remaining(), 0);

			buffer.write(v, FloatCodec.bigEndian());
			byte[] result = buffer.read(Codec.byteArray());
			Assert.assertArrayEquals(r1, result);

			buffer.write(v, FloatCodec.littleEndian());
			result = buffer.read(Codec.byteArray());
			Assert.assertArrayEquals(r2, result);

			// prepend
			buffer.rewind();
			buffer.write(bytes, Codec.byteArray());

			buffer.prepend(v, FloatCodec.bigEndian());
			buffer.prepend(v, FloatCodec.littleEndian());
			read = buffer.get(0, FloatCodec.littleEndian());
			Assert.assertEquals(v, read, 0);
			read = buffer.get(4, FloatCodec.bigEndian());
			Assert.assertEquals(v, read, 0);
		}
	}

	@Test
	public void test_writeReadDouble() {
		final byte[] bytes = createBytes();
		final double v = 3.14F;
		long t = Double.doubleToLongBits(v);
		final byte[] r1 = { (byte) (t >> 56), (byte) (t >> 48), (byte) (t >> 40), (byte) (t >> 32), (byte) (t >> 24),
				(byte) (t >> 16), (byte) (t >> 8), (byte) t };
		final byte[] r2 = { (byte) t, (byte) (t >> 8), (byte) (t >> 16), (byte) (t >> 24), (byte) (t >> 32),
				(byte) (t >> 40), (byte) (t >> 48), (byte) (t >> 56) };
		final int n = bytes.length;
		for (int i = 1; i < n + 10; ++i) {
			BufferFactory factory = initializeFactory(i);
			IBuffer buffer = factory.create();
			buffer.write(bytes, Codec.byteArray());

			buffer.write(v, DoubleCodec.bigEndian());
			buffer.write(v, DoubleCodec.littleEndian());

			Assert.assertEquals(n + 16, buffer.size());

			buffer.skip(n);

			double read = buffer.read(DoubleCodec.bigEndian());
			Assert.assertEquals(v, read, 0);
			read = buffer.get(n, DoubleCodec.bigEndian());
			Assert.assertEquals(v, read, 0);

			read = buffer.read(DoubleCodec.littleEndian());
			Assert.assertEquals(v, read, 0);
			read = buffer.get(n + 8, DoubleCodec.littleEndian());
			Assert.assertEquals(v, read, 0);

			buffer.set(n, v, DoubleCodec.littleEndian());
			read = buffer.get(n, DoubleCodec.littleEndian());
			Assert.assertEquals(v, read, 0);

			buffer.set(n, v, DoubleCodec.bigEndian());
			read = buffer.get(n, DoubleCodec.bigEndian());
			Assert.assertEquals(v, read, 0);

			Assert.assertEquals(buffer.remaining(), 0);

			buffer.write(v, DoubleCodec.bigEndian());
			byte[] result = buffer.read(Codec.byteArray());
			Assert.assertArrayEquals(r1, result);

			buffer.write(v, DoubleCodec.littleEndian());
			result = buffer.read(Codec.byteArray());
			Assert.assertArrayEquals(r2, result);

			// prepend
			buffer.rewind();
			buffer.write(bytes, Codec.byteArray());

			buffer.prepend(v, DoubleCodec.bigEndian());
			buffer.prepend(v, DoubleCodec.littleEndian());
			read = buffer.get(0, DoubleCodec.littleEndian());
			Assert.assertEquals(v, read, 0);
			read = buffer.get(8, DoubleCodec.bigEndian());
			Assert.assertEquals(v, read, 0);
		}
	}

	@Test
	public void test_writeReadInt() {
		final byte[] bytes = createBytes();
		final int v = new Random().nextInt();
		final int size = sizeOfVarint(v);
		final int t = 0x12345678;
		final byte[] r1 = { 0x12, 0x34, 0x56, 0x78 };
		final byte[] r2 = { 0x78, 0x56, 0x34, 0x12 };
		final byte[] r3 = { (byte) 0xF8, (byte) 0xAC, (byte) 0xD1, (byte) 0x91, 0x01 };
		final int n = bytes.length;
		for (int i = 1; i < n + 10 + size; ++i) {
			BufferFactory factory = initializeFactory(i);
			IBuffer buffer = factory.create();
			buffer.write(bytes, Codec.byteArray());

			buffer.write(v, IntCodec.bigEndian());
			buffer.write(v, IntCodec.littleEndian());
			buffer.write(v, IntCodec.varint());

			Assert.assertEquals(n + 8 + size, buffer.size());

			buffer.skip(n);

			int read = buffer.read(IntCodec.bigEndian());
			Assert.assertEquals(v, read);
			read = buffer.get(n, IntCodec.bigEndian());
			Assert.assertEquals(v, read);

			read = buffer.read(IntCodec.littleEndian());
			Assert.assertEquals(v, read);
			read = buffer.get(n + 4, IntCodec.littleEndian());
			Assert.assertEquals(v, read);

			read = buffer.read(IntCodec.varint());
			Assert.assertEquals(v, read);
			read = buffer.get(n + 8, IntCodec.varint());
			Assert.assertEquals(v, read);

			// set/get
			buffer.set(n, v, IntCodec.littleEndian());
			read = buffer.get(n, IntCodec.littleEndian());
			Assert.assertEquals(v, read);

			buffer.set(n, v, IntCodec.varint());
			read = buffer.get(n, IntCodec.varint());
			Assert.assertEquals(v, read);

			buffer.set(n, v, IntCodec.bigEndian());
			read = buffer.get(n, IntCodec.bigEndian());
			Assert.assertEquals(v, read);

			Assert.assertEquals(buffer.remaining(), 0);

			buffer.write(t, IntCodec.bigEndian());
			byte[] result = buffer.read(Codec.byteArray());
			Assert.assertArrayEquals(r1, result);

			buffer.write(t, IntCodec.littleEndian());
			result = buffer.read(Codec.byteArray());
			Assert.assertArrayEquals(r2, result);

			buffer.write(t, IntCodec.varint());
			result = buffer.read(Codec.byteArray());
			Assert.assertArrayEquals(r3, result);

			// prepend
			buffer.rewind();
			buffer.write(bytes, Codec.byteArray());

			buffer.prepend(v, IntCodec.varint());
			buffer.prepend(v, IntCodec.bigEndian());
			buffer.prepend(v, IntCodec.littleEndian());
			read = buffer.get(0, IntCodec.littleEndian());
			Assert.assertEquals(v, read);
			read = buffer.get(4, IntCodec.bigEndian());
			Assert.assertEquals(v, read);
			read = buffer.get(8, IntCodec.varint());
			Assert.assertEquals(v, read);
		}
	}

	@Test
	public void test_writeReadShort() {
		final byte[] bytes = createBytes();
		final short v = (short) new Random().nextInt();
		final int size = sizeOfVarint(v & 0xFFFF);
		final short t = 0x1234;
		final byte[] r1 = { 0x12, 0x34 };
		final byte[] r2 = { 0x34, 0x12 };
		final byte[] r3 = { (byte) 0xb4, 0x24 };
		final int n = bytes.length;
		for (int i = 1; i < n + 9 + 2 * size; ++i) {
			BufferFactory factory = initializeFactory(i);
			IBuffer buffer = factory.create();
			buffer.write(bytes, Codec.byteArray());

			buffer.write(v, ShortCodec.bigEndian());
			buffer.write(v, ShortCodec.littleEndian());
			buffer.write(v, ShortCodec.varint());
			buffer.write(v, ShortCodec.bigEndian());
			buffer.write(v, ShortCodec.littleEndian());
			buffer.write(v, ShortCodec.varint());

			Assert.assertEquals(n + (4 + size) * 2, buffer.size());

			buffer.skip(n);

			short read = buffer.read(ShortCodec.bigEndian());
			Assert.assertEquals(v, read);
			read = buffer.get(n, ShortCodec.bigEndian());
			Assert.assertEquals(v, read);

			read = buffer.read(ShortCodec.littleEndian());
			Assert.assertEquals(v, read);
			read = buffer.get(n + 2, ShortCodec.littleEndian());
			Assert.assertEquals(v, read);

			read = buffer.read(ShortCodec.varint());
			Assert.assertEquals(v, read);
			read = buffer.get(n + 4, ShortCodec.varint());
			Assert.assertEquals(v, read);

			// set/get
			buffer.set(n, v, ShortCodec.littleEndian());
			read = buffer.get(n, ShortCodec.littleEndian());
			Assert.assertEquals(v, read);

			buffer.set(n, v, ShortCodec.varint());
			read = buffer.get(n, ShortCodec.varint());
			Assert.assertEquals(v, read);

			buffer.set(n, v, ShortCodec.bigEndian());
			read = buffer.get(n, ShortCodec.bigEndian());
			Assert.assertEquals(v, read);

			int u = buffer.readUnsignedShort(ShortCodec.bigEndian());
			Assert.assertEquals(v & 0xFFFF, u);
			u = buffer.getUnsignedShort(n + 4 + size, ShortCodec.bigEndian());
			Assert.assertEquals(v & 0xFFFF, u);

			u = buffer.readUnsignedShort(ShortCodec.littleEndian());
			Assert.assertEquals(v & 0xFFFF, u);
			u = buffer.getUnsignedShort(n + 6 + size, ShortCodec.littleEndian());
			Assert.assertEquals(v & 0xFFFF, u);

			u = buffer.readUnsignedShort(ShortCodec.varint());
			Assert.assertEquals(v & 0xFFFF, u);
			u = buffer.getUnsignedShort(n + 8 + size, ShortCodec.varint());
			Assert.assertEquals(v & 0xFFFF, u);

			Assert.assertEquals(0, buffer.remaining());

			buffer.write(t, ShortCodec.bigEndian());
			Assert.assertArrayEquals(r1, buffer.read(Codec.byteArray()));

			buffer.write(t, ShortCodec.littleEndian());
			Assert.assertArrayEquals(r2, buffer.read(Codec.byteArray()));

			buffer.write(t, ShortCodec.varint());
			Assert.assertArrayEquals(r3, buffer.read(Codec.byteArray()));

			// prepend
			buffer.rewind();
			buffer.write(bytes, Codec.byteArray());

			buffer.prepend(v, ShortCodec.varint());
			buffer.prepend(v, ShortCodec.bigEndian());
			buffer.prepend(v, ShortCodec.littleEndian());
			read = buffer.get(0, ShortCodec.littleEndian());
			Assert.assertEquals(v, read);
			read = buffer.get(2, ShortCodec.bigEndian());
			Assert.assertEquals(v, read);
			read = buffer.get(4, ShortCodec.varint());
			Assert.assertEquals(v, read);
		}
	}

	@Test
	public void test_writeReadLong() {
		final byte[] bytes = createBytes();
		final long v = new Random().nextLong();
		final int size = sizeOfVarint(v);
		final long t = 0x1234567890abcdefL;
		final byte[] r1 = { 0x12, 0x34, 0x56, 0x78, (byte) 0x90, (byte) 0xab, (byte) 0xcd, (byte) 0xef };
		final byte[] r2 = { (byte) 0xef, (byte) 0xcd, (byte) 0xab, (byte) 0x90, 0x78, 0x56, 0x34, 0x12 };
		final byte[] r3 = { (byte) 0xef, (byte) 0x9b, (byte) 0xaf, (byte) 0x85, (byte) 0x89, (byte) 0xcf, (byte) 0x95,
				(byte) 0x9a, 0x12 };
		final int n = bytes.length;
		for (int i = 1; i < n + 17 + size; ++i) {
			BufferFactory factory = initializeFactory(i);
			IBuffer buffer = factory.create();
			buffer.write(bytes, Codec.byteArray());

			buffer.write(v, LongCodec.bigEndian());
			buffer.write(v, LongCodec.littleEndian());
			buffer.write(v, LongCodec.varint());

			Assert.assertEquals(n + 16 + size, buffer.size());

			buffer.skip(n);

			long read = buffer.read(LongCodec.bigEndian());
			Assert.assertEquals(v, read);
			read = buffer.get(n, LongCodec.bigEndian());
			Assert.assertEquals(v, read);

			read = buffer.read(LongCodec.littleEndian());
			Assert.assertEquals(v, read);
			read = buffer.get(n + 8, LongCodec.littleEndian());
			Assert.assertEquals(v, read);

			read = buffer.read(LongCodec.varint());
			Assert.assertEquals(v, read);
			read = buffer.get(n + 16, LongCodec.varint());
			Assert.assertEquals(v, read);

			// set/get
			buffer.set(n, v, LongCodec.littleEndian());
			read = buffer.get(n, LongCodec.littleEndian());
			Assert.assertEquals(v, read);

			buffer.set(n, v, LongCodec.varint());
			read = buffer.get(n, LongCodec.varint());
			Assert.assertEquals(v, read);

			buffer.set(n, v, LongCodec.bigEndian());
			read = buffer.get(n, LongCodec.bigEndian());
			Assert.assertEquals(v, read);

			Assert.assertEquals(0, buffer.remaining());

			buffer.write(t, LongCodec.bigEndian());
			Assert.assertArrayEquals(r1, buffer.read(Codec.byteArray()));

			buffer.write(t, LongCodec.littleEndian());
			Assert.assertArrayEquals(r2, buffer.read(Codec.byteArray()));

			buffer.write(t, LongCodec.varint());
			Assert.assertArrayEquals(r3, buffer.read(Codec.byteArray()));

			// prepend
			buffer.rewind();
			buffer.write(bytes, Codec.byteArray());

			buffer.prepend(v, LongCodec.varint());
			buffer.prepend(v, LongCodec.bigEndian());
			buffer.prepend(v, LongCodec.littleEndian());
			read = buffer.get(0, LongCodec.littleEndian());
			Assert.assertEquals(v, read);
			read = buffer.get(8, LongCodec.bigEndian());
			Assert.assertEquals(v, read);
			read = buffer.get(16, LongCodec.varint());
			Assert.assertEquals(v, read);
		}
	}

	@Test
	public void test_writeReadString() throws UnsupportedEncodingException {
		String testStr1 = "Test Buffer.";
		String testStr2 = "readString/Buffer.writeString;???read???write?????????";
		String testStr = testStr1 + testStr2;
		byte[] bytes1 = testStr1.getBytes("UTF-8");
		byte[] bytes2 = testStr2.getBytes("UTF-8");
		final int n1 = bytes1.length;
		final int n2 = bytes2.length;
		for (int i = 1; i < testStr.length() + 2; ++i) {
			BufferFactory factory = initializeFactory(i);
			IBuffer buffer = factory.create();
			buffer.write(bytes1, Codec.byteArray());
			buffer.write(bytes2, Codec.byteArray());

			String result = buffer.get(0, n1, StringCodec.utf_8()) + buffer.get(n1, n2, StringCodec.utf_8());
			Assert.assertEquals(testStr, result);

			IBuffer buffer2 = factory.create();
			buffer2.write(testStr, StringCodec.utf_8());
			String result2 = buffer2.get(0, StringCodec.utf_8());
			Assert.assertEquals(testStr, result2);

			result = buffer.read(n1, StringCodec.utf_8()) + buffer.read(n2, StringCodec.utf_8());
			Assert.assertEquals(testStr, result);

			result2 = buffer2.read(n1, StringCodec.utf_8()) + buffer2.read(n2, StringCodec.utf_8());
			Assert.assertEquals(testStr, result2);
		}
	}

	@Test
	public void test_indexOf() {
		byte[] bytes = createBytes();
		Random random = new Random();
		int n = random.nextInt(bytes.length);
		int len = random.nextInt(bytes.length - n);
		if (len < 1)
			++len;

		byte[] target = new byte[len];
		for (int t = 0; t < len; ++t)
			target[t] = (byte) (n + t);

		byte[] zeroBytes = new byte[0];
		ByteKmp kmp = new ByteKmp(target);
		ByteKmp emptyKmp = new ByteKmp(zeroBytes);

		byte b = (byte) n;

		for (int i = 1; i < bytes.length + 2; ++i) {
			BufferFactory factory = initializeFactory(i);

			IBuffer buffer = factory.create();
			buffer.write(target, Codec.byteArray());
			Assert.assertEquals(-1, buffer.indexOf(bytes));
			buffer.close();

			buffer = factory.create();
			buffer.write(bytes, Codec.byteArray());

			Assert.assertEquals(-1, buffer.indexOf((byte) bytes.length, 0));

			n = b & 0xFF;
			for (int j = -1; j <= n; ++j) {
				Assert.assertEquals(n, buffer.indexOf(b, j));

				Assert.assertEquals(j < 0 ? 0 : j, buffer.indexOf(zeroBytes, j));
				Assert.assertEquals(j < 0 ? 0 : j, buffer.indexOf(emptyKmp, j));

				Assert.assertEquals(n, buffer.indexOf(target, j));
				Assert.assertEquals(n, buffer.indexOf(kmp, j));
			}

			Assert.assertEquals(n, buffer.indexOf(target, n));
			Assert.assertEquals(n, buffer.indexOf(kmp, n));

			n = bytes.length + 1;
			for (int j = (b & 0xFF) + 1; j <= n; ++j) {
				Assert.assertEquals(-1, buffer.indexOf(b, j));

				Assert.assertEquals(j > bytes.length ? bytes.length : j, buffer.indexOf(zeroBytes, j));
				Assert.assertEquals(j > bytes.length ? bytes.length : j, buffer.indexOf(emptyKmp, j));

				Assert.assertEquals(-1, buffer.indexOf(target, j));
				Assert.assertEquals(-1, buffer.indexOf(kmp, j));
			}
		}
	}

	@Test
	public void test_lastIndexOf() {
		byte[] bytes = createBytes();
		Random random = new Random();
		int n = random.nextInt(bytes.length);
		int len = random.nextInt(bytes.length - n);
		if (len < 1)
			++len;
		byte[] target = new byte[len];
		for (int t = 0; t < len; ++t)
			target[t] = (byte) (n + t);

		ByteKmp kmp = new ByteKmp(target);

		byte[] zeroBytes = new byte[0];
		ByteKmp emptyKmp = new ByteKmp(zeroBytes);

		byte b = (byte) n;

		for (int i = 1; i < bytes.length + 2; ++i) {
			BufferFactory factory = initializeFactory(i);

			IBuffer buffer = factory.create();
			buffer.write(target, Codec.byteArray());
			Assert.assertEquals(-1, buffer.lastIndexOf(bytes));
			buffer.close();

			buffer = factory.create();
			buffer.write(bytes, Codec.byteArray());

			Assert.assertEquals(-1, buffer.lastIndexOf((byte) bytes.length, buffer.size()));

			n = b & 0xFF;
			for (int j = -1; j < n; ++j) {
				Assert.assertEquals(-1, buffer.lastIndexOf(b, j));

				Assert.assertEquals(j < -1 ? 0 : j, buffer.lastIndexOf(zeroBytes, j));
				Assert.assertEquals(j < -1 ? 0 : j, buffer.lastIndexOf(emptyKmp, j));

				Assert.assertEquals(-1, buffer.lastIndexOf(target, j));
				Assert.assertEquals(-1, buffer.lastIndexOf(kmp, j));
			}

			for (int j = n; j <= bytes.length + 1; ++j) {
				Assert.assertEquals(n, buffer.lastIndexOf(b, j));

				Assert.assertEquals(j > bytes.length ? bytes.length : j, buffer.lastIndexOf(zeroBytes, j));
				Assert.assertEquals(j > bytes.length ? bytes.length : j, buffer.lastIndexOf(emptyKmp, j));

				Assert.assertEquals(n, buffer.lastIndexOf(target, j));
				Assert.assertEquals(n, buffer.lastIndexOf(kmp, j));
			}
		}
	}

	@Test
	public void test_startsWith() {
		byte[] bytes = createBytes();
		Random random = new Random();
		int n = random.nextInt(bytes.length) + 1;
		byte[] target = new byte[n];
		System.arraycopy(bytes, 0, target, 0, n);
		byte[] target2 = new byte[n];
		System.arraycopy(target, 0, target2, 0, n - 1);
		target2[n - 1] = (byte) 255;

		byte[] zeroBytes = new byte[0];

		for (int i = 1; i < bytes.length + 2; ++i) {
			BufferFactory factory = initializeFactory(i);
			IBuffer buffer = factory.create();
			buffer.write(bytes, Codec.byteArray());

			Assert.assertTrue(buffer.startsWith(zeroBytes));
			Assert.assertTrue(buffer.startsWith(target));

			Assert.assertFalse(buffer.startsWith(target2));
		}
	}

	@Test
	public void test_endsWith() {
		byte[] bytes = createBytes();
		Random random = new Random();
		int n = random.nextInt(bytes.length) + 1;
		byte[] target = new byte[n];
		System.arraycopy(bytes, bytes.length - n, target, 0, n);
		byte[] target2 = new byte[n];
		System.arraycopy(target, 1, target2, 1, n - 1);
		target2[0] = (byte) 255;

		byte[] zeroBytes = new byte[0];
		for (int i = 1; i < bytes.length + 2; ++i) {
			BufferFactory factory = initializeFactory(i);
			IBuffer buffer = factory.create();
			buffer.write(bytes, Codec.byteArray());

			Assert.assertTrue(buffer.endsWith(zeroBytes));
			Assert.assertTrue(buffer.endsWith(target));

			Assert.assertFalse(buffer.endsWith(target2));
		}
	}

	@Test
	public void test_drainTo() {
		byte[] bytes = createBytes();
		Random random = new Random();
		int n = random.nextInt(bytes.length + 1) + 1;
		BufferFactory factory = initializeFactory(n);
		for (int i = 1; i < bytes.length; ++i) {
			IBuffer dst = factory.create();
			dst.write(bytes, 0, i, Codec.byteArray());

			IBuffer src = factory.create();
			src.write(bytes, i, bytes.length - i, Codec.byteArray());

			src.drainTo(dst);

			byte[] results = dst.read(Codec.byteArray());

			Assert.assertTrue(src.isEmpty());
			Assert.assertArrayEquals(bytes, results);
		}
	}

	@Test
	public void test_compareTo() {
		byte[] bytes = createBytes();
		for (int i = 1; i < bytes.length + 2; ++i) {
			BufferFactory factory = initializeFactory(i);
			IBuffer thisBuf = factory.create();
			IBuffer thatBuf = factory.create();
			Assert.assertEquals(0, thisBuf.compareTo(thatBuf));

			thisBuf.write(bytes, Codec.byteArray());
			thisBuf.read(Codec.byteArray());
			Assert.assertEquals(0, thisBuf.compareTo(thatBuf));

			thatBuf.write(bytes, Codec.byteArray());
			thatBuf.read(Codec.byteArray());
			Assert.assertEquals(0, thisBuf.compareTo(thatBuf));

			thisBuf.rewind();
			thatBuf.rewind();
			Assert.assertEquals(0, thisBuf.compareTo(thatBuf));

			thatBuf.write(i, IntCodec.bigEndian());
			Assert.assertEquals(-1, thisBuf.compareTo(thatBuf));
			Assert.assertEquals(1, thatBuf.compareTo(thisBuf));
		}
	}

	private static byte[] createBytes() {
		Random random = new Random();
		int n = random.nextInt(155) + 100;
		byte[] bytes = new byte[n];
		for (int i = 0; i < n; ++i)
			bytes[i] = (byte) i;
		return bytes;
	}

	private static BufferFactory initializeFactory(int unitCapacity) {
		Map<String, Object> props = s_props;
		props.put(UNIT_CAPACITY, unitCapacity);
		BufferFactory factory = s_factory;
		factory.modified(props);
		return factory;
	}

	private static int sizeOfVarint(int v) {
		if ((v & 0xF0000000) != 0)
			return 5;

		if ((v & 0xFFE00000) != 0)
			return 4;

		if ((v & 0xFFFFC000) != 0)
			return 3;

		if ((v & 0xFFFFFF80) != 0)
			return 2;

		return 1;
	}

	private static int sizeOfVarint(long v) {
		if ((v & 0x8000000000000000L) != 0L)
			return 10;

		if ((v & 0xFF00000000000000L) != 0L)
			return 9;

		if ((v & 0xFFFE000000000000L) != 0L)
			return 8;

		if ((v & 0xFFFFFC0000000000L) != 0L)
			return 7;

		if ((v & 0xFFFFFFF800000000L) != 0L)
			return 6;

		if ((v & 0xFFFFFFFFF0000000L) != 0L)
			return 5;

		if ((v & 0xFFFFFFFFFFE00000L) != 0L)
			return 4;

		if ((v & 0xFFFFFFFFFFFFC000L) != 0L)
			return 3;

		if ((v & 0xFFFFFFFFFFFFFF80L) != 0L)
			return 2;

		return 1;
	}
}
