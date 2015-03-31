package org.spongycastle.cert.jcajce;

import java.security.cert.CRLException;
import java.security.cert.X509CRL;

import org.spongycastle.asn1.x509.CertificateList;
import org.spongycastle.cert.X509CRLHolder;

/**
 * JCA helper class for converting an X509CRL into a X509CRLHolder object.
 */
public class JcaX509CRLHolder
    extends X509CRLHolder
{
    /**
     * Base constructor.
     *
     * @param crl CRL to be used a the source for the holder creation.
     * @throws CRLException if there is a problem extracting the CRL information.
     */
    public JcaX509CRLHolder(X509CRL crl)
        throws CRLException
    {
        super(CertificateList.getInstance(crl.getEncoded()));
    }
}
