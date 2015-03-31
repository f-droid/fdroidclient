package org.spongycastle.jce.cert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1Object;
import org.spongycastle.asn1.ASN1Sequence;

/**
 * A trust anchor or most-trusted Certification Authority (CA). <br />
 * <br />
 * This class represents a "most-trusted CA", which is used as a trust anchor
 * for validating X.509 certification paths. A most-trusted CA includes the
 * public key of the CA, the CA's name, and any constraints upon the set of
 * paths which may be validated using this key. These parameters can be
 * specified in the form of a trusted X509Certificate or as individual
 * parameters. <br />
 * <br />
 * <strong>Concurrent Access</strong><br />
 * <br />
 * All TrustAnchor objects must be immutable and thread-safe. That is, multiple
 * threads may concurrently invoke the methods defined in this class on a
 * single TrustAnchor object (or more than one) with no ill effects. Requiring
 * TrustAnchor objects to be immutable and thread-safe allows them to be passed
 * around to various pieces of code without worrying about coordinating access.
 * This stipulation applies to all public fields and methods of this class and
 * any added or overridden by subclasses.<br />
 * <br />
 * <b>TODO: implement better nameConstraints testing.</b>
 **/
public class TrustAnchor
{
    private X509Certificate trustCert = null;

    private PublicKey trustPublicKey = null;

    private String trustName = null;

    private byte[] nameConstraints = null;

    /**
     * Creates an instance of TrustAnchor with the specified X509Certificate and
     * optional name constraints, which are intended to be used as additional
     * constraints when validating an X.509 certification path.<br />
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
     * Note that the name constraints byte array supplied is cloned to protect
     * against subsequent modifications.
     * 
     * @param trustedCert
     *            a trusted X509Certificate
     * @param nameConstraints
     *            a byte array containing the ASN.1 DER encoding of a
     *            NameConstraints extension to be used for checking name
     *            constraints. Only the value of the extension is included, not
     *            the OID or criticality flag. Specify null to omit the
     *            parameter.
     * 
     * @exception IllegalArgumentException
     *                if the name constraints cannot be decoded
     * @exception NullPointerException
     *                if the specified X509Certificate is null
     */
    public TrustAnchor(X509Certificate trustedCert, byte[] nameConstraints)
    {
        if (trustedCert == null)
        {
            throw new NullPointerException("trustedCert must be non-null");
        }

        this.trustCert = trustedCert;
        if (nameConstraints != null)
        {
            this.nameConstraints = (byte[])nameConstraints.clone();
            checkNameConstraints(this.nameConstraints);
        }
    }

    /**
     * Creates an instance of <code>TrustAnchor</code> where the most-trusted
     * CA is specified as a distinguished name and public key. Name constraints
     * are an optional parameter, and are intended to be used as additional
     * constraints when validating an X.509 certification path.
     * 
     * The name constraints are specified as a byte array. This byte array
     * contains the DER encoded form of the name constraints, as they would
     * appear in the NameConstraints structure defined in RFC 2459 and X.509.
     * The ASN.1 notation for this structure is supplied in the documentation
     * for {@link #TrustAnchor(X509Certificate trustedCert, byte[]
     * nameConstraints) TrustAnchor(X509Certificate trustedCert, byte[]
     * nameConstraints) }.
     * 
     * Note that the name constraints byte array supplied here is cloned to
     * protect against subsequent modifications.
     * 
     * @param caName
     *            the X.500 distinguished name of the most-trusted CA in RFC
     *            2253 String format
     * @param pubKey
     *            the public key of the most-trusted CA
     * @param nameConstraints
     *            a byte array containing the ASN.1 DER encoding of a
     *            NameConstraints extension to be used for checking name
     *            constraints. Only the value of the extension is included, not
     *            the OID or criticality flag. Specify null to omit the
     *            parameter.
     * 
     * @exception IllegalArgumentException
     *                if the specified caName parameter is empty (<code>caName.length() == 0</code>)
     *                or incorrectly formatted or the name constraints cannot be
     *                decoded
     * @exception NullPointerException
     *                if the specified caName or pubKey parameter is null
     */
    public TrustAnchor(String caName, PublicKey pubKey, byte[] nameConstraints)
    {
        if (caName == null)
        {
            throw new NullPointerException("caName must be non-null");
        }
        if (pubKey == null)
        {
            throw new NullPointerException("pubKey must be non-null");
        }
        if (caName.length() == 0)
        {
            throw new IllegalArgumentException(
                    "caName can not be an empty string");
        }

        this.trustName = caName;
        this.trustPublicKey = pubKey;
        if (nameConstraints != null)
        {
            this.nameConstraints = (byte[])nameConstraints.clone();
            checkNameConstraints(this.nameConstraints);
        }
    }

    /**
     * Returns the most-trusted CA certificate.
     * 
     * @return a trusted <code>X509Certificate</code> or <code>null</code>
     *         if the trust anchor was not specified as a trusted certificate
     */
    public final X509Certificate getTrustedCert()
    {
        return trustCert;
    }

    /**
     * Returns the name of the most-trusted CA in RFC 2253 String format.
     * 
     * @return the X.500 distinguished name of the most-trusted CA, or
     *         <code>null</code> if the trust anchor was not specified as a
     *         trusted public key and name pair
     */
    public final String getCAName()
    {
        return trustName;
    }

    /**
     * Returns the public key of the most-trusted CA.
     * 
     * @return the public key of the most-trusted CA, or null if the trust
     *         anchor was not specified as a trusted public key and name pair
     */
    public final PublicKey getCAPublicKey()
    {
        return trustPublicKey;
    }

    /**
     * Returns the name constraints parameter. The specified name constraints
     * are associated with this trust anchor and are intended to be used as
     * additional constraints when validating an X.509 certification path.<br />
     * <br />
     * The name constraints are returned as a byte array. This byte array
     * contains the DER encoded form of the name constraints, as they would
     * appear in the NameConstraints structure defined in RFC 2459 and X.509.
     * The ASN.1 notation for this structure is supplied in the documentation
     * for <code>TrustAnchor(X509Certificate trustedCert, byte[]
     * nameConstraints)</code>.<br />
     * <br />
     * Note that the byte array returned is cloned to protect against subsequent
     * modifications.
     * 
     * @return a byte array containing the ASN.1 DER encoding of a
     *         NameConstraints extension used for checking name constraints, or
     *         <code>null</code> if not set.
     */
    public final byte[] getNameConstraints()
    {
        return (byte[])nameConstraints.clone();
    }

    /**
     * Returns a formatted string describing the <code>TrustAnchor</code>.
     * 
     * @return a formatted string describing the <code>TrustAnchor</code>
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("[\n");
        if (getCAPublicKey() != null)
        {
            sb.append("  Trusted CA Public Key: ").append(getCAPublicKey()).append('\n');
            sb.append("  Trusted CA Issuer Name: ").append(getCAName()).append('\n');
        }
        else
        {
            sb.append("  Trusted CA cert: ").append(getTrustedCert()).append('\n');
        }
        if (nameConstraints != null)
        {
            sb.append("  Name Constraints: ").append(nameConstraints).append('\n');
        }
        return sb.toString();
    }

    /**
     * Check given DER encoded nameConstraints for correct decoding. Currently
     * only basic DER decoding test.<br />
     * <br />
     * <b>TODO: implement more testing.</b>
     * 
     * @param data
     *            the DER encoded nameConstrains to be checked or
     *            <code>null</code>
     * @exception IllegalArgumentException
     *                if the check failed.
     */
    private void checkNameConstraints(byte[] data)
    {
        if (data != null)
        {
            try
            {
                ByteArrayInputStream inStream = new ByteArrayInputStream(data);
                ASN1InputStream derInStream = new ASN1InputStream(inStream);
                ASN1Object derObject = derInStream.readObject();
                if (!(derObject instanceof ASN1Sequence))
                {
                    throw new IllegalArgumentException(
                            "nameConstraints parameter decoding error");
                }
            }
            catch (IOException ex)
            {
                throw new IllegalArgumentException(
                        "nameConstraints parameter decoding error: " + ex);
            }
        }
    }
}