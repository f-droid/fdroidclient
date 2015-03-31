package org.spongycastle.jcajce.provider.asymmetric.x509;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactorySpi;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.jce.provider.BouncyCastleProvider;

public class KeyFactory
    extends KeyFactorySpi
{

    protected PrivateKey engineGeneratePrivate(
        KeySpec keySpec)
        throws InvalidKeySpecException
    {
        if (keySpec instanceof PKCS8EncodedKeySpec)
        {
            try
            {
                PrivateKeyInfo info = PrivateKeyInfo.getInstance(((PKCS8EncodedKeySpec)keySpec).getEncoded());
                PrivateKey     key = BouncyCastleProvider.getPrivateKey(info);

                if (key != null)
                {
                    return key;
                }

                throw new InvalidKeySpecException("no factory found for OID: " + info.getPrivateKeyAlgorithm().getAlgorithm());
            }
            catch (Exception e)
            {
                throw new InvalidKeySpecException(e.toString());
            }
        }

        throw new InvalidKeySpecException("Unknown KeySpec type: " + keySpec.getClass().getName());
    }

    protected PublicKey engineGeneratePublic(
        KeySpec keySpec)
        throws InvalidKeySpecException
    {
        if (keySpec instanceof X509EncodedKeySpec)
        {
            try
            {
                SubjectPublicKeyInfo info = SubjectPublicKeyInfo.getInstance(((X509EncodedKeySpec)keySpec).getEncoded());
                PublicKey            key = BouncyCastleProvider.getPublicKey(info);

                if (key != null)
                {
                    return key;
                }

                throw new InvalidKeySpecException("no factory found for OID: " + info.getAlgorithm().getAlgorithm());
            }
            catch (Exception e)
            {
                throw new InvalidKeySpecException(e.toString());
            }
        }

        throw new InvalidKeySpecException("Unknown KeySpec type: " + keySpec.getClass().getName());
    }

    protected KeySpec engineGetKeySpec(Key key, Class keySpec)
        throws InvalidKeySpecException
    {
        if (keySpec.isAssignableFrom(PKCS8EncodedKeySpec.class) && key.getFormat().equals("PKCS#8"))
        {
            return new PKCS8EncodedKeySpec(key.getEncoded());
        }
        else if (keySpec.isAssignableFrom(X509EncodedKeySpec.class) && key.getFormat().equals("X.509"))
        {
            return new X509EncodedKeySpec(key.getEncoded());
        }

        throw new InvalidKeySpecException("not implemented yet " + key + " " + keySpec);
    }

    protected Key engineTranslateKey(Key key)
        throws InvalidKeyException
    {
        throw new InvalidKeyException("not implemented yet " + key);
    }
}