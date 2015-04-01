package org.spongycastle.util;

public final class Times
{
    private static long NANOS_PER_MILLI = 1000000L;

    public static long nanoTime()
    {
        return NANOS_PER_MILLI * System.currentTimeMillis();
    }
}
