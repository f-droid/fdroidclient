package org.spongycastle.cms.bc;

import java.io.InputStream;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.cms.CMSException;
import org.spongycastle.cms.RecipientOperator;
import org.spongycastle.crypto.BufferedBlockCipher;
import org.spongycastle.crypto.StreamCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.operator.InputDecryptor;
import org.spongycastle.operator.bc.BcSymmetricKeyUnwrapper;

public class BcKEKEnvelopedRecipient
    extends BcKEKRecipient
{
    public BcKEKEnvelopedRecipient(BcSymmetricKeyUnwrapper unwrapper)
    {
        super(unwrapper);
    }

    public RecipientOperator getRecipientOperator(AlgorithmIdentifier keyEncryptionAlgorithm, final AlgorithmIdentifier contentEncryptionAlgorithm, byte[] encryptedContentEncryptionKey)
        throws CMSException
    {
        KeyParameter secretKey = (KeyParameter)extractSecretKey(keyEncryptionAlgorithm, contentEncryptionAlgorithm, encryptedContentEncryptionKey);

        final Object dataCipher = EnvelopedDataHelper.createContentCipher(false, secretKey, contentEncryptionAlgorithm);

        return new RecipientOperator(new InputDecryptor()
        {
            public AlgorithmIdentifier getAlgorithmIdentifier()
            {
                return contentEncryptionAlgorithm;
            }

            public InputStream getInputStream(InputStream dataOut)
            {
                if (dataCipher instanceof BufferedBlockCipher)
                {
                    return new org.spongycastle.crypto.io.CipherInputStream(dataOut, (BufferedBlockCipher)dataCipher);
                }
                else
                {
                    return new org.spongycastle.crypto.io.CipherInputStream(dataOut, (StreamCipher)dataCipher);
                }
            }
        });
    }
}
