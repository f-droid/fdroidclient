package org.spongycastle.cms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.ASN1Set;
import org.spongycastle.asn1.BERSequence;
import org.spongycastle.asn1.DERSet;
import org.spongycastle.asn1.cms.ContentInfo;
import org.spongycastle.asn1.cms.SignedData;
import org.spongycastle.asn1.cms.SignerInfo;
import org.spongycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.SignatureAlgorithmIdentifierFinder;
import org.spongycastle.util.Store;

/**
 * general class for handling a pkcs7-signature message.
 *
 * A simple example of usage - note, in the example below the validity of
 * the certificate isn't verified, just the fact that one of the certs 
 * matches the given signer...
 *
 * <pre>
 *  Store                   certStore = s.getCertificates();
 *  SignerInformationStore  signers = s.getSignerInfos();
 *  Collection              c = signers.getSigners();
 *  Iterator                it = c.iterator();
 *  
 *  while (it.hasNext())
 *  {
 *      SignerInformation   signer = (SignerInformation)it.next();
 *      Collection          certCollection = certStore.getMatches(signer.getSID());
 *
 *      Iterator              certIt = certCollection.iterator();
 *      X509CertificateHolder cert = (X509CertificateHolder)certIt.next();
 *  
 *      if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("SC").build(cert)))
 *      {
 *          verified++;
 *      }   
 *  }
 * </pre>
 */
public class CMSSignedData
{
    private static final CMSSignedHelper HELPER = CMSSignedHelper.INSTANCE;
    
    SignedData              signedData;
    ContentInfo             contentInfo;
    CMSTypedData            signedContent;
    SignerInformationStore  signerInfoStore;

    private Map             hashes;

    private CMSSignedData(
        CMSSignedData   c)
    {
        this.signedData = c.signedData;
        this.contentInfo = c.contentInfo;
        this.signedContent = c.signedContent;
        this.signerInfoStore = c.signerInfoStore;
    }

    public CMSSignedData(
        byte[]      sigBlock)
        throws CMSException
    {
        this(CMSUtils.readContentInfo(sigBlock));
    }

    public CMSSignedData(
        CMSProcessable  signedContent,
        byte[]          sigBlock)
        throws CMSException
    {
        this(signedContent, CMSUtils.readContentInfo(sigBlock));
    }

    /**
     * Content with detached signature, digests precomputed
     *
     * @param hashes a map of precomputed digests for content indexed by name of hash.
     * @param sigBlock the signature object.
     */
    public CMSSignedData(
        Map     hashes,
        byte[]  sigBlock)
        throws CMSException
    {
        this(hashes, CMSUtils.readContentInfo(sigBlock));
    }

    /**
     * base constructor - content with detached signature.
     *
     * @param signedContent the content that was signed.
     * @param sigData the signature object.
     */
    public CMSSignedData(
        CMSProcessable  signedContent,
        InputStream     sigData)
        throws CMSException
    {
        this(signedContent, CMSUtils.readContentInfo(new ASN1InputStream(sigData)));
    }

    /**
     * base constructor - with encapsulated content
     */
    public CMSSignedData(
        InputStream sigData)
        throws CMSException
    {
        this(CMSUtils.readContentInfo(sigData));
    }

    public CMSSignedData(
        final CMSProcessable  signedContent,
        ContentInfo     sigData)
        throws CMSException
    {
        if (signedContent instanceof CMSTypedData)
        {
            this.signedContent = (CMSTypedData)signedContent;
        }
        else
        {
            this.signedContent = new CMSTypedData()
            {
                public ASN1ObjectIdentifier getContentType()
                {
                    return signedData.getEncapContentInfo().getContentType();
                }

                public void write(OutputStream out)
                    throws IOException, CMSException
                {
                    signedContent.write(out);
                }

                public Object getContent()
                {
                    return signedContent.getContent();
                }
            };
        }

        this.contentInfo = sigData;
        this.signedData = getSignedData();
    }

    public CMSSignedData(
        Map             hashes,
        ContentInfo     sigData)
        throws CMSException
    {
        this.hashes = hashes;
        this.contentInfo = sigData;
        this.signedData = getSignedData();
    }

    public CMSSignedData(
        ContentInfo sigData)
        throws CMSException
    {
        this.contentInfo = sigData;
        this.signedData = getSignedData();

        //
        // this can happen if the signed message is sent simply to send a
        // certificate chain.
        //
        if (signedData.getEncapContentInfo().getContent() != null)
        {
            this.signedContent = new CMSProcessableByteArray(signedData.getEncapContentInfo().getContentType(),
                    ((ASN1OctetString)(signedData.getEncapContentInfo()
                                                .getContent())).getOctets());
        }
        else
        {
            this.signedContent = null;
        }
    }

    private SignedData getSignedData()
        throws CMSException
    {
        try
        {
            return SignedData.getInstance(contentInfo.getContent());
        }
        catch (ClassCastException e)
        {
            throw new CMSException("Malformed content.", e);
        }
        catch (IllegalArgumentException e)
        {
            throw new CMSException("Malformed content.", e);
        }
    }

    /**
     * Return the version number for this object
     */
    public int getVersion()
    {
        return signedData.getVersion().getValue().intValue();
    }

    /**
     * return the collection of signers that are associated with the
     * signatures for the message.
     */
    public SignerInformationStore getSignerInfos()
    {
        if (signerInfoStore == null)
        {
            ASN1Set         s = signedData.getSignerInfos();
            List            signerInfos = new ArrayList();
            SignatureAlgorithmIdentifierFinder sigAlgFinder = new DefaultSignatureAlgorithmIdentifierFinder();

            for (int i = 0; i != s.size(); i++)
            {
                SignerInfo info = SignerInfo.getInstance(s.getObjectAt(i));
                ASN1ObjectIdentifier contentType = signedData.getEncapContentInfo().getContentType();

                if (hashes == null)
                {
                    signerInfos.add(new SignerInformation(info, contentType, signedContent, null));
                }
                else
                {
                    Object obj = hashes.keySet().iterator().next();
                    byte[] hash = (obj instanceof String) ? (byte[])hashes.get(info.getDigestAlgorithm().getAlgorithm().getId()) : (byte[])hashes.get(info.getDigestAlgorithm().getAlgorithm());

                    signerInfos.add(new SignerInformation(info, contentType, null, hash));
                }
            }

            signerInfoStore = new SignerInformationStore(signerInfos);
        }

        return signerInfoStore;
    }

    /**
     * Return any X.509 certificate objects in this SignedData structure as a Store of X509CertificateHolder objects.
     *
     * @return a Store of X509CertificateHolder objects.
     */
    public Store getCertificates()
    {
        return HELPER.getCertificates(signedData.getCertificates());
    }

    /**
     * Return any X.509 CRL objects in this SignedData structure as a Store of X509CRLHolder objects.
     *
     * @return a Store of X509CRLHolder objects.
     */
    public Store getCRLs()
    {
        return HELPER.getCRLs(signedData.getCRLs());
    }

    /**
     * Return any X.509 attribute certificate objects in this SignedData structure as a Store of X509AttributeCertificateHolder objects.
     *
     * @return a Store of X509AttributeCertificateHolder objects.
     */
    public Store getAttributeCertificates()
    {
        return HELPER.getAttributeCertificates(signedData.getCertificates());
    }

    /**
     * Return any OtherRevocationInfo OtherRevInfo objects of the type indicated by otherRevocationInfoFormat in
     * this SignedData structure.
     *
     * @param otherRevocationInfoFormat OID of the format type been looked for.
     *
     * @return a Store of ASN1Encodable objects representing any objects of otherRevocationInfoFormat found.
     */
    public Store getOtherRevocationInfo(ASN1ObjectIdentifier otherRevocationInfoFormat)
    {
        return HELPER.getOtherRevocationInfo(otherRevocationInfoFormat, signedData.getCRLs());
    }

    /**
     * Return the a string representation of the OID associated with the
     * encapsulated content info structure carried in the signed data.
     * 
     * @return the OID for the content type.
     */
    public String getSignedContentTypeOID()
    {
        return signedData.getEncapContentInfo().getContentType().getId();
    }
    
    public CMSTypedData getSignedContent()
    {
        return signedContent;
    }

    /**
     * return the ContentInfo
     */
    public ContentInfo toASN1Structure()
    {
        return contentInfo;
    }

    /**
     * return the ASN.1 encoded representation of this object.
     */
    public byte[] getEncoded()
        throws IOException
    {
        return contentInfo.getEncoded();
    }

    /**
     * Verify all the SignerInformation objects and their associated counter signatures attached
     * to this CMS SignedData object.
     *
     * @param verifierProvider  a provider of SignerInformationVerifier objects.
     * @return true if all verify, false otherwise.
     * @throws CMSException  if an exception occurs during the verification process.
     */
    public boolean verifySignatures(SignerInformationVerifierProvider verifierProvider)
        throws CMSException
    {
        return verifySignatures(verifierProvider, false);
    }

    /**
     * Verify all the SignerInformation objects and optionally their associated counter signatures attached
     * to this CMS SignedData object.
     *
     * @param verifierProvider  a provider of SignerInformationVerifier objects.
     * @param ignoreCounterSignatures if true don't check counter signatures. If false check counter signatures as well.
     * @return true if all verify, false otherwise.
     * @throws CMSException  if an exception occurs during the verification process.
     */
    public boolean verifySignatures(SignerInformationVerifierProvider verifierProvider, boolean ignoreCounterSignatures)
        throws CMSException
    {
        Collection signers = this.getSignerInfos().getSigners();

        for (Iterator it = signers.iterator(); it.hasNext();)
        {
            SignerInformation signer = (SignerInformation)it.next();

            try
            {
                SignerInformationVerifier verifier = verifierProvider.get(signer.getSID());

                if (!signer.verify(verifier))
                {
                    return false;
                }

                if (!ignoreCounterSignatures)
                {
                    Collection counterSigners = signer.getCounterSignatures().getSigners();

                    for  (Iterator cIt = counterSigners.iterator(); cIt.hasNext();)
                    {
                        SignerInformation counterSigner = (SignerInformation)cIt.next();
                        SignerInformationVerifier counterVerifier = verifierProvider.get(signer.getSID());

                        if (!counterSigner.verify(counterVerifier))
                        {
                            return false;
                        }
                    }
                }
            }
            catch (OperatorCreationException e)
            {
                throw new CMSException("failure in verifier provider: " + e.getMessage(), e);
            }
        }

        return true;
    }

    /**
     * Replace the SignerInformation store associated with this
     * CMSSignedData object with the new one passed in. You would
     * probably only want to do this if you wanted to change the unsigned 
     * attributes associated with a signer, or perhaps delete one.
     * 
     * @param signedData the signed data object to be used as a base.
     * @param signerInformationStore the new signer information store to use.
     * @return a new signed data object.
     */
    public static CMSSignedData replaceSigners(
        CMSSignedData           signedData,
        SignerInformationStore  signerInformationStore)
    {
        //
        // copy
        //
        CMSSignedData   cms = new CMSSignedData(signedData);
        
        //
        // replace the store
        //
        cms.signerInfoStore = signerInformationStore;

        //
        // replace the signers in the SignedData object
        //
        ASN1EncodableVector digestAlgs = new ASN1EncodableVector();
        ASN1EncodableVector vec = new ASN1EncodableVector();
        
        Iterator    it = signerInformationStore.getSigners().iterator();
        while (it.hasNext())
        {
            SignerInformation signer = (SignerInformation)it.next();
            digestAlgs.add(CMSSignedHelper.INSTANCE.fixAlgID(signer.getDigestAlgorithmID()));
            vec.add(signer.toASN1Structure());
        }

        ASN1Set             digests = new DERSet(digestAlgs);
        ASN1Set             signers = new DERSet(vec);
        ASN1Sequence        sD = (ASN1Sequence)signedData.signedData.toASN1Primitive();

        vec = new ASN1EncodableVector();
        
        //
        // signers are the last item in the sequence.
        //
        vec.add(sD.getObjectAt(0)); // version
        vec.add(digests);

        for (int i = 2; i != sD.size() - 1; i++)
        {
            vec.add(sD.getObjectAt(i));
        }
        
        vec.add(signers);
        
        cms.signedData = SignedData.getInstance(new BERSequence(vec));
        
        //
        // replace the contentInfo with the new one
        //
        cms.contentInfo = new ContentInfo(cms.contentInfo.getContentType(), cms.signedData);
        
        return cms;
    }

    /**
     * Replace the certificate and CRL information associated with this
     * CMSSignedData object with the new one passed in.
     *
     * @param signedData the signed data object to be used as a base.
     * @param certificates the new certificates to be used.
     * @param attrCerts the new attribute certificates to be used.
     * @param crls the new CRLs to be used.
     * @return a new signed data object.
     * @exception CMSException if there is an error processing the CertStore
     */
    public static CMSSignedData replaceCertificatesAndCRLs(
        CMSSignedData   signedData,
        Store           certificates,
        Store           attrCerts,
        Store           crls)
        throws CMSException
    {
        //
        // copy
        //
        CMSSignedData   cms = new CMSSignedData(signedData);

        //
        // replace the certs and crls in the SignedData object
        //
        ASN1Set certSet = null;
        ASN1Set crlSet = null;

        if (certificates != null || attrCerts != null)
        {
            List certs = new ArrayList();

            if (certificates != null)
            {
                certs.addAll(CMSUtils.getCertificatesFromStore(certificates));
            }
            if (attrCerts != null)
            {
                certs.addAll(CMSUtils.getAttributeCertificatesFromStore(attrCerts));   
            }

            ASN1Set set = CMSUtils.createBerSetFromList(certs);

            if (set.size() != 0)
            {
                certSet = set;
            }
        }

        if (crls != null)
        {
            ASN1Set set = CMSUtils.createBerSetFromList(CMSUtils.getCRLsFromStore(crls));

            if (set.size() != 0)
            {
                crlSet = set;
            }
        }

        //
        // replace the CMS structure.
        //
        cms.signedData = new SignedData(signedData.signedData.getDigestAlgorithms(),
                                   signedData.signedData.getEncapContentInfo(),
                                   certSet,
                                   crlSet,
                                   signedData.signedData.getSignerInfos());

        //
        // replace the contentInfo with the new one
        //
        cms.contentInfo = new ContentInfo(cms.contentInfo.getContentType(), cms.signedData);

        return cms;
    }
}
