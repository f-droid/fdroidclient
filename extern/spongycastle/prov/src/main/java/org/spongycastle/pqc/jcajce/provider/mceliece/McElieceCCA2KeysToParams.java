package org.spongycastle.pqc.jcajce.provider.mceliece;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.pqc.crypto.mceliece.McElieceCCA2PrivateKeyParameters;
import org.spongycastle.pqc.crypto.mceliece.McElieceCCA2PublicKeyParameters;

/**
 * utility class for converting jce/jca McElieceCCA2 objects
 * objects into their org.spongycastle.crypto counterparts.
 */
public class McElieceCCA2KeysToParams
{


    static public AsymmetricKeyParameter generatePublicKeyParameter(
        PublicKey key)
        throws InvalidKeyException
    {
        if (key instanceof BCMcElieceCCA2PublicKey)
        {
            BCMcElieceCCA2PublicKey k = (BCMcElieceCCA2PublicKey)key;

            return new McElieceCCA2PublicKeyParameters(k.getOIDString(), k.getN(), k.getT(), k.getG(), k.getMcElieceCCA2Parameters());
        }

        throw new InvalidKeyException("can't identify McElieceCCA2 public key: " + key.getClass().getName());
    }


    static public AsymmetricKeyParameter generatePrivateKeyParameter(
        PrivateKey key)
        throws InvalidKeyException
    {
        if (key instanceof BCMcElieceCCA2PrivateKey)
        {
            BCMcElieceCCA2PrivateKey k = (BCMcElieceCCA2PrivateKey)key;
            return new McElieceCCA2PrivateKeyParameters(k.getOIDString(), k.getN(), k.getK(), k.getField(), k.getGoppaPoly(),
                k.getP(), k.getH(), k.getQInv(), k.getMcElieceCCA2Parameters());
        }

        throw new InvalidKeyException("can't identify McElieceCCA2 private key.");
    }
}
