package org.spongycastle.eac.operator.jcajce;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;

class NamedEACHelper
    extends EACHelper
{
    private final String providerName;

    NamedEACHelper(String providerName)
    {
        this.providerName = providerName;
    }

    protected Signature createSignature(String type)
        throws NoSuchProviderException, NoSuchAlgorithmException
    {
        return Signature.getInstance(type, providerName);
    }
}