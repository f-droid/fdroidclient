package org.spongycastle.jcajce.provider.asymmetric.util;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.params.GOST3410Parameters;
import org.spongycastle.crypto.params.GOST3410PrivateKeyParameters;
import org.spongycastle.crypto.params.GOST3410PublicKeyParameters;
import org.spongycastle.jce.interfaces.GOST3410PrivateKey;
import org.spongycastle.jce.interfaces.GOST3410PublicKey;
import org.spongycastle.jce.spec.GOST3410PublicKeyParameterSetSpec;

/**
 * utility class for converting jce/jca GOST3410-94 objects
 * objects into their org.spongycastle.crypto counterparts.
 */
public class GOST3410Util
{
    static public AsymmetricKeyParameter generatePublicKeyParameter(
        PublicKey    key)
        throws InvalidKeyException
    {
        if (key instanceof GOST3410PublicKey)
        {
            GOST3410PublicKey          k = (GOST3410PublicKey)key;
            GOST3410PublicKeyParameterSetSpec p = k.getParameters().getPublicKeyParameters();
            
            return new GOST3410PublicKeyParameters(k.getY(),
                new GOST3410Parameters(p.getP(), p.getQ(), p.getA()));
        }

        throw new InvalidKeyException("can't identify GOST3410 public key: " + key.getClass().getName());
    }

    static public AsymmetricKeyParameter generatePrivateKeyParameter(
        PrivateKey    key)
        throws InvalidKeyException
    {
        if (key instanceof GOST3410PrivateKey)
        {
            GOST3410PrivateKey         k = (GOST3410PrivateKey)key;
            GOST3410PublicKeyParameterSetSpec p = k.getParameters().getPublicKeyParameters();
            
            return new GOST3410PrivateKeyParameters(k.getX(),
                new GOST3410Parameters(p.getP(), p.getQ(), p.getA()));
        }

        throw new InvalidKeyException("can't identify GOST3410 private key.");
    }
}
