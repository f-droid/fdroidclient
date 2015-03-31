package org.spongycastle.jcajce.provider.asymmetric.gost;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import org.spongycastle.crypto.generators.GOST3410ParametersGenerator;
import org.spongycastle.crypto.params.GOST3410Parameters;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.GOST3410ParameterSpec;
import org.spongycastle.jce.spec.GOST3410PublicKeyParameterSetSpec;

public abstract class AlgorithmParameterGeneratorSpi
    extends java.security.AlgorithmParameterGeneratorSpi
{
    protected SecureRandom random;
    protected int strength = 1024;

    protected void engineInit(
        int strength,
        SecureRandom random)
    {
        this.strength = strength;
        this.random = random;
    }

    protected void engineInit(
        AlgorithmParameterSpec genParamSpec,
        SecureRandom random)
        throws InvalidAlgorithmParameterException
    {
        throw new InvalidAlgorithmParameterException("No supported AlgorithmParameterSpec for GOST3410 parameter generation.");
    }

    protected AlgorithmParameters engineGenerateParameters()
    {
        GOST3410ParametersGenerator pGen = new GOST3410ParametersGenerator();

        if (random != null)
        {
            pGen.init(strength, 2, random);
        }
        else
        {
            pGen.init(strength, 2, new SecureRandom());
        }

        GOST3410Parameters p = pGen.generateParameters();

        AlgorithmParameters params;

        try
        {
            params = AlgorithmParameters.getInstance("GOST3410", BouncyCastleProvider.PROVIDER_NAME);
            params.init(new GOST3410ParameterSpec(new GOST3410PublicKeyParameterSetSpec(p.getP(), p.getQ(), p.getA())));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage());
        }

        return params;
    }
}
