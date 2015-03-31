
package java.security.interfaces;

import java.math.BigInteger;
import java.security.PublicKey;

public interface RSAPublicKey extends PublicKey 
{
    public static final long serialVersionUID = 7187392471159151072L;

    public abstract BigInteger getModulus();
    public abstract BigInteger getPublicExponent();
}
