/*
 * Copyright (C) 2010 Ken Ellinwood
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
package kellinwood.zipio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import kellinwood.logging.LoggerInterface;
import kellinwood.logging.LoggerManager;


/** Input stream used to read just the data from a zip file entry. */
public class ZioEntryInputStream extends InputStream {

    RandomAccessFile raf;
    int size;
    int offset;
    LoggerInterface log;
    boolean debug;
    boolean returnDummyByte = false;
    OutputStream monitor = null;
    
    public ZioEntryInputStream( ZioEntry entry) throws IOException {

        log = LoggerManager.getLogger( this.getClass().getName());
        debug = log.isDebugEnabled();
        offset = 0;
        size = entry.getCompressedSize();
        raf = entry.getZipInput().in;
        long dpos = entry.getDataPosition();
        if (dpos >= 0) {
            if (debug) log.debug(String.format("Seeking to %d", entry.getDataPosition()));
            raf.seek( entry.getDataPosition());
        }
        else {
            // seeks to, then reads, the local header, causing the 
            // file pointer to be positioned at the start of the data.
            entry.readLocalHeader();
        }
        
    }

    public void setReturnDummyByte( boolean returnExtraByte) {
        returnDummyByte = returnExtraByte;
    }
    
    // For debugging, if the monitor is set we write all data read to the monitor.
    public void setMonitorStream(OutputStream monitorStream) {
        monitor = monitorStream;
    }
    
    @Override
    public void close() throws IOException {
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int available() throws IOException {
        int available = size - offset;
        if (debug) log.debug(String.format("Available = %d", available));
        if (available == 0 && returnDummyByte) return 1;
        else return available;
    }

    @Override
    public int read() throws IOException {
        if ((size - offset) == 0) {
            if (returnDummyByte) {
                returnDummyByte = false;
                return 0;
            }
            else return -1;
        }
        int b = raf.read();
        if (b >= 0) {
            if (monitor != null) monitor.write(b);
            if (debug) log.debug("Read 1 byte");
            offset += 1;
        }
        else if (debug) log.debug("Read 0 bytes");        
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return readBytes( b, off, len);
    }

    private int readBytes(byte[] b, int off, int len) throws IOException {
        if ((size - offset) == 0) {
            if (returnDummyByte) {
                returnDummyByte = false;
                b[off] = 0;
                return 1;
            }
            else return -1;
        }        
        int numToRead = Math.min( len, available());
        int numRead = raf.read(b, off, numToRead);
        if (numRead > 0) {
            if (monitor != null) monitor.write(b, off, numRead);
            offset += numRead;
        }
        if (debug) log.debug(String.format("Read %d bytes for read(b,%d,%d)", numRead, off, len));
        return numRead;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return readBytes( b, 0, b.length);
    }

    @Override
    public long skip(long n) throws IOException {
        long numToSkip = Math.min( n, available());
        raf.seek( raf.getFilePointer() + numToSkip);
        if (debug) log.debug(String.format("Skipped %d bytes", numToSkip));                
        return numToSkip;
    }
}


