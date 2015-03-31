package java.security.cert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1Object;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.DEROutputStream;
import org.spongycastle.asn1.util.ASN1Dump;

/**
 * An immutable policy qualifier represented by the ASN.1 PolicyQualifierInfo
 * structure.<br />
 * <br />
 * The ASN.1 definition is as follows:<br />
 * <br />
 * 
 * <pre>
 *    PolicyQualifierInfo ::= SEQUENCE {
 *         policyQualifierId       PolicyQualifierId,
 *         qualifier               ANY DEFINED BY policyQualifierId }
 * </pre>
 * 
 * <br />
 * <br />
 * A certificate policies extension, if present in an X.509 version 3
 * certificate, contains a sequence of one or more policy information terms,
 * each of which consists of an object identifier (OID) and optional qualifiers.
 * In an end-entity certificate, these policy information terms indicate the
 * policy under which the certificate has been issued and the purposes for which
 * the certificate may be used. In a CA certificate, these policy information
 * terms limit the set of policies for certification paths which include this
 * certificate.<br />
 * <br />
 * A <code>Set</code> of <code>PolicyQualifierInfo</code> objects are
 * returned by the
 * {@link PolicyNode#getPolicyQualifiers PolicyNode.getPolicyQualifiers} method.
 * This allows applications with specific policy requirements to process and
 * validate each policy qualifier. Applications that need to process policy
 * qualifiers should explicitly set the <code>policyQualifiersRejected</code>
 * flag to false (by calling the
 * {@link PKIXParameters#setPolicyQualifiersRejected 
 * PKIXParameters.setPolicyQualifiersRejected} method) before validating a
 * certification path.<br />
 * <br />
 * Note that the PKIX certification path validation algorithm specifies that any
 * policy qualifier in a certificate policies extension that is marked critical
 * must be processed and validated. Otherwise the certification path must be
 * rejected. If the <code>policyQualifiersRejected</code> flag is set to
 * false, it is up to the application to validate all policy qualifiers in this
 * manner in order to be PKIX compliant.<br />
 * <br />
 * <b>Concurrent Access</b><br />
 * <br />
 * All <code>PolicyQualifierInfo</code> objects must be immutable and
 * thread-safe. That is, multiple threads may concurrently invoke the methods
 * defined in this class on a single <code>PolicyQualifierInfo</code> object
 * (or more than one) with no ill effects. Requiring
 * <code>PolicyQualifierInfo</code> objects to be immutable and thread-safe
 * allows them to be passed around to various pieces of code without worrying
 * about coordinating access.<br />
 * <br />
 * Uses {@link org.spongycastle.asn1.ASN1InputStream ASN1InputStream},
 * {@link org.spongycastle.asn1.ASN1Sequence ASN1Sequence},
 * {@link org.spongycastle.asn1.ASN1ObjectIdentifier ASN1ObjectIdentifier},
 * {@link org.spongycastle.asn1.DEROutputStream DEROutputStream},
 * {@link org.spongycastle.asn1.ASN1Object ASN1Object}
 */
public final class PolicyQualifierInfo
{
    private String id;

    private byte[] encoded;

    private byte[] qualifier;

    /**
     * Creates an instance of <code>PolicyQualifierInfo</code> from the
     * encoded bytes. The encoded byte array is copied on construction.<br />
     * <br />
     * Uses {@link org.spongycastle.asn1.ASN1InputStream ASN1InputStream},
     * {@link org.spongycastle.asn1.ASN1Sequence ASN1Sequence},
     * {@link org.spongycastle.asn1.ASN1ObjectIdentifier ASN1ObjectIdentifier} and
     * {@link org.spongycastle.asn1.DEROutputStream DEROutputStream}
     * 
     * @param encoded
     *            a byte array containing the qualifier in DER encoding
     * 
     * @exception IOException
     *                thrown if the byte array does not represent a valid and
     *                parsable policy qualifier
     */
    public PolicyQualifierInfo(byte[] encoded) throws IOException
    {
        this.encoded = (byte[])encoded.clone();
        try
        {
            ByteArrayInputStream inStream = new ByteArrayInputStream(
                    this.encoded);
            ASN1InputStream derInStream = new ASN1InputStream(inStream);
            ASN1Sequence obj = (ASN1Sequence)derInStream.readObject();
            id = ((ASN1ObjectIdentifier)obj.getObjectAt(0)).getId();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DEROutputStream derOutStream = new DEROutputStream(outStream);

            derOutStream.writeObject(obj.getObjectAt(1));
            derOutStream.close();

            qualifier = outStream.toByteArray();
        }
        catch (Exception ex)
        {
            throw new IOException("parsing exception : " + ex.toString());
        }
    }

    /**
     * Returns the <code>policyQualifierId</code> field of this
     * <code>PolicyQualifierInfo</code>. The <code>policyQualifierId</code>
     * is an Object Identifier (OID) represented by a set of nonnegative
     * integers separated by periods.
     * 
     * @return the OID (never <code>null</code>)
     */
    public String getPolicyQualifierId()
    {
        return id;
    }

    /**
     * Returns the ASN.1 DER encoded form of this
     * <code>PolicyQualifierInfo</code>.
     * 
     * @return the ASN.1 DER encoded bytes (never <code>null</code>). Note
     *         that a copy is returned, so the data is cloned each time this
     *         method is called.
     */
    public byte[] getEncoded()
    {
        return (byte[])encoded.clone();
    }

    /**
     * Returns the ASN.1 DER encoded form of the <code>qualifier</code> field
     * of this <code>PolicyQualifierInfo</code>.
     * 
     * @return the ASN.1 DER encoded bytes of the <code>qualifier</code>
     *         field. Note that a copy is returned, so the data is cloned each
     *         time this method is called.
     */
    public byte[] getPolicyQualifier()
    {
        if (qualifier == null)
        {
            return null;
        }

        return (byte[])qualifier.clone();
    }

    /**
     * Return a printable representation of this
     * <code>PolicyQualifierInfo</code>.<br />
     * <br />
     * Uses {@link org.spongycastle.asn1.ASN1InputStream ASN1InputStream},
     * {@link org.spongycastle.asn1.ASN1Object ASN1Object}
     * 
     * @return a <code>String</code> describing the contents of this
     *         <code>PolicyQualifierInfo</code>
     */
    public String toString()
    {
        StringBuffer s = new StringBuffer();
        s.append("PolicyQualifierInfo: [\n");
        s.append("qualifierID: ").append(id).append('\n');
        try
        {
            ByteArrayInputStream inStream = new ByteArrayInputStream(qualifier);
            ASN1InputStream derInStream = new ASN1InputStream(inStream);
            ASN1Object derObject = derInStream.readObject();
            s
                    .append("  qualifier:\n").append(ASN1Dump.dumpAsString(derObject))
                    .append('\n');
        }
        catch (IOException ex)
        {
            s.append(ex.getMessage());
        }
        s.append("qualifier: ").append(id).append('\n');
        s.append(']');
        return s.toString();
    }
}
