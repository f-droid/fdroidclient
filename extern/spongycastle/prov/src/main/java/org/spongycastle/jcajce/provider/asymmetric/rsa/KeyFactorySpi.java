package org.spongycastle.jcajce.provider.asymmetric.rsa;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.pkcs.RSAPrivateKey;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.jcajce.provider.asymmetric.util.BaseKeyFactorySpi;
import org.spongycastle.jcajce.provider.asymmetric.util.ExtendedInvalidKeySpecException;

public class KeyFactorySpi
    extends BaseKeyFactorySpi
{
    public KeyFactorySpi()
    {
    }

    protected KeySpec engineGetKeySpec(
        Key key,
        Class spec)
        throws InvalidKeySpecException
    {
        if (spec.isAssignableFrom(RSAPublicKeySpec.class) && key instanceof RSAPublicKey)
        {
            RSAPublicKey k = (RSAPublicKey)key;

            return new RSAPublicKeySpec(k.getModulus(), k.getPublicExponent());
        }
        else if (spec.isAssignableFrom(RSAPrivateKeySpec.class) && key instanceof java.security.interfaces.RSAPrivateKey)
        {
            java.security.interfaces.RSAPrivateKey k = (java.security.interfaces.RSAPrivateKey)key;

            return new RSAPrivateKeySpec(k.getModulus(), k.getPrivateExponent());
        }
        else if (spec.isAssignableFrom(RSAPrivateCrtKeySpec.class) && key instanceof RSAPrivateCrtKey)
        {
            RSAPrivateCrtKey k = (RSAPrivateCrtKey)key;

            return new RSAPrivateCrtKeySpec(
                k.getModulus(), k.getPublicExponent(),
                k.getPrivateExponent(),
                k.getPrimeP(), k.getPrimeQ(),
                k.getPrimeExponentP(), k.getPrimeExponentQ(),
                k.getCrtCoefficient());
        }

        return super.engineGetKeySpec(key, spec);
    }

    protected Key engineTranslateKey(
        Key key)
        throws InvalidKeyException
    {
        if (key instanceof RSAPublicKey)
        {
            return new BCRSAPublicKey((RSAPublicKey)key);
        }
        else if (key instanceof RSAPrivateCrtKey)
        {
            return new BCRSAPrivateCrtKey((RSAPrivateCrtKey)key);
        }
        else if (key instanceof java.security.interfaces.RSAPrivateKey)
        {
            return new BCRSAPrivateKey((java.security.interfaces.RSAPrivateKey)key);
        }

        throw new InvalidKeyException("key type unknown");
    }

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
                //
                // in case it's just a RSAPrivateKey object... -- openSSL produces these
                //
                try
                {
                    return new BCRSAPrivateCrtKey(
                        RSAPrivateKey.getInstance(((PKCS8EncodedKeySpec)keySpec).getEncoded()));
                }
                catch (Exception ex)
                {
                    throw new ExtendedInvalidKeySpecException("unable to process key spec: " + e.toString(), e);
                }
            }
        }
        else if (keySpec instanceof RSAPrivateCrtKeySpec)
        {
            return new BCRSAPrivateCrtKey((RSAPrivateCrtKeySpec)keySpec);
        }
        else if (keySpec instanceof RSAPrivateKeySpec)
        {
            return new BCRSAPrivateKey((RSAPrivateKeySpec)keySpec);
        }

        throw new InvalidKeySpecException("Unknown KeySpec type: " + keySpec.getClass().getName());
    }

    protected PublicKey engineGeneratePublic(
        KeySpec keySpec)
        throws InvalidKeySpecException
    {
        if (keySpec instanceof RSAPublicKeySpec)
        {
            return new BCRSAPublicKey((RSAPublicKeySpec)keySpec);
        }

        return super.engineGeneratePublic(keySpec);
    }

    public PrivateKey generatePrivate(PrivateKeyInfo keyInfo)
        throws IOException
    {
        ASN1ObjectIdentifier algOid = keyInfo.getPrivateKeyAlgorithm().getAlgorithm();

        if (RSAUtil.isRsaOid(algOid))
        {
            return new BCRSAPrivateCrtKey(keyInfo);
        }
        else
        {
            throw new IOException("algorithm identifier " + algOid + " in key not recognised");
        }
    }

    public PublicKey generatePublic(SubjectPublicKeyInfo keyInfo)
        throws IOException
    {
        ASN1ObjectIdentifier algOid = keyInfo.getAlgorithm().getAlgorithm();

        if (RSAUtil.isRsaOid(algOid))
        {
            return new BCRSAPublicKey(keyInfo);
        }
        else
        {
            throw new IOException("algorithm identifier " + algOid + " in key not recognised");
        }
    }
}
