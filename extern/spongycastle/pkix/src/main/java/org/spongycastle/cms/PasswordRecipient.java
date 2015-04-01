package org.spongycastle.cms;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;

public interface PasswordRecipient
    extends Recipient
{
    public static final int PKCS5_SCHEME2 = 0;
    public static final int PKCS5_SCHEME2_UTF8 = 1;

    byte[] calculateDerivedKey(byte[] encodedPassword, AlgorithmIdentifier derivationAlgorithm, int keySize)
        throws CMSException;

    RecipientOperator getRecipientOperator(AlgorithmIdentifier keyEncryptionAlgorithm, AlgorithmIdentifier contentEncryptionAlgorithm, byte[] derivedKey, byte[] encryptedEncryptedContentKey)
        throws CMSException;

    int getPasswordConversionScheme();

    char[] getPassword();
}
