package org.spongycastle.cms;

import java.util.HashMap;
import java.util.Map;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.spongycastle.asn1.eac.EACObjectIdentifiers;
import org.spongycastle.asn1.nist.NISTObjectIdentifiers;
import org.spongycastle.asn1.oiw.OIWObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.teletrust.TeleTrusTObjectIdentifiers;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.X509ObjectIdentifiers;
import org.spongycastle.asn1.x9.X9ObjectIdentifiers;

public class DefaultCMSSignatureAlgorithmNameGenerator
    implements CMSSignatureAlgorithmNameGenerator
{
    private final Map encryptionAlgs = new HashMap();
    private final Map     digestAlgs = new HashMap();

    private void addEntries(ASN1ObjectIdentifier alias, String digest, String encryption)
    {
        digestAlgs.put(alias, digest);
        encryptionAlgs.put(alias, encryption);
    }

    public DefaultCMSSignatureAlgorithmNameGenerator()
    {
        addEntries(NISTObjectIdentifiers.dsa_with_sha224, "SHA224", "DSA");
        addEntries(NISTObjectIdentifiers.dsa_with_sha256, "SHA256", "DSA");
        addEntries(NISTObjectIdentifiers.dsa_with_sha384, "SHA384", "DSA");
        addEntries(NISTObjectIdentifiers.dsa_with_sha512, "SHA512", "DSA");
        addEntries(OIWObjectIdentifiers.dsaWithSHA1, "SHA1", "DSA");
        addEntries(OIWObjectIdentifiers.md4WithRSA, "MD4", "RSA");
        addEntries(OIWObjectIdentifiers.md4WithRSAEncryption, "MD4", "RSA");
        addEntries(OIWObjectIdentifiers.md5WithRSA, "MD5", "RSA");
        addEntries(OIWObjectIdentifiers.sha1WithRSA, "SHA1", "RSA");
        addEntries(PKCSObjectIdentifiers.md2WithRSAEncryption, "MD2", "RSA");
        addEntries(PKCSObjectIdentifiers.md4WithRSAEncryption, "MD4", "RSA");
        addEntries(PKCSObjectIdentifiers.md5WithRSAEncryption, "MD5", "RSA");
        addEntries(PKCSObjectIdentifiers.sha1WithRSAEncryption, "SHA1", "RSA");
        addEntries(PKCSObjectIdentifiers.sha224WithRSAEncryption, "SHA224", "RSA");
        addEntries(PKCSObjectIdentifiers.sha256WithRSAEncryption, "SHA256", "RSA");
        addEntries(PKCSObjectIdentifiers.sha384WithRSAEncryption, "SHA384", "RSA");
        addEntries(PKCSObjectIdentifiers.sha512WithRSAEncryption, "SHA512", "RSA");
        addEntries(X9ObjectIdentifiers.ecdsa_with_SHA1, "SHA1", "ECDSA");
        addEntries(X9ObjectIdentifiers.ecdsa_with_SHA224, "SHA224", "ECDSA");
        addEntries(X9ObjectIdentifiers.ecdsa_with_SHA256, "SHA256", "ECDSA");
        addEntries(X9ObjectIdentifiers.ecdsa_with_SHA384, "SHA384", "ECDSA");
        addEntries(X9ObjectIdentifiers.ecdsa_with_SHA512, "SHA512", "ECDSA");
        addEntries(X9ObjectIdentifiers.id_dsa_with_sha1, "SHA1", "DSA");
        addEntries(EACObjectIdentifiers.id_TA_ECDSA_SHA_1, "SHA1", "ECDSA");
        addEntries(EACObjectIdentifiers.id_TA_ECDSA_SHA_224, "SHA224", "ECDSA");
        addEntries(EACObjectIdentifiers.id_TA_ECDSA_SHA_256, "SHA256", "ECDSA");
        addEntries(EACObjectIdentifiers.id_TA_ECDSA_SHA_384, "SHA384", "ECDSA");
        addEntries(EACObjectIdentifiers.id_TA_ECDSA_SHA_512, "SHA512", "ECDSA");
        addEntries(EACObjectIdentifiers.id_TA_RSA_v1_5_SHA_1, "SHA1", "RSA");
        addEntries(EACObjectIdentifiers.id_TA_RSA_v1_5_SHA_256, "SHA256", "RSA");
        addEntries(EACObjectIdentifiers.id_TA_RSA_PSS_SHA_1, "SHA1", "RSAandMGF1");
        addEntries(EACObjectIdentifiers.id_TA_RSA_PSS_SHA_256, "SHA256", "RSAandMGF1");

        encryptionAlgs.put(X9ObjectIdentifiers.id_dsa, "DSA");
        encryptionAlgs.put(PKCSObjectIdentifiers.rsaEncryption, "RSA");
        encryptionAlgs.put(TeleTrusTObjectIdentifiers.teleTrusTRSAsignatureAlgorithm, "RSA");
        encryptionAlgs.put(X509ObjectIdentifiers.id_ea_rsa, "RSA");
        encryptionAlgs.put(PKCSObjectIdentifiers.id_RSASSA_PSS, "RSAandMGF1");
        encryptionAlgs.put(CryptoProObjectIdentifiers.gostR3410_94, "GOST3410");
        encryptionAlgs.put(CryptoProObjectIdentifiers.gostR3410_2001, "ECGOST3410");
        encryptionAlgs.put(new ASN1ObjectIdentifier("1.3.6.1.4.1.5849.1.6.2"), "ECGOST3410");
        encryptionAlgs.put(new ASN1ObjectIdentifier("1.3.6.1.4.1.5849.1.1.5"), "GOST3410");
        encryptionAlgs.put(CryptoProObjectIdentifiers.gostR3411_94_with_gostR3410_2001, "ECGOST3410");
        encryptionAlgs.put(CryptoProObjectIdentifiers.gostR3411_94_with_gostR3410_94, "GOST3410");

        digestAlgs.put(PKCSObjectIdentifiers.md2, "MD2");
        digestAlgs.put(PKCSObjectIdentifiers.md4, "MD4");
        digestAlgs.put(PKCSObjectIdentifiers.md5, "MD5");
        digestAlgs.put(OIWObjectIdentifiers.idSHA1, "SHA1");
        digestAlgs.put(NISTObjectIdentifiers.id_sha224, "SHA224");
        digestAlgs.put(NISTObjectIdentifiers.id_sha256, "SHA256");
        digestAlgs.put(NISTObjectIdentifiers.id_sha384, "SHA384");
        digestAlgs.put(NISTObjectIdentifiers.id_sha512, "SHA512");
        digestAlgs.put(TeleTrusTObjectIdentifiers.ripemd128, "RIPEMD128");
        digestAlgs.put(TeleTrusTObjectIdentifiers.ripemd160, "RIPEMD160");
        digestAlgs.put(TeleTrusTObjectIdentifiers.ripemd256, "RIPEMD256");
        digestAlgs.put(CryptoProObjectIdentifiers.gostR3411,  "GOST3411");
        digestAlgs.put(new ASN1ObjectIdentifier("1.3.6.1.4.1.5849.1.2.1"),  "GOST3411");
    }

    /**
     * Return the digest algorithm using one of the standard JCA string
     * representations rather than the algorithm identifier (if possible).
     */
    private String getDigestAlgName(
        ASN1ObjectIdentifier digestAlgOID)
    {
        String algName = (String)digestAlgs.get(digestAlgOID);

        if (algName != null)
        {
            return algName;
        }

        return digestAlgOID.getId();
    }

    /**
     * Return the digest encryption algorithm using one of the standard
     * JCA string representations rather the the algorithm identifier (if
     * possible).
     */
    private String getEncryptionAlgName(
        ASN1ObjectIdentifier encryptionAlgOID)
    {
        String algName = (String)encryptionAlgs.get(encryptionAlgOID);

        if (algName != null)
        {
            return algName;
        }

        return encryptionAlgOID.getId();
    }

    /**
     * Set the mapping for the encryption algorithm used in association with a SignedData generation
     * or interpretation.
     *
     * @param oid object identifier to map.
     * @param algorithmName algorithm name to use.
     */
    protected void setSigningEncryptionAlgorithmMapping(ASN1ObjectIdentifier oid, String algorithmName)
    {
        encryptionAlgs.put(oid, algorithmName);
    }

    /**
     * Set the mapping for the digest algorithm to use in conjunction with a SignedData generation
     * or interpretation.
     *
     * @param oid object identifier to map.
     * @param algorithmName algorithm name to use.
     */
    protected void setSigningDigestAlgorithmMapping(ASN1ObjectIdentifier oid, String algorithmName)
    {
        digestAlgs.put(oid, algorithmName);
    }

    public String getSignatureName(AlgorithmIdentifier digestAlg, AlgorithmIdentifier encryptionAlg)
    {
        return getDigestAlgName(digestAlg.getAlgorithm()) + "with" + getEncryptionAlgName(encryptionAlg.getAlgorithm());
    }
}
