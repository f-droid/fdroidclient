package org.spongycastle.jcajce.provider.util;

import java.util.HashMap;
import java.util.Map;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.nist.NISTObjectIdentifiers;
import org.spongycastle.asn1.ntt.NTTObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.util.Integers;

public class SecretKeyUtil
{
    private static Map keySizes = new HashMap();

    static
    {
        keySizes.put(PKCSObjectIdentifiers.des_EDE3_CBC.getId(), Integers.valueOf(192));

        keySizes.put(NISTObjectIdentifiers.id_aes128_CBC, Integers.valueOf(128));
        keySizes.put(NISTObjectIdentifiers.id_aes192_CBC, Integers.valueOf(192));
        keySizes.put(NISTObjectIdentifiers.id_aes256_CBC, Integers.valueOf(256));

        keySizes.put(NTTObjectIdentifiers.id_camellia128_cbc, Integers.valueOf(128));
        keySizes.put(NTTObjectIdentifiers.id_camellia192_cbc, Integers.valueOf(192));
        keySizes.put(NTTObjectIdentifiers.id_camellia256_cbc, Integers.valueOf(256));
    }

    public static int getKeySize(ASN1ObjectIdentifier oid)
    {
        Integer size = (Integer)keySizes.get(oid);

        if (size != null)
        {
            return size.intValue();
        }

        return -1;
    }
}
