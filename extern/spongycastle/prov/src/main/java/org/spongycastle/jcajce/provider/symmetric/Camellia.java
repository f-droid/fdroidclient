package org.spongycastle.jcajce.provider.symmetric;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.spec.IvParameterSpec;

import org.spongycastle.asn1.ntt.NTTObjectIdentifiers;
import org.spongycastle.crypto.BlockCipher;
import org.spongycastle.crypto.CipherKeyGenerator;
import org.spongycastle.crypto.engines.CamelliaEngine;
import org.spongycastle.crypto.engines.CamelliaWrapEngine;
import org.spongycastle.crypto.engines.RFC3211WrapEngine;
import org.spongycastle.crypto.generators.Poly1305KeyGenerator;
import org.spongycastle.crypto.macs.GMac;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.modes.GCMBlockCipher;
import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.symmetric.util.BaseAlgorithmParameterGenerator;
import org.spongycastle.jcajce.provider.symmetric.util.BaseBlockCipher;
import org.spongycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.spongycastle.jcajce.provider.symmetric.util.BaseMac;
import org.spongycastle.jcajce.provider.symmetric.util.BaseWrapCipher;
import org.spongycastle.jcajce.provider.symmetric.util.BlockCipherProvider;
import org.spongycastle.jcajce.provider.symmetric.util.IvAlgorithmParameters;
import org.spongycastle.jce.provider.BouncyCastleProvider;

public final class Camellia
{
    private Camellia()
    {
    }
    
    public static class ECB
        extends BaseBlockCipher
    {
        public ECB()
        {
            super(new BlockCipherProvider()
            {
                public BlockCipher get()
                {
                    return new CamelliaEngine();
                }
            });
        }
    }

    public static class CBC
       extends BaseBlockCipher
    {
        public CBC()
        {
            super(new CBCBlockCipher(new CamelliaEngine()), 128);
        }
    }

    public static class Wrap
        extends BaseWrapCipher
    {
        public Wrap()
        {
            super(new CamelliaWrapEngine());
        }
    }

    public static class RFC3211Wrap
        extends BaseWrapCipher
    {
        public RFC3211Wrap()
        {
            super(new RFC3211WrapEngine(new CamelliaEngine()), 16);
        }
    }

    public static class GMAC
        extends BaseMac
    {
        public GMAC()
        {
            super(new GMac(new GCMBlockCipher(new CamelliaEngine())));
        }
    }

    public static class Poly1305
        extends BaseMac
    {
        public Poly1305()
        {
            super(new org.spongycastle.crypto.macs.Poly1305(new CamelliaEngine()));
        }
    }

    public static class Poly1305KeyGen
        extends BaseKeyGenerator
    {
        public Poly1305KeyGen()
        {
            super("Poly1305-Camellia", 256, new Poly1305KeyGenerator());
        }
    }

    public static class KeyGen
        extends BaseKeyGenerator
    {
        public KeyGen()
        {
            this(256);
        }

        public KeyGen(int keySize)
        {
            super("Camellia", keySize, new CipherKeyGenerator());
        }
    }

    public static class KeyGen128
        extends KeyGen
    {
        public KeyGen128()
        {
            super(128);
        }
    }

    public static class KeyGen192
        extends KeyGen
    {
        public KeyGen192()
        {
            super(192);
        }
    }

    public static class KeyGen256
        extends KeyGen
    {
        public KeyGen256()
        {
            super(256);
        }
    }

    public static class AlgParamGen
        extends BaseAlgorithmParameterGenerator
    {
        protected void engineInit(
            AlgorithmParameterSpec genParamSpec,
            SecureRandom random)
            throws InvalidAlgorithmParameterException
        {
            throw new InvalidAlgorithmParameterException("No supported AlgorithmParameterSpec for Camellia parameter generation.");
        }

        protected AlgorithmParameters engineGenerateParameters()
        {
            byte[] iv = new byte[16];

            if (random == null)
            {
                random = new SecureRandom();
            }

            random.nextBytes(iv);

            AlgorithmParameters params;

            try
            {
                params = AlgorithmParameters.getInstance("Camellia", BouncyCastleProvider.PROVIDER_NAME);
                params.init(new IvParameterSpec(iv));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e.getMessage());
            }

            return params;
        }
    }

    public static class AlgParams
        extends IvAlgorithmParameters
    {
        protected String engineToString()
        {
            return "Camellia IV";
        }
    }

    public static class Mappings
        extends SymmetricAlgorithmProvider
    {
        private static final String PREFIX = Camellia.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {

            provider.addAlgorithm("AlgorithmParameters.CAMELLIA", PREFIX + "$AlgParams");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + NTTObjectIdentifiers.id_camellia128_cbc, "CAMELLIA");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + NTTObjectIdentifiers.id_camellia192_cbc, "CAMELLIA");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + NTTObjectIdentifiers.id_camellia256_cbc, "CAMELLIA");

            provider.addAlgorithm("AlgorithmParameterGenerator.CAMELLIA", PREFIX + "$AlgParamGen");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator." + NTTObjectIdentifiers.id_camellia128_cbc, "CAMELLIA");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator." + NTTObjectIdentifiers.id_camellia192_cbc, "CAMELLIA");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator." + NTTObjectIdentifiers.id_camellia256_cbc, "CAMELLIA");

            provider.addAlgorithm("Cipher.CAMELLIA", PREFIX + "$ECB");
            provider.addAlgorithm("Cipher." + NTTObjectIdentifiers.id_camellia128_cbc, PREFIX + "$CBC");
            provider.addAlgorithm("Cipher." + NTTObjectIdentifiers.id_camellia192_cbc, PREFIX + "$CBC");
            provider.addAlgorithm("Cipher." + NTTObjectIdentifiers.id_camellia256_cbc, PREFIX + "$CBC");

            provider.addAlgorithm("Cipher.CAMELLIARFC3211WRAP", PREFIX + "$RFC3211Wrap");
            provider.addAlgorithm("Cipher.CAMELLIAWRAP", PREFIX + "$Wrap");
            provider.addAlgorithm("Alg.Alias.Cipher." + NTTObjectIdentifiers.id_camellia128_wrap, "CAMELLIAWRAP");
            provider.addAlgorithm("Alg.Alias.Cipher." + NTTObjectIdentifiers.id_camellia192_wrap, "CAMELLIAWRAP");
            provider.addAlgorithm("Alg.Alias.Cipher." + NTTObjectIdentifiers.id_camellia256_wrap, "CAMELLIAWRAP");

            provider.addAlgorithm("KeyGenerator.CAMELLIA", PREFIX + "$KeyGen");
            provider.addAlgorithm("KeyGenerator." + NTTObjectIdentifiers.id_camellia128_wrap, PREFIX + "$KeyGen128");
            provider.addAlgorithm("KeyGenerator." + NTTObjectIdentifiers.id_camellia192_wrap, PREFIX + "$KeyGen192");
            provider.addAlgorithm("KeyGenerator." + NTTObjectIdentifiers.id_camellia256_wrap, PREFIX + "$KeyGen256");
            provider.addAlgorithm("KeyGenerator." + NTTObjectIdentifiers.id_camellia128_cbc, PREFIX + "$KeyGen128");
            provider.addAlgorithm("KeyGenerator." + NTTObjectIdentifiers.id_camellia192_cbc, PREFIX + "$KeyGen192");
            provider.addAlgorithm("KeyGenerator." + NTTObjectIdentifiers.id_camellia256_cbc, PREFIX + "$KeyGen256");

            addGMacAlgorithm(provider, "CAMELLIA", PREFIX + "$GMAC", PREFIX + "$KeyGen");
            addPoly1305Algorithm(provider, "CAMELLIA", PREFIX + "$Poly1305", PREFIX + "$Poly1305KeyGen");
        }
    }
}
