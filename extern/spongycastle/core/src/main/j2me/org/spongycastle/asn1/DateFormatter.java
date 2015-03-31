package org.spongycastle.asn1;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

class DateFormatter
{
    // YYMMDDHHMMSSZ
    static String toUTCDateString(Date date)
    {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        calendar.setTime(date);

        return format2Year(calendar.get(Calendar.YEAR)) + format2(calendar.get(Calendar.MONTH) + 1) + format2(calendar.get(Calendar.DAY_OF_MONTH))
            + format2(calendar.get(Calendar.HOUR_OF_DAY)) + format2(calendar.get(Calendar.MINUTE)) + format2(calendar.get(Calendar.SECOND)) + "Z";
    }

    static Date adjustedFromUTCDateString(byte[] date)
    {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        int year = toInt2(date, 0);

        if (year < 50)
        {
            year += 2000;
        }
        else
        {
            year += 1900;
        }

        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, toInt2(date, 2) - 1);
        calendar.set(Calendar.DAY_OF_MONTH, toInt2(date, 4));
        calendar.set(Calendar.HOUR_OF_DAY, toInt2(date, 6));
        calendar.set(Calendar.MINUTE, toInt2(date, 8));

        int tzChar = 10;

        if (isNumber(date, tzChar))
        {
            calendar.set(Calendar.SECOND, toInt2(date, 10));
            tzChar = 12;
        }
        else
        {
            calendar.set(Calendar.SECOND, 0);
        }

        calendar.set(Calendar.MILLISECOND, 0);

        if (date[tzChar] != 'Z')
        {
            int hoursOff = 0;
            int minutesOff = 0;

            hoursOff = toInt2(date, tzChar + 1) * 60 * 60 * 1000;

            if (date.length > tzChar + 3)
            {
                minutesOff = toInt2(date, tzChar + 3) * 60 * 1000;
            }

            if (date[tzChar] == '-')
            {
                return new Date(calendar.getTime().getTime() + hoursOff + minutesOff);
            }
            else
            {
                return new Date(calendar.getTime().getTime() - (hoursOff + minutesOff));
            }
        }

        return calendar.getTime();
    }

    static String getGeneralizedTimeDateString(Date date, boolean includeMillis)
    {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        calendar.setTime(date);

        String time = format4Year(calendar.get(Calendar.YEAR)) + format2(calendar.get(Calendar.MONTH) + 1) + format2(calendar.get(Calendar.DAY_OF_MONTH))
            + format2(calendar.get(Calendar.HOUR_OF_DAY)) + format2(calendar.get(Calendar.MINUTE)) + format2(calendar.get(Calendar.SECOND));

        if (includeMillis)
        {
            time += "." + format3(calendar.get(Calendar.MILLISECOND));
        }

        return time + "Z";
    }

    static Date fromGeneralizedTimeString(byte[] date)
    {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        int year = toInt4(date, 0);

        if (isLocalTime(date))
        {
            calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        }

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, toInt2(date, 4) - 1);
        calendar.set(Calendar.DAY_OF_MONTH, toInt2(date, 6));
        calendar.set(Calendar.HOUR_OF_DAY, toInt2(date, 8));
        calendar.set(Calendar.MINUTE, toInt2(date, 10));

        int tzChar = 12;

        if (isNumber(date, tzChar))
        {
            calendar.set(Calendar.SECOND, toInt2(date, 12));
            tzChar = 14;
        }
        else
        {
            calendar.set(Calendar.SECOND, 0);
        }

        if (tzChar != date.length && date[tzChar] == '.')
        {
            int millis = 0;
            tzChar++;
            if (isNumber(date, tzChar))
            {
                millis = (date[tzChar] - '0') * 100;
                tzChar++;
            }
            if (tzChar != date.length && isNumber(date, tzChar))
            {
                millis += (date[tzChar] - '0') * 10;
                tzChar++;
            }
            if (tzChar != date.length && isNumber(date, tzChar))
            {
                millis += (date[tzChar] - '0');
                tzChar++;
            }
            calendar.set(Calendar.MILLISECOND, millis);
        }
        else
        {
            calendar.set(Calendar.MILLISECOND, 0);
        }

        // skip nano-seconds
        while (tzChar != date.length && isNumber(date, tzChar))
        {
            tzChar++;
        }

        if (tzChar != date.length && date[tzChar] != 'Z')
        {
            int hoursOff = 0;
            int minutesOff = 0;

            hoursOff = toInt2(date, tzChar + 1) * 60 * 60 * 1000;

            if (date.length > tzChar + 3)
            {
                minutesOff = toInt2(date, tzChar + 3) * 60 * 1000;
            }

            if (date[tzChar] == '-')
            {
                return new Date(calendar.getTime().getTime() + hoursOff + minutesOff);
            }
            else
            {
                return new Date(calendar.getTime().getTime() - (hoursOff + minutesOff));
            }
        }

        return calendar.getTime();
    }

    private static String format2(int v)
    {
        if (v < 10)
        {
            return "0" + v;
        }

        return Integer.toString(v);
    }

    private static String format2Year(int v)
    {
        if (v > 2000)
        {
            v = v - 2000;
        }
        else
        {
            v = v - 1900;
        }

        return format2(v);
    }

    private static String format3(int v)
    {
        if (v < 10)
        {
            return "00" + v;
        }

        if (v < 100)
        {
            return "0" + v;
        }

        return Integer.toString(v);
    }

    private static String format4Year(int v)
    {
        if (v < 10)
        {
            return "000" + v;
        }

        if (v < 100)
        {
            return "00" + v;
        }

        if (v < 1000)
        {
            return "0" + v;
        }

        return Integer.toString(v);
    }

    private static boolean isNumber(byte[] input, int off)
    {
        byte b = input[off];
        return (b >= '0') && (b <= '9');
    }

    private static boolean isLocalTime(byte[] date)
    {
        for (int i = date.length - 1; i > date.length - 6; i--)
        {
            if (date[i] == 'Z' || date[i] == '-' || date[i] == '+')
            {
                return false;
            }
        }

        return true;
    }

    private static int toInt2(byte[] input, int off)
    {
        return (input[off] - '0') * 10 + (input[off + 1] - '0');
    }

    private static int toInt4(byte[] input, int off)
    {
        return toInt2(input, off) * 100 + toInt2(input, off + 2) ;
    }
}
