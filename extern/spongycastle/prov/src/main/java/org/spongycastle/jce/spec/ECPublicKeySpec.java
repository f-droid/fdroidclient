package org.spongycastle.jce.spec;

import org.spongycastle.math.ec.ECPoint;

/**
 * Elliptic Curve public key specification
 */
public class ECPublicKeySpec
    extends ECKeySpec
{
    private ECPoint    q;

    /**
     * base constructor
     *
     * @param q the public point on the curve.
     * @param spec the domain parameters for the curve.
     */
    public ECPublicKeySpec(
        ECPoint         q,
        ECParameterSpec spec)
    {
        super(spec);

        if (q.getCurve() != null)
        {
            this.q = q.normalize();
        }
        else
        {
            this.q = q;
        }
    }

    /**
     * return the public point q
     */
    public ECPoint getQ()
    {
        return q;
    }
}
