package org.spongycastle.jcajce.provider.symmetric;

import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.RC2ParameterSpec;

import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.pkcs.RC2CBCParameter;
import org.spongycastle.crypto.CipherKeyGenerator;
import org.spongycastle.crypto.engines.RC2Engine;
import org.spongycastle.crypto.engines.RC2WrapEngine;
import org.spongycastle.crypto.macs.CBCBlockCipherMac;
import org.spongycastle.crypto.macs.CFBBlockCipherMac;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.symmetric.util.BaseAlgorithmParameterGenerator;
import org.spongycastle.jcajce.provider.symmetric.util.BaseAlgorithmParameters;
import org.spongycastle.jcajce.provider.symmetric.util.BaseBlockCipher;
import org.spongycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.spongycastle.jcajce.provider.symmetric.util.BaseMac;
import org.spongycastle.jcajce.provider.symmetric.util.BaseWrapCipher;
import org.spongycastle.jcajce.provider.symmetric.util.PBESecretKeyFactory;
import org.spongycastle.jcajce.provider.util.AlgorithmProvider;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.util.Arrays;

public final class RC2
{
    private RC2()
    {
    }

    /**
     * RC2
     */
    static public class ECB
        extends BaseBlockCipher
    {
        public ECB()
        {
            super(new RC2Engine());
        }
    }

    /**
     * RC2CBC
     */
    static public class CBC
        extends BaseBlockCipher
    {
        public CBC()
        {
            super(new CBCBlockCipher(new RC2Engine()), 64);
        }
    }

    public static class Wrap
        extends BaseWrapCipher
    {
        public Wrap()
        {
            super(new RC2WrapEngine());
        }
    }

    /**
     * RC2
     */
    public static class CBCMAC
        extends BaseMac
    {
        public CBCMAC()
        {
            super(new CBCBlockCipherMac(new RC2Engine()));
        }
    }

    public static class CFB8MAC
        extends BaseMac
    {
        public CFB8MAC()
        {
            super(new CFBBlockCipherMac(new RC2Engine()));
        }
    }

    /**
     * PBEWithSHA1AndRC2
     */
    static public class PBEWithSHA1KeyFactory
        extends PBESecretKeyFactory
    {
        public PBEWithSHA1KeyFactory()
        {
            super("PBEwithSHA1andRC2", PKCSObjectIdentifiers.pbeWithSHA1AndRC2_CBC, true, PKCS5S1, SHA1, 64, 64);
        }
    }

    /**
     * PBEWithSHAAnd128BitRC2-CBC
     */
    static public class PBEWithSHAAnd128BitKeyFactory
        extends PBESecretKeyFactory
    {
        public PBEWithSHAAnd128BitKeyFactory()
        {
            super("PBEwithSHAand128BitRC2-CBC", PKCSObjectIdentifiers.pbeWithSHAAnd128BitRC2_CBC, true, PKCS12, SHA1, 128, 64);
        }
    }

    /**
     * PBEWithSHAAnd40BitRC2-CBC
     */
    static public class PBEWithSHAAnd40BitKeyFactory
        extends PBESecretKeyFactory
    {
        public PBEWithSHAAnd40BitKeyFactory()
        {
            super("PBEwithSHAand40BitRC2-CBC", PKCSObjectIdentifiers.pbeWithSHAAnd40BitRC2_CBC, true, PKCS12, SHA1, 40, 64);
        }
    }

    /**
     * PBEWithMD5AndRC2
     */
    static public class PBEWithMD5AndRC2
        extends BaseBlockCipher
    {
        public PBEWithMD5AndRC2()
        {
            super(new CBCBlockCipher(new RC2Engine()));
        }
    }
    
    /**
     * PBEWithSHA1AndRC2
     */
    static public class PBEWithSHA1AndRC2
        extends BaseBlockCipher
    {
        public PBEWithSHA1AndRC2()
        {
            super(new CBCBlockCipher(new RC2Engine()));
        }
    }

    /**
     * PBEWithSHAAnd128BitRC2-CBC
     */
    static public class PBEWithSHAAnd128BitRC2
        extends BaseBlockCipher
    {
        public PBEWithSHAAnd128BitRC2()
        {
            super(new CBCBlockCipher(new RC2Engine()));
        }
    }

    /**
     * PBEWithSHAAnd40BitRC2-CBC
     */
    static public class PBEWithSHAAnd40BitRC2
        extends BaseBlockCipher
    {
        public PBEWithSHAAnd40BitRC2()
        {
            super(new CBCBlockCipher(new RC2Engine()));
        }
    }

    /**
     * PBEWithMD2AndRC2
     */
    static public class PBEWithMD2KeyFactory
        extends PBESecretKeyFactory
    {
        public PBEWithMD2KeyFactory()
        {
            super("PBEwithMD2andRC2", PKCSObjectIdentifiers.pbeWithMD2AndRC2_CBC, true, PKCS5S1, MD2, 64, 64);
        }
    }

   /**
    * PBEWithMD5AndRC2
    */
   static public class PBEWithMD5KeyFactory
       extends PBESecretKeyFactory
   {
       public PBEWithMD5KeyFactory()
       {
           super("PBEwithMD5andRC2", PKCSObjectIdentifiers.pbeWithMD5AndRC2_CBC, true, PKCS5S1, MD5, 64, 64);
       }
   }

    public static class AlgParamGen
        extends BaseAlgorithmParameterGenerator
    {
        RC2ParameterSpec spec = null;

        protected void engineInit(
            AlgorithmParameterSpec genParamSpec,
            SecureRandom random)
            throws InvalidAlgorithmParameterException
        {
            if (genParamSpec instanceof RC2ParameterSpec)
            {
                spec = (RC2ParameterSpec)genParamSpec;
                return;
            }

            throw new InvalidAlgorithmParameterException("No supported AlgorithmParameterSpec for RC2 parameter generation.");
        }

        protected AlgorithmParameters engineGenerateParameters()
        {
            AlgorithmParameters params;

            if (spec == null)
            {
                byte[] iv = new byte[8];

                if (random == null)
                {
                    random = new SecureRandom();
                }

                random.nextBytes(iv);

                try
                {
                    params = AlgorithmParameters.getInstance("RC2", BouncyCastleProvider.PROVIDER_NAME);
                    params.init(new IvParameterSpec(iv));
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e.getMessage());
                }
            }
            else
            {
                try
                {
                    params = AlgorithmParameters.getInstance("RC2", BouncyCastleProvider.PROVIDER_NAME);
                    params.init(spec);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e.getMessage());
                }
            }

            return params;
        }
    }

    public static class KeyGenerator
        extends BaseKeyGenerator
    {
        public KeyGenerator()
        {
            super("RC2", 128, new CipherKeyGenerator());
        }
    }

    public static class AlgParams
        extends BaseAlgorithmParameters
    {
        private static final short[] table = {
            0xbd, 0x56, 0xea, 0xf2, 0xa2, 0xf1, 0xac, 0x2a, 0xb0, 0x93, 0xd1, 0x9c, 0x1b, 0x33, 0xfd, 0xd0,
            0x30, 0x04, 0xb6, 0xdc, 0x7d, 0xdf, 0x32, 0x4b, 0xf7, 0xcb, 0x45, 0x9b, 0x31, 0xbb, 0x21, 0x5a,
            0x41, 0x9f, 0xe1, 0xd9, 0x4a, 0x4d, 0x9e, 0xda, 0xa0, 0x68, 0x2c, 0xc3, 0x27, 0x5f, 0x80, 0x36,
            0x3e, 0xee, 0xfb, 0x95, 0x1a, 0xfe, 0xce, 0xa8, 0x34, 0xa9, 0x13, 0xf0, 0xa6, 0x3f, 0xd8, 0x0c,
            0x78, 0x24, 0xaf, 0x23, 0x52, 0xc1, 0x67, 0x17, 0xf5, 0x66, 0x90, 0xe7, 0xe8, 0x07, 0xb8, 0x60,
            0x48, 0xe6, 0x1e, 0x53, 0xf3, 0x92, 0xa4, 0x72, 0x8c, 0x08, 0x15, 0x6e, 0x86, 0x00, 0x84, 0xfa,
            0xf4, 0x7f, 0x8a, 0x42, 0x19, 0xf6, 0xdb, 0xcd, 0x14, 0x8d, 0x50, 0x12, 0xba, 0x3c, 0x06, 0x4e,
            0xec, 0xb3, 0x35, 0x11, 0xa1, 0x88, 0x8e, 0x2b, 0x94, 0x99, 0xb7, 0x71, 0x74, 0xd3, 0xe4, 0xbf,
            0x3a, 0xde, 0x96, 0x0e, 0xbc, 0x0a, 0xed, 0x77, 0xfc, 0x37, 0x6b, 0x03, 0x79, 0x89, 0x62, 0xc6,
            0xd7, 0xc0, 0xd2, 0x7c, 0x6a, 0x8b, 0x22, 0xa3, 0x5b, 0x05, 0x5d, 0x02, 0x75, 0xd5, 0x61, 0xe3,
            0x18, 0x8f, 0x55, 0x51, 0xad, 0x1f, 0x0b, 0x5e, 0x85, 0xe5, 0xc2, 0x57, 0x63, 0xca, 0x3d, 0x6c,
            0xb4, 0xc5, 0xcc, 0x70, 0xb2, 0x91, 0x59, 0x0d, 0x47, 0x20, 0xc8, 0x4f, 0x58, 0xe0, 0x01, 0xe2,
            0x16, 0x38, 0xc4, 0x6f, 0x3b, 0x0f, 0x65, 0x46, 0xbe, 0x7e, 0x2d, 0x7b, 0x82, 0xf9, 0x40, 0xb5,
            0x1d, 0x73, 0xf8, 0xeb, 0x26, 0xc7, 0x87, 0x97, 0x25, 0x54, 0xb1, 0x28, 0xaa, 0x98, 0x9d, 0xa5,
            0x64, 0x6d, 0x7a, 0xd4, 0x10, 0x81, 0x44, 0xef, 0x49, 0xd6, 0xae, 0x2e, 0xdd, 0x76, 0x5c, 0x2f,
            0xa7, 0x1c, 0xc9, 0x09, 0x69, 0x9a, 0x83, 0xcf, 0x29, 0x39, 0xb9, 0xe9, 0x4c, 0xff, 0x43, 0xab
        };

        private static final short[] ekb = {
            0x5d, 0xbe, 0x9b, 0x8b, 0x11, 0x99, 0x6e, 0x4d, 0x59, 0xf3, 0x85, 0xa6, 0x3f, 0xb7, 0x83, 0xc5,
            0xe4, 0x73, 0x6b, 0x3a, 0x68, 0x5a, 0xc0, 0x47, 0xa0, 0x64, 0x34, 0x0c, 0xf1, 0xd0, 0x52, 0xa5,
            0xb9, 0x1e, 0x96, 0x43, 0x41, 0xd8, 0xd4, 0x2c, 0xdb, 0xf8, 0x07, 0x77, 0x2a, 0xca, 0xeb, 0xef,
            0x10, 0x1c, 0x16, 0x0d, 0x38, 0x72, 0x2f, 0x89, 0xc1, 0xf9, 0x80, 0xc4, 0x6d, 0xae, 0x30, 0x3d,
            0xce, 0x20, 0x63, 0xfe, 0xe6, 0x1a, 0xc7, 0xb8, 0x50, 0xe8, 0x24, 0x17, 0xfc, 0x25, 0x6f, 0xbb,
            0x6a, 0xa3, 0x44, 0x53, 0xd9, 0xa2, 0x01, 0xab, 0xbc, 0xb6, 0x1f, 0x98, 0xee, 0x9a, 0xa7, 0x2d,
            0x4f, 0x9e, 0x8e, 0xac, 0xe0, 0xc6, 0x49, 0x46, 0x29, 0xf4, 0x94, 0x8a, 0xaf, 0xe1, 0x5b, 0xc3,
            0xb3, 0x7b, 0x57, 0xd1, 0x7c, 0x9c, 0xed, 0x87, 0x40, 0x8c, 0xe2, 0xcb, 0x93, 0x14, 0xc9, 0x61,
            0x2e, 0xe5, 0xcc, 0xf6, 0x5e, 0xa8, 0x5c, 0xd6, 0x75, 0x8d, 0x62, 0x95, 0x58, 0x69, 0x76, 0xa1,
            0x4a, 0xb5, 0x55, 0x09, 0x78, 0x33, 0x82, 0xd7, 0xdd, 0x79, 0xf5, 0x1b, 0x0b, 0xde, 0x26, 0x21,
            0x28, 0x74, 0x04, 0x97, 0x56, 0xdf, 0x3c, 0xf0, 0x37, 0x39, 0xdc, 0xff, 0x06, 0xa4, 0xea, 0x42,
            0x08, 0xda, 0xb4, 0x71, 0xb0, 0xcf, 0x12, 0x7a, 0x4e, 0xfa, 0x6c, 0x1d, 0x84, 0x00, 0xc8, 0x7f,
            0x91, 0x45, 0xaa, 0x2b, 0xc2, 0xb1, 0x8f, 0xd5, 0xba, 0xf2, 0xad, 0x19, 0xb2, 0x67, 0x36, 0xf7,
            0x0f, 0x0a, 0x92, 0x7d, 0xe3, 0x9d, 0xe9, 0x90, 0x3e, 0x23, 0x27, 0x66, 0x13, 0xec, 0x81, 0x15,
            0xbd, 0x22, 0xbf, 0x9f, 0x7e, 0xa9, 0x51, 0x4b, 0x4c, 0xfb, 0x02, 0xd3, 0x70, 0x86, 0x31, 0xe7,
            0x3b, 0x05, 0x03, 0x54, 0x60, 0x48, 0x65, 0x18, 0xd2, 0xcd, 0x5f, 0x32, 0x88, 0x0e, 0x35, 0xfd
        };

        private byte[] iv;
        private int parameterVersion = 58;

        protected byte[] engineGetEncoded()
        {
            return Arrays.clone(iv);
        }

        protected byte[] engineGetEncoded(
            String format)
            throws IOException
        {
            if (this.isASN1FormatString(format))
            {
                if (parameterVersion == -1)
                {
                    return new RC2CBCParameter(engineGetEncoded()).getEncoded();
                }
                else
                {
                    return new RC2CBCParameter(parameterVersion, engineGetEncoded()).getEncoded();
                }
            }

            if (format.equals("RAW"))
            {
                return engineGetEncoded();
            }

            return null;
        }

        protected AlgorithmParameterSpec localEngineGetParameterSpec(
            Class paramSpec)
            throws InvalidParameterSpecException
        {
            if (paramSpec == RC2ParameterSpec.class)
            {
                if (parameterVersion != -1)
                {
                    if (parameterVersion < 256)
                    {
                        return new RC2ParameterSpec(ekb[parameterVersion], iv);
                    }
                    else
                    {
                        return new RC2ParameterSpec(parameterVersion, iv);
                    }
                }
            }

            if (paramSpec == IvParameterSpec.class)
            {
                return new IvParameterSpec(iv);
            }

            throw new InvalidParameterSpecException("unknown parameter spec passed to RC2 parameters object.");
        }

        protected void engineInit(
            AlgorithmParameterSpec paramSpec)
            throws InvalidParameterSpecException
        {
            if (paramSpec instanceof IvParameterSpec)
            {
                this.iv = ((IvParameterSpec)paramSpec).getIV();
            }
            else if (paramSpec instanceof RC2ParameterSpec)
            {
                int effKeyBits = ((RC2ParameterSpec)paramSpec).getEffectiveKeyBits();
                if (effKeyBits != -1)
                {
                    if (effKeyBits < 256)
                    {
                        parameterVersion = table[effKeyBits];
                    }
                    else
                    {
                        parameterVersion = effKeyBits;
                    }
                }

                this.iv = ((RC2ParameterSpec)paramSpec).getIV();
            }
            else
            {
                throw new InvalidParameterSpecException("IvParameterSpec or RC2ParameterSpec required to initialise a RC2 parameters algorithm parameters object");
            }
        }

        protected void engineInit(
            byte[] params)
            throws IOException
        {
            this.iv = Arrays.clone(params);
        }

        protected void engineInit(
            byte[] params,
            String format)
            throws IOException
        {
            if (this.isASN1FormatString(format))
            {
                RC2CBCParameter p = RC2CBCParameter.getInstance(ASN1Primitive.fromByteArray(params));

                if (p.getRC2ParameterVersion() != null)
                {
                    parameterVersion = p.getRC2ParameterVersion().intValue();
                }

                iv = p.getIV();

                return;
            }

            if (format.equals("RAW"))
            {
                engineInit(params);
                return;
            }

            throw new IOException("Unknown parameters format in IV parameters object");
        }

        protected String engineToString()
        {
            return "RC2 Parameters";
        }
    }

    public static class Mappings
        extends AlgorithmProvider
    {
        private static final String PREFIX = RC2.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {

            provider.addAlgorithm("AlgorithmParameterGenerator.RC2", PREFIX + "$AlgParamGen");
            provider.addAlgorithm("AlgorithmParameterGenerator.1.2.840.113549.3.2", PREFIX + "$AlgParamGen");

            provider.addAlgorithm("KeyGenerator.RC2", PREFIX + "$KeyGenerator");
            provider.addAlgorithm("KeyGenerator.1.2.840.113549.3.2", PREFIX + "$KeyGenerator");

            provider.addAlgorithm("AlgorithmParameters.RC2", PREFIX + "$AlgParams");
            provider.addAlgorithm("AlgorithmParameters.1.2.840.113549.3.2", PREFIX + "$AlgParams");

            provider.addAlgorithm("Cipher.RC2", PREFIX + "$ECB");
            provider.addAlgorithm("Cipher.RC2WRAP", PREFIX + "$Wrap");
            provider.addAlgorithm("Alg.Alias.Cipher." + PKCSObjectIdentifiers.id_alg_CMSRC2wrap, "RC2WRAP");
            provider.addAlgorithm("Cipher.1.2.840.113549.3.2", PREFIX + "$CBC");

            provider.addAlgorithm("Mac.RC2MAC", PREFIX + "$CBCMAC");
            provider.addAlgorithm("Alg.Alias.Mac.RC2", "RC2MAC");
            provider.addAlgorithm("Mac.RC2MAC/CFB8", PREFIX + "$CFB8MAC");
            provider.addAlgorithm("Alg.Alias.Mac.RC2/CFB8", "RC2MAC/CFB8");

            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBEWITHMD2ANDRC2-CBC", "PBEWITHMD2ANDRC2");

            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBEWITHMD5ANDRC2-CBC", "PBEWITHMD5ANDRC2");

            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBEWITHSHA1ANDRC2-CBC", "PBEWITHSHA1ANDRC2");

            provider.addAlgorithm("Alg.Alias.SecretKeyFactory." + PKCSObjectIdentifiers.pbeWithMD2AndRC2_CBC, "PBEWITHMD2ANDRC2");

            provider.addAlgorithm("Alg.Alias.SecretKeyFactory." + PKCSObjectIdentifiers.pbeWithMD5AndRC2_CBC, "PBEWITHMD5ANDRC2");

            provider.addAlgorithm("Alg.Alias.SecretKeyFactory." + PKCSObjectIdentifiers.pbeWithSHA1AndRC2_CBC, "PBEWITHSHA1ANDRC2");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.1.2.840.113549.1.12.1.5", "PBEWITHSHAAND128BITRC2-CBC");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.1.2.840.113549.1.12.1.6", "PBEWITHSHAAND40BITRC2-CBC");

            provider.addAlgorithm("SecretKeyFactory.PBEWITHMD2ANDRC2", PREFIX + "$PBEWithMD2KeyFactory");
            provider.addAlgorithm("SecretKeyFactory.PBEWITHMD5ANDRC2", PREFIX + "$PBEWithMD5KeyFactory");
            provider.addAlgorithm("SecretKeyFactory.PBEWITHSHA1ANDRC2", PREFIX + "$PBEWithSHA1KeyFactory");

            provider.addAlgorithm("SecretKeyFactory.PBEWITHSHAAND128BITRC2-CBC", PREFIX + "$PBEWithSHAAnd128BitKeyFactory");
            provider.addAlgorithm("SecretKeyFactory.PBEWITHSHAAND40BITRC2-CBC", PREFIX + "$PBEWithSHAAnd40BitKeyFactory");
            
            provider.addAlgorithm("Alg.Alias.Cipher." + PKCSObjectIdentifiers.pbeWithMD2AndRC2_CBC, "PBEWITHMD2ANDRC2");

            provider.addAlgorithm("Alg.Alias.Cipher." + PKCSObjectIdentifiers.pbeWithMD5AndRC2_CBC, "PBEWITHMD5ANDRC2");

            provider.addAlgorithm("Alg.Alias.Cipher." + PKCSObjectIdentifiers.pbeWithSHA1AndRC2_CBC, "PBEWITHSHA1ANDRC2");

            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.1.2.840.113549.1.12.1.5", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.1.2.840.113549.1.12.1.6", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWithSHAAnd3KeyTripleDES", "PKCS12PBE");

            provider.addAlgorithm("Alg.Alias.Cipher.1.2.840.113549.1.12.1.5", "PBEWITHSHAAND128BITRC2-CBC");
            provider.addAlgorithm("Alg.Alias.Cipher.1.2.840.113549.1.12.1.6", "PBEWITHSHAAND40BITRC2-CBC");
            provider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA1AND128BITRC2-CBC", "PBEWITHSHAAND128BITRC2-CBC");
            provider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA1AND40BITRC2-CBC", "PBEWITHSHAAND40BITRC2-CBC");
            provider.addAlgorithm("Cipher.PBEWITHSHA1ANDRC2", PREFIX + "$PBEWithSHA1AndRC2");

            provider.addAlgorithm("Cipher.PBEWITHSHAAND128BITRC2-CBC", PREFIX + "$PBEWithSHAAnd128BitRC2");
            provider.addAlgorithm("Cipher.PBEWITHSHAAND40BITRC2-CBC", PREFIX + "$PBEWithSHAAnd40BitRC2");
            provider.addAlgorithm("Cipher.PBEWITHMD5ANDRC2", PREFIX + "$PBEWithMD5AndRC2");

            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHA1ANDRC2", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAANDRC2", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHA1ANDRC2-CBC", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAAND40BITRC2-CBC", "PKCS12PBE");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAAND128BITRC2-CBC", "PKCS12PBE");
        }
    }
}
