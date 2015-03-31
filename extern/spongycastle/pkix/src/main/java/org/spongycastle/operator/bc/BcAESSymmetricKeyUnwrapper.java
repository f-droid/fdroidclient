package org.spongycastle.operator.bc;

import org.spongycastle.crypto.engines.AESWrapEngine;
import org.spongycastle.crypto.params.KeyParameter;

public class BcAESSymmetricKeyUnwrapper
    extends BcSymmetricKeyUnwrapper
{
    public BcAESSymmetricKeyUnwrapper(KeyParameter wrappingKey)
    {
        super(AESUtil.determineKeyEncAlg(wrappingKey), new AESWrapEngine(), wrappingKey);
    }
}
