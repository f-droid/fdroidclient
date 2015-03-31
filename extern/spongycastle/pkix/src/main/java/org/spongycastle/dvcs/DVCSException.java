package org.spongycastle.dvcs;

/**
 * General DVCSException.
 */
public class DVCSException
    extends Exception
{
    private static final long serialVersionUID = 389345256020131488L;

    private Throwable cause;

    public DVCSException(String message)
    {
        super(message);
    }

    public DVCSException(String message, Throwable cause)
    {
        super(message);
        this.cause = cause;
    }

    public Throwable getCause()
    {
        return cause;
    }
}
