package org.spongycastle.jcajce.provider.symmetric;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.spongycastle.asn1.ASN1Encoding;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PBKDF2Params;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.jcajce.provider.config.ConfigurableProvider;
import org.spongycastle.jcajce.provider.symmetric.util.BCPBEKey;
import org.spongycastle.jcajce.provider.symmetric.util.BaseAlgorithmParameters;
import org.spongycastle.jcajce.provider.symmetric.util.BaseSecretKeyFactory;
import org.spongycastle.jcajce.provider.symmetric.util.PBE;
import org.spongycastle.jcajce.provider.util.AlgorithmProvider;
import org.spongycastle.jcajce.spec.PBKDF2KeySpec;

public class PBEPBKDF2
{
    private PBEPBKDF2()
    {

    }

    public static class AlgParams
        extends BaseAlgorithmParameters
    {
        PBKDF2Params params;

        protected byte[] engineGetEncoded()
        {
            try
            {
                return params.getEncoded(ASN1Encoding.DER);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Oooops! " + e.toString());
            }
        }

        protected byte[] engineGetEncoded(
            String format)
        {
            if (this.isASN1FormatString(format))
            {
                return engineGetEncoded();
            }

            return null;
        }

        protected AlgorithmParameterSpec localEngineGetParameterSpec(
            Class paramSpec)
            throws InvalidParameterSpecException
        {
            if (paramSpec == PBEParameterSpec.class)
            {
                return new PBEParameterSpec(params.getSalt(),
                                params.getIterationCount().intValue());
            }

            throw new InvalidParameterSpecException("unknown parameter spec passed to PBKDF2 PBE parameters object.");
        }

        protected void engineInit(
            AlgorithmParameterSpec paramSpec)
            throws InvalidParameterSpecException
        {
            if (!(paramSpec instanceof PBEParameterSpec))
            {
                throw new InvalidParameterSpecException("PBEParameterSpec required to initialise a PBKDF2 PBE parameters algorithm parameters object");
            }

            PBEParameterSpec    pbeSpec = (PBEParameterSpec)paramSpec;

            this.params = new PBKDF2Params(pbeSpec.getSalt(),
                                pbeSpec.getIterationCount());
        }

        protected void engineInit(
            byte[] params)
            throws IOException
        {
            this.params = PBKDF2Params.getInstance(ASN1Primitive.fromByteArray(params));
        }

        protected void engineInit(
            byte[] params,
            String format)
            throws IOException
        {
            if (this.isASN1FormatString(format))
            {
                engineInit(params);
                return;
            }

            throw new IOException("Unknown parameters format in PBKDF2 parameters object");
        }

        protected String engineToString()
        {
            return "PBKDF2 Parameters";
        }
    }

    public static class BasePBKDF2
        extends BaseSecretKeyFactory
    {
        private int scheme;

        public BasePBKDF2(String name, int scheme)
        {
            super(name, PKCSObjectIdentifiers.id_PBKDF2);

            this.scheme = scheme;
        }

        protected SecretKey engineGenerateSecret(
            KeySpec keySpec)
            throws InvalidKeySpecException
        {
            if (keySpec instanceof PBEKeySpec)
            {
                PBEKeySpec pbeSpec = (PBEKeySpec)keySpec;

                if (pbeSpec.getSalt() == null)
                {
                    throw new InvalidKeySpecException("missing required salt");
                }

                if (pbeSpec.getIterationCount() <= 0)
                {
                    throw new InvalidKeySpecException("positive iteration count required: "
                        + pbeSpec.getIterationCount());
                }

                if (pbeSpec.getKeyLength() <= 0)
                {
                    throw new InvalidKeySpecException("positive key length required: "
                        + pbeSpec.getKeyLength());
                }

                if (pbeSpec.getPassword().length == 0)
                {
                    throw new IllegalArgumentException("password empty");
                }

                if (pbeSpec instanceof PBKDF2KeySpec)
                {
                    PBKDF2KeySpec spec = (PBKDF2KeySpec)pbeSpec;

                    int digest = getDigestCode(spec.getPrf().getAlgorithm());
                    int keySize = pbeSpec.getKeyLength();
                    int ivSize = -1;    // JDK 1,2 and earlier does not understand simplified version.
                    CipherParameters param = PBE.Util.makePBEMacParameters(pbeSpec, scheme, digest, keySize);

                    return new BCPBEKey(this.algName, this.algOid, scheme, digest, keySize, ivSize, pbeSpec, param);
                }
                else
                {
                    int digest = SHA1;
                    int keySize = pbeSpec.getKeyLength();
                    int ivSize = -1;    // JDK 1,2 and earlier does not understand simplified version.
                    CipherParameters param = PBE.Util.makePBEMacParameters(pbeSpec, scheme, digest, keySize);

                    return new BCPBEKey(this.algName, this.algOid, scheme, digest, keySize, ivSize, pbeSpec, param);
                }
            }

            throw new InvalidKeySpecException("Invalid KeySpec");
        }


        private int getDigestCode(ASN1ObjectIdentifier algorithm)
            throws InvalidKeySpecException
        {
            if (algorithm.equals(CryptoProObjectIdentifiers.gostR3411Hmac))
            {
                return GOST3411;
            }
            else if (algorithm.equals(PKCSObjectIdentifiers.id_hmacWithSHA1))
            {
                return SHA1;
            }

            throw new InvalidKeySpecException("Invalid KeySpec: unknown PRF algorithm " + algorithm);
        }
    }

    public static class PBKDF2withUTF8
        extends BasePBKDF2
    {
        public PBKDF2withUTF8()
        {
            super("PBKDF2", PKCS5S2_UTF8);
        }
    }

    public static class Mappings
        extends AlgorithmProvider
    {
        private static final String PREFIX = PBEPBKDF2.class.getName();

        public Mappings()
        {
        }

        public void configure(ConfigurableProvider provider)
        {
            provider.addAlgorithm("AlgorithmParameters.PBKDF2", PREFIX + "$AlgParams");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters." + PKCSObjectIdentifiers.id_PBKDF2, "PBKDF2");
            provider.addAlgorithm("SecretKeyFactory.PBKDF2", PREFIX + "$PBKDF2withUTF8");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory." + PKCSObjectIdentifiers.id_PBKDF2, "PBKDF2");
        }
    }
}
