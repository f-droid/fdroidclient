package org.spongycastle.operator.jcajce;

import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Provider;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.jcajce.DefaultJcaJceHelper;
import org.spongycastle.jcajce.NamedJcaJceHelper;
import org.spongycastle.jcajce.ProviderJcaJceHelper;
import org.spongycastle.operator.AsymmetricKeyWrapper;
import org.spongycastle.operator.GenericKey;
import org.spongycastle.operator.OperatorException;

public class JceAsymmetricKeyWrapper
    extends AsymmetricKeyWrapper
{
    private OperatorHelper helper = new OperatorHelper(new DefaultJcaJceHelper());
    private Map extraMappings = new HashMap();
    private PublicKey publicKey;
    private SecureRandom random;

    public JceAsymmetricKeyWrapper(PublicKey publicKey)
    {
        super(SubjectPublicKeyInfo.getInstance(publicKey.getEncoded()).getAlgorithm());

        this.publicKey = publicKey;
    }

    public JceAsymmetricKeyWrapper(X509Certificate certificate)
    {
        this(certificate.getPublicKey());
    }

    /**
     * Create a wrapper, overriding the algorithm type that is stored in the public key.
     *
     * @param algorithmIdentifier identifier for encryption algorithm to be used.
     * @param publicKey the public key to be used.
     */
    public JceAsymmetricKeyWrapper(AlgorithmIdentifier algorithmIdentifier, PublicKey publicKey)
    {
        super(algorithmIdentifier);

        this.publicKey = publicKey;
    }

    public JceAsymmetricKeyWrapper setProvider(Provider provider)
    {
        this.helper = new OperatorHelper(new ProviderJcaJceHelper(provider));

        return this;
    }

    public JceAsymmetricKeyWrapper setProvider(String providerName)
    {
        this.helper = new OperatorHelper(new NamedJcaJceHelper(providerName));

        return this;
    }

    public JceAsymmetricKeyWrapper setSecureRandom(SecureRandom random)
    {
        this.random = random;

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
     * @return the current Wrapper.
     */
    public JceAsymmetricKeyWrapper setAlgorithmMapping(ASN1ObjectIdentifier algorithm, String algorithmName)
    {
        extraMappings.put(algorithm, algorithmName);

        return this;
    }

    public byte[] generateWrappedKey(GenericKey encryptionKey)
        throws OperatorException
    {
        Cipher keyEncryptionCipher = helper.createAsymmetricWrapper(getAlgorithmIdentifier().getAlgorithm(), extraMappings);
        AlgorithmParameters algParams = helper.createAlgorithmParameters(this.getAlgorithmIdentifier());

        byte[] encryptedKeyBytes = null;

        try
        {
            if (algParams != null)
            {
                keyEncryptionCipher.init(Cipher.WRAP_MODE, publicKey, algParams, random);
            }
            else
            {
                keyEncryptionCipher.init(Cipher.WRAP_MODE, publicKey, random);
            }
            encryptedKeyBytes = keyEncryptionCipher.wrap(OperatorUtils.getJceKey(encryptionKey));
        }
        catch (InvalidKeyException e)
        {
        }
        catch (GeneralSecurityException e)
        {
        }
        catch (IllegalStateException e)
        {
        }
        catch (UnsupportedOperationException e)
        {
        }
        catch (ProviderException e)
        {
        }

        // some providers do not support WRAP (this appears to be only for asymmetric algorithms)
        if (encryptedKeyBytes == null)
        {
            try
            {
                keyEncryptionCipher.init(Cipher.ENCRYPT_MODE, publicKey, random);
                encryptedKeyBytes = keyEncryptionCipher.doFinal(OperatorUtils.getJceKey(encryptionKey).getEncoded());
            }
            catch (InvalidKeyException e)
            {
                throw new OperatorException("unable to encrypt contents key", e);
            }
            catch (GeneralSecurityException e)
            {
                throw new OperatorException("unable to encrypt contents key", e);
            }
        }

        return encryptedKeyBytes;
    }
}
