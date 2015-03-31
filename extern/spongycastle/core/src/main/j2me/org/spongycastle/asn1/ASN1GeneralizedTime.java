package org.spongycastle.asn1;

import java.util.Date;

public class ASN1GeneralizedTime
    extends DERGeneralizedTime
{
    ASN1GeneralizedTime(byte[] bytes)
    {
        super(bytes);
    }

    public ASN1GeneralizedTime(Date date)
    {
        super(date);
    }

    public ASN1GeneralizedTime(Date date, boolean includeMillis)
    {
        super(date, includeMillis);
    }

    public ASN1GeneralizedTime(String time)
    {
        super(time);
    }
}
