package org.spongycastle.eac.jcajce;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;

class DefaultEACHelper
    implements EACHelper
{
    public KeyFactory createKeyFactory(String type)
        throws NoSuchAlgorithmException
    {
        return KeyFactory.getInstance(type);
    }
}
