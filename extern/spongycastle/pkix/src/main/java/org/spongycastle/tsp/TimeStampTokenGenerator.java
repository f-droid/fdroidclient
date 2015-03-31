package org.spongycastle.tsp;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.spongycastle.asn1.ASN1Boolean;
import org.spongycastle.asn1.ASN1Encoding;
import org.spongycastle.asn1.ASN1GeneralizedTime;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.DERNull;
import org.spongycastle.asn1.cms.AttributeTable;
import org.spongycastle.asn1.ess.ESSCertID;
import org.spongycastle.asn1.ess.ESSCertIDv2;
import org.spongycastle.asn1.ess.SigningCertificate;
import org.spongycastle.asn1.ess.SigningCertificateV2;
import org.spongycastle.asn1.oiw.OIWObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.tsp.Accuracy;
import org.spongycastle.asn1.tsp.MessageImprint;
import org.spongycastle.asn1.tsp.TSTInfo;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.GeneralName;
import org.spongycastle.asn1.x509.GeneralNames;
import org.spongycastle.asn1.x509.IssuerSerial;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cms.CMSAttributeTableGenerationException;
import org.spongycastle.cms.CMSAttributeTableGenerator;
import org.spongycastle.cms.CMSException;
import org.spongycastle.cms.CMSProcessableByteArray;
import org.spongycastle.cms.CMSSignedData;
import org.spongycastle.cms.CMSSignedDataGenerator;
import org.spongycastle.cms.SignerInfoGenerator;
import org.spongycastle.operator.DigestCalculator;
import org.spongycastle.util.CollectionStore;
import org.spongycastle.util.Store;

/**
 * Currently the class supports ESSCertID by if a digest calculator based on SHA1 is passed in, otherwise it uses
 * ESSCertIDv2. In the event you need to pass both types, you will need to override the SignedAttributeGenerator
 * for the SignerInfoGeneratorBuilder you are using. For the default for ESSCertIDv2 the code will look something
 * like the following:
 * <pre>
 * final ESSCertID essCertid = new ESSCertID(certHashSha1, issuerSerial);
 * final ESSCertIDv2 essCertidV2 = new ESSCertIDv2(certHashSha256, issuerSerial);
 *
 * signerInfoGenBuilder.setSignedAttributeGenerator(new CMSAttributeTableGenerator()
 * {
 *     public AttributeTable getAttributes(Map parameters)
 *         throws CMSAttributeTableGenerationException
 *     {
 *         CMSAttributeTableGenerator attrGen = new DefaultSignedAttributeTableGenerator();
 *
 *         AttributeTable table = attrGen.getAttributes(parameters);
 *
 *         table = table.add(PKCSObjectIdentifiers.id_aa_signingCertificate, new SigningCertificate(essCertid));
 *         table = table.add(PKCSObjectIdentifiers.id_aa_signingCertificateV2, new SigningCertificateV2(essCertidV2));
 *
 *         return table;
 *     }
 * });
 * </pre>
 */
public class TimeStampTokenGenerator
{
    int accuracySeconds = -1;

    int accuracyMillis = -1;

    int accuracyMicros = -1;

    boolean ordering = false;

    GeneralName tsa = null;
    
    private ASN1ObjectIdentifier  tsaPolicyOID;

    private List certs = new ArrayList();
    private List crls = new ArrayList();
    private List attrCerts = new ArrayList();
    private SignerInfoGenerator signerInfoGen;

    /**
     * Basic Constructor - set up a calculator based on signerInfoGen with a ESSCertID calculated from
     * the signer's associated certificate using the sha1DigestCalculator. If alternate values are required
     * for id-aa-signingCertificate they should be added to the signerInfoGen object before it is passed in,
     * otherwise a standard digest based value will be added.
     *
     * @param signerInfoGen the generator for the signer we are using.
     * @param digestCalculator calculator for to use for digest of certificate.
     * @param tsaPolicy tasPolicy to send.
     * @throws IllegalArgumentException if calculator is not SHA-1 or there is no associated certificate for the signer,
     * @throws TSPException if the signer certificate cannot be processed.
     */
    public TimeStampTokenGenerator(
        final SignerInfoGenerator       signerInfoGen,
        DigestCalculator                digestCalculator,
        ASN1ObjectIdentifier            tsaPolicy)
        throws IllegalArgumentException, TSPException
    {
        this(signerInfoGen, digestCalculator, tsaPolicy, false);
    }

    /**
     * Basic Constructor - set up a calculator based on signerInfoGen with a ESSCertID calculated from
     * the signer's associated certificate using the sha1DigestCalculator. If alternate values are required
     * for id-aa-signingCertificate they should be added to the signerInfoGen object before it is passed in,
     * otherwise a standard digest based value will be added.
     *
     * @param signerInfoGen the generator for the signer we are using.
     * @param digestCalculator calculator for to use for digest of certificate.
     * @param tsaPolicy tasPolicy to send.
     * @param isIssuerSerialIncluded should issuerSerial be included in the ESSCertIDs, true if yes, by default false.
     * @throws IllegalArgumentException if calculator is not SHA-1 or there is no associated certificate for the signer,
     * @throws TSPException if the signer certificate cannot be processed.
     */
    public TimeStampTokenGenerator(
        final SignerInfoGenerator       signerInfoGen,
        DigestCalculator                digestCalculator,
        ASN1ObjectIdentifier            tsaPolicy,
        boolean                         isIssuerSerialIncluded)
        throws IllegalArgumentException, TSPException
    {
        this.signerInfoGen = signerInfoGen;
        this.tsaPolicyOID = tsaPolicy;

        if (!signerInfoGen.hasAssociatedCertificate())
        {
            throw new IllegalArgumentException("SignerInfoGenerator must have an associated certificate");
        }

        X509CertificateHolder assocCert = signerInfoGen.getAssociatedCertificate();
        TSPUtil.validateCertificate(assocCert);

        try
        {
            OutputStream dOut = digestCalculator.getOutputStream();

            dOut.write(assocCert.getEncoded());

            dOut.close();

            if (digestCalculator.getAlgorithmIdentifier().getAlgorithm().equals(OIWObjectIdentifiers.idSHA1))
            {
                final ESSCertID essCertid = new ESSCertID(digestCalculator.getDigest(),
                                            isIssuerSerialIncluded ? new IssuerSerial(new GeneralNames(new GeneralName(assocCert.getIssuer())), assocCert.getSerialNumber())
                                                                   : null);

                this.signerInfoGen = new SignerInfoGenerator(signerInfoGen, new CMSAttributeTableGenerator()
                {
                    public AttributeTable getAttributes(Map parameters)
                        throws CMSAttributeTableGenerationException
                    {
                        AttributeTable table = signerInfoGen.getSignedAttributeTableGenerator().getAttributes(parameters);

                        if (table.get(PKCSObjectIdentifiers.id_aa_signingCertificate) == null)
                        {
                            return table.add(PKCSObjectIdentifiers.id_aa_signingCertificate, new SigningCertificate(essCertid));
                        }

                        return table;
                    }
                }, signerInfoGen.getUnsignedAttributeTableGenerator());
            }
            else
            {
                AlgorithmIdentifier digAlgID = new AlgorithmIdentifier(digestCalculator.getAlgorithmIdentifier().getAlgorithm());
                final ESSCertIDv2   essCertid = new ESSCertIDv2(digAlgID, digestCalculator.getDigest(),
                                                    isIssuerSerialIncluded ? new IssuerSerial(new GeneralNames(new GeneralName(assocCert.getIssuer())), new ASN1Integer(assocCert.getSerialNumber()))
                                                                           : null);

                this.signerInfoGen = new SignerInfoGenerator(signerInfoGen, new CMSAttributeTableGenerator()
                {
                    public AttributeTable getAttributes(Map parameters)
                        throws CMSAttributeTableGenerationException
                    {
                        AttributeTable table = signerInfoGen.getSignedAttributeTableGenerator().getAttributes(parameters);

                        if (table.get(PKCSObjectIdentifiers.id_aa_signingCertificateV2) == null)
                        {
                            return table.add(PKCSObjectIdentifiers.id_aa_signingCertificateV2, new SigningCertificateV2(essCertid));
                        }

                        return table;
                    }
                }, signerInfoGen.getUnsignedAttributeTableGenerator());
            }
        }
        catch (IOException e)
        {
            throw new TSPException("Exception processing certificate.", e);
        }
    }

    /**
     * Add the store of X509 Certificates to the generator.
     *
     * @param certStore  a Store containing X509CertificateHolder objects
     */
    public void addCertificates(
        Store certStore)
    {
        certs.addAll(certStore.getMatches(null));
    }

    /**
     *
     * @param crlStore a Store containing X509CRLHolder objects.
     */
    public void addCRLs(
        Store crlStore)
    {
        crls.addAll(crlStore.getMatches(null));
    }

    /**
     *
     * @param attrStore a Store containing X509AttributeCertificate objects.
     */
    public void addAttributeCertificates(
        Store attrStore)
    {
        attrCerts.addAll(attrStore.getMatches(null));
    }

    public void setAccuracySeconds(int accuracySeconds)
    {
        this.accuracySeconds = accuracySeconds;
    }

    public void setAccuracyMillis(int accuracyMillis)
    {
        this.accuracyMillis = accuracyMillis;
    }

    public void setAccuracyMicros(int accuracyMicros)
    {
        this.accuracyMicros = accuracyMicros;
    }

    public void setOrdering(boolean ordering)
    {
        this.ordering = ordering;
    }

    public void setTSA(GeneralName tsa)
    {
        this.tsa = tsa;
    }

    /**
     * Generate a TimeStampToken for the passed in request and serialNumber marking it with the passed in genTime.
     *
     * @param request the originating request.
     * @param serialNumber serial number for the TimeStampToken
     * @param genTime token generation time.
     * @return a TimeStampToken
     * @throws TSPException
     */
    public TimeStampToken generate(
        TimeStampRequest    request,
        BigInteger          serialNumber,
        Date                genTime)
        throws TSPException
    {
        ASN1ObjectIdentifier digestAlgOID = request.getMessageImprintAlgOID();

        AlgorithmIdentifier algID = new AlgorithmIdentifier(digestAlgOID, DERNull.INSTANCE);
        MessageImprint      messageImprint = new MessageImprint(algID, request.getMessageImprintDigest());

        Accuracy accuracy = null;
        if (accuracySeconds > 0 || accuracyMillis > 0 || accuracyMicros > 0)
        {
            ASN1Integer seconds = null;
            if (accuracySeconds > 0)
            {
                seconds = new ASN1Integer(accuracySeconds);
            }

            ASN1Integer millis = null;
            if (accuracyMillis > 0)
            {
                millis = new ASN1Integer(accuracyMillis);
            }

            ASN1Integer micros = null;
            if (accuracyMicros > 0)
            {
                micros = new ASN1Integer(accuracyMicros);
            }

            accuracy = new Accuracy(seconds, millis, micros);
        }

        ASN1Boolean derOrdering = null;
        if (ordering)
        {
            derOrdering = new ASN1Boolean(ordering);
        }

        ASN1Integer  nonce = null;
        if (request.getNonce() != null)
        {
            nonce = new ASN1Integer(request.getNonce());
        }

        ASN1ObjectIdentifier tsaPolicy = tsaPolicyOID;
        if (request.getReqPolicy() != null)
        {
            tsaPolicy = request.getReqPolicy();
        }

        TSTInfo tstInfo = new TSTInfo(tsaPolicy,
                messageImprint, new ASN1Integer(serialNumber),
                new ASN1GeneralizedTime(genTime), accuracy, derOrdering,
                nonce, tsa, request.getExtensions());

        try
        {
            CMSSignedDataGenerator  signedDataGenerator = new CMSSignedDataGenerator();

            if (request.getCertReq())
            {
                // TODO: do we need to check certs non-empty?
                signedDataGenerator.addCertificates(new CollectionStore(certs));
                signedDataGenerator.addCRLs(new CollectionStore(crls));
                signedDataGenerator.addAttributeCertificates(new CollectionStore(attrCerts));
            }
            else
            {
                signedDataGenerator.addCRLs(new CollectionStore(crls));
            }

            signedDataGenerator.addSignerInfoGenerator(signerInfoGen);

            byte[] derEncodedTSTInfo = tstInfo.getEncoded(ASN1Encoding.DER);

            CMSSignedData signedData = signedDataGenerator.generate(new CMSProcessableByteArray(PKCSObjectIdentifiers.id_ct_TSTInfo, derEncodedTSTInfo), true);

            return new TimeStampToken(signedData);
        }
        catch (CMSException cmsEx)
        {
            throw new TSPException("Error generating time-stamp token", cmsEx);
        }
        catch (IOException e)
        {
            throw new TSPException("Exception encoding info", e);
        }
    }
}
