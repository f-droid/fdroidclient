package java.security.cert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CRL;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1Object;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.DERInteger;
import org.spongycastle.asn1.x509.X509Extensions;
import org.spongycastle.asn1.x509.X509Name;
import org.spongycastle.jce.PrincipalUtil;

/**
 * A <code>CRLSelector</code> that selects <code>X509CRLs</code> that match
 * all specified criteria. This class is particularly useful when selecting CRLs
 * from a <code>CertStore</code> to check revocation status of a particular
 * certificate.<br />
 * <br />
 * When first constructed, an <code>X509CRLSelector</code> has no criteria
 * enabled and each of the <code>get</code> methods return a default value (<code>null</code>).
 * Therefore, the {@link #match match} method would return <code>true</code>
 * for any <code>X509CRL</code>. Typically, several criteria are enabled (by
 * calling {@link #setIssuerNames setIssuerNames} or
 * {@link #setDateAndTime setDateAndTime}, for instance) and then the
 * <code>X509CRLSelector</code> is passed to
 * {@link CertStore#getCRLs CertStore.getCRLs} or some similar method.<br />
 * <br />
 * Please refer to RFC 2459 for definitions of the X.509 CRL fields and
 * extensions mentioned below.<br />
 * <br />
 * <b>Concurrent Access</b><br />
 * <br />
 * Unless otherwise specified, the methods defined in this class are not
 * thread-safe. Multiple threads that need to access a single object
 * concurrently should synchronize amongst themselves and provide the necessary
 * locking. Multiple threads each manipulating separate objects need not
 * synchronize.<br />
 * <br />
 * Uses {@link org.spongycastle.asn1.ASN1InputStream ASN1InputStream},
 * {@link org.spongycastle.asn1.ASN1Sequence ASN1Sequence},
 * {@link org.spongycastle.asn1.ASN1ObjectIdentifier ASN1ObjectIdentifier},
 * {@link org.spongycastle.asn1.DEROutputStream DEROutputStream},
 * {@link org.spongycastle.asn1.ASN1Object ASN1Object},
 * {@link org.spongycastle.asn1.x509.X509Name X509Name}
 * 
 * @see CRLSelector
 * @see X509CRL
 */
public class X509CRLSelector implements CRLSelector
{
    private Set issuerNames = null;

    private Set issuerNamesX509 = null;

    private BigInteger minCRL = null;

    private BigInteger maxCRL = null;

    private Date dateAndTime = null;

    private X509Certificate certChecking = null;

    /**
     * Creates an <code>X509CRLSelector</code>. Initially, no criteria are
     * set so any <code>X509CRL</code> will match.
     */
    public X509CRLSelector()
    {
    }

    /**
     * Sets the issuerNames criterion. The issuer distinguished name in the
     * <code>X509CRL</code> must match at least one of the specified
     * distinguished names. If <code>null</code>, any issuer distinguished
     * name will do.<br />
     * <br />
     * This method allows the caller to specify, with a single method call, the
     * complete set of issuer names which <code>X509CRLs</code> may contain.
     * The specified value replaces the previous value for the issuerNames
     * criterion.<br />
     * <br />
     * The <code>names</code> parameter (if not <code>null</code>) is a
     * <code>Collection</code> of names. Each name is a <code>String</code>
     * or a byte array representing a distinguished name (in RFC 2253 or ASN.1
     * DER encoded form, respectively). If <code>null</code> is supplied as
     * the value for this argument, no issuerNames check will be performed.<br />
     * <br />
     * Note that the <code>names</code> parameter can contain duplicate
     * distinguished names, but they may be removed from the
     * <code>Collection</code> of names returned by the
     * {@link #getIssuerNames getIssuerNames} method.<br />
     * <br />
     * If a name is specified as a byte array, it should contain a single DER
     * encoded distinguished name, as defined in X.501. The ASN.1 notation for
     * this structure is as follows.
     * 
     * <pre><code>
     *  Name ::= CHOICE {
     *    RDNSequence }
     * 
     *  RDNSequence ::= SEQUENCE OF RDN
     * 
     *  RDN ::=
     *    SET SIZE (1 .. MAX) OF AttributeTypeAndValue
     * 
     *  AttributeTypeAndValue ::= SEQUENCE {
     *    type     AttributeType,
     *    value    AttributeValue }
     * 
     *  AttributeType ::= OBJECT IDENTIFIER
     * 
     *  AttributeValue ::= ANY DEFINED BY AttributeType
     *  ....
     *  DirectoryString ::= CHOICE {
     *        teletexString           TeletexString (SIZE (1..MAX)),
     *        printableString         PrintableString (SIZE (1..MAX)),
     *        universalString         UniversalString (SIZE (1..MAX)),
     *        utf8String              UTF8String (SIZE (1.. MAX)),
     *        bmpString               BMPString (SIZE (1..MAX)) }
     * </code></pre>
     * 
     * <br />
     * <br />
     * Note that a deep copy is performed on the <code>Collection</code> to
     * protect against subsequent modifications.
     * 
     * @param names
     *            a <code>Collection</code> of names (or <code>null</code>)
     * 
     * @exception IOException
     *                if a parsing error occurs
     * 
     * @see #getIssuerNames
     */
    public void setIssuerNames(Collection names) throws IOException
    {
        if (names == null || names.isEmpty())
        {
            issuerNames = null;
            issuerNamesX509 = null;
        }
        else
        {
            Object item;
            Iterator iter = names.iterator();
            while (iter.hasNext())
            {
                item = iter.next();
                if (item instanceof String)
                {
                    addIssuerName((String)item);
                }
                else if (item instanceof byte[])
                {
                    addIssuerName((byte[])item);
                }
                else
                {
                    throw new IOException("name not byte[]or String: "
                            + item.toString());
                }
            }
        }
    }

    /**
     * Adds a name to the issuerNames criterion. The issuer distinguished name
     * in the <code>X509CRL</code> must match at least one of the specified
     * distinguished names.<br />
     * <br />
     * This method allows the caller to add a name to the set of issuer names
     * which <code>X509CRLs</code> may contain. The specified name is added to
     * any previous value for the issuerNames criterion. If the specified name
     * is a duplicate, it may be ignored.<br />
     * <br />
     * Uses {@link org.spongycastle.asn1.x509.X509Name X509Name} for parsing the
     * name
     * 
     * @param name
     *            the name in RFC 2253 form
     * 
     * @exception IOException
     *                if a parsing error occurs
     */
    public void addIssuerName(String name) throws IOException
    {
        if (issuerNames == null)
        {
            issuerNames = new HashSet();
            issuerNamesX509 = new HashSet();
        }
        X509Name nameX509;
        try
        {
            nameX509 = new X509Name(name);
        }
        catch (IllegalArgumentException ex)
        {
            throw new IOException(ex.getMessage());
        }
        issuerNamesX509.add(nameX509);
        issuerNames.add(name);
    }

    /**
     * Adds a name to the issuerNames criterion. The issuer distinguished name
     * in the <code>X509CRL</code> must match at least one of the specified
     * distinguished names.<br />
     * <br />
     * This method allows the caller to add a name to the set of issuer names
     * which <code>X509CRLs</code> may contain. The specified name is added to
     * any previous value for the issuerNames criterion. If the specified name
     * is a duplicate, it may be ignored. If a name is specified as a byte
     * array, it should contain a single DER encoded distinguished name, as
     * defined in X.501. The ASN.1 notation for this structure is as follows.<br />
     * <br />
     * The name is provided as a byte array. This byte array should contain a
     * single DER encoded distinguished name, as defined in X.501. The ASN.1
     * notation for this structure appears in the documentation for
     * {@link #setIssuerNames setIssuerNames(Collection names)}.<br />
     * <br />
     * Note that the byte array supplied here is cloned to protect against
     * subsequent modifications.<br />
     * <br />
     * Uses {@link org.spongycastle.asn1.x509.X509Name X509Name} for parsing the
     * name, {@link org.spongycastle.asn1.ASN1InputStream ASN1InputStream},
     * {@link org.spongycastle.asn1.ASN1Object ASN1Object} and
     * {@link org.spongycastle.asn1.ASN1Sequence ASN1Sequence}
     * 
     * @param name
     *            a byte array containing the name in ASN.1 DER encoded form
     * 
     * @exception IOException
     *                if a parsing error occurs
     */
    public void addIssuerName(byte[] name) throws IOException
    {
        if (issuerNames == null)
        {
            issuerNames = new HashSet();
            issuerNamesX509 = new HashSet();
        }

        ByteArrayInputStream inStream = new ByteArrayInputStream(name);
        ASN1InputStream derInStream = new ASN1InputStream(inStream);
        ASN1Object obj = derInStream.readObject();
        if (obj instanceof ASN1Sequence)
        {
            issuerNamesX509.add(new X509Name((ASN1Sequence)obj));
        }
        else
        {
            throw new IOException("parsing error");
        }
        issuerNames.add(name.clone());
    }

    /**
     * Sets the minCRLNumber criterion. The <code>X509CRL</code> must have a
     * CRL number extension whose value is greater than or equal to the
     * specified value. If <code>null</code>, no minCRLNumber check will be
     * done.
     * 
     * @param minCRL
     *            the minimum CRL number accepted (or <code>null</code>)
     */
    public void setMinCRLNumber(BigInteger minCRL)
    {
        this.minCRL = minCRL;
    }

    /**
     * Sets the maxCRLNumber criterion. The <code>X509CRL</code> must have a
     * CRL number extension whose value is less than or equal to the specified
     * value. If <code>null</code>, no maxCRLNumber check will be done.
     * 
     * @param maxCRL
     *            the maximum CRL number accepted (or <code>null</code>)
     */
    public void setMaxCRLNumber(BigInteger maxCRL)
    {
        this.maxCRL = maxCRL;
    }

    /**
     * Sets the dateAndTime criterion. The specified date must be equal to or
     * later than the value of the thisUpdate component of the
     * <code>X509CRL</code> and earlier than the value of the nextUpdate
     * component. There is no match if the <code>X509CRL</code> does not
     * contain a nextUpdate component. If <code>null</code>, no dateAndTime
     * check will be done.<br />
     * <br />
     * Note that the <code>Date</code> supplied here is cloned to protect
     * against subsequent modifications.
     * 
     * @param dateAndTime
     *            the <code>Date</code> to match against (or <code>null</code>)
     * 
     * @see #getDateAndTime
     */
    public void setDateAndTime(Date dateAndTime)
    {
        if (dateAndTime == null)
        {
            this.dateAndTime = null;
        }
        else
        {
            this.dateAndTime = new Date(dateAndTime.getTime());
        }
    }

    /**
     * Sets the certificate being checked. This is not a criterion. Rather, it
     * is optional information that may help a <code>CertStore</code> find
     * CRLs that would be relevant when checking revocation for the specified
     * certificate. If <code>null</code> is specified, then no such optional
     * information is provided.
     * 
     * @param cert
     *            the <code>X509Certificate</code> being checked (or
     *            <code>null</code>)
     * 
     * @see #getCertificateChecking
     */
    public void setCertificateChecking(X509Certificate cert)
    {
        certChecking = cert;
    }

    /**
     * Returns a copy of the issuerNames criterion. The issuer distinguished
     * name in the <code>X509CRL</code> must match at least one of the
     * specified distinguished names. If the value returned is <code>null</code>,
     * any issuer distinguished name will do.<br />
     * <br />
     * If the value returned is not <code>null</code>, it is a
     * <code>Collection</code> of names. Each name is a <code>String</code>
     * or a byte array representing a distinguished name (in RFC 2253 or ASN.1
     * DER encoded form, respectively). Note that the <code>Collection</code>
     * returned may contain duplicate names.<br />
     * <br />
     * If a name is specified as a byte array, it should contain a single DER
     * encoded distinguished name, as defined in X.501. The ASN.1 notation for
     * this structure is given in the documentation for
     * {@link #setIssuerNames setIssuerNames(Collection names)}.<br />
     * <br />
     * Note that a deep copy is performed on the <code>Collection</code> to
     * protect against subsequent modifications.
     * 
     * @return a <code>Collection</code> of names (or <code>null</code>)
     * @see #setIssuerNames
     */
    public Collection getIssuerNames()
    {
        if (issuerNames == null)
        {
            return null;
        }

        Collection set = new HashSet();
        Iterator iter = issuerNames.iterator();
        Object item;
        while (iter.hasNext())
        {
            item = iter.next();
            if (item instanceof String)
            {
                set.add(new String((String)item));
            }
            else if (item instanceof byte[])
            {
                set.add(((byte[])item).clone());
            }
        }
        return set;
    }

    /**
     * Returns the minCRLNumber criterion. The <code>X509CRL</code> must have
     * a CRL number extension whose value is greater than or equal to the
     * specified value. If <code>null</code>, no minCRLNumber check will be
     * done.
     * 
     * @return the minimum CRL number accepted (or <code>null</code>)
     */
    public BigInteger getMinCRL()
    {
        return minCRL;
    }

    /**
     * Returns the maxCRLNumber criterion. The <code>X509CRL</code> must have
     * a CRL number extension whose value is less than or equal to the specified
     * value. If <code>null</code>, no maxCRLNumber check will be done.
     * 
     * @return the maximum CRL number accepted (or <code>null</code>)
     */
    public BigInteger getMaxCRL()
    {
        return maxCRL;
    }

    /**
     * Returns the dateAndTime criterion. The specified date must be equal to or
     * later than the value of the thisUpdate component of the
     * <code>X509CRL</code> and earlier than the value of the nextUpdate
     * component. There is no match if the <code>X509CRL</code> does not
     * contain a nextUpdate component. If <code>null</code>, no dateAndTime
     * check will be done.<br />
     * <br />
     * Note that the <code>Date</code> returned is cloned to protect against
     * subsequent modifications.
     * 
     * @return the <code>Date</code> to match against (or <code>null</code>)
     * 
     * @see #setDateAndTime
     */
    public Date getDateAndTime()
    {
        if (dateAndTime == null)
        {
            return null;
        }

        return new Date(dateAndTime.getTime());
    }

    /**
     * Returns the certificate being checked. This is not a criterion. Rather,
     * it is optional information that may help a <code>CertStore</code> find
     * CRLs that would be relevant when checking revocation for the specified
     * certificate. If the value returned is <code>null</code>, then no such
     * optional information is provided.
     * 
     * @return the certificate being checked (or <code>null</code>)
     * 
     * @see #setCertificateChecking
     */
    public X509Certificate getCertificateChecking()
    {
        return certChecking;
    }

    /**
     * Returns a printable representation of the <code>X509CRLSelector</code>.<br />
     * <br />
     * Uses
     * {@link org.spongycastle.asn1.x509.X509Name#toString X509Name.toString} to
     * format the output
     * 
     * @return a <code>String</code> describing the contents of the
     *         <code>X509CRLSelector</code>.
     */
    public String toString()
    {
        StringBuffer s = new StringBuffer();
        s.append("X509CRLSelector: [\n");
        if (issuerNamesX509 != null)
        {
            s.append("  IssuerNames:\n");
            Iterator iter = issuerNamesX509.iterator();
            while (iter.hasNext())
            {
                s.append("    ").append(iter.next()).append('\n');
            }
        }
        if (minCRL != null)
        {
            s.append("  minCRLNumber: ").append(minCRL).append('\n');
        }
        if (maxCRL != null)
        {
            s.append("  maxCRLNumber: ").append(maxCRL).append('\n');
        }
        if (dateAndTime != null)
        {
            s.append("  dateAndTime: ").append(dateAndTime).append('\n');
        }
        if (certChecking != null)
        {
            s.append("  Certificate being checked: ").append(certChecking).append('\n');
        }
        s.append(']');
        return s.toString();
    }

    /**
     * Decides whether a <code>CRL</code> should be selected.<br />
     * <br />
     * Uses
     * {@link org.spongycastle.asn1.x509.X509Name#toString X509Name.toString} to
     * parse and to compare the crl parameter issuer and
     * {@link org.spongycastle.asn1.x509.X509Extensions#CRLNumber CRLNumber} to
     * access the CRL number extension.
     * 
     * @param crl
     *            the <code>CRL</code> to be checked
     * 
     * @return <code>true</code> if the <code>CRL</code> should be selected,
     *         <code>false</code> otherwise
     */
    public boolean match(CRL crl)
    {
        if (!(crl instanceof X509CRL))
        {
            return false;
        }

        X509CRL crlX509 = (X509CRL)crl;
        boolean test;

        if (issuerNamesX509 != null)
        {
            Iterator iter = issuerNamesX509.iterator();
            test = false;
            X509Name crlIssuer = null;
            try
            {
                crlIssuer = PrincipalUtil.getIssuerX509Principal(crlX509);
            }
            catch (Exception ex)
            {

                return false;
            }

            while (iter.hasNext())
            {
                if (crlIssuer.equals(iter.next(), true))
                {
                    test = true;
                    break;
                }
            }
            if (!test)
            {
                return false;
            }
        }

        byte[] data = crlX509.getExtensionValue(X509Extensions.CRLNumber
                .getId());
        if (data != null)
        {
            try
            {
                ByteArrayInputStream inStream = new ByteArrayInputStream(data);
                ASN1InputStream derInputStream = new ASN1InputStream(inStream);
                inStream = new ByteArrayInputStream(
                        ((ASN1OctetString)derInputStream.readObject())
                                .getOctets());
                derInputStream = new ASN1InputStream(inStream);
                BigInteger crlNumber = ((DERInteger)derInputStream.readObject())
                        .getPositiveValue();
                if (minCRL != null && minCRL.compareTo(crlNumber) > 0)
                {
                    return false;
                }
                if (maxCRL != null && maxCRL.compareTo(crlNumber) < 0)
                {
                    return false;
                }
            }
            catch (IOException ex)
            {
                return false;
            }
        }
        else if (minCRL != null || maxCRL != null)
        {
            return false;
        }

        if (dateAndTime != null)
        {
            Date check = crlX509.getThisUpdate();
            if (check == null)
            {
                return false;
            }
            else if (dateAndTime.before(check))
            {
                return false;
            }

            check = crlX509.getNextUpdate();
            if (check == null)
            {
                return false;
            }
            else if (!dateAndTime.before(check))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a copy of this object.
     * 
     * @return the copy
     */
    public Object clone()
    {
        try
        {
            X509CRLSelector copy = (X509CRLSelector)super.clone();
            if (issuerNames != null)
            {
                copy.issuerNames = new HashSet();
                Iterator iter = issuerNames.iterator();
                Object obj;
                while (iter.hasNext())
                {
                    obj = iter.next();
                    if (obj instanceof byte[])
                    {
                        copy.issuerNames.add(((byte[])obj).clone());
                    }
                    else
                    {
                        copy.issuerNames.add(obj);
                    }
                }
                copy.issuerNamesX509 = new HashSet(issuerNamesX509);
            }
            return copy;
        }
        catch (CloneNotSupportedException e)
        {
            /* Cannot happen */
            throw new InternalError(e.toString());
        }
    }

    /**
     * Decides whether a <code>CRL</code> should be selected.
     * 
     * @param crl
     *            the <code>CRL</code> to be checked
     * 
     * @return <code>true</code> if the <code>CRL</code> should be selected,
     *         <code>false</code> otherwise
     */
    public boolean equals(Object obj)
    {
        if (!(obj instanceof X509CRLSelector))
        {
            return false;
        }

        X509CRLSelector equalsCRL = (X509CRLSelector)obj;

        if (!equals(dateAndTime, equalsCRL.dateAndTime))
        {
            return false;
        }

        if (!equals(minCRL, equalsCRL.minCRL))
        {
            return false;
        }

        if (!equals(maxCRL, equalsCRL.maxCRL))
        {
            return false;
        }

        if (!equals(issuerNamesX509, equalsCRL.issuerNamesX509))
        {
            return false;
        }

        if (!equals(certChecking, equalsCRL.certChecking))
        {
            return false;
        }

        return true;
    }

    /**
     * Return <code>true</code> if two Objects are unequal.
     * This means that one is <code>null</code> and the other is
     * not or <code>obj1.equals(obj2)</code> returns
     * <code>false</code>.
     **/
    private boolean equals(Object obj1, Object obj2)
    {
        if (obj1 == null)
        {
            if (obj2 != null)
            {
                return true;
            }
        }
        else if (!obj1.equals(obj2))
        {
            return true;
        }
        return false;
    }    
}
