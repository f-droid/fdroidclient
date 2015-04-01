package org.spongycastle.cert;

import java.math.BigInteger;
import java.util.Date;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1GeneralizedTime;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.DERSet;
import org.spongycastle.asn1.x509.AttCertIssuer;
import org.spongycastle.asn1.x509.Attribute;
import org.spongycastle.asn1.x509.ExtensionsGenerator;
import org.spongycastle.asn1.x509.V2AttributeCertificateInfoGenerator;
import org.spongycastle.operator.ContentSigner;

/**
 * class to produce an X.509 Version 2 AttributeCertificate.
 */
public class X509v2AttributeCertificateBuilder
{
    private V2AttributeCertificateInfoGenerator   acInfoGen;
    private ExtensionsGenerator extGenerator;

    /**
     * Base constructor.
     *
     * @param holder holder certificate details
     * @param issuer issuer of this attribute certificate.
     * @param serialNumber serial number of this attribute certificate.
     * @param notBefore the date before which the certificate is not valid.
     * @param notAfter the date after which the certificate is not valid.
     */
    public X509v2AttributeCertificateBuilder(AttributeCertificateHolder holder, AttributeCertificateIssuer  issuer, BigInteger serialNumber, Date notBefore, Date notAfter)
    {
        acInfoGen = new V2AttributeCertificateInfoGenerator();
        extGenerator = new ExtensionsGenerator();

        acInfoGen.setHolder(holder.holder);
        acInfoGen.setIssuer(AttCertIssuer.getInstance(issuer.form));
        acInfoGen.setSerialNumber(new ASN1Integer(serialNumber));
        acInfoGen.setStartDate(new ASN1GeneralizedTime(notBefore));
        acInfoGen.setEndDate(new ASN1GeneralizedTime(notAfter));
    }

    /**
     * Add an attribute to the certification request we are building.
     *
     * @param attrType the OID giving the type of the attribute.
     * @param attrValue the ASN.1 structure that forms the value of the attribute.
     * @return this builder object.
     */
    public X509v2AttributeCertificateBuilder addAttribute(ASN1ObjectIdentifier attrType, ASN1Encodable attrValue)
    {
        acInfoGen.addAttribute(new Attribute(attrType, new DERSet(attrValue)));

        return this;
    }

    /**
     * Add an attribute with multiple values to the certification request we are building.
     *
     * @param attrType the OID giving the type of the attribute.
     * @param attrValues an array of ASN.1 structures that form the value of the attribute.
     * @return this builder object.
     */
    public X509v2AttributeCertificateBuilder addAttribute(ASN1ObjectIdentifier attrType, ASN1Encodable[] attrValues)
    {
        acInfoGen.addAttribute(new Attribute(attrType, new DERSet(attrValues)));

        return this;
    }

    public void setIssuerUniqueId(
        boolean[] iui)
    {
        acInfoGen.setIssuerUniqueID(CertUtils.booleanToBitString(iui));
    }

    /**
     * Add a given extension field for the standard extensions tag
     *
     * @param oid the OID defining the extension type.
     * @param isCritical true if the extension is critical, false otherwise.
     * @param value the ASN.1 structure that forms the extension's value.
     * @return this builder object.
     */
    public X509v2AttributeCertificateBuilder addExtension(
        ASN1ObjectIdentifier oid,
        boolean isCritical,
        ASN1Encodable value)
        throws CertIOException
    {
        CertUtils.addExtension(extGenerator, oid, isCritical, value);

        return this;
    }

    /**
     * Add a given extension field for the standard extensions tag (tag 3) using a byte encoding of the
     * extension value.
     *
     * @param oid the OID defining the extension type.
     * @param isCritical true if the extension is critical, false otherwise.
     * @param encodedValue a byte array representing the encoding of the extension value.
     * @return this builder object.
     */
    public X509v2AttributeCertificateBuilder addExtension(
        ASN1ObjectIdentifier oid,
        boolean isCritical,
        byte[] encodedValue)
        throws CertIOException
    {
        extGenerator.addExtension(oid, isCritical, encodedValue);

        return this;
    }

   /**
     * Generate an X509 certificate, based on the current issuer and subject
     * using the passed in signer.
     *
     * @param signer the content signer to be used to generate the signature validating the certificate.
     * @return a holder containing the resulting signed certificate.
     */
    public X509AttributeCertificateHolder build(
        ContentSigner signer)
    {
        acInfoGen.setSignature(signer.getAlgorithmIdentifier());

        if (!extGenerator.isEmpty())
        {
            acInfoGen.setExtensions(extGenerator.generate());
        }

        return CertUtils.generateFullAttrCert(signer, acInfoGen.generateAttributeCertificateInfo());
    }
}
