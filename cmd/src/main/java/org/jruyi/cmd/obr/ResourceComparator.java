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
package org.jruyi.cmd.obr;

import java.util.Comparator;

import org.apache.felix.bundlerepository.Resource;

final class ResourceComparator implements Comparator<Resource> {

	public static final ResourceComparator INST = new ResourceComparator();

	@Override
	public int compare(Resource r1, Resource r2) {
		// Assume if the symbolic name is equal, then the two are equal,
		// since we are trying to aggregate by symbolic name.
		int symCompare = r1.getSymbolicName().compareTo(r2.getSymbolicName());
		if (symCompare == 0)
			// in descending version order (newest first)
			return r2.getVersion().compareTo(r1.getVersion());

		// Otherwise, compare the presentation name to keep them sorted
		// by presentation name. If the presentation names are equal,
		// then use the symbolic name to differentiate.
		String pn1 = r1.getPresentationName();
		String pn2 = r2.getPresentationName();
		int compare = pn1 == null ? -1 : (pn2 == null) ? 1 : pn1
				.compareToIgnoreCase(pn2);
		if (compare == 0)
			return symCompare;

		return compare;
	}
}
