package org.spongycastle.cms.jcajce;

import org.spongycastle.jce.cert.X509CertSelector;

import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.cms.KeyTransRecipientId;
import org.spongycastle.cms.SignerId;

public class JcaSelectorConverter
{
    public JcaSelectorConverter()
    {

    }

    public SignerId getSignerId(X509CertSelector certSelector)
    {
try
{
        if (certSelector.getSubjectKeyIdentifier() != null)
        {
            return new SignerId(X500Name.getInstance(certSelector.getIssuerAsBytes()), certSelector.getSerialNumber(), ASN1OctetString.getInstance(certSelector.getSubjectKeyIdentifier()).getOctets());
        }
        else
        {
            return new SignerId(X500Name.getInstance(certSelector.getIssuerAsBytes()), certSelector.getSerialNumber());
        }
}
catch (Exception e)
{
    throw new IllegalArgumentException("conversion failed: " + e.toString());
}
    }

    public KeyTransRecipientId getKeyTransRecipientId(X509CertSelector certSelector)
    {
try
{
        if (certSelector.getSubjectKeyIdentifier() != null)
        {
            return new KeyTransRecipientId(X500Name.getInstance(certSelector.getIssuerAsBytes()), certSelector.getSerialNumber(), ASN1OctetString.getInstance(certSelector.getSubjectKeyIdentifier()).getOctets());
        }
        else
        {
            return new KeyTransRecipientId(X500Name.getInstance(certSelector.getIssuerAsBytes()), certSelector.getSerialNumber());
        }
}
catch (Exception e)
{
    throw new IllegalArgumentException("conversion failed: " + e.toString());
}
    }
}
