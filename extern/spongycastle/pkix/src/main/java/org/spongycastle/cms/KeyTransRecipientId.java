package org.spongycastle.cms;

import java.math.BigInteger;

import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.cert.selector.X509CertificateHolderSelector;

public class KeyTransRecipientId
    extends RecipientId
{
    private X509CertificateHolderSelector baseSelector;

    private KeyTransRecipientId(X509CertificateHolderSelector baseSelector)
    {
        super(keyTrans);

        this.baseSelector = baseSelector;
    }

    /**
     * Construct a key trans recipient ID with the value of a public key's subjectKeyId.
     *
     * @param subjectKeyId a subjectKeyId
     */
    public KeyTransRecipientId(byte[] subjectKeyId)
    {
        this(null, null, subjectKeyId);
    }

    /**
     * Construct a key trans recipient ID based on the issuer and serial number of the recipient's associated
     * certificate.
     *
     * @param issuer the issuer of the recipient's associated certificate.
     * @param serialNumber the serial number of the recipient's associated certificate.
     */
    public KeyTransRecipientId(X500Name issuer, BigInteger serialNumber)
    {
        this(issuer, serialNumber, null);
    }

    /**
     * Construct a key trans recipient ID based on the issuer and serial number of the recipient's associated
     * certificate.
     *
     * @param issuer the issuer of the recipient's associated certificate.
     * @param serialNumber the serial number of the recipient's associated certificate.
     * @param subjectKeyId the subject key identifier to use to match the recipients associated certificate.
     */
    public KeyTransRecipientId(X500Name issuer, BigInteger serialNumber, byte[] subjectKeyId)
    {
        this(new X509CertificateHolderSelector(issuer, serialNumber, subjectKeyId));
    }

    public X500Name getIssuer()
    {
        return baseSelector.getIssuer();
    }

    public BigInteger getSerialNumber()
    {
        return baseSelector.getSerialNumber();
    }

    public byte[] getSubjectKeyIdentifier()
    {
        return baseSelector.getSubjectKeyIdentifier();
    }

    public int hashCode()
    {
        return baseSelector.hashCode();
    }

    public boolean equals(
        Object  o)
    {
        if (!(o instanceof KeyTransRecipientId))
        {
            return false;
        }

        KeyTransRecipientId id = (KeyTransRecipientId)o;

        return this.baseSelector.equals(id.baseSelector);
    }

    public Object clone()
    {
        return new KeyTransRecipientId(this.baseSelector);
    }

    public boolean match(Object obj)
    {
        if (obj instanceof KeyTransRecipientInformation)
        {
            return ((KeyTransRecipientInformation)obj).getRID().equals(this);
        }

        return baseSelector.match(obj);
    }
}
