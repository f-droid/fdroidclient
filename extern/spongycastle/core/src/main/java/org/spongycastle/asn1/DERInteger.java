package org.spongycastle.asn1;

import java.io.IOException;
import java.math.BigInteger;

import org.spongycastle.util.Arrays;

/**
 * Use ASN1Integer instead of this,
 */
public class DERInteger
    extends ASN1Primitive
{
    byte[]      bytes;

    /**
     * return an integer from the passed in object
     *
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static ASN1Integer getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof ASN1Integer)
        {
            return (ASN1Integer)obj;
        }
        if (obj instanceof DERInteger)
        {
            return new ASN1Integer((((DERInteger)obj).getValue()));
        }

        if (obj instanceof byte[])
        {
            try
            {
                return (ASN1Integer)fromByteArray((byte[])obj);
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException("encoding error in getInstance: " + e.toString());
            }
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    /**
     * return an Integer from a tagged object.
     *
     * @param obj the tagged object holding the object we want
     * @param explicit true if the object is meant to be explicitly
     *              tagged false otherwise.
     * @exception IllegalArgumentException if the tagged object cannot
     *               be converted.
     */
    public static ASN1Integer getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        ASN1Primitive o = obj.getObject();

        if (explicit || o instanceof DERInteger)
        {
            return getInstance(o);
        }
        else
        {
            return new ASN1Integer(ASN1OctetString.getInstance(obj.getObject()).getOctets());
        }
    }

    /**
     * @deprecated use ASN1Integer constructor
     */
    public DERInteger(
        long         value)
    {
        bytes = BigInteger.valueOf(value).toByteArray();
    }

    /**
     * @deprecated use ASN1Integer constructor
     */
    public DERInteger(
        BigInteger   value)
    {
        bytes = value.toByteArray();
    }

    /**
     * @deprecated use ASN1Integer constructor
     */
    public DERInteger(
        byte[]   bytes)
    {
        this.bytes = bytes;
    }

    public BigInteger getValue()
    {
        return new BigInteger(bytes);
    }

    /**
     * in some cases positive values get crammed into a space,
     * that's not quite big enough...
     */
    public BigInteger getPositiveValue()
    {
        return new BigInteger(1, bytes);
    }

    boolean isConstructed()
    {
        return false;
    }

    int encodedLength()
    {
        return 1 + StreamUtil.calculateBodyLength(bytes.length) + bytes.length;
    }

    void encode(
        ASN1OutputStream out)
        throws IOException
    {
        out.writeEncoded(BERTags.INTEGER, bytes);
    }
    
    public int hashCode()
    {
         int     value = 0;
 
         for (int i = 0; i != bytes.length; i++)
         {
             value ^= (bytes[i] & 0xff) << (i % 4);
         }
 
         return value;
    }

    boolean asn1Equals(
        ASN1Primitive  o)
    {
        if (!(o instanceof DERInteger))
        {
            return false;
        }

        DERInteger other = (DERInteger)o;

        return Arrays.areEqual(bytes, other.bytes);
    }

    public String toString()
    {
      return getValue().toString();
    }
}
