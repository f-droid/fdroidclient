package org.spongycastle.asn1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import org.spongycastle.util.Arrays;

/**
 * Use ASN1ObjectIdentifier instead of this,
 */
public class DERObjectIdentifier
    extends ASN1Primitive
{
    String identifier;

    private byte[] body;

    /**
     * return an OID from the passed in object
     *
     * @throws IllegalArgumentException if the object cannot be converted.
     */
    public static ASN1ObjectIdentifier getInstance(
        Object obj)
    {
        if (obj == null || obj instanceof ASN1ObjectIdentifier)
        {
            return (ASN1ObjectIdentifier)obj;
        }

        if (obj instanceof DERObjectIdentifier)
        {
            return new ASN1ObjectIdentifier(((DERObjectIdentifier)obj).getId());
        }

        if (obj instanceof ASN1Encodable && ((ASN1Encodable)obj).toASN1Primitive() instanceof ASN1ObjectIdentifier)
        {
            return (ASN1ObjectIdentifier)((ASN1Encodable)obj).toASN1Primitive();
        }

        if (obj instanceof byte[])
        {
            byte[] enc = (byte[])obj;
            if (enc[0] == BERTags.OBJECT_IDENTIFIER)
            {
                try
                {
                    return (ASN1ObjectIdentifier)fromByteArray(enc);
                }
                catch (IOException e)
                {
                    throw new IllegalArgumentException("failed to construct sequence from byte[]: " + e.getMessage());
                }
            }
            else
            {    // TODO: this really shouldn't be supported here...
                return ASN1ObjectIdentifier.fromOctetString((byte[])obj);
            }
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    /**
     * return an Object Identifier from a tagged object.
     *
     * @param obj      the tagged object holding the object we want
     * @param explicit true if the object is meant to be explicitly
     *                 tagged false otherwise.
     * @throws IllegalArgumentException if the tagged object cannot
     * be converted.
     */
    public static ASN1ObjectIdentifier getInstance(
        ASN1TaggedObject obj,
        boolean explicit)
    {
        ASN1Primitive o = obj.getObject();

        if (explicit || o instanceof DERObjectIdentifier)
        {
            return getInstance(o);
        }
        else
        {
            return ASN1ObjectIdentifier.fromOctetString(ASN1OctetString.getInstance(obj.getObject()).getOctets());
        }
    }

    private static final long LONG_LIMIT = (Long.MAX_VALUE >> 7) - 0x7f;

    DERObjectIdentifier(
        byte[] bytes)
    {
        StringBuffer objId = new StringBuffer();
        long value = 0;
        BigInteger bigValue = null;
        boolean first = true;

        for (int i = 0; i != bytes.length; i++)
        {
            int b = bytes[i] & 0xff;

            if (value <= LONG_LIMIT)
            {
                value += (b & 0x7f);
                if ((b & 0x80) == 0)             // end of number reached
                {
                    if (first)
                    {
                        if (value < 40)
                        {
                            objId.append('0');
                        }
                        else if (value < 80)
                        {
                            objId.append('1');
                            value -= 40;
                        }
                        else
                        {
                            objId.append('2');
                            value -= 80;
                        }
                        first = false;
                    }

                    objId.append('.');
                    objId.append(value);
                    value = 0;
                }
                else
                {
                    value <<= 7;
                }
            }
            else
            {
                if (bigValue == null)
                {
                    bigValue = BigInteger.valueOf(value);
                }
                bigValue = bigValue.or(BigInteger.valueOf(b & 0x7f));
                if ((b & 0x80) == 0)
                {
                    if (first)
                    {
                        objId.append('2');
                        bigValue = bigValue.subtract(BigInteger.valueOf(80));
                        first = false;
                    }

                    objId.append('.');
                    objId.append(bigValue);
                    bigValue = null;
                    value = 0;
                }
                else
                {
                    bigValue = bigValue.shiftLeft(7);
                }
            }
        }

        this.identifier = objId.toString();
        this.body = Arrays.clone(bytes);
    }

    /**
     * @deprecated use ASN1ObjectIdentifier constructor.
     */
    public DERObjectIdentifier(
        String identifier)
    {
        if (identifier == null)
        {
            throw new IllegalArgumentException("'identifier' cannot be null");
        }
        if (!isValidIdentifier(identifier))
        {
            throw new IllegalArgumentException("string " + identifier + " not an OID");
        }

        this.identifier = identifier;
    }

    DERObjectIdentifier(DERObjectIdentifier oid, String branchID)
    {
        if (!isValidBranchID(branchID, 0))
        {
            throw new IllegalArgumentException("string " + branchID + " not a valid OID branch");
        }

        this.identifier = oid.getId() + "." + branchID;
    }

    public String getId()
    {
        return identifier;
    }

    private void writeField(
        ByteArrayOutputStream out,
        long fieldValue)
    {
        byte[] result = new byte[9];
        int pos = 8;
        result[pos] = (byte)((int)fieldValue & 0x7f);
        while (fieldValue >= (1L << 7))
        {
            fieldValue >>= 7;
            result[--pos] = (byte)((int)fieldValue & 0x7f | 0x80);
        }
        out.write(result, pos, 9 - pos);
    }

    private void writeField(
        ByteArrayOutputStream out,
        BigInteger fieldValue)
    {
        int byteCount = (fieldValue.bitLength() + 6) / 7;
        if (byteCount == 0)
        {
            out.write(0);
        }
        else
        {
            BigInteger tmpValue = fieldValue;
            byte[] tmp = new byte[byteCount];
            for (int i = byteCount - 1; i >= 0; i--)
            {
                tmp[i] = (byte)((tmpValue.intValue() & 0x7f) | 0x80);
                tmpValue = tmpValue.shiftRight(7);
            }
            tmp[byteCount - 1] &= 0x7f;
            out.write(tmp, 0, tmp.length);
        }
    }

    private void doOutput(ByteArrayOutputStream aOut)
    {
        OIDTokenizer tok = new OIDTokenizer(identifier);
        int first = Integer.parseInt(tok.nextToken()) * 40;

        String secondToken = tok.nextToken();
        if (secondToken.length() <= 18)
        {
            writeField(aOut, first + Long.parseLong(secondToken));
        }
        else
        {
            writeField(aOut, new BigInteger(secondToken).add(BigInteger.valueOf(first)));
        }

        while (tok.hasMoreTokens())
        {
            String token = tok.nextToken();
            if (token.length() <= 18)
            {
                writeField(aOut, Long.parseLong(token));
            }
            else
            {
                writeField(aOut, new BigInteger(token));
            }
        }
    }

    protected synchronized byte[] getBody()
    {
        if (body == null)
        {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();

            doOutput(bOut);

            body = bOut.toByteArray();
        }

        return body;
    }

    boolean isConstructed()
    {
        return false;
    }

    int encodedLength()
        throws IOException
    {
        int length = getBody().length;

        return 1 + StreamUtil.calculateBodyLength(length) + length;
    }

    void encode(
        ASN1OutputStream out)
        throws IOException
    {
        byte[] enc = getBody();

        out.write(BERTags.OBJECT_IDENTIFIER);
        out.writeLength(enc.length);
        out.write(enc);
    }

    public int hashCode()
    {
        return identifier.hashCode();
    }

    boolean asn1Equals(
        ASN1Primitive o)
    {
        if (!(o instanceof DERObjectIdentifier))
        {
            return false;
        }

        return identifier.equals(((DERObjectIdentifier)o).identifier);
    }

    public String toString()
    {
        return getId();
    }

    private static boolean isValidBranchID(
        String branchID, int start)
    {
        boolean periodAllowed = false;

        int pos = branchID.length();
        while (--pos >= start)
        {
            char ch = branchID.charAt(pos);

            // TODO Leading zeroes?
            if ('0' <= ch && ch <= '9')
            {
                periodAllowed = true;
                continue;
            }

            if (ch == '.')
            {
                if (!periodAllowed)
                {
                    return false;
                }

                periodAllowed = false;
                continue;
            }

            return false;
        }

        return periodAllowed;
    }

    private static boolean isValidIdentifier(
        String identifier)
    {
        if (identifier.length() < 3 || identifier.charAt(1) != '.')
        {
            return false;
        }

        char first = identifier.charAt(0);
        if (first < '0' || first > '2')
        {
            return false;
        }

        return isValidBranchID(identifier, 2);
    }

    private static ASN1ObjectIdentifier[][] cache = new ASN1ObjectIdentifier[256][];

    static ASN1ObjectIdentifier fromOctetString(byte[] enc)
    {
        if (enc.length < 3)
        {
            return new ASN1ObjectIdentifier(enc);
        }

        int idx1 = enc[enc.length - 2] & 0xff;
        // in this case top bit is always zero
        int idx2 = enc[enc.length - 1] & 0x7f;

        ASN1ObjectIdentifier possibleMatch;

        synchronized (cache)
        {
            ASN1ObjectIdentifier[] first = cache[idx1];
            if (first == null)
            {
                first = cache[idx1] = new ASN1ObjectIdentifier[128];
            }

            possibleMatch = first[idx2];
            if (possibleMatch == null)
            {
                return first[idx2] = new ASN1ObjectIdentifier(enc);
            }

            if (Arrays.areEqual(enc, possibleMatch.getBody()))
            {
                return possibleMatch;
            }

            idx1 = (idx1 + 1) & 0xff;
            first = cache[idx1];
            if (first == null)
            {
                first = cache[idx1] = new ASN1ObjectIdentifier[128];
            }

            possibleMatch = first[idx2];
            if (possibleMatch == null)
            {
                return first[idx2] = new ASN1ObjectIdentifier(enc);
            }

            if (Arrays.areEqual(enc, possibleMatch.getBody()))
            {
                return possibleMatch;
            }

            idx2 = (idx2 + 1) & 0x7f;
            possibleMatch = first[idx2];
            if (possibleMatch == null)
            {
                return first[idx2] = new ASN1ObjectIdentifier(enc);
            }
        }

        if (Arrays.areEqual(enc, possibleMatch.getBody()))
        {
            return possibleMatch;
        }

        return new ASN1ObjectIdentifier(enc);
    }
}
