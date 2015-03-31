package org.spongycastle.asn1;

public class BERApplicationSpecific
    extends DERApplicationSpecific
{
    public BERApplicationSpecific(int tagNo, ASN1EncodableVector vec)
    {
        super(tagNo, vec);
    }
}
