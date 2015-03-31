package org.spongycastle.pqc.jcajce.spec;

import java.security.spec.KeySpec;

import org.spongycastle.pqc.math.linearalgebra.GF2Matrix;


/**
 * This class provides a specification for a McEliece CCA2 public key.
 *
 * @see org.spongycastle.pqc.jcajce.provider.mceliece.BCMcElieceCCA2PublicKey
 */
public class McElieceCCA2PublicKeySpec
    implements KeySpec
{

    // the OID of the algorithm
    private String oid;

    // the length of the code
    private int n;

    // the error correction capability of the code
    private int t;

    // the generator matrix
    private GF2Matrix matrixG;

    /**
     * Constructor.
     *
     * @param n      length of the code
     * @param t      error correction capability
     * @param matrix generator matrix
     */
    public McElieceCCA2PublicKeySpec(String oid, int n, int t, GF2Matrix matrix)
    {
        this.oid = oid;
        this.n = n;
        this.t = t;
        this.matrixG = new GF2Matrix(matrix);
    }

    /**
     * Constructor (used by {@link org.spongycastle.pqc.jcajce.provider.mceliece.McElieceKeyFactorySpi}).
     *
     * @param n         length of the code
     * @param t         error correction capability of the code
     * @param encMatrix encoded generator matrix
     */
    public McElieceCCA2PublicKeySpec(String oid, int n, int t, byte[] encMatrix)
    {
        this.oid = oid;
        this.n = n;
        this.t = t;
        this.matrixG = new GF2Matrix(encMatrix);
    }

    /**
     * @return the length of the code
     */
    public int getN()
    {
        return n;
    }

    /**
     * @return the error correction capability of the code
     */
    public int getT()
    {
        return t;
    }

    /**
     * @return the generator matrix
     */
    public GF2Matrix getMatrixG()
    {
        return matrixG;
    }

    public String getOIDString()
    {
        return oid;

    }
}
