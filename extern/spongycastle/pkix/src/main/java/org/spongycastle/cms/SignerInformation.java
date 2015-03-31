package org.spongycastle.cms;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1Encoding;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.ASN1Set;
import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.DERSet;
import org.spongycastle.asn1.cms.Attribute;
import org.spongycastle.asn1.cms.AttributeTable;
import org.spongycastle.asn1.cms.CMSAttributes;
import org.spongycastle.asn1.cms.IssuerAndSerialNumber;
import org.spongycastle.asn1.cms.SignerIdentifier;
import org.spongycastle.asn1.cms.SignerInfo;
import org.spongycastle.asn1.cms.Time;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.DigestInfo;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.operator.ContentVerifier;
import org.spongycastle.operator.DigestCalculator;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.RawContentVerifier;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.io.TeeOutputStream;

/**
 * an expanded SignerInfo block from a CMS Signed message
 */
public class SignerInformation
{
    private SignerId                sid;
    private SignerInfo              info;
    private AlgorithmIdentifier     digestAlgorithm;
    private AlgorithmIdentifier     encryptionAlgorithm;
    private final ASN1Set           signedAttributeSet;
    private final ASN1Set           unsignedAttributeSet;
    private CMSProcessable          content;
    private byte[]                  signature;
    private ASN1ObjectIdentifier    contentType;
    private byte[]                  resultDigest;

    // Derived
    private AttributeTable          signedAttributeValues;
    private AttributeTable          unsignedAttributeValues;
    private boolean                 isCounterSignature;

    SignerInformation(
        SignerInfo          info,
        ASN1ObjectIdentifier contentType,
        CMSProcessable      content,
        byte[]              resultDigest)
    {
        this.info = info;
        this.contentType = contentType;
        this.isCounterSignature = contentType == null;

        SignerIdentifier   s = info.getSID();

        if (s.isTagged())
        {
            ASN1OctetString octs = ASN1OctetString.getInstance(s.getId());

            sid = new SignerId(octs.getOctets());
        }
        else
        {
            IssuerAndSerialNumber   iAnds = IssuerAndSerialNumber.getInstance(s.getId());

            sid = new SignerId(iAnds.getName(), iAnds.getSerialNumber().getValue());
        }

        this.digestAlgorithm = info.getDigestAlgorithm();
        this.signedAttributeSet = info.getAuthenticatedAttributes();
        this.unsignedAttributeSet = info.getUnauthenticatedAttributes();
        this.encryptionAlgorithm = info.getDigestEncryptionAlgorithm();
        this.signature = info.getEncryptedDigest().getOctets();

        this.content = content;
        this.resultDigest = resultDigest;
    }

    public boolean isCounterSignature()
    {
        return isCounterSignature;
    }

    public ASN1ObjectIdentifier getContentType()
    {
        return this.contentType;
    }

    private byte[] encodeObj(
        ASN1Encodable    obj)
        throws IOException
    {
        if (obj != null)
        {
            return obj.toASN1Primitive().getEncoded();
        }

        return null;
    }

    public SignerId getSID()
    {
        return sid;
    }

    /**
     * return the version number for this objects underlying SignerInfo structure.
     */
    public int getVersion()
    {
        return info.getVersion().getValue().intValue();
    }

    public AlgorithmIdentifier getDigestAlgorithmID()
    {
        return digestAlgorithm;
    }

    /**
     * return the object identifier for the signature.
     */
    public String getDigestAlgOID()
    {
        return digestAlgorithm.getAlgorithm().getId();
    }

    /**
     * return the signature parameters, or null if there aren't any.
     */
    public byte[] getDigestAlgParams()
    {
        try
        {
            return encodeObj(digestAlgorithm.getParameters());
        }
        catch (Exception e)
        {
            throw new RuntimeException("exception getting digest parameters " + e);
        }
    }

    /**
     * return the content digest that was calculated during verification.
     */
    public byte[] getContentDigest()
    {
        if (resultDigest == null)
        {
            throw new IllegalStateException("method can only be called after verify.");
        }
        
        return Arrays.clone(resultDigest);
    }
    
    /**
     * return the object identifier for the signature.
     */
    public String getEncryptionAlgOID()
    {
        return encryptionAlgorithm.getAlgorithm().getId();
    }

    /**
     * return the signature/encryption algorithm parameters, or null if
     * there aren't any.
     */
    public byte[] getEncryptionAlgParams()
    {
        try
        {
            return encodeObj(encryptionAlgorithm.getParameters());
        }
        catch (Exception e)
        {
            throw new RuntimeException("exception getting encryption parameters " + e);
        }
    }  

    /**
     * return a table of the signed attributes - indexed by
     * the OID of the attribute.
     */
    public AttributeTable getSignedAttributes()
    {
        if (signedAttributeSet != null && signedAttributeValues == null)
        {
            signedAttributeValues = new AttributeTable(signedAttributeSet);
        }

        return signedAttributeValues;
    }

    /**
     * return a table of the unsigned attributes indexed by
     * the OID of the attribute.
     */
    public AttributeTable getUnsignedAttributes()
    {
        if (unsignedAttributeSet != null && unsignedAttributeValues == null)
        {
            unsignedAttributeValues = new AttributeTable(unsignedAttributeSet);
        }

        return unsignedAttributeValues;
    }

    /**
     * return the encoded signature
     */
    public byte[] getSignature()
    {
        return Arrays.clone(signature);
    }

    /**
     * Return a SignerInformationStore containing the counter signatures attached to this
     * signer. If no counter signatures are present an empty store is returned.
     */
    public SignerInformationStore getCounterSignatures()
    {
        // TODO There are several checks implied by the RFC3852 comments that are missing

        /*
        The countersignature attribute MUST be an unsigned attribute; it MUST
        NOT be a signed attribute, an authenticated attribute, an
        unauthenticated attribute, or an unprotected attribute.
        */        
        AttributeTable unsignedAttributeTable = getUnsignedAttributes();
        if (unsignedAttributeTable == null)
        {
            return new SignerInformationStore(new ArrayList(0));
        }

        List counterSignatures = new ArrayList();

        /*
        The UnsignedAttributes syntax is defined as a SET OF Attributes.  The
        UnsignedAttributes in a signerInfo may include multiple instances of
        the countersignature attribute.
        */
        ASN1EncodableVector allCSAttrs = unsignedAttributeTable.getAll(CMSAttributes.counterSignature);

        for (int i = 0; i < allCSAttrs.size(); ++i)
        {
            Attribute counterSignatureAttribute = (Attribute)allCSAttrs.get(i);            

            /*
            A countersignature attribute can have multiple attribute values.  The
            syntax is defined as a SET OF AttributeValue, and there MUST be one
            or more instances of AttributeValue present.
            */
            ASN1Set values = counterSignatureAttribute.getAttrValues();
            if (values.size() < 1)
            {
                // TODO Throw an appropriate exception?
            }

            for (Enumeration en = values.getObjects(); en.hasMoreElements();)
            {
                /*
                Countersignature values have the same meaning as SignerInfo values
                for ordinary signatures, except that:

                   1. The signedAttributes field MUST NOT contain a content-type
                      attribute; there is no content type for countersignatures.

                   2. The signedAttributes field MUST contain a message-digest
                      attribute if it contains any other attributes.

                   3. The input to the message-digesting process is the contents
                      octets of the DER encoding of the signatureValue field of the
                      SignerInfo value with which the attribute is associated.
                */
                SignerInfo si = SignerInfo.getInstance(en.nextElement());

                counterSignatures.add(new SignerInformation(si, null, new CMSProcessableByteArray(getSignature()), null));
            }
        }

        return new SignerInformationStore(counterSignatures);
    }
    
    /**
     * return the DER encoding of the signed attributes.
     * @throws IOException if an encoding error occurs.
     */
    public byte[] getEncodedSignedAttributes()
        throws IOException
    {
        if (signedAttributeSet != null)
        {
            return signedAttributeSet.getEncoded();
        }

        return null;
    }

    private boolean doVerify(
        SignerInformationVerifier verifier)
        throws CMSException
    {
        String          encName = CMSSignedHelper.INSTANCE.getEncryptionAlgName(this.getEncryptionAlgOID());
        ContentVerifier contentVerifier;

        try
        {
            contentVerifier = verifier.getContentVerifier(encryptionAlgorithm, info.getDigestAlgorithm());
        }
        catch (OperatorCreationException e)
        {
            throw new CMSException("can't create content verifier: " + e.getMessage(), e);
        }

        try
        {
            OutputStream sigOut = contentVerifier.getOutputStream();

            if (resultDigest == null)
            {
                DigestCalculator calc = verifier.getDigestCalculator(this.getDigestAlgorithmID());
                if (content != null)
                {
                    OutputStream      digOut = calc.getOutputStream();

                    if (signedAttributeSet == null)
                    {
                        if (contentVerifier instanceof RawContentVerifier)
                        {
                            content.write(digOut);
                        }
                        else
                        {
                            OutputStream cOut = new TeeOutputStream(digOut, sigOut);

                            content.write(cOut);

                            cOut.close();
                        }
                    }
                    else
                    {
                        content.write(digOut);
                        sigOut.write(this.getEncodedSignedAttributes());
                    }

                    digOut.close();
                }
                else if (signedAttributeSet != null)
                {
                    sigOut.write(this.getEncodedSignedAttributes());
                }
                else
                {
                    // TODO Get rid of this exception and just treat content==null as empty not missing?
                    throw new CMSException("data not encapsulated in signature - use detached constructor.");
                }

                resultDigest = calc.getDigest();
            }
            else
            {
                if (signedAttributeSet == null)
                {
                    if (content != null)
                    {
                        content.write(sigOut);
                    }
                }
                else
                {
                    sigOut.write(this.getEncodedSignedAttributes());
                }
            }

            sigOut.close();
        }
        catch (IOException e)
        {
            throw new CMSException("can't process mime object to create signature.", e);
        }
        catch (OperatorCreationException e)
        {
            throw new CMSException("can't create digest calculator: " + e.getMessage(), e);
        }

        // RFC 3852 11.1 Check the content-type attribute is correct
        {
            ASN1Primitive validContentType = getSingleValuedSignedAttribute(
                CMSAttributes.contentType, "content-type");
            if (validContentType == null)
            {
                if (!isCounterSignature && signedAttributeSet != null)
                {
                    throw new CMSException("The content-type attribute type MUST be present whenever signed attributes are present in signed-data");
                }
            }
            else
            {
                if (isCounterSignature)
                {
                    throw new CMSException("[For counter signatures,] the signedAttributes field MUST NOT contain a content-type attribute");
                }

                if (!(validContentType instanceof ASN1ObjectIdentifier))
                {
                    throw new CMSException("content-type attribute value not of ASN.1 type 'OBJECT IDENTIFIER'");
                }

                ASN1ObjectIdentifier signedContentType = (ASN1ObjectIdentifier)validContentType;

                if (!signedContentType.equals(contentType))
                {
                    throw new CMSException("content-type attribute value does not match eContentType");
                }
            }
        }

        // RFC 3852 11.2 Check the message-digest attribute is correct
        {
            ASN1Primitive validMessageDigest = getSingleValuedSignedAttribute(
                CMSAttributes.messageDigest, "message-digest");
            if (validMessageDigest == null)
            {
                if (signedAttributeSet != null)
                {
                    throw new CMSException("the message-digest signed attribute type MUST be present when there are any signed attributes present");
                }
            }
            else
            {
                if (!(validMessageDigest instanceof ASN1OctetString))
                {
                    throw new CMSException("message-digest attribute value not of ASN.1 type 'OCTET STRING'");
                }

                ASN1OctetString signedMessageDigest = (ASN1OctetString)validMessageDigest;

                if (!Arrays.constantTimeAreEqual(resultDigest, signedMessageDigest.getOctets()))
                {
                    throw new CMSSignerDigestMismatchException("message-digest attribute value does not match calculated value");
                }
            }
        }

        // RFC 3852 11.4 Validate countersignature attribute(s)
        {
            AttributeTable signedAttrTable = this.getSignedAttributes();
            if (signedAttrTable != null
                && signedAttrTable.getAll(CMSAttributes.counterSignature).size() > 0)
            {
                throw new CMSException("A countersignature attribute MUST NOT be a signed attribute");
            }

            AttributeTable unsignedAttrTable = this.getUnsignedAttributes();
            if (unsignedAttrTable != null)
            {
                ASN1EncodableVector csAttrs = unsignedAttrTable.getAll(CMSAttributes.counterSignature);
                for (int i = 0; i < csAttrs.size(); ++i)
                {
                    Attribute csAttr = (Attribute)csAttrs.get(i);
                    if (csAttr.getAttrValues().size() < 1)
                    {
                        throw new CMSException("A countersignature attribute MUST contain at least one AttributeValue");
                    }

                    // Note: We don't recursively validate the countersignature value
                }
            }
        }

        try
        {
            if (signedAttributeSet == null && resultDigest != null)
            {
                if (contentVerifier instanceof RawContentVerifier)
                {
                    RawContentVerifier rawVerifier = (RawContentVerifier)contentVerifier;

                    if (encName.equals("RSA"))
                    {
                        DigestInfo digInfo = new DigestInfo(new AlgorithmIdentifier(digestAlgorithm.getAlgorithm(), DERNull.INSTANCE), resultDigest);

                        return rawVerifier.verify(digInfo.getEncoded(ASN1Encoding.DER), this.getSignature());
                    }

                    return rawVerifier.verify(resultDigest, this.getSignature());
                }
            }

            return contentVerifier.verify(this.getSignature());
        }
        catch (IOException e)
        {
            throw new CMSException("can't process mime object to create signature.", e);
        }
    }

    /**
     * Verify that the given verifier can successfully verify the signature on
     * this SignerInformation object.
     *
     * @param verifier a suitably configured SignerInformationVerifier.
     * @return true if the signer information is verified, false otherwise.
     * @throws org.spongycastle.cms.CMSVerifierCertificateNotValidException if the provider has an associated certificate and the certificate is not valid at the time given as the SignerInfo's signing time.
     * @throws org.spongycastle.cms.CMSException if the verifier is unable to create a ContentVerifiers or DigestCalculators.
     */
    public boolean verify(SignerInformationVerifier verifier)
        throws CMSException
    {
        Time signingTime = getSigningTime();   // has to be validated if present.

        if (verifier.hasAssociatedCertificate())
        {
            if (signingTime != null)
            {
                X509CertificateHolder dcv = verifier.getAssociatedCertificate();

                if (!dcv.isValidOn(signingTime.getDate()))
                {
                    throw new CMSVerifierCertificateNotValidException("verifier not valid at signingTime");
                }
            }
        }

        return doVerify(verifier);
    }

    /**
     * Return the underlying ASN.1 object defining this SignerInformation object.
     *
     * @return a SignerInfo.
     */
    public SignerInfo toASN1Structure()
    {
        return info;
    }

    private ASN1Primitive getSingleValuedSignedAttribute(
        ASN1ObjectIdentifier attrOID, String printableName)
        throws CMSException
    {
        AttributeTable unsignedAttrTable = this.getUnsignedAttributes();
        if (unsignedAttrTable != null
            && unsignedAttrTable.getAll(attrOID).size() > 0)
        {
            throw new CMSException("The " + printableName
                + " attribute MUST NOT be an unsigned attribute");
        }

        AttributeTable signedAttrTable = this.getSignedAttributes();
        if (signedAttrTable == null)
        {
            return null;
        }

        ASN1EncodableVector v = signedAttrTable.getAll(attrOID);
        switch (v.size())
        {
            case 0:
                return null;
            case 1:
            {
                Attribute t = (Attribute)v.get(0);
                ASN1Set attrValues = t.getAttrValues();
                if (attrValues.size() != 1)
                {
                    throw new CMSException("A " + printableName
                        + " attribute MUST have a single attribute value");
                }

                return attrValues.getObjectAt(0).toASN1Primitive();
            }
            default:
                throw new CMSException("The SignedAttributes in a signerInfo MUST NOT include multiple instances of the "
                    + printableName + " attribute");
        }
    }

    private Time getSigningTime() throws CMSException
    {
        ASN1Primitive validSigningTime = getSingleValuedSignedAttribute(
            CMSAttributes.signingTime, "signing-time");

        if (validSigningTime == null)
        {
            return null;
        }

        try
        {
            return Time.getInstance(validSigningTime);
        }
        catch (IllegalArgumentException e)
        {
            throw new CMSException("signing-time attribute value not a valid 'Time' structure");
        }
    }

    /**
     * Return a signer information object with the passed in unsigned
     * attributes replacing the ones that are current associated with
     * the object passed in.
     * 
     * @param signerInformation the signerInfo to be used as the basis.
     * @param unsignedAttributes the unsigned attributes to add.
     * @return a copy of the original SignerInformationObject with the changed attributes.
     */
    public static SignerInformation replaceUnsignedAttributes(
        SignerInformation   signerInformation,
        AttributeTable      unsignedAttributes)
    {
        SignerInfo  sInfo = signerInformation.info;
        ASN1Set     unsignedAttr = null;
        
        if (unsignedAttributes != null)
        {
            unsignedAttr = new DERSet(unsignedAttributes.toASN1EncodableVector());
        }
        
        return new SignerInformation(
                new SignerInfo(sInfo.getSID(), sInfo.getDigestAlgorithm(),
                    sInfo.getAuthenticatedAttributes(), sInfo.getDigestEncryptionAlgorithm(), sInfo.getEncryptedDigest(), unsignedAttr),
                    signerInformation.contentType, signerInformation.content, null);
    }

    /**
     * Return a signer information object with passed in SignerInformationStore representing counter
     * signatures attached as an unsigned attribute.
     *
     * @param signerInformation the signerInfo to be used as the basis.
     * @param counterSigners signer info objects carrying counter signature.
     * @return a copy of the original SignerInformationObject with the changed attributes.
     */
    public static SignerInformation addCounterSigners(
        SignerInformation        signerInformation,
        SignerInformationStore   counterSigners)
    {
        // TODO Perform checks from RFC 3852 11.4

        SignerInfo          sInfo = signerInformation.info;
        AttributeTable      unsignedAttr = signerInformation.getUnsignedAttributes();
        ASN1EncodableVector v;

        if (unsignedAttr != null)
        {
            v = unsignedAttr.toASN1EncodableVector();
        }
        else
        {
            v = new ASN1EncodableVector();
        }

        ASN1EncodableVector sigs = new ASN1EncodableVector();

        for (Iterator it = counterSigners.getSigners().iterator(); it.hasNext();)
        {
            sigs.add(((SignerInformation)it.next()).toASN1Structure());
        }

        v.add(new Attribute(CMSAttributes.counterSignature, new DERSet(sigs)));

        return new SignerInformation(
                new SignerInfo(sInfo.getSID(), sInfo.getDigestAlgorithm(),
                    sInfo.getAuthenticatedAttributes(), sInfo.getDigestEncryptionAlgorithm(), sInfo.getEncryptedDigest(), new DERSet(v)),
                    signerInformation.contentType, signerInformation.content, null);
    }
}
