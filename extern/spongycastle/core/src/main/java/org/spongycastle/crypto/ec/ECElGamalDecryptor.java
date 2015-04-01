package org.spongycastle.crypto.ec;

import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.math.ec.ECPoint;

/**
 * this does your basic decryption ElGamal style using EC
 */
public class ECElGamalDecryptor
    implements ECDecryptor
{
    private ECPrivateKeyParameters key;

    /**
     * initialise the decryptor.
     *
     * @param param the necessary EC key parameters.
     */
    public void init(
        CipherParameters param)
    {
        if (!(param instanceof ECPrivateKeyParameters))
        {
            throw new IllegalArgumentException("ECPrivateKeyParameters are required for decryption.");
        }

        this.key = (ECPrivateKeyParameters)param;
    }

    /**
     * Decrypt an EC pair producing the original EC point.
     *
     * @param pair the EC point pair to process.
     * @return the result of the Elgamal process.
     */
    public ECPoint decrypt(ECPair pair)
    {
        if (key == null)
        {
            throw new IllegalStateException("ECElGamalDecryptor not initialised");
        }

        ECPoint tmp = pair.getX().multiply(key.getD());

        return pair.getY().subtract(tmp).normalize();
    }
}
