package org.spongycastle.cms.jcajce;

import java.io.IOException;
import java.security.cert.X509CertSelector;

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
        catch (IOException e)
        {
            throw new IllegalArgumentException("unable to convert issuer: " + e.getMessage());
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
        catch (IOException e)
        {
            throw new IllegalArgumentException("unable to convert issuer: " + e.getMessage());
        }
    }
}
