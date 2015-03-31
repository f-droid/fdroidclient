package org.spongycastle.jcajce.provider.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.nist.NISTObjectIdentifiers;
import org.spongycastle.asn1.oiw.OIWObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.digests.MD5Digest;
import org.spongycastle.crypto.digests.SHA1Digest;
import org.spongycastle.crypto.digests.SHA224Digest;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.digests.SHA384Digest;
import org.spongycastle.crypto.digests.SHA512Digest;
import org.spongycastle.util.Strings;

public class DigestFactory
{
    private static Set md5 = new HashSet();
    private static Set sha1 = new HashSet();
    private static Set sha224 = new HashSet();
    private static Set sha256 = new HashSet();
    private static Set sha384 = new HashSet();
    private static Set sha512 = new HashSet();
    
    private static Map oids = new HashMap();
    
    static
    {
        md5.add("MD5");
        md5.add(PKCSObjectIdentifiers.md5.getId());
        
        sha1.add("SHA1");
        sha1.add("SHA-1");
        sha1.add(OIWObjectIdentifiers.idSHA1.getId());
        
        sha224.add("SHA224");
        sha224.add("SHA-224");
        sha224.add(NISTObjectIdentifiers.id_sha224.getId());
        
        sha256.add("SHA256");
        sha256.add("SHA-256");
        sha256.add(NISTObjectIdentifiers.id_sha256.getId());
        
        sha384.add("SHA384");
        sha384.add("SHA-384");
        sha384.add(NISTObjectIdentifiers.id_sha384.getId());
        
        sha512.add("SHA512");
        sha512.add("SHA-512");
        sha512.add(NISTObjectIdentifiers.id_sha512.getId()); 

        oids.put("MD5", PKCSObjectIdentifiers.md5);
        oids.put(PKCSObjectIdentifiers.md5.getId(), PKCSObjectIdentifiers.md5);
        
        oids.put("SHA1", OIWObjectIdentifiers.idSHA1);
        oids.put("SHA-1", OIWObjectIdentifiers.idSHA1);
        oids.put(OIWObjectIdentifiers.idSHA1.getId(), OIWObjectIdentifiers.idSHA1);
        
        oids.put("SHA224", NISTObjectIdentifiers.id_sha224);
        oids.put("SHA-224", NISTObjectIdentifiers.id_sha224);
        oids.put(NISTObjectIdentifiers.id_sha224.getId(), NISTObjectIdentifiers.id_sha224);
        
        oids.put("SHA256", NISTObjectIdentifiers.id_sha256);
        oids.put("SHA-256", NISTObjectIdentifiers.id_sha256);
        oids.put(NISTObjectIdentifiers.id_sha256.getId(), NISTObjectIdentifiers.id_sha256);
        
        oids.put("SHA384", NISTObjectIdentifiers.id_sha384);
        oids.put("SHA-384", NISTObjectIdentifiers.id_sha384);
        oids.put(NISTObjectIdentifiers.id_sha384.getId(), NISTObjectIdentifiers.id_sha384);
        
        oids.put("SHA512", NISTObjectIdentifiers.id_sha512);
        oids.put("SHA-512", NISTObjectIdentifiers.id_sha512);
        oids.put(NISTObjectIdentifiers.id_sha512.getId(), NISTObjectIdentifiers.id_sha512); 
    }
    
    public static Digest getDigest(
        String digestName) 
    {
        digestName = Strings.toUpperCase(digestName);
        
        if (sha1.contains(digestName))
        {
            return new SHA1Digest();
        }
        if (md5.contains(digestName))
        {
            return new MD5Digest();
        }
        if (sha224.contains(digestName))
        {
            return new SHA224Digest();
        }
        if (sha256.contains(digestName))
        {
            return new SHA256Digest();
        }
        if (sha384.contains(digestName))
        {
            return new SHA384Digest();
        }
        if (sha512.contains(digestName))
        {
            return new SHA512Digest();
        }
        
        return null;
    }
    
    public static boolean isSameDigest(
        String digest1,
        String digest2)
    {
        return (sha1.contains(digest1) && sha1.contains(digest2))
            || (sha224.contains(digest1) && sha224.contains(digest2))
            || (sha256.contains(digest1) && sha256.contains(digest2))
            || (sha384.contains(digest1) && sha384.contains(digest2))
            || (sha512.contains(digest1) && sha512.contains(digest2))
            || (md5.contains(digest1) && md5.contains(digest2));
    }
    
    public static ASN1ObjectIdentifier getOID(
        String digestName)
    {
        return (ASN1ObjectIdentifier)oids.get(digestName);
    }
}
