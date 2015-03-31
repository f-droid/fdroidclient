package org.spongycastle.eac;

/**
 * General checked Exception thrown in the cert package and its sub-packages.
 */
public class EACException
    extends Exception
{
    private Throwable cause;

    public EACException(String msg, Throwable cause)
    {
        super(msg);

        this.cause = cause;
    }

    public EACException(String msg)
    {
        super(msg);
    }

    public Throwable getCause()
    {
        return cause;
    }
}
