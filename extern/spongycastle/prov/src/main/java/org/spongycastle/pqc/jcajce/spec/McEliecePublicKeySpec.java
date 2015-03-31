package org.spongycastle.pqc.jcajce.spec;


import java.security.spec.KeySpec;

import org.spongycastle.pqc.math.linearalgebra.GF2Matrix;

/**
 * This class provides a specification for a McEliece public key.
 *
 * @see org.spongycastle.pqc.jcajce.provider.mceliece.BCMcEliecePublicKey
 */
public class McEliecePublicKeySpec
    implements KeySpec
{

    // the OID of the algorithm
    private String oid;

    // the length of the code
    private int n;

    // the error correction capability of the code
    private int t;

    // the generator matrix
    private GF2Matrix g;

    /**
     * Constructor (used by {@link org.spongycastle.pqc.jcajce.provider.mceliece.McElieceKeyFactorySpi}).
     *
     * @param oid
     * @param n   the length of the code
     * @param t   the error correction capability of the code
     * @param g   the generator matrix
     */
    public McEliecePublicKeySpec(String oid, int n, int t, GF2Matrix g)
    {
        this.oid = oid;
        this.n = n;
        this.t = t;
        this.g = new GF2Matrix(g);
    }

    /**
     * Constructor (used by {@link org.spongycastle.pqc.jcajce.provider.mceliece.McElieceKeyFactorySpi}).
     *
     * @param oid
     * @param n    the length of the code
     * @param t    the error correction capability of the code
     * @param encG the encoded generator matrix
     */
    public McEliecePublicKeySpec(String oid, int t, int n, byte[] encG)
    {
        this.oid = oid;
        this.n = n;
        this.t = t;
        this.g = new GF2Matrix(encG);
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
    public GF2Matrix getG()
    {
        return g;
    }

    public String getOIDString()
    {
        return oid;

    }

}
