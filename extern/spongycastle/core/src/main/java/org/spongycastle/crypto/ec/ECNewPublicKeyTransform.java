package org.spongycastle.crypto.ec;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.crypto.params.ParametersWithRandom;
import org.spongycastle.math.ec.ECPoint;

/**
 * this does your basic Elgamal encryption algorithm using EC
 */
public class ECNewPublicKeyTransform
    implements ECPairTransform
{
    private ECPublicKeyParameters key;
    private SecureRandom          random;

    /**
     * initialise the EC Elgamal engine.
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
                throw new IllegalArgumentException("ECPublicKeyParameters are required for new public key transform.");
            }
            this.key = (ECPublicKeyParameters)p.getParameters();
            this.random = p.getRandom();
        }
        else
        {
            if (!(param instanceof ECPublicKeyParameters))
            {
                throw new IllegalArgumentException("ECPublicKeyParameters are required for new public key transform.");
            }

            this.key = (ECPublicKeyParameters)param;
            this.random = new SecureRandom();
        }
    }

    /**
     * Transform an existing cipher test pair using the ElGamal algorithm. Note: the input cipherText will
     * need to be preserved in order to complete the transformation to the new public key.
     *
     * @param cipherText the EC point to process.
     * @return returns a new ECPair representing the result of the process.
     */
    public ECPair transform(ECPair cipherText)
    {
        if (key == null)
        {
            throw new IllegalStateException("ECNewPublicKeyTransform not initialised");
        }

        BigInteger             n = key.getParameters().getN();
        BigInteger             k = ECUtil.generateK(n, random);

        ECPoint  g = key.getParameters().getG();
        ECPoint  gamma = g.multiply(k);
        ECPoint  phi = key.getQ().multiply(k).add(cipherText.getY());

        return new ECPair(gamma.normalize(), phi.normalize());
    }
}
