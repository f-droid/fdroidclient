package java.security.cert;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;

/**
 * An exception indicating one of a variety of problems retrieving
 * certificates and CRLs from a <code>CertStore</code>.<br />
 * <br />
 * A <code>CertStoreException</code> provides support for wrapping
 * exceptions. The {@link #getCause getCause} method returns the throwable, 
 * if any, that caused this exception to be thrown.<br />
 * <br />
 * <b>Concurrent Access</b><br />
 * <br />
 * Unless otherwise specified, the methods defined in this class are not
 * thread-safe. Multiple threads that need to access a single
 * object concurrently should synchronize amongst themselves and
 * provide the necessary locking. Multiple threads each manipulating
 * separate objects need not synchronize.
 *
 * @see CertStore
 **/
public class CertStoreException extends GeneralSecurityException
{
    private Throwable cause;

    /**
     * Creates a <code>CertStoreException</code> with <code>null</code> as
     * its detail message. 
     */
    public CertStoreException()
    {
    super();
    }

    /**
     * Creates a <code>CertStoreException</code> with the given detail
     * message. A detail message is a <code>String</code> that describes this
     * particular exception.
     *
     * @param messag the detail message
     */
    public CertStoreException(String message)
    {
    super(message);
    }

    /**
     * Creates a <code>CertStoreException</code> with the specified detail
     * message and cause.
     *
     * @param messag the detail message
     * @param cause the cause (which is saved for later retrieval by the 
     * {@link #getCause getCause()} method). (A <code>null</code> value is 
     * permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public CertStoreException(String message, Throwable cause)
    {
    super(message);
    this.cause = cause;
    }

    /**
     * Creates a <code>CertStoreException</code> that wraps the specified
     * throwable. This allows any exception to be converted into a
     * <code>CertStoreException</code>, while retaining information about the
     * cause, which may be useful for debugging. The detail message is
     * set to (<code>cause==null ? null : cause.toString()</code>) (which 
     * typically contains the class and detail message of cause).
     *
     * @param cause the cause (which is saved for later retrieval by the 
     * {@link #getCause getCause()} method). (A <code>null</code> value is 
     * permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public CertStoreException(Throwable cause)
    {
    this.cause = cause;
    }

    /**
     * Returns the detail message for this <code>CertStoreException</code>.
     * 
     * @return the detail message, or <code>null</code> if neither the message
     * nor cause were specified
     */
    public String getMessage()
    {
    String message = super.getMessage();

    if ( message == null && cause == null )
        return null;

    StringBuffer s = new StringBuffer();
    if ( message != null )
    {
        s.append(message).append('\n');
    }
    if ( cause != null )
    {
        s.append("Cause:\n").append(cause.getMessage());
    }
    return s.toString();
    }

    /**
     * Returns the cause of this <code>CertStoreException</code> or 
     * <code>null</code> if the cause is nonexistent or unknown.
     * 
     * @return the cause of this throwable or <code>null</code> if the cause 
     * is nonexistent or unknown.
     */
    public Throwable getCause()
    {
    return cause;
    }

    /**
     * Returns a string describing this exception, including a description
     * of the internal (wrapped) cause if there is one.
     *
     * @return a string representation of this
     * <code>CertStoreException</code>
     */
    public String toString()
    {
    String message = getMessage();
    if ( message == null )
        return "";

    return message;
    }

    /**
     * Prints a stack trace to <code>System.err</code>, including the backtrace 
     * of the cause, if any.
     */
    public void printStackTrace() {
    printStackTrace(System.err);
    }

    /**
     * Prints a stack trace to a <code>PrintStream</code>, including the
     * backtrace of the cause, if any.
     *
     * @param ps the <code>PrintStream</code> to use for output
     */
    public void printStackTrace(PrintStream ps) {
    super.printStackTrace(ps);
    if ( cause != null ) {
        cause.printStackTrace(ps);
    }
    }

    /**
     * Prints a stack trace to a <code>PrintWriter</code>, including the
     * backtrace of the cause, if any.
     *
     * @param pw the <code>PrintWriter</code> to use for output
     */
    public void printStackTrace(PrintWriter pw) {
    if ( cause != null ) {
        cause.printStackTrace(pw);
    }
    super.printStackTrace(pw);
    if ( cause != null ) {
        cause.printStackTrace(pw);
    }
    }
}

