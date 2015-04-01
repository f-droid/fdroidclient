package org.spongycastle.crypto.generators;

import java.math.BigInteger;

import org.spongycastle.crypto.DataLengthException;
import org.spongycastle.crypto.DerivationParameters;
import org.spongycastle.crypto.Mac;
import org.spongycastle.crypto.MacDerivationFunction;
import org.spongycastle.crypto.params.KDFCounterParameters;
import org.spongycastle.crypto.params.KeyParameter;

/**
 * This KDF has been defined by the publicly available NIST SP 800-108 specification.
 * NIST SP800-108 allows for alternative orderings of the input fields, meaning that the input can be formated in multiple ways.
 * There are 3 supported formats:  - Below [i]_2 is a counter of r-bits length concatenated to the fixedInputData.
 * <ul>
 * <li>1: K(i) := PRF( KI, [i]_2 || Label || 0x00 || Context || [L]_2 ) with the counter at the very beginning of the fixedInputData (The default implementation has this format)</li>
 * <li>2: K(i) := PRF( KI, Label || 0x00 || Context || [L]_2 || [i]_2 ) with the counter at the very end of the fixedInputData</li>
 * <li>3a: K(i) := PRF( KI, Label || 0x00 || [i]_2 || Context || [L]_2 ) OR:</li>
 * <li>3b: K(i) := PRF( KI, Label || 0x00 || [i]_2 || [L]_2 || Context ) OR:</li>
 * <li>3c: K(i) := PRF( KI, Label || [i]_2 || 0x00 || Context || [L]_2 ) etc... with the counter somewhere in the 'middle' of the fixedInputData.</li>
 * </ul>
 * </p>
 * <p>
 * This function must be called with the following KDFCounterParameters():
 *  - KI      <br/>
 *  - The part of the fixedInputData that comes BEFORE the counter OR null  <br/>
 *  - the part of the fixedInputData that comes AFTER the counter OR null <br/>
 *  - the length of the counter in bits (not bytes)
 *  </p>
 * Resulting function calls assuming an 8 bit counter.
 * <ul>
 * <li>1.  KDFCounterParameters(ki, 	null, 									"Label || 0x00 || Context || [L]_2]",	8);</li>
 * <li>2.  KDFCounterParameters(ki, 	"Label || 0x00 || Context || [L]_2]", 	null,									8);</li>
 * <li>3a. KDFCounterParameters(ki, 	"Label || 0x00",						"Context || [L]_2]",					8);</li>
 * <li>3b. KDFCounterParameters(ki, 	"Label || 0x00",						"[L]_2] || Context",					8);</li>
 * <li>3c. KDFCounterParameters(ki, 	"Label", 								"0x00 || Context || [L]_2]",			8);</li>
 * </ul>
 */
public class KDFCounterBytesGenerator
    implements MacDerivationFunction
{

    private static final BigInteger INTEGER_MAX = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final BigInteger TWO = BigInteger.valueOf(2);

    // please refer to the standard for the meaning of the variable names
    // all field lengths are in bytes, not in bits as specified by the standard

    // fields set by the constructor
    private final Mac prf;
    private final int h;

    // fields set by init
    private byte[] fixedInputDataCtrPrefix;
    private byte[] fixedInputData_afterCtr;
    private int maxSizeExcl;
    // ios is i defined as an octet string (the binary representation)
    private byte[] ios;

    // operational
    private int generatedBytes;
    // k is used as buffer for all K(i) values
    private byte[] k;


    public KDFCounterBytesGenerator(Mac prf)
    {
        this.prf = prf;
        this.h = prf.getMacSize();
        this.k = new byte[h];
    }


    public void init(DerivationParameters param)
    {
        if (!(param instanceof KDFCounterParameters))
        {
            throw new IllegalArgumentException("Wrong type of arguments given");
        }

        KDFCounterParameters kdfParams = (KDFCounterParameters)param;

        // --- init mac based PRF ---

        this.prf.init(new KeyParameter(kdfParams.getKI()));

        // --- set arguments ---

        this.fixedInputDataCtrPrefix = kdfParams.getFixedInputDataCounterPrefix();
        this.fixedInputData_afterCtr = kdfParams.getFixedInputDataCounterSuffix();

        int r = kdfParams.getR();
        this.ios = new byte[r / 8];

        BigInteger maxSize = TWO.pow(r).multiply(BigInteger.valueOf(h));
        this.maxSizeExcl = maxSize.compareTo(INTEGER_MAX) == 1 ?
            Integer.MAX_VALUE : maxSize.intValue();

        // --- set operational state ---

        generatedBytes = 0;
    }


    public Mac getMac()
    {
        return prf;
    }

    public int generateBytes(byte[] out, int outOff, int len)
        throws DataLengthException, IllegalArgumentException
    {

        int generatedBytesAfter = generatedBytes + len;
        if (generatedBytesAfter < 0 || generatedBytesAfter >= maxSizeExcl)
        {
            throw new DataLengthException(
                "Current KDFCTR may only be used for " + maxSizeExcl + " bytes");
        }

        if (generatedBytes % h == 0)
        {
            generateNext();
        }

        // copy what is left in the currentT (1..hash
        int toGenerate = len;
        int posInK = generatedBytes % h;
        int leftInK = h - generatedBytes % h;
        int toCopy = Math.min(leftInK, toGenerate);
        System.arraycopy(k, posInK, out, outOff, toCopy);
        generatedBytes += toCopy;
        toGenerate -= toCopy;
        outOff += toCopy;

        while (toGenerate > 0)
        {
            generateNext();
            toCopy = Math.min(h, toGenerate);
            System.arraycopy(k, 0, out, outOff, toCopy);
            generatedBytes += toCopy;
            toGenerate -= toCopy;
            outOff += toCopy;
        }

        return len;
    }

    private void generateNext()
    {
        int i = generatedBytes / h + 1;

        // encode i into counter buffer
        switch (ios.length)
        {
        case 4:
            ios[0] = (byte)(i >>> 24);
            // fall through
        case 3:
            ios[ios.length - 3] = (byte)(i >>> 16);
            // fall through
        case 2:
            ios[ios.length - 2] = (byte)(i >>> 8);
            // fall through
        case 1:
            ios[ios.length - 1] = (byte)i;
            break;
        default:
            throw new IllegalStateException("Unsupported size of counter i");
        }


        // special case for K(0): K(0) is empty, so no update
        prf.update(fixedInputDataCtrPrefix, 0, fixedInputDataCtrPrefix.length);
        prf.update(ios, 0, ios.length);
        prf.update(fixedInputData_afterCtr, 0, fixedInputData_afterCtr.length);
        prf.doFinal(k, 0);
    }
}
