package org.spongycastle.jcajce.provider.asymmetric.util;

import org.spongycastle.crypto.engines.IESEngine;
import org.spongycastle.jce.spec.IESParameterSpec;

public class IESUtil
{
    public static IESParameterSpec guessParameterSpec(IESEngine engine)
    {
        if (engine.getCipher() == null)
        {
            return new IESParameterSpec(null, null, 128);
        }
        else if (engine.getCipher().getUnderlyingCipher().getAlgorithmName().equals("DES") ||
                engine.getCipher().getUnderlyingCipher().getAlgorithmName().equals("RC2") ||
                engine.getCipher().getUnderlyingCipher().getAlgorithmName().equals("RC5-32") ||
                engine.getCipher().getUnderlyingCipher().getAlgorithmName().equals("RC5-64"))
        {
            return new IESParameterSpec(null, null, 64, 64);
        }
        else if (engine.getCipher().getUnderlyingCipher().getAlgorithmName().equals("SKIPJACK"))
        {
            return new IESParameterSpec(null, null, 80, 80);
        }
        else if (engine.getCipher().getUnderlyingCipher().getAlgorithmName().equals("GOST28147"))
        {
            return new IESParameterSpec(null, null, 256, 256);
        }

        return new IESParameterSpec(null, null, 128, 128);
    }
}
