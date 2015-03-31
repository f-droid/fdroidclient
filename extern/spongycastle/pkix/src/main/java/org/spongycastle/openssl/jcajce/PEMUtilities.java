package org.spongycastle.openssl.jcajce;

import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.RC2ParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.DERObjectIdentifier;
import org.spongycastle.asn1.nist.NISTObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.crypto.PBEParametersGenerator;
import org.spongycastle.crypto.generators.OpenSSLPBEParametersGenerator;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.jcajce.JcaJceHelper;
import org.spongycastle.openssl.EncryptionException;
import org.spongycastle.openssl.PEMException;
import org.spongycastle.util.Integers;

class PEMUtilities
{
    private static final Map KEYSIZES = new HashMap();
    private static final Set PKCS5_SCHEME_1 = new HashSet();
    private static final Set PKCS5_SCHEME_2 = new HashSet();

    static
    {
        PKCS5_SCHEME_1.add(PKCSObjectIdentifiers.pbeWithMD2AndDES_CBC);
        PKCS5_SCHEME_1.add(PKCSObjectIdentifiers.pbeWithMD2AndRC2_CBC);
        PKCS5_SCHEME_1.add(PKCSObjectIdentifiers.pbeWithMD5AndDES_CBC);
        PKCS5_SCHEME_1.add(PKCSObjectIdentifiers.pbeWithMD5AndRC2_CBC);
        PKCS5_SCHEME_1.add(PKCSObjectIdentifiers.pbeWithSHA1AndDES_CBC);
        PKCS5_SCHEME_1.add(PKCSObjectIdentifiers.pbeWithSHA1AndRC2_CBC);

        PKCS5_SCHEME_2.add(PKCSObjectIdentifiers.id_PBES2);
        PKCS5_SCHEME_2.add(PKCSObjectIdentifiers.des_EDE3_CBC);
        PKCS5_SCHEME_2.add(NISTObjectIdentifiers.id_aes128_CBC);
        PKCS5_SCHEME_2.add(NISTObjectIdentifiers.id_aes192_CBC);
        PKCS5_SCHEME_2.add(NISTObjectIdentifiers.id_aes256_CBC);

        KEYSIZES.put(PKCSObjectIdentifiers.des_EDE3_CBC.getId(), Integers.valueOf(192));
        KEYSIZES.put(NISTObjectIdentifiers.id_aes128_CBC.getId(), Integers.valueOf(128));
        KEYSIZES.put(NISTObjectIdentifiers.id_aes192_CBC.getId(), Integers.valueOf(192));
        KEYSIZES.put(NISTObjectIdentifiers.id_aes256_CBC.getId(), Integers.valueOf(256));
    }

    static int getKeySize(String algorithm)
    {
        if (!KEYSIZES.containsKey(algorithm))
        {
            throw new IllegalStateException("no key size for algorithm: " + algorithm);
        }
        
        return ((Integer)KEYSIZES.get(algorithm)).intValue();
    }

    static boolean isPKCS5Scheme1(DERObjectIdentifier algOid)
    {
        return PKCS5_SCHEME_1.contains(algOid);
    }

    static boolean isPKCS5Scheme2(ASN1ObjectIdentifier algOid)
    {
        return PKCS5_SCHEME_2.contains(algOid);
    }

    public static boolean isPKCS12(DERObjectIdentifier algOid)
    {
        return algOid.getId().startsWith(PKCSObjectIdentifiers.pkcs_12PbeIds.getId());
    }

    public static SecretKey generateSecretKeyForPKCS5Scheme2(String algorithm, char[] password, byte[] salt, int iterationCount)
    {
        PBEParametersGenerator generator = new PKCS5S2ParametersGenerator();

        generator.init(
            PBEParametersGenerator.PKCS5PasswordToBytes(password),
            salt,
            iterationCount);

        return new SecretKeySpec(((KeyParameter)generator.generateDerivedParameters(PEMUtilities.getKeySize(algorithm))).getKey(), algorithm);
    }

    static byte[] crypt(
        boolean encrypt,
        JcaJceHelper helper,
        byte[]  bytes,
        char[]  password,
        String  dekAlgName,
        byte[]  iv)
        throws PEMException
    {
        AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
        String                 alg;
        String                 blockMode = "CBC";
        String                 padding = "PKCS5Padding";
        Key                    sKey;

        // Figure out block mode and padding.
        if (dekAlgName.endsWith("-CFB"))
        {
            blockMode = "CFB";
            padding = "NoPadding";
        }
        if (dekAlgName.endsWith("-ECB") ||
            "DES-EDE".equals(dekAlgName) ||
            "DES-EDE3".equals(dekAlgName))
        {
            // ECB is actually the default (though seldom used) when OpenSSL
            // uses DES-EDE (des2) or DES-EDE3 (des3).
            blockMode = "ECB";
            paramSpec = null;
        }
        if (dekAlgName.endsWith("-OFB"))
        {
            blockMode = "OFB";
            padding = "NoPadding";
        }


        // Figure out algorithm and key size.
        if (dekAlgName.startsWith("DES-EDE"))
        {
            alg = "DESede";
            // "DES-EDE" is actually des2 in OpenSSL-speak!
            // "DES-EDE3" is des3.
            boolean des2 = !dekAlgName.startsWith("DES-EDE3");
            sKey = getKey(password, alg, 24, iv, des2);
        }
        else if (dekAlgName.startsWith("DES-"))
        {
            alg = "DES";
            sKey = getKey(password, alg, 8, iv);
        }
        else if (dekAlgName.startsWith("BF-"))
        {
            alg = "Blowfish";
            sKey = getKey(password, alg, 16, iv);
        }
        else if (dekAlgName.startsWith("RC2-"))
        {
            alg = "RC2";
            int keyBits = 128;
            if (dekAlgName.startsWith("RC2-40-"))
            {
                keyBits = 40;
            }
            else if (dekAlgName.startsWith("RC2-64-"))
            {
                keyBits = 64;
            }
            sKey = getKey(password, alg, keyBits / 8, iv);
            if (paramSpec == null) // ECB block mode
            {
                paramSpec = new RC2ParameterSpec(keyBits);
            }
            else
            {
                paramSpec = new RC2ParameterSpec(keyBits, iv);
            }
        }
        else if (dekAlgName.startsWith("AES-"))
        {
            alg = "AES";
            byte[] salt = iv;
            if (salt.length > 8)
            {
                salt = new byte[8];
                System.arraycopy(iv, 0, salt, 0, 8);
            }

            int keyBits;
            if (dekAlgName.startsWith("AES-128-"))
            {
                keyBits = 128;
            }
            else if (dekAlgName.startsWith("AES-192-"))
            {
                keyBits = 192;
            }
            else if (dekAlgName.startsWith("AES-256-"))
            {
                keyBits = 256;
            }
            else
            {
                throw new EncryptionException("unknown AES encryption with private key");
            }
            sKey = getKey(password, "AES", keyBits / 8, salt);
        }
        else
        {
            throw new EncryptionException("unknown encryption with private key");
        }

        String transformation = alg + "/" + blockMode + "/" + padding;

        try
        {
            Cipher c = helper.createCipher(transformation);
            int    mode = encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;

            if (paramSpec == null) // ECB block mode
            {
                c.init(mode, sKey);
            }
            else
            {
                c.init(mode, sKey, paramSpec);
            }
            return c.doFinal(bytes);
        }
        catch (Exception e)
        {
            throw new EncryptionException("exception using cipher - please check password and data.", e);
        }
    }

    private static SecretKey getKey(
        char[]  password,
        String  algorithm,
        int     keyLength,
        byte[]  salt)
    {
        return getKey(password, algorithm, keyLength, salt, false);
    }

    private static SecretKey getKey(
        char[]  password,
        String  algorithm,
        int     keyLength,
        byte[]  salt,
        boolean des2)
    {
        OpenSSLPBEParametersGenerator   pGen = new OpenSSLPBEParametersGenerator();

        pGen.init(PBEParametersGenerator.PKCS5PasswordToBytes(password), salt);

        KeyParameter keyParam;
        keyParam = (KeyParameter) pGen.generateDerivedParameters(keyLength * 8);
        byte[] key = keyParam.getKey();
        if (des2 && key.length >= 24)
        {
            // For DES2, we must copy first 8 bytes into the last 8 bytes.
            System.arraycopy(key, 0, key, 16, 8);
        }
        return new SecretKeySpec(key, algorithm);
    }
}
