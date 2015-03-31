package org.spongycastle.cert;

import java.io.IOException;
import java.io.OutputStream;

import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.x509.AuthorityKeyIdentifier;
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.asn1.x509.GeneralName;
import org.spongycastle.asn1.x509.GeneralNames;
import org.spongycastle.asn1.x509.SubjectKeyIdentifier;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.operator.DigestCalculator;

/**
 * General utility class for creating calculated extensions using the standard methods.
 * <p>
 * <b>Note:</b> This class is not thread safe!
 * </p>
 */
public class X509ExtensionUtils
{
    private DigestCalculator calculator;

    public X509ExtensionUtils(DigestCalculator calculator)
    {
        this.calculator = calculator;
    }

    public AuthorityKeyIdentifier createAuthorityKeyIdentifier(
        X509CertificateHolder certHolder)
    {
        if (certHolder.getVersionNumber() != 3)
        {
            GeneralName genName = new GeneralName(certHolder.getIssuer());
            SubjectPublicKeyInfo info = certHolder.getSubjectPublicKeyInfo();

            return new AuthorityKeyIdentifier(
                           calculateIdentifier(info), new GeneralNames(genName), certHolder.getSerialNumber());
        }
        else
        {
            GeneralName             genName = new GeneralName(certHolder.getIssuer());
            Extension ext = certHolder.getExtension(Extension.subjectKeyIdentifier);

            if (ext != null)
            {
                ASN1OctetString str = ASN1OctetString.getInstance(ext.getParsedValue());

                return new AuthorityKeyIdentifier(
                                str.getOctets(), new GeneralNames(genName), certHolder.getSerialNumber());
            }
            else
            {
                SubjectPublicKeyInfo info = certHolder.getSubjectPublicKeyInfo();

                return new AuthorityKeyIdentifier(
                        calculateIdentifier(info), new GeneralNames(genName), certHolder.getSerialNumber());
            }
        }
    }

    public AuthorityKeyIdentifier createAuthorityKeyIdentifier(SubjectPublicKeyInfo publicKeyInfo)
    {
        return new AuthorityKeyIdentifier(calculateIdentifier(publicKeyInfo));
    }

    /**
     * Return a RFC 3280 type 1 key identifier. As in:
     * <pre>
     * (1) The keyIdentifier is composed of the 160-bit SHA-1 hash of the
     * value of the BIT STRING subjectPublicKey (excluding the tag,
     * length, and number of unused bits).
     * </pre>
     * @param publicKeyInfo the key info object containing the subjectPublicKey field.
     * @return the key identifier.
     */
    public SubjectKeyIdentifier createSubjectKeyIdentifier(
        SubjectPublicKeyInfo publicKeyInfo)
    {
        return new SubjectKeyIdentifier(calculateIdentifier(publicKeyInfo));
    }

    /**
     * Return a RFC 3280 type 2 key identifier. As in:
     * <pre>
     * (2) The keyIdentifier is composed of a four bit type field with
     * the value 0100 followed by the least significant 60 bits of the
     * SHA-1 hash of the value of the BIT STRING subjectPublicKey.
     * </pre>
     * @param publicKeyInfo the key info object containing the subjectPublicKey field.
     * @return the key identifier.
     */
    public SubjectKeyIdentifier createTruncatedSubjectKeyIdentifier(SubjectPublicKeyInfo publicKeyInfo)
    {
        byte[] digest = calculateIdentifier(publicKeyInfo);
        byte[] id = new byte[8];

        System.arraycopy(digest, digest.length - 8, id, 0, id.length);

        id[0] &= 0x0f;
        id[0] |= 0x40;

        return new SubjectKeyIdentifier(id);
    }

    private byte[] calculateIdentifier(SubjectPublicKeyInfo publicKeyInfo)
    {
        byte[] bytes = publicKeyInfo.getPublicKeyData().getBytes();

        OutputStream cOut = calculator.getOutputStream();

        try
        {
            cOut.write(bytes);

            cOut.close();
        }
        catch (IOException e)
        {   // it's hard to imagine this happening, but yes it does!
            throw new CertRuntimeException("unable to calculate identifier: " + e.getMessage(), e);
        }

        return calculator.getDigest();
    }
}
