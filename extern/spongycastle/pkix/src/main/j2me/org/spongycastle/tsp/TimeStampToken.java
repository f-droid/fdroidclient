package org.spongycastle.tsp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;

import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.cms.Attribute;
import org.spongycastle.asn1.cms.AttributeTable;
import org.spongycastle.asn1.cms.ContentInfo;
import org.spongycastle.asn1.cms.IssuerAndSerialNumber;
import org.spongycastle.asn1.ess.ESSCertID;
import org.spongycastle.asn1.ess.ESSCertIDv2;
import org.spongycastle.asn1.ess.SigningCertificate;
import org.spongycastle.asn1.ess.SigningCertificateV2;
import org.spongycastle.asn1.nist.NISTObjectIdentifiers;
import org.spongycastle.asn1.oiw.OIWObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.tsp.TSTInfo;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.GeneralName;
import org.spongycastle.asn1.x509.IssuerSerial;
import org.spongycastle.asn1.x509.X509Name;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cms.CMSException;
import org.spongycastle.cms.CMSProcessable;
import org.spongycastle.cms.CMSSignedData;
import org.spongycastle.cms.SignerId;
import org.spongycastle.cms.SignerInformation;
import org.spongycastle.cms.SignerInformationVerifier;
import org.spongycastle.operator.DigestCalculator;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.Store;

public class TimeStampToken
{
    CMSSignedData tsToken;

    SignerInformation tsaSignerInfo;

    Date genTime;

    TimeStampTokenInfo tstInfo;
    
    CertID   certID;

    public TimeStampToken(ContentInfo contentInfo)
        throws TSPException, IOException
    {
        this(getSignedData(contentInfo));
    }

    private static CMSSignedData getSignedData(ContentInfo contentInfo)
        throws TSPException
    {
        try
        {
            return new CMSSignedData(contentInfo);
        }
        catch (CMSException e)
        {
            throw new TSPException("TSP parsing error: " + e.getMessage(), e.getCause());
        }
    }

    public TimeStampToken(CMSSignedData signedData)
        throws TSPException, IOException
    {
        this.tsToken = signedData;

        if (!this.tsToken.getSignedContentTypeOID().equals(PKCSObjectIdentifiers.id_ct_TSTInfo.getId()))
        {
            throw new TSPValidationException("ContentInfo object not for a time stamp.");
        }
        
        Collection signers = tsToken.getSignerInfos().getSigners();

        if (signers.size() != 1)
        {
            throw new IllegalArgumentException("Time-stamp token signed by "
                    + signers.size()
                    + " signers, but it must contain just the TSA signature.");
        }

        tsaSignerInfo = (SignerInformation)signers.iterator().next();

        try
        {
            CMSProcessable content = tsToken.getSignedContent();
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();

            content.write(bOut);

            ASN1InputStream aIn = new ASN1InputStream(new ByteArrayInputStream(bOut.toByteArray()));

            this.tstInfo = new TimeStampTokenInfo(TSTInfo.getInstance(aIn.readObject()));
            
            Attribute   attr = tsaSignerInfo.getSignedAttributes().get(PKCSObjectIdentifiers.id_aa_signingCertificate);

            if (attr != null)
            {
                SigningCertificate    signCert = SigningCertificate.getInstance(attr.getAttrValues().getObjectAt(0));

                this.certID = new CertID(ESSCertID.getInstance(signCert.getCerts()[0]));
            }
            else
            {
                attr = tsaSignerInfo.getSignedAttributes().get(PKCSObjectIdentifiers.id_aa_signingCertificateV2);

                if (attr == null)
                {
                    throw new TSPValidationException("no signing certificate attribute found, time stamp invalid.");
                }

                SigningCertificateV2 signCertV2 = SigningCertificateV2.getInstance(attr.getAttrValues().getObjectAt(0));

                this.certID = new CertID(ESSCertIDv2.getInstance(signCertV2.getCerts()[0]));
            }
        }
        catch (CMSException e)
        {
            throw new TSPException(e.getMessage(), e.getUnderlyingException());
        }
    }

    public TimeStampTokenInfo getTimeStampInfo()
    {
        return tstInfo;
    }

    public SignerId getSID()
    {
        return tsaSignerInfo.getSID();
    }
    
    public AttributeTable getSignedAttributes()
    {
        return tsaSignerInfo.getSignedAttributes();
    }

    public AttributeTable getUnsignedAttributes()
    {
        return tsaSignerInfo.getUnsignedAttributes();
    }

    public Store getCertificates()
    {
        return tsToken.getCertificates();
    }

    public Store getCRLs()
    {
        return tsToken.getCRLs();
    }

    public Store getAttributeCertificates()
    {
        return tsToken.getAttributeCertificates();
    }

    /**
     * Validate the time stamp token.
     * <p>
     * To be valid the token must be signed by the passed in certificate and
     * the certificate must be the one referred to by the SigningCertificate
     * attribute included in the hashed attributes of the token. The
     * certificate must also have the ExtendedKeyUsageExtension with only
     * KeyPurposeId.id_kp_timeStamping and have been valid at the time the
     * timestamp was created.
     * </p>
     * <p>
     * A successful call to validate means all the above are true.
     * </p>
     *
     * @param sigVerifier the content verifier create the objects required to verify the CMS object in the timestamp.
     * @throws TSPException if an exception occurs in processing the token.
     * @throws TSPValidationException if the certificate or signature fail to be valid.
     * @throws IllegalArgumentException if the sigVerifierProvider has no associated certificate.
     */
    public void validate(
        SignerInformationVerifier sigVerifier)
        throws TSPException, TSPValidationException
    {
        if (!sigVerifier.hasAssociatedCertificate())
        {
            throw new IllegalArgumentException("verifier provider needs an associated certificate");
        }

        try
        {
            X509CertificateHolder certHolder = sigVerifier.getAssociatedCertificate();
            DigestCalculator calc = sigVerifier.getDigestCalculator(certID.getHashAlgorithm());

            OutputStream cOut = calc.getOutputStream();

            cOut.write(certHolder.getEncoded());
            cOut.close();

            if (!Arrays.constantTimeAreEqual(certID.getCertHash(), calc.getDigest()))
            {
                throw new TSPValidationException("certificate hash does not match certID hash.");
            }

            if (certID.getIssuerSerial() != null)
            {
                IssuerAndSerialNumber issuerSerial = new IssuerAndSerialNumber(certHolder.toASN1Structure());

                if (!certID.getIssuerSerial().getSerial().equals(issuerSerial.getSerialNumber()))
                {
                    throw new TSPValidationException("certificate serial number does not match certID for signature.");
                }

                GeneralName[]   names = certID.getIssuerSerial().getIssuer().getNames();
                boolean         found = false;

                for (int i = 0; i != names.length; i++)
                {
                    if (names[i].getTagNo() == 4 && X500Name.getInstance(names[i].getName()).equals(X500Name.getInstance(issuerSerial.getName())))
                    {
                        found = true;
                        break;
                    }
                }

                if (!found)
                {
                    throw new TSPValidationException("certificate name does not match certID for signature. ");
                }
            }

            TSPUtil.validateCertificate(certHolder);

            if (!certHolder.isValidOn(tstInfo.getGenTime()))
            {
                throw new TSPValidationException("certificate not valid when time stamp created.");
            }

            if (!tsaSignerInfo.verify(sigVerifier))
            {
                throw new TSPValidationException("signature not created by certificate.");
            }
        }
        catch (CMSException e)
        {
            if (e.getUnderlyingException() != null)
            {
                throw new TSPException(e.getMessage(), e.getUnderlyingException());
            }
            else
            {
                throw new TSPException("CMS exception: " + e, e);
            }
        }
        catch (IOException e)
        {
            throw new TSPException("problem processing certificate: " + e, e);
        }
        catch (OperatorCreationException e)
        {
            throw new TSPException("unable to create digest: " + e.getMessage(), e);
        }
    }

    /**
     * Return true if the signature on time stamp token is valid.
     * <p>
     * Note: this is a much weaker proof of correctness than calling validate().
     * </p>
     *
     * @param sigVerifier the content verifier create the objects required to verify the CMS object in the timestamp.
     * @return true if the signature matches, false otherwise.
     * @throws TSPException if the signature cannot be processed or the provider cannot match the algorithm.
     */
    public boolean isSignatureValid(
        SignerInformationVerifier sigVerifier)
        throws TSPException
    {
        try
        {
            return tsaSignerInfo.verify(sigVerifier);
        }
        catch (CMSException e)
        {
            if (e.getUnderlyingException() != null)
            {
                throw new TSPException(e.getMessage(), e.getUnderlyingException());
            }
            else
            {
                throw new TSPException("CMS exception: " + e, e);
            }
        }
    }

    /**
     * Return the underlying CMSSignedData object.
     * 
     * @return the underlying CMS structure.
     */
    public CMSSignedData toCMSSignedData()
    {
        return tsToken;
    }
    
    /**
     * Return a ASN.1 encoded byte stream representing the encoded object.
     * 
     * @throws IOException if encoding fails.
     */
    public byte[] getEncoded() 
        throws IOException
    {
        return tsToken.getEncoded();
    }

    // perhaps this should be done using an interface on the ASN.1 classes...
    private class CertID
    {
        private ESSCertID certID;
        private ESSCertIDv2 certIDv2;

        CertID(ESSCertID certID)
        {
            this.certID = certID;
            this.certIDv2 = null;
        }

        CertID(ESSCertIDv2 certID)
        {
            this.certIDv2 = certID;
            this.certID = null;
        }

        public String getHashAlgorithmName()
        {
            if (certID != null)
            {
                return "SHA-1";
            }
            else
            {
                if (NISTObjectIdentifiers.id_sha256.equals(certIDv2.getHashAlgorithm().getAlgorithm()))
                {
                    return "SHA-256";
                }
                return certIDv2.getHashAlgorithm().getAlgorithm().getId();
            }
        }

        public AlgorithmIdentifier getHashAlgorithm()
        {
            if (certID != null)
            {
                return new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1);
            }
            else
            {
                return certIDv2.getHashAlgorithm();
            }
        }

        public byte[] getCertHash()
        {
            if (certID != null)
            {
                return certID.getCertHash();
            }
            else
            {
                return certIDv2.getCertHash();
            }
        }

        public IssuerSerial getIssuerSerial()
        {
            if (certID != null)
            {
                return certID.getIssuerSerial();
            }
            else
            {
                return certIDv2.getIssuerSerial();
            }
        }
    }
}
