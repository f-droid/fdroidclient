package org.spongycastle.cms.bc;

import java.io.InputStream;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.cms.CMSException;
import org.spongycastle.cms.RecipientOperator;
import org.spongycastle.crypto.BufferedBlockCipher;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.StreamCipher;
import org.spongycastle.crypto.io.CipherInputStream;
import org.spongycastle.crypto.params.AsymmetricKeyParameter;
import org.spongycastle.operator.InputDecryptor;

public class BcRSAKeyTransEnvelopedRecipient
    extends BcKeyTransRecipient
{
    public BcRSAKeyTransEnvelopedRecipient(AsymmetricKeyParameter key)
    {
        super(key);
    }

    public RecipientOperator getRecipientOperator(AlgorithmIdentifier keyEncryptionAlgorithm, final AlgorithmIdentifier contentEncryptionAlgorithm, byte[] encryptedContentEncryptionKey)
        throws CMSException
    {
        CipherParameters secretKey = extractSecretKey(keyEncryptionAlgorithm, contentEncryptionAlgorithm, encryptedContentEncryptionKey);

        final Object dataCipher = EnvelopedDataHelper.createContentCipher(false, secretKey, contentEncryptionAlgorithm);

        return new RecipientOperator(new InputDecryptor()
        {
            public AlgorithmIdentifier getAlgorithmIdentifier()
            {
                return contentEncryptionAlgorithm;
            }

            public InputStream getInputStream(InputStream dataIn)
            {
                if (dataCipher instanceof BufferedBlockCipher)
                {
                    return new CipherInputStream(dataIn, (BufferedBlockCipher)dataCipher);
                }
                else
                {
                    return new CipherInputStream(dataIn, (StreamCipher)dataCipher);
                }
            }
        });
    }
}
