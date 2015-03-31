package org.spongycastle.operator.jcajce;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
//import java.security.spec.PSSParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.spongycastle.asn1.kisa.KISAObjectIdentifiers;
import org.spongycastle.asn1.nist.NISTObjectIdentifiers;
import org.spongycastle.asn1.ntt.NTTObjectIdentifiers;
import org.spongycastle.asn1.oiw.OIWObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.pkcs.RSASSAPSSparams;
import org.spongycastle.asn1.teletrust.TeleTrusTObjectIdentifiers;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.asn1.x9.X9ObjectIdentifiers;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.jcajce.JcaJceHelper;
import org.spongycastle.jcajce.JcaJceUtils;
import org.spongycastle.operator.OperatorCreationException;

class OperatorHelper
{
    private static final Map oids = new HashMap();
    private static final Map asymmetricWrapperAlgNames = new HashMap();
    private static final Map symmetricWrapperAlgNames = new HashMap();
    private static final Map symmetricKeyAlgNames = new HashMap();

    static
    {
        //
        // reverse mappings
        //
        oids.put(new ASN1ObjectIdentifier("1.2.840.113549.1.1.5"), "SHA1WITHRSA");
        oids.put(PKCSObjectIdentifiers.sha224WithRSAEncryption, "SHA224WITHRSA");
        oids.put(PKCSObjectIdentifiers.sha256WithRSAEncryption, "SHA256WITHRSA");
        oids.put(PKCSObjectIdentifiers.sha384WithRSAEncryption, "SHA384WITHRSA");
        oids.put(PKCSObjectIdentifiers.sha512WithRSAEncryption, "SHA512WITHRSA");
        oids.put(CryptoProObjectIdentifiers.gostR3411_94_with_gostR3410_94, "GOST3411WITHGOST3410");
        oids.put(CryptoProObjectIdentifiers.gostR3411_94_with_gostR3410_2001, "GOST3411WITHECGOST3410");

        oids.put(new ASN1ObjectIdentifier("1.2.840.113549.1.1.4"), "MD5WITHRSA");
        oids.put(new ASN1ObjectIdentifier("1.2.840.113549.1.1.2"), "MD2WITHRSA");
        oids.put(new ASN1ObjectIdentifier("1.2.840.10040.4.3"), "SHA1WITHDSA");
        oids.put(X9ObjectIdentifiers.ecdsa_with_SHA1, "SHA1WITHECDSA");
        oids.put(X9ObjectIdentifiers.ecdsa_with_SHA224, "SHA224WITHECDSA");
        oids.put(X9ObjectIdentifiers.ecdsa_with_SHA256, "SHA256WITHECDSA");
        oids.put(X9ObjectIdentifiers.ecdsa_with_SHA384, "SHA384WITHECDSA");
        oids.put(X9ObjectIdentifiers.ecdsa_with_SHA512, "SHA512WITHECDSA");
        oids.put(OIWObjectIdentifiers.sha1WithRSA, "SHA1WITHRSA");
        oids.put(OIWObjectIdentifiers.dsaWithSHA1, "SHA1WITHDSA");
        oids.put(NISTObjectIdentifiers.dsa_with_sha224, "SHA224WITHDSA");
        oids.put(NISTObjectIdentifiers.dsa_with_sha256, "SHA256WITHDSA");

        oids.put(OIWObjectIdentifiers.idSHA1, "SHA-1");
        oids.put(NISTObjectIdentifiers.id_sha224, "SHA-224");
        oids.put(NISTObjectIdentifiers.id_sha256, "SHA-256");
        oids.put(NISTObjectIdentifiers.id_sha384, "SHA-384");
        oids.put(NISTObjectIdentifiers.id_sha512, "SHA-512");
        oids.put(TeleTrusTObjectIdentifiers.ripemd128, "RIPEMD-128");
        oids.put(TeleTrusTObjectIdentifiers.ripemd160, "RIPEMD-160");
        oids.put(TeleTrusTObjectIdentifiers.ripemd256, "RIPEMD-256");

        asymmetricWrapperAlgNames.put(PKCSObjectIdentifiers.rsaEncryption, "RSA/ECB/PKCS1Padding");

        symmetricWrapperAlgNames.put(PKCSObjectIdentifiers.id_alg_CMS3DESwrap, "DESEDEWrap");
        symmetricWrapperAlgNames.put(PKCSObjectIdentifiers.id_alg_CMSRC2wrap, "RC2Wrap");
        symmetricWrapperAlgNames.put(NISTObjectIdentifiers.id_aes128_wrap, "AESWrap");
        symmetricWrapperAlgNames.put(NISTObjectIdentifiers.id_aes192_wrap, "AESWrap");
        symmetricWrapperAlgNames.put(NISTObjectIdentifiers.id_aes256_wrap, "AESWrap");
        symmetricWrapperAlgNames.put(NTTObjectIdentifiers.id_camellia128_wrap, "CamelliaWrap");
        symmetricWrapperAlgNames.put(NTTObjectIdentifiers.id_camellia192_wrap, "CamelliaWrap");
        symmetricWrapperAlgNames.put(NTTObjectIdentifiers.id_camellia256_wrap, "CamelliaWrap");
        symmetricWrapperAlgNames.put(KISAObjectIdentifiers.id_npki_app_cmsSeed_wrap, "SEEDWrap");
        symmetricWrapperAlgNames.put(PKCSObjectIdentifiers.des_EDE3_CBC, "DESede");

        symmetricKeyAlgNames.put(NISTObjectIdentifiers.aes, "AES");
        symmetricKeyAlgNames.put(NISTObjectIdentifiers.id_aes128_CBC, "AES");
        symmetricKeyAlgNames.put(NISTObjectIdentifiers.id_aes192_CBC, "AES");
        symmetricKeyAlgNames.put(NISTObjectIdentifiers.id_aes256_CBC, "AES");
        symmetricKeyAlgNames.put(PKCSObjectIdentifiers.des_EDE3_CBC, "DESede");
        symmetricKeyAlgNames.put(PKCSObjectIdentifiers.RC2_CBC, "RC2");
    }

    private JcaJceHelper helper;

    OperatorHelper(JcaJceHelper helper)
    {
        this.helper = helper;
    }

    Cipher createAsymmetricWrapper(ASN1ObjectIdentifier algorithm, Map extraAlgNames)
        throws OperatorCreationException
    {
        try
        {
            String cipherName = null;

            if (!extraAlgNames.isEmpty())
            {
                cipherName = (String)extraAlgNames.get(algorithm);
            }

            if (cipherName == null)
            {
                cipherName = (String)asymmetricWrapperAlgNames.get(algorithm);
            }

            if (cipherName != null)
            {
                try
                {
                    // this is reversed as the Sun policy files now allow unlimited strength RSA
                    return helper.createCipher(cipherName);
                }
                catch (NoSuchAlgorithmException e)
                {
                    // try alternate for RSA
                    if (cipherName.equals("RSA/ECB/PKCS1Padding"))
                    {
                        try
                        {
                            return helper.createCipher("RSA/NONE/PKCS1Padding");
                        }
                        catch (NoSuchAlgorithmException ex)
                        {
                            // Ignore
                        }
                    }
                    // Ignore
                }
            }

            return helper.createCipher(algorithm.getId());
        }
        catch (GeneralSecurityException e)
        {
            throw new OperatorCreationException("cannot create cipher: " + e.getMessage(), e);
        }
    }

    Cipher createSymmetricWrapper(ASN1ObjectIdentifier algorithm)
        throws OperatorCreationException
    {
        try
        {
            String cipherName = (String)symmetricWrapperAlgNames.get(algorithm);

            if (cipherName != null)
            {
                try
                {
                    // this is reversed as the Sun policy files now allow unlimited strength RSA
                    return helper.createCipher(cipherName);
                }
                catch (NoSuchAlgorithmException e)
                {
                    // Ignore
                }
            }
            return helper.createCipher(algorithm.getId());
        }
        catch (GeneralSecurityException e)
        {
            throw new OperatorCreationException("cannot create cipher: " + e.getMessage(), e);
        }
    }

    AlgorithmParameters createAlgorithmParameters(AlgorithmIdentifier cipherAlgId)
        throws OperatorCreationException
    {
        AlgorithmParameters parameters;

        if (cipherAlgId.getAlgorithm().equals(PKCSObjectIdentifiers.rsaEncryption))
        {
            return null;
        }

        try
        {
            parameters = helper.createAlgorithmParameters(cipherAlgId.getAlgorithm().getId());
        }
        catch (NoSuchAlgorithmException e)
        {
            return null;   // There's a good chance there aren't any!
        }
        catch (NoSuchProviderException e)
        {
            throw new OperatorCreationException("cannot create algorithm parameters: " + e.getMessage(), e);
        }

        try
        {
            parameters.init(cipherAlgId.getParameters().toASN1Primitive().getEncoded());
        }
        catch (IOException e)
        {
            throw new OperatorCreationException("cannot initialise algorithm parameters: " + e.getMessage(), e);
        }

        return parameters;
    }

    MessageDigest createDigest(AlgorithmIdentifier digAlgId)
        throws GeneralSecurityException
    {
        MessageDigest dig;

        try
        {
            dig = helper.createDigest(getDigestAlgName(digAlgId.getAlgorithm()));
        }
        catch (NoSuchAlgorithmException e)
        {
            //
            // try an alternate
            //
            if (oids.get(digAlgId.getAlgorithm()) != null)
            {
                String  digestAlgorithm = (String)oids.get(digAlgId.getAlgorithm());

                dig = helper.createDigest(digestAlgorithm);
            }
            else
            {
                throw e;
            }
        }

        return dig;
    }

    Signature createSignature(AlgorithmIdentifier sigAlgId)
        throws GeneralSecurityException
    {
        Signature   sig;

        try
        {
            sig = helper.createSignature(getSignatureName(sigAlgId));
        }
        catch (NoSuchAlgorithmException e)
        {
            //
            // try an alternate
            //
            if (oids.get(sigAlgId.getAlgorithm()) != null)
            {
                String  signatureAlgorithm = (String)oids.get(sigAlgId.getAlgorithm());

                sig = helper.createSignature(signatureAlgorithm);
            }
            else
            {
                throw e;
            }
        }

        return sig;
    }

    public Signature createRawSignature(AlgorithmIdentifier algorithm)
    {
        Signature   sig;

        try
        {
            String algName = getSignatureName(algorithm);

            algName = "NONE" + algName.substring(algName.indexOf("WITH"));

            sig = helper.createSignature(algName);

            // RFC 4056
            // When the id-RSASSA-PSS algorithm identifier is used for a signature,
            // the AlgorithmIdentifier parameters field MUST contain RSASSA-PSS-params.
/*
            if (algorithm.getAlgorithm().equals(PKCSObjectIdentifiers.id_RSASSA_PSS))
            {
                AlgorithmParameters params = helper.createAlgorithmParameters(algName);

                JcaJceUtils.loadParameters(params, algorithm.getParameters());

                PSSParameterSpec spec = (PSSParameterSpec)params.getParameterSpec(PSSParameterSpec.class);
                sig.setParameter(spec);
            }
*/
        }
        catch (Exception e)
        {
            return null;
        }

        return sig;
    }

    private static String getSignatureName(
        AlgorithmIdentifier sigAlgId)
    {
        ASN1Encodable params = sigAlgId.getParameters();

        if (params != null && !DERNull.INSTANCE.equals(params))
        {
            if (sigAlgId.getAlgorithm().equals(PKCSObjectIdentifiers.id_RSASSA_PSS))
            {
                RSASSAPSSparams rsaParams = RSASSAPSSparams.getInstance(params);
                return getDigestAlgName(rsaParams.getHashAlgorithm().getAlgorithm()) + "WITHRSAANDMGF1";
            }
        }

        if (oids.containsKey(sigAlgId.getAlgorithm()))
        {
            return (String)oids.get(sigAlgId.getAlgorithm());
        }

        return sigAlgId.getAlgorithm().getId();
    }

    private static String getDigestAlgName(
        ASN1ObjectIdentifier digestAlgOID)
    {
        if (PKCSObjectIdentifiers.md5.equals(digestAlgOID))
        {
            return "MD5";
        }
        else if (OIWObjectIdentifiers.idSHA1.equals(digestAlgOID))
        {
            return "SHA1";
        }
        else if (NISTObjectIdentifiers.id_sha224.equals(digestAlgOID))
        {
            return "SHA224";
        }
        else if (NISTObjectIdentifiers.id_sha256.equals(digestAlgOID))
        {
            return "SHA256";
        }
        else if (NISTObjectIdentifiers.id_sha384.equals(digestAlgOID))
        {
            return "SHA384";
        }
        else if (NISTObjectIdentifiers.id_sha512.equals(digestAlgOID))
        {
            return "SHA512";
        }
        else if (TeleTrusTObjectIdentifiers.ripemd128.equals(digestAlgOID))
        {
            return "RIPEMD128";
        }
        else if (TeleTrusTObjectIdentifiers.ripemd160.equals(digestAlgOID))
        {
            return "RIPEMD160";
        }
        else if (TeleTrusTObjectIdentifiers.ripemd256.equals(digestAlgOID))
        {
            return "RIPEMD256";
        }
        else if (CryptoProObjectIdentifiers.gostR3411.equals(digestAlgOID))
        {
            return "GOST3411";
        }
        else
        {
            return digestAlgOID.getId();
        }
    }

    public X509Certificate convertCertificate(X509CertificateHolder certHolder)
        throws CertificateException
    {

        try
        {
            CertificateFactory certFact = helper.createCertificateFactory("X.509");

            return (X509Certificate)certFact.generateCertificate(new ByteArrayInputStream(certHolder.getEncoded()));
        }
        catch (IOException e)
        {
            throw new OpCertificateException("cannot get encoded form of certificate: " + e.getMessage(), e);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new OpCertificateException("cannot create certificate factory: " + e.getMessage(), e);
        }
        catch (NoSuchProviderException e)
        {
            throw new OpCertificateException("cannot find factory provider: " + e.getMessage(), e);
        }
    }

    public PublicKey convertPublicKey(SubjectPublicKeyInfo publicKeyInfo)
        throws OperatorCreationException
    {
        try
        {
            KeyFactory keyFact = helper.createKeyFactory(publicKeyInfo.getAlgorithm().getAlgorithm().getId());

            return keyFact.generatePublic(new X509EncodedKeySpec(publicKeyInfo.getEncoded()));
        }
        catch (IOException e)
        {
            throw new OperatorCreationException("cannot get encoded form of key: " + e.getMessage(), e);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new OperatorCreationException("cannot create key factory: " + e.getMessage(), e);
        }
        catch (NoSuchProviderException e)
        {
            throw new OperatorCreationException("cannot find factory provider: " + e.getMessage(), e);
        }
        catch (InvalidKeySpecException e)
        {
            throw new OperatorCreationException("cannot create key factory: " + e.getMessage(), e);
        }
    }

    // TODO: put somewhere public so cause easily accessed
    private static class OpCertificateException
        extends CertificateException
    {
        private Throwable cause;

        public OpCertificateException(String msg, Throwable cause)
        {
            super(msg);

            this.cause = cause;
        }

        public Throwable getCause()
        {
            return cause;
        }
    }

    String getKeyAlgorithmName(ASN1ObjectIdentifier oid)
    {

        String name = (String)symmetricKeyAlgNames.get(oid);

        if (name != null)
        {
            return name;
        }

        return oid.getId();
    }
}
