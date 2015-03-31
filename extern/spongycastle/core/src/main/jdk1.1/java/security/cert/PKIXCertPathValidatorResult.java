package java.security.cert;

import java.security.PublicKey;

/**
 * This class represents the successful result of the PKIX certification 
 * path validation algorithm. <br />
 * <br />
 * Instances of <code>PKIXCertPathValidatorResult</code> are returned by the 
 * {@link CertPathValidator#validate validate} method of 
 * <code>CertPathValidator</code> objects implementing the PKIX algorithm.<br />
 * <br />
 * All <code>PKIXCertPathValidatorResult</code> objects contain the 
 * valid policy tree and subject public key resulting from the 
 * validation algorithm, as well as a <code>TrustAnchor</code> describing
 * the certification authority (CA) that served as a trust anchor for the 
 * certification path.<br />
 * <br />
 * <b>Concurrent Access</b><br />
 * <br />
 * Unless otherwise specified, the methods defined in this class are not
 * thread-safe. Multiple threads that need to access a single
 * object concurrently should synchronize amongst themselves and
 * provide the necessary locking. Multiple threads each manipulating
 * separate objects need not synchronize.
 *
 * @see CertPathValidatorResult
 **/
public class PKIXCertPathValidatorResult implements CertPathValidatorResult
{
    private TrustAnchor trustAnchor;
    private PolicyNode policyTree;
    private PublicKey subjectPublicKey;

    /**
     * Creates an instance of <code>PKIXCertPathValidatorResult</code>
     * containing the specified parameters. 
     * 
     * @param trustAnchor a <code>TrustAnchor</code> describing the CA that 
     * served as a trust anchor for the certification path
     * @param policyTree the immutable valid policy tree, or <code>null</code> 
     * if there are no valid policies
     * @param subjectPublicKey the public key of the subject
     *
     * @exception NullPointerException if the <code>subjectPublicKey</code> or
     * <code>trustAnchor</code> parameters are <code>null</code>
     */
    public PKIXCertPathValidatorResult(TrustAnchor trustAnchor,
                       PolicyNode policyTree,
                       PublicKey subjectPublicKey)
    {
    if ( subjectPublicKey == null )
        throw new NullPointerException( "subjectPublicKey must be non-null" );
    if ( trustAnchor == null )
        throw new NullPointerException( "trustAnchor must be non-null" );

    this.trustAnchor = trustAnchor;
    this.policyTree = policyTree;
    this.subjectPublicKey = subjectPublicKey;
    }

    /**
     * Returns the <code>TrustAnchor</code> describing the CA that served
     * as a trust anchor for the certification path.
     *
     * @return the <code>TrustAnchor</code> (never <code>null</code>)
     */
    public TrustAnchor getTrustAnchor()
    {
    return trustAnchor;
    }

    /**
     * Returns the root node of the valid policy tree resulting from the 
     * PKIX certification path validation algorithm. The 
     * <code>PolicyNode</code> object that is returned and any objects that 
     * it returns through public methods are immutable.<br />
     * <br />
     * Most applications will not need to examine the valid policy tree.
     * They can achieve their policy processing goals by setting the
     * policy-related parameters in <code>PKIXParameters</code>. However, more
     * sophisticated applications, especially those that process policy
     * qualifiers, may need to traverse the valid policy tree using the
     * {@link PolicyNode#getParent PolicyNode.getParent} and 
     * {@link PolicyNode#getChildren PolicyNode.getChildren} methods.
     * 
     * @return the root node of the valid policy tree, or <code>null</code> 
     * if there are no valid policies
     */
    public PolicyNode getPolicyTree()
    {
    return policyTree;
    }

    /**
     * Returns the public key of the subject (target) of the certification
     * path, including any inherited public key parameters if applicable. 
     *
     * @return the public key of the subject (never <code>null</code>)
     */
    public PublicKey getPublicKey()
    {
    return subjectPublicKey;
    }

    /**
     * Returns a copy of this object.
     *
     * @return the copy
     */
    public Object clone()
    {
    try {
        return super.clone();
    } catch ( CloneNotSupportedException ex ) {
        throw new InternalError( ex.toString() );
    }
    }

    /**
     * Return a printable representation of this 
     * <code>PKIXCertPathValidatorResult</code>.
     *
     * @return a <code>String</code> describing the contents of this
     *         <code>PKIXCertPathValidatorResult</code>
     */
    public String toString()
    {
    StringBuffer s = new StringBuffer();
    s.append( "PKIXCertPathValidatorResult: [ \n" );
    s.append( "  Trust Anchor: ").append(getTrustAnchor()).append('\n' );
    s.append( "  Policy Tree: ").append(getPolicyTree()).append('\n' );
    s.append( "  Subject Public Key: ").append(getPublicKey()).append("\n]" );
    return s.toString();
    }
}
