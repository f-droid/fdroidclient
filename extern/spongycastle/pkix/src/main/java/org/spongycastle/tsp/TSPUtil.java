package org.spongycastle.tsp;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Set;
import org.spongycastle.asn1.cms.Attribute;
import org.spongycastle.asn1.cms.AttributeTable;
import org.spongycastle.asn1.cms.ContentInfo;
import org.spongycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.spongycastle.asn1.nist.NISTObjectIdentifiers;
import org.spongycastle.asn1.oiw.OIWObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.teletrust.TeleTrusTObjectIdentifiers;
import org.spongycastle.asn1.x509.ExtendedKeyUsage;
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.asn1.x509.Extensions;
import org.spongycastle.asn1.x509.ExtensionsGenerator;
import org.spongycastle.asn1.x509.KeyPurposeId;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cms.SignerInformation;
import org.spongycastle.operator.DigestCalculator;
import org.spongycastle.operator.DigestCalculatorProvider;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.Integers;

public class TSPUtil
{
    private static List EMPTY_LIST = Collections.unmodifiableList(new ArrayList());

    private static final Map digestLengths = new HashMap();
    private static final Map digestNames = new HashMap();

    static
    {
        digestLengths.put(PKCSObjectIdentifiers.md5.getId(), Integers.valueOf(16));
        digestLengths.put(OIWObjectIdentifiers.idSHA1.getId(), Integers.valueOf(20));
        digestLengths.put(NISTObjectIdentifiers.id_sha224.getId(), Integers.valueOf(28));
        digestLengths.put(NISTObjectIdentifiers.id_sha256.getId(), Integers.valueOf(32));
        digestLengths.put(NISTObjectIdentifiers.id_sha384.getId(), Integers.valueOf(48));
        digestLengths.put(NISTObjectIdentifiers.id_sha512.getId(), Integers.valueOf(64));
        digestLengths.put(TeleTrusTObjectIdentifiers.ripemd128.getId(), Integers.valueOf(16));
        digestLengths.put(TeleTrusTObjectIdentifiers.ripemd160.getId(), Integers.valueOf(20));
        digestLengths.put(TeleTrusTObjectIdentifiers.ripemd256.getId(), Integers.valueOf(32));
        digestLengths.put(CryptoProObjectIdentifiers.gostR3411.getId(), Integers.valueOf(32));

        digestNames.put(PKCSObjectIdentifiers.md5.getId(), "MD5");
        digestNames.put(OIWObjectIdentifiers.idSHA1.getId(), "SHA1");
        digestNames.put(NISTObjectIdentifiers.id_sha224.getId(), "SHA224");
        digestNames.put(NISTObjectIdentifiers.id_sha256.getId(), "SHA256");
        digestNames.put(NISTObjectIdentifiers.id_sha384.getId(), "SHA384");
        digestNames.put(NISTObjectIdentifiers.id_sha512.getId(), "SHA512");
        digestNames.put(PKCSObjectIdentifiers.sha1WithRSAEncryption.getId(), "SHA1");
        digestNames.put(PKCSObjectIdentifiers.sha224WithRSAEncryption.getId(), "SHA224");
        digestNames.put(PKCSObjectIdentifiers.sha256WithRSAEncryption.getId(), "SHA256");
        digestNames.put(PKCSObjectIdentifiers.sha384WithRSAEncryption.getId(), "SHA384");
        digestNames.put(PKCSObjectIdentifiers.sha512WithRSAEncryption.getId(), "SHA512");
        digestNames.put(TeleTrusTObjectIdentifiers.ripemd128.getId(), "RIPEMD128");
        digestNames.put(TeleTrusTObjectIdentifiers.ripemd160.getId(), "RIPEMD160");
        digestNames.put(TeleTrusTObjectIdentifiers.ripemd256.getId(), "RIPEMD256");
        digestNames.put(CryptoProObjectIdentifiers.gostR3411.getId(), "GOST3411");
    }

     /**
     * Fetches the signature time-stamp attributes from a SignerInformation object.
     * Checks that the MessageImprint for each time-stamp matches the signature field.
     * (see RFC 3161 Appendix A).
     *
     * @param signerInfo a SignerInformation to search for time-stamps
     * @param digCalcProvider provider for digest calculators
     * @return a collection of TimeStampToken objects
     * @throws TSPValidationException
     */
    public static Collection getSignatureTimestamps(SignerInformation signerInfo, DigestCalculatorProvider digCalcProvider)
        throws TSPValidationException
    {
        List timestamps = new ArrayList();

        AttributeTable unsignedAttrs = signerInfo.getUnsignedAttributes();
        if (unsignedAttrs != null)
        {
            ASN1EncodableVector allTSAttrs = unsignedAttrs.getAll(
                PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
            for (int i = 0; i < allTSAttrs.size(); ++i)
            {
                Attribute tsAttr = (Attribute)allTSAttrs.get(i);
                ASN1Set tsAttrValues = tsAttr.getAttrValues();
                for (int j = 0; j < tsAttrValues.size(); ++j)
                {
                    try
                    {
                        ContentInfo contentInfo = ContentInfo.getInstance(tsAttrValues.getObjectAt(j));
                        TimeStampToken timeStampToken = new TimeStampToken(contentInfo);
                        TimeStampTokenInfo tstInfo = timeStampToken.getTimeStampInfo();

                        DigestCalculator digCalc = digCalcProvider.get(tstInfo.getHashAlgorithm());

                        OutputStream dOut = digCalc.getOutputStream();

                        dOut.write(signerInfo.getSignature());
                        dOut.close();

                        byte[] expectedDigest = digCalc.getDigest();

                        if (!Arrays.constantTimeAreEqual(expectedDigest, tstInfo.getMessageImprintDigest()))
                        {
                            throw new TSPValidationException("Incorrect digest in message imprint");
                        }

                        timestamps.add(timeStampToken);
                    }
                    catch (OperatorCreationException e)
                    {
                        throw new TSPValidationException("Unknown hash algorithm specified in timestamp");
                    }
                    catch (Exception e)
                    {
                        throw new TSPValidationException("Timestamp could not be parsed");
                    }
                }
            }
        }

        return timestamps;
    }

    /**
     * Validate the passed in certificate as being of the correct type to be used
     * for time stamping. To be valid it must have an ExtendedKeyUsage extension
     * which has a key purpose identifier of id-kp-timeStamping.
     *
     * @param cert the certificate of interest.
     * @throws TSPValidationException if the certificate fails on one of the check points.
     */
    public static void validateCertificate(
        X509CertificateHolder cert)
        throws TSPValidationException
    {
        if (cert.toASN1Structure().getVersionNumber() != 3)
        {
            throw new IllegalArgumentException("Certificate must have an ExtendedKeyUsage extension.");
        }

        Extension ext = cert.getExtension(Extension.extendedKeyUsage);
        if (ext == null)
        {
            throw new TSPValidationException("Certificate must have an ExtendedKeyUsage extension.");
        }

        if (!ext.isCritical())
        {
            throw new TSPValidationException("Certificate must have an ExtendedKeyUsage extension marked as critical.");
        }

        ExtendedKeyUsage    extKey = ExtendedKeyUsage.getInstance(ext.getParsedValue());

        if (!extKey.hasKeyPurposeId(KeyPurposeId.id_kp_timeStamping) || extKey.size() != 1)
        {
            throw new TSPValidationException("ExtendedKeyUsage not solely time stamping.");
        }
    }

    static int getDigestLength(
        String digestAlgOID)
        throws TSPException
    {
        Integer length = (Integer)digestLengths.get(digestAlgOID);

        if (length != null)
        {
            return length.intValue();
        }

        throw new TSPException("digest algorithm cannot be found.");
    }

    static List getExtensionOIDs(Extensions extensions)
    {
        if (extensions == null)
        {
            return EMPTY_LIST;
        }

        return Collections.unmodifiableList(java.util.Arrays.asList(extensions.getExtensionOIDs()));
    }

    static void addExtension(ExtensionsGenerator extGenerator, ASN1ObjectIdentifier oid, boolean isCritical, ASN1Encodable value)
        throws TSPIOException
    {
        try
        {
            extGenerator.addExtension(oid, isCritical, value);
        }
        catch (IOException e)
        {
            throw new TSPIOException("cannot encode extension: " + e.getMessage(), e);
        }
    }
}
