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

import java.nio.BufferUnderflowException;

/**
 * Decodes a {@code float} from a chain of buffer units.
 *
 * @since 2.5
 */
public interface IReadFloatDecoder {

	/**
	 * Decodes the bytes from the specified {@code unitChain}, starting at
	 * <i>position</i> in the current unit, to a {@code float} value and returns
	 * the resultant {@code float} value.
	 *
	 * @param unitChain
	 *            the bytes from which to be decoded
	 * @return the decoded {@code float} value
	 * @throws BufferUnderflowException
	 *             if there's not enough data remaining in the {@code unitChain}
	 */
	float read(IUnitChain unitChain);
}
