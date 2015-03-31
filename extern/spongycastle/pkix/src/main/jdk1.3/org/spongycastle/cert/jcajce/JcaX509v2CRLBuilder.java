package org.spongycastle.cert.jcajce;

import java.util.Date;

import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.cert.X509v2CRLBuilder;

public class JcaX509v2CRLBuilder
    extends X509v2CRLBuilder
{
    public JcaX509v2CRLBuilder(X500Name issuer, Date now)
    {
        super(issuer, now);
    }
}
