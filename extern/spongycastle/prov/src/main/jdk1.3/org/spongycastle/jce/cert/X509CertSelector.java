package org.spongycastle.jce.cert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1Object;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.ASN1TaggedObject;
import org.spongycastle.asn1.DERGeneralizedTime;
import org.spongycastle.asn1.DEROutputStream;
import org.spongycastle.asn1.util.ASN1Dump;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.ExtendedKeyUsage;
import org.spongycastle.asn1.x509.KeyPurposeId;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.asn1.x509.X509Extensions;
import org.spongycastle.asn1.x509.X509Name;
import org.spongycastle.jce.PrincipalUtil;
import org.spongycastle.util.Integers;

/**
 * A <code>CertSelector</code> that selects
 * <code>X509Certificates that match all
 * specified criteria. This class is particularly useful when
 * selecting certificates from a CertStore to build a PKIX-compliant
 * certification path.<br />
 * <br />
 * When first constructed, an <code>X509CertSelector</code> has no criteria enabled
 * and each of the get methods return a default value (<code>null</code>, or -1 for
 * the {@link #getBasicConstraints} method). Therefore, the {@link #match} method would
 * return true for any <code>X509Certificate</code>. Typically, several criteria
 * are enabled (by calling {@link #setIssuer} or {@link #setKeyUsage}, for instance) and
 * then the <code>X509CertSelector</code> is passed to {@link CertStore#getCertificates} or
 * some similar method.<br />
 * <br />
 * Several criteria can be enabled (by calling {@link #setIssuer} and
 * {@link #setSerialNumber}, for example) such that the match method usually
 * uniquely matches a single <code>X509Certificate</code>. We say usually, since it
 * is possible for two issuing CAs to have the same distinguished name
 * and each issue a certificate with the same serial number. Other
 * unique combinations include the issuer, subject,
 * subjectKeyIdentifier and/or the subjectPublicKey criteria.<br />
 * <br />
 * Please refer to RFC 2459 for definitions of the X.509 certificate
 * extensions mentioned below.<br />
 * <br />
 * <b>Concurrent Access</b><br />
 * <br />
 * Unless otherwise specified, the methods defined in this class are
 * not thread-safe. Multiple threads that need to access a single
 * object concurrently should synchronize amongst themselves and
 * provide the necessary locking. Multiple threads each manipulating
 * separate objects need not synchronize.<br />
 * <br />
 * <b>TODO: implement name constraints</b>
 * <b>TODO: implement match check for path to names</b><br />
 * <br />
 * Uses {@link org.spongycastle.asn1.ASN1InputStream ASN1InputStream},
 * {@link org.spongycastle.asn1.ASN1Sequence ASN1Sequence},
 * {@link org.spongycastle.asn1.ASN1ObjectIdentifier ASN1ObjectIdentifier},
 * {@link org.spongycastle.asn1.DEROutputStream DEROutputStream},
 * {@link org.spongycastle.asn1.ASN1Object ASN1Object},
 * {@link org.spongycastle.asn1.OIDTokenizer OIDTokenizer},
 * {@link org.spongycastle.asn1.x509.X509Name X509Name},
 * {@link org.spongycastle.asn1.x509.X509Extensions X509Extensions},
 * {@link org.spongycastle.asn1.x509.ExtendedKeyUsage ExtendedKeyUsage},
 * {@link org.spongycastle.asn1.x509.KeyPurposeId KeyPurposeId},
 * {@link org.spongycastle.asn1.x509.SubjectPublicKeyInfo SubjectPublicKeyInfo},
 * {@link org.spongycastle.asn1.x509.AlgorithmIdentifier AlgorithmIdentifier}
 */
public class X509CertSelector implements CertSelector
{
    private static final Hashtable keyPurposeIdMap = new Hashtable();
    static
    {
        keyPurposeIdMap.put(KeyPurposeId.id_kp_serverAuth.getId(),
                KeyPurposeId.id_kp_serverAuth);
        keyPurposeIdMap.put(KeyPurposeId.id_kp_clientAuth.getId(),
                KeyPurposeId.id_kp_clientAuth);
        keyPurposeIdMap.put(KeyPurposeId.id_kp_codeSigning.getId(),
                KeyPurposeId.id_kp_codeSigning);
        keyPurposeIdMap.put(KeyPurposeId.id_kp_emailProtection.getId(),
                KeyPurposeId.id_kp_emailProtection);
        keyPurposeIdMap.put(KeyPurposeId.id_kp_ipsecEndSystem.getId(),
                KeyPurposeId.id_kp_ipsecEndSystem);
        keyPurposeIdMap.put(KeyPurposeId.id_kp_ipsecTunnel.getId(),
                KeyPurposeId.id_kp_ipsecTunnel);
        keyPurposeIdMap.put(KeyPurposeId.id_kp_ipsecUser.getId(),
                KeyPurposeId.id_kp_ipsecUser);
        keyPurposeIdMap.put(KeyPurposeId.id_kp_timeStamping.getId(),
                KeyPurposeId.id_kp_timeStamping);
    }

    private X509Certificate x509Cert = null;

    private BigInteger serialNumber = null;

    private Object issuerDN = null;

    private X509Name issuerDNX509 = null;

    private Object subjectDN = null;

    private X509Name subjectDNX509 = null;

    private byte[] subjectKeyID = null;

    private byte[] authorityKeyID = null;

    private Date certValid = null;

    private Date privateKeyValid = null;

    private ASN1ObjectIdentifier subjectKeyAlgID = null;

    private PublicKey subjectPublicKey = null;

    private byte[] subjectPublicKeyByte = null;

    private boolean[] keyUsage = null;

    private Set keyPurposeSet = null;

    private boolean matchAllSubjectAltNames = true;

    private Set subjectAltNames = null;

    private Set subjectAltNamesByte = null;

    private int minMaxPathLen = -1;

    private Set policy = null;

    private Set policyOID = null;

    private Set pathToNames = null;

    private Set pathToNamesByte = null;

    /**
     * Creates an <code>X509CertSelector</code>. Initially, no criteria are
     * set so any <code>X509Certificate</code> will match.
     */
    public X509CertSelector()
    {
    }

    /**
     * Sets the certificateEquals criterion. The specified
     * <code>X509Certificate</code> must be equal to the
     * <code>X509Certificate</code> passed to the match method. If
     * <code>null</code>, then this check is not applied.<br />
     * <br />
     * This method is particularly useful when it is necessary to match a single
     * certificate. Although other criteria can be specified in conjunction with
     * the certificateEquals criterion, it is usually not practical or
     * necessary.
     * 
     * @param cert
     *            the X509Certificate to match (or <code>null</code>)
     * 
     * @see #getCertificate()
     */
    public void setCertificate(X509Certificate cert)
    {
        x509Cert = cert;
    }

    /**
     * Sets the serialNumber criterion. The specified serial number must match
     * the certificate serial number in the <code>X509Certificate</code>. If
     * <code>null</code>, any certificate serial number will do.
     * 
     * @param serial
     *            the certificate serial number to match (or <code>null</code>)
     * 
     * @see #getSerialNumber()
     */
    public void setSerialNumber(BigInteger serial)
    {
        serialNumber = serial;
    }

    /**
     * Sets the issuer criterion. The specified distinguished name must match
     * the issuer distinguished name in the <code>X509Certificate</code>. If
     * <code>null</code>, any issuer distinguished name will do.<br />
     * <br />
     * If <code>issuerDN</code> is not <code>null</code>, it should contain
     * a distinguished name, in RFC 2253 format.<br />
     * <br />
     * Uses {@link org.spongycastle.asn1.x509.X509Name X509Name} for parsing the
     * issuerDN.
     * 
     * @param issuerDN
     *            a distinguished name in RFC 2253 format (or <code>null</code>)
     * 
     * @exception IOException
     *                if a parsing error occurs (incorrect form for DN)
     */
    public void setIssuer(String issuerDN) throws IOException
    {
        if (issuerDN == null)
        {
            this.issuerDN = null;
            this.issuerDNX509 = null;
        }
        else
        {
            X509Name nameX509;
            try
            {
                nameX509 = new X509Name(issuerDN);
            }
            catch (IllegalArgumentException ex)
            {
                throw new IOException(ex.getMessage());
            }
            this.issuerDNX509 = nameX509;
            this.issuerDN = issuerDN;
        }
    }

    /**
     * Sets the issuer criterion. The specified distinguished name must match
     * the issuer distinguished name in the <code>X509Certificate</code>. If
     * null is specified, the issuer criterion is disabled and any issuer
     * distinguished name will do.<br />
     * <br />
     * If <code>issuerDN</code> is not <code>null</code>, it should contain
     * a single DER encoded distinguished name, as defined in X.501. The ASN.1
     * notation for this structure is as follows.<br />
     * <br />
     * 
     * <pre>
     *    Name ::= CHOICE {
     *      RDNSequence }
     * 
     *    RDNSequence ::= SEQUENCE OF RDN
     * 
     *    RDN ::=
     *      SET SIZE (1 .. MAX) OF AttributeTypeAndValue
     * 
     *    AttributeTypeAndValue ::= SEQUENCE {
     *      type     AttributeType,
     *      value    AttributeValue }
     * 
     *    AttributeType ::= OBJECT IDENTIFIER
     * 
     *    AttributeValue ::= ANY DEFINED BY AttributeType
     *    ....
     *    DirectoryString ::= CHOICE {
     *      teletexString           TeletexString (SIZE (1..MAX)),
     *      printableString         PrintableString (SIZE (1..MAX)),
     *      universalString         UniversalString (SIZE (1..MAX)),
     *      utf8String              UTF8String (SIZE (1.. MAX)),
     *      bmpString               BMPString (SIZE (1..MAX)) }
     * </pre>
     * 
     * <br />
     * <br />
     * Note that the byte array specified here is cloned to protect against
     * subsequent modifications.<br />
     * <br />
     * Uses {@link org.spongycastle.asn1.ASN1InputStream ASN1InputStream},
     * {@link org.spongycastle.asn1.ASN1Object ASN1Object},
     * {@link org.spongycastle.asn1.ASN1Sequence ASN1Sequence},
     * {@link org.spongycastle.asn1.x509.X509Name X509Name}
     * 
     * @param issuerDN -
     *            a byte array containing the distinguished name in ASN.1 DER
     *            encoded form (or <code>null</code>)
     * 
     * @exception IOException
     *                if an encoding error occurs (incorrect form for DN)
     */
    public void setIssuer(byte[] issuerDN) throws IOException
    {
        if (issuerDN == null)
        {
            this.issuerDN = null;
            this.issuerDNX509 = null;
        }
        else
        {
            ByteArrayInputStream inStream = new ByteArrayInputStream(issuerDN);
            ASN1InputStream derInStream = new ASN1InputStream(inStream);
            ASN1Object obj = derInStream.readObject();
            if (obj instanceof ASN1Sequence)
            {
                this.issuerDNX509 = new X509Name((ASN1Sequence)obj);
            }
            else
            {
                throw new IOException("parsing error");
            }
            this.issuerDN = (byte[])issuerDN.clone();
        }
    }

    /**
     * Sets the subject criterion. The specified distinguished name must match
     * the subject distinguished name in the <code>X509Certificate</code>. If
     * null, any subject distinguished name will do.<br />
     * <br />
     * If <code>subjectDN</code> is not <code>null</code>, it should
     * contain a distinguished name, in RFC 2253 format.<br />
     * <br />
     * Uses {@link org.spongycastle.asn1.x509.X509Name X509Name} for parsing the
     * subjectDN.
     * 
     * @param subjectDN
     *            a distinguished name in RFC 2253 format (or <code>null</code>)
     * 
     * @exception IOException
     *                if a parsing error occurs (incorrect form for DN)
     */
    public void setSubject(String subjectDN) throws IOException
    {
        if (subjectDN == null)
        {
            this.subjectDN = null;
            this.subjectDNX509 = null;
        }
        else
        {
            X509Name nameX509;
            try
            {
                nameX509 = new X509Name(subjectDN);
            }
            catch (IllegalArgumentException ex)
            {
                throw new IOException(ex.getMessage());
            }

            this.subjectDNX509 = nameX509;
            this.subjectDN = subjectDN;
        }
    }

    /**
     * Sets the subject criterion. The specified distinguished name must match
     * the subject distinguished name in the <code>X509Certificate</code>. If
     * null, any subject distinguished name will do.<br />
     * <br />
     * If <code>subjectDN</code> is not <code>null</code>, it should
     * contain a single DER encoded distinguished name, as defined in X.501. For
     * the ASN.1 notation for this structure, see
     * {@link #setIssuer(byte []) setIssuer(byte [] issuerDN)}.<br />
     * <br />
     * Uses {@link org.spongycastle.asn1.ASN1InputStream ASN1InputStream},
     * {@link org.spongycastle.asn1.ASN1Object ASN1Object},
     * {@link org.spongycastle.asn1.ASN1Sequence ASN1Sequence},
     * {@link org.spongycastle.asn1.x509.X509Name X509Name}
     * 
     * @param subjectDN
     *            a byte array containing the distinguished name in ASN.1 DER
     *            format (or <code>null</code>)
     * 
     * @exception IOException
     *                if an encoding error occurs (incorrect form for DN)
     */
    public void setSubject(byte[] subjectDN) throws IOException
    {
        if (subjectDN == null)
        {
            this.subjectDN = null;
            this.subjectDNX509 = null;
        }
        else
        {
            ByteArrayInputStream inStream = new ByteArrayInputStream(subjectDN);
            ASN1InputStream derInStream = new ASN1InputStream(inStream);
            ASN1Object obj = derInStream.readObject();

            if (obj instanceof ASN1Sequence)
            {
                this.subjectDNX509 = new X509Name((ASN1Sequence)obj);
            }
            else
            {
                throw new IOException("parsing error");
            }
            this.subjectDN = (byte[])subjectDN.clone();
        }
    }

    /**
     * Sets the subjectKeyIdentifier criterion. The <code>X509Certificate</code>
     * must contain a SubjectKeyIdentifier extension for which the contents of
     * the extension matches the specified criterion value. If the criterion
     * value is null, no subjectKeyIdentifier check will be done.<br />
     * <br />
     * If <code>subjectKeyID</code> is not <code>null</code>, it should
     * contain a single DER encoded value corresponding to the contents of the
     * extension value (not including the object identifier, criticality
     * setting, and encapsulating OCTET STRING) for a SubjectKeyIdentifier
     * extension. The ASN.1 notation for this structure follows.<br />
     * <br />
     * 
     * <pre>
     *    SubjectKeyIdentifier ::= KeyIdentifier
     * 
     *    KeyIdentifier ::= OCTET STRING
     * </pre>
     * 
     * <br />
     * <br />
     * Since the format of subject key identifiers is not mandated by any
     * standard, subject key identifiers are not parsed by the
     * <code>X509CertSelector</code>. Instead, the values are compared using
     * a byte-by-byte comparison.<br />
     * <br />
     * Note that the byte array supplied here is cloned to protect against
     * subsequent modifications.
     * 
     * @param subjectKeyID -
     *            the subject key identifier (or <code>null</code>)
     * 
     * @see #getSubjectKeyIdentifier()
     */
    public void setSubjectKeyIdentifier(byte[] subjectKeyID)
    {
        if (subjectKeyID == null)
        {
            this.subjectKeyID = null;
        }
        else
        {
            this.subjectKeyID = (byte[])subjectKeyID.clone();
        }
    }

    /**
     * Sets the authorityKeyIdentifier criterion. The
     * <code>X509Certificate</code> must contain an AuthorityKeyIdentifier
     * extension for which the contents of the extension value matches the
     * specified criterion value. If the criterion value is <code>null</code>,
     * no authorityKeyIdentifier check will be done.<br />
     * <br />
     * If <code>authorityKeyID</code> is not <code>null</code>, it should
     * contain a single DER encoded value corresponding to the contents of the
     * extension value (not including the object identifier, criticality
     * setting, and encapsulating OCTET STRING) for an AuthorityKeyIdentifier
     * extension. The ASN.1 notation for this structure follows.<br />
     * <br />
     * 
     * <pre>
     *    AuthorityKeyIdentifier ::= SEQUENCE {
     *      keyIdentifier             [0] KeyIdentifier           OPTIONAL,
     *      authorityCertIssuer       [1] GeneralNames            OPTIONAL,
     *      authorityCertSerialNumber [2] CertificateSerialNumber OPTIONAL  }
     * 
     *    KeyIdentifier ::= OCTET STRING
     * </pre>
     * 
     * <br />
     * <br />
     * Authority key identifiers are not parsed by the
     * <code>X509CertSelector</code>. Instead, the values are compared using
     * a byte-by-byte comparison.<br />
     * <br />
     * When the <code>keyIdentifier</code> field of
     * <code>AuthorityKeyIdentifier</code> is populated, the value is usually
     * taken from the SubjectKeyIdentifier extension in the issuer's
     * certificate. Note, however, that the result of
     * X509Certificate.getExtensionValue(<SubjectKeyIdentifier Object
     * Identifier>) on the issuer's certificate may NOT be used directly as the
     * input to setAuthorityKeyIdentifier. This is because the
     * SubjectKeyIdentifier contains only a KeyIdentifier OCTET STRING, and not
     * a SEQUENCE of KeyIdentifier, GeneralNames, and CertificateSerialNumber.
     * In order to use the extension value of the issuer certificate's
     * SubjectKeyIdentifier extension, it will be necessary to extract the value
     * of the embedded KeyIdentifier OCTET STRING, then DER encode this OCTET
     * STRING inside a SEQUENCE. For more details on SubjectKeyIdentifier, see
     * {@link #setSubjectKeyIdentifier(byte[])  setSubjectKeyIdentifier(byte[] subjectKeyID }).<br />
     * <br />
     * Note also that the byte array supplied here is cloned to protect against
     * subsequent modifications.
     * 
     * @param authorityKeyID
     *            the authority key identifier (or <code>null</code>)
     * 
     * @see #getAuthorityKeyIdentifier()
     */
    public void setAuthorityKeyIdentifier(byte[] authorityKeyID)
    {
        if (authorityKeyID == null)
        {
            this.authorityKeyID = null;
        }
        else
        {
            this.authorityKeyID = (byte[])authorityKeyID.clone();
        }
    }

    /**
     * Sets the certificateValid criterion. The specified date must fall within
     * the certificate validity period for the X509Certificate. If
     * <code>null</code>, no certificateValid check will be done.<br />
     * <br />
     * Note that the Date supplied here is cloned to protect against subsequent
     * modifications.
     * 
     * @param certValid
     *            the Date to check (or <code>null</code>)
     * 
     * @see #getCertificateValid()
     */
    public void setCertificateValid(Date certValid)
    {
        if (certValid == null)
        {
            this.certValid = null;
        }
        else
        {
            this.certValid = new Date(certValid.getTime());
        }
    }

    /**
     * Sets the privateKeyValid criterion. The specified date must fall within
     * the private key validity period for the X509Certificate. If
     * <code>null</code>, no privateKeyValid check will be done.<br />
     * <br />
     * Note that the Date supplied here is cloned to protect against subsequent
     * modifications.
     * 
     * @param privateKeyValid
     *            the Date to check (or <code>null</code>)
     * 
     * @see #getPrivateKeyValid()
     */
    public void setPrivateKeyValid(Date privateKeyValid)
    {
        if (privateKeyValid == null)
        {
            this.privateKeyValid = null;
        }
        else
        {
            this.privateKeyValid = new Date(privateKeyValid.getTime());
        }
    }

    /**
     * Sets the subjectPublicKeyAlgID criterion. The X509Certificate must
     * contain a subject public key with the specified algorithm. If
     * <code>null</code>, no subjectPublicKeyAlgID check will be done.
     * 
     * @param oid
     *            The object identifier (OID) of the algorithm to check for (or
     *            <code>null</code>). An OID is represented by a set of
     *            nonnegative integers separated by periods.
     * 
     * @exception IOException
     *                if the OID is invalid, such as the first component being
     *                not 0, 1 or 2 or the second component being greater than
     *                39.
     * 
     * @see #getSubjectPublicKeyAlgID()
     */
    public void setSubjectPublicKeyAlgID(String oid) throws IOException
    {
        if (oid != null)
        {
            CertUtil.parseOID(oid);
            subjectKeyAlgID = new ASN1ObjectIdentifier(oid);
        }
        else
        {
            subjectKeyAlgID = null;
        }
    }

    /**
     * Sets the subjectPublicKey criterion. The X509Certificate must contain the
     * specified subject public key. If null, no subjectPublicKey check will be
     * done.
     * 
     * @param key
     *            the subject public key to check for (or null)
     * 
     * @see #getSubjectPublicKey()
     */
    public void setSubjectPublicKey(PublicKey key)
    {
        if (key == null)
        {
            subjectPublicKey = null;
            subjectPublicKeyByte = null;
        }
        else
        {
            subjectPublicKey = key;
            subjectPublicKeyByte = key.getEncoded();
        }
    }

    /**
     * Sets the subjectPublicKey criterion. The <code>X509Certificate</code>
     * must contain the specified subject public key. If <code>null</code>,
     * no subjectPublicKey check will be done.<br />
     * <br />
     * Because this method allows the public key to be specified as a byte
     * array, it may be used for unknown key types.<br />
     * <br />
     * If key is not <code>null</code>, it should contain a single DER
     * encoded SubjectPublicKeyInfo structure, as defined in X.509. The ASN.1
     * notation for this structure is as follows.<br />
     * <br />
     * 
     * <pre>
     *    SubjectPublicKeyInfo  ::=  SEQUENCE  {
     *      algorithm            AlgorithmIdentifier,
     *      subjectPublicKey     BIT STRING  }
     * 
     *    AlgorithmIdentifier  ::=  SEQUENCE  {
     *      algorithm               OBJECT IDENTIFIER,
     *      parameters              ANY DEFINED BY algorithm OPTIONAL  }
     *                                -- contains a value of the type
     *                                -- registered for use with the
     *                                -- algorithm object identifier value
     * </pre>
     * 
     * <br />
     * <br />
     * Note that the byte array supplied here is cloned to protect against
     * subsequent modifications.
     * 
     * @param key
     *            a byte array containing the subject public key in ASN.1 DER
     *            form (or <code>null</code>)
     * 
     * @exception IOException
     *                if an encoding error occurs (incorrect form for subject
     *                public key)
     * 
     * @see #getSubjectPublicKey()
     */
    public void setSubjectPublicKey(byte[] key) throws IOException
    {
        if (key == null)
        {
            subjectPublicKey = null;
            subjectPublicKeyByte = null;
        }
        else
        {
            subjectPublicKey = null;
            subjectPublicKeyByte = (byte[])key.clone();
            // TODO
            // try to generyte PublicKey Object from subjectPublicKeyByte
        }
    }

    /**
     * Sets the keyUsage criterion. The X509Certificate must allow the specified
     * keyUsage values. If null, no keyUsage check will be done. Note that an
     * X509Certificate that has no keyUsage extension implicitly allows all
     * keyUsage values.<br />
     * <br />
     * Note that the boolean array supplied here is cloned to protect against
     * subsequent modifications.
     * 
     * @param keyUsage
     *            a boolean array in the same format as the boolean array
     *            returned by X509Certificate.getKeyUsage(). Or
     *            <code>null</code>.
     * 
     * @see #getKeyUsage()
     */
    public void setKeyUsage(boolean[] keyUsage)
    {
        if (keyUsage == null)
        {
            this.keyUsage = null;
        }
        else
        {
            this.keyUsage = (boolean[])keyUsage.clone();
        }
    }

    /**
     * Sets the extendedKeyUsage criterion. The <code>X509Certificate</code>
     * must allow the specified key purposes in its extended key usage
     * extension. If <code>keyPurposeSet</code> is empty or <code>null</code>,
     * no extendedKeyUsage check will be done. Note that an
     * <code>X509Certificate</code> that has no extendedKeyUsage extension
     * implicitly allows all key purposes.<br />
     * <br />
     * Note that the Set is cloned to protect against subsequent modifications.<br />
     * <br />
     * Uses {@link org.spongycastle.asn1.x509.KeyPurposeId KeyPurposeId}
     * 
     * @param keyPurposeSet
     *            a <code>Set</code> of key purpose OIDs in string format (or
     *            <code>null</code>). Each OID is represented by a set of
     *            nonnegative integers separated by periods.
     * 
     * @exception IOException
     *                if the OID is invalid, such as the first component being
     *                not 0, 1 or 2 or the second component being greater than
     *                39.
     * 
     * @see #getExtendedKeyUsage()
     */
    public void setExtendedKeyUsage(Set keyPurposeSet) throws IOException
    {
        if (keyPurposeSet == null || keyPurposeSet.isEmpty())
        {
            this.keyPurposeSet = keyPurposeSet;
        }
        else
        {
            this.keyPurposeSet = new HashSet();
            Iterator iter = keyPurposeSet.iterator();
            Object obj;
            KeyPurposeId purposeID;
            while (iter.hasNext())
            {
                obj = iter.next();
                if (obj instanceof String)
                {
                    purposeID = (KeyPurposeId)keyPurposeIdMap.get((String)obj);
                    if (purposeID == null)
                    {
                        throw new IOException("unknown purposeID "
                                + (String)obj);
                    }
                    this.keyPurposeSet.add(purposeID);
                }
            }
        }
    }

    /**
     * Enables/disables matching all of the subjectAlternativeNames specified in
     * the {@link #setSubjectAlternativeNames setSubjectAlternativeNames} or
     * {@link #addSubjectAlternativeName addSubjectAlternativeName} methods. If
     * enabled, the <code>X509Certificate</code> must contain all of the
     * specified subject alternative names. If disabled, the X509Certificate
     * must contain at least one of the specified subject alternative names.<br />
     * <br />
     * The matchAllNames flag is <code>true</code> by default.
     * 
     * @param matchAllNames
     *            if <code>true</code>, the flag is enabled; if
     *            <code>false</code>, the flag is disabled.
     * 
     * @see #getMatchAllSubjectAltNames()
     */
    public void setMatchAllSubjectAltNames(boolean matchAllNames)
    {
        matchAllSubjectAltNames = matchAllNames;
    }

    /**
     * Sets the subjectAlternativeNames criterion. The
     * <code>X509Certificate</code> must contain all or at least one of the
     * specified subjectAlternativeNames, depending on the value of the
     * matchAllNames flag (see {@link #setMatchAllSubjectAltNames}).<br />
     * <br />
     * This method allows the caller to specify, with a single method call, the
     * complete set of subject alternative names for the subjectAlternativeNames
     * criterion. The specified value replaces the previous value for the
     * subjectAlternativeNames criterion.<br />
     * <br />
     * The <code>names</code> parameter (if not <code>null</code>) is a
     * <code>Collection</code> with one entry for each name to be included in
     * the subject alternative name criterion. Each entry is a <code>List</code>
     * whose first entry is an <code>Integer</code> (the name type, 0-8) and
     * whose second entry is a <code>String</code> or a byte array (the name,
     * in string or ASN.1 DER encoded form, respectively). There can be multiple
     * names of the same type. If <code>null</code> is supplied as the value
     * for this argument, no subjectAlternativeNames check will be performed.<br />
     * <br />
     * Each subject alternative name in the <code>Collection</code> may be
     * specified either as a <code>String</code> or as an ASN.1 encoded byte
     * array. For more details about the formats used, see
     * {@link #addSubjectAlternativeName(int, String) addSubjectAlternativeName(int type, String name)}
     * and
     * {@link #addSubjectAlternativeName(int, byte[]) addSubjectAlternativeName(int type, byte [] name}).<br />
     * <br />
     * Note that the <code>names</code> parameter can contain duplicate names
     * (same name and name type), but they may be removed from the
     * <code>Collection</code> of names returned by the
     * {@link #getSubjectAlternativeNames} method.<br />
     * <br />
     * Note that a deep copy is performed on the Collection to protect against
     * subsequent modifications.
     * 
     * @param names -
     *            a Collection of names (or null)
     * 
     * @exception IOException
     *                if a parsing error occurs
     * 
     * @see #getSubjectAlternativeNames()
     */
    public void setSubjectAlternativeNames(Collection names) throws IOException
    {
        try
        {
            if (names == null || names.isEmpty())
            {
                subjectAltNames = null;
                subjectAltNamesByte = null;
            }
            else
            {
                subjectAltNames = new HashSet();
                subjectAltNamesByte = new HashSet();
                Iterator iter = names.iterator();
                List item;
                int type;
                Object data;
                while (iter.hasNext())
                {
                    item = (List)iter.next();
                    type = ((Integer)item.get(0)).intValue();
                    data = item.get(1);
                    if (data instanceof String)
                    {
                        addSubjectAlternativeName(type, (String)data);
                    }
                    else if (data instanceof byte[])
                    {
                        addSubjectAlternativeName(type, (byte[])data);
                    }
                    else
                    {
                        throw new IOException(
                                "parsing error: unknown data type");
                    }
                }
            }
        }
        catch (Exception ex)
        {
            throw new IOException("parsing exception:\n" + ex.toString());
        }
    }

    /**
     * Adds a name to the subjectAlternativeNames criterion. The
     * <code>X509Certificate</code> must contain all or at least one of the
     * specified subjectAlternativeNames, depending on the value of the
     * matchAllNames flag (see {@link #setMatchAllSubjectAltNames}).<br />
     * <br />
     * This method allows the caller to add a name to the set of subject
     * alternative names. The specified name is added to any previous value for
     * the subjectAlternativeNames criterion. If the specified name is a
     * duplicate, it may be ignored.<br />
     * <br />
     * The name is provided in string format. RFC 822, DNS, and URI names use
     * the well-established string formats for those types (subject to the
     * restrictions included in RFC 2459). IPv4 address names are supplied using
     * dotted quad notation. OID address names are represented as a series of
     * nonnegative integers separated by periods. And directory names
     * (distinguished names) are supplied in RFC 2253 format. No standard string
     * format is defined for otherNames, X.400 names, EDI party names, IPv6
     * address names, or any other type of names. They should be specified using
     * the
     * {@link #addSubjectAlternativeName(int, byte[]) addSubjectAlternativeName(int type, byte [] name)}
     * method.
     * 
     * @param type
     *            the name type (0-8, as specified in RFC 2459, section 4.2.1.7)
     * @param name -
     *            the name in string form (not null)
     * 
     * @exception IOException
     *                if a parsing error occurs
     */
    public void addSubjectAlternativeName(int type, String name)
            throws IOException
    {
        // TODO full implementation of CertUtil.parseGeneralName
        byte[] encoded = CertUtil.parseGeneralName(type, name);
        List tmpList = new ArrayList();
        tmpList.add(Integers.valueOf(type));
        tmpList.add(name);
        subjectAltNames.add(tmpList);
        tmpList.set(1, encoded);
        subjectAltNamesByte.add(tmpList);
    }

    /**
     * Adds a name to the subjectAlternativeNames criterion. The
     * <code>X509Certificate</code> must contain all or at least one of the
     * specified subjectAlternativeNames, depending on the value of the
     * matchAllNames flag (see {@link #setMatchAllSubjectAltNames}).<br />
     * <br />
     * This method allows the caller to add a name to the set of subject
     * alternative names. The specified name is added to any previous value for
     * the subjectAlternativeNames criterion. If the specified name is a
     * duplicate, it may be ignored.<br />
     * <br />
     * The name is provided as a byte array. This byte array should contain the
     * DER encoded name, as it would appear in the GeneralName structure defined
     * in RFC 2459 and X.509. The encoded byte array should only contain the
     * encoded value of the name, and should not include the tag associated with
     * the name in the GeneralName structure. The ASN.1 definition of this
     * structure appears below.<br />
     * <br />
     * 
     * <pre>
     *    GeneralName ::= CHOICE {
     *        otherName                       [0]     OtherName,
     *        rfc822Name                      [1]     IA5String,
     *        dNSName                         [2]     IA5String,
     *        x400Address                     [3]     ORAddress,
     *        directoryName                   [4]     Name,
     *        ediPartyName                    [5]     EDIPartyName,
     *        uniformResourceIdentifier       [6]     IA5String,
     *        iPAddress                       [7]     OCTET STRING,
     *        registeredID                    [8]     OBJECT IDENTIFIER}
     * </pre>
     * 
     * <br />
     * <br />
     * Note that the byte array supplied here is cloned to protect against
     * subsequent modifications.<br />
     * <br />
     * <b>TODO: check encoded format</b>
     * 
     * @param type
     *            the name type (0-8, as listed above)
     * @param name
     *            a byte array containing the name in ASN.1 DER encoded form
     * 
     * @exception IOException
     *                if a parsing error occurs
     */
    public void addSubjectAlternativeName(int type, byte[] name)
            throws IOException
    {
        // TODO check encoded format
        List tmpList = new ArrayList();
        tmpList.add(Integers.valueOf(type));
        tmpList.add(name.clone());
        subjectAltNames.add(tmpList);
        subjectAltNamesByte.add(tmpList);
    }

    /**
     * Sets the name constraints criterion. The <code>X509Certificate</code>
     * must have subject and subject alternative names that meet the specified
     * name constraints.<br />
     * <br />
     * The name constraints are specified as a byte array. This byte array
     * should contain the DER encoded form of the name constraints, as they
     * would appear in the NameConstraints structure defined in RFC 2459 and
     * X.509. The ASN.1 definition of this structure appears below.<br />
     * <br />
     * 
     * <pre>
     *   NameConstraints ::= SEQUENCE {
     *        permittedSubtrees       [0]     GeneralSubtrees OPTIONAL,
     *        excludedSubtrees        [1]     GeneralSubtrees OPTIONAL }
     * 
     *   GeneralSubtrees ::= SEQUENCE SIZE (1..MAX) OF GeneralSubtree
     * 
     *   GeneralSubtree ::= SEQUENCE {
     *        base                    GeneralName,
     *        minimum         [0]     BaseDistance DEFAULT 0,
     *        maximum         [1]     BaseDistance OPTIONAL }
     * 
     *   BaseDistance ::= INTEGER (0..MAX)
     * 
     *   GeneralName ::= CHOICE {
     *        otherName                       [0]     OtherName,
     *        rfc822Name                      [1]     IA5String,
     *        dNSName                         [2]     IA5String,
     *        x400Address                     [3]     ORAddress,
     *        directoryName                   [4]     Name,
     *        ediPartyName                    [5]     EDIPartyName,
     *        uniformResourceIdentifier       [6]     IA5String,
     *        iPAddress                       [7]     OCTET STRING,
     *        registeredID                    [8]     OBJECT IDENTIFIER}
     * </pre>
     * 
     * <br />
     * <br />
     * Note that the byte array supplied here is cloned to protect against
     * subsequent modifications.<br />
     * <br />
     * <b>TODO: implement this</b>
     * 
     * @param bytes
     *            a byte array containing the ASN.1 DER encoding of a
     *            NameConstraints extension to be used for checking name
     *            constraints. Only the value of the extension is included, not
     *            the OID or criticality flag. Can be <code>null</code>, in
     *            which case no name constraints check will be performed
     * 
     * @exception IOException
     *                if a parsing error occurs
     * @exception UnsupportedOperationException
     *                because this method is not supported
     * @see #getNameConstraints()
     */
    public void setNameConstraints(byte[] bytes) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the basic constraints constraint. If the value is greater than or
     * equal to zero, <code>X509Certificates</code> must include a
     * basicConstraints extension with a pathLen of at least this value. If the
     * value is -2, only end-entity certificates are accepted. If the value is
     * -1, no check is done.<br />
     * <br />
     * This constraint is useful when building a certification path forward
     * (from the target toward the trust anchor. If a partial path has been
     * built, any candidate certificate must have a maxPathLen value greater
     * than or equal to the number of certificates in the partial path.
     * 
     * @param minMaxPathLen
     *            the value for the basic constraints constraint
     * 
     * @exception IllegalArgumentException
     *                if the value is less than -2
     * 
     * @see #getBasicConstraints()
     */
    public void setBasicConstraints(int minMaxPathLen)
    {
        if (minMaxPathLen < -2)
        {
            throw new IllegalArgumentException("minMaxPathLen must be >= -2");
        }

        this.minMaxPathLen = minMaxPathLen;
    }

    /**
     * Sets the policy constraint. The X509Certificate must include at least one
     * of the specified policies in its certificate policies extension. If
     * certPolicySet is empty, then the X509Certificate must include at least
     * some specified policy in its certificate policies extension. If
     * certPolicySet is null, no policy check will be performed.<br />
     * <br />
     * Note that the Set is cloned to protect against subsequent modifications.<br />
     * <br />
     * <b>TODO: implement match check for this</b>
     * 
     * @param certPolicySet
     *            a Set of certificate policy OIDs in string format (or null).
     *            Each OID is represented by a set of nonnegative integers
     *            separated by periods.
     * 
     * @exception IOException
     *                if a parsing error occurs on the OID such as the first
     *                component is not 0, 1 or 2 or the second component is
     *                greater than 39.
     * 
     * @see #getPolicy()
     */
    public void setPolicy(Set certPolicySet) throws IOException
    {
        if (certPolicySet == null)
        {
            policy = null;
            policyOID = null;
        }
        else
        {
            policyOID = new HashSet();
            Iterator iter = certPolicySet.iterator();
            Object item;
            while (iter.hasNext())
            {
                item = iter.next();
                if (item instanceof String)
                {
                    CertUtil.parseOID((String)item);
                    policyOID.add(new ASN1ObjectIdentifier((String)item));
                }
                else
                {
                    throw new IOException(
                            "certPolicySet contains null values or non String objects");
                }
            }
            policy = new HashSet(certPolicySet);
        }
    }

    /**
     * Sets the pathToNames criterion. The <code>X509Certificate</code> must
     * not include name constraints that would prohibit building a path to the
     * specified names.<br />
     * <br />
     * This method allows the caller to specify, with a single method call, the
     * complete set of names which the <code>X509Certificates</code>'s name
     * constraints must permit. The specified value replaces the previous value
     * for the pathToNames criterion.<br />
     * <br />
     * This constraint is useful when building a certification path forward
     * (from the target toward the trust anchor. If a partial path has been
     * built, any candidate certificate must not include name constraints that
     * would prohibit building a path to any of the names in the partial path.<br />
     * <br />
     * The names parameter (if not <code>null</code>) is a
     * <code>Collection</code> with one entry for each name to be included in
     * the pathToNames criterion. Each entry is a <code>List</code> whose
     * first entry is an Integer (the name type, 0-8) and whose second entry is
     * a <code>String</code> or a byte array (the name, in string or ASN.1 DER
     * encoded form, respectively). There can be multiple names of the same
     * type. If <code>null</code> is supplied as the value for this argument,
     * no pathToNames check will be performed.<br />
     * <br />
     * Each name in the Collection may be specified either as a String or as an
     * ASN.1 encoded byte array. For more details about the formats used, see
     * {@link #addPathToName(int, String) addPathToName(int type, String name)}
     * and
     * {@link #addPathToName(int, byte[]) addPathToName(int type, byte [] name)}.<br />
     * <br />
     * Note that the names parameter can contain duplicate names (same name and
     * name type), but they may be removed from the Collection of names returned
     * by the {@link #getPathToNames} method.<br />
     * <br />
     * Note that a deep copy is performed on the Collection to protect against
     * subsequent modifications.<br />
     * <br />
     * <b>TODO: implement this match check for this</b>
     * 
     * @param names
     *            a Collection with one entry per name (or <code>null</code>)
     * 
     * @exception IOException
     *                if a parsing error occurs
     * @exception UnsupportedOperationException
     *                because this method is not supported
     * 
     * @see #getPathToNames()
     */
    public void setPathToNames(Collection names) throws IOException
    {
        try
        {
            if (names == null || names.isEmpty())
            {
                pathToNames = null;
                pathToNamesByte = null;
            }
            else
            {
                pathToNames = new HashSet();
                pathToNamesByte = new HashSet();
                Iterator iter = names.iterator();
                List item;
                int type;
                Object data;

                while (iter.hasNext())
                {
                    item = (List)iter.next();
                    type = ((Integer)item.get(0)).intValue();
                    data = item.get(1);
                    if (data instanceof String)
                    {
                        addPathToName(type, (String)data);
                    }
                    else if (data instanceof byte[])
                    {
                        addPathToName(type, (byte[])data);
                    }
                    else
                    {
                        throw new IOException(
                                "parsing error: unknown data type");
                    }
                }
            }
        }
        catch (Exception ex)
        {
            throw new IOException("parsing exception:\n" + ex.toString());
        }
    }

    /**
     * Adds a name to the pathToNames criterion. The
     * <code>X509Certificate</code> must not include name constraints that
     * would prohibit building a path to the specified name.<br />
     * <br />
     * This method allows the caller to add a name to the set of names which the
     * <code>X509Certificates</code>'s name constraints must permit. The
     * specified name is added to any previous value for the pathToNames
     * criterion. If the name is a duplicate, it may be ignored.<br />
     * <br />
     * The name is provided in string format. RFC 822, DNS, and URI names use
     * the well-established string formats for those types (subject to the
     * restrictions included in RFC 2459). IPv4 address names are supplied using
     * dotted quad notation. OID address names are represented as a series of
     * nonnegative integers separated by periods. And directory names
     * (distinguished names) are supplied in RFC 2253 format. No standard string
     * format is defined for otherNames, X.400 names, EDI party names, IPv6
     * address names, or any other type of names. They should be specified using
     * the
     * {@link #addPathToName(int, byte[]) addPathToName(int type, byte [] name)}
     * method.<br />
     * <br />
     * <b>TODO: implement this match check for this</b>
     * 
     * @param type
     *            the name type (0-8, as specified in RFC 2459, section 4.2.1.7)
     * @param name
     *            the name in string form
     * 
     * @exceptrion IOException if a parsing error occurs
     */
    public void addPathToName(int type, String name) throws IOException
    {
        // TODO full implementation of CertUtil.parseGeneralName
        byte[] encoded = CertUtil.parseGeneralName(type, name);
        List tmpList = new ArrayList();
        tmpList.add(Integers.valueOf(type));
        tmpList.add(name);
        pathToNames.add(tmpList);
        tmpList.set(1, encoded);
        pathToNamesByte.add(tmpList);
        throw new UnsupportedOperationException();
    }

    /**
     * Adds a name to the pathToNames criterion. The
     * <code>X509Certificate</code> must not include name constraints that
     * would prohibit building a path to the specified name.<br />
     * <br />
     * This method allows the caller to add a name to the set of names which the
     * <code>X509Certificates</code>'s name constraints must permit. The
     * specified name is added to any previous value for the pathToNames
     * criterion. If the name is a duplicate, it may be ignored.<br />
     * <br />
     * The name is provided as a byte array. This byte array should contain the
     * DER encoded name, as it would appear in the GeneralName structure defined
     * in RFC 2459 and X.509. The ASN.1 definition of this structure appears in
     * the documentation for
     * {@link #addSubjectAlternativeName(int,byte[]) addSubjectAlternativeName(int type, byte[] name)}.<br />
     * <br />
     * Note that the byte array supplied here is cloned to protect against
     * subsequent modifications.<br />
     * <br />
     * <b>TODO: implement this match check for this</b>
     * 
     * @param type
     *            the name type (0-8, as specified in RFC 2459, section 4.2.1.7)
     * @param name
     *            a byte array containing the name in ASN.1 DER encoded form
     * 
     * @exception IOException
     *                if a parsing error occurs
     */
    public void addPathToName(int type, byte[] name) throws IOException
    {
        // TODO check encoded format
        List tmpList = new ArrayList();
        tmpList.add(Integers.valueOf(type));
        tmpList.add(name.clone());
        pathToNames.add(tmpList);
        pathToNamesByte.add(tmpList);
    }

    /**
     * Returns the certificateEquals criterion. The specified
     * <code>X509Certificate</code> must be equal to the
     * <code>X509Certificate</code> passed to the match method. If
     * <code>null</code>, this check is not applied.
     * 
     * @retrun the <code>X509Certificate</code> to match (or <code>null</code>)
     * 
     * @see #setCertificate(java.security.cert.X509Certificate)
     */
    public X509Certificate getCertificate()
    {
        return x509Cert;
    }

    /**
     * Returns the serialNumber criterion. The specified serial number must
     * match the certificate serial number in the <code>X509Certificate</code>.
     * If <code>null</code>, any certificate serial number will do.
     * 
     * @return the certificate serial number to match (or <code>null</code>)
     * 
     * @see #setSerialNumber(java.math.BigInteger)
     */
    public BigInteger getSerialNumber()
    {
        return serialNumber;
    }

    /**
     * Returns the issuer criterion as a String. This distinguished name must
     * match the issuer distinguished name in the <code>X509Certificate</code>.
     * If <code>null</code>, the issuer criterion is disabled and any issuer
     * distinguished name will do.<br />
     * <br />
     * If the value returned is not <code>null</code>, it is a distinguished
     * name, in RFC 2253 format.<br />
     * <br />
     * Uses {@link org.spongycastle.asn1.x509.X509Name X509Name} for formatiing
     * byte[] issuerDN to String.
     * 
     * @return the required issuer distinguished name in RFC 2253 format (or
     *         <code>null</code>)
     */
    public String getIssuerAsString()
    {
        if (issuerDN instanceof String)
        {
            return new String((String)issuerDN);
        }
        else if (issuerDNX509 != null)
        {
            return issuerDNX509.toString();
        }

        return null;
    }

    /**
     * Returns the issuer criterion as a byte array. This distinguished name
     * must match the issuer distinguished name in the
     * <code>X509Certificate</code>. If <code>null</code>, the issuer
     * criterion is disabled and any issuer distinguished name will do.<br />
     * <br />
     * If the value returned is not <code>null</code>, it is a byte array
     * containing a single DER encoded distinguished name, as defined in X.501.
     * The ASN.1 notation for this structure is supplied in the documentation
     * for {@link #setIssuer(byte[]) setIssuer(byte [] issuerDN)}.<br />
     * <br />
     * Note that the byte array returned is cloned to protect against subsequent
     * modifications.<br />
     * <br />
     * Uses {@link org.spongycastle.asn1.DEROutputStream DEROutputStream},
     * {@link org.spongycastle.asn1.x509.X509Name X509Name} to gnerate byte[]
     * output for String issuerDN.
     * 
     * @return a byte array containing the required issuer distinguished name in
     *         ASN.1 DER format (or <code>null</code>)
     * 
     * @exception IOException
     *                if an encoding error occurs
     */
    public byte[] getIssuerAsBytes() throws IOException
    {
        if (issuerDN instanceof byte[])
        {
            return (byte[])((byte[])issuerDN).clone();
        }
        else if (issuerDNX509 != null)
        {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DEROutputStream derOutStream = new DEROutputStream(outStream);

            derOutStream.writeObject(issuerDNX509.toASN1Primitive());
            derOutStream.close();

            return outStream.toByteArray();
        }

        return null;
    }

    /**
     * Returns the subject criterion as a String. This distinguished name must
     * match the subject distinguished name in the <code>X509Certificate</code>.
     * If <code>null</code>, the subject criterion is disabled and any
     * subject distinguished name will do.<br />
     * <br />
     * If the value returned is not <code>null</code>, it is a distinguished
     * name, in RFC 2253 format.<br />
     * <br />
     * Uses {@link org.spongycastle.asn1.x509.X509Name X509Name} for formatiing
     * byte[] subjectDN to String.
     * 
     * @return the required subject distinguished name in RFC 2253 format (or
     *         <code>null</code>)
     */
    public String getSubjectAsString()
    {
        if (subjectDN instanceof String)
        {
            return new String((String)subjectDN);
        }
        else if (subjectDNX509 != null)
        {
            return subjectDNX509.toString();
        }

        return null;
    }

    /**
     * Returns the subject criterion as a byte array. This distinguished name
     * must match the subject distinguished name in the
     * <code>X509Certificate</code>. If <code>null</code>, the subject
     * criterion is disabled and any subject distinguished name will do.<br />
     * <br />
     * If the value returned is not <code>null</code>, it is a byte array
     * containing a single DER encoded distinguished name, as defined in X.501.
     * The ASN.1 notation for this structure is supplied in the documentation
     * for {@link #setSubject(byte [] subjectDN) setSubject(byte [] subjectDN)}.<br />
     * <br />
     * Note that the byte array returned is cloned to protect against subsequent
     * modifications.<br />
     * <br />
     * Uses {@link org.spongycastle.asn1.DEROutputStream DEROutputStream},
     * {@link org.spongycastle.asn1.x509.X509Name X509Name} to gnerate byte[]
     * output for String subjectDN.
     * 
     * @return a byte array containing the required subject distinguished name
     *         in ASN.1 DER format (or <code>null</code>)
     * 
     * @exception IOException
     *                if an encoding error occurs
     */
    public byte[] getSubjectAsBytes() throws IOException
    {
        if (subjectDN instanceof byte[])
        {
            return (byte[])((byte[])subjectDN).clone();
        }
        else if (subjectDNX509 != null)
        {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DEROutputStream derOutStream = new DEROutputStream(outStream);

            derOutStream.writeObject(subjectDNX509.toASN1Primitive());
            derOutStream.close();

            return outStream.toByteArray();
        }

        return null;
    }

    /**
     * Returns the subjectKeyIdentifier criterion. The
     * <code>X509Certificate</code> must contain a SubjectKeyIdentifier
     * extension with the specified value. If <code>null</code>, no
     * subjectKeyIdentifier check will be done.<br />
     * <br />
     * Note that the byte array returned is cloned to protect against subsequent
     * modifications.
     * 
     * @return the key identifier (or <code>null</code>)
     * 
     * @see #setSubjectKeyIdentifier
     */
    public byte[] getSubjectKeyIdentifier()
    {
        if (subjectKeyID != null)
        {
            return (byte[])subjectKeyID.clone();
        }

        return null;
    }

    /**
     * Returns the authorityKeyIdentifier criterion. The
     * <code>X509Certificate</code> must contain a AuthorityKeyIdentifier
     * extension with the specified value. If <code>null</code>, no
     * authorityKeyIdentifier check will be done.<br />
     * <br />
     * Note that the byte array returned is cloned to protect against subsequent
     * modifications.
     * 
     * @return the key identifier (or <code>null</code>)
     * 
     * @see #setAuthorityKeyIdentifier
     */
    public byte[] getAuthorityKeyIdentifier()
    {
        if (authorityKeyID != null)
        {
            return (byte[])authorityKeyID.clone();
        }

        return null;
    }

    /**
     * Returns the certificateValid criterion. The specified date must fall
     * within the certificate validity period for the
     * <code>X509Certificate</code>. If <code>null</code>, no
     * certificateValid check will be done.<br />
     * <br />
     * Note that the <code>Date</code> returned is cloned to protect against
     * subsequent modifications.
     * 
     * @return the <code>Date</code> to check (or <code>null</code>)
     * 
     * @see #setCertificateValid
     */
    public Date getCertificateValid()
    {
        if (certValid != null)
        {
            return new Date(certValid.getTime());
        }

        return null;
    }

    /**
     * Returns the privateKeyValid criterion. The specified date must fall
     * within the private key validity period for the
     * <code>X509Certificate</code>. If <code>null</code>, no
     * privateKeyValid check will be done.<br />
     * <br />
     * Note that the <code>Date</code> returned is cloned to protect against
     * subsequent modifications.
     * 
     * @return the <code>Date</code> to check (or <code>null</code>)
     * 
     * @see #setPrivateKeyValid
     */
    public Date getPrivateKeyValid()
    {
        if (privateKeyValid != null)
        {
            return new Date(privateKeyValid.getTime());
        }

        return null;
    }

    /**
     * Returns the subjectPublicKeyAlgID criterion. The
     * <code>X509Certificate</code> must contain a subject public key with the
     * specified algorithm. If <code>null</code>, no subjectPublicKeyAlgID
     * check will be done.
     * 
     * @return the object identifier (OID) of the signature algorithm to check
     *         for (or <code>null</code>). An OID is represented by a set of
     *         nonnegative integers separated by periods.
     * 
     * @see #setSubjectPublicKeyAlgID
     */
    public String getSubjectPublicKeyAlgID()
    {
        if (subjectKeyAlgID != null)
        {
            return subjectKeyAlgID.toString();
        }

        return null;
    }

    /**
     * Returns the subjectPublicKey criterion. The <code>X509Certificate</code>
     * must contain the specified subject public key. If <code>null</code>,
     * no subjectPublicKey check will be done.
     * 
     * @return the subject public key to check for (or <code>null</code>)
     * 
     * @see #setSubjectPublicKey
     */
    public PublicKey getSubjectPublicKey()
    {
        return subjectPublicKey;
    }

    /**
     * Returns the keyUsage criterion. The <code>X509Certificate</code> must
     * allow the specified keyUsage values. If null, no keyUsage check will be
     * done.<br />
     * <br />
     * Note that the boolean array returned is cloned to protect against
     * subsequent modifications.
     * 
     * @return a boolean array in the same format as the boolean array returned
     *         by
     *         {@link X509Certificate#getKeyUsage() X509Certificate.getKeyUsage()}.
     *         Or <code>null</code>.
     * 
     * @see #setKeyUsage
     */
    public boolean[] getKeyUsage()
    {
        if (keyUsage != null)
        {
            return (boolean[])keyUsage.clone();
        }

        return null;
    }

    /**
     * Returns the extendedKeyUsage criterion. The <code>X509Certificate</code>
     * must allow the specified key purposes in its extended key usage
     * extension. If the <code>keyPurposeSet</code> returned is empty or
     * <code>null</code>, no extendedKeyUsage check will be done. Note that
     * an <code>X509Certificate</code> that has no extendedKeyUsage extension
     * implicitly allows all key purposes.
     * 
     * @return an immutable <code>Set</code> of key purpose OIDs in string
     *         format (or <code>null</code>)
     * @see #setExtendedKeyUsage
     */
    public Set getExtendedKeyUsage()
    {
        if (keyPurposeSet == null || keyPurposeSet.isEmpty())
        {
            return keyPurposeSet;
        }

        Set returnSet = new HashSet();
        Iterator iter = keyPurposeSet.iterator();
        while (iter.hasNext())
        {
            returnSet.add(iter.next().toString());
        }

        return Collections.unmodifiableSet(returnSet);
    }

    /**
     * Indicates if the <code>X509Certificate</code> must contain all or at
     * least one of the subjectAlternativeNames specified in the
     * {@link #setSubjectAlternativeNames setSubjectAlternativeNames} or
     * {@link #addSubjectAlternativeName addSubjectAlternativeName} methods. If
     * <code>true</code>, the <code>X509Certificate</code> must contain all
     * of the specified subject alternative names. If <code>false</code>, the
     * <code>X509Certificate</code> must contain at least one of the specified
     * subject alternative names.
     * 
     * @return <code>true</code> if the flag is enabled; <code>false</code>
     *         if the flag is disabled. The flag is <code>true</code> by
     *         default.
     * 
     * @see #setMatchAllSubjectAltNames
     */
    public boolean getMatchAllSubjectAltNames()
    {
        return matchAllSubjectAltNames;
    }

    /**
     * Returns a copy of the subjectAlternativeNames criterion. The
     * <code>X509Certificate</code> must contain all or at least one of the
     * specified subjectAlternativeNames, depending on the value of the
     * matchAllNames flag (see {@link #getMatchAllSubjectAltNames
     * getMatchAllSubjectAltNames}). If the value returned is <code>null</code>,
     * no subjectAlternativeNames check will be performed.<br />
     * <br />
     * If the value returned is not <code>null</code>, it is a
     * <code>Collection</code> with one entry for each name to be included in
     * the subject alternative name criterion. Each entry is a <code>List</code>
     * whose first entry is an <code>Integer</code> (the name type, 0-8) and
     * whose second entry is a <code>String</code> or a byte array (the name,
     * in string or ASN.1 DER encoded form, respectively). There can be multiple
     * names of the same type. Note that the <code>Collection</code> returned
     * may contain duplicate names (same name and name type).<br />
     * <br />
     * Each subject alternative name in the <code>Collection</code> may be
     * specified either as a <code>String</code> or as an ASN.1 encoded byte
     * array. For more details about the formats used, see
     * {@link #addSubjectAlternativeName(int type, String name) 
     * addSubjectAlternativeName(int type, String name)} and
     * {@link #addSubjectAlternativeName(int type, byte [] name) 
     * addSubjectAlternativeName(int type, byte [] name)}.<br />
     * <br />
     * Note that a deep copy is performed on the <code>Collection</code> to
     * protect against subsequent modifications.
     * 
     * @return a <code>Collection</code> of names (or <code>null</code>)
     * 
     * @see #setSubjectAlternativeNames
     */
    public Collection getSubjectAlternativeNames()
    {
        if (subjectAltNames != null)
        {
            return null;
        }

        Set returnAltNames = new HashSet();
        List returnList;
        Iterator iter = subjectAltNames.iterator();
        List obj;
        while (iter.hasNext())
        {
            obj = (List)iter.next();
            returnList = new ArrayList();
            returnList.add(obj.get(0));
            if (obj.get(1) instanceof byte[])
            {
                returnList.add(((byte[])obj.get(1)).clone());
            }
            else
            {
                returnList.add(obj.get(1));
            }
            returnAltNames.add(returnList);
        }

        return returnAltNames;
    }

    /**
     * Returns the name constraints criterion. The <code>X509Certificate</code>
     * must have subject and subject alternative names that meet the specified
     * name constraints.<br />
     * <br />
     * The name constraints are returned as a byte array. This byte array
     * contains the DER encoded form of the name constraints, as they would
     * appear in the NameConstraints structure defined in RFC 2459 and X.509.
     * The ASN.1 notation for this structure is supplied in the documentation
     * for
     * {@link #setNameConstraints(byte [] bytes) setNameConstraints(byte [] bytes)}.<br />
     * <br />
     * Note that the byte array returned is cloned to protect against subsequent
     * modifications.<br />
     * <br />
     * <b>TODO: implement this</b>
     * 
     * @return a byte array containing the ASN.1 DER encoding of a
     *         NameConstraints extension used for checking name constraints.
     *         <code>null</code> if no name constraints check will be
     *         performed.
     * 
     * @exception UnsupportedOperationException
     *                because this method is not supported
     * 
     * @see #setNameConstraints
     */
    public byte[] getNameConstraints()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the basic constraints constraint. If the value is greater than or
     * equal to zero, the <code>X509Certificates</code> must include a
     * basicConstraints extension with a pathLen of at least this value. If the
     * value is -2, only end-entity certificates are accepted. If the value is
     * -1, no basicConstraints check is done.
     * 
     * @return the value for the basic constraints constraint
     * 
     * @see #setBasicConstraints
     */
    public int getBasicConstraints()
    {
        return minMaxPathLen;
    }

    /**
     * Returns the policy criterion. The <code>X509Certificate</code> must
     * include at least one of the specified policies in its certificate
     * policies extension. If the <code>Set</code> returned is empty, then the
     * <code>X509Certificate</code> must include at least some specified
     * policy in its certificate policies extension. If the <code>Set</code>
     * returned is <code>null</code>, no policy check will be performed.
     * 
     * @return an immutable <code>Set</code> of certificate policy OIDs in
     *         string format (or <code>null</code>)
     * 
     * @see #setPolicy
     */
    public Set getPolicy()
    {
        if (policy == null)
        {
            return null;
        }

        return Collections.unmodifiableSet(policy);
    }

    /**
     * Returns a copy of the pathToNames criterion. The
     * <code>X509Certificate</code> must not include name constraints that
     * would prohibit building a path to the specified names. If the value
     * returned is <code>null</code>, no pathToNames check will be performed.<br />
     * <br />
     * If the value returned is not <code>null</code>, it is a
     * <code>Collection</code> with one entry for each name to be included in
     * the pathToNames criterion. Each entry is a <code>List</code> whose
     * first entry is an <code>Integer</code> (the name type, 0-8) and whose
     * second entry is a <code>String</code> or a byte array (the name, in
     * string or ASN.1 DER encoded form, respectively). There can be multiple
     * names of the same type. Note that the <code>Collection</code> returned
     * may contain duplicate names (same name and name type).<br />
     * <br />
     * Each name in the <code>Collection</code> may be specified either as a
     * <code>String</code> or as an ASN.1 encoded byte array. For more details
     * about the formats used, see {@link #addPathToName(int type, String name) 
     * addPathToName(int type, String name)} and
     * {@link #addPathToName(int type, byte [] name)  addPathToName(int type,
     * byte [] name)}.<br />
     * <br />
     * Note that a deep copy is performed on the <code>Collection</code> to
     * protect against subsequent modifications.
     * 
     * @return a <code>Collection</code> of names (or <code>null</code>)
     * 
     * @see #setPathToNames
     */
    public Collection getPathToNames()
    {
        if (pathToNames == null)
        {
            return null;
        }

        Set returnPathToNames = new HashSet();
        List returnList;
        Iterator iter = pathToNames.iterator();
        List obj;

        while (iter.hasNext())
        {
            obj = (List)iter.next();
            returnList = new ArrayList();
            returnList.add(obj.get(0));
            if (obj.get(1) instanceof byte[])
            {
                returnList.add(((byte[])obj.get(1)).clone());
            }
            else
            {
                returnList.add(obj.get(1));
            }
            returnPathToNames.add(returnList);
        }

        return returnPathToNames;
    }

    /**
     * Return a printable representation of the <code>CertSelector</code>.<br />
     * <br />
     * <b>TODO: implement output for currently unsupported options(name
     * constraints)</b><br />
     * <br />
     * Uses {@link org.spongycastle.asn1.ASN1InputStream ASN1InputStream},
     * {@link org.spongycastle.asn1.ASN1Object ASN1Object},
     * {@link org.spongycastle.asn1.x509.KeyPurposeId KeyPurposeId}
     * 
     * @return a <code>String</code> describing the contents of the
     *         <code>CertSelector</code>
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("X509CertSelector: [\n");
        if (x509Cert != null)
        {
            sb.append("  Certificate: ").append(x509Cert).append('\n');
        }
        if (serialNumber != null)
        {
            sb.append("  Serial Number: ").append(serialNumber).append('\n');
        }
        if (issuerDN != null)
        {
            sb.append("  Issuer: ").append(getIssuerAsString()).append('\n');
        }
        if (subjectDN != null)
        {
            sb.append("  Subject: ").append(getSubjectAsString()).append('\n');
        }
        try
        {
            if (subjectKeyID != null)
            {
                ByteArrayInputStream inStream = new ByteArrayInputStream(
                        subjectKeyID);
                ASN1InputStream derInStream = new ASN1InputStream(inStream);
                ASN1Object derObject = derInStream.readObject();
                sb.append("  Subject Key Identifier: ")
                       .append(ASN1Dump.dumpAsString(derObject)).append('\n');
            }
            if (authorityKeyID != null)
            {
                ByteArrayInputStream inStream = new ByteArrayInputStream(
                        authorityKeyID);
                ASN1InputStream derInStream = new ASN1InputStream(inStream);
                ASN1Object derObject = derInStream.readObject();
                sb.append("  Authority Key Identifier: ")
                       .append(ASN1Dump.dumpAsString(derObject)).append('\n');
            }
        }
        catch (IOException ex)
        {
            sb.append(ex.getMessage()).append('\n');
        }
        if (certValid != null)
        {
            sb.append("  Certificate Valid: ").append(certValid).append('\n');
        }
        if (privateKeyValid != null)
        {
            sb.append("  Private Key Valid: ").append(privateKeyValid)
                   .append('\n');
        }
        if (subjectKeyAlgID != null)
        {
            sb.append("  Subject Public Key AlgID: ")
                   .append(subjectKeyAlgID).append('\n');
        }
        if (subjectPublicKey != null)
        {
            sb.append("  Subject Public Key: ").append(subjectPublicKey)
                   .append('\n');
        }
        if (keyUsage != null)
        {
            sb.append("  Key Usage: ").append(keyUsage).append('\n');
        }
        if (keyPurposeSet != null)
        {
            sb.append("  Extended Key Usage: ").append(keyPurposeSet)
                   .append('\n');
        }
        if (policy != null)
        {
            sb.append("  Policy: ").append(policy).append('\n');
        }
        sb.append("  matchAllSubjectAltNames flag: ")
               .append(matchAllSubjectAltNames).append('\n');
        if (subjectAltNamesByte != null)
        {
            sb.append("   SubjectAlternativNames: \n[");
            Iterator iter = subjectAltNamesByte.iterator();
            List obj;
            try
            {
                while (iter.hasNext())
                {
                    obj = (List)iter.next();
                    ByteArrayInputStream inStream = new ByteArrayInputStream(
                            (byte[])obj.get(1));
                    ASN1InputStream derInStream = new ASN1InputStream(inStream);
                    ASN1Object derObject = derInStream.readObject();
                    sb.append("  Type: ").append(obj.get(0)).append(" Data: ")
                           .append(ASN1Dump.dumpAsString(derObject)).append('\n');
                }
            }
            catch (IOException ex)
            {
                sb.append(ex.getMessage()).append('\n');
            }
            sb.append("]\n");
        }
        if (pathToNamesByte != null)
        {
            sb.append("   PathToNamesNames: \n[");
            Iterator iter = pathToNamesByte.iterator();
            List obj;
            try
            {
                while (iter.hasNext())
                {
                    obj = (List)iter.next();
                    ByteArrayInputStream inStream = new ByteArrayInputStream(
                            (byte[])obj.get(1));
                    ASN1InputStream derInStream = new ASN1InputStream(inStream);
                    ASN1Object derObject = derInStream.readObject();
                    sb.append("  Type: ").append(obj.get(0)).append(" Data: ")
                           .append(ASN1Dump.dumpAsString(derObject)).append('\n');
                }
            }
            catch (IOException ex)
            {
                sb.append(ex.getMessage()).append('\n');
            }
            sb.append("]\n");
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Decides whether a <code>Certificate</code> should be selected.<br />
     * <br />
     * <b>TODO: implement missing tests (name constraints and path to names)</b><br />
     * <br />
     * Uses {@link org.spongycastle.asn1.ASN1InputStream ASN1InputStream},
     * {@link org.spongycastle.asn1.ASN1Sequence ASN1Sequence},
     * {@link org.spongycastle.asn1.ASN1ObjectIdentifier ASN1ObjectIdentifier},
     * {@link org.spongycastle.asn1.ASN1Object ASN1Object},
     * {@link org.spongycastle.asn1.DERGeneralizedTime DERGeneralizedTime},
     * {@link org.spongycastle.asn1.x509.X509Name X509Name},
     * {@link org.spongycastle.asn1.x509.X509Extensions X509Extensions},
     * {@link org.spongycastle.asn1.x509.ExtendedKeyUsage ExtendedKeyUsage},
     * {@link org.spongycastle.asn1.x509.KeyPurposeId KeyPurposeId},
     * {@link org.spongycastle.asn1.x509.SubjectPublicKeyInfo SubjectPublicKeyInfo},
     * {@link org.spongycastle.asn1.x509.AlgorithmIdentifier AlgorithmIdentifier}
     * to access X509 extensions
     * 
     * @param cert
     *            the <code>Certificate</code> to be checked
     * 
     * @return <code>true</code> if the <code>Certificate</code> should be
     *         selected, <code>false</code> otherwise
     */
    public boolean match(Certificate cert)
    {
        boolean[] booleanArray;
        List tempList;
        Iterator tempIter;

        if (!(cert instanceof X509Certificate))
        {
            return false;
        }
        X509Certificate certX509 = (X509Certificate)cert;

        if (x509Cert != null && !x509Cert.equals(certX509))
        {
            return false;
        }
        if (serialNumber != null
                && !serialNumber.equals(certX509.getSerialNumber()))
        {
            return false;
        }
        try
        {
            if (issuerDNX509 != null)
            {
                if (!issuerDNX509.equals(PrincipalUtil
                        .getIssuerX509Principal(certX509), true))
                {
                    return false;
                }
            }
            if (subjectDNX509 != null)
            {
                if (!subjectDNX509.equals(PrincipalUtil
                        .getSubjectX509Principal(certX509), true))
                {
                    return false;
                }
            }
        }
        catch (Exception ex)
        {
            return false;
        }
        if (subjectKeyID != null)
        {
            byte[] data = certX509
                    .getExtensionValue(X509Extensions.SubjectKeyIdentifier
                            .getId());
            if (data == null)
            {
                return false;
            }
            try
            {
                ByteArrayInputStream inStream = new ByteArrayInputStream(data);
                ASN1InputStream derInputStream = new ASN1InputStream(inStream);
                byte[] testData = ((ASN1OctetString)derInputStream.readObject())
                        .getOctets();
                if (!Arrays.equals(subjectKeyID, testData))
                {
                    return false;
                }
            }
            catch (IOException ex)
            {
                return false;
            }
        }
        if (authorityKeyID != null)
        {
            byte[] data = certX509
                    .getExtensionValue(X509Extensions.AuthorityKeyIdentifier
                            .getId());
            if (data == null)
            {
                return false;
            }
            try
            {
                ByteArrayInputStream inStream = new ByteArrayInputStream(data);
                ASN1InputStream derInputStream = new ASN1InputStream(inStream);
                byte[] testData = ((ASN1OctetString)derInputStream.readObject())
                        .getOctets();
                if (!Arrays.equals(authorityKeyID, testData))
                {
                    return false;
                }
            }
            catch (IOException ex)
            {
                return false;
            }
        }
        if (certValid != null)
        {
            if (certX509.getNotAfter() != null
                    && certValid.after(certX509.getNotAfter()))
            {
                return false;
            }
            if (certX509.getNotBefore() != null
                    && certValid.before(certX509.getNotBefore()))
            {
                return false;
            }
        }
        if (privateKeyValid != null)
        {
            try
            {
                byte[] data = certX509
                        .getExtensionValue(X509Extensions.PrivateKeyUsagePeriod
                                .getId());
                if (data != null)
                {
                    ByteArrayInputStream inStream = new ByteArrayInputStream(
                            data);
                    ASN1InputStream derInputStream = new ASN1InputStream(inStream);
                    inStream = new ByteArrayInputStream(
                            ((ASN1OctetString)derInputStream.readObject())
                                    .getOctets());
                    derInputStream = new ASN1InputStream(inStream);
                    // TODO fix this, Sequence contains tagged objects
                    ASN1Sequence derObject = (ASN1Sequence)derInputStream
                            .readObject();
                    DERGeneralizedTime derDate = DERGeneralizedTime
                            .getInstance(derObject.getObjectAt(0));
                    SimpleDateFormat dateF = new SimpleDateFormat(
                            "yyyyMMddHHmmssZ");
                    if (privateKeyValid.before(dateF.parse(derDate.getTime())))
                    {
                        return false;
                    }
                    derDate = DERGeneralizedTime.getInstance(derObject
                            .getObjectAt(1));
                    if (privateKeyValid.after(dateF.parse(derDate.getTime())))
                    {
                        return false;
                    }
                }
            }
            catch (Exception ex)
            {
                return false;
            }
        }
        if (subjectKeyAlgID != null)
        {
            try
            {
                ByteArrayInputStream inStream = new ByteArrayInputStream(
                        certX509.getPublicKey().getEncoded());
                ASN1InputStream derInputStream = new ASN1InputStream(inStream);
                SubjectPublicKeyInfo publicKeyInfo = new SubjectPublicKeyInfo(
                        (ASN1Sequence)derInputStream.readObject());
                AlgorithmIdentifier algInfo = publicKeyInfo.getAlgorithmId();
                if (!algInfo.getObjectId().equals(subjectKeyAlgID))
                {
                    return false;
                }
            }
            catch (Exception ex)
            {
                return false;
            }
        }
        if (subjectPublicKeyByte != null)
        {
            if (!Arrays.equals(subjectPublicKeyByte, certX509.getPublicKey()
                    .getEncoded()))
            {
                return false;
            }
        }
        if (subjectPublicKey != null)
        {
            if (!subjectPublicKey.equals(certX509.getPublicKey()))
            {
                return false;
            }
        }
        if (keyUsage != null)
        {
            booleanArray = certX509.getKeyUsage();
            if (booleanArray != null)
            {
                for (int i = 0; i < keyUsage.length; i++)
                {
                    if (keyUsage[i]
                            && (booleanArray.length <= i || !booleanArray[i]))
                    {
                        return false;
                    }
                }
            }
        }
        if (keyPurposeSet != null && !keyPurposeSet.isEmpty())
        {
            try
            {
                byte[] data = certX509
                        .getExtensionValue(X509Extensions.ExtendedKeyUsage
                                .getId());
                if (data != null)
                {
                    ByteArrayInputStream inStream = new ByteArrayInputStream(
                            data);
                    ASN1InputStream derInputStream = new ASN1InputStream(inStream);
                    ExtendedKeyUsage extendedKeyUsage = ExtendedKeyUsage.getInstance(
                            derInputStream.readObject());
                    tempIter = keyPurposeSet.iterator();
                    while (tempIter.hasNext())
                    {
                        if (!extendedKeyUsage
                                .hasKeyPurposeId((KeyPurposeId)tempIter.next()))
                        {
                            return false;
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                return false;
            }
        }
        if (minMaxPathLen != -1)
        {
            if (minMaxPathLen == -2 && certX509.getBasicConstraints() != -1)
            {
                return false;
            }
            if (minMaxPathLen >= 0
                    && certX509.getBasicConstraints() < minMaxPathLen)
            {
                return false;
            }
        }
        if (policyOID != null)
        {
            try
            {
                byte[] data = certX509
                        .getExtensionValue(X509Extensions.CertificatePolicies
                                .getId());
                if (data == null)
                {
                    return false;
                }
                if (!policyOID.isEmpty())
                {
                    ByteArrayInputStream inStream = new ByteArrayInputStream(
                            data);
                    ASN1InputStream derInputStream = new ASN1InputStream(inStream);
                    inStream = new ByteArrayInputStream(
                            ((ASN1OctetString)derInputStream.readObject())
                                    .getOctets());
                    derInputStream = new ASN1InputStream(inStream);
                    Enumeration policySequence = ((ASN1Sequence)derInputStream
                            .readObject()).getObjects();
                    ASN1Sequence policyObject;
                    boolean test = false;
                    while (policySequence.hasMoreElements() && !test)
                    {
                        policyObject = (ASN1Sequence)policySequence
                                .nextElement();
                        if (policyOID.contains(policyObject.getObjectAt(0)))
                        {
                            test = true;
                        }
                    }
                    if (!test)
                    {
                        return false;
                    }
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                return false;
            }
        }
        if (subjectAltNamesByte != null)
        {
            try
            {
                byte[] data = certX509
                        .getExtensionValue(X509Extensions.SubjectAlternativeName
                                .getId());
                if (data == null)
                {
                    return false;
                }
                ByteArrayInputStream inStream = new ByteArrayInputStream(data);
                ASN1InputStream derInputStream = new ASN1InputStream(inStream);
                inStream = new ByteArrayInputStream(
                        ((ASN1OctetString)derInputStream.readObject())
                                .getOctets());
                derInputStream = new ASN1InputStream(inStream);
                Enumeration altNamesSequence = ((ASN1Sequence)derInputStream
                        .readObject()).getObjects();
                ASN1TaggedObject altNameObject;
                boolean test = false;
                Set testSet = new HashSet(subjectAltNamesByte);
                List testList;
                ASN1Object derData;
                ByteArrayOutputStream outStream;
                DEROutputStream derOutStream;
                while (altNamesSequence.hasMoreElements() && !test)
                {
                    altNameObject = (ASN1TaggedObject)altNamesSequence
                            .nextElement();
                    testList = new ArrayList(2);
                    testList.add(Integers.valueOf(altNameObject.getTagNo()));
                    derData = altNameObject.getObject();
                    outStream = new ByteArrayOutputStream();
                    derOutStream = new DEROutputStream(outStream);
                    derOutStream.writeObject(derData);
                    derOutStream.close();
                    testList.add(outStream.toByteArray());

                    if (testSet.remove(testList))
                    {
                        test = true;
                    }

                    if (matchAllSubjectAltNames && !testSet.isEmpty())
                    {
                        test = false;
                    }
                }
                if (!test)
                {
                    return false;
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
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
            X509CertSelector copy = (X509CertSelector)super.clone();
            if (issuerDN instanceof byte[])
            {
                copy.issuerDN = ((byte[])issuerDN).clone();
            }
            if (subjectDN instanceof byte[])
            {
                copy.subjectDN = ((byte[])subjectDN).clone();
            }
            if (subjectKeyID != null)
            {
                copy.subjectKeyID = (byte[])subjectKeyID.clone();
            }
            if (authorityKeyID != null)
            {
                copy.authorityKeyID = (byte[])authorityKeyID.clone();
            }
            if (subjectPublicKeyByte != null)
            {
                copy.subjectPublicKeyByte = (byte[])subjectPublicKeyByte
                        .clone();
            }
            if (keyUsage != null)
            {
                copy.keyUsage = (boolean[])keyUsage.clone();
            }
            if (keyPurposeSet != null)
            {
                copy.keyPurposeSet = new HashSet(keyPurposeSet);
            }
            if (policy != null)
            {
                copy.policy = new HashSet(policy);
                copy.policyOID = new HashSet();
                Iterator iter = policyOID.iterator();
                while (iter.hasNext())
                {
                    copy.policyOID.add(new ASN1ObjectIdentifier(
                            ((ASN1ObjectIdentifier)iter.next()).getId()));
                }
            }
            if (subjectAltNames != null)
            {
                copy.subjectAltNames = new HashSet(getSubjectAlternativeNames());
                Iterator iter = subjectAltNamesByte.iterator();
                List obj;
                List cloneObj;
                while (iter.hasNext())
                {
                    obj = (List)iter.next();
                    cloneObj = new ArrayList();
                    cloneObj.add(obj.get(0));
                    cloneObj.add(((byte[])obj.get(1)).clone());
                    copy.subjectAltNamesByte.add(cloneObj);
                }
            }
            if (pathToNames != null)
            {
                copy.pathToNames = new HashSet(getPathToNames());
                Iterator iter = pathToNamesByte.iterator();
                List obj;
                List cloneObj;
                while (iter.hasNext())
                {
                    obj = (List)iter.next();
                    cloneObj = new ArrayList();
                    cloneObj.add(obj.get(0));
                    cloneObj.add(((byte[])obj.get(1)).clone());
                    copy.pathToNamesByte.add(cloneObj);
                }
            }
            return copy;
        }
        catch (CloneNotSupportedException e)
        {
            /* Cannot happen */
            throw new InternalError(e.toString());
        }
    }
}
