/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.fdroid.fdroid;

import java.io.*;

public final class Utils {

    public static final int BUFFER_SIZE = 4096;

    private static final String[] FRIENDLY_SIZE_FORMAT = {
            "%.0f B", "%.0f KiB", "%.1f MiB", "%.2f GiB" };


    public static void copy(InputStream input, OutputStream output)
            throws IOException {
        copy(input, output, null, null);
    }

    public static void copy(InputStream input, OutputStream output,
                    ProgressListener progressListener,
                    ProgressListener.Event templateProgressEvent)
    throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead = 0;
        while (true) {
            int count = input.read(buffer);
            if (count == -1) {
                break;
            }
            if (progressListener != null) {
                bytesRead += count;
                templateProgressEvent.progress = bytesRead;
                progressListener.onProgress(templateProgressEvent);
            }
            output.write(buffer, 0, count);
        }
        output.flush();
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ioe) {
            // ignore
        }
    }

    public static String getFriendlySize(int size) {
        double s = size;
        int i = 0;
        while (i < FRIENDLY_SIZE_FORMAT.length - 1 && s >= 1024) {
            s = (100 * s / 1024) / 100.0;
            i++;
        }
        return String.format(FRIENDLY_SIZE_FORMAT[i], s);
    }

    public static int countSubstringOccurrence(File file, String substring) throws IOException {
        int count = 0;
        BufferedReader reader = null;
        try {

            reader = new BufferedReader(new FileReader(file));
            while(true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                count += countSubstringOccurrence(line, substring);
            }

        } finally {
            closeQuietly(reader);
        }
        return count;
    }

    /**
     * Thanks to http://stackoverflow.com/a/767910
     */
    public static int countSubstringOccurrence(String toSearch, String substring) {
        int count = 0;
        int index = 0;
        while (true) {
            index = toSearch.indexOf(substring, index);
            if (index == -1){
                break;
            }
            count ++;
            index += substring.length();
        }
        return count;
    }

}
