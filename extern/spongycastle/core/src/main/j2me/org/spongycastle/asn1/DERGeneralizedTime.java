package org.spongycastle.asn1;

import java.util.Date;

public class DERGeneralizedTime
    extends ASN1GeneralizedTime
{
    DERGeneralizedTime(byte[] bytes)
    {
        super(bytes);
    }

    public DERGeneralizedTime(Date date)
    {
        super(date);
    }

    public DERGeneralizedTime(Date date, boolean includeMillis)
    {
        super(date, includeMillis);
    }

    public DERGeneralizedTime(String time)
    {
        super(time);
    }
}
