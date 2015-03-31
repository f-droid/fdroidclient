package org.spongycastle.crypto.prng;

import java.security.SecureRandom;

/**
 * An EntropySourceProvider where entropy generation is based on a SecureRandom output using SecureRandom.generateSeed().
 */
public class BasicEntropySourceProvider
    implements EntropySourceProvider
{
    private SecureRandom _sr;
    private boolean      _predictionResistant;

    /**
     * Create a entropy source provider based on the passed in SecureRandom.
     *
     * @param random the SecureRandom to base EntropySource construction on.
     * @param isPredictionResistant boolean indicating if the SecureRandom is based on prediction resistant entropy or not (true if it is).
     */
    public BasicEntropySourceProvider(SecureRandom random, boolean isPredictionResistant)
    {
        _sr = random;
        _predictionResistant = isPredictionResistant;
    }

    /**
     * Return an entropy source that will create bitsRequired bits of entropy on
     * each invocation of getEntropy().
     *
     * @param bitsRequired size (in bits) of entropy to be created by the provided source.
     * @return an EntropySource that generates bitsRequired bits of entropy on each call to its getEntropy() method.
     */
    public EntropySource get(final int bitsRequired)
    {
        return new EntropySource()
        {
            public boolean isPredictionResistant()
            {
                return _predictionResistant;
            }

            public byte[] getEntropy()
            {
                byte[] rv = new byte[(bitsRequired + 7) / 8];

                _sr.nextBytes(rv);

		return rv;
            }

            public int entropySize()
            {
                return bitsRequired;
            }
        };
    }
}
