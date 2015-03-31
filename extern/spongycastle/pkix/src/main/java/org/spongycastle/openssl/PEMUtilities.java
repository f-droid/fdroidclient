package org.spongycastle.openssl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.DERObjectIdentifier;
import org.spongycastle.asn1.nist.NISTObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.util.Integers;

public final class PEMUtilities
{
    private static final Map KEYSIZES = new HashMap();
    private static final Set PKCS5_SCHEME_1 = new HashSet();
    private static final Set PKCS5_SCHEME_2 = new HashSet();

    static
    {
        PKCS5_SCHEME_1.add(PKCSObjectIdentifiers.pbeWithMD2AndDES_CBC);
        PKCS5_SCHEME_1.add(PKCSObjectIdentifiers.pbeWithMD2AndRC2_CBC);
        PKCS5_SCHEME_1.add(PKCSObjectIdentifiers.pbeWithMD5AndDES_CBC);
        PKCS5_SCHEME_1.add(PKCSObjectIdentifiers.pbeWithMD5AndRC2_CBC);
        PKCS5_SCHEME_1.add(PKCSObjectIdentifiers.pbeWithSHA1AndDES_CBC);
        PKCS5_SCHEME_1.add(PKCSObjectIdentifiers.pbeWithSHA1AndRC2_CBC);

        PKCS5_SCHEME_2.add(PKCSObjectIdentifiers.id_PBES2);
        PKCS5_SCHEME_2.add(PKCSObjectIdentifiers.des_EDE3_CBC);
        PKCS5_SCHEME_2.add(NISTObjectIdentifiers.id_aes128_CBC);
        PKCS5_SCHEME_2.add(NISTObjectIdentifiers.id_aes192_CBC);
        PKCS5_SCHEME_2.add(NISTObjectIdentifiers.id_aes256_CBC);

        KEYSIZES.put(PKCSObjectIdentifiers.des_EDE3_CBC.getId(), Integers.valueOf(192));
        KEYSIZES.put(NISTObjectIdentifiers.id_aes128_CBC.getId(), Integers.valueOf(128));
        KEYSIZES.put(NISTObjectIdentifiers.id_aes192_CBC.getId(), Integers.valueOf(192));
        KEYSIZES.put(NISTObjectIdentifiers.id_aes256_CBC.getId(), Integers.valueOf(256));
    }

    static int getKeySize(String algorithm)
    {
        if (!KEYSIZES.containsKey(algorithm))
        {
            throw new IllegalStateException("no key size for algorithm: " + algorithm);
        }
        
        return ((Integer)KEYSIZES.get(algorithm)).intValue();
    }

    static boolean isPKCS5Scheme1(DERObjectIdentifier algOid)
    {
        return PKCS5_SCHEME_1.contains(algOid);
    }

    public static boolean isPKCS5Scheme2(ASN1ObjectIdentifier algOid)
    {
        return PKCS5_SCHEME_2.contains(algOid);
    }

    public static boolean isPKCS12(DERObjectIdentifier algOid)
    {
        return algOid.getId().startsWith(PKCSObjectIdentifiers.pkcs_12PbeIds.getId());
    }
}
