package org.spongycastle.crypto.engines;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.DataLengthException;
import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.params.CramerShoupKeyParameters;
import org.spongycastle.crypto.params.CramerShoupPrivateKeyParameters;
import org.spongycastle.crypto.params.CramerShoupPublicKeyParameters;
import org.spongycastle.crypto.params.ParametersWithRandom;
import org.spongycastle.util.BigIntegers;

/**
 * Essentially the Cramer-Shoup encryption / decryption algorithms according to
 * "A practical public key cryptosystem provably secure against adaptive chosen ciphertext attack." (Crypto 1998)
 */
public class CramerShoupCoreEngine
{

    private static final BigInteger ONE = BigInteger.valueOf(1);

    private CramerShoupKeyParameters key;
    private SecureRandom random;
    private boolean forEncryption;
    private String label = null;

    /**
     * initialise the CramerShoup engine.
     *
     * @param forEncryption whether this engine should encrypt or decrypt
     * @param param         the necessary CramerShoup key parameters.
     * @param label         the label for labelled CS as {@link String}
     */
    public void init(boolean forEncryption, CipherParameters param, String label)
    {
        init(forEncryption, param);

        this.label = label;
    }

    /**
     * initialise the CramerShoup engine.
     *
     * @param forEncryption whether this engine should encrypt or decrypt
     * @param param         the necessary CramerShoup key parameters.
     */
    public void init(boolean forEncryption, CipherParameters param)
    {
        SecureRandom providedRandom = null;

        if (param instanceof ParametersWithRandom)
        {
            ParametersWithRandom rParam = (ParametersWithRandom)param;

            key = (CramerShoupKeyParameters)rParam.getParameters();
            providedRandom = rParam.getRandom();
        }
        else
        {
            key = (CramerShoupKeyParameters)param;
        }

        this.random = initSecureRandom(forEncryption, providedRandom);
        this.forEncryption = forEncryption;
    }

    /**
     * Return the maximum size for an input block to this engine. For Cramer
     * Shoup this is always one byte less than the key size on encryption, and
     * the same length as the key size on decryption.
     *
     * @return maximum size for an input block.
     * <p/>
     * TODO: correct?
     */
    public int getInputBlockSize()
    {
        int bitSize = key.getParameters().getP().bitLength();

        if (forEncryption)
        {
            return (bitSize + 7) / 8 - 1;
        }
        else
        {
            return (bitSize + 7) / 8;
        }
    }

    /**
     * Return the maximum size for an output block to this engine. For Cramer
     * Shoup this is always one byte less than the key size on decryption, and
     * the same length as the key size on encryption.
     *
     * @return maximum size for an output block.
     * <p/>
     * TODO: correct?
     */
    public int getOutputBlockSize()
    {
        int bitSize = key.getParameters().getP().bitLength();

        if (forEncryption)
        {
            return (bitSize + 7) / 8;
        }
        else
        {
            return (bitSize + 7) / 8 - 1;
        }
    }

    public BigInteger convertInput(byte[] in, int inOff, int inLen)
    {
        if (inLen > (getInputBlockSize() + 1))
        {
            throw new DataLengthException("input too large for Cramer Shoup cipher.");
        }
        else if (inLen == (getInputBlockSize() + 1) && forEncryption)
        {
            throw new DataLengthException("input too large for Cramer Shoup cipher.");
        }

        byte[] block;

        if (inOff != 0 || inLen != in.length)
        {
            block = new byte[inLen];

            System.arraycopy(in, inOff, block, 0, inLen);
        }
        else
        {
            block = in;
        }

        BigInteger res = new BigInteger(1, block);
        if (res.compareTo(key.getParameters().getP()) >= 0)
        {
            throw new DataLengthException("input too large for Cramer Shoup cipher.");
        }

        return res;
    }

    public byte[] convertOutput(BigInteger result)
    {
        byte[] output = result.toByteArray();

        if (!forEncryption)
        {
            if (output[0] == 0 && output.length > getOutputBlockSize())
            { // have ended up with an extra zero byte, copy down.
                byte[] tmp = new byte[output.length - 1];

                System.arraycopy(output, 1, tmp, 0, tmp.length);

                return tmp;
            }

            if (output.length < getOutputBlockSize())
            {// have ended up with less bytes than normal, lengthen
                byte[] tmp = new byte[getOutputBlockSize()];

                System.arraycopy(output, 0, tmp, tmp.length - output.length, output.length);

                return tmp;
            }
        }
        else
        {
            if (output[0] == 0)
            { // have ended up with an extra zero byte, copy down.
                byte[] tmp = new byte[output.length - 1];

                System.arraycopy(output, 1, tmp, 0, tmp.length);

                return tmp;
            }
        }

        return output;
    }

    public CramerShoupCiphertext encryptBlock(BigInteger input)
    {

        CramerShoupCiphertext result = null;

        if (!key.isPrivate() && this.forEncryption && key instanceof CramerShoupPublicKeyParameters)
        {
            CramerShoupPublicKeyParameters pk = (CramerShoupPublicKeyParameters)key;
            BigInteger p = pk.getParameters().getP();
            BigInteger g1 = pk.getParameters().getG1();
            BigInteger g2 = pk.getParameters().getG2();

            BigInteger h = pk.getH();

            if (!isValidMessage(input, p))
            {
                return result;
            }

            BigInteger r = generateRandomElement(p, random);

            BigInteger u1, u2, v, e, a;

            u1 = g1.modPow(r, p);
            u2 = g2.modPow(r, p);
            e = h.modPow(r, p).multiply(input).mod(p);

            Digest digest = pk.getParameters().getH();
            byte[] u1Bytes = u1.toByteArray();
            digest.update(u1Bytes, 0, u1Bytes.length);
            byte[] u2Bytes = u2.toByteArray();
            digest.update(u2Bytes, 0, u2Bytes.length);
            byte[] eBytes = e.toByteArray();
            digest.update(eBytes, 0, eBytes.length);
            if (this.label != null)
            {
                byte[] lBytes = this.label.getBytes();
                digest.update(lBytes, 0, lBytes.length);
            }
            byte[] out = new byte[digest.getDigestSize()];
            digest.doFinal(out, 0);
            a = new BigInteger(1, out);

            v = pk.getC().modPow(r, p).multiply(pk.getD().modPow(r.multiply(a), p)).mod(p);

            result = new CramerShoupCiphertext(u1, u2, e, v);
        }
        return result;
    }

    public BigInteger decryptBlock(CramerShoupCiphertext input)
        throws CramerShoupCiphertextException
    {

        BigInteger result = null;

        if (key.isPrivate() && !this.forEncryption && key instanceof CramerShoupPrivateKeyParameters)
        {
            CramerShoupPrivateKeyParameters sk = (CramerShoupPrivateKeyParameters)key;

            BigInteger p = sk.getParameters().getP();

            Digest digest = sk.getParameters().getH();
            byte[] u1Bytes = input.getU1().toByteArray();
            digest.update(u1Bytes, 0, u1Bytes.length);
            byte[] u2Bytes = input.getU2().toByteArray();
            digest.update(u2Bytes, 0, u2Bytes.length);
            byte[] eBytes = input.getE().toByteArray();
            digest.update(eBytes, 0, eBytes.length);
            if (this.label != null)
            {
                byte[] lBytes = this.label.getBytes();
                digest.update(lBytes, 0, lBytes.length);
            }
            byte[] out = new byte[digest.getDigestSize()];
            digest.doFinal(out, 0);

            BigInteger a = new BigInteger(1, out);
            BigInteger v = input.u1.modPow(sk.getX1().add(sk.getY1().multiply(a)), p).
                multiply(input.u2.modPow(sk.getX2().add(sk.getY2().multiply(a)), p)).mod(p);

            // check correctness of ciphertext
            if (input.v.equals(v))
            {
                result = input.e.multiply(input.u1.modPow(sk.getZ(), p).modInverse(p)).mod(p);
            }
            else
            {
                throw new CramerShoupCiphertextException("Sorry, that ciphertext is not correct");
            }
        }
        return result;
    }

    private BigInteger generateRandomElement(BigInteger p, SecureRandom random)
    {
        return BigIntegers.createRandomInRange(ONE, p.subtract(ONE), random);
    }

    /**
     * just checking whether the message m is actually less than the group order p
     */
    private boolean isValidMessage(BigInteger m, BigInteger p)
    {
        return m.compareTo(p) < 0;
    }

    protected SecureRandom initSecureRandom(boolean needed, SecureRandom provided)
    {
        return !needed ? null : (provided != null) ? provided : new SecureRandom();
    }

    /**
     * CS exception for wrong cipher-texts
     */
    public static class CramerShoupCiphertextException
        extends Exception
    {
        private static final long serialVersionUID = -6360977166495345076L;

        public CramerShoupCiphertextException(String msg)
        {
            super(msg);
        }

    }
}
