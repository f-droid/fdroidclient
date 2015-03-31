package org.spongycastle.operator.bc;

import java.io.IOException;
import java.io.OutputStream;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.crypto.Signer;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.operator.ContentVerifier;
import org.spongycastle.operator.ContentVerifierProvider;
import org.spongycastle.operator.OperatorCreationException;

public abstract class BcContentVerifierProviderBuilder
{
    protected BcDigestProvider digestProvider;

    public BcContentVerifierProviderBuilder()
    {
        this.digestProvider = BcDefaultDigestProvider.INSTANCE;
    }

    public ContentVerifierProvider build(final X509CertificateHolder certHolder)
        throws OperatorCreationException
    {
        return new ContentVerifierProvider()
        {
            public boolean hasAssociatedCertificate()
            {
                return true;
            }

            public X509CertificateHolder getAssociatedCertificate()
            {
                return certHolder;
            }

            public ContentVerifier get(AlgorithmIdentifier algorithm)
                throws OperatorCreationException
            {
                try
                {
                    AsymmetricKeyParameter publicKey = extractKeyParameters(certHolder.getSubjectPublicKeyInfo());
                    BcSignerOutputStream stream = createSignatureStream(algorithm, publicKey);

                    return new SigVerifier(algorithm, stream);
                }
                catch (IOException e)
                {
                    throw new OperatorCreationException("exception on setup: " + e, e);
                }
            }
        };
    }

    public ContentVerifierProvider build(final AsymmetricKeyParameter publicKey)
        throws OperatorCreationException
    {
        return new ContentVerifierProvider()
        {
            public boolean hasAssociatedCertificate()
            {
                return false;
            }

            public X509CertificateHolder getAssociatedCertificate()
            {
                return null;
            }

            public ContentVerifier get(AlgorithmIdentifier algorithm)
                throws OperatorCreationException
            {
                BcSignerOutputStream stream = createSignatureStream(algorithm, publicKey);

                return new SigVerifier(algorithm, stream);
            }
        };
    }

    private BcSignerOutputStream createSignatureStream(AlgorithmIdentifier algorithm, AsymmetricKeyParameter publicKey)
        throws OperatorCreationException
    {
        Signer sig = createSigner(algorithm);

        sig.init(false, publicKey);

        return new BcSignerOutputStream(sig);
    }

    /**
     * Extract an AsymmetricKeyParameter from the passed in SubjectPublicKeyInfo structure.
     *
     * @param publicKeyInfo a publicKeyInfo structure describing the public key required.
     * @return an AsymmetricKeyParameter object containing the appropriate public key.
     * @throws IOException if the publicKeyInfo data cannot be parsed,
     */
    protected abstract AsymmetricKeyParameter extractKeyParameters(SubjectPublicKeyInfo publicKeyInfo)
        throws IOException;

    /**
     * Create the correct signer for the algorithm identifier sigAlgId.
     *
     * @param sigAlgId the algorithm details for the signature we want to verify.
     * @return a Signer object.
     * @throws OperatorCreationException if the Signer cannot be constructed.
     */
    protected abstract Signer createSigner(AlgorithmIdentifier sigAlgId)
        throws OperatorCreationException;

    private class SigVerifier
        implements ContentVerifier
    {
        private BcSignerOutputStream stream;
        private AlgorithmIdentifier algorithm;

        SigVerifier(AlgorithmIdentifier algorithm, BcSignerOutputStream stream)
        {
            this.algorithm = algorithm;
            this.stream = stream;
        }

        public AlgorithmIdentifier getAlgorithmIdentifier()
        {
            return algorithm;
        }

        public OutputStream getOutputStream()
        {
            if (stream == null)
            {
                throw new IllegalStateException("verifier not initialised");
            }

            return stream;
        }

        public boolean verify(byte[] expected)
        {
            return stream.verify(expected);
        }
    }
}