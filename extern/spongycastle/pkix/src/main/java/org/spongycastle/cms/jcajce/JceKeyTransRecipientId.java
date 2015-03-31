package org.spongycastle.cms.jcajce;

import java.math.BigInteger;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.cms.KeyTransRecipientId;

public class JceKeyTransRecipientId
    extends KeyTransRecipientId
{
    /**
     * Construct a recipient id based on the issuer, serial number and subject key identifier (if present) of the passed in
     * certificate.
     *
     * @param certificate certificate providing the issue and serial number and subject key identifier.
     */
    public JceKeyTransRecipientId(X509Certificate certificate)
    {
        super(convertPrincipal(certificate.getIssuerX500Principal()), certificate.getSerialNumber(), CMSUtils.getSubjectKeyId(certificate));
    }

    /**
     * Construct a recipient id based on the provided issuer and serial number..
     *
     * @param issuer the issuer to use.
     * @param serialNumber  the serial number to use.
     */
    public JceKeyTransRecipientId(X500Principal issuer, BigInteger serialNumber)
    {
        super(convertPrincipal(issuer), serialNumber);
    }

    /**
     * Construct a recipient id based on the provided issuer, serial number, and subjectKeyId..
     *
     * @param issuer the issuer to use.
     * @param serialNumber  the serial number to use.
     * @param subjectKeyId the subject key ID to use.
     */
    public JceKeyTransRecipientId(X500Principal issuer, BigInteger serialNumber, byte[] subjectKeyId)
    {
        super(convertPrincipal(issuer), serialNumber, subjectKeyId);
    }

    private static X500Name convertPrincipal(X500Principal issuer)
    {
        if (issuer == null)
        {
            return null;
        }

        return X500Name.getInstance(issuer.getEncoded());
    }
}
