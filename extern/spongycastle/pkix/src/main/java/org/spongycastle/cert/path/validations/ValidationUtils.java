package org.spongycastle.cert.path.validations;

import org.spongycastle.cert.X509CertificateHolder;

class ValidationUtils
{
    static boolean isSelfIssued(X509CertificateHolder cert)
    {
        return cert.getSubject().equals(cert.getIssuer());
    }
}
