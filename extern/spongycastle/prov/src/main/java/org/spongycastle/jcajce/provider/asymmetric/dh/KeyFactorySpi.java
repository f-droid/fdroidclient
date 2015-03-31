package org.spongycastle.jcajce.provider.asymmetric.dh;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHPrivateKeySpec;
import javax.crypto.spec.DHPublicKeySpec;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.asn1.x9.X9ObjectIdentifiers;
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
        if (spec.isAssignableFrom(DHPrivateKeySpec.class) && key instanceof DHPrivateKey)
        {
            DHPrivateKey k = (DHPrivateKey)key;

            return new DHPrivateKeySpec(k.getX(), k.getParams().getP(), k.getParams().getG());
        }
        else if (spec.isAssignableFrom(DHPublicKeySpec.class) && key instanceof DHPublicKey)
        {
            DHPublicKey k = (DHPublicKey)key;

            return new DHPublicKeySpec(k.getY(), k.getParams().getP(), k.getParams().getG());
        }

        return super.engineGetKeySpec(key, spec);
    }

    protected Key engineTranslateKey(
        Key key)
        throws InvalidKeyException
    {
        if (key instanceof DHPublicKey)
        {
            return new BCDHPublicKey((DHPublicKey)key);
        }
        else if (key instanceof DHPrivateKey)
        {
            return new BCDHPrivateKey((DHPrivateKey)key);
        }

        throw new InvalidKeyException("key type unknown");
    }

    protected PrivateKey engineGeneratePrivate(
        KeySpec keySpec)
        throws InvalidKeySpecException
    {
        if (keySpec instanceof DHPrivateKeySpec)
        {
            return new BCDHPrivateKey((DHPrivateKeySpec)keySpec);
        }

        return super.engineGeneratePrivate(keySpec);
    }

    protected PublicKey engineGeneratePublic(
        KeySpec keySpec)
        throws InvalidKeySpecException
    {
        if (keySpec instanceof DHPublicKeySpec)
        {
            return new BCDHPublicKey((DHPublicKeySpec)keySpec);
        }

        return super.engineGeneratePublic(keySpec);
    }

    public PrivateKey generatePrivate(PrivateKeyInfo keyInfo)
        throws IOException
    {
        ASN1ObjectIdentifier algOid = keyInfo.getPrivateKeyAlgorithm().getAlgorithm();

        if (algOid.equals(PKCSObjectIdentifiers.dhKeyAgreement))
        {
            return new BCDHPrivateKey(keyInfo);
        }
        else if (algOid.equals(X9ObjectIdentifiers.dhpublicnumber))
        {
            return new BCDHPrivateKey(keyInfo);
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

        if (algOid.equals(PKCSObjectIdentifiers.dhKeyAgreement))
        {
            return new BCDHPublicKey(keyInfo);
        }
        else if (algOid.equals(X9ObjectIdentifiers.dhpublicnumber))
        {
            return new BCDHPublicKey(keyInfo);
        }
        else
        {
            throw new IOException("algorithm identifier " + algOid + " in key not recognised");
        }
    }
}
