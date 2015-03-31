package org.spongycastle.math.ec;

/**
 * Class holding precomputation data for the WNAF (Window Non-Adjacent Form)
 * algorithm.
 */
public class WNafPreCompInfo implements PreCompInfo
{
    /**
     * Array holding the precomputed <code>ECPoint</code>s used for a Window
     * NAF multiplication.
     */
    private ECPoint[] preComp = null;

    /**
     * Array holding the negations of the precomputed <code>ECPoint</code>s used
     * for a Window NAF multiplication.
     */
    private ECPoint[] preCompNeg = null;

    /**
     * Holds an <code>ECPoint</code> representing twice(this). Used for the
     * Window NAF multiplication to create or extend the precomputed values.
     */
    private ECPoint twiceP = null;

    protected ECPoint[] getPreComp()
    {
        return preComp;
    }

    protected ECPoint[] getPreCompNeg()
    {
        return preCompNeg;
    }

    protected void setPreComp(ECPoint[] preComp)
    {
        this.preComp = preComp;
    }

    protected void setPreCompNeg(ECPoint[] preCompNeg)
    {
        this.preCompNeg = preCompNeg;
    }

    protected ECPoint getTwiceP()
    {
        return twiceP;
    }

    protected void setTwiceP(ECPoint twiceP)
    {
        this.twiceP = twiceP;
    }
}
