package org.spongycastle.crypto.signers;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.DSA;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.crypto.params.ECKeyParameters;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.crypto.params.ParametersWithRandom;
import org.spongycastle.math.ec.ECAlgorithms;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECFieldElement;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.Arrays;

/**
 * DSTU 4145-2002
 * <p>
 * National Ukrainian standard of digital signature based on elliptic curves (DSTU 4145-2002).
 * </p>
 */
public class DSTU4145Signer
    implements DSA
{
    private static final BigInteger ONE = BigInteger.valueOf(1);

    private ECKeyParameters key;
    private SecureRandom random;

    public void init(boolean forSigning, CipherParameters param)
    {
        if (forSigning)
        {
            if (param instanceof ParametersWithRandom)
            {
                ParametersWithRandom rParam = (ParametersWithRandom)param;

                this.random = rParam.getRandom();
                param = rParam.getParameters();
            }
            else
            {
                this.random = new SecureRandom();
            }

            this.key = (ECPrivateKeyParameters)param;
        }
        else
        {
            this.key = (ECPublicKeyParameters)param;
        }

    }

    public BigInteger[] generateSignature(byte[] message)
    {
        ECDomainParameters parameters = key.getParameters();

        ECCurve curve = parameters.getCurve();

        ECFieldElement h = hash2FieldElement(curve, message);
        if (h.isZero())
        {
            h = curve.fromBigInteger(ONE);
        }

        BigInteger n = parameters.getN();
        BigInteger e, r, s;
        ECFieldElement Fe, y;

        do
        {
            do
            {
                do
                {
                    e = generateRandomInteger(n, random);
                    Fe = parameters.getG().multiply(e).normalize().getAffineXCoord();
                }
                while (Fe.isZero());

                y = h.multiply(Fe);
                r = fieldElement2Integer(n, y);
            }
            while (r.signum() == 0);

            s = r.multiply(((ECPrivateKeyParameters)key).getD()).add(e).mod(n);
        }
        while (s.signum() == 0);

        return new BigInteger[]{r, s};
    }

    public boolean verifySignature(byte[] message, BigInteger r, BigInteger s)
    {
        if (r.signum() <= 0 || s.signum() <= 0)
        {
            return false;
        }

        ECDomainParameters parameters = key.getParameters();

        BigInteger n = parameters.getN();
        if (r.compareTo(n) >= 0 || s.compareTo(n) >= 0)
        {
            return false;
        }

        ECCurve curve = parameters.getCurve();

        ECFieldElement h = hash2FieldElement(curve, message);
        if (h.isZero())
        {
            h = curve.fromBigInteger(ONE);
        }

        ECPoint R = ECAlgorithms.sumOfTwoMultiplies(parameters.getG(), s, ((ECPublicKeyParameters)key).getQ(), r).normalize();

        // components must be bogus.
        if (R.isInfinity())
        {
            return false;
        }

        ECFieldElement y = h.multiply(R.getAffineXCoord());
        return fieldElement2Integer(n, y).compareTo(r) == 0;
    }

    /**
     * Generates random integer such, than its bit length is less than that of n
     */
    private static BigInteger generateRandomInteger(BigInteger n, SecureRandom random)
    {
        return new BigInteger(n.bitLength() - 1, random);
    }
    
    private static void reverseBytes(byte[] bytes)
    {
        byte tmp;
        
        for (int i=0; i<bytes.length/2; i++)
        {
            tmp=bytes[i];
            bytes[i]=bytes[bytes.length-1-i];
            bytes[bytes.length-1-i]=tmp;
        }
    }

    private static ECFieldElement hash2FieldElement(ECCurve curve, byte[] hash)
    {
        byte[] data = Arrays.clone(hash);
        reverseBytes(data);
        BigInteger num = new BigInteger(1, data);
        while (num.bitLength() > curve.getFieldSize())
        {
            num = num.clearBit(num.bitLength() - 1);
        }

        return curve.fromBigInteger(num);
    }

    private static BigInteger fieldElement2Integer(BigInteger n, ECFieldElement fieldElement)
    {
        BigInteger num = fieldElement.toBigInteger();
        while (num.bitLength() >= n.bitLength())
        {
            num = num.clearBit(num.bitLength() - 1);
        }

        return num;
    }
}
