package java.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Parameters used as input for the PKIX CertPathValidator  algorithm.<br />
 * <br />
 * A PKIX <code>CertPathValidator</code> uses these parameters to validate a
 * <code>CertPath</code> according to the PKIX certification path validation
 * algorithm.<br />
 * <br />
 * To instantiate a <code>PKIXParameters</code> object, an application must specify
 * one or more <i>most-trusted CAs</i> as defined by the PKIX certification
 * path validation algorithm. The most-trusted CAs can be specified
 * using one of two constructors. An application can call
 * {@link #PKIXParameters(Set)}, specifying a Set of <code>TrustAnchor</code> objects, each
 * of which identify a most-trusted CA. Alternatively, an application
 * can call {@link #PKIXParameters(KeyStore)}, specifying a <code>KeyStore</code> instance
 * containing trusted certificate entries, each of which will be
 * considered as a most-trusted CA.<br />
 * <br />
 * Once a <code>PKIXParameters</code> object has been created, other parameters can
 * be specified (by calling {@link #setInitialPolicies} or {@link #setDate}, for
 * instance) and then the <code>PKIXParameters</code> is passed along with the
 * <code>CertPath</code> to be validated to {@link CertPathValidator#validate}.<br />
 * <br />
 * Any parameter that is not set (or is set to null) will be set to the
 * default value for that parameter. The default value for the date
 * parameter is null, which indicates the current time when the path is
 * validated. The default for the remaining parameters is the least
 * constrained.<br />
 * <br />
 * <b>Concurrent Access</b><br />
 * <br />
 * Unless otherwise specified, the methods defined in this class are
 * not thread-safe. Multiple threads that need to access a single
 * object concurrently should synchronize amongst themselves and
 * provide the necessary locking. Multiple threads each manipulating
 * separate objects need not synchronize.
 *
 * @see CertPathValidator
 **/
public class PKIXParameters implements CertPathParameters {
    private Set trustAnchors;
    private Set initialPolicies = new HashSet();
    private List certStores = new ArrayList();
    private CertSelector certSelector;
    private List certPathCheckers = new ArrayList();
    private boolean revocationEnabled = true;
    private boolean explicitPolicyRequired = false;
    private boolean policyMappingInhibited = false;
    private boolean anyPolicyInhibited = false;
    private boolean policyQualifiersRejected = true;
    private Date date;
    private String sigProvider;

    /**
     * Creates an instance of PKIXParameters with the specified
     * Set of most-trusted CAs. Each element of the set is a
     * TrustAnchor.<br />
     * <br />
     * Note that the Set is copied to protect against subsequent
     * modifications.
     *
     * @param trustAnchors a Set of TrustAnchors
     *
     * @exception InvalidAlgorithmParameterException if the
     * specified Set is empty <code>(trustAnchors.isEmpty() == true)</code>
     * @exception NullPointerException if the specified Set is <code>null</code>
     * @exception ClassCastException if any of the elements in the
     * Set are not of type
     * <code>java.security.cert.TrustAnchor</code>
     **/
    public PKIXParameters(Set trustAnchors)
    throws InvalidAlgorithmParameterException
    {
    setTrustAnchors( trustAnchors );
    }

    /**
     * Creates an instance of PKIXParameters that populates the
     * set of most-trusted CAs from the trusted certificate
     * entries contained in the specified KeyStore. Only keystore
     * entries that contain trusted X509Certificates  are
     * considered; all other certificate types are ignored.
     *
     * @param keystore a KeyStore from which the set of
     * most-trusted CAs will be populated
     *
     * @exception KeyStoreException if the keystore has not been
     * initialized
     * @exception InvalidAlgorithmParameterException if the keystore
     * does not contain at least one trusted certificate entry
     * @exception NullPointerException if the keystore is null
    **/
    public PKIXParameters(KeyStore keystore)
    throws KeyStoreException,
    InvalidAlgorithmParameterException
    {
    if ( keystore == null )
        throw new NullPointerException( "the keystore parameter must be non-null" );

    Set trustAnchors = new HashSet();
    String alias;
    Certificate cert;
    Enumeration enum = keystore.aliases();
    while ( enum.hasMoreElements() ) {
        alias = (String)enum.nextElement();
        if ( keystore.isCertificateEntry( alias ) ) {
        cert = keystore.getCertificate( alias );
        if ( cert instanceof X509Certificate )
            trustAnchors.add( new TrustAnchor( (X509Certificate)cert, null ) );
        }
    }
    setTrustAnchors( trustAnchors );
    }

    /**
     * Returns an immutable Set of the most-trusted CAs.
     *
     * @return an immutable <code>Set</code> of
     * <code>TrustAnchors</code> (never <code>null</code>)
     *
     * @see #setTrustAnchors
     **/
    public Set getTrustAnchors()
    {
    return Collections.unmodifiableSet(trustAnchors);
    }

    /**
     * Sets the Set of most-trusted CAs.<br />
     * <br />
     * Note that the Set is copied to protect against subsequent
     * modifications.<br />
     * <br />
     * @param trustAnchors a Set of TrustAnchors
     *
     * @exception InvalidAlgorithmParameterException if the specified Set is empty <code>(trustAnchors.isEmpty() == true)</code>
     * @exception NullPointerException if the specified Set is <code>null</code>
     * @exception ClassCastException if any of the elements in
     * the set are not of type java.security.cert.TrustAnchor
     *
     * @see #getTrustAnchors
     **/
    public void setTrustAnchors(Set trustAnchors)
    throws InvalidAlgorithmParameterException
    {
    if ( trustAnchors == null )
        throw new NullPointerException("the trustAnchors parameter must be non-null");
    if ( trustAnchors.isEmpty() )
        throw new InvalidAlgorithmParameterException("the trustAnchors parameter must be non-empty");

    Iterator iter = trustAnchors.iterator();
    TrustAnchor obj;
    this.trustAnchors = new HashSet();
    while( iter.hasNext() ) {
        obj = (TrustAnchor)iter.next();
        if ( obj != null ) {
        this .trustAnchors.add( obj );
        }
    }
    }

    /**
     * Returns an immutable Set of initial policy identifiers (OID
     * strings), indicating that any one of these policies would
     * be acceptable to the certificate user for the purposes of
     * certification path processing. The default return value is
     * an empty <code>Set</code>, which is interpreted as meaning that any
     * policy would be acceptable.
     *
     * @return an immutable <code>Set</code> of initial policy
     * OIDs in String format, or an empty <code>Set</code> (implying any policy
     * is acceptable). Never returns <code>null</code>.
     *
     * @see #setInitialPolicies(java.util.Set)
     **/
    public Set getInitialPolicies()
    {
    Set returnSet = initialPolicies;
    if ( initialPolicies == null )
        returnSet = new HashSet();

    return Collections.unmodifiableSet( returnSet );
    }

    /**
     * Sets the <code>Set</code> of initial policy identifiers (OID strings),
     * indicating that any one of these policies would be
     * acceptable to the certificate user for the purposes of
     * certification path processing. By default, any policy is
     * acceptable (i.e. all policies), so a user that wants to
     * allow any policy as acceptable does not need to call this
     * method, or can call it with an empty <code>Set</code> (or <code>null</code>).<br />
     * <br />
     * Note that the Set is copied to protect against subsequent
     * modifications.<br />
     * <br />
     * @param initialPolicies a Set of initial policy OIDs in String format (or <code>null</code>)
     *
     * @exception ClassCastException if any of the elements in the
     * set are not of type String
     *
     * @see #getInitialPolicies()
     **/
    public void setInitialPolicies(Set initialPolicies)
    {
    if ( initialPolicies == null || initialPolicies.isEmpty() )
    {
        this.initialPolicies = null;
    }
    else
    {
        Iterator iter = initialPolicies.iterator();
        this.initialPolicies = new HashSet();
        String obj;
        while ( iter.hasNext() )
        {
        obj = (String)iter.next();
        if ( obj != null ) {
            this.initialPolicies.add( obj );
        }
        }
    }
    }

    /**
     * Sets the list of CertStores to be used in finding
     * certificates and CRLs. May be null, in which case no
     * CertStores will be used. The first CertStores in the list
     * may be preferred to those that appear later.<br />
     * <br />
     * Note that the List is copied to protect against subsequent
     * modifications.<br />
     * <br />
     * @param stores a List of CertStores (or <code>null</code>)
     *
     * @exception ClassCastException if any of the elements in the
     * list are not of type <code>java.security.cert.CertStore</code>
     *
     * @see #getCertStores()
     **/
    public void setCertStores(List stores)
    {
    certStores = new ArrayList();
    if ( stores != null && ! stores.isEmpty() )
    {
        Iterator iter = stores.iterator();
        CertStore obj;
        while ( iter.hasNext() )
        {
        obj = (CertStore)iter.next();
        if ( obj != null )
        {
            certStores.add( obj );
        }
        }
    }
    }

    /**
     * Adds a CertStore to the end of the list of CertStores used
     * in finding certificates and CRLs.
     *
     * @param store  the <code>CertStore</code> to add. If
     * <code>null</code<, the store is ignored (not added to
     * list).
     **/
    public void addCertStore(CertStore store)
    {
    if ( store != null )
        certStores.add( store );
    }

    /**
     * Returns an immutable List of CertStores that are used to
     * find certificates and CRLs.
     *
     * @return an immutable List of CertStores (may be empty, but never <code>null</code>)
     *
     * @see #setCertStores(java.util.List)
     **/
    public List getCertStores()
    {
    return Collections.unmodifiableList(certStores);
    }

    /**
    * Sets the RevocationEnabled flag. If this flag is true, the default
    * revocation checking mechanism of the underlying PKIX service provider 
    * will be used. If this flag is false, the default revocation checking 
    * mechanism will be disabled (not used).<br />
    * <br />
    * When a <code>PKIXParameters</code> object is created, this flag is set
    * to true. This setting reflects the most common strategy for checking
    * revocation, since each service provider must support revocation 
    * checking to be PKIX compliant. Sophisticated applications should set
    * this flag to false when it is not practical to use a PKIX service 
    * provider's default revocation checking mechanism or when an alternative 
    * revocation checking mechanism is to be substituted (by also calling the 
    * {@link #addCertPathChecker addCertPathChecker} or {@link 
    * #setCertPathCheckers setCertPathCheckers} methods).
    *
    * @param val the new value of the RevocationEnabled flag
    **/
    public void setRevocationEnabled(boolean val)
    {
    revocationEnabled = val;
    }

    /** 
     * Checks the RevocationEnabled flag. If this flag is true,
     * the default revocation checking mechanism of the underlying
     * PKIX service provider will be used. If this flag is false,
     * the default revocation checking mechanism will be disabled
     * (not used). See the setRevocationEnabled method for more
     * details on setting the value of this flag.
     *
     * @return the current value of the RevocationEnabled flag
    **/
    public boolean isRevocationEnabled()
    {
    return revocationEnabled;
    }

    /**
     * Sets the ExplicitPolicyRequired flag. If this flag is true,
     * an acceptable policy needs to be explicitly identified in
     * every certificate. By default, the ExplicitPolicyRequired
     * flag is false.
     *
     * @param val true if explicit policy is to be required, false
     * otherwise
     **/
    public void setExplicitPolicyRequired(boolean val)
    {
    explicitPolicyRequired = val;
    }

    /**
     * Checks if explicit policy is required. If this flag is
     * true, an acceptable policy needs to be explicitly
     * identified in every certificate. By default, the
     * ExplicitPolicyRequired flag is false.
     *
     * @return true if explicit policy is required, false otherwise
     **/
    public boolean isExplicitPolicyRequired()
    {
    return explicitPolicyRequired;
    }

    /**
     * Sets the PolicyMappingInhibited flag. If this flag is true,
     * policy mapping is inhibited. By default, policy mapping is
     * not inhibited (the flag is false).
     *
     * @param val true if policy mapping is to be inhibited, false otherwise
     **/
    public void setPolicyMappingInhibited(boolean val)
    {
    policyMappingInhibited = val;
    }

    /**
     * Checks if policy mapping is inhibited. If this flag is
     * true, policy mapping is inhibited. By default, policy
     * mapping is not inhibited (the flag is false).
     *
     * @return true if policy mapping is inhibited, false otherwise
     **/
    public boolean isPolicyMappingInhibited()
    {
    return policyMappingInhibited;
    }

    /**
     * Sets state to determine if the any policy OID should be
     * processed if it is included in a certificate. By default,
     * the any policy OID is not inhibited ({@link #isAnyPolicyInhibited()}
     * returns false).
     *
     * @return val - <code>true</code> if the any policy OID is to be inhibited, <code>false</code> otherwise
     **/
    public void setAnyPolicyInhibited(boolean val)
    {
    anyPolicyInhibited = val;
    }

    /**
     * Checks whether the any policy OID should be processed if it
     * is included in a certificate.
     *
     * @return <code>true</code> if the any policy OID is inhibited, <code>false</code> otherwise
     **/
    public boolean isAnyPolicyInhibited()
    {
    return anyPolicyInhibited;
    }

    /**
     * Sets the PolicyQualifiersRejected flag. If this flag is
     * true, certificates that include policy qualifiers in a
     * certificate policies extension that is marked critical are
     * rejected. If the flag is false, certificates are not
     * rejected on this basis.<br />
     * <br />
     * When a <code>PKIXParameters</code> object is created, this flag is set
     * to true. This setting reflects the most common (and
     * simplest) strategy for processing policy
     * qualifiers. Applications that want to use a more
     * sophisticated policy must set this flag to false.<br />
     * <br />
     * Note that the PKIX certification path validation algorithm
     * specifies that any policy qualifier in a certificate
     * policies extension that is marked critical must be
     * processed and validated. Otherwise the certification path
     * must be rejected. If the policyQualifiersRejected flag is
     * set to false, it is up to the application to validate all
     * policy qualifiers in this manner in order to be PKIX
     * compliant.
     *
     * @param qualifiersRejected the new value of the PolicyQualifiersRejected flag
     *
     * @see #getPolicyQualifiersRejected()
     * @see PolicyQualifierInfo
     **/
    public void setPolicyQualifiersRejected(boolean qualifiersRejected)
    {
    policyQualifiersRejected = qualifiersRejected;
    }

    /**
     * Gets the PolicyQualifiersRejected flag. If this flag is
     * true, certificates that include policy qualifiers in a
     * certificate policies extension that is marked critical are
     * rejected. If the flag is false, certificates are not
     * rejected on this basis.<br />
     * <br />
     * When a PKIXParameters object is created, this flag is set to
     * true. This setting reflects the most common (and simplest)
     * strategy for processing policy qualifiers. Applications that
     * want to use a more sophisticated policy must set this flag
     * to false.
     *
     * @return the current value of the PolicyQualifiersRejected flag
     *
     * @see #setPolicyQualifiersRejected(boolean)
     **/
    public boolean getPolicyQualifiersRejected()
    {
    return policyQualifiersRejected;
    }

        /**
     * Returns the time for which the validity of the
     * certification path should be determined. If null, the
     * current time is used.<br />
     * <br />
     * Note that the Date returned is copied to protect against
     * subsequent modifications.
     *
     * @return the Date, or <code>null</code> if not set
     *
     * @see #setDate(java.util.Date)
     **/
    public Date getDate()
    {
    if ( date == null )
        return null;

    return new Date( date.getTime() );
    }

    /**
     * Sets the time for which the validity of the certification
     * path should be determined. If null, the current time is
     * used.<br />
     * <br />
     * Note that the Date supplied here is copied to protect
     * against subsequent modifications.
     *
     * @param date the Date, or <code>null</code> for the current time
     *
     * @see #getDate()
     **/
    public void setDate(Date date)
    {
    if ( date == null )
        this.date = null;
    else
        this.date = new Date( date.getTime() );
    }

    /**
     * Sets a <code>List</code> of additional certification path checkers. If
     * the specified List contains an object that is not a
     * PKIXCertPathChecker, it is ignored.<br />
     * <br />
     * Each <code>PKIXCertPathChecker</code> specified implements additional
     * checks on a certificate. Typically, these are checks to
     * process and verify private extensions contained in
     * certificates. Each <code>PKIXCertPathChecker</code> should be
     * instantiated with any initialization parameters needed to
     * execute the check.<br />
     * <br />
     * This method allows sophisticated applications to extend a
     * PKIX <code>CertPathValidator</code> or <code>CertPathBuilder</code>. Each of the
     * specified PKIXCertPathCheckers will be called, in turn, by
     * a PKIX <code>CertPathValidator</code> or <code>CertPathBuilder</code> for each
     * certificate processed or validated.<br />
     * <br />
     * Regardless of whether these additional PKIXCertPathCheckers
     * are set, a PKIX <code>CertPathValidator</code> or <code>CertPathBuilder</code> must
     * perform all of the required PKIX checks on each
     * certificate. The one exception to this rule is if the
     * RevocationEnabled flag is set to false (see the
     * {@link #setRevocationEnabled(boolean) setRevocationEnabled} method).<br />
     * <br />
     * Note that the List supplied here is copied and each
     * PKIXCertPathChecker in the list is cloned to protect against
     * subsequent modifications.
     *
     * @param checkers a List of PKIXCertPathCheckers. May be
     * null, in which case no additional checkers will be used.
     * @exception ClassCastException if any of the elements in the
     * list are not of type
     * <code>java.security.cert.PKIXCertPathChecker</code>
     * @see #getCertPathCheckers()
     **/
    public void setCertPathCheckers(List checkers)
    {
    certPathCheckers = new ArrayList();
    if ( checkers == null )
        return;
    Iterator iter = checkers.iterator();
    while ( iter.hasNext() )
        certPathCheckers.add( (PKIXCertPathChecker)((PKIXCertPathChecker)iter.next()).clone() );
    }

    /**
     * Returns the List of certification path checkers. The
     * returned List is immutable, and each PKIXCertPathChecker in
     * the List is cloned to protect against subsequent
     * modifications.
     *
     * @return an immutable List of PKIXCertPathCheckers (may be empty, but not <code>null</code>)
     *
     * @see #setCertPathCheckers(java.util.List)
     **/
    public List getCertPathCheckers()
    {
    List checkers = new ArrayList();
    Iterator iter = certPathCheckers.iterator();
    while ( iter.hasNext() )
    {    
        checkers.add( (PKIXCertPathChecker)((PKIXCertPathChecker)iter.next()).clone() );
    }
    return Collections.unmodifiableList(checkers);
    }

    /**
     * Adds a PKIXCertPathChecker to the list of certification
     * path checkers. See the {@link #setCertPathCheckers} method for more
     * details.<br />
     * <br />
     * Note that the <code>PKIXCertPathChecker</code> is cloned to protect
     * against subsequent modifications.
     *
     * @param checker  a <code>PKIXCertPathChecker</code> to add
     * to the list of checks. If <code>null</code>, the checker is
     * ignored (not added to list).
     **/
    public void addCertPathChecker( PKIXCertPathChecker checker )
    {
    if ( checker != null )
    {
        certPathCheckers.add( checker.clone() );
    }
    }

    /**
     * Returns the signature provider's name, or <code>null</code> if not set.
     *
     * @return the signature provider's name (or <code>null</code>)
     *
     * @see #setSigProvider(java.lang.String)
     **/
    public String getSigProvider()
    {
    return sigProvider;
    }

    /**
     * Sets the signature provider's name. The specified provider
     * will be preferred when creating Signature objects. If null
     * or not set, the first provider found supporting the
     * algorithm will be used.
     *
     * @param sigProvider the signature provider's name (or <code>null</code>)
     *
     * @see #getSigProvider()
     **/
    public void setSigProvider(String sigProvider)
    {
    this.sigProvider = sigProvider;
    }

    /**
     * Returns the required constraints on the target
     * certificate. The constraints are returned as an instance of
     * CertSelector. If <code>null</code>, no constraints are defined.<br />
     * <br />
     * Note that the CertSelector returned is cloned to protect
     * against subsequent modifications.
     *
     * @return a CertSelector specifying the constraints on the target certificate (or <code>null</code>)
     *
     * @see #setTargetCertConstraints(java.security.cert.CertSelector)
     **/
    public CertSelector getTargetCertConstraints()
    {
    if ( certSelector == null )
        return null;

    return (CertSelector)certSelector.clone();
    }

    /**
     * Sets the required constraints on the target
     * certificate. The constraints are specified as an instance
     * of CertSelector. If null, no constraints are defined.<br />
     * <br />
     * Note that the CertSelector specified is cloned to protect
     * against subsequent modifications.
     *
     * @param selector  a CertSelector specifying the constraints
     * on the target certificate (or <code>null</code>)
     *
     * @see #getTargetCertConstraints()
     **/
    public void setTargetCertConstraints(CertSelector selector)
    {
    if ( selector == null )
        certSelector = null;
    else
        certSelector = (CertSelector)selector.clone();
    }

    /**
     * Makes a copy of this PKIXParameters object. Changes to the
     * copy will not affect the original and vice versa.
     *
     * @return a copy of this <code>PKIXParameters</code> object
     **/
    public Object clone()
    {
    try {
        PKIXParameters obj = (PKIXParameters)super.clone();
        obj.certStores = new ArrayList( certStores );
        Iterator iter = certPathCheckers.iterator();
        obj.certPathCheckers = new ArrayList();
        while ( iter.hasNext() )
        {
        obj.certPathCheckers.add( ((PKIXCertPathChecker)iter.next()).clone() );
        }
        if ( initialPolicies != null )
        {
        obj.initialPolicies = new HashSet( initialPolicies );
        }
        if ( trustAnchors != null )
        {
        obj.trustAnchors = new HashSet( trustAnchors );
        }
        if ( certSelector != null )
        {
        obj.certSelector = (CertSelector)certSelector.clone();
        }
        return obj;
    } catch ( CloneNotSupportedException ex ) {
        throw new InternalError();
    }
    }

    /**
     * Returns a formatted string describing the parameters.
     *
     * @return a formatted string describing the parameters.
     **/
    public String toString()
    {
    StringBuffer s = new StringBuffer();
    s.append("[\n");
    if ( trustAnchors != null )
    {
        s.append("  Trust Anchors: ").append(trustAnchors).append('\n');
    }
    if ( initialPolicies != null )
    {
        if ( initialPolicies.isEmpty() )
        {
        s.append("  Initial Policy OIDs: any\n" );
        }
        else
        {
        s.append("  Initial Policy OIDs: [").append(initialPolicies).append("]\n");
        }
    }
    s.append("  Validity Date: ");
    if ( date != null )
        s.append(date);
    else
        s.append("null");
    s.append('\n');

    s.append("  Signature Provider: ");
    if ( sigProvider != null )
        s.append(sigProvider);
    else
        s.append("null");
    s.append('\n');

    s.append("  Default Revocation Enabled: ");
    s.append(revocationEnabled);
    s.append('\n' );

    s.append("  Explicit Policy Required: ");
    s.append(explicitPolicyRequired);
    s.append('\n');

    s.append("  Policy Mapping Inhibited: ");
    s.append(policyMappingInhibited);
    s.append('\n');

    s.append("  Any Policy Inhibited: ");
    s.append(anyPolicyInhibited);
    s.append('\n');

    s.append("  Policy Qualifiers Rejected: ");
    s.append(policyQualifiersRejected);
    s.append('\n');

    s.append("  Target Cert Constraints: ");
    s.append(certSelector);
    s.append('\n');

    s.append("  Certification Path Checkers: [");
    s.append(certPathCheckers);
    s.append( "}\n");

    s.append("  CertStores: [");
    s.append(certStores);
    s.append("}\n");

    s.append("]\n");

    return s.toString();
    }
}
