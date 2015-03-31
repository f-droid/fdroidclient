
package java.security.spec;

import java.math.BigInteger;

public class DSAPrivateKeySpec implements KeySpec
{
    private BigInteger x;
    private BigInteger p;
    private BigInteger q;
    private BigInteger g;

    public DSAPrivateKeySpec(BigInteger x, BigInteger p, BigInteger q, BigInteger g)
    {
        this.x = x;
        this.p = p;
        this.q = q;
        this.g = g;
    }

    public BigInteger getG()
    {
        return g;
    }

    public BigInteger getP()
    {
        return p;
    }

    public BigInteger getQ()
    {
        return q;
    }

    public BigInteger getX()
    {
        return x;
    }
}
