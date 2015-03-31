package org.spongycastle.cert.path;

class CertPathValidationResultBuilder
{
    public CertPathValidationResult build()
    {
        return new CertPathValidationResult(null, 0, 0, null);
    }

    public void addException(CertPathValidationException exception)
    {
        //To change body of created methods use File | Settings | File Templates.
    }
}
