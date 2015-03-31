package org.spongycastle.cert.path;

import java.util.HashSet;
import java.util.Set;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.util.Memoable;

public class CertPathValidationContext
    implements Memoable
{
    private Set criticalExtensions;

    private Set handledExtensions = new HashSet();
    private boolean endEntity;
    private int index;

    public CertPathValidationContext(Set criticalExtensionsOIDs)
    {
        this.criticalExtensions = criticalExtensionsOIDs;
    }

    public void addHandledExtension(ASN1ObjectIdentifier extensionIdentifier)
    {
        this.handledExtensions.add(extensionIdentifier);
    }

    public void setIsEndEntity(boolean isEndEntity)
    {
        this.endEntity = isEndEntity;
    }

    public Set getUnhandledCriticalExtensionOIDs()
    {
        Set rv = new HashSet(criticalExtensions);

        rv.removeAll(handledExtensions);

        return rv;
    }

    /**
     * Returns true if the current certificate is the end-entity certificate.
     *
     * @return if current cert end-entity, false otherwise.
     */
    public boolean isEndEntity()
    {
        return endEntity;
    }

    public Memoable copy()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void reset(Memoable other)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
