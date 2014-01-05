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
package org.jruyi.io.buffer;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.jruyi.common.ByteKmp;
import org.jruyi.common.StringBuilder;
import org.jruyi.io.Codec;
import org.jruyi.io.IBuffer;
import org.jruyi.io.IntCodec;
import org.jruyi.io.LongCodec;
import org.jruyi.io.ShortCodec;
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
		s_props = new HashMap<String, Object>();
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

			for (int i = 1; i < bytes.length + 11; i += 10) {
				BufferFactory factory = initializeFactory(i);
				IBuffer buffer = factory.create();
				buffer.write(bytes, Codec.byteArray());

				Assert.assertEquals(buffer.position(), 0);
				Assert.assertEquals(buffer.size(), bytes.length);

				buffer.dump(builder);
				hexDump2 = builder.toString();

				builder.setLength(0);

				Assert.assertEquals(hexDump2, hexDump1);

				byte[] bytes2 = buffer.read(bytes.length, Codec.byteArray());

				Assert.assertArrayEquals(bytes2, bytes);
				Assert.assertEquals(buffer.remaining(), 0);
			}
		} finally {
			builder.close();
		}
	}

	@Test
	public void test_writeReadInt() {
		byte[] bytes = createBytes();
		int v = new Random().nextInt();
		int t = 0x12345678;
		byte[] r1 = { 0x12, 0x34, 0x56, 0x78 };
		byte[] r2 = { 0x78, 0x56, 0x34, 0x12 };
		for (int i = bytes.length / 2 + 1; i < bytes.length + 10; ++i) {
			BufferFactory factory = initializeFactory(i);
			IBuffer buffer = factory.create();
			buffer.write(bytes, Codec.byteArray());

			buffer.write(v, IntCodec.bigEndian());
			buffer.write(v, IntCodec.littleEndian());

			Assert.assertEquals(buffer.size(), bytes.length + 8);

			int n = bytes.length;
			buffer.skip(n);

			int read = buffer.read(IntCodec.bigEndian());
			Assert.assertEquals(read, v);
			read = buffer.get(n, IntCodec.bigEndian());
			Assert.assertEquals(read, v);

			read = buffer.read(IntCodec.littleEndian());
			Assert.assertEquals(read, v);
			read = buffer.get(n + 4, IntCodec.littleEndian());
			Assert.assertEquals(read, v);

			Assert.assertEquals(buffer.remaining(), 0);

			buffer.write(t, IntCodec.bigEndian());
			byte[] result = buffer.read(Codec.byteArray());
			if (!Arrays.equals(result, r1)) {
				StringBuilder builder = StringBuilder.get();
				System.out.println(builder.appendHexDump(result));
				builder.close();
				buffer.getBytes(n + 8);
			}
			Assert.assertArrayEquals(result, r1);

			buffer.write(t, IntCodec.littleEndian());
			Assert.assertArrayEquals(buffer.read(Codec.byteArray()), r2);
		}
	}

	@Test
	public void test_writeReadShort() {
		byte[] bytes = createBytes();
		short v = (short) new Random().nextInt();
		short t = 0x1234;
		byte[] r1 = { 0x12, 0x34 };
		byte[] r2 = { 0x34, 0x12 };
		for (int i = bytes.length / 2 + 1; i < bytes.length + 9; ++i) {
			BufferFactory factory = initializeFactory(i);
			IBuffer buffer = factory.create();
			buffer.write(bytes, Codec.byteArray());

			buffer.write(v, ShortCodec.bigEndian());
			buffer.write(v, ShortCodec.littleEndian());
			buffer.write(v, ShortCodec.bigEndian());
			buffer.write(v, ShortCodec.littleEndian());

			Assert.assertEquals(buffer.size(), bytes.length + 8);

			int n = bytes.length;
			buffer.skip(n);

			short read = buffer.read(ShortCodec.bigEndian());
			Assert.assertEquals(read, v);
			read = buffer.get(n, ShortCodec.bigEndian());
			Assert.assertEquals(read, v);

			read = buffer.read(ShortCodec.littleEndian());
			Assert.assertEquals(read, v);
			read = buffer.get(n + 2, ShortCodec.littleEndian());
			Assert.assertEquals(read, v);

			int u = buffer.readUnsignedShort(ShortCodec.bigEndian());
			Assert.assertEquals(u, v & 0xFFFF);
			u = buffer.getUnsignedShort(n + 4, ShortCodec.bigEndian());
			Assert.assertEquals(u, v & 0xFFFF);

			u = buffer.readUnsignedShort(ShortCodec.littleEndian());
			Assert.assertEquals(u, v & 0xFFFF);
			u = buffer.getUnsignedShort(n + 6, ShortCodec.littleEndian());
			Assert.assertEquals(u, v & 0xFFFF);

			Assert.assertEquals(buffer.remaining(), 0);

			buffer.write(t, ShortCodec.bigEndian());
			Assert.assertArrayEquals(buffer.read(Codec.byteArray()), r1);

			buffer.write(t, ShortCodec.littleEndian());
			Assert.assertArrayEquals(buffer.read(Codec.byteArray()), r2);
		}
	}

	@Test
	public void test_writeReadLong() {
		byte[] bytes = createBytes();
		long v = new Random().nextLong();
		long t = 0x1234567890abcdefL;
		byte[] r1 = { 0x12, 0x34, 0x56, 0x78, (byte) 0x90, (byte) 0xab,
				(byte) 0xcd, (byte) 0xef };
		byte[] r2 = { (byte) 0xef, (byte) 0xcd, (byte) 0xab, (byte) 0x90, 0x78,
				0x56, 0x34, 0x12 };
		for (int i = bytes.length / 2 + 1; i < bytes.length + 17; ++i) {
			BufferFactory factory = initializeFactory(i);
			IBuffer buffer = factory.create();
			buffer.write(bytes, Codec.byteArray());

			buffer.write(v, LongCodec.bigEndian());
			buffer.write(v, LongCodec.littleEndian());

			Assert.assertEquals(buffer.size(), bytes.length + 16);

			int n = bytes.length;
			buffer.skip(n);

			long read = buffer.read(LongCodec.bigEndian());
			Assert.assertEquals(read, v);
			read = buffer.get(n, LongCodec.bigEndian());
			Assert.assertEquals(read, v);

			read = buffer.read(LongCodec.littleEndian());
			Assert.assertEquals(read, v);
			read = buffer.get(n + 8, LongCodec.littleEndian());
			Assert.assertEquals(read, v);

			Assert.assertEquals(buffer.remaining(), 0);

			buffer.write(t, LongCodec.bigEndian());
			Assert.assertArrayEquals(buffer.read(Codec.byteArray()), r1);

			buffer.write(t, LongCodec.littleEndian());
			Assert.assertArrayEquals(buffer.read(Codec.byteArray()), r2);
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

			String result = buffer.get(0, n1, Codec.utf_8())
					+ buffer.get(n1, n2, Codec.utf_8());
			Assert.assertEquals(result, testStr);

			IBuffer buffer2 = factory.create();
			buffer2.write(testStr, Codec.utf_8());
			String result2 = buffer2.get(0, Codec.utf_8());
			Assert.assertEquals(result2, testStr);

			result = buffer.read(n1, Codec.utf_8())
					+ buffer.read(n2, Codec.utf_8());
			Assert.assertEquals(result, testStr);

			result2 = buffer2.read(n1, Codec.utf_8())
					+ buffer2.read(n2, Codec.utf_8());
			Assert.assertEquals(result2, testStr);
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
			Assert.assertEquals(buffer.indexOf(bytes), -1);
			buffer.close();

			buffer = factory.create();
			buffer.write(bytes, Codec.byteArray());

			Assert.assertEquals(buffer.indexOf((byte) bytes.length, 0), -1);

			n = b & 0xFF;
			for (int j = -1; j <= n; ++j) {
				Assert.assertEquals(buffer.indexOf(b, j), n);

				Assert.assertEquals(buffer.indexOf(zeroBytes, j), j < 0 ? 0 : j);
				Assert.assertEquals(buffer.indexOf(emptyKmp, j), j < 0 ? 0 : j);

				Assert.assertEquals(buffer.indexOf(target, j), n);
				Assert.assertEquals(buffer.indexOf(kmp, j), n);
			}

			Assert.assertEquals(buffer.indexOf(target, n), n);
			Assert.assertEquals(buffer.indexOf(kmp, n), n);

			n = bytes.length + 1;
			for (int j = (b & 0xFF) + 1; j <= n; ++j) {
				Assert.assertEquals(buffer.indexOf(b, j), -1);

				Assert.assertEquals(buffer.indexOf(zeroBytes, j),
						j > bytes.length ? bytes.length : j);
				Assert.assertEquals(buffer.indexOf(emptyKmp, j),
						j > bytes.length ? bytes.length : j);

				Assert.assertEquals(buffer.indexOf(target, j), -1);
				Assert.assertEquals(buffer.indexOf(kmp, j), -1);
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
			Assert.assertEquals(buffer.lastIndexOf(bytes), -1);
			buffer.close();

			buffer = factory.create();
			buffer.write(bytes, Codec.byteArray());

			Assert.assertEquals(
					buffer.lastIndexOf((byte) bytes.length, buffer.size()), -1);

			n = b & 0xFF;
			for (int j = -1; j < n; ++j) {
				Assert.assertEquals(buffer.lastIndexOf(b, j), -1);

				Assert.assertEquals(buffer.lastIndexOf(zeroBytes, j),
						j < -1 ? 0 : j);
				Assert.assertEquals(buffer.lastIndexOf(emptyKmp, j), j < -1 ? 0
						: j);

				Assert.assertEquals(buffer.lastIndexOf(target, j), -1);
				Assert.assertEquals(buffer.lastIndexOf(kmp, j), -1);
			}

			for (int j = n; j <= bytes.length + 1; ++j) {
				Assert.assertEquals(buffer.lastIndexOf(b, j), n);

				Assert.assertEquals(buffer.lastIndexOf(zeroBytes, j),
						j > bytes.length ? bytes.length : j);
				Assert.assertEquals(buffer.lastIndexOf(emptyKmp, j),
						j > bytes.length ? bytes.length : j);

				Assert.assertEquals(buffer.lastIndexOf(target, j), n);
				Assert.assertEquals(buffer.lastIndexOf(kmp, j), n);
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
			Assert.assertEquals(thisBuf.compareTo(thatBuf), 0);

			thisBuf.write(bytes, Codec.byteArray());
			thisBuf.read(Codec.byteArray());
			Assert.assertEquals(thisBuf.compareTo(thatBuf), 0);

			thatBuf.write(bytes, Codec.byteArray());
			thatBuf.read(Codec.byteArray());
			Assert.assertEquals(thisBuf.compareTo(thatBuf), 0);

			thisBuf.rewind();
			thatBuf.rewind();
			Assert.assertEquals(thisBuf.compareTo(thatBuf), 0);

			thatBuf.write(i, IntCodec.bigEndian());
			Assert.assertEquals(thisBuf.compareTo(thatBuf), -1);
			Assert.assertEquals(thatBuf.compareTo(thisBuf), 1);
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
}
