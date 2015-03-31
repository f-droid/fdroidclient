package org.spongycastle.jcajce.provider.symmetric;

import org.spongycastle.crypto.CipherKeyGenerator;
import org.spongycastle.crypto.engines.Grainv1Engine;
import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.spongycastle.jcajce.provider.symmetric.util.BaseStreamCipher;
import org.spongycastle.jcajce.provider.util.AlgorithmProvider;

public final class Grainv1
{
    private Grainv1()
    {
    }
    
    public static class Base
        extends BaseStreamCipher
    {
        public Base()
        {
            super(new Grainv1Engine(), 8);
        }
    }

    public static class KeyGen
        extends BaseKeyGenerator
    {
        public KeyGen()
        {
            super("Grainv1", 80, new CipherKeyGenerator());
        }
    }

    public static class Mappings
        extends AlgorithmProvider
    {
        private static final String PREFIX = Grainv1.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {
            provider.addAlgorithm("Cipher.Grainv1", PREFIX + "$Base");
            provider.addAlgorithm("KeyGenerator.Grainv1", PREFIX + "$KeyGen");
        }
    }
}
