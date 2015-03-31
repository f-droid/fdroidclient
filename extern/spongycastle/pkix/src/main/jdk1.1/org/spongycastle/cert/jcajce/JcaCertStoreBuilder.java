package org.spongycastle.cert.jcajce;

import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.NoSuchProviderException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CRLException;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CollectionCertStoreParameters;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.spongycastle.cert.X509CRLHolder;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.util.Store;

/**
 * Builder to create a CertStore from certificate and CRL stores.
 */
public class JcaCertStoreBuilder
{
    private List certs = new ArrayList();
    private List crls = new ArrayList();
    private Object provider;
    private JcaX509CertificateConverter certificateConverter = new JcaX509CertificateConverter();
    private JcaX509CRLConverter crlConverter = new JcaX509CRLConverter();

    /**
     *  Add a store full of X509CertificateHolder objects.
     *
     * @param certStore a store of X509CertificateHolder objects.
     */
    public JcaCertStoreBuilder addCertificates(Store certStore)
    {
        certs.addAll(certStore.getMatches(null));

        return this;
    }

    /**
     * Add a single certificate.
     *
     * @param cert  the X509 certificate holder containing the certificate.
     */
    public JcaCertStoreBuilder addCertificate(X509CertificateHolder cert)
    {
        certs.add(cert);

        return this;
    }

    /**
     * Add a store full of X509CRLHolder objects.
     * @param crlStore  a store of X509CRLHolder objects.
     */
    public JcaCertStoreBuilder addCRLs(Store crlStore)
    {
        crls.addAll(crlStore.getMatches(null));

        return this;
    }

    /**
     * Add a single CRL.
     *
     * @param crl  the X509 CRL holder containing the CRL.
     */
    public JcaCertStoreBuilder addCRL(X509CRLHolder crl)
    {
        crls.add(crl);

        return this;
    }

    public JcaCertStoreBuilder setProvider(String providerName)
        throws GeneralSecurityException
    {
        certificateConverter.setProvider(providerName);
        crlConverter.setProvider(providerName);
        this.provider = providerName;

        return this;
    }

    public JcaCertStoreBuilder setProvider(Provider provider)
        throws GeneralSecurityException
    {
        certificateConverter.setProvider(provider);
        crlConverter.setProvider(provider);
        this.provider = provider;

        return this;
    }

    /**
     * Build the CertStore from the current inputs.
     *
     * @return  a CertStore.
     * @throws GeneralSecurityException
     */
    public CertStore build()
        throws GeneralSecurityException
    {
        CollectionCertStoreParameters params = convertHolders(certificateConverter, crlConverter);

        try
{
        if (provider instanceof String)
        {
            return CertStore.getInstance("Collection", params, (String)provider);
        }

        if (provider instanceof Provider)
        {
            return CertStore.getInstance("Collection", params, (Provider)provider);
        }

        return CertStore.getInstance("Collection", params);
}
catch (NoSuchAlgorithmException e)
{
    throw new GeneralSecurityException(e.toString());
}
catch (NoSuchProviderException e)
{
    throw new GeneralSecurityException(e.toString());
}
    }

    private CollectionCertStoreParameters convertHolders(JcaX509CertificateConverter certificateConverter, JcaX509CRLConverter crlConverter)
        throws CertificateException, CRLException
    {
        List jcaObjs = new ArrayList(certs.size() + crls.size());

        for (Iterator it = certs.iterator(); it.hasNext();)
        {
            jcaObjs.add(certificateConverter.getCertificate((X509CertificateHolder)it.next()));
        }

        for (Iterator it = crls.iterator(); it.hasNext();)
        {
            jcaObjs.add(crlConverter.getCRL((X509CRLHolder)it.next()));
        }

        return new CollectionCertStoreParameters(jcaObjs);
    }
}
