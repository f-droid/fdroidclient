package org.spongycastle.math.ec;

import java.math.BigInteger;

public class ECAlgorithms
{
    public static ECPoint sumOfTwoMultiplies(ECPoint P, BigInteger a,
        ECPoint Q, BigInteger b)
    {
        ECCurve cp = P.getCurve();
        Q = importPoint(cp, Q);

        // Point multiplication for Koblitz curves (using WTNAF) beats Shamir's trick
        if (cp instanceof ECCurve.F2m)
        {
            ECCurve.F2m f2mCurve = (ECCurve.F2m)cp;
            if (f2mCurve.isKoblitz())
            {
                return P.multiply(a).add(Q.multiply(b));
            }
        }

        return implShamirsTrick(P, a, Q, b);
    }

    /*
     * "Shamir's Trick", originally due to E. G. Straus
     * (Addition chains of vectors. American Mathematical Monthly,
     * 71(7):806-808, Aug./Sept. 1964)
     * <pre>
     * Input: The points P, Q, scalar k = (km?, ... , k1, k0)
     * and scalar l = (lm?, ... , l1, l0).
     * Output: R = k * P + l * Q.
     * 1: Z <- P + Q
     * 2: R <- O
     * 3: for i from m-1 down to 0 do
     * 4:        R <- R + R        {point doubling}
     * 5:        if (ki = 1) and (li = 0) then R <- R + P end if
     * 6:        if (ki = 0) and (li = 1) then R <- R + Q end if
     * 7:        if (ki = 1) and (li = 1) then R <- R + Z end if
     * 8: end for
     * 9: return R
     * </pre>
     */
    public static ECPoint shamirsTrick(ECPoint P, BigInteger k,
        ECPoint Q, BigInteger l)
    {
        ECCurve cp = P.getCurve();
        Q = importPoint(cp, Q);

        return implShamirsTrick(P, k, Q, l);
    }

    public static ECPoint importPoint(ECCurve c, ECPoint p)
    {
        ECCurve cp = p.getCurve();
        if (!c.equals(cp))
        {
            throw new IllegalArgumentException("Point must be on the same curve");
        }
        return c.importPoint(p);
    }

    static void implMontgomeryTrick(ECFieldElement[] zs, int off, int len)
    {
        /*
         * Uses the "Montgomery Trick" to invert many field elements, with only a single actual
         * field inversion. See e.g. the paper:
         * "Fast Multi-scalar Multiplication Methods on Elliptic Curves with Precomputation Strategy Using Montgomery Trick"
         * by Katsuyuki Okeya, Kouichi Sakurai.
         */

        ECFieldElement[] c = new ECFieldElement[len];
        c[0] = zs[off];

        int i = 0;
        while (++i < len)
        {
            c[i] = c[i - 1].multiply(zs[off + i]);
        }

        ECFieldElement u = c[--i].invert();

        while (i > 0)
        {
            int j = off + i--;
            ECFieldElement tmp = zs[j];
            zs[j] = c[i].multiply(u);
            u = u.multiply(tmp);
        }

        zs[off] = u;
    }

    static ECPoint implShamirsTrick(ECPoint P, BigInteger k,
        ECPoint Q, BigInteger l)
    {
        ECCurve curve = P.getCurve();
        ECPoint infinity = curve.getInfinity();

        // TODO conjugate co-Z addition (ZADDC) can return both of these
        ECPoint PaddQ = P.add(Q);
        ECPoint PsubQ = P.subtract(Q);

        ECPoint[] points = new ECPoint[]{ Q, PsubQ, P, PaddQ };
        curve.normalizeAll(points);

        ECPoint[] table = new ECPoint[] {
            points[3].negate(), points[2].negate(), points[1].negate(),
            points[0].negate(), infinity, points[0],
            points[1], points[2], points[3] };

        byte[] jsf = WNafUtil.generateJSF(k, l);

        ECPoint R = infinity;

        int i = jsf.length;
        while (--i >= 0)
        {
            int jsfi = jsf[i];
            int kDigit = (jsfi >> 4), lDigit = ((jsfi << 28) >> 28);

            int index = 4 + (kDigit * 3) + lDigit;
            R = R.twicePlus(table[index]);
        }

        return R;
    }
}
