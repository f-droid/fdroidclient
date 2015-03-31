package org.spongycastle.cert.path;

public class CertPathValidationException
    extends Exception
{
    private final Exception cause;

    public CertPathValidationException(String msg)
    {
        this(msg, null);
    }

    public CertPathValidationException(String msg, Exception cause)
    {
        super(msg);

        this.cause = cause;
    }

    public Throwable getCause()
    {
        return cause;
    }
}
