package org.spongycastle.cms;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;

public interface KeyTransRecipient
    extends Recipient
{
    RecipientOperator getRecipientOperator(AlgorithmIdentifier keyEncAlg, AlgorithmIdentifier contentEncryptionAlgorithm, byte[] encryptedContentKey)
        throws CMSException;
}
