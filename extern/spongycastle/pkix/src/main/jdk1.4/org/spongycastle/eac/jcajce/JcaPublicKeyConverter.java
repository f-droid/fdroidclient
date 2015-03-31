package org.spongycastle.eac.jcajce;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.eac.EACObjectIdentifiers;
import org.spongycastle.asn1.eac.ECDSAPublicKey;
import org.spongycastle.asn1.eac.PublicKeyDataObject;
import org.spongycastle.asn1.eac.RSAPublicKey;
import org.spongycastle.eac.EACException;
import org.spongycastle.jce.interfaces.ECPublicKey;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.jce.spec.ECPublicKeySpec;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECFieldElement;
import org.spongycastle.math.ec.ECPoint;

public class JcaPublicKeyConverter
{
    private EACHelper helper = new DefaultEACHelper();

    public JcaPublicKeyConverter setProvider(String providerName)
    {
        this.helper = new NamedEACHelper(providerName);

        return this;
    }

    public JcaPublicKeyConverter setProvider(Provider provider)
    {
        this.helper = new ProviderEACHelper(provider);

        return this;
    }

    public PublicKey getKey(PublicKeyDataObject publicKeyDataObject)
        throws EACException, InvalidKeySpecException
    {
        if (publicKeyDataObject.getUsage().on(EACObjectIdentifiers.id_TA_ECDSA))
        {
            return getECPublicKeyPublicKey((ECDSAPublicKey)publicKeyDataObject);
        }
        else
        {
            RSAPublicKey pubKey = (RSAPublicKey)publicKeyDataObject;
            RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(pubKey.getModulus(), pubKey.getPublicExponent());

            try
            {
                KeyFactory factk = helper.createKeyFactory("RSA");

                return factk.generatePublic(pubKeySpec);
            }
            catch (NoSuchProviderException e)
            {
                throw new EACException("cannot find provider: " + e.getMessage(), e);
            }
            catch (NoSuchAlgorithmException e)
            {
                throw new EACException("cannot find algorithm ECDSA: " + e.getMessage(), e);
            }
        }
    }

    private PublicKey getECPublicKeyPublicKey(ECDSAPublicKey key)
        throws EACException, InvalidKeySpecException
    {
        ECParameterSpec spec = getParams(key);
        ECCurve curve = spec.getCurve();

        ECPoint point = curve.decodePoint(key.getPublicPointY());
        ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(point, spec);

        KeyFactory factk;
        try
        {
            factk = helper.createKeyFactory("ECDSA");
        }
        catch (NoSuchProviderException e)
        {
            throw new EACException("cannot find provider: " + e.getMessage(), e);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new EACException("cannot find algorithm ECDSA: " + e.getMessage(), e);
        }

        return factk.generatePublic(pubKeySpec);
    }

    private ECParameterSpec getParams(ECDSAPublicKey key)
    {
        if (!key.hasParameters())
        {
            throw new IllegalArgumentException("Public key does not contains EC Params");
        }

        BigInteger p = key.getPrimeModulusP();
        ECCurve.Fp curve = new ECCurve.Fp(p, key.getFirstCoefA(), key.getSecondCoefB());

        ECPoint G = curve.decodePoint(key.getBasePointG());

        BigInteger order = key.getOrderOfBasePointR();
        BigInteger coFactor = key.getCofactorF();

        ECParameterSpec ecspec = new ECParameterSpec(curve, G, order, coFactor);

        return ecspec;
    }

    public PublicKeyDataObject getPublicKeyDataObject(ASN1ObjectIdentifier usage, PublicKey publicKey)
    {
        if (publicKey instanceof java.security.interfaces.RSAPublicKey)
        {
            java.security.interfaces.RSAPublicKey pubKey = (java.security.interfaces.RSAPublicKey)publicKey;

            return new RSAPublicKey(usage, pubKey.getModulus(), pubKey.getPublicExponent());
        }
        else
        {
            ECPublicKey pubKey = (ECPublicKey)publicKey;
            ECParameterSpec params = pubKey.getParameters();

            return new ECDSAPublicKey(
                usage,
                ((ECCurve.Fp)params.getCurve()).getQ(),
                ((ECFieldElement.Fp)params.getCurve().getA()).toBigInteger(), ((ECFieldElement.Fp)params.getCurve().getB()).toBigInteger(),
                params.getG().getEncoded(),
                params.getN(),
                pubKey.getQ().getEncoded(),
                params.getH().intValue());
        }
    }
}
