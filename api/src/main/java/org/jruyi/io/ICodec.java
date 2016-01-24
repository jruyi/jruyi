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

package org.jruyi.io;

/**
 * A codec used to encode/decode data to/from a chain of buffer units.
 *
 * @param <T>
 *            the type of the data object to be encoded/decoded
 *
 * @see Codec
 */
public interface ICodec<T> extends IReadDecoder<T>, IReadLimitedDecoder<T>, IReadToDstDecoder<T>, IReadToRangedDstDecoder<T>,
		IGetDecoder<T>, IGetLimitedDecoder<T>, IGetToDstDecoder<T>, IGetToRangedDstDecoder<T>, IWriteEncoder<T>,
		IWriteRangedEncoder<T>, ISetEncoder<T>, ISetRangedEncoder<T>, IPrependEncoder<T>, IPrependRangedEncoder<T> {
}
