package org.spongycastle.jcajce.provider.symmetric;

import org.spongycastle.crypto.CipherKeyGenerator;
import org.spongycastle.crypto.engines.BlowfishEngine;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.symmetric.util.BaseBlockCipher;
import org.spongycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.spongycastle.jcajce.provider.symmetric.util.IvAlgorithmParameters;
import org.spongycastle.jcajce.provider.util.AlgorithmProvider;

public final class Blowfish
{
    private Blowfish()
    {
    }
    
    public static class ECB
        extends BaseBlockCipher
    {
        public ECB()
        {
            super(new BlowfishEngine());
        }
    }

    public static class CBC
        extends BaseBlockCipher
    {
        public CBC()
        {
            super(new CBCBlockCipher(new BlowfishEngine()), 64);
        }
    }

    public static class KeyGen
        extends BaseKeyGenerator
    {
        public KeyGen()
        {
            super("Blowfish", 128, new CipherKeyGenerator());
        }
    }

    public static class AlgParams
        extends IvAlgorithmParameters
    {
        protected String engineToString()
        {
            return "Blowfish IV";
        }
    }

    public static class Mappings
        extends AlgorithmProvider
    {
        private static final String PREFIX = Blowfish.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {

            provider.addAlgorithm("Cipher.BLOWFISH", PREFIX + "$ECB");
            provider.addAlgorithm("Cipher.1.3.6.1.4.1.3029.1.2", PREFIX + "$CBC");
            provider.addAlgorithm("KeyGenerator.BLOWFISH", PREFIX + "$KeyGen");
            provider.addAlgorithm("Alg.Alias.KeyGenerator.1.3.6.1.4.1.3029.1.2", "BLOWFISH");
            provider.addAlgorithm("AlgorithmParameters.BLOWFISH", PREFIX + "$AlgParams");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.1.3.6.1.4.1.3029.1.2", "BLOWFISH");

        }
    }
}
