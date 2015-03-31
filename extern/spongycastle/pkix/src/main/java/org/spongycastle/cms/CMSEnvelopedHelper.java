package org.spongycastle.cms;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1Set;
import org.spongycastle.asn1.cms.KEKRecipientInfo;
import org.spongycastle.asn1.cms.KeyAgreeRecipientInfo;
import org.spongycastle.asn1.cms.KeyTransRecipientInfo;
import org.spongycastle.asn1.cms.PasswordRecipientInfo;
import org.spongycastle.asn1.cms.RecipientInfo;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.operator.DigestCalculator;
import org.spongycastle.util.Integers;

class CMSEnvelopedHelper
{
    static final CMSEnvelopedHelper INSTANCE = new CMSEnvelopedHelper();

    private static final Map KEYSIZES = new HashMap();
    private static final Map BASE_CIPHER_NAMES = new HashMap();
    private static final Map CIPHER_ALG_NAMES = new HashMap();
    private static final Map MAC_ALG_NAMES = new HashMap();

    static
    {
        KEYSIZES.put(CMSEnvelopedGenerator.DES_EDE3_CBC, Integers.valueOf(192));
        KEYSIZES.put(CMSEnvelopedGenerator.AES128_CBC, Integers.valueOf(128));
        KEYSIZES.put(CMSEnvelopedGenerator.AES192_CBC, Integers.valueOf(192));
        KEYSIZES.put(CMSEnvelopedGenerator.AES256_CBC, Integers.valueOf(256));

        BASE_CIPHER_NAMES.put(CMSEnvelopedGenerator.DES_EDE3_CBC,  "DESEDE");
        BASE_CIPHER_NAMES.put(CMSEnvelopedGenerator.AES128_CBC,  "AES");
        BASE_CIPHER_NAMES.put(CMSEnvelopedGenerator.AES192_CBC,  "AES");
        BASE_CIPHER_NAMES.put(CMSEnvelopedGenerator.AES256_CBC,  "AES");

        CIPHER_ALG_NAMES.put(CMSEnvelopedGenerator.DES_EDE3_CBC,  "DESEDE/CBC/PKCS5Padding");
        CIPHER_ALG_NAMES.put(CMSEnvelopedGenerator.AES128_CBC,  "AES/CBC/PKCS5Padding");
        CIPHER_ALG_NAMES.put(CMSEnvelopedGenerator.AES192_CBC,  "AES/CBC/PKCS5Padding");
        CIPHER_ALG_NAMES.put(CMSEnvelopedGenerator.AES256_CBC,  "AES/CBC/PKCS5Padding");

        MAC_ALG_NAMES.put(CMSEnvelopedGenerator.DES_EDE3_CBC,  "DESEDEMac");
        MAC_ALG_NAMES.put(CMSEnvelopedGenerator.AES128_CBC,  "AESMac");
        MAC_ALG_NAMES.put(CMSEnvelopedGenerator.AES192_CBC,  "AESMac");
        MAC_ALG_NAMES.put(CMSEnvelopedGenerator.AES256_CBC,  "AESMac");
    }



    int getKeySize(String oid)
    {
        Integer keySize = (Integer)KEYSIZES.get(oid);

        if (keySize == null)
        {
            throw new IllegalArgumentException("no keysize for " + oid);
        }

        return keySize.intValue();
    }



    static RecipientInformationStore buildRecipientInformationStore(
        ASN1Set recipientInfos, AlgorithmIdentifier messageAlgorithm, CMSSecureReadable secureReadable)
    {
        return buildRecipientInformationStore(recipientInfos, messageAlgorithm, secureReadable, null);
    }

    static RecipientInformationStore buildRecipientInformationStore(
        ASN1Set recipientInfos, AlgorithmIdentifier messageAlgorithm, CMSSecureReadable secureReadable, AuthAttributesProvider additionalData)
    {
        List infos = new ArrayList();
        for (int i = 0; i != recipientInfos.size(); i++)
        {
            RecipientInfo info = RecipientInfo.getInstance(recipientInfos.getObjectAt(i));

            readRecipientInfo(infos, info, messageAlgorithm, secureReadable, additionalData);
        }
        return new RecipientInformationStore(infos);
    }

    private static void readRecipientInfo(
        List infos, RecipientInfo info, AlgorithmIdentifier messageAlgorithm, CMSSecureReadable secureReadable, AuthAttributesProvider additionalData)
    {
        ASN1Encodable recipInfo = info.getInfo();
        if (recipInfo instanceof KeyTransRecipientInfo)
        {
            infos.add(new KeyTransRecipientInformation(
                (KeyTransRecipientInfo)recipInfo, messageAlgorithm, secureReadable, additionalData));
        }
        else if (recipInfo instanceof KEKRecipientInfo)
        {
            infos.add(new KEKRecipientInformation(
                (KEKRecipientInfo)recipInfo, messageAlgorithm, secureReadable, additionalData));
        }
        else if (recipInfo instanceof KeyAgreeRecipientInfo)
        {
            KeyAgreeRecipientInformation.readRecipientInfo(infos,
                (KeyAgreeRecipientInfo)recipInfo, messageAlgorithm, secureReadable, additionalData);
        }
        else if (recipInfo instanceof PasswordRecipientInfo)
        {
            infos.add(new PasswordRecipientInformation(
                (PasswordRecipientInfo)recipInfo, messageAlgorithm, secureReadable, additionalData));
        }
    }

    static class CMSDigestAuthenticatedSecureReadable
        implements CMSSecureReadable
    {
        private DigestCalculator digestCalculator;
        private CMSReadable readable;

        public CMSDigestAuthenticatedSecureReadable(DigestCalculator digestCalculator, CMSReadable readable)
        {
            this.digestCalculator = digestCalculator;
            this.readable = readable;
        }

        public InputStream getInputStream()
            throws IOException, CMSException
        {
            return new FilterInputStream(readable.getInputStream())
            {
                public int read()
                    throws IOException
                {
                    int b = in.read();

                    if (b >= 0)
                    {
                        digestCalculator.getOutputStream().write(b);
                    }

                    return b;
                }

                public int read(byte[] inBuf, int inOff, int inLen)
                    throws IOException
                {
                    int n = in.read(inBuf, inOff, inLen);
                    
                    if (n >= 0)
                    {
                        digestCalculator.getOutputStream().write(inBuf, inOff, n);
                    }

                    return n;
                }
            };
        }

        public byte[] getDigest()
        {
            return digestCalculator.getDigest();
        }
    }

    static class CMSAuthenticatedSecureReadable implements CMSSecureReadable
    {
        private AlgorithmIdentifier algorithm;
        private CMSReadable readable;

        CMSAuthenticatedSecureReadable(AlgorithmIdentifier algorithm, CMSReadable readable)
        {
            this.algorithm = algorithm;
            this.readable = readable;
        }

        public InputStream getInputStream()
            throws IOException, CMSException
        {
            return readable.getInputStream();
        }

    }

    static class CMSEnvelopedSecureReadable implements CMSSecureReadable
    {
        private AlgorithmIdentifier algorithm;
        private CMSReadable readable;

        CMSEnvelopedSecureReadable(AlgorithmIdentifier algorithm, CMSReadable readable)
        {
            this.algorithm = algorithm;
            this.readable = readable;
        }

        public InputStream getInputStream()
            throws IOException, CMSException
        {
            return readable.getInputStream();
        }

    }
}
