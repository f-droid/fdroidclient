package org.spongycastle.jcajce.provider.symmetric;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.spec.IvParameterSpec;

import org.spongycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.spongycastle.crypto.BufferedBlockCipher;
import org.spongycastle.crypto.CipherKeyGenerator;
import org.spongycastle.crypto.engines.GOST28147Engine;
import org.spongycastle.crypto.macs.GOST28147Mac;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.modes.GCFBBlockCipher;
import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.symmetric.util.BaseAlgorithmParameterGenerator;
import org.spongycastle.jcajce.provider.symmetric.util.BaseBlockCipher;
import org.spongycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.spongycastle.jcajce.provider.symmetric.util.BaseMac;
import org.spongycastle.jcajce.provider.symmetric.util.IvAlgorithmParameters;
import org.spongycastle.jcajce.provider.util.AlgorithmProvider;
import org.spongycastle.jce.provider.BouncyCastleProvider;

public final class GOST28147
{
    private GOST28147()
    {
    }
    
    public static class ECB
        extends BaseBlockCipher
    {
        public ECB()
        {
            super(new GOST28147Engine());
        }
    }

    public static class CBC
       extends BaseBlockCipher
    {
        public CBC()
        {
            super(new CBCBlockCipher(new GOST28147Engine()), 64);
        }
    }

    public static class GCFB
       extends BaseBlockCipher
    {
        public GCFB()
        {
            super(new BufferedBlockCipher(new GCFBBlockCipher(new GOST28147Engine())), 64);
        }
    }

    /**
     * GOST28147
     */
    public static class Mac
        extends BaseMac
    {
        public Mac()
        {
            super(new GOST28147Mac());
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
            super("GOST28147", keySize, new CipherKeyGenerator());
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
            throw new InvalidAlgorithmParameterException("No supported AlgorithmParameterSpec for AES parameter generation.");
        }

        protected AlgorithmParameters engineGenerateParameters()
        {
            byte[]  iv = new byte[16];

            if (random == null)
            {
                random = new SecureRandom();
            }

            random.nextBytes(iv);

            AlgorithmParameters params;

            try
            {
                params = AlgorithmParameters.getInstance("GOST28147", BouncyCastleProvider.PROVIDER_NAME);
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
            return "GOST IV";
        }
    }

    public static class Mappings
        extends AlgorithmProvider
    {
        private static final String PREFIX = GOST28147.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {
            provider.addAlgorithm("Cipher.GOST28147", PREFIX + "$ECB");
            provider.addAlgorithm("Alg.Alias.Cipher.GOST", "GOST28147");
            provider.addAlgorithm("Alg.Alias.Cipher.GOST-28147", "GOST28147");
            provider.addAlgorithm("Cipher." + CryptoProObjectIdentifiers.gostR28147_gcfb, PREFIX + "$GCFB");

            provider.addAlgorithm("KeyGenerator.GOST28147", PREFIX + "$KeyGen");
            provider.addAlgorithm("Alg.Alias.KeyGenerator.GOST", "GOST28147");
            provider.addAlgorithm("Alg.Alias.KeyGenerator.GOST-28147", "GOST28147");
            provider.addAlgorithm("Alg.Alias.KeyGenerator." + CryptoProObjectIdentifiers.gostR28147_gcfb, "GOST28147");

            provider.addAlgorithm("Mac.GOST28147MAC", PREFIX + "$Mac");
            provider.addAlgorithm("Alg.Alias.Mac.GOST28147", "GOST28147MAC");
        }
    }
}
