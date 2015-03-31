package org.spongycastle.cms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1Generator;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1OctetStringParser;
import org.spongycastle.asn1.ASN1SequenceParser;
import org.spongycastle.asn1.ASN1Set;
import org.spongycastle.asn1.ASN1SetParser;
import org.spongycastle.asn1.ASN1StreamParser;
import org.spongycastle.asn1.BERSequenceGenerator;
import org.spongycastle.asn1.BERSetParser;
import org.spongycastle.asn1.BERTaggedObject;
import org.spongycastle.asn1.BERTags;
import org.spongycastle.asn1.DERSet;
import org.spongycastle.asn1.DERTaggedObject;
import org.spongycastle.asn1.cms.CMSObjectIdentifiers;
import org.spongycastle.asn1.cms.ContentInfoParser;
import org.spongycastle.asn1.cms.SignedDataParser;
import org.spongycastle.asn1.cms.SignerInfo;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.operator.DigestCalculator;
import org.spongycastle.operator.DigestCalculatorProvider;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.util.Store;
import org.spongycastle.util.io.Streams;

/**
 * Parsing class for an CMS Signed Data object from an input stream.
 * <p>
 * Note: that because we are in a streaming mode only one signer can be tried and it is important 
 * that the methods on the parser are called in the appropriate order.
 * </p>
 * <p>
 * A simple example of usage for an encapsulated signature.
 * </p>
 * <p>
 * Two notes: first, in the example below the validity of
 * the certificate isn't verified, just the fact that one of the certs 
 * matches the given signer, and, second, because we are in a streaming
 * mode the order of the operations is important.
 * </p>
 * <pre>
 *      CMSSignedDataParser     sp = new CMSSignedDataParser(new JcaDigestCalculatorProviderBuilder().setProvider("SC").build(), encapSigData);
 *
 *      sp.getSignedContent().drain();
 *
 *      Store                   certStore = sp.getCertificates();
 *      SignerInformationStore  signers = sp.getSignerInfos();
 *      
 *      Collection              c = signers.getSigners();
 *      Iterator                it = c.iterator();
 *
 *      while (it.hasNext())
 *      {
 *          SignerInformation   signer = (SignerInformation)it.next();
 *          Collection          certCollection = certStore.getMatches(signer.getSID());
 *
 *          Iterator        certIt = certCollection.iterator();
 *          X509CertificateHolder cert = (X509CertificateHolder)certIt.next();
 *
 *          System.out.println("verify returns: " + signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("SC").build(cert)));
 *      }
 * </pre>
 *  Note also: this class does not introduce buffering - if you are processing large files you should create
 *  the parser with:
 *  <pre>
 *          CMSSignedDataParser     ep = new CMSSignedDataParser(new BufferedInputStream(encapSigData, bufSize));
 *  </pre>
 *  where bufSize is a suitably large buffer size.
 */
public class CMSSignedDataParser
    extends CMSContentInfoParser
{
    private static final CMSSignedHelper HELPER = CMSSignedHelper.INSTANCE;

    private SignedDataParser        _signedData;
    private ASN1ObjectIdentifier    _signedContentType;
    private CMSTypedStream          _signedContent;
    private Map                     digests;

    private SignerInformationStore  _signerInfoStore;
    private ASN1Set                 _certSet, _crlSet;
    private boolean                 _isCertCrlParsed;

    public CMSSignedDataParser(
        DigestCalculatorProvider digestCalculatorProvider,
        byte[]      sigBlock)
        throws CMSException
    {
        this(digestCalculatorProvider, new ByteArrayInputStream(sigBlock));
    }

    public CMSSignedDataParser(
        DigestCalculatorProvider digestCalculatorProvider,
        CMSTypedStream  signedContent,
        byte[]          sigBlock)
        throws CMSException
    {
        this(digestCalculatorProvider, signedContent, new ByteArrayInputStream(sigBlock));
    }

    /**
     * base constructor - with encapsulated content
     */
    public CMSSignedDataParser(
        DigestCalculatorProvider digestCalculatorProvider,
        InputStream sigData)
        throws CMSException
    {
        this(digestCalculatorProvider, null, sigData);
    }

    /**
     * base constructor
     *
     * @param digestCalculatorProvider for generating accumulating digests
     * @param signedContent the content that was signed.
     * @param sigData the signature object stream.
     */
    public CMSSignedDataParser(
        DigestCalculatorProvider digestCalculatorProvider,
        CMSTypedStream  signedContent,
        InputStream     sigData)
        throws CMSException
    {
        super(sigData);
        
        try
        {
            _signedContent = signedContent;
            _signedData = SignedDataParser.getInstance(_contentInfo.getContent(BERTags.SEQUENCE));
            digests = new HashMap();
            
            ASN1SetParser digAlgs = _signedData.getDigestAlgorithms();
            ASN1Encodable  o;
            
            while ((o = digAlgs.readObject()) != null)
            {
                AlgorithmIdentifier algId = AlgorithmIdentifier.getInstance(o);
                try
                {
                    DigestCalculator calculator = digestCalculatorProvider.get(algId);

                    if (calculator != null)
                    {
                        this.digests.put(algId.getAlgorithm(), calculator);
                    }
                }
                catch (OperatorCreationException e)
                {
                     //  ignore
                }
            }

            //
            // If the message is simply a certificate chain message getContent() may return null.
            //
            ContentInfoParser     cont = _signedData.getEncapContentInfo();
            ASN1OctetStringParser octs = (ASN1OctetStringParser)
                cont.getContent(BERTags.OCTET_STRING);

            if (octs != null)
            {
                CMSTypedStream ctStr = new CMSTypedStream(
                    cont.getContentType().getId(), octs.getOctetStream());

                if (_signedContent == null)
                {
                    _signedContent = ctStr; 
                }
                else
                {
                    //
                    // content passed in, need to read past empty encapsulated content info object if present
                    //
                    ctStr.drain();
                }
            }

            if (signedContent == null)
            {
                _signedContentType = cont.getContentType();
            }
            else
            {
                _signedContentType = _signedContent.getContentType();
            }
        }
        catch (IOException e)
        {
            throw new CMSException("io exception: " + e.getMessage(), e);
        }
    }

    /**
     * Return the version number for the SignedData object
     *
     * @return the version number
     */
    public int getVersion()
    {
        return _signedData.getVersion().getValue().intValue();
    }

    /**
     * return the collection of signers that are associated with the
     * signatures for the message.
     * @throws CMSException 
     */
    public SignerInformationStore getSignerInfos() 
        throws CMSException
    {
        if (_signerInfoStore == null)
        {
            populateCertCrlSets();
            
            List      signerInfos = new ArrayList();
            Map       hashes = new HashMap();
            
            Iterator  it = digests.keySet().iterator();
            while (it.hasNext())
            {
                Object digestKey = it.next();

                hashes.put(digestKey, ((DigestCalculator)digests.get(digestKey)).getDigest());
            }
            
            try
            {
                ASN1SetParser     s = _signedData.getSignerInfos();
                ASN1Encodable      o;

                while ((o = s.readObject()) != null)
                {
                    SignerInfo info = SignerInfo.getInstance(o.toASN1Primitive());

                    byte[] hash = (byte[])hashes.get(info.getDigestAlgorithm().getAlgorithm());

                    signerInfos.add(new SignerInformation(info, _signedContentType, null, hash));
                }
            }
            catch (IOException e)
            {
                throw new CMSException("io exception: " + e.getMessage(), e);
            }

            _signerInfoStore = new SignerInformationStore(signerInfos);
        }

        return _signerInfoStore;
    }

    /**
     * Return any X.509 certificate objects in this SignedData structure as a Store of X509CertificateHolder objects.
     *
     * @return a Store of X509CertificateHolder objects.
     */
    public Store getCertificates()
        throws CMSException
    {
        populateCertCrlSets();

        return HELPER.getCertificates(_certSet);
    }

    /**
     * Return any X.509 CRL objects in this SignedData structure as a Store of X509CRLHolder objects.
     *
     * @return a Store of X509CRLHolder objects.
     */
    public Store getCRLs()
        throws CMSException
    {
        populateCertCrlSets();

        return HELPER.getCRLs(_crlSet);
    }

    /**
     * Return any X.509 attribute certificate objects in this SignedData structure as a Store of X509AttributeCertificateHolder objects.
     *
     * @return a Store of X509AttributeCertificateHolder objects.
     */
    public Store getAttributeCertificates()
        throws CMSException
    {
        populateCertCrlSets();

        return HELPER.getAttributeCertificates(_certSet);
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
        throws CMSException
    {
        populateCertCrlSets();

        return HELPER.getOtherRevocationInfo(otherRevocationInfoFormat, _crlSet);
    }

    private void populateCertCrlSets()
        throws CMSException
    {
        if (_isCertCrlParsed)
        {
            return;
        }

        _isCertCrlParsed = true;

        try
        {
            // care! Streaming - these must be done in exactly this order.
            _certSet = getASN1Set(_signedData.getCertificates());
            _crlSet = getASN1Set(_signedData.getCrls());
        }
        catch (IOException e)
        {
            throw new CMSException("problem parsing cert/crl sets", e);
        }
    }

    /**
     * Return the a string representation of the OID associated with the
     * encapsulated content info structure carried in the signed data.
     * 
     * @return the OID for the content type.
     */
    public String getSignedContentTypeOID()
    {
        return _signedContentType.getId();
    }

    public CMSTypedStream getSignedContent()
    {
        if (_signedContent == null)
        {
            return null;
        }

        InputStream digStream = CMSUtils.attachDigestsToInputStream(
            digests.values(), _signedContent.getContentStream());

        return new CMSTypedStream(_signedContent.getContentType(), digStream);
    }

    /**
     * Replace the signerinformation store associated with the passed
     * in message contained in the stream original with the new one passed in.
     * You would probably only want to do this if you wanted to change the unsigned
     * attributes associated with a signer, or perhaps delete one.
     * <p>
     * The output stream is returned unclosed.
     * </p>
     * @param original the signed data stream to be used as a base.
     * @param signerInformationStore the new signer information store to use.
     * @param out the stream to write the new signed data object to.
     * @return out.
     */
    public static OutputStream replaceSigners(
        InputStream             original,
        SignerInformationStore  signerInformationStore,
        OutputStream            out)
        throws CMSException, IOException
    {
        ASN1StreamParser in = new ASN1StreamParser(original);
        ContentInfoParser contentInfo = new ContentInfoParser((ASN1SequenceParser)in.readObject());
        SignedDataParser signedData = SignedDataParser.getInstance(contentInfo.getContent(BERTags.SEQUENCE));

        BERSequenceGenerator sGen = new BERSequenceGenerator(out);

        sGen.addObject(CMSObjectIdentifiers.signedData);

        BERSequenceGenerator sigGen = new BERSequenceGenerator(sGen.getRawOutputStream(), 0, true);

        // version number
        sigGen.addObject(signedData.getVersion());

        // digests
        signedData.getDigestAlgorithms().toASN1Primitive();  // skip old ones

        ASN1EncodableVector digestAlgs = new ASN1EncodableVector();

        for (Iterator it = signerInformationStore.getSigners().iterator(); it.hasNext();)
        {
            SignerInformation signer = (SignerInformation)it.next();
            digestAlgs.add(CMSSignedHelper.INSTANCE.fixAlgID(signer.getDigestAlgorithmID()));
        }

        sigGen.getRawOutputStream().write(new DERSet(digestAlgs).getEncoded());

        // encap content info
        ContentInfoParser encapContentInfo = signedData.getEncapContentInfo();

        BERSequenceGenerator eiGen = new BERSequenceGenerator(sigGen.getRawOutputStream());

        eiGen.addObject(encapContentInfo.getContentType());

        pipeEncapsulatedOctetString(encapContentInfo, eiGen.getRawOutputStream());

        eiGen.close();


        writeSetToGeneratorTagged(sigGen, signedData.getCertificates(), 0);
        writeSetToGeneratorTagged(sigGen, signedData.getCrls(), 1);


        ASN1EncodableVector signerInfos = new ASN1EncodableVector();
        for (Iterator it = signerInformationStore.getSigners().iterator(); it.hasNext();)
        {
            SignerInformation        signer = (SignerInformation)it.next();

            signerInfos.add(signer.toASN1Structure());
        }

        sigGen.getRawOutputStream().write(new DERSet(signerInfos).getEncoded());

        sigGen.close();

        sGen.close();

        return out;
    }

    /**
     * Replace the certificate and CRL information associated with this
     * CMSSignedData object with the new one passed in.
     * <p>
     * The output stream is returned unclosed.
     * </p>
     * @param original the signed data stream to be used as a base.
     * @param certs new certificates to be used, if any.
     * @param crls new CRLs to be used, if any.
     * @param attrCerts new attribute certificates to be used, if any.
     * @param out the stream to write the new signed data object to.
     * @return out.
     * @exception CMSException if there is an error processing the CertStore
     */
    public static OutputStream replaceCertificatesAndCRLs(
        InputStream   original,
        Store         certs,
        Store         crls,
        Store         attrCerts,
        OutputStream  out)
        throws CMSException, IOException
    {
        ASN1StreamParser in = new ASN1StreamParser(original);
        ContentInfoParser contentInfo = new ContentInfoParser((ASN1SequenceParser)in.readObject());
        SignedDataParser signedData = SignedDataParser.getInstance(contentInfo.getContent(BERTags.SEQUENCE));

        BERSequenceGenerator sGen = new BERSequenceGenerator(out);

        sGen.addObject(CMSObjectIdentifiers.signedData);

        BERSequenceGenerator sigGen = new BERSequenceGenerator(sGen.getRawOutputStream(), 0, true);

        // version number
        sigGen.addObject(signedData.getVersion());

        // digests
        sigGen.getRawOutputStream().write(signedData.getDigestAlgorithms().toASN1Primitive().getEncoded());

        // encap content info
        ContentInfoParser encapContentInfo = signedData.getEncapContentInfo();

        BERSequenceGenerator eiGen = new BERSequenceGenerator(sigGen.getRawOutputStream());

        eiGen.addObject(encapContentInfo.getContentType());

        pipeEncapsulatedOctetString(encapContentInfo, eiGen.getRawOutputStream());

        eiGen.close();

        //
        // skip existing certs and CRLs
        //
        getASN1Set(signedData.getCertificates());
        getASN1Set(signedData.getCrls());

        //
        // replace the certs and crls in the SignedData object
        //
        if (certs != null || attrCerts != null)
        {
            List certificates = new ArrayList();

            if (certs != null)
            {
                certificates.addAll(CMSUtils.getCertificatesFromStore(certs));
            }
            if (attrCerts != null)
            {
                certificates.addAll(CMSUtils.getAttributeCertificatesFromStore(attrCerts));
            }

            ASN1Set asn1Certs = CMSUtils.createBerSetFromList(certificates);

            if (asn1Certs.size() > 0)
            {
                sigGen.getRawOutputStream().write(new DERTaggedObject(false, 0, asn1Certs).getEncoded());
            }
        }

        if (crls != null)
        {
            ASN1Set asn1Crls = CMSUtils.createBerSetFromList(CMSUtils.getCRLsFromStore(crls));

            if (asn1Crls.size() > 0)
            {
                sigGen.getRawOutputStream().write(new DERTaggedObject(false, 1, asn1Crls).getEncoded());
            }
        }

        sigGen.getRawOutputStream().write(signedData.getSignerInfos().toASN1Primitive().getEncoded());

        sigGen.close();

        sGen.close();

        return out;
    }

    private static void writeSetToGeneratorTagged(
        ASN1Generator asn1Gen,
        ASN1SetParser asn1SetParser,
        int           tagNo)
        throws IOException
    {
        ASN1Set asn1Set = getASN1Set(asn1SetParser);

        if (asn1Set != null)
        {
            if (asn1SetParser instanceof BERSetParser)
            {
                asn1Gen.getRawOutputStream().write(new BERTaggedObject(false, tagNo, asn1Set).getEncoded());
            }
            else
            {
                asn1Gen.getRawOutputStream().write(new DERTaggedObject(false, tagNo, asn1Set).getEncoded());
            }
        }
    }

    private static ASN1Set getASN1Set(
        ASN1SetParser asn1SetParser)
    {
        return asn1SetParser == null
            ?   null
            :   ASN1Set.getInstance(asn1SetParser.toASN1Primitive());
    }

    private static void pipeEncapsulatedOctetString(ContentInfoParser encapContentInfo,
        OutputStream rawOutputStream) throws IOException
    {
        ASN1OctetStringParser octs = (ASN1OctetStringParser)
            encapContentInfo.getContent(BERTags.OCTET_STRING);

        if (octs != null)
        {
            pipeOctetString(octs, rawOutputStream);
        }

//        BERTaggedObjectParser contentObject = (BERTaggedObjectParser)encapContentInfo.getContentObject();
//        if (contentObject != null)
//        {
//            // Handle IndefiniteLengthInputStream safely
//            InputStream input = ASN1StreamParser.getSafeRawInputStream(contentObject.getContentStream(true));
//
//            // TODO BerTaggedObjectGenerator?
//            BEROutputStream berOut = new BEROutputStream(rawOutputStream);
//            berOut.write(DERTags.CONSTRUCTED | DERTags.TAGGED | 0);
//            berOut.write(0x80);
//
//            pipeRawOctetString(input, rawOutputStream);
//
//            berOut.write(0x00);
//            berOut.write(0x00);
//
//            input.close();
//        }
    }

    private static void pipeOctetString(
        ASN1OctetStringParser octs,
        OutputStream          output)
        throws IOException
    {
        // TODO Allow specification of a specific fragment size?
        OutputStream outOctets = CMSUtils.createBEROctetOutputStream(
            output, 0, true, 0);
        Streams.pipeAll(octs.getOctetStream(), outOctets);
        outOctets.close();
    }

//    private static void pipeRawOctetString(
//        InputStream     rawInput,
//        OutputStream    rawOutput)
//        throws IOException
//    {
//        InputStream tee = new TeeInputStream(rawInput, rawOutput);
//        ASN1StreamParser sp = new ASN1StreamParser(tee);
//        ASN1OctetStringParser octs = (ASN1OctetStringParser)sp.readObject();
//        Streams.drain(octs.getOctetStream());
//    }
}
