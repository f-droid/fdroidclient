package org.spongycastle.cms.jcajce;

import java.security.Provider;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.cms.IssuerAndSerialNumber;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.cert.jcajce.JcaX509CertificateHolder;
import org.spongycastle.cms.KeyTransRecipientInfoGenerator;
import org.spongycastle.operator.jcajce.JceAsymmetricKeyWrapper;

public class JceKeyTransRecipientInfoGenerator
    extends KeyTransRecipientInfoGenerator
{
    public JceKeyTransRecipientInfoGenerator(X509Certificate recipientCert)
        throws CertificateEncodingException
    {
        super(new IssuerAndSerialNumber(new JcaX509CertificateHolder(recipientCert).toASN1Structure()), new JceAsymmetricKeyWrapper(recipientCert));
    }

    public JceKeyTransRecipientInfoGenerator(byte[] subjectKeyIdentifier, PublicKey publicKey)
    {
        super(subjectKeyIdentifier, new JceAsymmetricKeyWrapper(publicKey));
    }

    /**
     * Create a generator overriding the algorithm type implied by the public key in the certificate passed in.
     *
     * @param recipientCert certificate carrying the public key.
     * @param algorithmIdentifier the identifier and parameters for the encryption algorithm to be used.
     */
    public JceKeyTransRecipientInfoGenerator(X509Certificate recipientCert, AlgorithmIdentifier algorithmIdentifier)
        throws CertificateEncodingException
    {
        super(new IssuerAndSerialNumber(new JcaX509CertificateHolder(recipientCert).toASN1Structure()), new JceAsymmetricKeyWrapper(algorithmIdentifier, recipientCert.getPublicKey()));
    }

    /**
     * Create a generator overriding the algorithm type implied by the public key passed in.
     *
     * @param subjectKeyIdentifier  the subject key identifier value to associate with the public key.
     * @param algorithmIdentifier  the identifier and parameters for the encryption algorithm to be used.
     * @param publicKey the public key to use.
     */
    public JceKeyTransRecipientInfoGenerator(byte[] subjectKeyIdentifier, AlgorithmIdentifier algorithmIdentifier, PublicKey publicKey)
    {
        super(subjectKeyIdentifier, new JceAsymmetricKeyWrapper(algorithmIdentifier, publicKey));
    }

    public JceKeyTransRecipientInfoGenerator setProvider(String providerName)
    {
        ((JceAsymmetricKeyWrapper)this.wrapper).setProvider(providerName);

        return this;
    }

    public JceKeyTransRecipientInfoGenerator setProvider(Provider provider)
    {
        ((JceAsymmetricKeyWrapper)this.wrapper).setProvider(provider);

        return this;
    }

    /**
     * Internally algorithm ids are converted into cipher names using a lookup table. For some providers
     * the standard lookup table won't work. Use this method to establish a specific mapping from an
     * algorithm identifier to a specific algorithm.
     * <p>
     *     For example:
     * <pre>
     *     unwrapper.setAlgorithmMapping(PKCSObjectIdentifiers.rsaEncryption, "RSA");
     * </pre>
     * </p>
     * @param algorithm  OID of algorithm in recipient.
     * @param algorithmName JCE algorithm name to use.
     * @return the current RecipientInfoGenerator.
     */
    public JceKeyTransRecipientInfoGenerator setAlgorithmMapping(ASN1ObjectIdentifier algorithm, String algorithmName)
    {
        ((JceAsymmetricKeyWrapper)this.wrapper).setAlgorithmMapping(algorithm, algorithmName);

        return this;
    }
}