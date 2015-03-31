package org.spongycastle.eac.operator.jcajce;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Signature;

class ProviderEACHelper
    extends EACHelper
{
    private final Provider provider;

    ProviderEACHelper(Provider provider)
    {
        this.provider = provider;
    }

    protected Signature createSignature(String type)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return Signature.getInstance(type, provider.getName());
    }
}
