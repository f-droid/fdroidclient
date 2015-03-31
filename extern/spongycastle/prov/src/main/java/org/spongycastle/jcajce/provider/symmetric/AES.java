package org.spongycastle.jcajce.provider.symmetric;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.spec.IvParameterSpec;

import org.spongycastle.asn1.bc.BCObjectIdentifiers;
import org.spongycastle.asn1.cms.GCMParameters;
import org.spongycastle.asn1.nist.NISTObjectIdentifiers;
import org.spongycastle.crypto.BlockCipher;
import org.spongycastle.crypto.BufferedBlockCipher;
import org.spongycastle.crypto.CipherKeyGenerator;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.engines.AESWrapEngine;
import org.spongycastle.crypto.engines.RFC3211WrapEngine;
import org.spongycastle.crypto.generators.Poly1305KeyGenerator;
import org.spongycastle.crypto.macs.CMac;
import org.spongycastle.crypto.macs.GMac;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.modes.CFBBlockCipher;
import org.spongycastle.crypto.modes.GCMBlockCipher;
import org.spongycastle.crypto.modes.OFBBlockCipher;
import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.symmetric.util.BaseAlgorithmParameterGenerator;
import org.spongycastle.jcajce.provider.symmetric.util.BaseAlgorithmParameters;
import org.spongycastle.jcajce.provider.symmetric.util.BaseBlockCipher;
import org.spongycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.spongycastle.jcajce.provider.symmetric.util.BaseMac;
import org.spongycastle.jcajce.provider.symmetric.util.BaseWrapCipher;
import org.spongycastle.jcajce.provider.symmetric.util.BlockCipherProvider;
import org.spongycastle.jcajce.provider.symmetric.util.IvAlgorithmParameters;
import org.spongycastle.jcajce.provider.symmetric.util.PBESecretKeyFactory;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.util.Integers;

public final class AES
{
    private static final Class gcmSpecClass = lookup("javax.crypto.spec.GCMParameterSpec");

    private AES()
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
                    return new AESFastEngine();
                }
            });
        }
    }

    public static class CBC
       extends BaseBlockCipher
    {
        public CBC()
        {
            super(new CBCBlockCipher(new AESFastEngine()), 128);
        }
    }

    static public class CFB
        extends BaseBlockCipher
    {
        public CFB()
        {
            super(new BufferedBlockCipher(new CFBBlockCipher(new AESFastEngine(), 128)), 128);
        }
    }

    static public class OFB
        extends BaseBlockCipher
    {
        public OFB()
        {
            super(new BufferedBlockCipher(new OFBBlockCipher(new AESFastEngine(), 128)), 128);
        }
    }

    static public class GCM
        extends BaseBlockCipher
    {
        public GCM()
        {
            super(new GCMBlockCipher(new AESFastEngine()));
        }
    }

    public static class AESCMAC
        extends BaseMac
    {
        public AESCMAC()
        {
            super(new CMac(new AESFastEngine()));
        }
    }

    public static class AESGMAC
        extends BaseMac
    {
        public AESGMAC()
        {
            super(new GMac(new GCMBlockCipher(new AESFastEngine())));
        }
    }

    public static class Poly1305
        extends BaseMac
    {
        public Poly1305()
        {
            super(new org.spongycastle.crypto.macs.Poly1305(new AESFastEngine()));
        }
    }

    public static class Poly1305KeyGen
        extends BaseKeyGenerator
    {
        public Poly1305KeyGen()
        {
            super("Poly1305-AES", 256, new Poly1305KeyGenerator());
        }
    }

    static public class Wrap
        extends BaseWrapCipher
    {
        public Wrap()
        {
            super(new AESWrapEngine());
        }
    }

    public static class RFC3211Wrap
        extends BaseWrapCipher
    {
        public RFC3211Wrap()
        {
            super(new RFC3211WrapEngine(new AESFastEngine()), 16);
        }
    }

    
    /**
     * PBEWithAES-CBC
     */
    static public class PBEWithAESCBC
        extends BaseBlockCipher
    {
        public PBEWithAESCBC()
        {
            super(new CBCBlockCipher(new AESFastEngine()));
        }
    }
    
    public static class KeyGen
        extends BaseKeyGenerator
    {
        public KeyGen()
        {
            this(192);
        }

        public KeyGen(int keySize)
        {
            super("AES", keySize, new CipherKeyGenerator());
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
    
    /**
     * PBEWithSHA1And128BitAES-BC
     */
    static public class PBEWithSHAAnd128BitAESBC
        extends PBESecretKeyFactory
    {
        public PBEWithSHAAnd128BitAESBC()
        {
            super("PBEWithSHA1And128BitAES-CBC-BC", null, true, PKCS12, SHA1, 128, 128);
        }
    }
    
    /**
     * PBEWithSHA1And192BitAES-BC
     */
    static public class PBEWithSHAAnd192BitAESBC
        extends PBESecretKeyFactory
    {
        public PBEWithSHAAnd192BitAESBC()
        {
            super("PBEWithSHA1And192BitAES-CBC-BC", null, true, PKCS12, SHA1, 192, 128);
        }
    }
    
    /**
     * PBEWithSHA1And256BitAES-BC
     */
    static public class PBEWithSHAAnd256BitAESBC
        extends PBESecretKeyFactory
    {
        public PBEWithSHAAnd256BitAESBC()
        {
            super("PBEWithSHA1And256BitAES-CBC-BC", null, true, PKCS12, SHA1, 256, 128);
        }
    }
    
    /**
     * PBEWithSHA256And128BitAES-BC
     */
    static public class PBEWithSHA256And128BitAESBC
        extends PBESecretKeyFactory
    {
        public PBEWithSHA256And128BitAESBC()
        {
            super("PBEWithSHA256And128BitAES-CBC-BC", null, true, PKCS12, SHA256, 128, 128);
        }
    }
    
    /**
     * PBEWithSHA256And192BitAES-BC
     */
    static public class PBEWithSHA256And192BitAESBC
        extends PBESecretKeyFactory
    {
        public PBEWithSHA256And192BitAESBC()
        {
            super("PBEWithSHA256And192BitAES-CBC-BC", null, true, PKCS12, SHA256, 192, 128);
        }
    }
    
    /**
     * PBEWithSHA256And256BitAES-BC
     */
    static public class PBEWithSHA256And256BitAESBC
        extends PBESecretKeyFactory
    {
        public PBEWithSHA256And256BitAESBC()
        {
            super("PBEWithSHA256And256BitAES-CBC-BC", null, true, PKCS12, SHA256, 256, 128);
        }
    }
    
    /**
     * PBEWithMD5And128BitAES-OpenSSL
     */
    static public class PBEWithMD5And128BitAESCBCOpenSSL
        extends PBESecretKeyFactory
    {
        public PBEWithMD5And128BitAESCBCOpenSSL()
        {
            super("PBEWithMD5And128BitAES-CBC-OpenSSL", null, true, OPENSSL, MD5, 128, 128);
        }
    }
    
    /**
     * PBEWithMD5And192BitAES-OpenSSL
     */
    static public class PBEWithMD5And192BitAESCBCOpenSSL
        extends PBESecretKeyFactory
    {
        public PBEWithMD5And192BitAESCBCOpenSSL()
        {
            super("PBEWithMD5And192BitAES-CBC-OpenSSL", null, true, OPENSSL, MD5, 192, 128);
        }
    }
    
    /**
     * PBEWithMD5And256BitAES-OpenSSL
     */
    static public class PBEWithMD5And256BitAESCBCOpenSSL
        extends PBESecretKeyFactory
    {
        public PBEWithMD5And256BitAESCBCOpenSSL()
        {
            super("PBEWithMD5And256BitAES-CBC-OpenSSL", null, true, OPENSSL, MD5, 256, 128);
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
                params = AlgorithmParameters.getInstance("AES", BouncyCastleProvider.PROVIDER_NAME);
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
            return "AES IV";
        }
    }

    public static class AlgParamsGCM
        extends BaseAlgorithmParameters
    {
        private GCMParameters gcmParams;

        protected void engineInit(AlgorithmParameterSpec paramSpec)
            throws InvalidParameterSpecException
        {
            if (gcmSpecClass != null)
            {
                try
                {
                    Method tLen = gcmSpecClass.getDeclaredMethod("getTLen", new Class[0]);
                    Method iv= gcmSpecClass.getDeclaredMethod("getIV", new Class[0]);


                    gcmParams = new GCMParameters((byte[])iv.invoke(paramSpec, new Object[0]), ((Integer)tLen.invoke(paramSpec, new Object[0])).intValue());
                }
                catch (Exception e)
                {
                    throw new InvalidParameterSpecException("Cannot process GCMParameterSpec.");
                }
            }
        }

        protected void engineInit(byte[] params)
            throws IOException
        {
            gcmParams = GCMParameters.getInstance(params);
        }

        protected void engineInit(byte[] params, String format)
            throws IOException
        {
            if (!isASN1FormatString(format))
            {
                throw new IOException("unknown format specified");
            }

            gcmParams = GCMParameters.getInstance(params);
        }

        protected byte[] engineGetEncoded()
            throws IOException
        {
            return gcmParams.getEncoded();
        }

        protected byte[] engineGetEncoded(String format)
            throws IOException
        {
            if (!isASN1FormatString(format))
            {
                throw new IOException("unknown format specified");
            }

            return gcmParams.getEncoded();
        }

        protected String engineToString()
        {
            return "GCM";
        }

        protected AlgorithmParameterSpec localEngineGetParameterSpec(Class paramSpec)
            throws InvalidParameterSpecException
        {
            if (gcmSpecClass != null)
            {
                try
                {
                    Constructor constructor = gcmSpecClass.getConstructor(new Class[] { byte[].class, Integer.class });

                    return (AlgorithmParameterSpec)constructor.newInstance(new Object[] { gcmParams.getNonce(), Integers.valueOf(gcmParams.getIcvLen()) });
                }
                catch (NoSuchMethodException e)
                {
                    throw new InvalidParameterSpecException("no constructor found!");   // should never happen
                }
                catch (Exception e)
                {
                    throw new InvalidParameterSpecException("construction failed: " + e.getMessage());   // should never happen
                }
            }

            throw new InvalidParameterSpecException("unknown parameter spec: " + paramSpec.getName());
        }
    }

    public static class Mappings
        extends SymmetricAlgorithmProvider
    {
        private static final String PREFIX = AES.class.getName();
        
        /**
         * These three got introduced in some messages as a result of a typo in an
         * early document. We don't produce anything using these OID values, but we'll
         * read them.
         */
        private static final String wrongAES128 = "2.16.840.1.101.3.4.2";
        private static final String wrongAES192 = "2.16.840.1.101.3.4.22";
        private static final String wrongAES256 = "2.16.840.1.101.3.4.42";

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {
            provider.addAlgorithm("AlgorithmParameters.AES", PREFIX + "$AlgParams");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + wrongAES128, "AES");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + wrongAES192, "AES");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + wrongAES256, "AES");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + NISTObjectIdentifiers.id_aes128_CBC, "AES");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + NISTObjectIdentifiers.id_aes192_CBC, "AES");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + NISTObjectIdentifiers.id_aes256_CBC, "AES");

            provider.addAlgorithm("AlgorithmParameters.GCM", PREFIX + "$AlgParamsGCM");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + NISTObjectIdentifiers.id_aes128_GCM, "GCM");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + NISTObjectIdentifiers.id_aes192_GCM, "GCM");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + NISTObjectIdentifiers.id_aes256_GCM, "GCM");

            provider.addAlgorithm("AlgorithmParameterGenerator.AES", PREFIX + "$AlgParamGen");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator." + wrongAES128, "AES");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator." + wrongAES192, "AES");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator." + wrongAES256, "AES");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator." + NISTObjectIdentifiers.id_aes128_CBC, "AES");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator." + NISTObjectIdentifiers.id_aes192_CBC, "AES");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator." + NISTObjectIdentifiers.id_aes256_CBC, "AES");

            provider.addAlgorithm("Cipher.AES", PREFIX + "$ECB");
            provider.addAlgorithm("Alg.Alias.Cipher." + wrongAES128, "AES");
            provider.addAlgorithm("Alg.Alias.Cipher." + wrongAES192, "AES");
            provider.addAlgorithm("Alg.Alias.Cipher." + wrongAES256, "AES");
            provider.addAlgorithm("Cipher." + NISTObjectIdentifiers.id_aes128_ECB, PREFIX + "$ECB");
            provider.addAlgorithm("Cipher." + NISTObjectIdentifiers.id_aes192_ECB, PREFIX + "$ECB");
            provider.addAlgorithm("Cipher." + NISTObjectIdentifiers.id_aes256_ECB, PREFIX + "$ECB");
            provider.addAlgorithm("Cipher." + NISTObjectIdentifiers.id_aes128_CBC, PREFIX + "$CBC");
            provider.addAlgorithm("Cipher." + NISTObjectIdentifiers.id_aes192_CBC, PREFIX + "$CBC");
            provider.addAlgorithm("Cipher." + NISTObjectIdentifiers.id_aes256_CBC, PREFIX + "$CBC");
            provider.addAlgorithm("Cipher." + NISTObjectIdentifiers.id_aes128_OFB, PREFIX + "$OFB");
            provider.addAlgorithm("Cipher." + NISTObjectIdentifiers.id_aes192_OFB, PREFIX + "$OFB");
            provider.addAlgorithm("Cipher." + NISTObjectIdentifiers.id_aes256_OFB, PREFIX + "$OFB");
            provider.addAlgorithm("Cipher." + NISTObjectIdentifiers.id_aes128_CFB, PREFIX + "$CFB");
            provider.addAlgorithm("Cipher." + NISTObjectIdentifiers.id_aes192_CFB, PREFIX + "$CFB");
            provider.addAlgorithm("Cipher." + NISTObjectIdentifiers.id_aes256_CFB, PREFIX + "$CFB");
            provider.addAlgorithm("Cipher.AESWRAP", PREFIX + "$Wrap");
            provider.addAlgorithm("Alg.Alias.Cipher." + NISTObjectIdentifiers.id_aes128_wrap, "AESWRAP");
            provider.addAlgorithm("Alg.Alias.Cipher." + NISTObjectIdentifiers.id_aes192_wrap, "AESWRAP");
            provider.addAlgorithm("Alg.Alias.Cipher." + NISTObjectIdentifiers.id_aes256_wrap, "AESWRAP");
            provider.addAlgorithm("Cipher.AESRFC3211WRAP", PREFIX + "$RFC3211Wrap");

            provider.addAlgorithm("Cipher.GCM", PREFIX + "$GCM");
            provider.addAlgorithm("Alg.Alias.Cipher." + NISTObjectIdentifiers.id_aes128_GCM, "GCM");
            provider.addAlgorithm("Alg.Alias.Cipher." + NISTObjectIdentifiers.id_aes192_GCM, "GCM");
            provider.addAlgorithm("Alg.Alias.Cipher." + NISTObjectIdentifiers.id_aes256_GCM, "GCM");

            provider.addAlgorithm("KeyGenerator.AES", PREFIX + "$KeyGen");
            provider.addAlgorithm("KeyGenerator." + wrongAES128, PREFIX + "$KeyGen128");
            provider.addAlgorithm("KeyGenerator." + wrongAES192, PREFIX + "$KeyGen192");
            provider.addAlgorithm("KeyGenerator." + wrongAES256, PREFIX + "$KeyGen256");
            provider.addAlgorithm("KeyGenerator." + NISTObjectIdentifiers.id_aes128_ECB, PREFIX + "$KeyGen128");
            provider.addAlgorithm("KeyGenerator." + NISTObjectIdentifiers.id_aes128_CBC, PREFIX + "$KeyGen128");
            provider.addAlgorithm("KeyGenerator." + NISTObjectIdentifiers.id_aes128_OFB, PREFIX + "$KeyGen128");
            provider.addAlgorithm("KeyGenerator." + NISTObjectIdentifiers.id_aes128_CFB, PREFIX + "$KeyGen128");
            provider.addAlgorithm("KeyGenerator." + NISTObjectIdentifiers.id_aes192_ECB, PREFIX + "$KeyGen192");
            provider.addAlgorithm("KeyGenerator." + NISTObjectIdentifiers.id_aes192_CBC, PREFIX + "$KeyGen192");
            provider.addAlgorithm("KeyGenerator." + NISTObjectIdentifiers.id_aes192_OFB, PREFIX + "$KeyGen192");
            provider.addAlgorithm("KeyGenerator." + NISTObjectIdentifiers.id_aes192_CFB, PREFIX + "$KeyGen192");
            provider.addAlgorithm("KeyGenerator." + NISTObjectIdentifiers.id_aes256_ECB, PREFIX + "$KeyGen256");
            provider.addAlgorithm("KeyGenerator." + NISTObjectIdentifiers.id_aes256_CBC, PREFIX + "$KeyGen256");
            provider.addAlgorithm("KeyGenerator." + NISTObjectIdentifiers.id_aes256_OFB, PREFIX + "$KeyGen256");
            provider.addAlgorithm("KeyGenerator." + NISTObjectIdentifiers.id_aes256_CFB, PREFIX + "$KeyGen256");
            provider.addAlgorithm("KeyGenerator.AESWRAP", PREFIX + "$KeyGen");
            provider.addAlgorithm("KeyGenerator." + NISTObjectIdentifiers.id_aes128_wrap, PREFIX + "$KeyGen128");
            provider.addAlgorithm("KeyGenerator." + NISTObjectIdentifiers.id_aes192_wrap, PREFIX + "$KeyGen192");
            provider.addAlgorithm("KeyGenerator." + NISTObjectIdentifiers.id_aes256_wrap, PREFIX + "$KeyGen256");

            provider.addAlgorithm("Mac.AESCMAC", PREFIX + "$AESCMAC");
            
            provider.addAlgorithm("Alg.Alias.Cipher." + BCObjectIdentifiers.bc_pbe_sha1_pkcs12_aes128_cbc.getId(), "PBEWITHSHAAND128BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.Cipher." + BCObjectIdentifiers.bc_pbe_sha1_pkcs12_aes192_cbc.getId(), "PBEWITHSHAAND192BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.Cipher." + BCObjectIdentifiers.bc_pbe_sha1_pkcs12_aes256_cbc.getId(), "PBEWITHSHAAND256BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.Cipher." + BCObjectIdentifiers.bc_pbe_sha256_pkcs12_aes128_cbc.getId(), "PBEWITHSHA256AND128BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.Cipher." + BCObjectIdentifiers.bc_pbe_sha256_pkcs12_aes192_cbc.getId(), "PBEWITHSHA256AND192BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.Cipher." + BCObjectIdentifiers.bc_pbe_sha256_pkcs12_aes256_cbc.getId(), "PBEWITHSHA256AND256BITAES-CBC-BC");
    
            provider.addAlgorithm("Cipher.PBEWITHSHAAND128BITAES-CBC-BC", PREFIX + "$PBEWithAESCBC");
            provider.addAlgorithm("Cipher.PBEWITHSHAAND192BITAES-CBC-BC", PREFIX + "$PBEWithAESCBC");
            provider.addAlgorithm("Cipher.PBEWITHSHAAND256BITAES-CBC-BC", PREFIX + "$PBEWithAESCBC");
            provider.addAlgorithm("Cipher.PBEWITHSHA256AND128BITAES-CBC-BC", PREFIX + "$PBEWithAESCBC");
            provider.addAlgorithm("Cipher.PBEWITHSHA256AND192BITAES-CBC-BC", PREFIX + "$PBEWithAESCBC");
            provider.addAlgorithm("Cipher.PBEWITHSHA256AND256BITAES-CBC-BC", PREFIX + "$PBEWithAESCBC");
            
            provider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA1AND128BITAES-CBC-BC","PBEWITHSHAAND128BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA1AND192BITAES-CBC-BC","PBEWITHSHAAND192BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA1AND256BITAES-CBC-BC","PBEWITHSHAAND256BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA-1AND128BITAES-CBC-BC","PBEWITHSHAAND128BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA-1AND192BITAES-CBC-BC","PBEWITHSHAAND192BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA-1AND256BITAES-CBC-BC","PBEWITHSHAAND256BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA-256AND128BITAES-CBC-BC","PBEWITHSHA256AND128BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA-256AND192BITAES-CBC-BC","PBEWITHSHA256AND192BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA-256AND256BITAES-CBC-BC","PBEWITHSHA256AND256BITAES-CBC-BC");
            
            provider.addAlgorithm("Cipher.PBEWITHMD5AND128BITAES-CBC-OPENSSL", PREFIX + "$PBEWithAESCBC");
            provider.addAlgorithm("Cipher.PBEWITHMD5AND192BITAES-CBC-OPENSSL", PREFIX + "$PBEWithAESCBC");
            provider.addAlgorithm("Cipher.PBEWITHMD5AND256BITAES-CBC-OPENSSL", PREFIX + "$PBEWithAESCBC");
            
            provider.addAlgorithm("SecretKeyFactory.PBEWITHMD5AND128BITAES-CBC-OPENSSL", PREFIX + "$PBEWithMD5And128BitAESCBCOpenSSL");
            provider.addAlgorithm("SecretKeyFactory.PBEWITHMD5AND192BITAES-CBC-OPENSSL", PREFIX + "$PBEWithMD5And192BitAESCBCOpenSSL");
            provider.addAlgorithm("SecretKeyFactory.PBEWITHMD5AND256BITAES-CBC-OPENSSL", PREFIX + "$PBEWithMD5And256BitAESCBCOpenSSL");
            
            provider.addAlgorithm("SecretKeyFactory.PBEWITHSHAAND128BITAES-CBC-BC", PREFIX + "$PBEWithSHAAnd128BitAESBC");
            provider.addAlgorithm("SecretKeyFactory.PBEWITHSHAAND192BITAES-CBC-BC", PREFIX + "$PBEWithSHAAnd192BitAESBC");
            provider.addAlgorithm("SecretKeyFactory.PBEWITHSHAAND256BITAES-CBC-BC", PREFIX + "$PBEWithSHAAnd256BitAESBC");
            provider.addAlgorithm("SecretKeyFactory.PBEWITHSHA256AND128BITAES-CBC-BC", PREFIX + "$PBEWithSHA256And128BitAESBC");
            provider.addAlgorithm("SecretKeyFactory.PBEWITHSHA256AND192BITAES-CBC-BC", PREFIX + "$PBEWithSHA256And192BitAESBC");
            provider.addAlgorithm("SecretKeyFactory.PBEWITHSHA256AND256BITAES-CBC-BC", PREFIX + "$PBEWithSHA256And256BitAESBC");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBEWITHSHA1AND128BITAES-CBC-BC","PBEWITHSHAAND128BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBEWITHSHA1AND192BITAES-CBC-BC","PBEWITHSHAAND192BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBEWITHSHA1AND256BITAES-CBC-BC","PBEWITHSHAAND256BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBEWITHSHA-1AND128BITAES-CBC-BC","PBEWITHSHAAND128BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBEWITHSHA-1AND192BITAES-CBC-BC","PBEWITHSHAAND192BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBEWITHSHA-1AND256BITAES-CBC-BC","PBEWITHSHAAND256BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBEWITHSHA-256AND128BITAES-CBC-BC","PBEWITHSHA256AND128BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBEWITHSHA-256AND192BITAES-CBC-BC","PBEWITHSHA256AND192BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBEWITHSHA-256AND256BITAES-CBC-BC","PBEWITHSHA256AND256BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory." + BCObjectIdentifiers.bc_pbe_sha1_pkcs12_aes128_cbc.getId(), "PBEWITHSHAAND128BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory." + BCObjectIdentifiers.bc_pbe_sha1_pkcs12_aes192_cbc.getId(), "PBEWITHSHAAND192BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory." + BCObjectIdentifiers.bc_pbe_sha1_pkcs12_aes256_cbc.getId(), "PBEWITHSHAAND256BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory." + BCObjectIdentifiers.bc_pbe_sha256_pkcs12_aes128_cbc.getId(), "PBEWITHSHA256AND128BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory." + BCObjectIdentifiers.bc_pbe_sha256_pkcs12_aes192_cbc.getId(), "PBEWITHSHA256AND192BITAES-CBC-BC");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory." + BCObjectIdentifiers.bc_pbe_sha256_pkcs12_aes256_cbc.getId(), "PBEWITHSHA256AND256BITAES-CBC-BC");
            
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAAND128BITAES-CBC-BC", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAAND192BITAES-CBC-BC", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAAND256BITAES-CBC-BC", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHA256AND128BITAES-CBC-BC", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHA256AND192BITAES-CBC-BC", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHA256AND256BITAES-CBC-BC", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHA1AND128BITAES-CBC-BC","PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHA1AND192BITAES-CBC-BC","PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHA1AND256BITAES-CBC-BC","PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHA-1AND128BITAES-CBC-BC","PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHA-1AND192BITAES-CBC-BC","PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHA-1AND256BITAES-CBC-BC","PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHA-256AND128BITAES-CBC-BC","PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHA-256AND192BITAES-CBC-BC","PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHA-256AND256BITAES-CBC-BC","PKCS12PBE"); 
            
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + BCObjectIdentifiers.bc_pbe_sha1_pkcs12_aes128_cbc.getId(), "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + BCObjectIdentifiers.bc_pbe_sha1_pkcs12_aes192_cbc.getId(), "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + BCObjectIdentifiers.bc_pbe_sha1_pkcs12_aes256_cbc.getId(), "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + BCObjectIdentifiers.bc_pbe_sha256_pkcs12_aes128_cbc.getId(), "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + BCObjectIdentifiers.bc_pbe_sha256_pkcs12_aes192_cbc.getId(), "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + BCObjectIdentifiers.bc_pbe_sha256_pkcs12_aes256_cbc.getId(), "PKCS12PBE");

            addGMacAlgorithm(provider, "AES", PREFIX + "$AESGMAC", PREFIX + "$KeyGen128");
            addPoly1305Algorithm(provider, "AES", PREFIX + "$Poly1305", PREFIX + "$Poly1305KeyGen");
        }
    }

    private static Class lookup(String className)
    {
        try
        {
            Class def = AES.class.getClassLoader().loadClass(className);

            return def;
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
