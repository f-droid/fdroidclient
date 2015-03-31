package org.spongycastle.cms.jcajce;

import java.security.Key;
import java.security.Provider;

import javax.crypto.SecretKey;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.cms.CMSException;
import org.spongycastle.cms.KEKRecipient;
import org.spongycastle.operator.OperatorException;
import org.spongycastle.operator.SymmetricKeyUnwrapper;

public abstract class JceKEKRecipient
    implements KEKRecipient
{
    private SecretKey recipientKey;

    protected EnvelopedDataHelper helper = new EnvelopedDataHelper(new DefaultJcaJceExtHelper());
    protected EnvelopedDataHelper contentHelper = helper;
    protected boolean validateKeySize = false;

    public JceKEKRecipient(SecretKey recipientKey)
    {
        this.recipientKey = recipientKey;
    }

    /**
     * Set the provider to use for key recovery and content processing.
     *
     * @param provider provider to use.
     * @return this recipient.
     */
    public JceKEKRecipient setProvider(Provider provider)
    {
        this.helper = new EnvelopedDataHelper(new ProviderJcaJceExtHelper(provider));
        this.contentHelper = helper;

        return this;
    }

    /**
     * Set the provider to use for key recovery and content processing.
     *
     * @param providerName the name of the provider to use.
     * @return this recipient.
     */
    public JceKEKRecipient setProvider(String providerName)
    {
        this.helper = new EnvelopedDataHelper(new NamedJcaJceExtHelper(providerName));
        this.contentHelper = helper;

        return this;
    }

    /**
     * Set the provider to use for content processing.
     *
     * @param provider the provider to use.
     * @return this recipient.
     */
    public JceKEKRecipient setContentProvider(Provider provider)
    {
        this.contentHelper = new EnvelopedDataHelper(new ProviderJcaJceExtHelper(provider));

        return this;
    }

    /**
     * Set the provider to use for content processing.
     *
     * @param providerName the name of the provider to use.
     * @return this recipient.
     */
    public JceKEKRecipient setContentProvider(String providerName)
    {
        this.contentHelper = new EnvelopedDataHelper(new NamedJcaJceExtHelper(providerName));

        return this;
    }

    /**
     * Set validation of retrieved key sizes against the algorithm parameters for the encrypted key where possible - default is off.
     * <p>
     * This setting will not have any affect if the encryption algorithm in the recipient does not specify a particular key size, or
     * if the unwrapper is a HSM and the byte encoding of the unwrapped secret key is not available.
     * </p>
     * @param doValidate true if unwrapped key's should be validated against the content encryption algorithm, false otherwise.
     * @return this recipient.
     */
    public JceKEKRecipient setKeySizeValidation(boolean doValidate)
    {
        this.validateKeySize = doValidate;

        return this;
    }

    protected Key extractSecretKey(AlgorithmIdentifier keyEncryptionAlgorithm, AlgorithmIdentifier encryptedKeyAlgorithm, byte[] encryptedContentEncryptionKey)
        throws CMSException
    {
        SymmetricKeyUnwrapper unwrapper = helper.createSymmetricUnwrapper(keyEncryptionAlgorithm, recipientKey);

        try
        {
            Key key =  helper.getJceKey(encryptedKeyAlgorithm.getAlgorithm(), unwrapper.generateUnwrappedKey(encryptedKeyAlgorithm, encryptedContentEncryptionKey));

            if (validateKeySize)
            {
                helper.keySizeCheck(encryptedKeyAlgorithm, key);
            }

            return key;
        }
        catch (OperatorException e)
        {
            throw new CMSException("exception unwrapping key: " + e.getMessage(), e);
        }
    }
}
