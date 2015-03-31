package org.spongycastle.x509;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1Object;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.ASN1Set;
import org.spongycastle.asn1.DERSet;
import org.spongycastle.asn1.x509.Attribute;

/**
 * Class for carrying the values in an X.509 Attribute.
 */
public class X509Attribute
    extends ASN1Object
{
    Attribute    attr;
    
    /**
     * @param at an object representing an attribute.
     */
    X509Attribute(
        ASN1Encodable   at)
    {
        this.attr = Attribute.getInstance(at);
    }

    /**
     * Create an X.509 Attribute with the type given by the passed in oid and
     * the value represented by an ASN.1 Set containing value.
     * 
     * @param oid type of the attribute
     * @param value value object to go into the atribute's value set.
     */
    public X509Attribute(
        String          oid,
        ASN1Encodable   value)
    {
        this.attr = new Attribute(new ASN1ObjectIdentifier(oid), new DERSet(value));
    }
    
    /**
     * Create an X.59 Attribute with the type given by the passed in oid and the
     * value represented by an ASN.1 Set containing the objects in value.
     * 
     * @param oid type of the attribute
     * @param value vector of values to go in the attribute's value set.
     */
    public X509Attribute(
        String              oid,
        ASN1EncodableVector value)
    {
        this.attr = new Attribute(new ASN1ObjectIdentifier(oid), new DERSet(value));
    }
    
    public String getOID()
    {
        return attr.getAttrType().getId();
    }
    
    public ASN1Encodable[] getValues()
    {
        ASN1Set         s = attr.getAttrValues();
        ASN1Encodable[] values = new ASN1Encodable[s.size()];
        
        for (int i = 0; i != s.size(); i++)
        {
            values[i] = (ASN1Encodable)s.getObjectAt(i);
        }
        
        return values;
    }
    
    public ASN1Primitive toASN1Primitive()
    {
        return attr.toASN1Primitive();
    }
}
