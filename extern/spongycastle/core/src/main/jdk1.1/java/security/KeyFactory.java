
package java.security;

import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public class KeyFactory extends Object
{
    private KeyFactorySpi keyFacSpi;
    private Provider provider;
    private String algorithm;

    protected KeyFactory(
        KeyFactorySpi keyFacSpi,
        Provider provider,
        String algorithm)
    {
        this.keyFacSpi = keyFacSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    public final PrivateKey generatePrivate(KeySpec keySpec)
    throws InvalidKeySpecException
    {
        return keyFacSpi.engineGeneratePrivate(keySpec);
    }

    public final PublicKey generatePublic(KeySpec keySpec)
    throws InvalidKeySpecException
    {
        return keyFacSpi.engineGeneratePublic(keySpec);
    }

    public final String getAlgorithm()
    {
        return algorithm;
    }

    public static KeyFactory getInstance(String algorithm)
    throws NoSuchAlgorithmException
    {
        try
        {
            SecurityUtil.Implementation  imp = SecurityUtil.getImplementation("KeyFactory", algorithm, null);

            if (imp != null)
            {
                return new KeyFactory((KeyFactorySpi)imp.getEngine(), imp.getProvider(), algorithm);
            }

            throw new NoSuchAlgorithmException("can't find algorithm " + algorithm);
        }
        catch (NoSuchProviderException e)
        {
            throw new NoSuchAlgorithmException(algorithm + " not found");
        }
    }

    public static KeyFactory getInstance(String algorithm, String provider)
    throws NoSuchAlgorithmException, NoSuchProviderException
    {
        SecurityUtil.Implementation  imp = SecurityUtil.getImplementation("KeyFactory", algorithm, null);

        if (imp != null)
        {
            return new KeyFactory((KeyFactorySpi)imp.getEngine(), imp.getProvider(), algorithm);
        }

        throw new NoSuchAlgorithmException("can't find algorithm " + algorithm);
    }

    public final KeySpec getKeySpec(Key key, Class keySpec)
    throws InvalidKeySpecException
    {
        return keyFacSpi.engineGetKeySpec(key, keySpec);
    }

    public final Provider getProvider()
    {
        return provider;
    }

    public final Key translateKey(Key key)
    throws InvalidKeyException
    {
        return keyFacSpi.engineTranslateKey(key);
    }
}
