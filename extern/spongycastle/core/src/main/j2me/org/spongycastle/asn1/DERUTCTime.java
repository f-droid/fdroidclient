package org.spongycastle.asn1;

import java.util.Date;

public class DERUTCTime
    extends ASN1UTCTime
{
    DERUTCTime(byte[] bytes)
    {
        super(bytes);
    }

    public DERUTCTime(Date date)
    {
        super(date);
    }

    public DERUTCTime(String time)
    {
        super(time);
    }
}
