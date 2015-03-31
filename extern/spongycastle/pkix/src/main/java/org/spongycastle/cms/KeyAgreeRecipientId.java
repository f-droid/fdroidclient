package org.spongycastle.cms;

import java.math.BigInteger;

import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.cert.selector.X509CertificateHolderSelector;

public class KeyAgreeRecipientId
    extends RecipientId
{
    private X509CertificateHolderSelector baseSelector;

    private KeyAgreeRecipientId(X509CertificateHolderSelector baseSelector)
    {
        super(keyAgree);

        this.baseSelector = baseSelector;
    }

    /**
     * Construct a key agree recipient ID with the value of a public key's subjectKeyId.
     *
     * @param subjectKeyId a subjectKeyId
     */
    public KeyAgreeRecipientId(byte[] subjectKeyId)
    {
        this(null, null, subjectKeyId);
    }

    /**
     * Construct a key agree recipient ID based on the issuer and serial number of the recipient's associated
     * certificate.
     *
     * @param issuer the issuer of the recipient's associated certificate.
     * @param serialNumber the serial number of the recipient's associated certificate.
     */
    public KeyAgreeRecipientId(X500Name issuer, BigInteger serialNumber)
    {
        this(issuer, serialNumber, null);
    }

    public KeyAgreeRecipientId(X500Name issuer, BigInteger serialNumber, byte[] subjectKeyId)
    {
        this(new X509CertificateHolderSelector(issuer, serialNumber, subjectKeyId));
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
        if (!(o instanceof KeyAgreeRecipientId))
        {
            return false;
        }

        KeyAgreeRecipientId id = (KeyAgreeRecipientId)o;

        return this.baseSelector.equals(id.baseSelector);
    }

    public Object clone()
    {
        return new KeyAgreeRecipientId(baseSelector);
    }

    public boolean match(Object obj)
    {
        if (obj instanceof KeyAgreeRecipientInformation)
        {
            return ((KeyAgreeRecipientInformation)obj).getRID().equals(this);
        }

        return baseSelector.match(obj);
    }
}
