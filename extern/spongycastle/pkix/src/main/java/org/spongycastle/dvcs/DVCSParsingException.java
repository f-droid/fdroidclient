package org.spongycastle.dvcs;

/**
 * DVCS parsing exception - thrown when failed to parse DVCS message.
 */
public class DVCSParsingException
    extends DVCSException
{
    private static final long serialVersionUID = -7895880961377691266L;

    public DVCSParsingException(String message)
    {
        super(message);
    }

    public DVCSParsingException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
