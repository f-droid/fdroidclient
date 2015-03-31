package org.spongycastle.operator.bc;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ntt.NTTObjectIdentifiers;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.crypto.params.KeyParameter;

class CamelliaUtil
{
    static AlgorithmIdentifier determineKeyEncAlg(KeyParameter key)
    {
        int length = key.getKey().length * 8;
        ASN1ObjectIdentifier wrapOid;

        if (length == 128)
        {
            wrapOid = NTTObjectIdentifiers.id_camellia128_wrap;
        }
        else if (length == 192)
        {
            wrapOid = NTTObjectIdentifiers.id_camellia192_wrap;
        }
        else if (length == 256)
        {
            wrapOid = NTTObjectIdentifiers.id_camellia256_wrap;
        }
        else
        {
            throw new IllegalArgumentException(
                "illegal keysize in Camellia");
        }

        return new AlgorithmIdentifier(wrapOid); // parameters must be
        // absent
    }
}
