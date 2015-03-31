package org.spongycastle.pqc.jcajce.provider.rainbow;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.Arrays;

import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.pqc.asn1.PQCObjectIdentifiers;
import org.spongycastle.pqc.asn1.RainbowPrivateKey;
import org.spongycastle.pqc.crypto.rainbow.Layer;
import org.spongycastle.pqc.crypto.rainbow.RainbowPrivateKeyParameters;
import org.spongycastle.pqc.crypto.rainbow.util.RainbowUtil;
import org.spongycastle.pqc.jcajce.spec.RainbowPrivateKeySpec;

/**
 * The Private key in Rainbow consists of the linear affine maps L1, L2 and the
 * map F, consisting of quadratic polynomials. In this implementation, we
 * denote: L1 = A1*x + b1 L2 = A2*x + b2
 * <p/>
 * The coefficients of the polynomials in F are stored in 3-dimensional arrays
 * per layer. The indices of these arrays denote the polynomial, and the
 * variables.
 * <p/>
 * More detailed information about the private key is to be found in the paper
 * of Jintai Ding, Dieter Schmidt: Rainbow, a New Multivariable Polynomial
 * Signature Scheme. ACNS 2005: 164-175 (http://dx.doi.org/10.1007/11496137_12)
 */
public class BCRainbowPrivateKey
    implements PrivateKey
{
    private static final long serialVersionUID = 1L;

    // the inverse of L1
    private short[][] A1inv;

    // translation vector element of L1
    private short[] b1;

    // the inverse of L2
    private short[][] A2inv;

    // translation vector of L2
    private short[] b2;

    /*
      * components of F
      */
    private Layer[] layers;

    // set of vinegar vars per layer.
    private int[] vi;


    /**
     * Constructor.
     *
     * @param A1inv
     * @param b1
     * @param A2inv
     * @param b2
     * @param layers
     */
    public BCRainbowPrivateKey(short[][] A1inv, short[] b1, short[][] A2inv,
                               short[] b2, int[] vi, Layer[] layers)
    {
        this.A1inv = A1inv;
        this.b1 = b1;
        this.A2inv = A2inv;
        this.b2 = b2;
        this.vi = vi;
        this.layers = layers;
    }

    /**
     * Constructor (used by the {@link RainbowKeyFactorySpi}).
     *
     * @param keySpec a {@link RainbowPrivateKeySpec}
     */
    public BCRainbowPrivateKey(RainbowPrivateKeySpec keySpec)
    {
        this(keySpec.getInvA1(), keySpec.getB1(), keySpec.getInvA2(), keySpec
            .getB2(), keySpec.getVi(), keySpec.getLayers());
    }

    public BCRainbowPrivateKey(
        RainbowPrivateKeyParameters params)
    {
        this(params.getInvA1(), params.getB1(), params.getInvA2(), params.getB2(), params.getVi(), params.getLayers());
    }

    /**
     * Getter for the inverse matrix of A1.
     *
     * @return the A1inv inverse
     */
    public short[][] getInvA1()
    {
        return this.A1inv;
    }

    /**
     * Getter for the translation part of the private quadratic map L1.
     *
     * @return b1 the translation part of L1
     */
    public short[] getB1()
    {
        return this.b1;
    }

    /**
     * Getter for the translation part of the private quadratic map L2.
     *
     * @return b2 the translation part of L2
     */
    public short[] getB2()
    {
        return this.b2;
    }

    /**
     * Getter for the inverse matrix of A2
     *
     * @return the A2inv
     */
    public short[][] getInvA2()
    {
        return this.A2inv;
    }

    /**
     * Returns the layers contained in the private key
     *
     * @return layers
     */
    public Layer[] getLayers()
    {
        return this.layers;
    }

    /**
     * Returns the array of vi-s
     *
     * @return the vi
     */
    public int[] getVi()
    {
        return vi;
    }

    /**
     * Compare this Rainbow private key with another object.
     *
     * @param other the other object
     * @return the result of the comparison
     */
    public boolean equals(Object other)
    {
        if (other == null || !(other instanceof BCRainbowPrivateKey))
        {
            return false;
        }
        BCRainbowPrivateKey otherKey = (BCRainbowPrivateKey)other;

        boolean eq = true;
        // compare using shortcut rule ( && instead of &)
        eq = eq && RainbowUtil.equals(A1inv, otherKey.getInvA1());
        eq = eq && RainbowUtil.equals(A2inv, otherKey.getInvA2());
        eq = eq && RainbowUtil.equals(b1, otherKey.getB1());
        eq = eq && RainbowUtil.equals(b2, otherKey.getB2());
        eq = eq && Arrays.equals(vi, otherKey.getVi());
        if (layers.length != otherKey.getLayers().length)
        {
            return false;
        }
        for (int i = layers.length - 1; i >= 0; i--)
        {
            eq &= layers[i].equals(otherKey.getLayers()[i]);
        }
        return eq;
    }

    public int hashCode()
    {
        int hash = layers.length;

        hash = hash * 37 + org.spongycastle.util.Arrays.hashCode(A1inv);
        hash = hash * 37 + org.spongycastle.util.Arrays.hashCode(b1);
        hash = hash * 37 + org.spongycastle.util.Arrays.hashCode(A2inv);
        hash = hash * 37 + org.spongycastle.util.Arrays.hashCode(b2);
        hash = hash * 37 + org.spongycastle.util.Arrays.hashCode(vi);

        for (int i = layers.length - 1; i >= 0; i--)
        {
            hash = hash * 37 + layers[i].hashCode();
        }


        return hash;
    }

    /**
     * @return name of the algorithm - "Rainbow"
     */
    public final String getAlgorithm()
    {
        return "Rainbow";
    }

    public byte[] getEncoded()
    {
        RainbowPrivateKey privateKey = new RainbowPrivateKey(A1inv, b1, A2inv, b2, vi, layers);

        PrivateKeyInfo pki;
        try
        {
            AlgorithmIdentifier algorithmIdentifier = new AlgorithmIdentifier(PQCObjectIdentifiers.rainbow, DERNull.INSTANCE);
            pki = new PrivateKeyInfo(algorithmIdentifier, privateKey);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
        try
        {
            byte[] encoded = pki.getEncoded();
            return encoded;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public String getFormat()
    {
        return "PKCS#8";
    }
}
