package org.spongycastle.operator.bc;

import java.security.SecureRandom;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.crypto.Wrapper;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithRandom;
import org.spongycastle.operator.GenericKey;
import org.spongycastle.operator.OperatorException;
import org.spongycastle.operator.SymmetricKeyWrapper;

public class BcSymmetricKeyWrapper
    extends SymmetricKeyWrapper
{
    private SecureRandom random;
    private Wrapper wrapper;
    private KeyParameter wrappingKey;

    public BcSymmetricKeyWrapper(AlgorithmIdentifier wrappingAlgorithm, Wrapper wrapper, KeyParameter wrappingKey)
    {
        super(wrappingAlgorithm);

        this.wrapper = wrapper;
        this.wrappingKey = wrappingKey;
    }

    public BcSymmetricKeyWrapper setSecureRandom(SecureRandom random)
    {
        this.random = random;

        return this;
    }

    public byte[] generateWrappedKey(GenericKey encryptionKey)
        throws OperatorException
    {
        byte[] contentEncryptionKeySpec = OperatorUtils.getKeyBytes(encryptionKey);

        if (random == null)
        {
            wrapper.init(true, wrappingKey);
        }
        else
        {
            wrapper.init(true, new ParametersWithRandom(wrappingKey, random));
        }

        return wrapper.wrap(contentEncryptionKeySpec, 0, contentEncryptionKeySpec.length);
    }
}
