package org.spongycastle.pqc.jcajce.provider.mceliece;

import java.io.IOException;
import java.security.PrivateKey;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.pqc.asn1.McElieceCCA2PrivateKey;
import org.spongycastle.pqc.crypto.mceliece.McElieceCCA2KeyPairGenerator;
import org.spongycastle.pqc.crypto.mceliece.McElieceCCA2Parameters;
import org.spongycastle.pqc.crypto.mceliece.McElieceCCA2PrivateKeyParameters;
import org.spongycastle.pqc.jcajce.spec.McElieceCCA2PrivateKeySpec;
import org.spongycastle.pqc.math.linearalgebra.GF2Matrix;
import org.spongycastle.pqc.math.linearalgebra.GF2mField;
import org.spongycastle.pqc.math.linearalgebra.Permutation;
import org.spongycastle.pqc.math.linearalgebra.PolynomialGF2mSmallM;

/**
 * This class implements a McEliece CCA2 private key and is usually instantiated
 * by the {@link McElieceCCA2KeyPairGenerator} or {@link McElieceCCA2KeyFactorySpi}.
 *
 * @see McElieceCCA2KeyPairGenerator
 */
public class BCMcElieceCCA2PrivateKey
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

    // the dimension of the code, k>=n-mt
    private int k;

    // the finte field GF(2^m)
    private GF2mField field;

    // the irreducible Goppa polynomial
    private PolynomialGF2mSmallM goppaPoly;

    // the permutation
    private Permutation p;

    // the canonical check matrix
    private GF2Matrix h;

    // the matrix used to compute square roots in (GF(2^m))^t
    private PolynomialGF2mSmallM[] qInv;

    private McElieceCCA2Parameters mcElieceCCA2Params;

    /**
     * Constructor (used by the {@link McElieceCCA2KeyPairGenerator}).
     *
     * @param n     the length of the code
     * @param k     the dimension of the code
     * @param field the field polynomial
     * @param gp    the irreducible Goppa polynomial
     * @param p     the permutation
     * @param h     the canonical check matrix
     * @param qInv  the matrix used to compute square roots in
     *              <tt>(GF(2^m))^t</tt>
     */
    public BCMcElieceCCA2PrivateKey(String oid, int n, int k, GF2mField field,
                                    PolynomialGF2mSmallM gp, Permutation p, GF2Matrix h,
                                    PolynomialGF2mSmallM[] qInv)
    {
        this.oid = oid;
        this.n = n;
        this.k = k;
        this.field = field;
        this.goppaPoly = gp;
        this.p = p;
        this.h = h;
        this.qInv = qInv;
    }

    /**
     * Constructor (used by the {@link McElieceCCA2KeyFactorySpi}).
     *
     * @param keySpec a {@link McElieceCCA2PrivateKeySpec}
     */
    public BCMcElieceCCA2PrivateKey(McElieceCCA2PrivateKeySpec keySpec)
    {
        this(keySpec.getOIDString(), keySpec.getN(), keySpec.getK(), keySpec.getField(), keySpec
            .getGoppaPoly(), keySpec.getP(), keySpec.getH(), keySpec
            .getQInv());
    }

    public BCMcElieceCCA2PrivateKey(McElieceCCA2PrivateKeyParameters params)
    {
        this(params.getOIDString(), params.getN(), params.getK(), params.getField(), params.getGoppaPoly(),
            params.getP(), params.getH(), params.getQInv());
        this.mcElieceCCA2Params = params.getParameters();
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
     * @return the degree of the Goppa polynomial (error correcting capability)
     */
    public int getT()
    {
        return goppaPoly.getDegree();
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
     * @return the permutation vector
     */
    public Permutation getP()
    {
        return p;
    }

    /**
     * @return the canonical check matrix
     */
    public GF2Matrix getH()
    {
        return h;
    }

    /**
     * @return the matrix used to compute square roots in <tt>(GF(2^m))^t</tt>
     */
    public PolynomialGF2mSmallM[] getQInv()
    {
        return qInv;
    }

    /**
     * @return a human readable form of the key
     */
    public String toString()
    {
        String result = "";
        result += " extension degree of the field      : " + n + "\n";
        result += " dimension of the code              : " + k + "\n";
        result += " irreducible Goppa polynomial       : " + goppaPoly + "\n";
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
        if (other == null || !(other instanceof BCMcElieceCCA2PrivateKey))
        {
            return false;
        }

        BCMcElieceCCA2PrivateKey otherKey = (BCMcElieceCCA2PrivateKey)other;

        return (n == otherKey.n) && (k == otherKey.k)
            && field.equals(otherKey.field)
            && goppaPoly.equals(otherKey.goppaPoly) && p.equals(otherKey.p)
            && h.equals(otherKey.h);
    }

    /**
     * @return the hash code of this key
     */
    public int hashCode()
    {
        return k + n + field.hashCode() + goppaPoly.hashCode() + p.hashCode()
            + h.hashCode();
    }

    /**
     * @return the OID of the algorithm
     */
    public String getOIDString()
    {
        return oid;
    }

    /**
     * @return the OID to encode in the SubjectPublicKeyInfo structure
     */
    protected ASN1ObjectIdentifier getOID()
    {
        return new ASN1ObjectIdentifier(McElieceCCA2KeyFactorySpi.OID);
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
     * Return the keyData to encode in the SubjectPublicKeyInfo structure.
     * <p/>
     * The ASN.1 definition of the key structure is
     * <p/>
     * <pre>
     *   McEliecePrivateKey ::= SEQUENCE {
     *     m             INTEGER                  -- extension degree of the field
     *     k             INTEGER                  -- dimension of the code
     *     field         OCTET STRING             -- field polynomial
     *     goppaPoly     OCTET STRING             -- irreducible Goppa polynomial
     *     p             OCTET STRING             -- permutation vector
     *     matrixH       OCTET STRING             -- canonical check matrix
     *     sqRootMatrix  SEQUENCE OF OCTET STRING -- square root matrix
     *   }
     * </pre>
     *
     * @return the keyData to encode in the SubjectPublicKeyInfo structure
     */
    public byte[] getEncoded()
    {
        McElieceCCA2PrivateKey privateKey = new McElieceCCA2PrivateKey(new ASN1ObjectIdentifier(oid), n, k, field, goppaPoly, p, h, qInv);
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

    public McElieceCCA2Parameters getMcElieceCCA2Parameters()
    {
        return mcElieceCCA2Params;
    }

}
