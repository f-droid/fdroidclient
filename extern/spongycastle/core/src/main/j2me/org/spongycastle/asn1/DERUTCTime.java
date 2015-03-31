package org.spongycastle.asn1;

import java.io.IOException;
import java.util.Date;

import org.spongycastle.util.Arrays;
import org.spongycastle.util.Strings;

/**
 * UTC time object.
 */
public class DERUTCTime
    extends ASN1Primitive
{
    private byte[]      time;

    /**
     * return an UTC Time from the passed in object.
     *
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static ASN1UTCTime getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof ASN1UTCTime)
        {
            return (ASN1UTCTime)obj;
        }

        if (obj instanceof DERUTCTime)
        {
            return new ASN1UTCTime(((DERUTCTime)obj).time);
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    /**
     * return an UTC Time from a tagged object.
     *
     * @param obj the tagged object holding the object we want
     * @param explicit true if the object is meant to be explicitly
     *              tagged false otherwise.
     * @exception IllegalArgumentException if the tagged object cannot
     *               be converted.
     */
    public static ASN1UTCTime getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        ASN1Object o = obj.getObject();

        if (explicit || o instanceof ASN1UTCTime)
        {
            return getInstance(o);
        }
        else
        {
            return new ASN1UTCTime(((ASN1OctetString)o).getOctets());
        }
    }
    
    /**
     * The correct format for this is YYMMDDHHMMSSZ (it used to be that seconds were
     * never encoded. When you're creating one of these objects from scratch, that's
     * what you want to use, otherwise we'll try to deal with whatever gets read from
     * the input stream... (this is why the input format is different from the getTime()
     * method output).
     * <p>
     *
     * @param time the time string.
     */
    public DERUTCTime(
        String  time)
    {
        if (time.charAt(time.length() - 1) != 'Z')
        {
            // we accept this as a variation
            if (time.indexOf('-') < 0 && time.indexOf('+') < 0)
            {
                throw new IllegalArgumentException("time needs to be in format YYMMDDHHMMSSZ");
            }
        }

        this.time = Strings.toByteArray(time);
    }

    /**
     * base constructor from a java.util.date object
     */
    public DERUTCTime(
        Date time)
    {
        this.time = Strings.toByteArray(DateFormatter.toUTCDateString(time));
    }

    DERUTCTime(
        byte[]  time)
    {
        this.time = time;
    }

    /**
     * return the time as a date based on whatever a 2 digit year will return. For
     * standardised processing use getAdjustedDate().
     *
     * @return the resulting date
     */
    public Date getDate()
    {
        return DateFormatter.adjustedFromUTCDateString(time);
    }

    /**
     * return the time as an adjusted date
     * in the range of 1950 - 2049.
     *
     * @return a date in the range of 1950 to 2049.
     */
    public Date getAdjustedDate()
    {
         return DateFormatter.adjustedFromUTCDateString(time);
    }

    /**
     * return the time - always in the form of 
     *  YYMMDDhhmmssGMT(+hh:mm|-hh:mm).
     * <p>
     * Normally in a certificate we would expect "Z" rather than "GMT",
     * however adding the "GMT" means we can just use:
     * <pre>
     *     dateF = new SimpleDateFormat("yyMMddHHmmssz");
     * </pre>
     * To read in the time and get a date which is compatible with our local
     * time zone.
     * <p>
     * <b>Note:</b> In some cases, due to the local date processing, this
     * may lead to unexpected results. If you want to stick the normal
     * convention of 1950 to 2049 use the getAdjustedTime() method.
     */
    public String getTime()
    {
        String stime = Strings.fromByteArray(time);

        //
        // standardise the format.
        //
        if (stime.indexOf('-') < 0 && stime.indexOf('+') < 0)
        {
            if (stime.length() == 11)
            {
                return stime.substring(0, 10) + "00GMT+00:00";
            }
            else
            {
                return stime.substring(0, 12) + "GMT+00:00";
            }
        }
        else
        {
            int index = stime.indexOf('-');
            if (index < 0)
            {
                index = stime.indexOf('+');
            }
            String d = stime;

            if (index == stime.length() - 3)
            {
                d += "00";
            }

            if (index == 10)
            {
                return d.substring(0, 10) + "00GMT" + d.substring(10, 13) + ":" + d.substring(13, 15);
            }
            else
            {
                return d.substring(0, 12) + "GMT" + d.substring(12, 15) + ":" +  d.substring(15, 17);
            }
        }
    }

    /**
     * return a time string as an adjusted date with a 4 digit year. This goes
     * in the range of 1950 - 2049.
     */
    public String getAdjustedTime()
    {
        String   d = this.getTime();

        if (d.charAt(0) < '5')
        {
            return "20" + d;
        }
        else
        {
            return "19" + d;
        }
    }

    /**
     * Return the time.
     * @return The time string as it appeared in the encoded object.
     */
    public String getTimeString()
    {
        return Strings.fromByteArray(time);
    }

    boolean isConstructed()
    {
        return false;
    }

    int encodedLength()
    {
        int length = time.length;

        return 1 + StreamUtil.calculateBodyLength(length) + length;
    }

    void encode(
        ASN1OutputStream  out)
        throws IOException
    {
        out.write(BERTags.UTC_TIME);

        int length = time.length;

        out.writeLength(length);

        for (int i = 0; i != length; i++)
        {
            out.write((byte)time[i]);
        }
    }
    
    boolean asn1Equals(
        ASN1Primitive o)
    {
        if (!(o instanceof DERUTCTime))
        {
            return false;
        }

        return Arrays.areEqual(time, ((DERUTCTime)o).time);
    }
    
    public int hashCode()
    {
        return Arrays.hashCode(time);
    }

    public String toString() 
    {
      return Strings.fromByteArray(time);
    }
}
