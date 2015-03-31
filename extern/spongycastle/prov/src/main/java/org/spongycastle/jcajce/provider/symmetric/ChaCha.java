package org.spongycastle.jcajce.provider.symmetric;

import org.spongycastle.crypto.CipherKeyGenerator;
import org.spongycastle.crypto.engines.ChaChaEngine;
import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.spongycastle.jcajce.provider.symmetric.util.BaseStreamCipher;
import org.spongycastle.jcajce.provider.util.AlgorithmProvider;

public final class ChaCha
{
    private ChaCha()
    {
    }
    
    public static class Base
        extends BaseStreamCipher
    {
        public Base()
        {
            super(new ChaChaEngine(), 8);
        }
    }

    public static class KeyGen
        extends BaseKeyGenerator
    {
        public KeyGen()
        {
            super("ChaCha", 128, new CipherKeyGenerator());
        }
    }

    public static class Mappings
        extends AlgorithmProvider
    {
        private static final String PREFIX = ChaCha.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {

            provider.addAlgorithm("Cipher.CHACHA", PREFIX + "$Base");
            provider.addAlgorithm("KeyGenerator.CHACHA", PREFIX + "$KeyGen");

        }
    }
}
