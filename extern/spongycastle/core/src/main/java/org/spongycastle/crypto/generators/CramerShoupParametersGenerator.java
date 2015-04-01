package org.spongycastle.crypto.generators;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.params.CramerShoupParameters;
import org.spongycastle.crypto.params.DHParameters;
import org.spongycastle.util.BigIntegers;

public class CramerShoupParametersGenerator
{
    private static final BigInteger ONE = BigInteger.valueOf(1);

    private int size;
    private int certainty;
    private SecureRandom random;

    /**
     * Initialise the parameters generator.
     *
     * @param size      bit length for the prime p
     * @param certainty a measure of the uncertainty that the caller is willing to tolerate:
     *                  the probability that the generated modulus is prime exceeds (1 - 1/2^certainty).
     *                  The execution time of this method is proportional to the value of this parameter.
     * @param random    a source of randomness
     */
    public void init(int size, int certainty, SecureRandom random)
    {
        this.size = size;
        this.certainty = certainty;
        this.random = random;
    }

    /**
     * which generates the p and g values from the given parameters, returning
     * the CramerShoupParameters object.
     * <p/>
     * Note: can take a while...
     */
    public CramerShoupParameters generateParameters()
    {
        //
        // find a safe prime p where p = 2*q + 1, where p and q are prime.
        //
        BigInteger[] safePrimes = ParametersHelper.generateSafePrimes(size, certainty, random);

//		BigInteger p = safePrimes[0];
        BigInteger q = safePrimes[1];
        BigInteger g1 = ParametersHelper.selectGenerator(q, random);
        BigInteger g2 = ParametersHelper.selectGenerator(q, random);
        while (g1.equals(g2))
        {
            g2 = ParametersHelper.selectGenerator(q, random);
        }

        return new CramerShoupParameters(q, g1, g2, new SHA256Digest());
    }

    public CramerShoupParameters generateParameters(DHParameters dhParams)
    {
        BigInteger p = dhParams.getP();
        BigInteger g1 = dhParams.getG();

        // now we just need a second generator
        BigInteger g2 = ParametersHelper.selectGenerator(p, random);
        while (g1.equals(g2))
        {
            g2 = ParametersHelper.selectGenerator(p, random);
        }

        return new CramerShoupParameters(p, g1, g2, new SHA256Digest());
    }

    private static class ParametersHelper
    {

        private static final BigInteger TWO = BigInteger.valueOf(2);

        /*
         * Finds a pair of prime BigInteger's {p, q: p = 2q + 1}
         *
         * (see: Handbook of Applied Cryptography 4.86)
         */
        static BigInteger[] generateSafePrimes(int size, int certainty, SecureRandom random)
        {
            BigInteger p, q;
            int qLength = size - 1;

            for (; ; )
            {
                q = new BigInteger(qLength, 2, random);
                p = q.shiftLeft(1).add(ONE);
                if (p.isProbablePrime(certainty) && (certainty <= 2 || q.isProbablePrime(certainty)))
                {
                    break;
                }
            }

            return new BigInteger[]{p, q};
        }

        static BigInteger selectGenerator(BigInteger p, SecureRandom random)
        {
            BigInteger pMinusTwo = p.subtract(TWO);
            BigInteger g;

			/*
             * RFC 2631 2.2.1.2 (and see: Handbook of Applied Cryptography 4.81)
			 */
            do
            {
                BigInteger h = BigIntegers.createRandomInRange(TWO, pMinusTwo, random);

                g = h.modPow(TWO, p);
            }
            while (g.equals(ONE));

            return g;
        }
    }

}
