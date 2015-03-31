package org.spongycastle.jcajce.provider.asymmetric.dsa;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.DSAParameterSpec;

import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.generators.DSAKeyPairGenerator;
import org.spongycastle.crypto.generators.DSAParametersGenerator;
import org.spongycastle.crypto.params.DSAKeyGenerationParameters;
import org.spongycastle.crypto.params.DSAParameters;
import org.spongycastle.crypto.params.DSAPrivateKeyParameters;
import org.spongycastle.crypto.params.DSAPublicKeyParameters;

public class KeyPairGeneratorSpi
    extends java.security.KeyPairGenerator
{
    DSAKeyGenerationParameters param;
    DSAKeyPairGenerator engine = new DSAKeyPairGenerator();
    int strength = 1024;
    int certainty = 20;
    SecureRandom random = new SecureRandom();
    boolean initialised = false;

    public KeyPairGeneratorSpi()
    {
        super("DSA");
    }

    public void initialize(
        int strength,
        SecureRandom random)
    {
        if (strength < 512 || strength > 1024 || strength % 64 != 0)
        {
            throw new InvalidParameterException("strength must be from 512 - 1024 and a multiple of 64");
        }

        this.strength = strength;
        this.random = random;
    }

    public void initialize(
        AlgorithmParameterSpec params,
        SecureRandom random)
        throws InvalidAlgorithmParameterException
    {
        if (!(params instanceof DSAParameterSpec))
        {
            throw new InvalidAlgorithmParameterException("parameter object not a DSAParameterSpec");
        }
        DSAParameterSpec dsaParams = (DSAParameterSpec)params;

        param = new DSAKeyGenerationParameters(random, new DSAParameters(dsaParams.getP(), dsaParams.getQ(), dsaParams.getG()));

        engine.init(param);
        initialised = true;
    }

    public KeyPair generateKeyPair()
    {
        if (!initialised)
        {
            DSAParametersGenerator pGen = new DSAParametersGenerator();

            pGen.init(strength, certainty, random);
            param = new DSAKeyGenerationParameters(random, pGen.generateParameters());
            engine.init(param);
            initialised = true;
        }

        AsymmetricCipherKeyPair pair = engine.generateKeyPair();
        DSAPublicKeyParameters pub = (DSAPublicKeyParameters)pair.getPublic();
        DSAPrivateKeyParameters priv = (DSAPrivateKeyParameters)pair.getPrivate();

        return new KeyPair(new BCDSAPublicKey(pub),
            new BCDSAPrivateKey(priv));
    }
}
