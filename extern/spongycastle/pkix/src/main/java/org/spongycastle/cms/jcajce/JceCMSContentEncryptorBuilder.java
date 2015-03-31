package org.spongycastle.cms.jcajce;

import java.io.OutputStream;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.cms.CMSException;
import org.spongycastle.operator.DefaultSecretKeySizeProvider;
import org.spongycastle.operator.GenericKey;
import org.spongycastle.operator.OutputEncryptor;
import org.spongycastle.operator.SecretKeySizeProvider;
import org.spongycastle.operator.jcajce.JceGenericKey;

public class JceCMSContentEncryptorBuilder
{
    private static final SecretKeySizeProvider KEY_SIZE_PROVIDER = DefaultSecretKeySizeProvider.INSTANCE;


    private final ASN1ObjectIdentifier encryptionOID;
    private final int                  keySize;

    private EnvelopedDataHelper helper = new EnvelopedDataHelper(new DefaultJcaJceExtHelper());
    private SecureRandom random;

    public JceCMSContentEncryptorBuilder(ASN1ObjectIdentifier encryptionOID)
    {
        this(encryptionOID, KEY_SIZE_PROVIDER.getKeySize(encryptionOID));
    }

    public JceCMSContentEncryptorBuilder(ASN1ObjectIdentifier encryptionOID, int keySize)
    {
        this.encryptionOID = encryptionOID;
        this.keySize = keySize;

        int fixedSize = KEY_SIZE_PROVIDER.getKeySize(encryptionOID);

        if (encryptionOID.equals(PKCSObjectIdentifiers.des_EDE3_CBC))
        {
            if (keySize != 168 && keySize != fixedSize)
            {
                throw new IllegalArgumentException("incorrect keySize for encryptionOID passed to builder.");
            }
        }
        else
        {
            if (fixedSize > 0 && fixedSize != keySize)
            {
                throw new IllegalArgumentException("incorrect keySize for encryptionOID passed to builder.");
            }
        }
    }

    public JceCMSContentEncryptorBuilder setProvider(Provider provider)
    {
        this.helper = new EnvelopedDataHelper(new ProviderJcaJceExtHelper(provider));

        return this;
    }

    public JceCMSContentEncryptorBuilder setProvider(String providerName)
    {
        this.helper = new EnvelopedDataHelper(new NamedJcaJceExtHelper(providerName));

        return this;
    }

    public JceCMSContentEncryptorBuilder setSecureRandom(SecureRandom random)
    {
        this.random = random;

        return this;
    }

    public OutputEncryptor build()
        throws CMSException
    {
        return new CMSOutputEncryptor(encryptionOID, keySize, random);
    }

    private class CMSOutputEncryptor
        implements OutputEncryptor
    {
        private SecretKey encKey;
        private AlgorithmIdentifier algorithmIdentifier;
        private Cipher              cipher;

        CMSOutputEncryptor(ASN1ObjectIdentifier encryptionOID, int keySize, SecureRandom random)
            throws CMSException
        {
            KeyGenerator keyGen = helper.createKeyGenerator(encryptionOID);

            if (random == null)
            {
                random = new SecureRandom();
            }

            if (keySize < 0)
            {
                keyGen.init(random);
            }
            else
            {
                if (encryptionOID.equals(PKCSObjectIdentifiers.des_EDE3_CBC) && keySize == 192)
                {
                    keySize = 168;
                }
                keyGen.init(keySize, random);
            }

            cipher = helper.createCipher(encryptionOID);
            encKey = keyGen.generateKey();
            AlgorithmParameters params = helper.generateParameters(encryptionOID, encKey, random);

            try
            {
                cipher.init(Cipher.ENCRYPT_MODE, encKey, params, random);
            }
            catch (GeneralSecurityException e)
            {
                throw new CMSException("unable to initialize cipher: " + e.getMessage(), e);
            }

            //
            // If params are null we try and second guess on them as some providers don't provide
            // algorithm parameter generation explicity but instead generate them under the hood.
            //
            if (params == null)
            {
                params = cipher.getParameters();
            }

            algorithmIdentifier = helper.getAlgorithmIdentifier(encryptionOID, params);
        }

        public AlgorithmIdentifier getAlgorithmIdentifier()
        {
            return algorithmIdentifier;
        }

        public OutputStream getOutputStream(OutputStream dOut)
        {
            return new CipherOutputStream(dOut, cipher);
        }

        public GenericKey getKey()
        {
            return new JceGenericKey(algorithmIdentifier, encKey);
        }
    }
}
