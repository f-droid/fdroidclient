package org.spongycastle.cms;

import java.math.BigInteger;

import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.Selector;

/**
 * a basic index for an originator.
 */
class OriginatorId
    implements Selector
{
    private byte[] subjectKeyId;

    private X500Name issuer;
    private BigInteger serialNumber;

    /**
     * Construct a signer ID with the value of a public key's subjectKeyId.
     *
     * @param subjectKeyId a subjectKeyId
     */
    public OriginatorId(byte[] subjectKeyId)
    {
        setSubjectKeyID(subjectKeyId);
    }

    private void setSubjectKeyID(byte[] subjectKeyId)
    {
        this.subjectKeyId = subjectKeyId;
    }

    /**
     * Construct a signer ID based on the issuer and serial number of the signer's associated
     * certificate.
     *
     * @param issuer the issuer of the signer's associated certificate.
     * @param serialNumber the serial number of the signer's associated certificate.
     */
    public OriginatorId(X500Name issuer, BigInteger serialNumber)
    {
        setIssuerAndSerial(issuer, serialNumber);
    }

    private void setIssuerAndSerial(X500Name issuer, BigInteger serialNumber)
    {
        this.issuer = issuer;
        this.serialNumber = serialNumber;
    }

    /**
     * Construct a signer ID based on the issuer and serial number of the signer's associated
     * certificate.
     *
     * @param issuer the issuer of the signer's associated certificate.
     * @param serialNumber the serial number of the signer's associated certificate.
     * @param subjectKeyId the subject key identifier to use to match the signers associated certificate.
     */
    public OriginatorId(X500Name issuer, BigInteger serialNumber, byte[] subjectKeyId)
    {
        setIssuerAndSerial(issuer, serialNumber);
        setSubjectKeyID(subjectKeyId);
    }

    public X500Name getIssuer()
    {
        return issuer;
    }

    public Object clone()
    {
        return new OriginatorId(this.issuer, this.serialNumber, this.subjectKeyId);
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
        if (!(o instanceof OriginatorId))
        {
            return false;
        }

        OriginatorId id = (OriginatorId)o;

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
        return false;
    }
}
