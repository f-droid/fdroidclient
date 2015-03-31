package org.spongycastle.cms;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.cms.PasswordRecipientInfo;
import org.spongycastle.asn1.pkcs.PBKDF2Params;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.crypto.PBEParametersGenerator;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.util.Integers;

/**
 * the RecipientInfo class for a recipient who has been sent a message
 * encrypted using a password.
 */
public class PasswordRecipientInformation
    extends RecipientInformation
{
    static Map KEYSIZES = new HashMap();
    static Map BLOCKSIZES = new HashMap();

    static
    {
        BLOCKSIZES.put(CMSAlgorithm.DES_EDE3_CBC, Integers.valueOf(8));
        BLOCKSIZES.put(CMSAlgorithm.AES128_CBC, Integers.valueOf(16));
        BLOCKSIZES.put(CMSAlgorithm.AES192_CBC, Integers.valueOf(16));
        BLOCKSIZES.put(CMSAlgorithm.AES256_CBC, Integers.valueOf(16));

        KEYSIZES.put(CMSAlgorithm.DES_EDE3_CBC, Integers.valueOf(192));
        KEYSIZES.put(CMSAlgorithm.AES128_CBC, Integers.valueOf(128));
        KEYSIZES.put(CMSAlgorithm.AES192_CBC, Integers.valueOf(192));
        KEYSIZES.put(CMSAlgorithm.AES256_CBC, Integers.valueOf(256));
    }

    private PasswordRecipientInfo info;

    PasswordRecipientInformation(
        PasswordRecipientInfo   info,
        AlgorithmIdentifier     messageAlgorithm,
        CMSSecureReadable       secureReadable,
        AuthAttributesProvider  additionalData)
    {
        super(info.getKeyEncryptionAlgorithm(), messageAlgorithm, secureReadable, additionalData);

        this.info = info;
        this.rid = new PasswordRecipientId();
    }

    /**
     * return the object identifier for the key derivation algorithm, or null
     * if there is none present.
     *
     * @return OID for key derivation algorithm, if present.
     */
    public String getKeyDerivationAlgOID()
    {
        if (info.getKeyDerivationAlgorithm() != null)
        {
            return info.getKeyDerivationAlgorithm().getAlgorithm().getId();
        }

        return null;
    }

    /**
     * return the ASN.1 encoded key derivation algorithm parameters, or null if
     * there aren't any.
     * @return ASN.1 encoding of key derivation algorithm parameters.
     */
    public byte[] getKeyDerivationAlgParams()
    {
        try
        {
            if (info.getKeyDerivationAlgorithm() != null)
            {
                ASN1Encodable params = info.getKeyDerivationAlgorithm().getParameters();
                if (params != null)
                {
                    return params.toASN1Primitive().getEncoded();
                }
            }

            return null;
        }
        catch (Exception e)
        {
            throw new RuntimeException("exception getting encryption parameters " + e);
        }
    }

    /**
     * Return the key derivation algorithm details for the key in this recipient.
     *
     * @return AlgorithmIdentifier representing the key derivation algorithm.
     */
    public AlgorithmIdentifier getKeyDerivationAlgorithm()
    {
        return info.getKeyDerivationAlgorithm();
    }

    protected RecipientOperator getRecipientOperator(Recipient recipient)
        throws CMSException, IOException
    {
        PasswordRecipient pbeRecipient = (PasswordRecipient)recipient;
        AlgorithmIdentifier kekAlg = AlgorithmIdentifier.getInstance(info.getKeyEncryptionAlgorithm());
        AlgorithmIdentifier kekAlgParams = AlgorithmIdentifier.getInstance(kekAlg.getParameters());

        byte[] passwordBytes = getPasswordBytes(pbeRecipient.getPasswordConversionScheme(),
            pbeRecipient.getPassword());
        PBKDF2Params params = PBKDF2Params.getInstance(info.getKeyDerivationAlgorithm().getParameters());

        PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator();
        gen.init(passwordBytes, params.getSalt(), params.getIterationCount().intValue());

        int keySize = ((Integer)KEYSIZES.get(kekAlgParams.getAlgorithm())).intValue();

        byte[] derivedKey = ((KeyParameter)gen.generateDerivedParameters(keySize)).getKey();

        return pbeRecipient.getRecipientOperator(kekAlgParams, messageAlgorithm, derivedKey, info.getEncryptedKey().getOctets());
    }
    
    protected byte[] getPasswordBytes(int scheme, char[] password)
    {
        if (scheme == PasswordRecipient.PKCS5_SCHEME2)
        {
            return PBEParametersGenerator.PKCS5PasswordToBytes(password);
        }

        return PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(password);
    }
}
