package org.spongycastle.jcajce.provider.symmetric;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.crypto.KeyGenerationParameters;
import org.spongycastle.crypto.engines.DESedeEngine;
import org.spongycastle.crypto.engines.DESedeWrapEngine;
import org.spongycastle.crypto.engines.RFC3211WrapEngine;
import org.spongycastle.crypto.generators.DESedeKeyGenerator;
import org.spongycastle.crypto.macs.CBCBlockCipherMac;
import org.spongycastle.crypto.macs.CFBBlockCipherMac;
import org.spongycastle.crypto.macs.CMac;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.ISO7816d4Padding;
import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.symmetric.util.BaseAlgorithmParameterGenerator;
import org.spongycastle.jcajce.provider.symmetric.util.BaseBlockCipher;
import org.spongycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.spongycastle.jcajce.provider.symmetric.util.BaseMac;
import org.spongycastle.jcajce.provider.symmetric.util.BaseSecretKeyFactory;
import org.spongycastle.jcajce.provider.symmetric.util.BaseWrapCipher;
import org.spongycastle.jcajce.provider.util.AlgorithmProvider;
import org.spongycastle.jce.provider.BouncyCastleProvider;

public final class DESede
{
    private DESede()
    {
    }

    static public class ECB
        extends BaseBlockCipher
    {
        public ECB()
        {
            super(new DESedeEngine());
        }
    }

    static public class CBC
        extends BaseBlockCipher
    {
        public CBC()
        {
            super(new CBCBlockCipher(new DESedeEngine()), 64);
        }
    }

    /**
     * DESede   CFB8
     */
    public static class DESedeCFB8
        extends BaseMac
    {
        public DESedeCFB8()
        {
            super(new CFBBlockCipherMac(new DESedeEngine()));
        }
    }

    /**
     * DESede64
     */
    public static class DESede64
        extends BaseMac
    {
        public DESede64()
        {
            super(new CBCBlockCipherMac(new DESedeEngine(), 64));
        }
    }

    /**
     * DESede64with7816-4Padding
     */
    public static class DESede64with7816d4
        extends BaseMac
    {
        public DESede64with7816d4()
        {
            super(new CBCBlockCipherMac(new DESedeEngine(), 64, new ISO7816d4Padding()));
        }
    }
    
    public static class CBCMAC
        extends BaseMac
    {
        public CBCMAC()
        {
            super(new CBCBlockCipherMac(new DESedeEngine()));
        }
    }

    static public class CMAC
        extends BaseMac
    {
        public CMAC()
        {
            super(new CMac(new DESedeEngine()));
        }
    }

    public static class Wrap
        extends BaseWrapCipher
    {
        public Wrap()
        {
            super(new DESedeWrapEngine());
        }
    }

    public static class RFC3211
        extends BaseWrapCipher
    {
        public RFC3211()
        {
            super(new RFC3211WrapEngine(new DESedeEngine()), 8);
        }
    }

  /**
     * DESede - the default for this is to generate a key in
     * a-b-a format that's 24 bytes long but has 16 bytes of
     * key material (the first 8 bytes is repeated as the last
     * 8 bytes). If you give it a size, you'll get just what you
     * asked for.
     */
    public static class KeyGenerator
        extends BaseKeyGenerator
    {
        private boolean     keySizeSet = false;

        public KeyGenerator()
        {
            super("DESede", 192, new DESedeKeyGenerator());
        }

        protected void engineInit(
            int             keySize,
            SecureRandom random)
        {
            super.engineInit(keySize, random);
            keySizeSet = true;
        }

        protected SecretKey engineGenerateKey()
        {
            if (uninitialised)
            {
                engine.init(new KeyGenerationParameters(new SecureRandom(), defaultKeySize));
                uninitialised = false;
            }

            //
            // if no key size has been defined generate a 24 byte key in
            // the a-b-a format
            //
            if (!keySizeSet)
            {
                byte[]     k = engine.generateKey();

                System.arraycopy(k, 0, k, 16, 8);

                return new SecretKeySpec(k, algName);
            }
            else
            {
                return new SecretKeySpec(engine.generateKey(), algName);
            }
        }
    }

    /**
     * generate a desEDE key in the a-b-c format.
     */
    public static class KeyGenerator3
        extends BaseKeyGenerator
    {
        public KeyGenerator3()
        {
            super("DESede3", 192, new DESedeKeyGenerator());
        }
    }

    /**
     * PBEWithSHAAnd3-KeyTripleDES-CBC
     */
    static public class PBEWithSHAAndDES3Key
        extends BaseBlockCipher
    {
        public PBEWithSHAAndDES3Key()
        {
            super(new CBCBlockCipher(new DESedeEngine()));
        }
    }

    /**
     * PBEWithSHAAnd2-KeyTripleDES-CBC
     */
    static public class PBEWithSHAAndDES2Key
        extends BaseBlockCipher
    {
        public PBEWithSHAAndDES2Key()
        {
            super(new CBCBlockCipher(new DESedeEngine()));
        }
    }

    /**
     * PBEWithSHAAnd3-KeyTripleDES-CBC
     */
    static public class PBEWithSHAAndDES3KeyFactory
        extends DES.DESPBEKeyFactory
    {
        public PBEWithSHAAndDES3KeyFactory()
        {
            super("PBEwithSHAandDES3Key-CBC", PKCSObjectIdentifiers.pbeWithSHAAnd3_KeyTripleDES_CBC, true, PKCS12, SHA1, 192, 64);
        }
    }

    /**
     * PBEWithSHAAnd2-KeyTripleDES-CBC
     */
    static public class PBEWithSHAAndDES2KeyFactory
        extends DES.DESPBEKeyFactory
    {
        public PBEWithSHAAndDES2KeyFactory()
        {
            super("PBEwithSHAandDES2Key-CBC", PKCSObjectIdentifiers.pbeWithSHAAnd2_KeyTripleDES_CBC, true, PKCS12, SHA1, 128, 64);
        }
    }

    public static class AlgParamGen
        extends BaseAlgorithmParameterGenerator
    {
        protected void engineInit(
            AlgorithmParameterSpec genParamSpec,
            SecureRandom            random)
            throws InvalidAlgorithmParameterException
        {
            throw new InvalidAlgorithmParameterException("No supported AlgorithmParameterSpec for DES parameter generation.");
        }

        protected AlgorithmParameters engineGenerateParameters()
        {
            byte[]  iv = new byte[8];

            if (random == null)
            {
                random = new SecureRandom();
            }

            random.nextBytes(iv);

            AlgorithmParameters params;

            try
            {
                params = AlgorithmParameters.getInstance("DES", BouncyCastleProvider.PROVIDER_NAME);
                params.init(new IvParameterSpec(iv));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e.getMessage());
            }

            return params;
        }
    }

    static public class KeyFactory
        extends BaseSecretKeyFactory
    {
        public KeyFactory()
        {
            super("DESede", null);
        }

        protected KeySpec engineGetKeySpec(
            SecretKey key,
            Class keySpec)
        throws InvalidKeySpecException
        {
            if (keySpec == null)
            {
                throw new InvalidKeySpecException("keySpec parameter is null");
            }
            if (key == null)
            {
                throw new InvalidKeySpecException("key parameter is null");
            }

            if (SecretKeySpec.class.isAssignableFrom(keySpec))
            {
                return new SecretKeySpec(key.getEncoded(), algName);
            }
            else if (DESedeKeySpec.class.isAssignableFrom(keySpec))
            {
                byte[]  bytes = key.getEncoded();

                try
                {
                    if (bytes.length == 16)
                    {
                        byte[]  longKey = new byte[24];

                        System.arraycopy(bytes, 0, longKey, 0, 16);
                        System.arraycopy(bytes, 0, longKey, 16, 8);

                        return new DESedeKeySpec(longKey);
                    }
                    else
                    {
                        return new DESedeKeySpec(bytes);
                    }
                }
                catch (Exception e)
                {
                    throw new InvalidKeySpecException(e.toString());
                }
            }

            throw new InvalidKeySpecException("Invalid KeySpec");
        }

        protected SecretKey engineGenerateSecret(
            KeySpec keySpec)
        throws InvalidKeySpecException
        {
            if (keySpec instanceof DESedeKeySpec)
            {
                DESedeKeySpec desKeySpec = (DESedeKeySpec)keySpec;
                return new SecretKeySpec(desKeySpec.getKey(), "DESede");
            }

            return super.engineGenerateSecret(keySpec);
        }
    }

    public static class Mappings
        extends AlgorithmProvider
    {
        private static final String PREFIX = DESede.class.getName();
        private static final String PACKAGE = "org.spongycastle.jcajce.provider.symmetric"; // JDK 1.2
                
        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {
            provider.addAlgorithm("Cipher.DESEDE", PREFIX + "$ECB");
            provider.addAlgorithm("Cipher." + PKCSObjectIdentifiers.des_EDE3_CBC, PREFIX + "$CBC");
            provider.addAlgorithm("Cipher.DESEDEWRAP", PREFIX + "$Wrap");
            provider.addAlgorithm("Cipher." + PKCSObjectIdentifiers.id_alg_CMS3DESwrap, PREFIX + "$Wrap");
            provider.addAlgorithm("Cipher.DESEDERFC3211WRAP", PREFIX + "$RFC3211");

            provider.addAlgorithm("Alg.Alias.Cipher.TDEA", "DESEDE");
            provider.addAlgorithm("Alg.Alias.Cipher.TDEAWRAP", "DESEDEWRAP");
            provider.addAlgorithm("Alg.Alias.KeyGenerator.TDEA", "DESEDE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.TDEA", "DESEDE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator.TDEA", "DESEDE");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.TDEA", "DESEDE");

            if (provider.hasAlgorithm("MessageDigest", "SHA-1"))
            {
                provider.addAlgorithm("Cipher.PBEWITHSHAAND3-KEYTRIPLEDES-CBC", PREFIX + "$PBEWithSHAAndDES3Key");
                provider.addAlgorithm("Cipher.BROKENPBEWITHSHAAND3-KEYTRIPLEDES-CBC", PREFIX + "$BrokePBEWithSHAAndDES3Key");
                provider.addAlgorithm("Cipher.OLDPBEWITHSHAAND3-KEYTRIPLEDES-CBC", PREFIX + "$OldPBEWithSHAAndDES3Key");
                provider.addAlgorithm("Cipher.PBEWITHSHAAND2-KEYTRIPLEDES-CBC", PREFIX + "$PBEWithSHAAndDES2Key");
                provider.addAlgorithm("Cipher.BROKENPBEWITHSHAAND2-KEYTRIPLEDES-CBC", PREFIX + "$BrokePBEWithSHAAndDES2Key");
                provider.addAlgorithm("Alg.Alias.Cipher." + PKCSObjectIdentifiers.pbeWithSHAAnd3_KeyTripleDES_CBC, "PBEWITHSHAAND3-KEYTRIPLEDES-CBC");
                provider.addAlgorithm("Alg.Alias.Cipher." + PKCSObjectIdentifiers.pbeWithSHAAnd2_KeyTripleDES_CBC, "PBEWITHSHAAND2-KEYTRIPLEDES-CBC");
                provider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA1ANDDESEDE", "PBEWITHSHAAND3-KEYTRIPLEDES-CBC");
                provider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA1AND3-KEYTRIPLEDES-CBC", "PBEWITHSHAAND3-KEYTRIPLEDES-CBC");
                provider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA1AND2-KEYTRIPLEDES-CBC", "PBEWITHSHAAND2-KEYTRIPLEDES-CBC");
            }

            provider.addAlgorithm("KeyGenerator.DESEDE", PREFIX + "$KeyGenerator");
            provider.addAlgorithm("KeyGenerator." + PKCSObjectIdentifiers.des_EDE3_CBC, PREFIX + "$KeyGenerator3");
            provider.addAlgorithm("KeyGenerator.DESEDEWRAP", PREFIX + "$KeyGenerator");

            provider.addAlgorithm("SecretKeyFactory.DESEDE", PREFIX + "$KeyFactory");

            provider.addAlgorithm("Mac.DESEDECMAC", PREFIX + "$CMAC");
            provider.addAlgorithm("Mac.DESEDEMAC", PREFIX + "$CBCMAC");
            provider.addAlgorithm("Alg.Alias.Mac.DESEDE", "DESEDEMAC");

            provider.addAlgorithm("Mac.DESEDEMAC/CFB8", PREFIX + "$DESedeCFB8");
            provider.addAlgorithm("Alg.Alias.Mac.DESEDE/CFB8", "DESEDEMAC/CFB8");

            provider.addAlgorithm("Mac.DESEDEMAC64", PREFIX + "$DESede64");
            provider.addAlgorithm("Alg.Alias.Mac.DESEDE64", "DESEDEMAC64");

            provider.addAlgorithm("Mac.DESEDEMAC64WITHISO7816-4PADDING", PREFIX + "$DESede64with7816d4");
            provider.addAlgorithm("Alg.Alias.Mac.DESEDE64WITHISO7816-4PADDING", "DESEDEMAC64WITHISO7816-4PADDING");
            provider.addAlgorithm("Alg.Alias.Mac.DESEDEISO9797ALG1MACWITHISO7816-4PADDING", "DESEDEMAC64WITHISO7816-4PADDING");
            provider.addAlgorithm("Alg.Alias.Mac.DESEDEISO9797ALG1WITHISO7816-4PADDING", "DESEDEMAC64WITHISO7816-4PADDING");

            provider.addAlgorithm("AlgorithmParameters.DESEDE", PACKAGE + ".util.IvAlgorithmParameters");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + PKCSObjectIdentifiers.des_EDE3_CBC, "DESEDE");

            provider.addAlgorithm("AlgorithmParameterGenerator.DESEDE",  PREFIX + "$AlgParamGen");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator." + PKCSObjectIdentifiers.des_EDE3_CBC, "DESEDE");

            provider.addAlgorithm("SecretKeyFactory.PBEWITHSHAAND3-KEYTRIPLEDES-CBC", PREFIX + "$PBEWithSHAAndDES3KeyFactory");
            provider.addAlgorithm("SecretKeyFactory.PBEWITHSHAAND2-KEYTRIPLEDES-CBC", PREFIX + "$PBEWithSHAAndDES2KeyFactory");

            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAAND3-KEYTRIPLEDES", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAAND2-KEYTRIPLEDES", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAAND3-KEYTRIPLEDES-CBC", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAAND2-KEYTRIPLEDES-CBC", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAANDDES3KEY-CBC", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAANDDES2KEY-CBC", "PKCS12PBE");

            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.1.2.840.113549.1.12.1.3", "PBEWITHSHAAND3-KEYTRIPLEDES-CBC");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.1.2.840.113549.1.12.1.4", "PBEWITHSHAAND2-KEYTRIPLEDES-CBC");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBEWithSHAAnd3KeyTripleDES", "PBEWITHSHAAND3-KEYTRIPLEDES-CBC");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.1.2.840.113549.1.12.1.3", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.1.2.840.113549.1.12.1.4", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.Cipher.PBEWithSHAAnd3KeyTripleDES",  "PBEWITHSHAAND3-KEYTRIPLEDES-CBC");
        }
    }
}
