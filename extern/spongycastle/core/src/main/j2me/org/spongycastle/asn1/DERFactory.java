package org.spongycastle.asn1;

class DERFactory
{
    static final ASN1Sequence EMPTY_SEQUENCE = new DERSequence();
    static final ASN1Set EMPTY_SET = new DERSet();

    static ASN1Sequence createSequence(ASN1EncodableVector v)
    {
        if (v.size() < 1)
        {
            return EMPTY_SEQUENCE;
        }
        else
        {
            return new DLSequence(v);
        }
    }

    static ASN1Set createSet(ASN1EncodableVector v)
    {
        if (v.size() < 1)
        {
            return EMPTY_SET;
        }
        else
        {
            return new DLSet(v);
        }
    }
}
