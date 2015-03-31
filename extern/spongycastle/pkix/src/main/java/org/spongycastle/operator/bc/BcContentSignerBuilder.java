package org.spongycastle.operator.bc;

import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Map;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.crypto.CryptoException;
import org.spongycastle.crypto.Signer;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.params.ParametersWithRandom;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.RuntimeOperatorException;

public abstract class BcContentSignerBuilder
{
    private SecureRandom random;
    private AlgorithmIdentifier sigAlgId;
    private AlgorithmIdentifier digAlgId;

    protected BcDigestProvider                digestProvider;

    public BcContentSignerBuilder(AlgorithmIdentifier sigAlgId, AlgorithmIdentifier digAlgId)
    {
        this.sigAlgId = sigAlgId;
        this.digAlgId = digAlgId;
        this.digestProvider = BcDefaultDigestProvider.INSTANCE;
    }

    public BcContentSignerBuilder setSecureRandom(SecureRandom random)
    {
        this.random = random;

        return this;
    }

    public ContentSigner build(AsymmetricKeyParameter privateKey)
        throws OperatorCreationException
    {
        final Signer sig = createSigner(sigAlgId, digAlgId);

        if (random != null)
        {
            sig.init(true, new ParametersWithRandom(privateKey, random));
        }
        else
        {
            sig.init(true, privateKey);
        }

        return new ContentSigner()
        {
            private BcSignerOutputStream stream = new BcSignerOutputStream(sig);

            public AlgorithmIdentifier getAlgorithmIdentifier()
            {
                return sigAlgId;
            }

            public OutputStream getOutputStream()
            {
                return stream;
            }

            public byte[] getSignature()
            {
                try
                {
                    return stream.getSignature();
                }
                catch (CryptoException e)
                {
                    throw new RuntimeOperatorException("exception obtaining signature: " + e.getMessage(), e);
                }
            }
        };
    }

    protected abstract Signer createSigner(AlgorithmIdentifier sigAlgId, AlgorithmIdentifier algorithmIdentifier)
        throws OperatorCreationException;
}
