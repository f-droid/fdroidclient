package org.spongycastle.cms;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.operator.ContentVerifier;
import org.spongycastle.operator.ContentVerifierProvider;
import org.spongycastle.operator.DigestCalculator;
import org.spongycastle.operator.DigestCalculatorProvider;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.SignatureAlgorithmIdentifierFinder;

public class SignerInformationVerifier
{
    private ContentVerifierProvider verifierProvider;
    private DigestCalculatorProvider digestProvider;
    private SignatureAlgorithmIdentifierFinder sigAlgorithmFinder;
    private CMSSignatureAlgorithmNameGenerator sigNameGenerator;

    public SignerInformationVerifier(CMSSignatureAlgorithmNameGenerator sigNameGenerator, SignatureAlgorithmIdentifierFinder sigAlgorithmFinder, ContentVerifierProvider verifierProvider, DigestCalculatorProvider digestProvider)
    {
        this.sigNameGenerator = sigNameGenerator;
        this.sigAlgorithmFinder = sigAlgorithmFinder;
        this.verifierProvider = verifierProvider;
        this.digestProvider = digestProvider;
    }

    public boolean hasAssociatedCertificate()
    {
        return verifierProvider.hasAssociatedCertificate();
    }

    public X509CertificateHolder getAssociatedCertificate()
    {
        return verifierProvider.getAssociatedCertificate();
    }

    public ContentVerifier getContentVerifier(AlgorithmIdentifier signingAlgorithm, AlgorithmIdentifier digestAlgorithm)
        throws OperatorCreationException
    {
        String          signatureName = sigNameGenerator.getSignatureName(digestAlgorithm, signingAlgorithm);

        return verifierProvider.get(sigAlgorithmFinder.find(signatureName));
    }

    public DigestCalculator getDigestCalculator(AlgorithmIdentifier algorithmIdentifier)
        throws OperatorCreationException
    {
        return digestProvider.get(algorithmIdentifier);
    }
}
