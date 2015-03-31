package org.spongycastle.cert.path;

import java.util.Collections;
import java.util.Set;

public class CertPathValidationResult
{
    private final boolean isValid;
    private final CertPathValidationException cause;
    private final Set unhandledCriticalExtensionOIDs;

    private int[] certIndexes;

    public CertPathValidationResult(CertPathValidationContext context)
    {
        this.unhandledCriticalExtensionOIDs = Collections.unmodifiableSet(context.getUnhandledCriticalExtensionOIDs());
        this.isValid = this.unhandledCriticalExtensionOIDs.isEmpty();
        cause = null;
    }

    public CertPathValidationResult(CertPathValidationContext context, int certIndex, int ruleIndex, CertPathValidationException cause)
    {
        this.unhandledCriticalExtensionOIDs = Collections.unmodifiableSet(context.getUnhandledCriticalExtensionOIDs());
        this.isValid = false;
        this.cause = cause;
    }

    public CertPathValidationResult(CertPathValidationContext context, int[] certIndexes, int[] ruleIndexes, CertPathValidationException[] cause)
    {
        // TODO
        this.unhandledCriticalExtensionOIDs = Collections.unmodifiableSet(context.getUnhandledCriticalExtensionOIDs());
        this.isValid = false;
        this.cause = cause[0];
        this.certIndexes = certIndexes;
    }

    public boolean isValid()
    {
        return isValid;
    }

    public Exception getCause()
    {
        if (cause != null)
        {
            return cause;
        }

        if (!unhandledCriticalExtensionOIDs.isEmpty())
        {
            return new CertPathValidationException("Unhandled Critical Extensions");
        }

        return null;
    }

    public Set getUnhandledCriticalExtensionOIDs()
    {
        return unhandledCriticalExtensionOIDs;
    }

    public boolean isDetailed()
    {
        return this.certIndexes != null;
    }
}
