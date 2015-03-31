package org.spongycastle.pkcs.bc;

import java.io.InputStream;

import org.spongycastle.asn1.pkcs.PKCS12PBEParams;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.ExtendedDigest;
import org.spongycastle.crypto.digests.SHA1Digest;
import org.spongycastle.crypto.generators.PKCS12ParametersGenerator;
import org.spongycastle.crypto.io.CipherInputStream;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.operator.GenericKey;
import org.spongycastle.operator.InputDecryptor;
import org.spongycastle.operator.InputDecryptorProvider;

public class BcPKCS12PBEInputDecryptorProviderBuilder
{
    private ExtendedDigest digest;

    public BcPKCS12PBEInputDecryptorProviderBuilder()
    {
         this(new SHA1Digest());
    }

    public BcPKCS12PBEInputDecryptorProviderBuilder(ExtendedDigest digest)
    {
         this.digest = digest;
    }

    public InputDecryptorProvider build(final char[] password)
    {
        return new InputDecryptorProvider()
        {
            public InputDecryptor get(final AlgorithmIdentifier algorithmIdentifier)
            {
                final PaddedBufferedBlockCipher engine = PKCS12PBEUtils.getEngine(algorithmIdentifier.getAlgorithm());

                PKCS12PBEParams           pbeParams = PKCS12PBEParams.getInstance(algorithmIdentifier.getParameters());

                CipherParameters params = PKCS12PBEUtils.createCipherParameters(algorithmIdentifier.getAlgorithm(), digest, engine.getBlockSize(), pbeParams, password);

                engine.init(false, params);

                return new InputDecryptor()
                {
                    public AlgorithmIdentifier getAlgorithmIdentifier()
                    {
                        return algorithmIdentifier;
                    }

                    public InputStream getInputStream(InputStream input)
                    {
                        return new CipherInputStream(input, engine);
                    }

                    public GenericKey getKey()
                    {
                        return new GenericKey(PKCS12ParametersGenerator.PKCS12PasswordToBytes(password));
                    }
                };
            }
        };

    }
}
