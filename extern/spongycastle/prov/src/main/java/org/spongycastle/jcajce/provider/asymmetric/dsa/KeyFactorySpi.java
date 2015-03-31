package org.spongycastle.jcajce.provider.asymmetric.dsa;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.jcajce.provider.asymmetric.util.BaseKeyFactorySpi;

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
        if (spec.isAssignableFrom(DSAPublicKeySpec.class) && key instanceof DSAPublicKey)
        {
            DSAPublicKey k = (DSAPublicKey)key;

            return new DSAPublicKeySpec(k.getY(), k.getParams().getP(), k.getParams().getQ(), k.getParams().getG());
        }
        else if (spec.isAssignableFrom(DSAPrivateKeySpec.class) && key instanceof java.security.interfaces.DSAPrivateKey)
        {
            java.security.interfaces.DSAPrivateKey k = (java.security.interfaces.DSAPrivateKey)key;

            return new DSAPrivateKeySpec(k.getX(), k.getParams().getP(), k.getParams().getQ(), k.getParams().getG());
        }

        return super.engineGetKeySpec(key, spec);
    }

    protected Key engineTranslateKey(
        Key key)
        throws InvalidKeyException
    {
        if (key instanceof DSAPublicKey)
        {
            return new BCDSAPublicKey((DSAPublicKey)key);
        }
        else if (key instanceof DSAPrivateKey)
        {
            return new BCDSAPrivateKey((DSAPrivateKey)key);
        }

        throw new InvalidKeyException("key type unknown");
    }

    public PrivateKey generatePrivate(PrivateKeyInfo keyInfo)
        throws IOException
    {
        ASN1ObjectIdentifier algOid = keyInfo.getPrivateKeyAlgorithm().getAlgorithm();

        if (DSAUtil.isDsaOid(algOid))
        {
            return new BCDSAPrivateKey(keyInfo);
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

        if (DSAUtil.isDsaOid(algOid))
        {
            return new BCDSAPublicKey(keyInfo);
        }
        else
        {
            throw new IOException("algorithm identifier " + algOid + " in key not recognised");
        }
    }

    protected PrivateKey engineGeneratePrivate(
        KeySpec keySpec)
        throws InvalidKeySpecException
    {
        if (keySpec instanceof DSAPrivateKeySpec)
        {
            return new BCDSAPrivateKey((DSAPrivateKeySpec)keySpec);
        }

        return super.engineGeneratePrivate(keySpec);
    }

    protected PublicKey engineGeneratePublic(
        KeySpec keySpec)
        throws InvalidKeySpecException
    {
        if (keySpec instanceof DSAPublicKeySpec)
        {
            return new BCDSAPublicKey((DSAPublicKeySpec)keySpec);
        }

        return super.engineGeneratePublic(keySpec);
    }
}
