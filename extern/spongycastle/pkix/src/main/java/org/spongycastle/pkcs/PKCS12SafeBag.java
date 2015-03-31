package org.spongycastle.pkcs;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1Set;
import org.spongycastle.asn1.pkcs.Attribute;
import org.spongycastle.asn1.pkcs.CRLBag;
import org.spongycastle.asn1.pkcs.CertBag;
import org.spongycastle.asn1.pkcs.EncryptedPrivateKeyInfo;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.pkcs.SafeBag;
import org.spongycastle.asn1.x509.Certificate;
import org.spongycastle.asn1.x509.CertificateList;
import org.spongycastle.cert.X509CRLHolder;
import org.spongycastle.cert.X509CertificateHolder;

public class PKCS12SafeBag
{
    public static final ASN1ObjectIdentifier friendlyNameAttribute = PKCSObjectIdentifiers.pkcs_9_at_friendlyName;
    public static final ASN1ObjectIdentifier localKeyIdAttribute = PKCSObjectIdentifiers.pkcs_9_at_localKeyId;

    private SafeBag safeBag;

    public PKCS12SafeBag(SafeBag safeBag)
    {
        this.safeBag = safeBag;
    }

    /**
     * Return the underlying ASN.1 structure for this safe bag.
     *
     * @return a SafeBag
     */
    public SafeBag toASN1Structure()
    {
        return safeBag;
    }

    /**
     * Return the BagId giving the type of content in the bag.
     *
     * @return the bagId
     */
    public ASN1ObjectIdentifier getType()
    {
        return safeBag.getBagId();
    }

    public Attribute[] getAttributes()
    {
        ASN1Set attrs = safeBag.getBagAttributes();

        if (attrs == null)
        {
            return null;
        }

        Attribute[] attributes = new Attribute[attrs.size()];
        for (int i = 0; i != attrs.size(); i++)
        {
            attributes[i] = Attribute.getInstance(attrs.getObjectAt(i));
        }

        return attributes;
    }

    public Object getBagValue()
    {
        if (getType().equals(PKCSObjectIdentifiers.pkcs8ShroudedKeyBag))
        {
            return new PKCS8EncryptedPrivateKeyInfo(EncryptedPrivateKeyInfo.getInstance(safeBag.getBagValue()));
        }
        if (getType().equals(PKCSObjectIdentifiers.certBag))
        {
            CertBag certBag = CertBag.getInstance(safeBag.getBagValue());

            return new X509CertificateHolder(Certificate.getInstance(ASN1OctetString.getInstance(certBag.getCertValue()).getOctets()));
        }
        if (getType().equals(PKCSObjectIdentifiers.keyBag))
        {
            return PrivateKeyInfo.getInstance(safeBag.getBagValue());
        }
        if (getType().equals(PKCSObjectIdentifiers.crlBag))
        {
            CRLBag crlBag = CRLBag.getInstance(safeBag.getBagValue());

            return new X509CRLHolder(CertificateList.getInstance(ASN1OctetString.getInstance(crlBag.getCRLValue()).getOctets()));
        }

        return safeBag.getBagValue();
    }
}
