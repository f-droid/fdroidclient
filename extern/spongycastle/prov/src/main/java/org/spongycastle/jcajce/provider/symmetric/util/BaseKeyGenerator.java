package org.spongycastle.jcajce.provider.symmetric.util;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.KeyGeneratorSpi;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.spongycastle.crypto.CipherKeyGenerator;
import org.spongycastle.crypto.KeyGenerationParameters;

public class BaseKeyGenerator
    extends KeyGeneratorSpi
{
    protected String                algName;
    protected int                   keySize;
    protected int                   defaultKeySize;
    protected CipherKeyGenerator    engine;

    protected boolean               uninitialised = true;

    protected BaseKeyGenerator(
        String algName,
        int defaultKeySize,
        CipherKeyGenerator engine)
    {
        this.algName = algName;
        this.keySize = this.defaultKeySize = defaultKeySize;
        this.engine = engine;
    }

    protected void engineInit(
        AlgorithmParameterSpec  params,
        SecureRandom            random)
    throws InvalidAlgorithmParameterException
    {
        throw new InvalidAlgorithmParameterException("Not Implemented");
    }

    protected void engineInit(
        SecureRandom    random)
    {
        if (random != null)
        {
            engine.init(new KeyGenerationParameters(random, defaultKeySize));
            uninitialised = false;
        }
    }

    protected void engineInit(
        int             keySize,
        SecureRandom    random)
    {
        try
        {
            if (random == null)
            {
                random = new SecureRandom();
            }
            engine.init(new KeyGenerationParameters(random, keySize));
            uninitialised = false;
        }
        catch (IllegalArgumentException e)
        {
            throw new InvalidParameterException(e.getMessage());
        }
    }

    protected SecretKey engineGenerateKey()
    {
        if (uninitialised)
        {
            engine.init(new KeyGenerationParameters(new SecureRandom(), defaultKeySize));
            uninitialised = false;
        }

        return new SecretKeySpec(engine.generateKey(), algName);
    }
}
