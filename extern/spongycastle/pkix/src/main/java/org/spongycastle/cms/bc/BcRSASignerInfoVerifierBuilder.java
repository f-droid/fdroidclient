package org.spongycastle.cms.bc;

import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cms.CMSSignatureAlgorithmNameGenerator;
import org.spongycastle.cms.SignerInformationVerifier;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.operator.DigestAlgorithmIdentifierFinder;
import org.spongycastle.operator.DigestCalculatorProvider;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.SignatureAlgorithmIdentifierFinder;
import org.spongycastle.operator.bc.BcRSAContentVerifierProviderBuilder;

public class BcRSASignerInfoVerifierBuilder
{
    private BcRSAContentVerifierProviderBuilder contentVerifierProviderBuilder;
    private DigestCalculatorProvider digestCalculatorProvider;
    private CMSSignatureAlgorithmNameGenerator sigAlgNameGen;
    private SignatureAlgorithmIdentifierFinder sigAlgIdFinder;

    public BcRSASignerInfoVerifierBuilder(CMSSignatureAlgorithmNameGenerator sigAlgNameGen, SignatureAlgorithmIdentifierFinder sigAlgIdFinder, DigestAlgorithmIdentifierFinder digestAlgorithmFinder, DigestCalculatorProvider digestCalculatorProvider)
    {
        this.sigAlgNameGen = sigAlgNameGen;
        this.sigAlgIdFinder = sigAlgIdFinder;
        this.contentVerifierProviderBuilder = new BcRSAContentVerifierProviderBuilder(digestAlgorithmFinder);
        this.digestCalculatorProvider = digestCalculatorProvider;
    }

    public SignerInformationVerifier build(X509CertificateHolder certHolder)
        throws OperatorCreationException
    {
        return new SignerInformationVerifier(sigAlgNameGen, sigAlgIdFinder, contentVerifierProviderBuilder.build(certHolder), digestCalculatorProvider);
    }

    public SignerInformationVerifier build(AsymmetricKeyParameter pubKey)
        throws OperatorCreationException
    {
        return new SignerInformationVerifier(sigAlgNameGen, sigAlgIdFinder, contentVerifierProviderBuilder.build(pubKey), digestCalculatorProvider);
    }
}
