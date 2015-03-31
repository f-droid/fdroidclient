package org.spongycastle.asn1.eac;

import org.spongycastle.util.Arrays;

/**
 * EAC encoding date object
 */
public class PackedDate
{
    private byte[]      time;

    public PackedDate(
        String time)
    {
        this.time = convert(time);
    }

    private byte[] convert(String sTime)
    {
        char[] digs = sTime.toCharArray();
        byte[] date = new byte[6];

        for (int i = 0; i != 6; i++)
        {
            date[i] = (byte)(digs[i] - '0');
        }

        return date;
    }

    PackedDate(
        byte[] bytes)
    {
        this.time = bytes;
    }

    public int hashCode()
    {
        return Arrays.hashCode(time);
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof PackedDate))
        {
            return false;
        }

        PackedDate other = (PackedDate)o;

        return Arrays.areEqual(time, other.time);
    }

    public String toString() 
    {
        char[]  dateC = new char[time.length];

        for (int i = 0; i != dateC.length; i++)
        {
            dateC[i] = (char)((time[i] & 0xff) + '0');
        }

        return new String(dateC);
    }

    public byte[] getEncoding()
    {
        return time;
    }
}
