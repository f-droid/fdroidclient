package org.spongycastle.cert.ocsp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.spongycastle.asn1.ASN1GeneralizedTime;
import org.spongycastle.asn1.x509.Extensions;
import org.spongycastle.cert.X509CertificateHolder;

class OCSPUtils
{
    static final X509CertificateHolder[] EMPTY_CERTS = new X509CertificateHolder[0];

    static Set EMPTY_SET = Collections.unmodifiableSet(new HashSet());
    static List EMPTY_LIST = Collections.unmodifiableList(new ArrayList());

    static Date extractDate(ASN1GeneralizedTime time)
    {
        try
        {
            return time.getDate();
        }
        catch (Exception e)
        {
            throw new IllegalStateException("exception processing GeneralizedTime: " + e.getMessage());
        }
    }

    static Set getCriticalExtensionOIDs(Extensions extensions)
    {
        if (extensions == null)
        {
            return EMPTY_SET;
        }

        return Collections.unmodifiableSet(new HashSet(Arrays.asList(extensions.getCriticalExtensionOIDs())));
    }

    static Set getNonCriticalExtensionOIDs(Extensions extensions)
    {
        if (extensions == null)
        {
            return EMPTY_SET;
        }

        // TODO: should probably produce a set that imposes correct ordering
        return Collections.unmodifiableSet(new HashSet(Arrays.asList(extensions.getNonCriticalExtensionOIDs())));
    }

    static List getExtensionOIDs(Extensions extensions)
    {
        if (extensions == null)
        {
            return EMPTY_LIST;
        }

        return Collections.unmodifiableList(Arrays.asList(extensions.getExtensionOIDs()));
    }
}
