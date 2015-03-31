package java.security;

import java.security.spec.AlgorithmParameterSpec;

public abstract class AlgorithmParameterGeneratorSpi
{
    public AlgorithmParameterGeneratorSpi()
    {
    }

    protected abstract AlgorithmParameters engineGenerateParameters();
   
    protected abstract void engineInit(AlgorithmParameterSpec genParamSpec, SecureRandom random) throws InvalidAlgorithmParameterException;

    protected abstract void engineInit(int size, SecureRandom random);
}
