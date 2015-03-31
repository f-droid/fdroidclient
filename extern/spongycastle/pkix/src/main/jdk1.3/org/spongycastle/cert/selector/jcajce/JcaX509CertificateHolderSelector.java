package org.spongycastle.cert.selector.jcajce;

import java.math.BigInteger;
import java.security.cert.X509Certificate;

import org.spongycastle.jce.X509Principal;
import org.spongycastle.jce.PrincipalUtil;

import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.X509Extension;
import org.spongycastle.cert.selector.X509CertificateHolderSelector;

public class JcaX509CertificateHolderSelector
    extends X509CertificateHolderSelector
{
    /**
     * Construct a signer identifier based on the issuer, serial number and subject key identifier (if present) of the passed in
     * certificate.
     *
     * @param certificate certificate providing the issue and serial number and subject key identifier.
     */
    public JcaX509CertificateHolderSelector(X509Certificate certificate)
    {
        super(convertPrincipal(certificate), certificate.getSerialNumber(), getSubjectKeyId(certificate));
    }

    private static X500Name convertPrincipal(X509Certificate issuer)
    {
        if (issuer == null)
        {
            return null;
        }
try
{
        return X500Name.getInstance(PrincipalUtil.getIssuerX509Principal(issuer).toASN1Primitive());
}
catch (Exception e)
{
   throw new IllegalArgumentException("conversion failed: " + e.toString());
}
    }

    private static byte[] getSubjectKeyId(X509Certificate cert)
    {
        byte[] ext = cert.getExtensionValue(X509Extension.subjectKeyIdentifier.getId());

        if (ext != null)
        {
            return ASN1OctetString.getInstance(ASN1OctetString.getInstance(ext).getOctets()).getOctets();
        }
        else
        {
            return null;
        }
    }
}
