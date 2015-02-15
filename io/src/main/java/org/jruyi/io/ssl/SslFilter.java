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

package org.jruyi.io.ssl;

import java.util.Map;

import org.jruyi.io.IFilter;
import org.jruyi.io.ISslContextParameters;
import org.jruyi.io.IoConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

@Component(name = IoConstants.FID_SSL, //
service = { IFilter.class }, //
property = { IoConstants.FILTER_ID + "=" + IoConstants.FID_SSL }, //
xmlns = "http://www.osgi.org/xmlns/scr/v1.2.0")
public final class SslFilter extends AbstractSslFilter {

	private ISslContextParameters m_sslcp;

	@Reference(name = "sslcp")
	protected void setSslContextParameters(ISslContextParameters sslcp) {
		m_sslcp = sslcp;
	}

	protected void unsetSslContextParameters(ISslContextParameters sslcp) {
		m_sslcp = null;
	}

	@Override
	protected void updatedSslContextParameters(ISslContextParameters sslcp) throws Exception {
		super.updatedSslContextParameters(sslcp);
	}

	@Modified
	@Override
	protected void modified(Map<String, ?> properties) throws Exception {
		super.modified(properties);
	}

	@Override
	protected ISslContextParameters sslcp() {
		return m_sslcp;
	}
}
