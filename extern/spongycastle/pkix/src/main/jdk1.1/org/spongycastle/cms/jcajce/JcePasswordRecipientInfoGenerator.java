package org.spongycastle.cms.jcajce;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.Provider;
import java.security.InvalidKeyException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.cms.CMSException;
import org.spongycastle.cms.PasswordRecipientInfoGenerator;
import org.spongycastle.operator.GenericKey;

public class JcePasswordRecipientInfoGenerator
    extends PasswordRecipientInfoGenerator
{
    private EnvelopedDataHelper helper = new EnvelopedDataHelper(new DefaultJcaJceExtHelper());

    public JcePasswordRecipientInfoGenerator(ASN1ObjectIdentifier kekAlgorithm, char[] password)
    {
        super(kekAlgorithm, password);
    }

    public JcePasswordRecipientInfoGenerator setProvider(Provider provider)
    {
        this.helper = new EnvelopedDataHelper(new ProviderJcaJceExtHelper(provider));

        return this;
    }

    public JcePasswordRecipientInfoGenerator setProvider(String providerName)
    {
        this.helper = new EnvelopedDataHelper(new NamedJcaJceExtHelper(providerName));

        return this;
    }

    public byte[] generateEncryptedBytes(AlgorithmIdentifier keyEncryptionAlgorithm, byte[] derivedKey, GenericKey contentEncryptionKey)
        throws CMSException
    {
        Key contentEncryptionKeySpec = helper.getJceKey(contentEncryptionKey);
        Cipher keyEncryptionCipher = helper.createRFC3211Wrapper(keyEncryptionAlgorithm.getAlgorithm());

        try
        {
            IvParameterSpec ivSpec = new IvParameterSpec(ASN1OctetString.getInstance(keyEncryptionAlgorithm.getParameters()).getOctets());

            keyEncryptionCipher.init(Cipher.WRAP_MODE, new SecretKeySpec(derivedKey, keyEncryptionCipher.getAlgorithm()), ivSpec);

            return keyEncryptionCipher.wrap(contentEncryptionKeySpec);
        }
        catch (InvalidKeyException e)
        {
            throw new CMSException("cannot process content encryption key: " + e.getMessage(), e);
        }
        catch (GeneralSecurityException e)
        {
            throw new CMSException("cannot process content encryption key: " + e.getMessage(), e);
        }
    }
}
