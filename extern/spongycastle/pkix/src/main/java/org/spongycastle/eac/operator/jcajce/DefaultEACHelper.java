package org.spongycastle.eac.operator.jcajce;

import java.security.NoSuchAlgorithmException;
import java.security.Signature;

class DefaultEACHelper
    extends EACHelper
{
    protected Signature createSignature(String type)
        throws NoSuchAlgorithmException
    {
        return Signature.getInstance(type);
    }
}
