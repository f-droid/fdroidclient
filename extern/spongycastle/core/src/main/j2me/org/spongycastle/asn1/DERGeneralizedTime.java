package org.spongycastle.asn1;

import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;

import org.spongycastle.util.Arrays;
import org.spongycastle.util.Strings;

/**
 * Generalized time object.
 */
public class DERGeneralizedTime
    extends ASN1Primitive
{
    private byte[]      time;

    /**
     * return a generalized time from the passed in object
     *
     * @exception IllegalArgumentException if the object cannot be converted.
     */
    public static ASN1GeneralizedTime getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof ASN1GeneralizedTime)
        {
            return (ASN1GeneralizedTime)obj;
        }

        if (obj instanceof DERGeneralizedTime)
        {
            return new ASN1GeneralizedTime(((DERGeneralizedTime)obj).time);
        }

        throw new IllegalArgumentException("illegal object in getInstance: " + obj.getClass().getName());
    }

    /**
     * return a Generalized Time object from a tagged object.
     *
     * @param obj the tagged object holding the object we want
     * @param explicit true if the object is meant to be explicitly
     *              tagged false otherwise.
     * @exception IllegalArgumentException if the tagged object cannot
     *               be converted.
     */
    public static ASN1GeneralizedTime getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        ASN1Primitive o = obj.getObject();

        if (explicit || o instanceof DERGeneralizedTime)
        {
            return getInstance(o);
        }
        else
        {
            return new ASN1GeneralizedTime(((ASN1OctetString)o).getOctets());
        }
    }
    
    /**
     * The correct format for this is YYYYMMDDHHMMSS[.f]Z, or without the Z
     * for local time, or Z|[+|-]HHMM on the end, for difference between local
     * time and UTC time. The fractional second amount f must consist of at
     * least one number with trailing zeroes removed.
     *
     * @param time the time string.
     * @exception IllegalArgumentException if String is an illegal format.
     */
    public DERGeneralizedTime(
        String  time)
    {
        char last = time.charAt(time.length() - 1);
        if (last != 'Z' && !(last >= 0 && last <= '9'))
        {
            if (time.indexOf('-') < 0 && time.indexOf('+') < 0)
            {
                throw new IllegalArgumentException("time needs to be in format YYYYMMDDHHMMSS[.f]Z or YYYYMMDDHHMMSS[.f][+-]HHMM");
            }
        }

        this.time = Strings.toByteArray(time);
    }

    /**
     * base constructer from a java.util.date object
     */
    public DERGeneralizedTime(
        Date time)
    {
        this.time = Strings.toByteArray(DateFormatter.getGeneralizedTimeDateString(time, false));
    }

    protected DERGeneralizedTime(Date date, boolean includeMillis)
    {
        this.time = Strings.toByteArray(DateFormatter.getGeneralizedTimeDateString(date, true));
    }

    DERGeneralizedTime(
        byte[]  bytes)
    {
        this.time = bytes;
    }

    /**
     * Return the time.
     * @return The time string as it appeared in the encoded object.
     */
    public String getTimeString()
    {
        return Strings.fromByteArray(time);
    }
    
    /**
     * return the time - always in the form of 
     *  YYYYMMDDhhmmssGMT(+hh:mm|-hh:mm).
     * <p>
     * Normally in a certificate we would expect "Z" rather than "GMT",
     * however adding the "GMT" means we can just use:
     * <pre>
     *     dateF = new SimpleDateFormat("yyyyMMddHHmmssz");
     * </pre>
     * To read in the time and get a date which is compatible with our local
     * time zone.
     */
    public String getTime()
    {
        String stime = Strings.fromByteArray(time);

        //
        // standardise the format.
        //             
        if (stime.charAt(stime.length() - 1) == 'Z')
        {
            return stime.substring(0, stime.length() - 1) + "GMT+00:00";
        }
        else
        {
            int signPos = stime.length() - 5;
            char sign = stime.charAt(signPos);
            if (sign == '-' || sign == '+')
            {
                return stime.substring(0, signPos)
                    + "GMT"
                    + stime.substring(signPos, signPos + 3)
                    + ":"
                    + stime.substring(signPos + 3);
            }
            else
            {
                signPos = stime.length() - 3;
                sign = stime.charAt(signPos);
                if (sign == '-' || sign == '+')
                {
                    return stime.substring(0, signPos)
                        + "GMT"
                        + stime.substring(signPos)
                        + ":00";
                }
            }
        }            
        return stime + calculateGMTOffset();
    }

    private String calculateGMTOffset()
    {
        String sign = "+";
        TimeZone timeZone = TimeZone.getDefault();
        int offset = timeZone.getRawOffset();
        if (offset < 0)
        {
            sign = "-";
            offset = -offset;
        }
        int hours = offset / (60 * 60 * 1000);
        int minutes = (offset - (hours * 60 * 60 * 1000)) / (60 * 1000);

//        try
//        {
//            if (timeZone.useDaylightTime() && timeZone.inDaylightTime(this.getDate()))
//            {
//                hours += sign.equals("+") ? 1 : -1;
//            }
//        }
//        catch (ParseException e)
//        {
//            // we'll do our best and ignore daylight savings
//        }

        return "GMT" + sign + convert(hours) + ":" + convert(minutes);
    }

    private String convert(int time)
    {
        if (time < 10)
        {
            return "0" + time;
        }

        return Integer.toString(time);
    }

    public Date getDate()
    {
        return DateFormatter.fromGeneralizedTimeString(time);
    }

    private boolean hasFractionalSeconds()
    {
        for (int i = 0; i != time.length; i++)
        {
            if (time[i] == '.')
            {
                if (i == 14)
                {
                    return true;
                }
            }
        }
        return false;
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
        out.writeEncoded(BERTags.GENERALIZED_TIME, time);
    }
    
    boolean asn1Equals(
        ASN1Primitive  o)
    {
        if (!(o instanceof DERGeneralizedTime))
        {
            return false;
        }

        return Arrays.areEqual(time, ((DERGeneralizedTime)o).time);
    }
    
    public int hashCode()
    {
        return Arrays.hashCode(time);
    }
}
