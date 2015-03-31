package org.spongycastle.crypto.ec;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.crypto.params.ParametersWithRandom;
import org.spongycastle.math.ec.ECPoint;

/**
 * this does your basic ElGamal encryption algorithm using EC
 */
public class ECElGamalEncryptor
    implements ECEncryptor
{
    private ECPublicKeyParameters key;
    private SecureRandom          random;

    /**
     * initialise the encryptor.
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
                throw new IllegalArgumentException("ECPublicKeyParameters are required for encryption.");
            }
            this.key = (ECPublicKeyParameters)p.getParameters();
            this.random = p.getRandom();
        }
        else
        {
            if (!(param instanceof ECPublicKeyParameters))
            {
                throw new IllegalArgumentException("ECPublicKeyParameters are required for encryption.");
            }

            this.key = (ECPublicKeyParameters)param;
            this.random = new SecureRandom();
        }
    }

    /**
     * Process a single EC point using the basic ElGamal algorithm.
     *
     * @param point the EC point to process.
     * @return the result of the Elgamal process.
     */
    public ECPair encrypt(ECPoint point)
    {
        if (key == null)
        {
            throw new IllegalStateException("ECElGamalEncryptor not initialised");
        }

        BigInteger             n = key.getParameters().getN();
        BigInteger             k = ECUtil.generateK(n, random);

        ECPoint  g = key.getParameters().getG();
        ECPoint  gamma = g.multiply(k);
        ECPoint  phi = key.getQ().multiply(k).add(point);

        return new ECPair(gamma.normalize(), phi.normalize());
    }
}
