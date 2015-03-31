package org.spongycastle.cert.crmf.jcajce;

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
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.cert.crmf.CRMFException;
import org.spongycastle.jcajce.DefaultJcaJceHelper;
import org.spongycastle.jcajce.NamedJcaJceHelper;
import org.spongycastle.jcajce.ProviderJcaJceHelper;
import org.spongycastle.operator.GenericKey;
import org.spongycastle.operator.OutputEncryptor;

public class JceCRMFEncryptorBuilder
{
    private ASN1ObjectIdentifier encryptionOID;
    private int                  keySize;

    private CRMFHelper helper = new CRMFHelper(new DefaultJcaJceHelper());
    private SecureRandom random;

    public JceCRMFEncryptorBuilder(ASN1ObjectIdentifier encryptionOID)
    {
        this(encryptionOID, -1);
    }

    public JceCRMFEncryptorBuilder(ASN1ObjectIdentifier encryptionOID, int keySize)
    {
        this.encryptionOID = encryptionOID;
        this.keySize = keySize;
    }

    public JceCRMFEncryptorBuilder setProvider(Provider provider)
    {
        this.helper = new CRMFHelper(new ProviderJcaJceHelper(provider));

        return this;
    }

    public JceCRMFEncryptorBuilder setProvider(String providerName)
    {
        this.helper = new CRMFHelper(new NamedJcaJceHelper(providerName));

        return this;
    }

    public JceCRMFEncryptorBuilder setSecureRandom(SecureRandom random)
    {
        this.random = random;

        return this;
    }

    public OutputEncryptor build()
        throws CRMFException
    {
        return new CRMFOutputEncryptor(encryptionOID, keySize, random);
    }

    private class CRMFOutputEncryptor
        implements OutputEncryptor
    {
        private SecretKey encKey;
        private AlgorithmIdentifier algorithmIdentifier;
        private Cipher cipher;

        CRMFOutputEncryptor(ASN1ObjectIdentifier encryptionOID, int keySize, SecureRandom random)
            throws CRMFException
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
                throw new CRMFException("unable to initialize cipher: " + e.getMessage(), e);
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
            return new GenericKey(encKey);
        }
    }
}
