package org.spongycastle.crypto.tls;

/**
 * A certificate verifyer, that will always return true.
 * <p/>
 * <pre>
 * DO NOT USE THIS FILE UNLESS YOU KNOW EXACTLY WHAT YOU ARE DOING.
 * </pre>
 *
 * @deprecated Perform certificate verification in TlsAuthentication implementation
 */
public class AlwaysValidVerifyer
    implements CertificateVerifyer
{
    /**
     * Return true.
     *
     * @see org.spongycastle.crypto.tls.CertificateVerifyer#isValid(org.spongycastle.asn1.x509.Certificate[])
     */
    public boolean isValid(org.spongycastle.asn1.x509.Certificate[] certs)
    {
        return true;
    }
}
