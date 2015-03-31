package org.spongycastle.cert.crmf;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1Null;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.asn1.crmf.AttributeTypeAndValue;
import org.spongycastle.asn1.crmf.CertReqMsg;
import org.spongycastle.asn1.crmf.CertRequest;
import org.spongycastle.asn1.crmf.CertTemplate;
import org.spongycastle.asn1.crmf.CertTemplateBuilder;
import org.spongycastle.asn1.crmf.OptionalValidity;
import org.spongycastle.asn1.crmf.POPOPrivKey;
import org.spongycastle.asn1.crmf.ProofOfPossession;
import org.spongycastle.asn1.crmf.SubsequentMessage;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.ExtensionsGenerator;
import org.spongycastle.asn1.x509.GeneralName;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.asn1.x509.Time;
import org.spongycastle.cert.CertIOException;
import org.spongycastle.operator.ContentSigner;

public class CertificateRequestMessageBuilder
{
    private final BigInteger certReqId;

    private ExtensionsGenerator extGenerator;
    private CertTemplateBuilder templateBuilder;
    private List controls;
    private ContentSigner popSigner;
    private PKMACBuilder pkmacBuilder;
    private char[] password;
    private GeneralName sender;
    private POPOPrivKey popoPrivKey;
    private ASN1Null popRaVerified;

    public CertificateRequestMessageBuilder(BigInteger certReqId)
    {
        this.certReqId = certReqId;

        this.extGenerator = new ExtensionsGenerator();
        this.templateBuilder = new CertTemplateBuilder();
        this.controls = new ArrayList();
    }

    public CertificateRequestMessageBuilder setPublicKey(SubjectPublicKeyInfo publicKey)
    {
        if (publicKey != null)
        {
            templateBuilder.setPublicKey(publicKey);
        }

        return this;
    }

    public CertificateRequestMessageBuilder setIssuer(X500Name issuer)
    {
        if (issuer != null)
        {
            templateBuilder.setIssuer(issuer);
        }

        return this;
    }

    public CertificateRequestMessageBuilder setSubject(X500Name subject)
    {
        if (subject != null)
        {
            templateBuilder.setSubject(subject);
        }

        return this;
    }

    public CertificateRequestMessageBuilder setSerialNumber(BigInteger serialNumber)
    {
        if (serialNumber != null)
        {
            templateBuilder.setSerialNumber(new ASN1Integer(serialNumber));
        }

        return this;
    }

    /**
     * Request a validity period for the certificate. Either, but not both, of the date parameters may be null.
     *
     * @param notBeforeDate not before date for certificate requested.
     * @param notAfterDate not after date for the certificate requested.
     *
     * @return the current builder.
     */
    public CertificateRequestMessageBuilder setValidity(Date notBeforeDate, Date notAfterDate)
    {
        templateBuilder.setValidity(new OptionalValidity(createTime(notBeforeDate), createTime(notAfterDate)));

        return this;
    }

    private Time createTime(Date date)
    {
        if (date != null)
        {
            return new Time(date);
        }

        return null;
    }

    public CertificateRequestMessageBuilder addExtension(
        ASN1ObjectIdentifier oid,
        boolean              critical,
        ASN1Encodable        value)
        throws CertIOException
    {
        CRMFUtil.addExtension(extGenerator, oid, critical, value);

        return this;
    }

    public CertificateRequestMessageBuilder addExtension(
        ASN1ObjectIdentifier oid,
        boolean              critical,
        byte[]               value)
    {
        extGenerator.addExtension(oid, critical, value);

        return this;
    }

    public CertificateRequestMessageBuilder addControl(Control control)
    {
        controls.add(control);

        return this;
    }

    public CertificateRequestMessageBuilder setProofOfPossessionSigningKeySigner(ContentSigner popSigner)
    {
        if (popoPrivKey != null || popRaVerified != null)
        {
            throw new IllegalStateException("only one proof of possession allowed");
        }

        this.popSigner = popSigner;

        return this;
    }

    public CertificateRequestMessageBuilder setProofOfPossessionSubsequentMessage(SubsequentMessage msg)
    {
        if (popSigner != null || popRaVerified != null)
        {
            throw new IllegalStateException("only one proof of possession allowed");
        }

        this.popoPrivKey = new POPOPrivKey(msg);

        return this;
    }

    public CertificateRequestMessageBuilder setProofOfPossessionRaVerified()
    {
        if (popSigner != null || popoPrivKey != null)
        {
            throw new IllegalStateException("only one proof of possession allowed");
        }

        this.popRaVerified = DERNull.INSTANCE;

        return this;
    }

    public CertificateRequestMessageBuilder setAuthInfoPKMAC(PKMACBuilder pkmacBuilder, char[] password)
    {
        this.pkmacBuilder = pkmacBuilder;
        this.password = password;

        return this;
    }

    public CertificateRequestMessageBuilder setAuthInfoSender(X500Name sender)
    {
        return setAuthInfoSender(new GeneralName(sender));
    }

    public CertificateRequestMessageBuilder setAuthInfoSender(GeneralName sender)
    {
        this.sender = sender;

        return this;
    }

    public CertificateRequestMessage build()
        throws CRMFException
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(new ASN1Integer(certReqId));

        if (!extGenerator.isEmpty())
        {
            templateBuilder.setExtensions(extGenerator.generate());
        }

        v.add(templateBuilder.build());

        if (!controls.isEmpty())
        {
            ASN1EncodableVector controlV = new ASN1EncodableVector();

            for (Iterator it = controls.iterator(); it.hasNext();)
            {
                Control control = (Control)it.next();

                controlV.add(new AttributeTypeAndValue(control.getType(), control.getValue()));
            }

            v.add(new DERSequence(controlV));
        }

        CertRequest request = CertRequest.getInstance(new DERSequence(v));

        v = new ASN1EncodableVector();

        v.add(request);

        if (popSigner != null)
        {
            CertTemplate template = request.getCertTemplate();

            if (template.getSubject() == null || template.getPublicKey() == null)
            {
                SubjectPublicKeyInfo pubKeyInfo = request.getCertTemplate().getPublicKey();
                ProofOfPossessionSigningKeyBuilder builder = new ProofOfPossessionSigningKeyBuilder(pubKeyInfo);

                if (sender != null)
                {
                    builder.setSender(sender);
                }
                else
                {
                    PKMACValueGenerator pkmacGenerator = new PKMACValueGenerator(pkmacBuilder);

                    builder.setPublicKeyMac(pkmacGenerator, password);
                }

                v.add(new ProofOfPossession(builder.build(popSigner)));
            }
            else
            {
                ProofOfPossessionSigningKeyBuilder builder = new ProofOfPossessionSigningKeyBuilder(request);

                v.add(new ProofOfPossession(builder.build(popSigner)));
            }
        }
        else if (popoPrivKey != null)
        {
            v.add(new ProofOfPossession(ProofOfPossession.TYPE_KEY_ENCIPHERMENT, popoPrivKey));
        }
        else if (popRaVerified != null)
        {
            v.add(new ProofOfPossession());
        }

        return new CertificateRequestMessage(CertReqMsg.getInstance(new DERSequence(v)));
    }
}