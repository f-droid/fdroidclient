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

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class AbstractLogger implements LoggerInterface
{

	protected String category;
	
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	
	public AbstractLogger( String category) {
		this.category = category;
	}
	
	protected String format( String level, String message) {
		return String.format( "%s %s %s: %s\n", dateFormat.format(new Date()), level, category, message);
	}
	
	protected abstract void write( String level, String message, Throwable t);

    protected void writeFixNullMessage( String level, String message, Throwable t) {
        if (message == null) {
            if (t != null) message = t.getClass().getName();
            else message = "null";
        }
        write( level, message, t);
    }

	public void debug(String message, Throwable t) {
        writeFixNullMessage( DEBUG, message, t);
	}

	public void debug(String message) {
        writeFixNullMessage( DEBUG, message, null);
	}

	public void error(String message, Throwable t) {
        writeFixNullMessage( ERROR, message, t);
	}

	public void error(String message) {
        writeFixNullMessage( ERROR, message, null);
	}

	public void info(String message, Throwable t) {
        writeFixNullMessage( INFO, message, t);
	}

	public void info(String message) {
        writeFixNullMessage( INFO, message, null);
	}

	public void warning(String message, Throwable t) {
        writeFixNullMessage( WARNING, message, t);
	}

	public void warning(String message) {
        writeFixNullMessage( WARNING, message, null);
	}

	public boolean isDebugEnabled() {
		return true;
	}

	public boolean isErrorEnabled() {
		return true;
	}

	public boolean isInfoEnabled() {
		return true;
	}

	public boolean isWarningEnabled() {
		return true;
	}


}
