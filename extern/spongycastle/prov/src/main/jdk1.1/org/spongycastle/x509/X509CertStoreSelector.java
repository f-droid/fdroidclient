package org.spongycastle.x509;

import org.spongycastle.util.Selector;

import java.security.cert.X509CertSelector;
import java.security.cert.Certificate;

public class X509CertStoreSelector
    extends X509CertSelector
    implements Selector
{
    public boolean match(Object obj)
    {
        if (!(obj instanceof Certificate))
        {
            return false;
        }

        return super.match((Certificate)obj);
    }

    public boolean match(Certificate obj)
    {
        return this.match((Object)obj);
    }
}
