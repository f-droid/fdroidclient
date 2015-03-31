package org.spongycastle.pqc.jcajce.spec;


import java.security.spec.KeySpec;

/**
 * This class provides a specification for a RainbowSignature public key.
 *
 * @see KeySpec
 */
public class RainbowPublicKeySpec
    implements KeySpec
{
    private short[][] coeffquadratic;
    private short[][] coeffsingular;
    private short[] coeffscalar;
    private int docLength; // length of possible document to sign

    /**
     * Constructor
     *
     * @param docLength
     * @param coeffquadratic
     * @param coeffSingular
     * @param coeffScalar
     */
    public RainbowPublicKeySpec(int docLength,
                                short[][] coeffquadratic, short[][] coeffSingular,
                                short[] coeffScalar)
    {
        this.docLength = docLength;
        this.coeffquadratic = coeffquadratic;
        this.coeffsingular = coeffSingular;
        this.coeffscalar = coeffScalar;
    }

    /**
     * @return the docLength
     */
    public int getDocLength()
    {
        return this.docLength;
    }

    /**
     * @return the coeffquadratic
     */
    public short[][] getCoeffQuadratic()
    {
        return coeffquadratic;
    }

    /**
     * @return the coeffsingular
     */
    public short[][] getCoeffSingular()
    {
        return coeffsingular;
    }

    /**
     * @return the coeffscalar
     */
    public short[] getCoeffScalar()
    {
        return coeffscalar;
    }
}
