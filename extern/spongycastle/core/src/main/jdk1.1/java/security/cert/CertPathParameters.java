package java.security.cert;

/**
 * A specification of certification path algorithm parameters. The purpose
 * of this interface is to group (and provide type safety for) all CertPath
 * parameter specifications. All <code>CertPath</code> parameter specifications must
 * implement this interface. 
 **/
public interface CertPathParameters extends Cloneable
{
    /**
     * Makes a copy of this <code>CertPathParameters</code>. Changes to the
     * copy will not affect the original and vice versa.
     *
     * @return a copy of this <code>CertPathParameters</code>
     **/
    public Object clone();
}
