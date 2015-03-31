package java.security;

import java.security.spec.AlgorithmParameterSpec;

public class AlgorithmParameterGenerator
{
    AlgorithmParameterGeneratorSpi      spi;
    Provider                            provider;
    String                              algorithm; 

    protected AlgorithmParameterGenerator(
        AlgorithmParameterGeneratorSpi paramGenSpi,
        Provider provider,
        String algorithm) 
    {
        this.spi = paramGenSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    public final AlgorithmParameters generateParameters() 
    {
        return spi.engineGenerateParameters();
    }

    public final String getAlgorithm()
    {
        return algorithm;
    }

    public static AlgorithmParameterGenerator getInstance(String algorithm)
        throws NoSuchAlgorithmException
    {
        try
        {
            SecurityUtil.Implementation  imp = SecurityUtil.getImplementation("AlgorithmParameterGenerator", algorithm, null);

            if (imp != null)
            {
                return new AlgorithmParameterGenerator((AlgorithmParameterGeneratorSpi)imp.getEngine(), imp.getProvider(), algorithm);
            }

            throw new NoSuchAlgorithmException("can't find algorithm " + algorithm);
        }
        catch (NoSuchProviderException e)
        {
            throw new NoSuchAlgorithmException(algorithm + " not found");
        }
    }

    public static AlgorithmParameterGenerator getInstance(String algorithm, String provider)
        throws NoSuchAlgorithmException, NoSuchProviderException
    {
        SecurityUtil.Implementation  imp = SecurityUtil.getImplementation("AlgorithmParameterGenerator", algorithm, provider);

        if (imp != null)
        {
            return new AlgorithmParameterGenerator((AlgorithmParameterGeneratorSpi)imp.getEngine(), imp.getProvider(), algorithm);
        }

        throw new NoSuchAlgorithmException("can't find algorithm " + algorithm);
    }

    public final Provider getProvider() 
    {
        return provider;
    }

    public final void init(
        AlgorithmParameterSpec genParamSpec) 
        throws InvalidAlgorithmParameterException
    {
        spi.engineInit(genParamSpec, new SecureRandom());
    }

    public final void init(
        AlgorithmParameterSpec genParamSpec,
        SecureRandom random) 
        throws InvalidAlgorithmParameterException
    {
        spi.engineInit(genParamSpec, random);
    }

    public final void init(
        int size) 
    {
        spi.engineInit(size, new SecureRandom());
    }

    public final void init(
        int size,
        SecureRandom random) 
    {
        spi.engineInit(size, random);
    }
}
