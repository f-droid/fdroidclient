package org.spongycastle.eac.jcajce;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

class NamedEACHelper
    implements EACHelper
{
    private final String providerName;

    NamedEACHelper(String providerName)
    {
        this.providerName = providerName;
    }

    public KeyFactory createKeyFactory(String type)
        throws NoSuchProviderException, NoSuchAlgorithmException
    {
        return KeyFactory.getInstance(type, providerName);
    }
}