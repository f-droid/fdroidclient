package org.spongycastle.jcajce.provider.asymmetric;

import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.util.AsymmetricAlgorithmProvider;

public class IES
{
    private static final String PREFIX = "org.spongycastle.jcajce.provider.asymmetric" + ".ies.";

    public static class Mappings
        extends AsymmetricAlgorithmProvider
    {
        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {
            provider.addAlgorithm("AlgorithmParameters.IES", PREFIX + "AlgorithmParametersSpi");
            provider.addAlgorithm("Cipher.IES", PREFIX + "CipherSpi$IES");
        }
    }
}
