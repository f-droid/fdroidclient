package org.spongycastle.x509;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.DERBitString;
import org.spongycastle.asn1.ASN1GeneralizedTime;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.DERObjectIdentifier;
import org.spongycastle.asn1.DEROctetString;
import org.spongycastle.asn1.DEROutputStream;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.AttCertIssuer;
import org.spongycastle.asn1.x509.Attribute;
import org.spongycastle.asn1.x509.AttributeCertificate;
import org.spongycastle.asn1.x509.V2AttributeCertificateInfoGenerator;
import org.spongycastle.asn1.x509.AttributeCertificateInfo;
import org.spongycastle.asn1.x509.X509Extension;
import org.spongycastle.asn1.x509.X509Extensions;
import org.spongycastle.util.Strings;

/**
 * class to produce an X.509 Version 2 AttributeCertificate.
 */
public class X509V2AttributeCertificateGenerator
{
    private V2AttributeCertificateInfoGenerator   acInfoGen;
    private DERObjectIdentifier         sigOID;
    private AlgorithmIdentifier         sigAlgId;
    private String                      signatureAlgorithm;
    private Hashtable                   extensions = null;
    private Vector                      extOrdering = null;
    private static Hashtable            algorithms = new Hashtable();

    static
    {
        algorithms.put("MD2WITHRSAENCRYPTION", new DERObjectIdentifier("1.2.840.113549.1.1.2"));
        algorithms.put("MD2WITHRSA", new DERObjectIdentifier("1.2.840.113549.1.1.2"));
        algorithms.put("MD5WITHRSAENCRYPTION", new DERObjectIdentifier("1.2.840.113549.1.1.4"));
        algorithms.put("MD5WITHRSA", new DERObjectIdentifier("1.2.840.113549.1.1.4"));
        algorithms.put("SHA1WITHRSAENCRYPTION", new DERObjectIdentifier("1.2.840.113549.1.1.5"));
        algorithms.put("SHA1WITHRSA", new DERObjectIdentifier("1.2.840.113549.1.1.5"));
        algorithms.put("RIPEMD160WITHRSAENCRYPTION", new DERObjectIdentifier("1.3.36.3.3.1.2"));
        algorithms.put("RIPEMD160WITHRSA", new DERObjectIdentifier("1.3.36.3.3.1.2"));
        algorithms.put("SHA1WITHDSA", new DERObjectIdentifier("1.2.840.10040.4.3"));
        algorithms.put("DSAWITHSHA1", new DERObjectIdentifier("1.2.840.10040.4.3"));
        algorithms.put("SHA1WITHECDSA", new DERObjectIdentifier("1.2.840.10045.4.1"));
        algorithms.put("ECDSAWITHSHA1", new DERObjectIdentifier("1.2.840.10045.4.1"));
    }

    public X509V2AttributeCertificateGenerator()
    {
        acInfoGen = new V2AttributeCertificateInfoGenerator();
    }

    /**
     * reset the generator
     */
    public void reset()
    {
        acInfoGen = new V2AttributeCertificateInfoGenerator();
        extensions = null;
        extOrdering = null;
    }

    /**
     * Set the Holder of this Attribute Certificate
     */
    public void setHolder(
        AttributeCertificateHolder     holder)
    {
        acInfoGen.setHolder(holder.holder);
    }

    /**
     * Set the issuer
     */
    public void setIssuer(
        AttributeCertificateIssuer  issuer)
    {
        acInfoGen.setIssuer(AttCertIssuer.getInstance(issuer.form));
    }

    /**
     * Set the Signature inside the AttributeCertificateInfo
     */
    public void setSignature(
        AlgorithmIdentifier sig)
    {
        acInfoGen.setSignature(sig);
    }

    /**
     * set the serial number for the certificate.
     */
    public void setSerialNumber(
        BigInteger      serialNumber)
    {
        acInfoGen.setSerialNumber(new ASN1Integer(serialNumber));
    }

    public void setNotBefore(
        Date    date)
    {
        acInfoGen.setStartDate(new ASN1GeneralizedTime(date));
    }

    public void setNotAfter(
        Date    date)
    {
        acInfoGen.setEndDate(new ASN1GeneralizedTime(date));
    }

    public void setSignatureAlgorithm(
        String  signatureAlgorithm)
    {
        this.signatureAlgorithm = signatureAlgorithm;

        sigOID = (DERObjectIdentifier)algorithms.get(Strings.toUpperCase(signatureAlgorithm));

        if (sigOID == null)
        {
            throw new IllegalArgumentException("Unknown signature type requested");
        }

        sigAlgId = new AlgorithmIdentifier(this.sigOID, new DERNull());

        acInfoGen.setSignature(sigAlgId);
    }
    
    /**
     * add an attribute
     */
    public void addAttribute(
        X509Attribute       attribute)
    {
        acInfoGen.addAttribute(Attribute.getInstance(attribute.toASN1Object()));
    }

    public void setIssuerUniqueId(
        boolean[] iui)
    {
        // [TODO] convert boolean array to bit string
        //acInfoGen.setIssuerUniqueID(iui);
    }
     
    /**
     * add a given extension field for the standard extensions tag (tag 3)
     * @throws IOException
     */
    public void addExtension(
        String          OID,
        boolean         critical,
        ASN1Encodable   value)
        throws IOException
    {
        this.addExtension(OID, critical, value.toASN1Primitive().getEncoded());
    }

    /**
     * add a given extension field for the standard extensions tag (tag 3)
     * The value parameter becomes the contents of the octet string associated
     * with the extension.
     */
    public void addExtension(
        String          OID,
        boolean         critical,
        byte[]          value)
    {
        if (extensions == null)
        {
            extensions = new Hashtable();
            extOrdering = new Vector();
        }

        DERObjectIdentifier oid = new DERObjectIdentifier(OID);
        
        extensions.put(oid, new X509Extension(critical, new DEROctetString(value)));
        extOrdering.addElement(oid);
    }

    /**
     * generate an X509 certificate, based on the current issuer and subject,
     * using the passed in provider for the signing.
     */
    public X509AttributeCertificate generateCertificate(
        PrivateKey      key,
        String          provider)
        throws NoSuchProviderException, SecurityException, SignatureException, InvalidKeyException
    {
        return generateCertificate(key, provider, null);
    }

    /**
     * generate an X509 certificate, based on the current issuer and subject,
     * using the passed in provider for the signing and the supplied source
     * of randomness, if required.
     */
    public X509AttributeCertificate generateCertificate(
        PrivateKey      key,
        String          provider,
        SecureRandom    random)
        throws NoSuchProviderException, SecurityException, SignatureException, InvalidKeyException
    {
        Signature sig = null;

        if (sigOID == null)
        {
            throw new IllegalStateException("no signature algorithm specified");
        }

        try
        {
            sig = Signature.getInstance(sigOID.getId(), provider);
        }
        catch (NoSuchAlgorithmException ex)
        {
            try
            {
                sig = Signature.getInstance(signatureAlgorithm, provider);
            }
            catch (NoSuchAlgorithmException e)
            {
                throw new SecurityException("exception creating signature: " + e.toString());
            }
        }

        sig.initSign(key);

        if (extensions != null)
        {
            acInfoGen.setExtensions(new X509Extensions(extOrdering, extensions));
        }

        AttributeCertificateInfo acInfo = acInfoGen.generateAttributeCertificateInfo();

        try
        {
            ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
            DEROutputStream         dOut = new DEROutputStream(bOut);

            dOut.writeObject(acInfo);

            sig.update(bOut.toByteArray());
        }
        catch (Exception e)
        {
            throw new SecurityException("exception encoding Attribute cert - " + e);
        }

        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(acInfo);
        v.add(sigAlgId);
        v.add(new DERBitString(sig.sign()));

        try
        {
            return new X509V2AttributeCertificate(new AttributeCertificate(new DERSequence(v)));
        }
        catch (IOException e)
        {
            throw new RuntimeException("constructed invalid certificate!");
        }
    }
}
