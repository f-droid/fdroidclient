
package java.security.spec;

import java.math.BigInteger;
import java.security.interfaces.DSAParams;

public class DSAParameterSpec implements AlgorithmParameterSpec, DSAParams
{
    private BigInteger p;
    private BigInteger q;
    private BigInteger g;

    public DSAParameterSpec(BigInteger p, BigInteger q, BigInteger g)
    {
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
}
