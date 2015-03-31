package org.spongycastle.asn1.x9;

import java.util.Enumeration;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1Object;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.ASN1TaggedObject;
import org.spongycastle.asn1.DERSequence;

public class DHDomainParameters
    extends ASN1Object
{
    private ASN1Integer p, g, q, j;
    private DHValidationParms validationParms;

    public static DHDomainParameters getInstance(ASN1TaggedObject obj, boolean explicit)
    {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static DHDomainParameters getInstance(Object obj)
    {
        if (obj == null || obj instanceof DHDomainParameters)
        {
            return (DHDomainParameters)obj;
        }

        if (obj instanceof ASN1Sequence)
        {
            return new DHDomainParameters((ASN1Sequence)obj);
        }

        throw new IllegalArgumentException("Invalid DHDomainParameters: "
            + obj.getClass().getName());
    }

    public DHDomainParameters(ASN1Integer p, ASN1Integer g, ASN1Integer q, ASN1Integer j,
        DHValidationParms validationParms)
    {
        if (p == null)
        {
            throw new IllegalArgumentException("'p' cannot be null");
        }
        if (g == null)
        {
            throw new IllegalArgumentException("'g' cannot be null");
        }
        if (q == null)
        {
            throw new IllegalArgumentException("'q' cannot be null");
        }

        this.p = p;
        this.g = g;
        this.q = q;
        this.j = j;
        this.validationParms = validationParms;
    }

    private DHDomainParameters(ASN1Sequence seq)
    {
        if (seq.size() < 3 || seq.size() > 5)
        {
            throw new IllegalArgumentException("Bad sequence size: " + seq.size());
        }

        Enumeration e = seq.getObjects();
        this.p = ASN1Integer.getInstance(e.nextElement());
        this.g = ASN1Integer.getInstance(e.nextElement());
        this.q = ASN1Integer.getInstance(e.nextElement());

        ASN1Encodable next = getNext(e);

        if (next != null && next instanceof ASN1Integer)
        {
            this.j = ASN1Integer.getInstance(next);
            next = getNext(e);
        }

        if (next != null)
        {
            this.validationParms = DHValidationParms.getInstance(next.toASN1Primitive());
        }
    }

    private static ASN1Encodable getNext(Enumeration e)
    {
        return e.hasMoreElements() ? (ASN1Encodable)e.nextElement() : null;
    }

    public ASN1Integer getP()
    {
        return this.p;
    }

    public ASN1Integer getG()
    {
        return this.g;
    }

    public ASN1Integer getQ()
    {
        return this.q;
    }

    public ASN1Integer getJ()
    {
        return this.j;
    }

    public DHValidationParms getValidationParms()
    {
        return this.validationParms;
    }

    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(this.p);
        v.add(this.g);
        v.add(this.q);

        if (this.j != null)
        {
            v.add(this.j);
        }

        if (this.validationParms != null)
        {
            v.add(this.validationParms);
        }

        return new DERSequence(v);
    }
}
