package org.spongycastle.jce.provider;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1Encoding;
import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.util.ASN1Dump;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.CRLDistPoint;
import org.spongycastle.asn1.x509.CRLNumber;
import org.spongycastle.asn1.x509.CertificateList;
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.asn1.x509.Extensions;
import org.spongycastle.asn1.x509.GeneralNames;
import org.spongycastle.asn1.x509.IssuingDistributionPoint;
import org.spongycastle.asn1.x509.TBSCertList;
import org.spongycastle.jce.X509Principal;
import org.spongycastle.util.encoders.Hex;

/**
 * The following extensions are listed in RFC 2459 as relevant to CRLs
 *
 * Authority Key Identifier
 * Issuer Alternative Name
 * CRL Number
 * Delta CRL Indicator (critical)
 * Issuing Distribution Point (critical)
 */
public class X509CRLObject
    extends X509CRL
{
    private CertificateList c;
    private String sigAlgName;
    private byte[] sigAlgParams;
    private boolean isIndirect;
    private boolean isHashCodeSet = false;
    private int     hashCodeValue;

    static boolean isIndirectCRL(X509CRL crl)
        throws CRLException
    {
        try
        {
            byte[] idp = crl.getExtensionValue(Extension.issuingDistributionPoint.getId());
            return idp != null
                && IssuingDistributionPoint.getInstance(ASN1OctetString.getInstance(idp).getOctets()).isIndirectCRL();
        }
        catch (Exception e)
        {
            throw new ExtCRLException(
                    "Exception reading IssuingDistributionPoint", e);
        }
    }

    public X509CRLObject(
        CertificateList c)
        throws CRLException
    {
        this.c = c;
        
        try
        {
            this.sigAlgName = X509SignatureUtil.getSignatureName(c.getSignatureAlgorithm());
            
            if (c.getSignatureAlgorithm().getParameters() != null)
            {
                this.sigAlgParams = ((ASN1Encodable)c.getSignatureAlgorithm().getParameters()).toASN1Primitive().getEncoded(ASN1Encoding.DER);
            }
            else
            {
                this.sigAlgParams = null;
            }

            this.isIndirect = isIndirectCRL(this);
        }
        catch (Exception e)
        {
            throw new CRLException("CRL contents invalid: " + e);
        }
    }

    /**
     * Will return true if any extensions are present and marked
     * as critical as we currently dont handle any extensions!
     */
    public boolean hasUnsupportedCriticalExtension()
    {
        Set extns = getCriticalExtensionOIDs();

        if (extns == null)
        {
            return false;
        }

        extns.remove(RFC3280CertPathUtilities.ISSUING_DISTRIBUTION_POINT);
        extns.remove(RFC3280CertPathUtilities.DELTA_CRL_INDICATOR);

        return !extns.isEmpty();
    }

    private Set getExtensionOIDs(boolean critical)
    {
        if (this.getVersion() == 2)
        {
            Extensions extensions = c.getTBSCertList().getExtensions();

            if (extensions != null)
            {
                Set set = new HashSet();
                Enumeration e = extensions.oids();

                while (e.hasMoreElements())
                {
                    ASN1ObjectIdentifier oid = (ASN1ObjectIdentifier)e.nextElement();
                    Extension ext = extensions.getExtension(oid);

                    if (critical == ext.isCritical())
                    {
                        set.add(oid.getId());
                    }
                }

                return set;
            }
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
        Extensions exts = c.getTBSCertList().getExtensions();

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
                    throw new IllegalStateException("error parsing " + e.toString());
                }
            }
        }

        return null;
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

    public void verify(PublicKey key)
        throws CRLException,  NoSuchAlgorithmException,
            InvalidKeyException, NoSuchProviderException, SignatureException
    {
        verify(key, BouncyCastleProvider.PROVIDER_NAME);
    }

    public void verify(PublicKey key, String sigProvider)
        throws CRLException, NoSuchAlgorithmException,
            InvalidKeyException, NoSuchProviderException, SignatureException
    {
        if (!c.getSignatureAlgorithm().equals(c.getTBSCertList().getSignature()))
        {
            throw new CRLException("Signature algorithm on CertificateList does not match TBSCertList.");
        }

        Signature sig;

        if (sigProvider != null)
        {
            sig = Signature.getInstance(getSigAlgName(), sigProvider);
        }
        else
        {
            sig = Signature.getInstance(getSigAlgName());
        }

        sig.initVerify(key);
        sig.update(this.getTBSCertList());

        if (!sig.verify(this.getSignature()))
        {
            throw new SignatureException("CRL does not verify with supplied public key.");
        }
    }

    public int getVersion()
    {
        return c.getVersionNumber();
    }

    public Principal getIssuerDN()
    {
        return new X509Principal(X500Name.getInstance(c.getIssuer().toASN1Primitive()));
    }

    public X500Principal getIssuerX500Principal()
    {
        try
        {
            return new X500Principal(c.getIssuer().getEncoded());
        }
        catch (IOException e)
        {
            throw new IllegalStateException("can't encode issuer DN");
        }
    }

    public Date getThisUpdate()
    {
        return c.getThisUpdate().getDate();
    }

    public Date getNextUpdate()
    {
        if (c.getNextUpdate() != null)
        {
            return c.getNextUpdate().getDate();
        }

        return null;
    }
 
    private Set loadCRLEntries()
    {
        Set entrySet = new HashSet();
        Enumeration certs = c.getRevokedCertificateEnumeration();

        X500Name previousCertificateIssuer = null; // the issuer
        while (certs.hasMoreElements())
        {
            TBSCertList.CRLEntry entry = (TBSCertList.CRLEntry)certs.nextElement();
            X509CRLEntryObject crlEntry = new X509CRLEntryObject(entry, isIndirect, previousCertificateIssuer);
            entrySet.add(crlEntry);
            if (isIndirect && entry.hasExtensions())
            {
                Extension currentCaName = entry.getExtensions().getExtension(Extension.certificateIssuer);

                if (currentCaName != null)
                {
                    previousCertificateIssuer = X500Name.getInstance(GeneralNames.getInstance(currentCaName.getParsedValue()).getNames()[0].getName());
                }
            }
        }

        return entrySet;
    }

    public X509CRLEntry getRevokedCertificate(BigInteger serialNumber)
    {
        Enumeration certs = c.getRevokedCertificateEnumeration();

        X500Name previousCertificateIssuer = null; // the issuer
        while (certs.hasMoreElements())
        {
            TBSCertList.CRLEntry entry = (TBSCertList.CRLEntry)certs.nextElement();

            if (serialNumber.equals(entry.getUserCertificate().getValue()))
            {
                return new X509CRLEntryObject(entry, isIndirect, previousCertificateIssuer);
            }

            if (isIndirect && entry.hasExtensions())
            {
                Extension currentCaName = entry.getExtensions().getExtension(Extension.certificateIssuer);

                if (currentCaName != null)
                {
                    previousCertificateIssuer = X500Name.getInstance(GeneralNames.getInstance(currentCaName.getParsedValue()).getNames()[0].getName());
                }
            }
        }

        return null;
    }

    public Set getRevokedCertificates()
    {
        Set entrySet = loadCRLEntries();

        if (!entrySet.isEmpty())
        {
            return Collections.unmodifiableSet(entrySet);
        }

        return null;
    }

    public byte[] getTBSCertList()
        throws CRLException
    {
        try
        {
            return c.getTBSCertList().getEncoded("DER");
        }
        catch (IOException e)
        {
            throw new CRLException(e.toString());
        }
    }

    public byte[] getSignature()
    {
        return c.getSignature().getBytes();
    }

    public String getSigAlgName()
    {
        return sigAlgName;
    }

    public String getSigAlgOID()
    {
        return c.getSignatureAlgorithm().getAlgorithm().getId();
    }

    public byte[] getSigAlgParams()
    {
        if (sigAlgParams != null)
        {
            byte[] tmp = new byte[sigAlgParams.length];
            
            System.arraycopy(sigAlgParams, 0, tmp, 0, tmp.length);
            
            return tmp;
        }
        
        return null;
    }

    /**
     * Returns a string representation of this CRL.
     *
     * @return a string representation of this CRL.
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        String nl = System.getProperty("line.separator");

        buf.append("              Version: ").append(this.getVersion()).append(
            nl);
        buf.append("             IssuerDN: ").append(this.getIssuerDN())
            .append(nl);
        buf.append("          This update: ").append(this.getThisUpdate())
            .append(nl);
        buf.append("          Next update: ").append(this.getNextUpdate())
            .append(nl);
        buf.append("  Signature Algorithm: ").append(this.getSigAlgName())
            .append(nl);

        byte[] sig = this.getSignature();

        buf.append("            Signature: ").append(
            new String(Hex.encode(sig, 0, 20))).append(nl);
        for (int i = 20; i < sig.length; i += 20)
        {
            if (i < sig.length - 20)
            {
                buf.append("                       ").append(
                    new String(Hex.encode(sig, i, 20))).append(nl);
            }
            else
            {
                buf.append("                       ").append(
                    new String(Hex.encode(sig, i, sig.length - i))).append(nl);
            }
        }

        Extensions extensions = c.getTBSCertList().getExtensions();

        if (extensions != null)
        {
            Enumeration e = extensions.oids();

            if (e.hasMoreElements())
            {
                buf.append("           Extensions: ").append(nl);
            }

            while (e.hasMoreElements())
            {
                ASN1ObjectIdentifier oid = (ASN1ObjectIdentifier) e.nextElement();
                Extension ext = extensions.getExtension(oid);

                if (ext.getExtnValue() != null)
                {
                    byte[] octs = ext.getExtnValue().getOctets();
                    ASN1InputStream dIn = new ASN1InputStream(octs);
                    buf.append("                       critical(").append(
                        ext.isCritical()).append(") ");
                    try
                    {
                        if (oid.equals(Extension.cRLNumber))
                        {
                            buf.append(
                                new CRLNumber(ASN1Integer.getInstance(
                                    dIn.readObject()).getPositiveValue()))
                                .append(nl);
                        }
                        else if (oid.equals(Extension.deltaCRLIndicator))
                        {
                            buf.append(
                                "Base CRL: "
                                    + new CRLNumber(ASN1Integer.getInstance(
                                        dIn.readObject()).getPositiveValue()))
                                .append(nl);
                        }
                        else if (oid
                            .equals(Extension.issuingDistributionPoint))
                        {
                            buf.append(
                               IssuingDistributionPoint.getInstance(dIn.readObject())).append(nl);
                        }
                        else if (oid
                            .equals(Extension.cRLDistributionPoints))
                        {
                            buf.append(
                                CRLDistPoint.getInstance(dIn.readObject())).append(nl);
                        }
                        else if (oid.equals(Extension.freshestCRL))
                        {
                            buf.append(
                                CRLDistPoint.getInstance(dIn.readObject())).append(nl);
                        }
                        else
                        {
                            buf.append(oid.getId());
                            buf.append(" value = ").append(
                                ASN1Dump.dumpAsString(dIn.readObject()))
                                .append(nl);
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
        Set set = getRevokedCertificates();
        if (set != null)
        {
            Iterator it = set.iterator();
            while (it.hasNext())
            {
                buf.append(it.next());
                buf.append(nl);
            }
        }
        return buf.toString();
    }

    /**
     * Checks whether the given certificate is on this CRL.
     *
     * @param cert the certificate to check for.
     * @return true if the given certificate is on this CRL,
     * false otherwise.
     */
    public boolean isRevoked(Certificate cert)
    {
        if (!cert.getType().equals("X.509"))
        {
            throw new RuntimeException("X.509 CRL used with non X.509 Cert");
        }

        Enumeration certs = c.getRevokedCertificateEnumeration();

        X500Name caName = c.getIssuer();

        if (certs != null)
        {
            BigInteger serial = ((X509Certificate)cert).getSerialNumber();

            while (certs.hasMoreElements())
            {
                TBSCertList.CRLEntry entry = TBSCertList.CRLEntry.getInstance(certs.nextElement());

                if (isIndirect && entry.hasExtensions())
                {
                    Extension currentCaName = entry.getExtensions().getExtension(Extension.certificateIssuer);

                    if (currentCaName != null)
                    {
                        caName = X500Name.getInstance(GeneralNames.getInstance(currentCaName.getParsedValue()).getNames()[0].getName());
                    }
                }

                if (entry.getUserCertificate().getValue().equals(serial))
                {
                    X500Name issuer;

                    if (cert instanceof  X509Certificate)
                    {
                        issuer = X500Name.getInstance(((X509Certificate)cert).getIssuerX500Principal().getEncoded());
                    }
                    else
                    {
                        try
                        {
                            issuer = org.spongycastle.asn1.x509.Certificate.getInstance(cert.getEncoded()).getIssuer();
                        }
                        catch (CertificateEncodingException e)
                        {
                            throw new RuntimeException("Cannot process certificate");
                        }
                    }

                    if (!caName.equals(issuer))
                    {
                        return false;
                    }

                    return true;
                }
            }
        }

        return false;
    }

    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof X509CRL))
        {
            return false;
        }

        if (other instanceof X509CRLObject)
        {
            X509CRLObject crlObject = (X509CRLObject)other;

            if (isHashCodeSet)
            {
                boolean otherIsHashCodeSet = crlObject.isHashCodeSet;
                if (otherIsHashCodeSet)
                {
                    if (crlObject.hashCodeValue != hashCodeValue)
                    {
                        return false;
                    }
                }
            }

            return this.c.equals(crlObject.c);
        }

        return super.equals(other);
    }

    public int hashCode()
    {
        if (!isHashCodeSet)
        {
            isHashCodeSet = true;
            hashCodeValue = super.hashCode();
        }

        return hashCodeValue;
    }
}

