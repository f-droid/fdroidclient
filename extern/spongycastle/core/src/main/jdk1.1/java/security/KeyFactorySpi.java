
package java.security;

import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public abstract class KeyFactorySpi extends Object
{
    public KeyFactorySpi()
    {
    }

    protected abstract PrivateKey engineGeneratePrivate(KeySpec keySpec)
    throws InvalidKeySpecException;

    protected abstract PublicKey engineGeneratePublic(KeySpec keySpec)
    throws InvalidKeySpecException;

    protected abstract KeySpec engineGetKeySpec(Key key, Class keySpec)
    throws InvalidKeySpecException;

    protected abstract Key engineTranslateKey(Key key)
    throws InvalidKeyException;
}
