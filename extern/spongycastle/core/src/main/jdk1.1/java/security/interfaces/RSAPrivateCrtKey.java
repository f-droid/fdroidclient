
package java.security.interfaces;

import java.math.BigInteger;

public interface RSAPrivateCrtKey extends RSAPrivateKey 
{
    public static final long serialVersionUID = 6034044314589513430L;

    public abstract BigInteger getCrtCoefficient();
    public abstract BigInteger getPrimeExponentP();
    public abstract BigInteger getPrimeExponentQ();
    public abstract BigInteger getPrimeP();
    public abstract BigInteger getPrimeQ();
    public abstract BigInteger getPublicExponent();
}
