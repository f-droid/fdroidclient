package org.spongycastle.cert.selector;

import java.math.BigInteger;

import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.cms.IssuerAndSerialNumber;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.Selector;

/**
 * a basic index for a X509CertificateHolder class
 */
public class X509CertificateHolderSelector
    implements Selector
{
    private byte[] subjectKeyId;

    private X500Name issuer;
    private BigInteger serialNumber;

    /**
     * Construct a selector with the value of a public key's subjectKeyId.
     *
     * @param subjectKeyId a subjectKeyId
     */
    public X509CertificateHolderSelector(byte[] subjectKeyId)
    {
        this(null, null, subjectKeyId);
    }

    /**
     * Construct a signer ID based on the issuer and serial number of the signer's associated
     * certificate.
     *
     * @param issuer the issuer of the signer's associated certificate.
     * @param serialNumber the serial number of the signer's associated certificate.
     */
    public X509CertificateHolderSelector(X500Name issuer, BigInteger serialNumber)
    {
        this(issuer, serialNumber, null);
    }

    /**
     * Construct a signer ID based on the issuer and serial number of the signer's associated
     * certificate.
     *
     * @param issuer the issuer of the signer's associated certificate.
     * @param serialNumber the serial number of the signer's associated certificate.
     * @param subjectKeyId the subject key identifier to use to match the signers associated certificate.
     */
    public X509CertificateHolderSelector(X500Name issuer, BigInteger serialNumber, byte[] subjectKeyId)
    {
        this.issuer = issuer;
        this.serialNumber = serialNumber;
        this.subjectKeyId = subjectKeyId;
    }

    public X500Name getIssuer()
    {
        return issuer;
    }

    public BigInteger getSerialNumber()
    {
        return serialNumber;
    }

    public byte[] getSubjectKeyIdentifier()
    {
        return Arrays.clone(subjectKeyId);
    }

    public int hashCode()
    {
        int code = Arrays.hashCode(subjectKeyId);

        if (this.serialNumber != null)
        {
            code ^= this.serialNumber.hashCode();
        }

        if (this.issuer != null)
        {
            code ^= this.issuer.hashCode();
        }

        return code;
    }

    public boolean equals(
        Object  o)
    {
        if (!(o instanceof X509CertificateHolderSelector))
        {
            return false;
        }

        X509CertificateHolderSelector id = (X509CertificateHolderSelector)o;

        return Arrays.areEqual(subjectKeyId, id.subjectKeyId)
            && equalsObj(this.serialNumber, id.serialNumber)
            && equalsObj(this.issuer, id.issuer);
    }

    private boolean equalsObj(Object a, Object b)
    {
        return (a != null) ? a.equals(b) : b == null;
    }

    public boolean match(Object obj)
    {
        if (obj instanceof X509CertificateHolder)
        {
            X509CertificateHolder certHldr = (X509CertificateHolder)obj;

            if (this.getSerialNumber() != null)
            {
                IssuerAndSerialNumber iAndS = new IssuerAndSerialNumber(certHldr.toASN1Structure());

                return iAndS.getName().equals(this.issuer)
                    && iAndS.getSerialNumber().getValue().equals(this.serialNumber);
            }
            else if (subjectKeyId != null)
            {
                Extension ext = certHldr.getExtension(Extension.subjectKeyIdentifier);

                if (ext == null)
                {
                    return Arrays.areEqual(subjectKeyId, MSOutlookKeyIdCalculator.calculateKeyId(certHldr.getSubjectPublicKeyInfo()));
                }

                byte[] subKeyID = ASN1OctetString.getInstance(ext.getParsedValue()).getOctets();

                return Arrays.areEqual(subjectKeyId, subKeyID);
            }
        }
        else if (obj instanceof byte[])
        {
            return Arrays.areEqual(subjectKeyId, (byte[])obj);
        }

        return false;
    }

    public Object clone()
    {
        return new X509CertificateHolderSelector(this.issuer, this.serialNumber, this.subjectKeyId);
    }
}
