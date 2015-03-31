package org.spongycastle.pqc.jcajce.provider.mceliece;

import java.io.IOException;
import java.security.PublicKey;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.pqc.asn1.McEliecePublicKey;
import org.spongycastle.pqc.crypto.mceliece.McElieceKeyPairGenerator;
import org.spongycastle.pqc.crypto.mceliece.McElieceParameters;
import org.spongycastle.pqc.crypto.mceliece.McEliecePublicKeyParameters;
import org.spongycastle.pqc.jcajce.spec.McEliecePublicKeySpec;
import org.spongycastle.pqc.math.linearalgebra.GF2Matrix;

/**
 * This class implements a McEliece public key and is usually instantiated by
 * the {@link McElieceKeyPairGenerator} or {@link McElieceKeyFactorySpi}.
 */
public class BCMcEliecePublicKey
    implements CipherParameters, PublicKey
{

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    // the OID of the algorithm
    private String oid;

    /**
     * the length of the code
     */
    private int n;

    /**
     * the error correction capability of the code
     */
    private int t;

    /**
     * the generator matrix
     */
    private GF2Matrix g;

    private McElieceParameters McElieceParams;

    /**
     * Constructor (used by the {@link McElieceKeyPairGenerator}).
     *
     * @param oid
     * @param n   the length of the code
     * @param t   the error correction capability of the code
     * @param g   the generator matrix
     */
    public BCMcEliecePublicKey(String oid, int n, int t, GF2Matrix g)
    {
        this.oid = oid;
        this.n = n;
        this.t = t;
        this.g = g;
    }

    /**
     * Constructor (used by the {@link McElieceKeyFactorySpi}).
     *
     * @param keySpec a {@link McEliecePublicKeySpec}
     */
    public BCMcEliecePublicKey(McEliecePublicKeySpec keySpec)
    {
        this(keySpec.getOIDString(), keySpec.getN(), keySpec.getT(), keySpec.getG());
    }

    public BCMcEliecePublicKey(McEliecePublicKeyParameters params)
    {
        this(params.getOIDString(), params.getN(), params.getT(), params.getG());
        this.McElieceParams = params.getParameters();
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
        return g.getNumRows();
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

    /**
     * @return a human readable form of the key
     */
    public String toString()
    {
        String result = "McEliecePublicKey:\n";
        result += " length of the code         : " + n + "\n";
        result += " error correction capability: " + t + "\n";
        result += " generator matrix           : " + g.toString();
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
        if (!(other instanceof BCMcEliecePublicKey))
        {
            return false;
        }
        BCMcEliecePublicKey otherKey = (BCMcEliecePublicKey)other;

        return (n == otherKey.n) && (t == otherKey.t) && g.equals(otherKey.g);
    }

    /**
     * @return the hash code of this key
     */
    public int hashCode()
    {
        return n + t + g.hashCode();
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
     * Return the keyData to encode in the SubjectPublicKeyInfo structure.
     * <p/>
     * The ASN.1 definition of the key structure is
     * <p/>
     * <pre>
     *       McEliecePublicKey ::= SEQUENCE {
     *         n           Integer      -- length of the code
     *         t           Integer      -- error correcting capability
     *         matrixG     OctetString  -- generator matrix as octet string
     *       }
     * </pre>
     *
     * @return the keyData to encode in the SubjectPublicKeyInfo structure
     */
    public byte[] getEncoded()
    {
        McEliecePublicKey key = new McEliecePublicKey(new ASN1ObjectIdentifier(oid), n, t, g);
        AlgorithmIdentifier algorithmIdentifier = new AlgorithmIdentifier(this.getOID(), DERNull.INSTANCE);

        try
        {
            SubjectPublicKeyInfo subjectPublicKeyInfo = new SubjectPublicKeyInfo(algorithmIdentifier, key);

            return subjectPublicKeyInfo.getEncoded();
        }
        catch (IOException e)
        {
            return null;
        }
    }

    public String getFormat()
    {
        return null;
    }

    public McElieceParameters getMcElieceParameters()
    {
        return McElieceParams;
    }
}
