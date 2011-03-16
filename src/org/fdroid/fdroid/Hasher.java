/*
 * Copyright (C) 2010-2011 Ciaran Gultnieks <ciaran@ciarang.com>
 * Copyright (C) 2011 Henrik Tunedal <tunedal@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
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

import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.content.pm.Signature;

public class Hasher {

    private MessageDigest digest;
    private File f;
    private Signature s;
    private String hashCache;

    public Hasher(String type, File f) throws NoSuchAlgorithmException {
        init(type);
        this.f = f;
    }

    public Hasher(String type, Signature s) throws NoSuchAlgorithmException {
        init(type);
        this.s = s;
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
    // on subsequent invocations, unless reset() in called. Returns
    // the empty string on failure.
    public String getHash() {
        if (hashCache != null)
            return hashCache;

        String hash = null;
        byte[] buffer = new byte[1024];
        int read = 0;

        try {
            InputStream is;
            if (s == null)
                is = new FileInputStream(f);
            else
                is = new ByteArrayInputStream(s.toCharsString().getBytes());
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] checksum = digest.digest();
            BigInteger bigInt = new BigInteger(1, checksum);
            hash = bigInt.toString(16).toLowerCase();

            // Add leading zeros
            int targetLength = digest.getDigestLength() * 2;
            if (hash.length() < targetLength) {
                StringBuilder sb = new StringBuilder(targetLength);
                for (int i = hash.length(); i < targetLength; i++) {
                    sb.append('0');
                }
                sb.append(hash);
                hash = sb.toString();
            }

        } catch (Exception e) {
            return hashCache = "";
        }

        return hashCache = hash;
    }

    // Compare the calculated hash to another string, ignoring case,
    // returning true if they are equal. The empty string and null are
    // considered non-matching.
    public boolean match(String otherHash) {
        if (hashCache == null) getHash();
        if (otherHash == null || hashCache.equals(""))
            return false;
        return hashCache.equals(otherHash.toLowerCase());
    }

    public void reset() {
        hashCache = null;
        digest.reset();
    }

}
