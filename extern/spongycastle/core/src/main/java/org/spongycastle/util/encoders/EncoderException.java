package org.spongycastle.util.encoders;

public class EncoderException
    extends IllegalStateException
{
    private Throwable cause;

    EncoderException(String msg, Throwable cause)
    {
        super(msg);

        this.cause = cause;
    }

    public Throwable getCause()
    {
        return cause;
    }
}
