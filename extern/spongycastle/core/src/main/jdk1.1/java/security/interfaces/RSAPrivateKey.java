
package java.security.interfaces;

import java.math.BigInteger;
import java.security.PrivateKey;

public interface RSAPrivateKey extends PrivateKey 
{
    public static final long serialVersionUID = 6034044314589513430L;

    public abstract BigInteger getModulus();
    public abstract BigInteger getPrivateExponent();
}
