package org.spongycastle.jcajce.provider.asymmetric.util;

import java.io.IOException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.jcajce.provider.util.AsymmetricKeyInfoConverter;

public abstract class BaseKeyFactorySpi
    extends java.security.KeyFactorySpi
    implements AsymmetricKeyInfoConverter
{
    protected PrivateKey engineGeneratePrivate(
        KeySpec keySpec)
        throws InvalidKeySpecException
    {
        if (keySpec instanceof PKCS8EncodedKeySpec)
        {
            try
            {
                return generatePrivate(PrivateKeyInfo.getInstance(((PKCS8EncodedKeySpec)keySpec).getEncoded()));
            }
            catch (Exception e)
            {
                throw new InvalidKeySpecException("encoded key spec not recognised");
            }
        }
        else
        {
            throw new InvalidKeySpecException("key spec not recognised");
        }
    }

    protected PublicKey engineGeneratePublic(
        KeySpec keySpec)
        throws InvalidKeySpecException
    {
        if (keySpec instanceof X509EncodedKeySpec)
        {
            try
            {
                return generatePublic(SubjectPublicKeyInfo.getInstance(((X509EncodedKeySpec)keySpec).getEncoded()));
            }
            catch (Exception e)
            {
                throw new InvalidKeySpecException("encoded key spec not recognised");
            }
        }
        else
        {
            throw new InvalidKeySpecException("key spec not recognised");
        }
    }

    protected KeySpec engineGetKeySpec(
        Key key,
        Class spec)
        throws InvalidKeySpecException
    {
        if (spec.isAssignableFrom(PKCS8EncodedKeySpec.class) && key.getFormat().equals("PKCS#8"))
        {
            return new PKCS8EncodedKeySpec(key.getEncoded());
        }
        else if (spec.isAssignableFrom(X509EncodedKeySpec.class) && key.getFormat().equals("X.509"))
        {
            return new X509EncodedKeySpec(key.getEncoded());
        }

        throw new InvalidKeySpecException("not implemented yet " + key + " " + spec);
    }
}
