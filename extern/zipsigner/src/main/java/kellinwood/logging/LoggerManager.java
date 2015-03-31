/*
 * Copyright (C) 2010 Ken Ellinwood.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kellinwood.logging;

import java.util.Map;
import java.util.TreeMap;

public class LoggerManager {

	static LoggerFactory factory = new NullLoggerFactory();
	
	static Map<String,LoggerInterface> loggers = new TreeMap<String,LoggerInterface>();
	
	public static void setLoggerFactory( LoggerFactory f) {
		factory = f;
	}
	
	public static LoggerInterface getLogger(String category) {
		
		LoggerInterface logger = loggers.get( category);
		if (logger == null) {
			logger = factory.getLogger(category);
			loggers.put( category, logger);
		}
		return logger;
	}
}
