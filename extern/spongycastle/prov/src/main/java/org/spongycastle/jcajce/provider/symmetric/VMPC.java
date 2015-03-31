package org.spongycastle.jcajce.provider.symmetric;

import org.spongycastle.crypto.CipherKeyGenerator;
import org.spongycastle.crypto.engines.VMPCEngine;
import org.spongycastle.crypto.macs.VMPCMac;
import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.spongycastle.jcajce.provider.symmetric.util.BaseMac;
import org.spongycastle.jcajce.provider.symmetric.util.BaseStreamCipher;
import org.spongycastle.jcajce.provider.util.AlgorithmProvider;

public final class VMPC
{
    private VMPC()
    {
    }
    
    public static class Base
        extends BaseStreamCipher
    {
        public Base()
        {
            super(new VMPCEngine(), 16);
        }
    }

    public static class KeyGen
        extends BaseKeyGenerator
    {
        public KeyGen()
        {
            super("VMPC", 128, new CipherKeyGenerator());
        }
    }

    public static class Mac
        extends BaseMac
    {
        public Mac()
        {
            super(new VMPCMac());
        }
    }

    public static class Mappings
        extends AlgorithmProvider
    {
        private static final String PREFIX = VMPC.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {

            provider.addAlgorithm("Cipher.VMPC", PREFIX + "$Base");
            provider.addAlgorithm("KeyGenerator.VMPC", PREFIX + "$KeyGen");
            provider.addAlgorithm("Mac.VMPCMAC", PREFIX + "$Mac");
            provider.addAlgorithm("Alg.Alias.Mac.VMPC", "VMPCMAC");
            provider.addAlgorithm("Alg.Alias.Mac.VMPC-MAC", "VMPCMAC");

        }
    }
}
