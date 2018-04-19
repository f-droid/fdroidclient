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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.Date;
import java.util.zip.CRC32;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import kellinwood.logging.LoggerInterface;
import kellinwood.logging.LoggerManager;

public class ZioEntry implements Cloneable {

    private ZipInput zipInput;

    // public int signature = 0x02014b50;
    private short versionMadeBy;
    private short versionRequired;
    private short generalPurposeBits;
    private short compression;
    private short modificationTime;
    private short modificationDate;
    private int crc32;
    private int compressedSize;
    private int size;
    private String filename;
    private byte[] extraData;
    private short numAlignBytes = 0;
    private String fileComment;
    private short diskNumberStart;
    private short internalAttributes;
    private int externalAttributes;
    
    private int localHeaderOffset;
    private long dataPosition = -1;
    private byte[] data = null;
    private ZioEntryOutputStream entryOut = null;
    

    private static byte[] alignBytes = new byte[4];
    
    private static LoggerInterface log;

    public ZioEntry( ZipInput input) {
        zipInput = input;
    }

    public static LoggerInterface getLogger() {
        if (log == null) log = LoggerManager.getLogger( ZioEntry.class.getName());
        return log;
    }

    public ZioEntry( String name) {
        filename = name;
        fileComment = "";
        compression = 8;
        extraData = new byte[0];
        setTime( System.currentTimeMillis());
    }

    
    public ZioEntry( String name, String sourceDataFile)
        throws IOException
    {
        zipInput = new ZipInput( sourceDataFile);
        filename = name;
        fileComment = "";
        this.compression = 0;
        this.size = (int)zipInput.getFileLength();
        this.compressedSize = this.size;

        if (getLogger().isDebugEnabled()) 
            getLogger().debug(String.format("Computing CRC for %s, size=%d",sourceDataFile,size));
        
        // compute CRC
        CRC32 crc = new CRC32();

        byte[] buffer = new byte[8096];

        int numRead = 0;
        while (numRead != size) {
            int count = zipInput.read( buffer, 0, Math.min( buffer.length, (this.size - numRead)));
            if (count > 0) {
                crc.update( buffer, 0, count);
                numRead += count;
            }
        }

        this.crc32 = (int)crc.getValue();

        zipInput.seek(0);
        this.dataPosition = 0;
        extraData = new byte[0];
        setTime( new File(sourceDataFile).lastModified());
    }
    
    

    public ZioEntry( String name, String sourceDataFile, short compression, int crc32, int compressedSize, int size)
        throws IOException
    {
        zipInput = new ZipInput( sourceDataFile);
        filename = name;
        fileComment = "";
        this.compression = compression;
        this.crc32 = crc32;
        this.compressedSize = compressedSize;
        this.size = size;
        this.dataPosition = 0;
        extraData = new byte[0];
        setTime( new File(sourceDataFile).lastModified());
    }
    
    // Return a copy with a new name
    public ZioEntry getClonedEntry( String newName) 
    {
        
        ZioEntry clone;
        try {
            clone = (ZioEntry)this.clone();
        } 
        catch (CloneNotSupportedException e) 
        {
            throw new IllegalStateException("clone() failed!");
        }
        clone.setName(newName);
        return clone;
    }
    
    public void readLocalHeader() throws IOException
    {
        ZipInput input = zipInput;
        int tmp;
        boolean debug = getLogger().isDebugEnabled();

        input.seek( localHeaderOffset);

        if (debug) getLogger().debug( String.format("FILE POSITION: 0x%08x", input.getFilePointer()));

        // 0 	4 	Local file header signature = 0x04034b50
        int signature = input.readInt();
        if (signature != 0x04034b50) {
            throw new IllegalStateException( String.format("Local header not found at pos=0x%08x, file=%s", input.getFilePointer(), filename));
        }

        // This method is usually called just before the data read, so
        // its only purpose currently is to position the file pointer
        // for the data read.  The entry's attributes might also have
        // been changed since the central dir entry was read (e.g.,
        // filename), so throw away the values here.

        int tmpInt;
        short tmpShort;
        
        // 4 	2 	Version needed to extract (minimum)
        /* versionRequired */ tmpShort =  input.readShort();
        if (debug) log.debug(String.format("Version required: 0x%04x", tmpShort /*versionRequired*/));

        // 6 	2 	General purpose bit flag
        /* generalPurposeBits */ tmpShort = input.readShort();
        if (debug) log.debug(String.format("General purpose bits: 0x%04x", tmpShort /* generalPurposeBits */));
        
        // 8 	2 	Compression method
        /* compression */ tmpShort = input.readShort();
        if (debug) log.debug(String.format("Compression: 0x%04x", tmpShort /* compression */));

        // 10 	2 	File last modification time
        /* modificationTime */ tmpShort = input.readShort();
        if (debug) log.debug(String.format("Modification time: 0x%04x", tmpShort /* modificationTime */));

        // 12 	2 	File last modification date
        /* modificationDate */ tmpShort = input.readShort();
        if (debug) log.debug(String.format("Modification date: 0x%04x", tmpShort /* modificationDate */));

        // 14 	4 	CRC-32
        /* crc32 */ tmpInt = input.readInt();
        if (debug) log.debug(String.format("CRC-32: 0x%04x", tmpInt /*crc32*/));

        // 18 	4 	Compressed size
        /* compressedSize*/ tmpInt = input.readInt();
        if (debug) log.debug(String.format("Compressed size: 0x%04x", tmpInt /*compressedSize*/));

        // 22 	4 	Uncompressed size
        /* size */ tmpInt = input.readInt();
        if (debug) log.debug(String.format("Size: 0x%04x", tmpInt /*size*/ ));

        // 26 	2 	File name length (n)
        short fileNameLen = input.readShort();
        if (debug) log.debug(String.format("File name length: 0x%04x", fileNameLen));

        // 28 	2 	Extra field length (m)
        short extraLen = input.readShort();
        if (debug) log.debug(String.format("Extra length: 0x%04x", extraLen));

        // 30 	n 	File name      
        String filename = input.readString(fileNameLen);
        if (debug) log.debug("Filename: " + filename);

        // Extra data
        byte[] extra = input.readBytes( extraLen);

        // Record the file position of this entry's data.
        dataPosition = input.getFilePointer();
        if (debug) log.debug(String.format("Data position: 0x%08x",dataPosition));

    }

    public void writeLocalEntry( ZipOutput output) throws IOException
    {
        if (data == null && dataPosition < 0 && zipInput != null) {
            readLocalHeader();
        }
        
        localHeaderOffset = (int)output.getFilePointer();

        boolean debug = getLogger().isDebugEnabled();
        
        if (debug) {
            getLogger().debug( String.format("Writing local header at 0x%08x - %s", localHeaderOffset, filename));
        }
        
        if (entryOut != null) {
            entryOut.close();
            size = entryOut.getSize();
            data = ((ByteArrayOutputStream)entryOut.getWrappedStream()).toByteArray();
            compressedSize = data.length;
            crc32 = entryOut.getCRC();
        }
        
        output.writeInt( 0x04034b50);
        output.writeShort( versionRequired);
        output.writeShort( generalPurposeBits);
        output.writeShort( compression);
        output.writeShort( modificationTime);
        output.writeShort( modificationDate);
        output.writeInt( crc32);
        output.writeInt( compressedSize);
        output.writeInt( size);
        output.writeShort( (short)filename.length());

        numAlignBytes = 0;

        // Zipalign if the file is uncompressed, i.e., "Stored", and file size is not zero.
        if (compression == 0) {

            long dataPos = output.getFilePointer() + // current position
            2 +                                  // plus size of extra data length
            filename.length() +                  // plus filename
            extraData.length;                    // plus extra data

            short dataPosMod4 = (short)(dataPos % 4);

            if (dataPosMod4 > 0) {
                numAlignBytes = (short)(4 - dataPosMod4);
            }
        }

        
        // 28 	2 	Extra field length (m)
        output.writeShort( (short)(extraData.length + numAlignBytes));

        // 30 	n 	File name
        output.writeString( filename);

        // Extra data
        output.writeBytes( extraData);

        // Zipalign bytes
        if (numAlignBytes > 0) {
            output.writeBytes( alignBytes, 0, numAlignBytes);
        }

        if (debug) getLogger().debug(String.format("Data position 0x%08x", output.getFilePointer()));
        if (data != null) {
            output.writeBytes( data);
            if (debug) getLogger().debug(String.format("Wrote %d bytes", data.length));
        }
        else {

            if (debug) getLogger().debug(String.format("Seeking to position 0x%08x", dataPosition));
            zipInput.seek( dataPosition);
            
            int bufferSize = Math.min( compressedSize, 8096);
            byte[] buffer = new byte[bufferSize];
            long totalCount = 0;
            
            while (totalCount != compressedSize) {
                int numRead = zipInput.in.read( buffer, 0, (int)Math.min( compressedSize -  totalCount, bufferSize));  
                if (numRead > 0) {
                    output.writeBytes(buffer, 0, numRead);
                    if (debug) getLogger().debug(String.format("Wrote %d bytes", numRead));
                    totalCount += numRead;
                }
                else throw new IllegalStateException(String.format("EOF reached while copying %s with %d bytes left to go", filename, compressedSize -  totalCount));
            }
        }
    }		
    
    public static ZioEntry read(ZipInput input) throws IOException
    {

        // 0    4   Central directory header signature = 0x02014b50
        int signature = input.readInt();
        if (signature != 0x02014b50) {
            // back up to the signature
            input.seek( input.getFilePointer() - 4);
            return null;
        }

        ZioEntry entry = new ZioEntry( input);

        entry.doRead( input);
        return entry;
    }

    private void doRead( ZipInput input) throws IOException
    {

        boolean debug = getLogger().isDebugEnabled();

        // 4    2   Version needed to extract (minimum)
        versionMadeBy = input.readShort();
        if (debug) log.debug(String.format("Version made by: 0x%04x", versionMadeBy));

        // 4    2   Version required
        versionRequired = input.readShort();
        if (debug) log.debug(String.format("Version required: 0x%04x", versionRequired));

        // 6    2   General purpose bit flag
        generalPurposeBits = input.readShort();
        if (debug) log.debug(String.format("General purpose bits: 0x%04x", generalPurposeBits));
        // Bits 1, 2, 3, and 11 are allowed to be set (first bit is bit zero).  Any others are a problem.
        if ((generalPurposeBits & 0xF7F1) != 0x0000) {
            throw new IllegalStateException("Can't handle general purpose bits == "+String.format("0x%04x",generalPurposeBits));
        }

        // 8    2   Compression method
        compression = input.readShort();
        if (debug) log.debug(String.format("Compression: 0x%04x", compression));

        // 10   2   File last modification time
        modificationTime = input.readShort();
        if (debug) log.debug(String.format("Modification time: 0x%04x", modificationTime));

        // 12   2   File last modification date
        modificationDate = input.readShort();
        if (debug) log.debug(String.format("Modification date: 0x%04x", modificationDate));

        // 14   4   CRC-32
        crc32 = input.readInt();
        if (debug) log.debug(String.format("CRC-32: 0x%04x", crc32));

        // 18   4   Compressed size
        compressedSize = input.readInt();
        if (debug) log.debug(String.format("Compressed size: 0x%04x", compressedSize));

        // 22   4   Uncompressed size
        size = input.readInt();
        if (debug) log.debug(String.format("Size: 0x%04x", size));

        // 26   2   File name length (n)
        short fileNameLen = input.readShort();
        if (debug) log.debug(String.format("File name length: 0x%04x", fileNameLen));

        // 28   2   Extra field length (m)
        short extraLen = input.readShort();
        if (debug) log.debug(String.format("Extra length: 0x%04x", extraLen));

        short fileCommentLen = input.readShort();
        if (debug) log.debug(String.format("File comment length: 0x%04x", fileCommentLen));

        diskNumberStart = input.readShort();
        if (debug) log.debug(String.format("Disk number start: 0x%04x", diskNumberStart));

        internalAttributes = input.readShort();
        if (debug) log.debug(String.format("Internal attributes: 0x%04x", internalAttributes));

        externalAttributes = input.readInt();
        if (debug) log.debug(String.format("External attributes: 0x%08x", externalAttributes));

        localHeaderOffset = input.readInt();
        if (debug) log.debug(String.format("Local header offset: 0x%08x", localHeaderOffset));

        // 30   n   File name      
        filename = input.readString(fileNameLen);
        if (debug) log.debug("Filename: " + filename);

        extraData = input.readBytes( extraLen);

        fileComment = input.readString( fileCommentLen);
        if (debug) log.debug("File comment: " + fileComment);

        generalPurposeBits = (short)(generalPurposeBits & 0x0800); // Don't write a data descriptor, preserve UTF-8 encoded filename bit
        
        // Don't write zero-length entries with compression.
        if (size == 0) {
            compressedSize = 0;
            compression = 0;
            crc32 = 0;
        }

    }

    /** Returns the entry's data. */
    public byte[] getData() throws IOException
    {
        if (data != null) return data;
        
        byte[] tmpdata = new byte[size];
        
        InputStream din = getInputStream();
        int count = 0;
        
        while (count != size) {
            int numRead = din.read( tmpdata, count, size-count);
            if (numRead < 0) throw new IllegalStateException(String.format("Read failed, expecting %d bytes, got %d instead", size, count));
            count += numRead;
        }
        return tmpdata;
    }

    // Returns an input stream for reading the entry's data. 
    public InputStream getInputStream() throws IOException {
        return getInputStream(null);
    }

    // Returns an input stream for reading the entry's data. 
    public InputStream getInputStream(OutputStream monitorStream) throws IOException {
        
        if (entryOut != null) {
            entryOut.close();
            size = entryOut.getSize();
            data = ((ByteArrayOutputStream)entryOut.getWrappedStream()).toByteArray();
            compressedSize = data.length;
            crc32 = entryOut.getCRC();
            entryOut = null;
            InputStream rawis = new ByteArrayInputStream( data);
            if (compression == 0) return rawis;
            else {
                // Hacky, inflate using a sequence of input streams that returns 1 byte more than the actual length of the data.  
                // This extra dummy byte is required by InflaterInputStream when the data doesn't have the header and crc fields (as it is in zip files). 
                return new InflaterInputStream( new SequenceInputStream(rawis, new ByteArrayInputStream(new byte[1])), new Inflater( true));
            }
        }
        
        ZioEntryInputStream dataStream;
        dataStream = new ZioEntryInputStream(this);
        if (monitorStream != null) dataStream.setMonitorStream( monitorStream);
        if (compression != 0)  {
            // Note: When using nowrap=true with Inflater it is also necessary to provide 
            // an extra "dummy" byte as input. This is required by the ZLIB native library 
            // in order to support certain optimizations.
            dataStream.setReturnDummyByte(true);
            return new InflaterInputStream( dataStream, new Inflater( true));
        }
        else return dataStream;
    }

    // Returns an output stream for writing an entry's data.
    public OutputStream getOutputStream() 
    {
        entryOut = new ZioEntryOutputStream( compression, new ByteArrayOutputStream());
        return entryOut;
    }


    public void write( ZipOutput output) throws IOException {
        boolean debug = getLogger().isDebugEnabled();


        output.writeInt( 0x02014b50);
        output.writeShort( versionMadeBy);
        output.writeShort( versionRequired);
        output.writeShort( generalPurposeBits);
        output.writeShort( compression);
        output.writeShort( modificationTime);
        output.writeShort( modificationDate);
        output.writeInt( crc32);
        output.writeInt( compressedSize);
        output.writeInt( size);
        output.writeShort( (short)filename.length());
        output.writeShort( (short)(extraData.length + numAlignBytes));
        output.writeShort( (short)fileComment.length());
        output.writeShort( diskNumberStart);
        output.writeShort( internalAttributes);
        output.writeInt( externalAttributes);
        output.writeInt( localHeaderOffset);
        
        output.writeString( filename);
        output.writeBytes( extraData);
        if (numAlignBytes > 0) output.writeBytes( alignBytes, 0, numAlignBytes);
        output.writeString( fileComment);

    }

    /*
     * Returns timetamp in Java format
     */
    public long getTime() {
        int year = (int)(((modificationDate >> 9) & 0x007f) + 80);
        int month = (int)(((modificationDate >> 5) & 0x000f) - 1);
        int day = (int)(modificationDate & 0x001f);
        int hour = (int)((modificationTime >> 11) & 0x001f);
        int minute = (int)((modificationTime >> 5) & 0x003f);
        int seconds = (int)((modificationTime << 1) & 0x003e);
        Date d = new Date( year, month, day, hour, minute, seconds);
        return d.getTime();
    }

    /*
     * Set the file timestamp (using a Java time value).
     */
    public void setTime(long time) {
        Date d = new Date(time);
        long dtime;
        int year = d.getYear() + 1900;
        if (year < 1980) {
            dtime = (1 << 21) | (1 << 16);
        }
        else {
            dtime = (year - 1980) << 25 | (d.getMonth() + 1) << 21 |
            d.getDate() << 16 | d.getHours() << 11 | d.getMinutes() << 5 |
            d.getSeconds() >> 1;
        }

        modificationDate = (short)(dtime >> 16);
        modificationTime = (short)(dtime & 0xFFFF);
    }

    public boolean isDirectory() {
        return filename.endsWith("/");
    }
    
    public String getName() {
        return filename;
    }
    
    public void setName( String filename) {
        this.filename = filename;
    }
    
    /** Use 0 (STORED), or 8 (DEFLATE). */
    public void setCompression( int compression) {
        this.compression = (short)compression;
    }

    public short getVersionMadeBy() {
        return versionMadeBy;
    }

    public short getVersionRequired() {
        return versionRequired;
    }

    public short getGeneralPurposeBits() {
        return generalPurposeBits;
    }

    public short getCompression() {
        return compression;
    }

    public int getCrc32() {
        return crc32;
    }

    public int getCompressedSize() {
        return compressedSize;
    }

    public int getSize() {
        return size;
    }

    public byte[] getExtraData() {
        return extraData;
    }

    public String getFileComment() {
        return fileComment;
    }

    public short getDiskNumberStart() {
        return diskNumberStart;
    }

    public short getInternalAttributes() {
        return internalAttributes;
    }

    public int getExternalAttributes() {
        return externalAttributes;
    }

    public int getLocalHeaderOffset() {
        return localHeaderOffset;
    }

    public long getDataPosition() {
        return dataPosition;
    }

    public ZioEntryOutputStream getEntryOut() {
        return entryOut;
    }

    public ZipInput getZipInput() {
        return zipInput;
    }

}
