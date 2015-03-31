package org.spongycastle.cms;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.spongycastle.asn1.ASN1Encoding;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Set;
import org.spongycastle.asn1.DEROctetString;
import org.spongycastle.asn1.DERSet;
import org.spongycastle.asn1.cms.AttributeTable;
import org.spongycastle.asn1.cms.SignerIdentifier;
import org.spongycastle.asn1.cms.SignerInfo;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.spongycastle.operator.DigestAlgorithmIdentifierFinder;
import org.spongycastle.operator.DigestCalculator;
import org.spongycastle.operator.DigestCalculatorProvider;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.io.TeeOutputStream;

public class SignerInfoGenerator
{
    private final SignerIdentifier signerIdentifier;
    private final CMSAttributeTableGenerator sAttrGen;
    private final CMSAttributeTableGenerator unsAttrGen;
    private final ContentSigner signer;
    private final DigestCalculator digester;
    private final DigestAlgorithmIdentifierFinder digAlgFinder = new DefaultDigestAlgorithmIdentifierFinder();
    private final CMSSignatureEncryptionAlgorithmFinder sigEncAlgFinder;

    private byte[] calculatedDigest = null;
    private X509CertificateHolder certHolder;

    SignerInfoGenerator(
        SignerIdentifier signerIdentifier,
        ContentSigner signer,
        DigestCalculatorProvider digesterProvider,
        CMSSignatureEncryptionAlgorithmFinder sigEncAlgFinder)
        throws OperatorCreationException
    {
        this(signerIdentifier, signer, digesterProvider, sigEncAlgFinder, false);
    }

    SignerInfoGenerator(
        SignerIdentifier signerIdentifier,
        ContentSigner signer,
        DigestCalculatorProvider digesterProvider,
        CMSSignatureEncryptionAlgorithmFinder sigEncAlgFinder,
        boolean isDirectSignature)
        throws OperatorCreationException
    {
        this.signerIdentifier = signerIdentifier;
        this.signer = signer;

        if (digesterProvider != null)
        {
            this.digester = digesterProvider.get(digAlgFinder.find(signer.getAlgorithmIdentifier()));
        }
        else
        {
            this.digester = null;
        }

        if (isDirectSignature)
        {
            this.sAttrGen = null;
            this.unsAttrGen = null;
        }
        else
        {
            this.sAttrGen = new DefaultSignedAttributeTableGenerator();
            this.unsAttrGen = null;
        }

        this.sigEncAlgFinder = sigEncAlgFinder;
    }

    public SignerInfoGenerator(
        SignerInfoGenerator original,
        CMSAttributeTableGenerator sAttrGen,
        CMSAttributeTableGenerator unsAttrGen)
    {
        this.signerIdentifier = original.signerIdentifier;
        this.signer = original.signer;
        this.digester = original.digester;
        this.sigEncAlgFinder = original.sigEncAlgFinder;
        this.sAttrGen = sAttrGen;
        this.unsAttrGen = unsAttrGen;
    }

    SignerInfoGenerator(
        SignerIdentifier signerIdentifier,
        ContentSigner signer,
        DigestCalculatorProvider digesterProvider,
        CMSSignatureEncryptionAlgorithmFinder sigEncAlgFinder,
        CMSAttributeTableGenerator sAttrGen,
        CMSAttributeTableGenerator unsAttrGen)
        throws OperatorCreationException
    {
        this.signerIdentifier = signerIdentifier;
        this.signer = signer;

        if (digesterProvider != null)
        {
            this.digester = digesterProvider.get(digAlgFinder.find(signer.getAlgorithmIdentifier()));
        }
        else
        {
            this.digester = null;
        }

        this.sAttrGen = sAttrGen;
        this.unsAttrGen = unsAttrGen;
        this.sigEncAlgFinder = sigEncAlgFinder;
    }

    public SignerIdentifier getSID()
    {
        return signerIdentifier;
    }

    public int getGeneratedVersion()
    {
        return signerIdentifier.isTagged() ? 3 : 1;
    }

    public boolean hasAssociatedCertificate()
    {
        return certHolder != null;
    }

    public X509CertificateHolder getAssociatedCertificate()
    {
        return certHolder;
    }
    
    public AlgorithmIdentifier getDigestAlgorithm()
    {
        if (digester != null)
        {
            return digester.getAlgorithmIdentifier();
        }

        return digAlgFinder.find(signer.getAlgorithmIdentifier());
    }
    
    public OutputStream getCalculatingOutputStream()
    {
        if (digester != null)
        {
            if (sAttrGen == null)
            {
                return new TeeOutputStream(digester.getOutputStream(), signer.getOutputStream());    
            }
            return digester.getOutputStream();
        }
        else
        {
            return signer.getOutputStream();
        }
    }

    public SignerInfo generate(ASN1ObjectIdentifier contentType)
        throws CMSException
    {
        try
        {
            /* RFC 3852 5.4
             * The result of the message digest calculation process depends on
             * whether the signedAttrs field is present.  When the field is absent,
             * the result is just the message digest of the content as described
             *
             * above.  When the field is present, however, the result is the message
             * digest of the complete DER encoding of the SignedAttrs value
             * contained in the signedAttrs field.
             */
            ASN1Set signedAttr = null;

            AlgorithmIdentifier digestAlg = null;

            if (sAttrGen != null)
            {
                digestAlg = digester.getAlgorithmIdentifier();
                calculatedDigest = digester.getDigest();
                Map parameters = getBaseParameters(contentType, digester.getAlgorithmIdentifier(), calculatedDigest);
                AttributeTable signed = sAttrGen.getAttributes(Collections.unmodifiableMap(parameters));

                signedAttr = getAttributeSet(signed);

                // sig must be composed from the DER encoding.
                OutputStream sOut = signer.getOutputStream();

                sOut.write(signedAttr.getEncoded(ASN1Encoding.DER));

                sOut.close();
            }
            else
            {
                if (digester != null)
                {
                    digestAlg = digester.getAlgorithmIdentifier();
                    calculatedDigest = digester.getDigest();
                }
                else
                {
                    digestAlg = digAlgFinder.find(signer.getAlgorithmIdentifier());
                    calculatedDigest = null;
                }
            }

            byte[] sigBytes = signer.getSignature();

            ASN1Set unsignedAttr = null;
            if (unsAttrGen != null)
            {
                Map parameters = getBaseParameters(contentType, digestAlg, calculatedDigest);
                parameters.put(CMSAttributeTableGenerator.SIGNATURE, Arrays.clone(sigBytes));

                AttributeTable unsigned = unsAttrGen.getAttributes(Collections.unmodifiableMap(parameters));

                unsignedAttr = getAttributeSet(unsigned);
            }

            AlgorithmIdentifier digestEncryptionAlgorithm = sigEncAlgFinder.findEncryptionAlgorithm(signer.getAlgorithmIdentifier());

            return new SignerInfo(signerIdentifier, digestAlg,
                signedAttr, digestEncryptionAlgorithm, new DEROctetString(sigBytes), unsignedAttr);
        }
        catch (IOException e)
        {
            throw new CMSException("encoding error.", e);
        }
    }

    void setAssociatedCertificate(X509CertificateHolder certHolder)
    {
        this.certHolder = certHolder;
    }

    private ASN1Set getAttributeSet(
        AttributeTable attr)
    {
        if (attr != null)
        {
            return new DERSet(attr.toASN1EncodableVector());
        }

        return null;
    }

    private Map getBaseParameters(ASN1ObjectIdentifier contentType, AlgorithmIdentifier digAlgId, byte[] hash)
    {
        Map param = new HashMap();

        if (contentType != null)
        {
            param.put(CMSAttributeTableGenerator.CONTENT_TYPE, contentType);
        }

        param.put(CMSAttributeTableGenerator.DIGEST_ALGORITHM_IDENTIFIER, digAlgId);
        param.put(CMSAttributeTableGenerator.DIGEST,  Arrays.clone(hash));
        return param;
    }

    public byte[] getCalculatedDigest()
    {
        if (calculatedDigest != null)
        {
            return Arrays.clone(calculatedDigest);
        }

        return null;
    }

    public CMSAttributeTableGenerator getSignedAttributeTableGenerator()
    {
        return sAttrGen;
    }

    public CMSAttributeTableGenerator getUnsignedAttributeTableGenerator()
    {
        return unsAttrGen;
    }
}
