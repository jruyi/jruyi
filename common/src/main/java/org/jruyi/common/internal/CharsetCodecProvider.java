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
package org.jruyi.common.internal;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jruyi.common.BytesBuilder;
import org.jruyi.common.CharsetCodec.IFactory;
import org.jruyi.common.ICharsetCodec;
import org.jruyi.common.IThreadLocalCache;
import org.jruyi.common.StringBuilder;
import org.jruyi.common.ThreadLocalCache;

public final class CharsetCodecProvider implements IFactory {

	private static final CharsetCodecProvider c_inst = new CharsetCodecProvider();
	private static final Map<String, SoftReference<CharsetCodec>> c_cache = new ConcurrentHashMap<String, SoftReference<CharsetCodec>>();

	static final class CharsetCodec implements ICharsetCodec {

		private static final byte[] EMPTY_BYTES = new byte[0];
		private static final char[] EMPTY_CHARS = new char[0];
		private final Charset m_charset;
		private final IThreadLocalCache<CharsetEncoder> m_encoderCache = ThreadLocalCache
				.weakArrayCache();
		private final IThreadLocalCache<CharsetDecoder> m_decoderCache = ThreadLocalCache
				.weakArrayCache();

		CharsetCodec(Charset charset) {
			m_charset = charset;
		}

		@Override
		public Charset getCharset() {
			return m_charset;
		}

		@Override
		public void encode(StringBuilder in, BytesBuilder out) {
			encode(in, 0, in.length(), out);
		}

		@Override
		public void encode(StringBuilder in, int offset, int length,
				BytesBuilder out) {
			if (length == 0)
				return;

			CharBuffer cb = in.getCharBuffer(offset, length);

			CharsetEncoder ce = getEncoder();
			try {
				offset = out.length();
				length = scale(length, ce.maxBytesPerChar());
				out.ensureCapacity(offset + length);
				ByteBuffer bb = out.getByteBuffer(offset, length);

				ce.reset();
				CoderResult cr = ce.encode(cb, bb, true);
				if (!cr.isUnderflow())
					cr.throwException();
				cr = ce.flush(bb);
				if (!cr.isUnderflow())
					cr.throwException();

				out.setLength(bb.position());
			} catch (CharacterCodingException e) {
				throw new RuntimeException(e);
			} finally {
				releaseEncoder(ce);
			}
		}

		@Override
		public void encode(char[] chars, BytesBuilder out) {
			encode(chars, 0, chars.length, out);
		}

		@Override
		public void encode(char[] chars, int offset, int length,
				BytesBuilder out) {
			if (length == 0)
				return;

			CharBuffer cb = CharBuffer.wrap(chars, offset, length);
			CharsetEncoder ce = getEncoder();
			try {
				offset = out.length();
				length = scale(length, ce.maxBytesPerChar());
				out.ensureCapacity(offset + length);
				ByteBuffer bb = out.getByteBuffer(offset, length);

				ce.reset();
				CoderResult cr = ce.encode(cb, bb, true);
				if (!cr.isUnderflow())
					cr.throwException();
				cr = ce.flush(bb);
				if (!cr.isUnderflow())
					cr.throwException();

				out.setLength(bb.position());
			} catch (CharacterCodingException e) {
				throw new RuntimeException(e);
			} finally {
				releaseEncoder(ce);
			}
		}

		@Override
		public void encode(CharBuffer in, BytesBuilder out) {
			if (!in.hasRemaining())
				return;

			CharsetEncoder ce = getEncoder();
			try {
				int offset = out.length();
				int length = scale(in.remaining(), ce.maxBytesPerChar());
				out.ensureCapacity(offset + length);
				ByteBuffer bb = out.getByteBuffer(offset, length);

				ce.reset();
				CoderResult cr = ce.encode(in, bb, true);
				if (!cr.isUnderflow())
					cr.throwException();
				cr = ce.flush(bb);
				if (!cr.isUnderflow())
					cr.throwException();

				out.setLength(bb.position());
			} catch (CharacterCodingException e) {
				throw new RuntimeException(e);
			} finally {
				releaseEncoder(ce);
			}
		}

		@Override
		public byte[] encode(char[] chars) {
			return encode(chars, 0, chars.length);
		}

		@Override
		public byte[] encode(char[] chars, int offset, int length) {
			if (length == 0)
				return EMPTY_BYTES;

			CharBuffer cb = CharBuffer.wrap(chars, offset, length);

			CharsetEncoder ce = getEncoder();
			int size = scale(length, ce.maxBytesPerChar());

			BytesBuilder out = BytesBuilder.get(size);
			try {
				ByteBuffer bb = out.getByteBuffer(0, size);

				ce.reset();
				CoderResult cr = ce.encode(cb, bb, true);
				if (!cr.isUnderflow())
					cr.throwException();
				cr = ce.flush(bb);
				if (!cr.isUnderflow())
					cr.throwException();

				out.setLength(bb.position());
				return out.toBytes();

			} catch (CharacterCodingException e) {
				throw new RuntimeException(e);
			} finally {
				out.close();
				releaseEncoder(ce);
			}
		}

		@Override
		public byte[] encode(CharBuffer in) {
			if (!in.hasRemaining())
				return EMPTY_BYTES;

			CharsetEncoder ce = getEncoder();
			int size = scale(in.remaining(), ce.maxBytesPerChar());

			BytesBuilder out = BytesBuilder.get(size);
			try {
				ByteBuffer bb = out.getByteBuffer(0, size);

				ce.reset();
				CoderResult cr = ce.encode(in, bb, true);
				if (!cr.isUnderflow())
					cr.throwException();
				cr = ce.flush(bb);
				if (!cr.isUnderflow())
					cr.throwException();

				out.setLength(bb.position());
				return out.toBytes();

			} catch (CharacterCodingException e) {
				throw new RuntimeException(e);
			} finally {
				out.close();
				releaseEncoder(ce);
			}
		}

		@Override
		public byte[] toBytes(String str) {
			return toBytes(str, 0, str.length());
		}

		@Override
		public byte[] toBytes(String str, int offset, int length) {
			CharBuffer cb = CharBuffer.wrap(str, offset, offset + length);
			BytesBuilder out = BytesBuilder.get();
			try {
				encode(cb, out);
				return out.toBytes();
			} finally {
				out.close();
			}
		}

		@Override
		public void decode(BytesBuilder in, StringBuilder out) {
			decode(in, 0, in.length(), out);
		}

		@Override
		public void decode(BytesBuilder in, int offset, int length,
				StringBuilder out) {
			if (length == 0)
				return;

			ByteBuffer bb = in.getByteBuffer(offset, length);

			CharsetDecoder cd = getDecoder();
			try {
				offset = out.length();
				length = scale(length, cd.maxCharsPerByte());
				out.ensureCapacity(offset + length);
				CharBuffer cb = out.getCharBuffer(offset, length);

				cd.reset();
				CoderResult cr = cd.decode(bb, cb, true);
				if (!cr.isUnderflow())
					cr.throwException();
				cr = cd.flush(cb);
				if (!cr.isUnderflow())
					cr.throwException();

				out.setLength(cb.position());
			} catch (CharacterCodingException e) {
				throw new RuntimeException(e);
			} finally {
				releaseDecoder(cd);
			}
		}

		@Override
		public void decode(byte[] in, StringBuilder out) {
			decode(in, 0, in.length, out);
		}

		@Override
		public void decode(byte[] in, int offset, int length, StringBuilder out) {
			if (length == 0)
				return;

			ByteBuffer bb = ByteBuffer.wrap(in, offset, length);

			CharsetDecoder cd = getDecoder();
			try {
				offset = out.length();
				length = scale(length, cd.maxCharsPerByte());
				out.ensureCapacity(offset + length);
				CharBuffer cb = out.getCharBuffer(offset, length);

				cd.reset();
				CoderResult cr = cd.decode(bb, cb, true);
				if (!cr.isUnderflow())
					cr.throwException();
				cr = cd.flush(cb);
				if (!cr.isUnderflow())
					cr.throwException();

				out.setLength(cb.position());
			} catch (CharacterCodingException e) {
				throw new RuntimeException(e);
			} finally {
				releaseDecoder(cd);
			}
		}

		@Override
		public void decode(ByteBuffer in, StringBuilder out) {
			if (!in.hasRemaining())
				return;

			CharsetDecoder cd = getDecoder();
			try {
				int offset = out.length();
				int length = scale(in.remaining(), cd.maxCharsPerByte());
				out.ensureCapacity(offset + length);
				CharBuffer cb = out.getCharBuffer(offset, length);

				cd.reset();
				CoderResult cr = cd.decode(in, cb, true);
				if (!cr.isUnderflow())
					cr.throwException();
				cr = cd.flush(cb);
				if (!cr.isUnderflow())
					cr.throwException();

				out.setLength(cb.position());
			} catch (CharacterCodingException e) {
				throw new RuntimeException(e);
			} finally {
				releaseDecoder(cd);
			}
		}

		@Override
		public char[] decode(byte[] in) {
			return decode(in, 0, in.length);
		}

		@Override
		public char[] decode(byte[] in, int offset, int length) {
			if (length == 0)
				return EMPTY_CHARS;

			ByteBuffer bb = ByteBuffer.wrap(in, offset, length);

			CharsetDecoder cd = getDecoder();
			int size = scale(length, cd.maxCharsPerByte());

			StringBuilder out = StringBuilder.get(size);
			try {
				CharBuffer cb = out.getCharBuffer(0, size);

				cd.reset();
				CoderResult cr = cd.decode(bb, cb, true);
				if (!cr.isUnderflow())
					cr.throwException();
				cr = cd.flush(cb);
				if (!cr.isUnderflow())
					cr.throwException();

				out.setLength(cb.position());
				return out.toCharArray();

			} catch (CharacterCodingException e) {
				throw new RuntimeException(e);
			} finally {
				out.close();
				releaseDecoder(cd);
			}
		}

		@Override
		public char[] decode(ByteBuffer in) {
			if (!in.hasRemaining())
				return EMPTY_CHARS;

			CharsetDecoder cd = getDecoder();
			int size = scale(in.remaining(), cd.maxCharsPerByte());

			StringBuilder out = StringBuilder.get(size);
			try {
				CharBuffer cb = out.getCharBuffer(0, size);

				cd.reset();
				CoderResult cr = cd.decode(in, cb, true);
				if (!cr.isUnderflow())
					cr.throwException();
				cr = cd.flush(cb);
				if (!cr.isUnderflow())
					cr.throwException();

				out.setLength(cb.position());
				return out.toCharArray();

			} catch (CharacterCodingException e) {
				throw new RuntimeException(e);
			} finally {
				out.close();
				releaseDecoder(cd);
			}
		}

		@Override
		public String toString(byte[] in) {
			return toString(in, 0, in.length);
		}

		@Override
		public String toString(byte[] in, int offset, int length) {
			if (length == 0)
				return "";

			ByteBuffer bb = ByteBuffer.wrap(in, offset, length);

			CharsetDecoder cd = getDecoder();
			int size = scale(length, cd.maxCharsPerByte());

			StringBuilder out = StringBuilder.get(size);
			try {
				CharBuffer cb = out.getCharBuffer(0, size);

				cd.reset();
				CoderResult cr = cd.decode(bb, cb, true);
				if (!cr.isUnderflow())
					cr.throwException();
				cr = cd.flush(cb);
				if (!cr.isUnderflow())
					cr.throwException();

				out.setLength(cb.position());
				return out.toString();

			} catch (CharacterCodingException e) {
				throw new RuntimeException(e);
			} finally {
				out.close();
				releaseDecoder(cd);
			}
		}

		@Override
		public String toString(ByteBuffer in) {
			if (!in.hasRemaining())
				return "";

			CharsetDecoder cd = getDecoder();
			int size = scale(in.remaining(), cd.maxCharsPerByte());

			StringBuilder out = StringBuilder.get(size);
			try {
				CharBuffer cb = out.getCharBuffer(0, size);

				cd.reset();
				CoderResult cr = cd.decode(in, cb, true);
				if (!cr.isUnderflow())
					cr.throwException();
				cr = cd.flush(cb);
				if (!cr.isUnderflow())
					cr.throwException();

				out.setLength(cb.position());
				return out.toString();

			} catch (CharacterCodingException e) {
				throw new RuntimeException(e);
			} finally {
				out.close();
				releaseDecoder(cd);
			}
		}

		@Override
		public void encode(CharBuffer[] in, BytesBuilder out) {
			encode(in, 0, in.length, out);
		}

		@Override
		public void encode(CharBuffer[] in, int offset, int length,
				BytesBuilder out) {
			int n = 0;
			int i = offset;
			int j = offset + length;
			while (i < j)
				n += in[i++].remaining();

			if (n < 1)
				return;

			CharsetEncoder ce = getEncoder();
			StringBuilder builder = StringBuilder.get();
			try {
				i = out.length();
				length = scale(n, ce.maxBytesPerChar());
				out.ensureCapacity(i + length);
				ByteBuffer bb = out.getByteBuffer(i, length);

				ce.reset();
				for (i = offset; i < j; ++i) {
					CharBuffer cb = in[i];
					n = builder.length();
					if (n > 0) {
						length = cb.remaining();
						builder.ensureCapacity(n + length);
						cb = builder.getCharBuffer(n, length).put(cb);
						cb.flip();
					}

					CoderResult cr = ce.encode(cb, bb, i == j - 1);
					if (!cr.isUnderflow())
						cr.throwException();

					length = cb.remaining();
					if (length > 0) {
						if (n > 0) {
							cb.compact();
							builder.setLength(length);
						} else {
							builder.ensureCapacity(length);
							builder.getCharBuffer(0, length).put(cb);
							builder.setLength(length);
						}
					} else
						builder.setLength(0);
				}

				CoderResult cr = ce.flush(bb);
				if (!cr.isUnderflow())
					cr.throwException();

				out.setLength(bb.position());
			} catch (CharacterCodingException e) {
				throw new RuntimeException(e);
			} finally {
				builder.close();
				releaseEncoder(ce);
			}
		}

		@Override
		public byte[] encode(CharBuffer[] in) {
			return encode(in, 0, in.length);
		}

		@Override
		public byte[] encode(CharBuffer[] in, int offset, int length) {
			BytesBuilder builder = BytesBuilder.get();
			try {
				encode(in, offset, length, builder);
				return builder.toBytes();
			} finally {
				builder.close();
			}
		}

		@Override
		public void decode(ByteBuffer[] in, StringBuilder out) {
			decode(in, 0, in.length, out);
		}

		@Override
		public void decode(ByteBuffer[] in, int offset, int length,
				StringBuilder out) {
			int n = 0;
			int i = offset;
			int j = offset + length;
			while (i < j)
				n += in[i++].remaining();

			if (n < 1)
				return;

			CharsetDecoder cd = getDecoder();
			BytesBuilder builder = BytesBuilder.get();
			try {
				i = out.length();
				length = scale(n, cd.maxCharsPerByte());
				out.ensureCapacity(i + length);
				CharBuffer cb = out.getCharBuffer(i, length);

				cd.reset();
				for (i = offset; i < j; ++i) {
					ByteBuffer bb = in[i];
					n = builder.length();
					if (n > 0) {
						length = bb.remaining();
						builder.ensureCapacity(n + length);
						bb = builder.getByteBuffer(n, length).put(bb);
						bb.flip();
					}

					CoderResult cr = cd.decode(bb, cb, i == j - 1);
					if (!cr.isUnderflow())
						cr.throwException();

					length = bb.remaining();
					if (length > 0) {
						if (n > 0) {
							bb.compact();
							builder.setLength(length);
						} else {
							builder.ensureCapacity(length);
							builder.getByteBuffer(0, length).put(bb);
							builder.setLength(length);
						}
					} else
						builder.setLength(0);
				}

				CoderResult cr = cd.flush(cb);
				if (!cr.isUnderflow())
					cr.throwException();

				out.setLength(cb.position());
			} catch (CharacterCodingException e) {
				throw new RuntimeException(e);
			} finally {
				builder.close();
				releaseDecoder(cd);
			}
		}

		@Override
		public char[] decode(ByteBuffer[] in) {
			return decode(in, 0, in.length);
		}

		@Override
		public char[] decode(ByteBuffer[] in, int offset, int length) {
			StringBuilder builder = StringBuilder.get();
			try {
				decode(in, offset, length, builder);
				return builder.toCharArray();
			} finally {
				builder.close();
			}
		}

		@Override
		public String toString(ByteBuffer[] in) {
			return toString(in, 0, in.length);
		}

		@Override
		public String toString(ByteBuffer[] in, int offset, int length) {
			StringBuilder builder = StringBuilder.get();
			try {
				decode(in, offset, length, builder);
				return builder.toString();
			} finally {
				builder.close();
			}
		}

		@Override
		public CharsetEncoder getEncoder() {
			CharsetEncoder encoder = m_encoderCache.take();
			if (encoder == null)
				encoder = m_charset.newEncoder();

			return encoder;
		}

		@Override
		public void releaseEncoder(CharsetEncoder encoder) {
			m_encoderCache.put(encoder);
		}

		@Override
		public CharsetDecoder getDecoder() {
			CharsetDecoder decoder = m_decoderCache.take();
			if (decoder == null)
				decoder = m_charset.newDecoder();

			return decoder;
		}

		@Override
		public void releaseDecoder(CharsetDecoder decoder) {
			m_decoderCache.put(decoder);
		}

		private static int scale(int len, float expansionFactor) {
			return (int) (len * (double) expansionFactor);
		}
	}

	static final class DefaultCharsetCodecHolder {

		static final CharsetCodec c_defaultCharsetCodec = new CharsetCodec(
				Charset.defaultCharset());
	}

	private CharsetCodecProvider() {
	}

	public static CharsetCodecProvider getInstance() {
		return c_inst;
	}

	public IFactory getFactory() {
		return this;
	}

	@Override
	public ICharsetCodec get() {
		return DefaultCharsetCodecHolder.c_defaultCharsetCodec;
	}

	@Override
	public ICharsetCodec get(String charsetName) {
		if (charsetName == null)
			throw new NullPointerException();

		SoftReference<CharsetCodec> reference = c_cache.get(charsetName);
		CharsetCodec cc = reference != null ? reference.get() : null;
		if (cc == null) {
			Map<String, SoftReference<CharsetCodec>> cache = c_cache;
			Charset charset = Charset.forName(charsetName);
			String canonicalName = charset.name();
			reference = cache.get(canonicalName);
			if (reference != null)
				cc = reference.get();

			if (cc == null) {
				cc = new CharsetCodec(charset);
				reference = new SoftReference<CharsetCodec>(cc);

				cache.put(charsetName, reference);
				cache.put(canonicalName, reference);
			} else {
				cache.put(charsetName, reference);
			}
		}

		return cc;
	}

	@Override
	public ICharsetCodec get(Charset charset) {
		String name = charset.name();
		SoftReference<CharsetCodec> reference = c_cache.get(name);
		CharsetCodec cc = reference == null ? null : reference.get();
		if (cc != null)
			return cc;

		cc = new CharsetCodec(charset);
		reference = new SoftReference<CharsetCodec>(cc);
		c_cache.put(name, reference);

		return cc;
	}
}
