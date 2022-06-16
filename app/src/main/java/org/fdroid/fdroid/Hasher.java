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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

public class Hasher {

    private MessageDigest digest;
    private final byte[] array;
    private String hashCache;

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
        digest.update(array);
        hashCache = hex(digest.digest());
        return hashCache;
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

    static byte[] unhex(String data) {
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
