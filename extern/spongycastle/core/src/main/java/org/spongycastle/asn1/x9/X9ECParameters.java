package org.spongycastle.asn1.x9;

import java.math.BigInteger;

import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1Object;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECPoint;

/**
 * ASN.1 def for Elliptic-Curve ECParameters structure. See
 * X9.62, for further details.
 */
public class X9ECParameters
    extends ASN1Object
    implements X9ObjectIdentifiers
{
    private static final BigInteger   ONE = BigInteger.valueOf(1);

    private X9FieldID           fieldID;
    private ECCurve             curve;
    private ECPoint             g;
    private BigInteger          n;
    private BigInteger          h;
    private byte[]              seed;

    private X9ECParameters(
        ASN1Sequence  seq)
    {
        if (!(seq.getObjectAt(0) instanceof ASN1Integer)
           || !((ASN1Integer)seq.getObjectAt(0)).getValue().equals(ONE))
        {
            throw new IllegalArgumentException("bad version in X9ECParameters");
        }

        X9Curve     x9c = new X9Curve(
                        X9FieldID.getInstance(seq.getObjectAt(1)),
                        ASN1Sequence.getInstance(seq.getObjectAt(2)));

        this.curve = x9c.getCurve();
        Object p = seq.getObjectAt(3);

        if (p instanceof X9ECPoint)
        {
            this.g = ((X9ECPoint)p).getPoint();
        }
        else
        {
            this.g = new X9ECPoint(curve, (ASN1OctetString)p).getPoint();
        }

        this.n = ((ASN1Integer)seq.getObjectAt(4)).getValue();
        this.seed = x9c.getSeed();

        if (seq.size() == 6)
        {
            this.h = ((ASN1Integer)seq.getObjectAt(5)).getValue();
        }
    }

    public static X9ECParameters getInstance(Object obj)
    {
        if (obj instanceof X9ECParameters)
        {
            return (X9ECParameters)obj;
        }

        if (obj != null)
        {
            return new X9ECParameters(ASN1Sequence.getInstance(obj));
        }

        return null;
    }

    public X9ECParameters(
        ECCurve     curve,
        ECPoint     g,
        BigInteger  n)
    {
        this(curve, g, n, ONE, null);
    }

    public X9ECParameters(
        ECCurve     curve,
        ECPoint     g,
        BigInteger  n,
        BigInteger  h)
    {
        this(curve, g, n, h, null);
    }

    public X9ECParameters(
        ECCurve     curve,
        ECPoint     g,
        BigInteger  n,
        BigInteger  h,
        byte[]      seed)
    {
        this.curve = curve;
        this.g = g.normalize();
        this.n = n;
        this.h = h;
        this.seed = seed;

        if (curve instanceof ECCurve.Fp)
        {
            this.fieldID = new X9FieldID(((ECCurve.Fp)curve).getQ());
        }
        else
        {
            if (curve instanceof ECCurve.F2m)
            {
                ECCurve.F2m curveF2m = (ECCurve.F2m)curve;
                this.fieldID = new X9FieldID(curveF2m.getM(), curveF2m.getK1(),
                    curveF2m.getK2(), curveF2m.getK3());
            }
        }
    }

    public ECCurve getCurve()
    {
        return curve;
    }

    public ECPoint getG()
    {
        return g;
    }

    public BigInteger getN()
    {
        return n;
    }

    public BigInteger getH()
    {
        if (h == null)
        {
            return ONE;        // TODO - this should be calculated, it will cause issues with custom curves.
        }

        return h;
    }

    public byte[] getSeed()
    {
        return seed;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     *  ECParameters ::= SEQUENCE {
     *      version         INTEGER { ecpVer1(1) } (ecpVer1),
     *      fieldID         FieldID {{FieldTypes}},
     *      curve           X9Curve,
     *      base            X9ECPoint,
     *      order           INTEGER,
     *      cofactor        INTEGER OPTIONAL
     *  }
     * </pre>
     */
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(new ASN1Integer(1));
        v.add(fieldID);
        v.add(new X9Curve(curve, seed));
        v.add(new X9ECPoint(g));
        v.add(new ASN1Integer(n));

        if (h != null)
        {
            v.add(new ASN1Integer(h));
        }

        return new DERSequence(v);
    }
}
