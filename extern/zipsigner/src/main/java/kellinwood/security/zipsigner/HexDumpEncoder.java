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


package kellinwood.security.zipsigner;

import java.io.IOException;
import java.io.ByteArrayOutputStream;

/** Produces the classic hex dump with an address column, hex data
 * section (16 bytes per row) and right-column printable character dislpay.
 */    
public class HexDumpEncoder
{

    static HexEncoder encoder = new HexEncoder();

    public static String encode( byte[] data) {

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            encoder.encode( data, 0, data.length, baos);
            byte[] hex = baos.toByteArray();

            StringBuilder hexDumpOut = new StringBuilder();
            for (int i = 0; i < hex.length; i += 32) {

                int max = Math.min(i+32, hex.length);

                StringBuilder hexOut = new StringBuilder();
                StringBuilder chrOut = new StringBuilder();

                hexOut.append( String.format("%08x: ", (i/2)));

                for (int j = i; j < max; j+= 2) {
                    hexOut.append( Character.valueOf( (char)hex[j]));
                    hexOut.append( Character.valueOf( (char)hex[j+1]));
                    if ((j+2) % 4 == 0) hexOut.append( ' ');

                    int dataChar = data[j/2];
                    if (dataChar >= 32 && dataChar < 127) chrOut.append( Character.valueOf( (char)dataChar));
                    else chrOut.append( '.');

                }

                hexDumpOut.append( hexOut.toString());
                for (int k = hexOut.length(); k < 50; k++) hexDumpOut.append(' ');
                hexDumpOut.append( "  ");
                hexDumpOut.append( chrOut);
                hexDumpOut.append("\n");
            }

            return hexDumpOut.toString();
        }
        catch (IOException x) {
            throw new IllegalStateException( x.getClass().getName() + ": " + x.getMessage());
        }
    }

}