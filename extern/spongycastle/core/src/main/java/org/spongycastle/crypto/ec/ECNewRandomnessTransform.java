package org.spongycastle.crypto.ec;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.crypto.params.ParametersWithRandom;
import org.spongycastle.math.ec.ECPoint;

/**
 * this transforms the original randomness used for an ElGamal encryption.
 */
public class ECNewRandomnessTransform
    implements ECPairFactorTransform
{
    private ECPublicKeyParameters key;
    private SecureRandom          random;

    private BigInteger            lastK;

    /**
     * initialise the underlying EC ElGamal engine.
     *
     * @param param the necessary EC key parameters.
     */
    public void init(
        CipherParameters    param)
    {
        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom    p = (ParametersWithRandom)param;

            if (!(p.getParameters() instanceof ECPublicKeyParameters))
            {
                throw new IllegalArgumentException("ECPublicKeyParameters are required for new randomness transform.");
            }

            this.key = (ECPublicKeyParameters)p.getParameters();
            this.random = p.getRandom();
        }
        else
        {
            if (!(param instanceof ECPublicKeyParameters))
            {
                throw new IllegalArgumentException("ECPublicKeyParameters are required for new randomness transform.");
            }

            this.key = (ECPublicKeyParameters)param;
            this.random = new SecureRandom();
        }
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
            throw new IllegalStateException("ECNewRandomnessTransform not initialised");
        }

        BigInteger             n = key.getParameters().getN();
        BigInteger             k = ECUtil.generateK(n, random);

        ECPoint  g = key.getParameters().getG();
        ECPoint  gamma = g.multiply(k);
        ECPoint  phi = key.getQ().multiply(k).add(cipherText.getY());

        lastK = k;

        return new ECPair(cipherText.getX().add(gamma).normalize(), phi.normalize());
    }

    /**
     * Return the last random value generated for a transform
     *
     * @return a BigInteger representing the last random value.
     */
    public BigInteger getTransformValue()
    {
        return lastK;
    }
}
