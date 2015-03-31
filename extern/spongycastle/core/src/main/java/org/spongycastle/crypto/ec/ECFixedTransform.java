package org.spongycastle.crypto.ec;

import java.math.BigInteger;

import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.math.ec.ECPoint;

/**
 * this transforms the original randomness used for an ElGamal encryption by a fixed value.
 */
public class ECFixedTransform
    implements ECPairFactorTransform
{
    private ECPublicKeyParameters key;

    private BigInteger k;

    public ECFixedTransform(BigInteger k)
    {
        this.k = k;
    }

    /**
     * initialise the underlying EC ElGamal engine.
     *
     * @param param the necessary EC key parameters.
     */
    public void init(
        CipherParameters    param)
    {
        if (!(param instanceof ECPublicKeyParameters))
        {
            throw new IllegalArgumentException("ECPublicKeyParameters are required for fixed transform.");
        }

        this.key = (ECPublicKeyParameters)param;
    }

    /**
     * Transform an existing cipher test pair using the ElGamal algorithm. Note: it is assumed this
     * transform has been initialised with the same public key that was used to create the original
     * cipher text.
     *
     * @param cipherText the EC point to process.
     * @return returns a new ECPair representing the result of the process.
     */
    public ECPair transform(ECPair cipherText)
    {
        if (key == null)
        {
            throw new IllegalStateException("ECFixedTransform not initialised");
        }

        ECPoint  g = key.getParameters().getG();
        ECPoint  gamma = g.multiply(k);
        ECPoint  phi = key.getQ().multiply(k).add(cipherText.getY());

        return new ECPair(cipherText.getX().add(gamma).normalize(), phi.normalize());
    }

    /**
     * Return the last transform value used by the transform
     *
     * @return a BigInteger representing k value.
     */
    public BigInteger getTransformValue()
    {
        return k;
    }
}
