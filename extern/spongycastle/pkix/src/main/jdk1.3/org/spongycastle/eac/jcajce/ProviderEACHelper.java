package org.spongycastle.eac.jcajce;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;

class ProviderEACHelper
    implements EACHelper
{
    private final Provider provider;

    ProviderEACHelper(Provider provider)
    {
        this.provider = provider;
    }

    public KeyFactory createKeyFactory(String type)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        return KeyFactory.getInstance(type, provider.getName());
    }
}
