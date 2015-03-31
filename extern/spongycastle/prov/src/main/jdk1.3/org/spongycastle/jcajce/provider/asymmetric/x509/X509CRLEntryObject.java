package org.spongycastle.jcajce.provider.asymmetric.x509;

import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CRLException;
import java.security.cert.X509CRLEntry;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.spongycastle.asn1.ASN1Encoding;
import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.DEREnumerated;
import org.spongycastle.asn1.util.ASN1Dump;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.CRLReason;
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.asn1.x509.Extensions;
import org.spongycastle.asn1.x509.GeneralName;
import org.spongycastle.asn1.x509.GeneralNames;
import org.spongycastle.asn1.x509.TBSCertList;
import org.spongycastle.asn1.x509.X509Extension;
import org.spongycastle.x509.extension.X509ExtensionUtil;
import org.spongycastle.jce.X509Principal;

/**
 * The following extensions are listed in RFC 2459 as relevant to CRL Entries
 * 
 * ReasonCode Hode Instruction Code Invalidity Date Certificate Issuer
 * (critical)
 */
class X509CRLEntryObject extends X509CRLEntry
{
    private TBSCertList.CRLEntry c;

    private X500Name certificateIssuer;
    private int           hashValue;
    private boolean       isHashValueSet;

    public X509CRLEntryObject(TBSCertList.CRLEntry c)
    {
        this.c = c;
        this.certificateIssuer = null;
    }

    /**
     * Constructor for CRLEntries of indirect CRLs. If <code>isIndirect</code>
     * is <code>false</code> {@link #getCertificateIssuer()} will always
     * return <code>null</code>, <code>previousCertificateIssuer</code> is
     * ignored. If this <code>isIndirect</code> is specified and this CRLEntry
     * has no certificate issuer CRL entry extension
     * <code>previousCertificateIssuer</code> is returned by
     * {@link #getCertificateIssuer()}.
     * 
     * @param c
     *            TBSCertList.CRLEntry object.
     * @param isIndirect
     *            <code>true</code> if the corresponding CRL is a indirect
     *            CRL.
     * @param previousCertificateIssuer
     *            Certificate issuer of the previous CRLEntry.
     */
    public X509CRLEntryObject(
        TBSCertList.CRLEntry c,
        boolean isIndirect,
        X500Name previousCertificateIssuer)
    {
        this.c = c;
        this.certificateIssuer = loadCertificateIssuer(isIndirect, previousCertificateIssuer);
    }

    /**
     * Will return true if any extensions are present and marked as critical as
     * we currently don't handle any extensions!
     */
    public boolean hasUnsupportedCriticalExtension()
    {
        Set extns = getCriticalExtensionOIDs();

        return extns != null && !extns.isEmpty();
    }

    private X500Name loadCertificateIssuer(boolean isIndirect, X500Name previousCertificateIssuer)
    {
        if (!isIndirect)
        {
            return null;
        }

        byte[] ext = getExtensionValue(X509Extension.certificateIssuer.getId());
        if (ext == null)
        {
            return previousCertificateIssuer;
        }

        try
        {
            GeneralName[] names = GeneralNames.getInstance(
                    X509ExtensionUtil.fromExtensionValue(ext)).getNames();
            for (int i = 0; i < names.length; i++)
            {
                if (names[i].getTagNo() == GeneralName.directoryName)
                {
                    return X500Name.getInstance(names[i].getName());
                }
            }
            return null;
        }
        catch (IOException e)
        {
            return null;
        }
    }

    X509Principal getCertificateIssuer()
    {
        if (certificateIssuer == null)
        {
            return null;
        }
	try
	{
            return new X509Principal(certificateIssuer.getEncoded());
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e.toString());
        }
    }
    private Set getExtensionOIDs(boolean critical)
    {
        Extensions extensions = c.getExtensions();

        if (extensions != null)
        {
            Set set = new HashSet();
            Enumeration e = extensions.oids();

            while (e.hasMoreElements())
            {
                ASN1ObjectIdentifier oid = (ASN1ObjectIdentifier) e.nextElement();
                Extension ext = extensions.getExtension(oid);

                if (critical == ext.isCritical())
                {
                    set.add(oid.getId());
                }
            }

            return set;
        }

        return null;
    }

    public Set getCriticalExtensionOIDs()
    {
        return getExtensionOIDs(true);
    }

    public Set getNonCriticalExtensionOIDs()
    {
        return getExtensionOIDs(false);
    }

    public byte[] getExtensionValue(String oid)
    {
        Extensions exts = c.getExtensions();

        if (exts != null)
        {
            Extension ext = exts.getExtension(new ASN1ObjectIdentifier(oid));

            if (ext != null)
            {
                try
                {
                    return ext.getExtnValue().getEncoded();
                }
                catch (Exception e)
                {
                    throw new RuntimeException("error encoding " + e.toString());
                }
            }
        }

        return null;
    }

    /**
     * Cache the hashCode value - calculating it with the standard method.
     * @return  calculated hashCode.
     */
    public int hashCode()
    {
        if (!isHashValueSet)
        {
            hashValue = super.hashCode();
            isHashValueSet = true;
        }

        return hashValue;
    }

    public byte[] getEncoded()
        throws CRLException
    {
        try
        {
            return c.getEncoded(ASN1Encoding.DER);
        }
        catch (IOException e)
        {
            throw new CRLException(e.toString());
        }
    }

    public BigInteger getSerialNumber()
    {
        return c.getUserCertificate().getValue();
    }

    public Date getRevocationDate()
    {
        return c.getRevocationDate().getDate();
    }

    public boolean hasExtensions()
    {
        return c.getExtensions() != null;
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        String nl = System.getProperty("line.separator");

        buf.append("      userCertificate: ").append(this.getSerialNumber()).append(nl);
        buf.append("       revocationDate: ").append(this.getRevocationDate()).append(nl);

        Extensions extensions = c.getExtensions();

        if (extensions != null)
        {
            Enumeration e = extensions.oids();
            if (e.hasMoreElements())
            {
                buf.append("   crlEntryExtensions:").append(nl);

                while (e.hasMoreElements())
                {
                    ASN1ObjectIdentifier oid = (ASN1ObjectIdentifier)e.nextElement();
                    Extension ext = extensions.getExtension(oid);
                    if (ext.getExtnValue() != null)
                    {
                        byte[]                  octs = ext.getExtnValue().getOctets();
                        ASN1InputStream dIn = new ASN1InputStream(octs);
                        buf.append("                       critical(").append(ext.isCritical()).append(") ");
                        try
                        {
                            if (oid.equals(X509Extension.reasonCode))
                            {
                                buf.append(CRLReason.getInstance(DEREnumerated.getInstance(dIn.readObject()))).append(nl);
                            }
                            else if (oid.equals(X509Extension.certificateIssuer))
                            {
                                buf.append("Certificate issuer: ").append(GeneralNames.getInstance(dIn.readObject())).append(nl);
                            }
                            else 
                            {
                                buf.append(oid.getId());
                                buf.append(" value = ").append(ASN1Dump.dumpAsString(dIn.readObject())).append(nl);
                            }
                        }
                        catch (Exception ex)
                        {
                            buf.append(oid.getId());
                            buf.append(" value = ").append("*****").append(nl);
                        }
                    }
                    else
                    {
                        buf.append(nl);
                    }
                }
            }
        }

        return buf.toString();
    }
}
