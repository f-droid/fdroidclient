/*
 * Copyright (C) 2016 Hans-Christoph Steiner <hans@eds.org>
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

/*
 Copyright (c) 2016, Liu Dong
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution.

 * Neither the name of apk-parser nor the names of its
 contributors may be used to endorse or promote products derived from
 this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.fdroid.fdroid;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Parse the 'compressed' binary form of Android XML docs such as for
 * {@code AndroidManifest.xml} in APK files.  This is a very truncated
 * version of apk-parser since currently, we only need the header from
 * the binary XML AndroidManifest.xml. apk-parser provides full APK
 * parsing, which is a lot more than what is needed.
 *
 * @see <a href="https://github.com/caoqianli/apk-parser">apk-parser</a>
 * @see <a href="https://justanapplication.wordpress.com/category/android/android-binary-xml">Android Internals: Binary XML</a>
 * @see <a href="https://stackoverflow.com/a/4761689">a binary XML parser</a>
 */
public class AndroidXMLDecompress {
    public static int startTag = 0x00100102;

    /**
     * Just get the XML attributes from the {@code <manifest>} element.
     *
     * @return A key value map of the attributes, with values as {@link Object}s
     */
    public static Map<String, Object> getManifestHeaderAttributes(String filename) throws IOException {
        byte[] binaryXml = getManifestFromFilename(filename);
        int numbStrings = littleEndianWord(binaryXml, 4 * 4);
        int stringIndexTableOffset = 0x24;
        int stringTableOffset = stringIndexTableOffset + numbStrings * 4;
        int xmlTagOffset = littleEndianWord(binaryXml, 3 * 4);
        for (int i = xmlTagOffset; i < binaryXml.length - 4; i += 4) {
            if (littleEndianWord(binaryXml, i) == startTag) {
                xmlTagOffset = i;
                break;
            }
        }
        int offset = xmlTagOffset;

        while (offset < binaryXml.length) {
            int tag0 = littleEndianWord(binaryXml, offset);
            int nameStringIndex = littleEndianWord(binaryXml, offset + 5 * 4);

            if (tag0 == startTag) {
                int numbAttrs = littleEndianWord(binaryXml, offset + 7 * 4);
                offset += 9 * 4;

                HashMap<String, Object> attributes = new HashMap<String, Object>(3);
                for (int i = 0; i < numbAttrs; i++) {
                    int attributeNameStringIndex = littleEndianWord(binaryXml, offset + 1 * 4);
                    int attributeValueStringIndex = littleEndianWord(binaryXml, offset + 2 * 4);
                    int attributeResourceId = littleEndianWord(binaryXml, offset + 4 * 4);
                    offset += 5 * 4;

                    String attributeName = getString(binaryXml, stringIndexTableOffset, stringTableOffset, attributeNameStringIndex);
                    Object attributeValue;
                    if (attributeValueStringIndex != -1) {
                        attributeValue = getString(binaryXml, stringIndexTableOffset, stringTableOffset, attributeValueStringIndex);
                    } else {
                        attributeValue = attributeResourceId;
                    }
                    attributes.put(attributeName, attributeValue);
                }
                return attributes;
            } else {
                // we only need the first <manifest> start tag
                break;
            }
        }
        return new HashMap<String, Object>(0);
    }

    public static byte[] getManifestFromFilename(String filename) throws IOException {
        InputStream is = null;
        ZipFile zip = null;
        int size = 0;

        if (filename.endsWith(".apk")) {
            zip = new ZipFile(filename);
            ZipEntry ze = zip.getEntry("AndroidManifest.xml");
            is = zip.getInputStream(ze);
            size = (int) ze.getSize();
        } else {
            throw new RuntimeException("This only works on APK files!");
        }
        byte[] buf = new byte[size];
        is.read(buf);

        is.close();
        if (zip != null) {
            zip.close();
        }
        return buf;
    }

    public static String getString(byte[] bytes, int stringIndexTableOffset, int stringTableOffset, int stringIndex) {
        if (stringIndex < 0) {
            return null;
        }
        int stringOffset = stringTableOffset + littleEndianWord(bytes, stringIndexTableOffset + stringIndex * 4);
        return getStringAt(bytes, stringOffset);
    }

    public static String getStringAt(byte[] bytes, int stringOffset) {
        int length = bytes[stringOffset + 1] << 8 & 0xff00 | bytes[stringOffset] & 0xff;
        byte[] chars = new byte[length];
        for (int i = 0; i < length; i++) {
            chars[i] = bytes[stringOffset + 2 + i * 2];
        }
        return new String(chars);
    }

    /**
     * Return the little endian 32-bit word from the byte array at offset
     */
    public static int littleEndianWord(byte[] bytes, int offset) {
        return bytes[offset + 3]
                << 24 & 0xff000000
                | bytes[offset + 2]
                << 16 & 0xff0000
                | bytes[offset + 1]
                << 8 & 0xff00
                | bytes[offset] & 0xFF;
    }
}
