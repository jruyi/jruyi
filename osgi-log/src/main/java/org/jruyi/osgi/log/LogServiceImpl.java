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
package org.jruyi.osgi.log;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LogServiceImpl implements LogService {

	private final Logger m_logger;

	LogServiceImpl(Bundle bundle) {
		m_logger = LoggerFactory.getLogger(bundle.getSymbolicName());
	}

	@Override
	public void log(int level, String message) {
		switch (level) {
		case LogService.LOG_DEBUG:
			m_logger.debug(message);
			break;
		case LogService.LOG_INFO:
			m_logger.info(message);
			break;
		case LogService.LOG_WARNING:
			m_logger.warn(message);
			break;
		case LogService.LOG_ERROR:
			m_logger.error(message);
			break;
		}
	}

	@Override
	public void log(int level, String message, Throwable t) {
		switch (level) {
		case LogService.LOG_DEBUG:
			m_logger.debug(message, t);
			break;
		case LogService.LOG_INFO:
			m_logger.info(message, t);
			break;
		case LogService.LOG_WARNING:
			m_logger.warn(message, t);
			break;
		case LogService.LOG_ERROR:
			m_logger.error(message, t);
			break;
		}
	}

	@Override
	public void log(@SuppressWarnings("rawtypes") ServiceReference sr,
			int level, String message) {
		log(level, message);
	}

	@Override
	public void log(@SuppressWarnings("rawtypes") ServiceReference sr,
			int level, String message, Throwable t) {
		log(level, message, t);
	}
}
