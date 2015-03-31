package java.security.cert;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;

/**
 * An exception indicating one of a variety of problems encountered
 * when building a certification path with a
 * <code>CertPathBuilder</code>.<br />
 * <br />
 * A <code>CertPathBuilderException</code> provides support for
 * wrapping exceptions. The {@link #getCause() getCause} method
 * returns the throwable, if any, that caused this exception to be
 * thrown.<br />
 * <br />
 * <strong>Concurrent Access</strong><br />
 * <br />
 * Unless otherwise specified, the methods defined in this class are
 * not thread-safe. Multiple threads that need to access a single
 * object concurrently should synchronize amongst themselves and
 * provide the necessary locking. Multiple threads each manipulating
 * separate objects need not synchronize.
 *
 * @see CertPathBuilder
 **/
public class CertPathBuilderException extends GeneralSecurityException
{
    private Throwable cause;

    /**
     * Creates a <code>CertPathBuilderException</code> with <code>null</code>
     * as its detail message.
     */
    public CertPathBuilderException()
    {
    }

    /**
     * Creates a <code>CertPathBuilderException</code> with the given detail
     * message. The detail message is a <code>String</code> that describes
     * this particular exception in more detail.
     * 
     * @param msg
     *            the detail message
     */
    public CertPathBuilderException(String message)
    {
        super(message);
    }

    /**
     * Creates a <code>CertPathBuilderException</code> that wraps the
     * specified throwable. This allows any exception to be converted into a
     * <code>CertPathBuilderException</code>, while retaining information
     * about the wrapped exception, which may be useful for debugging. The
     * detail message is set to
     * <code>(cause==null ? null : cause.toString())</code> (which typically
     * contains the class and detail message of cause).
     * 
     * @param cause
     *            the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A null value is permitted, and
     *            indicates that the cause is nonexistent or unknown.)
     */
    public CertPathBuilderException(String message, Throwable cause)
    {
        super(message);
        this.cause = cause;
    }

    /**
     * Creates a <code>CertPathBuilderException</code> with the specified
     * detail message and cause.
     * 
     * @param msg
     *            the detail message
     * @param cause
     *            the cause (which is saved for later retrieval by the
     *            {@link #getCause()} method). (A null value is permitted, and
     *            indicates that the cause is nonexistent or unknown.)
     */
    public CertPathBuilderException(Throwable cause)
    {
        this.cause = cause;
    }

    /**
     * Returns the internal (wrapped) cause, or null if the cause is nonexistent
     * or unknown.
     * 
     * @return the cause of this throwable or <code>null</code> if the cause
     *         is nonexistent or unknown.
     */
    public Throwable getCause()
    {
        return cause;
    }

    /**
     * Returns the detail message for this CertPathBuilderException.
     * 
     * @return the detail message, or <code>null</code> if neither the message
     *         nor internal cause were specified
     */
    public String getMessage()
    {
        String message = super.getMessage();

        if (message == null && cause == null)
        {
            return null;
        }

        if (cause != null)
        {
            return cause.getMessage();
        }

        return message;
    }

    /**
     * Returns a string describing this exception, including a description of
     * the internal (wrapped) cause if there is one.
     * 
     * @return a string representation of this
     *         <code>CertPathBuilderException</code>
     */
    public String toString()
    {
        String message = getMessage();
        if (message == null)
        {
            return "";
        }

        return message;
    }

    /**
     * Prints a stack trace to <code>System.err</code>, including the
     * backtrace of the cause, if any.
     */
    public void printStackTrace()
    {
        printStackTrace(System.err);
    }

    /**
     * Prints a stack trace to a <code>PrintStream</code>, including the
     * backtrace of the cause, if any.
     * 
     * @param ps
     *            the <code>PrintStream</code> to use for output
     */
    public void printStackTrace(PrintStream ps)
    {
        super.printStackTrace(ps);
        if (getCause() != null)
        {
            getCause().printStackTrace(ps);
        }
    }

    /**
     * Prints a stack trace to a <code>PrintWriter</code>, including the
     * backtrace of the cause, if any.
     * 
     * @param ps
     *            the <code>PrintWriter</code> to use for output
     */
    public void printStackTrace(PrintWriter pw)
    {
        super.printStackTrace(pw);
        if (getCause() != null)
        {
            getCause().printStackTrace(pw);
        }
    }
}

