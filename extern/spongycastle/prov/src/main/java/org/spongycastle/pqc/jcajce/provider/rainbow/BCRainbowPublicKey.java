package org.spongycastle.pqc.jcajce.provider.rainbow;

import java.security.PublicKey;

import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.pqc.asn1.PQCObjectIdentifiers;
import org.spongycastle.pqc.asn1.RainbowPublicKey;
import org.spongycastle.pqc.crypto.rainbow.RainbowParameters;
import org.spongycastle.pqc.crypto.rainbow.RainbowPublicKeyParameters;
import org.spongycastle.pqc.crypto.rainbow.util.RainbowUtil;
import org.spongycastle.pqc.jcajce.provider.util.KeyUtil;
import org.spongycastle.pqc.jcajce.spec.RainbowPublicKeySpec;
import org.spongycastle.util.Arrays;

/**
 * This class implements CipherParameters and PublicKey.
 * <p/>
 * The public key in Rainbow consists of n - v1 polynomial components of the
 * private key's F and the field structure of the finite field k.
 * <p/>
 * The quadratic (or mixed) coefficients of the polynomials from the public key
 * are stored in the 2-dimensional array in lexicographical order, requiring n *
 * (n + 1) / 2 entries for each polynomial. The singular terms are stored in a
 * 2-dimensional array requiring n entries per polynomial, the scalar term of
 * each polynomial is stored in a 1-dimensional array.
 * <p/>
 * More detailed information on the public key is to be found in the paper of
 * Jintai Ding, Dieter Schmidt: Rainbow, a New Multivariable Polynomial
 * Signature Scheme. ACNS 2005: 164-175 (http://dx.doi.org/10.1007/11496137_12)
 */
public class BCRainbowPublicKey
    implements PublicKey
{
    private static final long serialVersionUID = 1L;

    private short[][] coeffquadratic;
    private short[][] coeffsingular;
    private short[] coeffscalar;
    private int docLength; // length of possible document to sign

    private RainbowParameters rainbowParams;

    /**
     * Constructor
     *
     * @param docLength
     * @param coeffQuadratic
     * @param coeffSingular
     * @param coeffScalar
     */
    public BCRainbowPublicKey(int docLength,
                              short[][] coeffQuadratic, short[][] coeffSingular,
                              short[] coeffScalar)
    {
        this.docLength = docLength;
        this.coeffquadratic = coeffQuadratic;
        this.coeffsingular = coeffSingular;
        this.coeffscalar = coeffScalar;
    }

    /**
     * Constructor (used by the {@link RainbowKeyFactorySpi}).
     *
     * @param keySpec a {@link RainbowPublicKeySpec}
     */
    public BCRainbowPublicKey(RainbowPublicKeySpec keySpec)
    {
        this(keySpec.getDocLength(), keySpec.getCoeffQuadratic(), keySpec
            .getCoeffSingular(), keySpec.getCoeffScalar());
    }

    public BCRainbowPublicKey(
        RainbowPublicKeyParameters params)
    {
        this(params.getDocLength(), params.getCoeffQuadratic(), params.getCoeffSingular(), params.getCoeffScalar());
    }

    /**
     * @return the docLength
     */
    public int getDocLength()
    {
        return this.docLength;
    }

    /**
     * @return the coeffQuadratic
     */
    public short[][] getCoeffQuadratic()
    {
        return coeffquadratic;
    }

    /**
     * @return the coeffSingular
     */
    public short[][] getCoeffSingular()
    {
        short[][] copy = new short[coeffsingular.length][];

        for (int i = 0; i != coeffsingular.length; i++)
        {
            copy[i] = Arrays.clone(coeffsingular[i]);
        }

        return copy;
    }


    /**
     * @return the coeffScalar
     */
    public short[] getCoeffScalar()
    {
        return Arrays.clone(coeffscalar);
    }

    /**
     * Compare this Rainbow public key with another object.
     *
     * @param other the other object
     * @return the result of the comparison
     */
    public boolean equals(Object other)
    {
        if (other == null || !(other instanceof BCRainbowPublicKey))
        {
            return false;
        }
        BCRainbowPublicKey otherKey = (BCRainbowPublicKey)other;

        return docLength == otherKey.getDocLength()
            && RainbowUtil.equals(coeffquadratic, otherKey.getCoeffQuadratic())
            && RainbowUtil.equals(coeffsingular, otherKey.getCoeffSingular())
            && RainbowUtil.equals(coeffscalar, otherKey.getCoeffScalar());
    }

    public int hashCode()
    {
        int hash = docLength;

        hash = hash * 37 + Arrays.hashCode(coeffquadratic);
        hash = hash * 37 + Arrays.hashCode(coeffsingular);
        hash = hash * 37 + Arrays.hashCode(coeffscalar);

        return hash;
    }

    /**
     * @return name of the algorithm - "Rainbow"
     */
    public final String getAlgorithm()
    {
        return "Rainbow";
    }

    public String getFormat()
    {
        return "X.509";
    }

    public byte[] getEncoded()
    {
        RainbowPublicKey key = new RainbowPublicKey(docLength, coeffquadratic, coeffsingular, coeffscalar);
        AlgorithmIdentifier algorithmIdentifier = new AlgorithmIdentifier(PQCObjectIdentifiers.rainbow, DERNull.INSTANCE);

        return KeyUtil.getEncodedSubjectPublicKeyInfo(algorithmIdentifier, key);
    }
}
