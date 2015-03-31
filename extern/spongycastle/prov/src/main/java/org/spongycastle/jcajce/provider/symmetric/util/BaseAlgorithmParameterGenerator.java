package org.spongycastle.jcajce.provider.symmetric.util;

import java.security.AlgorithmParameterGeneratorSpi;
import java.security.SecureRandom;

public abstract class BaseAlgorithmParameterGenerator
    extends AlgorithmParameterGeneratorSpi
{
    protected SecureRandom  random;
    protected int           strength = 1024;

    protected void engineInit(
        int             strength,
        SecureRandom    random)
    {
        this.strength = strength;
        this.random = random;
    }
}
