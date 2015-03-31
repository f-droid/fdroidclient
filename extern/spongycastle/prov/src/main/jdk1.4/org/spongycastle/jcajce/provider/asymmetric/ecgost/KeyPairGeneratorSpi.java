package org.spongycastle.jcajce.provider.asymmetric.ecgost;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import org.spongycastle.asn1.cryptopro.ECGOST3410NamedCurves;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.generators.ECKeyPairGenerator;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.crypto.params.ECKeyGenerationParameters;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECNamedCurveGenParameterSpec;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;
import org.spongycastle.jce.spec.ECParameterSpec;

public class KeyPairGeneratorSpi
    extends java.security.KeyPairGenerator
{
    ECParameterSpec ecParams = null;
    ECKeyPairGenerator engine = new ECKeyPairGenerator();

    String algorithm = "ECGOST3410";
    ECKeyGenerationParameters param;
    int strength = 239;
    SecureRandom random = null;
    boolean initialised = false;

    public KeyPairGeneratorSpi()
    {
        super("ECGOST3410");
    }

    public void initialize(
        int strength,
        SecureRandom random)
    {
        this.strength = strength;
        this.random = random;

        if (ecParams != null)
        {
            param = new ECKeyGenerationParameters(new ECDomainParameters(ecParams.getCurve(), ecParams.getG(), ecParams.getN()), random);

            engine.init(param);
            initialised = true;
        }
        else
        {
            throw new InvalidParameterException("unknown key size.");
        }
    }

    public void initialize(
        AlgorithmParameterSpec params,
        SecureRandom random)
        throws InvalidAlgorithmParameterException
    {
        if (params instanceof ECParameterSpec)
        {
            ECParameterSpec p = (ECParameterSpec)params;
            this.ecParams = p;

            param = new ECKeyGenerationParameters(new ECDomainParameters(p.getCurve(), p.getG(), p.getN()), random);

            engine.init(param);
            initialised = true;
        }
        else if (params instanceof ECNamedCurveGenParameterSpec)
        {
            String curveName;

            curveName = ((ECNamedCurveGenParameterSpec)params).getName();

            ECDomainParameters ecP = ECGOST3410NamedCurves.getByName(curveName);
            if (ecP == null)
            {
                throw new InvalidAlgorithmParameterException("unknown curve name: " + curveName);
            }

            this.ecParams = new ECNamedCurveParameterSpec(
                curveName,
                ecP.getCurve(),
                ecP.getG(),
                ecP.getN(),
                ecP.getH(),
                ecP.getSeed());

            param = new ECKeyGenerationParameters(new ECDomainParameters(ecParams.getCurve(), ecParams.getG(), ecParams.getN()), random);

            engine.init(param);
            initialised = true;
        }
        else if (params == null && BouncyCastleProvider.CONFIGURATION.getEcImplicitlyCa() != null)
        {
            ECParameterSpec p = BouncyCastleProvider.CONFIGURATION.getEcImplicitlyCa();
            this.ecParams = null;

            param = new ECKeyGenerationParameters(new ECDomainParameters(p.getCurve(), p.getG(), p.getN()), random);

            engine.init(param);
            initialised = true;
        }
        else if (params == null && BouncyCastleProvider.CONFIGURATION.getEcImplicitlyCa() == null)
        {
            throw new InvalidAlgorithmParameterException("null parameter passed but no implicitCA set");
        }
        else
        {
            throw new InvalidAlgorithmParameterException("parameter object not a ECParameterSpec: " + params.getClass().getName());
        }
    }

    public KeyPair generateKeyPair()
    {
        if (!initialised)
        {
            throw new IllegalStateException("EC Key Pair Generator not initialised");
        }

        AsymmetricCipherKeyPair pair = engine.generateKeyPair();
        ECPublicKeyParameters pub = (ECPublicKeyParameters)pair.getPublic();
        ECPrivateKeyParameters priv = (ECPrivateKeyParameters)pair.getPrivate();

        if (ecParams == null)
        {
            return new KeyPair(new BCECGOST3410PublicKey(algorithm, pub),
                new BCECGOST3410PrivateKey(algorithm, priv));
        }
        else 
        {
            ECParameterSpec p = (ECParameterSpec)ecParams;

            BCECGOST3410PublicKey pubKey = new BCECGOST3410PublicKey(algorithm, pub, p);
            return new KeyPair(pubKey,
                new BCECGOST3410PrivateKey(algorithm, priv, pubKey, p));
        }
    }
}

