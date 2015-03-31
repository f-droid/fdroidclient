package org.spongycastle.jce.provider;

import org.spongycastle.jce.MultiCertStoreParameters;

import java.security.InvalidAlgorithmParameterException;
import org.spongycastle.jce.cert.CRLSelector;
import org.spongycastle.jce.cert.CertSelector;
import org.spongycastle.jce.cert.CertStore;
import org.spongycastle.jce.cert.CertStoreException;
import org.spongycastle.jce.cert.CertStoreParameters;
import org.spongycastle.jce.cert.CertStoreSpi;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MultiCertStoreSpi
    extends CertStoreSpi
{
    private MultiCertStoreParameters params;

    public MultiCertStoreSpi(CertStoreParameters params)
        throws InvalidAlgorithmParameterException
    {
        super(params);

        if (!(params instanceof MultiCertStoreParameters))
        {
            throw new InvalidAlgorithmParameterException("org.spongycastle.jce.provider.MultiCertStoreSpi: parameter must be a MultiCertStoreParameters object\n" +  params.toString());
        }

        this.params = (MultiCertStoreParameters)params;
    }

    public Collection engineGetCertificates(CertSelector certSelector)
        throws CertStoreException
    {
        boolean searchAllStores = params.getSearchAllStores();
        Iterator iter = params.getCertStores().iterator();
        List allCerts = searchAllStores ? new ArrayList() : Collections.EMPTY_LIST;

        while (iter.hasNext())
        {
            CertStore store = (CertStore)iter.next();
            Collection certs = store.getCertificates(certSelector);

            if (searchAllStores)
            {
                allCerts.addAll(certs);
            }
            else if (!certs.isEmpty())
            {
                return certs;
            }
        }

        return allCerts;
    }

    public Collection engineGetCRLs(CRLSelector crlSelector)
        throws CertStoreException
    {
        boolean searchAllStores = params.getSearchAllStores();
        Iterator iter = params.getCertStores().iterator();
        List allCRLs = searchAllStores ? new ArrayList() : Collections.EMPTY_LIST;
        
        while (iter.hasNext())
        {
            CertStore store = (CertStore)iter.next();
            Collection crls = store.getCRLs(crlSelector);

            if (searchAllStores)
            {
                allCRLs.addAll(crls);
            }
            else if (!crls.isEmpty())
            {
                return crls;
            }
        }

        return allCRLs;
    }
}
