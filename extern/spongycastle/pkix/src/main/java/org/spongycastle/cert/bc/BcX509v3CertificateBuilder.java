package org.spongycastle.cert.bc;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;

import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.crypto.util.SubjectPublicKeyInfoFactory;

/**
 * JCA helper class to allow BC lightweight objects to be used in the construction of a Version 3 certificate.
 */
public class BcX509v3CertificateBuilder
    extends X509v3CertificateBuilder
{
    /**
     * Initialise the builder using a PublicKey.
     *
     * @param issuer X500Name representing the issuer of this certificate.
     * @param serial the serial number for the certificate.
     * @param notBefore date before which the certificate is not valid.
     * @param notAfter date after which the certificate is not valid.
     * @param subject X500Name representing the subject of this certificate.
     * @param publicKey the public key to be associated with the certificate.
     */
    public BcX509v3CertificateBuilder(X500Name issuer, BigInteger serial, Date notBefore, Date notAfter, X500Name subject, AsymmetricKeyParameter publicKey)
        throws IOException
    {
        super(issuer, serial, notBefore, notAfter, subject, SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(publicKey));
    }

    /**
     * Initialise the builder using the subject from the passed in issuerCert as the issuer, as well as
     * passing through and converting the other objects provided.
     *
     * @param issuerCert holder for certificate who's subject is the issuer of the certificate we are building.
     * @param serial the serial number for the certificate.
     * @param notBefore date before which the certificate is not valid.
     * @param notAfter date after which the certificate is not valid.
     * @param subject principal representing the subject of this certificate.
     * @param publicKey the public key to be associated with the certificate.
     */
    public BcX509v3CertificateBuilder(X509CertificateHolder issuerCert, BigInteger serial, Date notBefore, Date notAfter, X500Name subject, AsymmetricKeyParameter publicKey)
        throws IOException
    {
        super(issuerCert.getSubject(), serial, notBefore, notAfter, subject, SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(publicKey));
    }
}
