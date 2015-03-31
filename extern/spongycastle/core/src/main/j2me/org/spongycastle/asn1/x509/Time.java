package org.spongycastle.asn1.x509;

import java.util.Calendar;
import java.util.Date;

import org.spongycastle.asn1.ASN1Choice;
import org.spongycastle.asn1.ASN1Object;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.ASN1TaggedObject;
import org.spongycastle.asn1.DERGeneralizedTime;
import org.spongycastle.asn1.DERUTCTime;

public class Time
    extends ASN1Object
    implements ASN1Choice
{
    ASN1Primitive time;

    public static Time getInstance(
        ASN1TaggedObject obj,
        boolean          explicit)
    {
        return getInstance(obj.getObject()); // must be explicitly tagged
    }

    public Time(
        ASN1Primitive   time)
    {
        if (!(time instanceof DERUTCTime)
            && !(time instanceof DERGeneralizedTime))
        {
            throw new IllegalArgumentException("unknown object passed to Time");
        }

        this.time = time; 
    }

    /**
     * creates a time object from a given date - if the date is between 1950
     * and 2049 a UTCTime object is generated, otherwise a GeneralizedTime
     * is used.
     */
    public Time(
        Date    date)
    {
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(date);

        int     year = calendar.get(Calendar.YEAR);

        if (year < 1950 || year > 2049)
        {
            time = new DERGeneralizedTime(date);
        }
        else
        {
            time = new DERUTCTime(date);
        }
    }

    public static Time getInstance(
        Object  obj)
    {
        if (obj == null || obj instanceof Time)
        {
            return (Time)obj;
        }
        else if (obj instanceof DERUTCTime)
        {
            return new Time((DERUTCTime)obj);
        }
        else if (obj instanceof DERGeneralizedTime)
        {
            return new Time((DERGeneralizedTime)obj);
        }

        throw new IllegalArgumentException("unknown object in factory: " + obj.getClass().getName());
    }

    public String getTime()
    {
        if (time instanceof DERUTCTime)
        {
            return ((DERUTCTime)time).getAdjustedTime();
        }
        else
        {
            return ((DERGeneralizedTime)time).getTime();
        }
    }

    public Date getDate()
    {
        if (time instanceof DERUTCTime)
        {
            return ((DERUTCTime)time).getAdjustedDate();
        }
        else
        {
            return ((DERGeneralizedTime)time).getDate();
        }
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     * Time ::= CHOICE {
     *             utcTime        UTCTime,
     *             generalTime    GeneralizedTime }
     * </pre>
     */
    public ASN1Primitive toASN1Primitive()
    {
        return time;
    }

    public String toString()
    {
        return getTime();
    }
}
