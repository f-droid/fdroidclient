package org.spongycastle.cert.path;

import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.util.Memoable;

public interface CertPathValidation
    extends Memoable
{
    public void validate(CertPathValidationContext context, X509CertificateHolder certificate)
        throws CertPathValidationException;
}
