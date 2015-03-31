package org.spongycastle.cms.bc;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.cms.CMSException;
import org.spongycastle.cms.KEKRecipient;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.operator.OperatorException;
import org.spongycastle.operator.SymmetricKeyUnwrapper;
import org.spongycastle.operator.bc.BcSymmetricKeyUnwrapper;

public abstract class BcKEKRecipient
    implements KEKRecipient
{
    private SymmetricKeyUnwrapper unwrapper;

    public BcKEKRecipient(BcSymmetricKeyUnwrapper unwrapper)
    {
        this.unwrapper = unwrapper;
    }

    protected CipherParameters extractSecretKey(AlgorithmIdentifier keyEncryptionAlgorithm, AlgorithmIdentifier contentEncryptionAlgorithm, byte[] encryptedContentEncryptionKey)
        throws CMSException
    {
        try
        {
            return CMSUtils.getBcKey(unwrapper.generateUnwrappedKey(contentEncryptionAlgorithm, encryptedContentEncryptionKey));
        }
        catch (OperatorException e)
        {
            throw new CMSException("exception unwrapping key: " + e.getMessage(), e);
        }
    }
}
