/*
 * Copyright (C) 2010-2011 Ciaran Gultnieks <ciaran@ciarang.com>
 * Copyright (C) 2011 Henrik Tunedal <tunedal@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.fdroid.fdroid;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Locale;

public class Hasher {

    private MessageDigest digest;
    private File file;
    private byte[] array;
    private String hashCache;

    public Hasher(String type, File f) throws NoSuchAlgorithmException {
        init(type);
        this.file = f;
    }

    public Hasher(String type, byte[] a) throws NoSuchAlgorithmException {
        init(type);
        this.array = a;
    }

    private void init(String type) throws NoSuchAlgorithmException {
        try {
            digest = MessageDigest.getInstance(type);
        } catch (Exception e) {
            throw new NoSuchAlgorithmException(e);
        }
    }

    // Calculate hash (as lowercase hexadecimal string) for the file
    // specified in the constructor. This will return a cached value
    // on subsequent invocations. Returns the empty string on failure.
    public String getHash() {
        if (hashCache != null) {
            return hashCache;
        }
        if (file != null) {
            byte[] buffer = new byte[1024];
            int read;
            InputStream input = null;
            try {
                input = new BufferedInputStream(new FileInputStream(file));
                while ((read = input.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            } catch (Exception e) {
                hashCache = "";
                return hashCache;
            } finally {
                Utils.closeQuietly(input);
            }
        } else {
            digest.update(array);
        }
        hashCache = hex(digest.digest());
        return hashCache;
    }

    // Compare the calculated hash to another string, ignoring case,
    // returning true if they are equal. The empty string and null are
    // considered non-matching.
    public boolean match(String otherHash) {
        if (otherHash == null) {
            return false;
        }
        if (hashCache == null) {
            getHash();
        }
        return hashCache.equals(otherHash.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Checks the file against the provided hash, returning whether it is a match.
     */
    public static boolean isFileMatchingHash(File file, String hash, String hashType) {
        if (!file.exists()) {
            return false;
        }
        try {
            Hasher hasher = new Hasher(hashType, file);
            return hasher.match(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String hex(Certificate cert) {
        byte[] encoded;
        try {
            encoded = cert.getEncoded();
        } catch (CertificateEncodingException e) {
            encoded = new byte[0];
        }
        return hex(encoded);
    }

    private static String hex(byte[] sig) {
        byte[] csig = new byte[sig.length * 2];
        for (int j = 0; j < sig.length; j++) {
            byte v = sig[j];
            int d = (v >> 4) & 0xf;
            csig[j * 2] = (byte) (d >= 10 ? ('a' + d - 10) : ('0' + d));
            d = v & 0xf;
            csig[j * 2 + 1] = (byte) (d >= 10 ? ('a' + d - 10) : ('0' + d));
        }
        return new String(csig);
    }

    public static byte[] unhex(String data) {
        byte[] rawdata = new byte[data.length() / 2];
        for (int i = 0; i < data.length(); i++) {
            char halfbyte = data.charAt(i);
            int value;
            if ('0' <= halfbyte && halfbyte <= '9') {
                value = halfbyte - '0';
            } else if ('a' <= halfbyte && halfbyte <= 'f') {
                value = halfbyte - 'a' + 10;
            } else if ('A' <= halfbyte && halfbyte <= 'F') {
                value = halfbyte - 'A' + 10;
            } else {
                throw new IllegalArgumentException("Bad hex digit");
            }
            rawdata[i / 2] += (byte) (i % 2 == 0 ? value << 4 : value);
        }
        return rawdata;
    }

}
