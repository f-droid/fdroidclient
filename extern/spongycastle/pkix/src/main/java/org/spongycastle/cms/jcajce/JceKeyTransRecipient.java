package org.spongycastle.cms.jcajce;

import java.security.Key;
import java.security.PrivateKey;
import java.security.Provider;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.cms.CMSException;
import org.spongycastle.cms.KeyTransRecipient;
import org.spongycastle.operator.OperatorException;
import org.spongycastle.operator.jcajce.JceAsymmetricKeyUnwrapper;

public abstract class JceKeyTransRecipient
    implements KeyTransRecipient
{
    private PrivateKey recipientKey;

    protected EnvelopedDataHelper helper = new EnvelopedDataHelper(new DefaultJcaJceExtHelper());
    protected EnvelopedDataHelper contentHelper = helper;
    protected Map extraMappings = new HashMap();
    protected boolean validateKeySize = false;

    public JceKeyTransRecipient(PrivateKey recipientKey)
    {
        this.recipientKey = recipientKey;
    }

    /**
     * Set the provider to use for key recovery and content processing.
     *
     * @param provider provider to use.
     * @return this recipient.
     */
    public JceKeyTransRecipient setProvider(Provider provider)
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
    public JceKeyTransRecipient setProvider(String providerName)
    {
        this.helper = new EnvelopedDataHelper(new NamedJcaJceExtHelper(providerName));
        this.contentHelper = helper;

        return this;
    }

    /**
     * Internally algorithm ids are converted into cipher names using a lookup table. For some providers
     * the standard lookup table won't work. Use this method to establish a specific mapping from an
     * algorithm identifier to a specific algorithm.
     * <p>
     *     For example:
     * <pre>
     *     unwrapper.setAlgorithmMapping(PKCSObjectIdentifiers.rsaEncryption, "RSA");
     * </pre>
     * </p>
     * @param algorithm  OID of algorithm in recipient.
     * @param algorithmName JCE algorithm name to use.
     * @return the current Recipient.
     */
    public JceKeyTransRecipient setAlgorithmMapping(ASN1ObjectIdentifier algorithm, String algorithmName)
    {
        extraMappings.put(algorithm, algorithmName);

        return this;
    }

    /**
     * Set the provider to use for content processing.  If providerName is null a "no provider" search will be
     * used to satisfy getInstance calls.
     *
     * @param provider the provider to use.
     * @return this recipient.
     */
    public JceKeyTransRecipient setContentProvider(Provider provider)
    {
        this.contentHelper = CMSUtils.createContentHelper(provider);

        return this;
    }

    /**
     * Set the provider to use for content processing.  If providerName is null a "no provider" search will be
     *  used to satisfy getInstance calls.
     *
     * @param providerName the name of the provider to use.
     * @return this recipient.
     */
    public JceKeyTransRecipient setContentProvider(String providerName)
    {
        this.contentHelper = CMSUtils.createContentHelper(providerName);

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
    public JceKeyTransRecipient setKeySizeValidation(boolean doValidate)
    {
        this.validateKeySize = doValidate;

        return this;
    }

    protected Key extractSecretKey(AlgorithmIdentifier keyEncryptionAlgorithm, AlgorithmIdentifier encryptedKeyAlgorithm, byte[] encryptedEncryptionKey)
        throws CMSException
    {
        JceAsymmetricKeyUnwrapper unwrapper = helper.createAsymmetricUnwrapper(keyEncryptionAlgorithm, recipientKey);

        if (!extraMappings.isEmpty())
        {
            for (Iterator it = extraMappings.keySet().iterator(); it.hasNext();)
            {
                ASN1ObjectIdentifier algorithm = (ASN1ObjectIdentifier)it.next();

                unwrapper.setAlgorithmMapping(algorithm, (String)extraMappings.get(algorithm));
            }
        }

        try
        {
            Key key = helper.getJceKey(encryptedKeyAlgorithm.getAlgorithm(), unwrapper.generateUnwrappedKey(encryptedKeyAlgorithm, encryptedEncryptionKey));

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
