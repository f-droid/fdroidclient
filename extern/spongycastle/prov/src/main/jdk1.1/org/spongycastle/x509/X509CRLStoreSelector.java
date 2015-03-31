package org.spongycastle.x509;

import org.spongycastle.util.Selector;

import java.security.cert.X509CRLSelector;
import java.security.cert.CRL;

public class X509CRLStoreSelector
    extends X509CRLSelector
    implements Selector
{
    public boolean match(Object obj)
    {
        if (!(obj instanceof CRL))
        {
            return false;
        }

        return super.match((CRL)obj);
    }

    public boolean match(CRL obj)
    {
        return this.match((Object)obj);
    }
}
