package java.security.cert;

/**
 * A specification of the result of a certification path validator algorithm.<br />
 * <br />
 * The purpose of this interface is to group (and provide type safety 
 * for) all certification path validator results. All results returned 
 * by the {@link CertPathValidator#validate CertPathValidator.validate}
 * method must implement this interface.  
 *
 * @see CertPathValidator
 **/
public interface CertPathValidatorResult extends Cloneable
{
    /**
     * Makes a copy of this <code>CertPathValidatorResult</code>. Changes to the
     * copy will not affect the original and vice versa.
     *
     * @return a copy of this <code>CertPathValidatorResult</code>
     */
    public Object clone();
}
