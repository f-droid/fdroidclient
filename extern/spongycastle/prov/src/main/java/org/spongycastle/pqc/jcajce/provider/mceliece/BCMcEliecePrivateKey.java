package org.spongycastle.pqc.jcajce.provider.mceliece;

import java.io.IOException;
import java.security.PrivateKey;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.pqc.asn1.McEliecePrivateKey;
import org.spongycastle.pqc.crypto.mceliece.McElieceKeyPairGenerator;
import org.spongycastle.pqc.crypto.mceliece.McElieceParameters;
import org.spongycastle.pqc.crypto.mceliece.McEliecePrivateKeyParameters;
import org.spongycastle.pqc.jcajce.spec.McEliecePrivateKeySpec;
import org.spongycastle.pqc.math.linearalgebra.GF2Matrix;
import org.spongycastle.pqc.math.linearalgebra.GF2mField;
import org.spongycastle.pqc.math.linearalgebra.Permutation;
import org.spongycastle.pqc.math.linearalgebra.PolynomialGF2mSmallM;

/**
 * This class implements a McEliece private key and is usually instantiated by
 * the {@link McElieceKeyPairGenerator} or {@link McElieceKeyFactorySpi}.
 */
public class BCMcEliecePrivateKey
    implements CipherParameters, PrivateKey
{

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    // the OID of the algorithm
    private String oid;

    // the length of the code
    private int n;

    // the dimension of the code, where <tt>k &gt;= n - mt</tt>
    private int k;

    // the underlying finite field
    private GF2mField field;

    // the irreducible Goppa polynomial
    private PolynomialGF2mSmallM goppaPoly;

    // the matrix S^-1
    private GF2Matrix sInv;

    // the permutation P1 used to generate the systematic check matrix
    private Permutation p1;

    // the permutation P2 used to compute the public generator matrix
    private Permutation p2;

    // the canonical check matrix of the code
    private GF2Matrix h;

    // the matrix used to compute square roots in <tt>(GF(2^m))^t</tt>
    private PolynomialGF2mSmallM[] qInv;

    private McElieceParameters mcElieceParams;


    /**
     * Constructor (used by the {@link McElieceKeyPairGenerator}).
     *
     * @param oid
     * @param n         the length of the code
     * @param k         the dimension of the code
     * @param field     the field polynomial defining the finite field
     *                  <tt>GF(2<sup>m</sup>)</tt>
     * @param goppaPoly the irreducible Goppa polynomial
     * @param sInv      the matrix <tt>S<sup>-1</sup></tt>
     * @param p1        the permutation used to generate the systematic check
     *                  matrix
     * @param p2        the permutation used to compute the public generator
     *                  matrix
     * @param h         the canonical check matrix
     * @param qInv      the matrix used to compute square roots in
     *                  <tt>(GF(2<sup>m</sup>))<sup>t</sup></tt>
     */
    public BCMcEliecePrivateKey(String oid, int n, int k, GF2mField field,
                                PolynomialGF2mSmallM goppaPoly, GF2Matrix sInv, Permutation p1,
                                Permutation p2, GF2Matrix h, PolynomialGF2mSmallM[] qInv)
    {
        this.oid = oid;
        this.n = n;
        this.k = k;
        this.field = field;
        this.goppaPoly = goppaPoly;
        this.sInv = sInv;
        this.p1 = p1;
        this.p2 = p2;
        this.h = h;
        this.qInv = qInv;
    }

    /**
     * Constructor (used by the {@link McElieceKeyFactorySpi}).
     *
     * @param keySpec a {@link McEliecePrivateKeySpec}
     */
    public BCMcEliecePrivateKey(McEliecePrivateKeySpec keySpec)
    {
        this(keySpec.getOIDString(), keySpec.getN(), keySpec.getK(), keySpec.getField(), keySpec
            .getGoppaPoly(), keySpec.getSInv(), keySpec.getP1(), keySpec
            .getP2(), keySpec.getH(), keySpec.getQInv());
    }

    public BCMcEliecePrivateKey(McEliecePrivateKeyParameters params)
    {
        this(params.getOIDString(), params.getN(), params.getK(), params.getField(), params.getGoppaPoly(),
            params.getSInv(), params.getP1(), params.getP2(), params.getH(), params.getQInv());

        this.mcElieceParams = params.getParameters();
    }


    /**
     * Return the name of the algorithm.
     *
     * @return "McEliece"
     */
    public String getAlgorithm()
    {
        return "McEliece";
    }

    /**
     * @return the length of the code
     */
    public int getN()
    {
        return n;
    }

    /**
     * @return the dimension of the code
     */
    public int getK()
    {
        return k;
    }

    /**
     * @return the finite field
     */
    public GF2mField getField()
    {
        return field;
    }

    /**
     * @return the irreducible Goppa polynomial
     */
    public PolynomialGF2mSmallM getGoppaPoly()
    {
        return goppaPoly;
    }

    /**
     * @return the k x k random binary non-singular matrix S
     */
    public GF2Matrix getSInv()
    {
        return sInv;
    }

    /**
     * @return the permutation used to generate the systematic check matrix
     */
    public Permutation getP1()
    {
        return p1;
    }

    /**
     * @return the permutation used to compute the public generator matrix
     */
    public Permutation getP2()
    {
        return p2;
    }

    /**
     * @return the canonical check matrix
     */
    public GF2Matrix getH()
    {
        return h;
    }

    /**
     * @return the matrix for computing square roots in <tt>(GF(2^m))^t</tt>
     */
    public PolynomialGF2mSmallM[] getQInv()
    {
        return qInv;
    }

    /**
     * @return the OID of the algorithm
     */
    public String getOIDString()
    {
        return oid;
    }

    /**
     * @return a human readable form of the key
     */
    public String toString()
    {
        String result = " length of the code          : " + n + "\n";
        result += " dimension of the code       : " + k + "\n";
        result += " irreducible Goppa polynomial: " + goppaPoly + "\n";
        result += " (k x k)-matrix S^-1         : " + sInv + "\n";
        result += " permutation P1              : " + p1 + "\n";
        result += " permutation P2              : " + p2;
        return result;
    }

    /**
     * Compare this key with another object.
     *
     * @param other the other object
     * @return the result of the comparison
     */
    public boolean equals(Object other)
    {
        if (!(other instanceof BCMcEliecePrivateKey))
        {
            return false;
        }
        BCMcEliecePrivateKey otherKey = (BCMcEliecePrivateKey)other;

        return (n == otherKey.n) && (k == otherKey.k)
            && field.equals(otherKey.field)
            && goppaPoly.equals(otherKey.goppaPoly)
            && sInv.equals(otherKey.sInv) && p1.equals(otherKey.p1)
            && p2.equals(otherKey.p2) && h.equals(otherKey.h);
    }

    /**
     * @return the hash code of this key
     */
    public int hashCode()
    {
        return k + n + field.hashCode() + goppaPoly.hashCode()
            + sInv.hashCode() + p1.hashCode() + p2.hashCode()
            + h.hashCode();
    }

    /**
     * @return the OID to encode in the SubjectPublicKeyInfo structure
     */
    protected ASN1ObjectIdentifier getOID()
    {
        return new ASN1ObjectIdentifier(McElieceKeyFactorySpi.OID);
    }

    /**
     * @return the algorithm parameters to encode in the SubjectPublicKeyInfo
     *         structure
     */
    protected ASN1Primitive getAlgParams()
    {
        return null; // FIXME: needed at all?
    }

    /**
     * Return the key data to encode in the SubjectPublicKeyInfo structure.
     * <p/>
     * The ASN.1 definition of the key structure is
     * <p/>
     * <pre>
     *   McEliecePrivateKey ::= SEQUENCE {
     *     n          INTEGER                   -- length of the code
     *     k          INTEGER                   -- dimension of the code
     *     fieldPoly  OCTET STRING              -- field polynomial defining GF(2&circ;m)
     *     goppaPoly  OCTET STRING              -- irreducible Goppa polynomial
     *     sInv       OCTET STRING              -- matrix S&circ;-1
     *     p1         OCTET STRING              -- permutation P1
     *     p2         OCTET STRING              -- permutation P2
     *     h          OCTET STRING              -- canonical check matrix
     *     qInv       SEQUENCE OF OCTET STRING  -- matrix used to compute square roots
     *   }
     * </pre>
     *
     * @return the key data to encode in the SubjectPublicKeyInfo structure
     */
    public byte[] getEncoded()
    {
        McEliecePrivateKey privateKey = new McEliecePrivateKey(new ASN1ObjectIdentifier(oid), n, k, field, goppaPoly, sInv, p1, p2, h, qInv);
        PrivateKeyInfo pki;
        try
        {
            AlgorithmIdentifier algorithmIdentifier = new AlgorithmIdentifier(this.getOID(), DERNull.INSTANCE);
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
        // TODO Auto-generated method stub
        return null;
    }

    public McElieceParameters getMcElieceParameters()
    {
        return mcElieceParams;
    }


}
