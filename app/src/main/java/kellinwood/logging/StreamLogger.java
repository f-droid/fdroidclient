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

public class StreamLogger extends AbstractLogger {

	PrintStream out;
	
	public StreamLogger( String category, PrintStream out)
	{
		super( category);
		this.out = out;
	}
	
	@Override
	protected void write(String level, String message, Throwable t) {
		out.print( format( level, message));
		if (t != null) t.printStackTrace(out);
	}

}
