package org.spongycastle.jcajce.provider.symmetric;

import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.crypto.CipherKeyGenerator;
import org.spongycastle.crypto.engines.RC4Engine;
import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.spongycastle.jcajce.provider.symmetric.util.BaseStreamCipher;
import org.spongycastle.jcajce.provider.symmetric.util.PBESecretKeyFactory;
import org.spongycastle.jcajce.provider.util.AlgorithmProvider;

public final class ARC4
{
    private ARC4()
    {
    }
    
    public static class Base
        extends BaseStreamCipher
    {
        public Base()
        {
            super(new RC4Engine(), 0);
        }
    }

    public static class KeyGen
        extends BaseKeyGenerator
    {
        public KeyGen()
        {
            super("RC4", 128, new CipherKeyGenerator());
        }
    }

    /**
     * PBEWithSHAAnd128BitRC4
     */
    static public class PBEWithSHAAnd128BitKeyFactory
        extends PBESecretKeyFactory
    {
        public PBEWithSHAAnd128BitKeyFactory()
        {
            super("PBEWithSHAAnd128BitRC4", PKCSObjectIdentifiers.pbeWithSHAAnd128BitRC4, true, PKCS12, SHA1, 128, 0);
        }
    }

    /**
     * PBEWithSHAAnd40BitRC4
     */
    static public class PBEWithSHAAnd40BitKeyFactory
        extends PBESecretKeyFactory
    {
        public PBEWithSHAAnd40BitKeyFactory()
        {
            super("PBEWithSHAAnd128BitRC4", PKCSObjectIdentifiers.pbeWithSHAAnd128BitRC4, true, PKCS12, SHA1, 40, 0);
        }
    }


    /**
     * PBEWithSHAAnd128BitRC4
     */
    static public class PBEWithSHAAnd128Bit
        extends BaseStreamCipher
    {
        public PBEWithSHAAnd128Bit()
        {
            super(new RC4Engine(), 0);
        }
    }

    /**
     * PBEWithSHAAnd40BitRC4
     */
    static public class PBEWithSHAAnd40Bit
        extends BaseStreamCipher
    {
        public PBEWithSHAAnd40Bit()
        {
            super(new RC4Engine(), 0);
        }
    }

    public static class Mappings
        extends AlgorithmProvider
    {
        private static final String PREFIX = ARC4.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {
            provider.addAlgorithm("Cipher.ARC4", PREFIX + "$Base");
            provider.addAlgorithm("Alg.Alias.Cipher." + PKCSObjectIdentifiers.rc4, "ARC4");
            provider.addAlgorithm("Alg.Alias.Cipher.ARCFOUR", "ARC4");
            provider.addAlgorithm("Alg.Alias.Cipher.RC4", "ARC4");
            provider.addAlgorithm("KeyGenerator.ARC4", PREFIX + "$KeyGen");
            provider.addAlgorithm("Alg.Alias.KeyGenerator.RC4", "ARC4");
            provider.addAlgorithm("Alg.Alias.KeyGenerator.1.2.840.113549.3.4", "ARC4");
            provider.addAlgorithm("SecretKeyFactory.PBEWITHSHAAND128BITRC4", PREFIX + "$PBEWithSHAAnd128BitKeyFactory");
            provider.addAlgorithm("SecretKeyFactory.PBEWITHSHAAND40BITRC4", PREFIX + "$PBEWithSHAAnd40BitKeyFactory");

            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + PKCSObjectIdentifiers.pbeWithSHAAnd128BitRC4, "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + PKCSObjectIdentifiers.pbeWithSHAAnd40BitRC4, "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAAND40BITRC4", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAAND128BITRC4", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAANDRC4", "PKCS12PBE");
            provider.addAlgorithm("Cipher.PBEWITHSHAAND128BITRC4", PREFIX + "$PBEWithSHAAnd128Bit");
            provider.addAlgorithm("Cipher.PBEWITHSHAAND40BITRC4", PREFIX + "$PBEWithSHAAnd40Bit");

            provider.addAlgorithm("Alg.Alias.SecretKeyFactory." + PKCSObjectIdentifiers.pbeWithSHAAnd128BitRC4, "PBEWITHSHAAND128BITRC4");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory." + PKCSObjectIdentifiers.pbeWithSHAAnd40BitRC4, "PBEWITHSHAAND40BITRC4");

            provider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA1AND128BITRC4", "PBEWITHSHAAND128BITRC4");
            provider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA1AND40BITRC4", "PBEWITHSHAAND40BITRC4");

            provider.addAlgorithm("Alg.Alias.Cipher." + PKCSObjectIdentifiers.pbeWithSHAAnd128BitRC4, "PBEWITHSHAAND128BITRC4");
            provider.addAlgorithm("Alg.Alias.Cipher." + PKCSObjectIdentifiers.pbeWithSHAAnd40BitRC4, "PBEWITHSHAAND40BITRC4");
        }
    }
}
