package org.spongycastle.operator.bc;

import org.spongycastle.operator.GenericKey;

class OperatorUtils
{
    static byte[] getKeyBytes(GenericKey key)
    {
        if (key.getRepresentation() instanceof byte[])
        {
            return (byte[])key.getRepresentation();
        }

        throw new IllegalArgumentException("unknown generic key type");
    }
}
