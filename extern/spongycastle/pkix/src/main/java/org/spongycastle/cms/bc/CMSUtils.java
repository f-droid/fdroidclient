package org.spongycastle.cms.bc;

import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.operator.GenericKey;

class CMSUtils
{
    static CipherParameters getBcKey(GenericKey key)
    {
        if (key.getRepresentation() instanceof CipherParameters)
        {
            return (CipherParameters)key.getRepresentation();
        }

        if (key.getRepresentation() instanceof byte[])
        {
            return new KeyParameter((byte[])key.getRepresentation());
        }

        throw new IllegalArgumentException("unknown generic key type");
    }
}
