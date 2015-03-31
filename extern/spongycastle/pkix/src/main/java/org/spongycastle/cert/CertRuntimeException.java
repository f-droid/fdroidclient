package org.spongycastle.cert;

public class CertRuntimeException
    extends RuntimeException
{
    private Throwable cause;

    public CertRuntimeException(String msg, Throwable cause)
    {
        super(msg);

        this.cause = cause;
    }

    public Throwable getCause()
    {
        return cause;
    }
}