package org.spongycastle.jcajce.provider.symmetric;

import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.util.AlgorithmProvider;

abstract class SymmetricAlgorithmProvider
    extends AlgorithmProvider
{
    protected void addGMacAlgorithm(
        ConfigurableProvider provider,
        String algorithm,
        String algorithmClassName,
        String keyGeneratorClassName)
    {
        provider.addAlgorithm("Mac." + algorithm + "-GMAC", algorithmClassName);
        provider.addAlgorithm("Alg.Alias.Mac." + algorithm + "GMAC", algorithm + "-GMAC");

        provider.addAlgorithm("KeyGenerator." + algorithm + "-GMAC", keyGeneratorClassName);
        provider.addAlgorithm("Alg.Alias.KeyGenerator." + algorithm + "GMAC",  algorithm + "-GMAC");
    }

    protected void addPoly1305Algorithm(ConfigurableProvider provider,
                                        String algorithm,
                                        String algorithmClassName,
                                        String keyGeneratorClassName)
    {
        provider.addAlgorithm("Mac.POLY1305-" + algorithm, algorithmClassName);
        provider.addAlgorithm("Alg.Alias.Mac.POLY1305" + algorithm, "POLY1305-" + algorithm);

        provider.addAlgorithm("KeyGenerator.POLY1305-" + algorithm, keyGeneratorClassName);
        provider.addAlgorithm("Alg.Alias.KeyGenerator.POLY1305" + algorithm, "POLY1305-" + algorithm);
    }

}
