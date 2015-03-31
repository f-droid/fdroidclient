package org.spongycastle.cms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.ASN1OctetStringParser;
import org.spongycastle.asn1.ASN1SequenceParser;
import org.spongycastle.asn1.ASN1Set;
import org.spongycastle.asn1.ASN1SetParser;
import org.spongycastle.asn1.BERTags;
import org.spongycastle.asn1.DERSet;
import org.spongycastle.asn1.cms.AttributeTable;
import org.spongycastle.asn1.cms.AuthenticatedDataParser;
import org.spongycastle.asn1.cms.CMSAttributes;
import org.spongycastle.asn1.cms.ContentInfoParser;
import org.spongycastle.asn1.cms.OriginatorInfo;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.operator.DigestCalculatorProvider;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.util.Arrays;

/**
 * Parsing class for an CMS Authenticated Data object from an input stream.
 * <p>
 * Note: that because we are in a streaming mode only one recipient can be tried and it is important
 * that the methods on the parser are called in the appropriate order.
 * </p>
 * <p>
 * Example of use - assuming the first recipient matches the private key we have.
 * <pre>
 *      CMSAuthenticatedDataParser     ad = new CMSAuthenticatedDataParser(inputStream);
 *
 *      RecipientInformationStore  recipients = ad.getRecipientInfos();
 *
 *      Collection  c = recipients.getRecipients();
 *      Iterator    it = c.iterator();
 *
 *      if (it.hasNext())
 *      {
 *          RecipientInformation   recipient = (RecipientInformation)it.next();
 *
 *          CMSTypedStream recData = recipient.getContentStream(new JceKeyTransAuthenticatedRecipient(privateKey).setProvider("SC"));
 *
 *          processDataStream(recData.getContentStream());
 *
 *          if (!Arrays.equals(ad.getMac(), recipient.getMac())
 *          {
 *              System.err.println("Data corrupted!!!!");
 *          }
 *      }
 *  </pre>
 *  Note: this class does not introduce buffering - if you are processing large files you should create
 *  the parser with:
 *  <pre>
 *          CMSAuthenticatedDataParser     ep = new CMSAuthenticatedDataParser(new BufferedInputStream(inputStream, bufSize));
 *  </pre>
 *  where bufSize is a suitably large buffer size.
 */
public class CMSAuthenticatedDataParser
    extends CMSContentInfoParser
{
    RecipientInformationStore recipientInfoStore;
    AuthenticatedDataParser authData;

    private AlgorithmIdentifier macAlg;
    private byte[] mac;
    private AttributeTable authAttrs;
    private ASN1Set authAttrSet;
    private AttributeTable unauthAttrs;

    private boolean authAttrNotRead;
    private boolean unauthAttrNotRead;
    private OriginatorInformation originatorInfo;

    public CMSAuthenticatedDataParser(
        byte[] envelopedData)
        throws CMSException, IOException
    {
        this(new ByteArrayInputStream(envelopedData));
    }

    public CMSAuthenticatedDataParser(
        byte[] envelopedData,
        DigestCalculatorProvider digestCalculatorProvider)
        throws CMSException, IOException
    {
        this(new ByteArrayInputStream(envelopedData), digestCalculatorProvider);
    }

    public CMSAuthenticatedDataParser(
        InputStream envelopedData)
        throws CMSException, IOException
    {
        this(envelopedData, null);
    }

    public CMSAuthenticatedDataParser(
        InputStream envelopedData,
        DigestCalculatorProvider digestCalculatorProvider)
        throws CMSException, IOException
    {
        super(envelopedData);

        this.authAttrNotRead = true;
        this.authData = new AuthenticatedDataParser((ASN1SequenceParser)_contentInfo.getContent(BERTags.SEQUENCE));

        // TODO Validate version?
        //DERInteger version = this.authData.getVersion();

        OriginatorInfo info = authData.getOriginatorInfo();

        if (info != null)
        {
            this.originatorInfo = new OriginatorInformation(info);
        }
        //
        // read the recipients
        //
        ASN1Set recipientInfos = ASN1Set.getInstance(authData.getRecipientInfos().toASN1Primitive());

        this.macAlg = authData.getMacAlgorithm();

        //
        // build the RecipientInformationStore
        //
        AlgorithmIdentifier digestAlgorithm = authData.getDigestAlgorithm();

        if (digestAlgorithm != null)
        {
            if (digestCalculatorProvider == null)
            {
                throw new CMSException("a digest calculator provider is required if authenticated attributes are present");
            }

            //
            // read the authenticated content info
            //
            ContentInfoParser data = authData.getEncapsulatedContentInfo();
            CMSReadable readable = new CMSProcessableInputStream(
                ((ASN1OctetStringParser)data.getContent(BERTags.OCTET_STRING)).getOctetStream());

            try
            {
                CMSSecureReadable secureReadable = new CMSEnvelopedHelper.CMSDigestAuthenticatedSecureReadable(digestCalculatorProvider.get(digestAlgorithm), readable);

                this.recipientInfoStore = CMSEnvelopedHelper.buildRecipientInformationStore(recipientInfos, this.macAlg, secureReadable, new AuthAttributesProvider()
                {
                    public ASN1Set getAuthAttributes()
                    {
                        try
                        {
                            return getAuthAttrSet();
                        }
                        catch (IOException e)
                        {
                            throw new IllegalStateException("can't parse authenticated attributes!");
                        }
                    }
                });
            }
            catch (OperatorCreationException e)
            {
                throw new CMSException("unable to create digest calculator: " + e.getMessage(), e);
            }
        }
        else
        {
            //
            // read the authenticated content info
            //
            ContentInfoParser data = authData.getEncapsulatedContentInfo();
            CMSReadable readable = new CMSProcessableInputStream(
                ((ASN1OctetStringParser)data.getContent(BERTags.OCTET_STRING)).getOctetStream());

            CMSSecureReadable secureReadable = new CMSEnvelopedHelper.CMSAuthenticatedSecureReadable(this.macAlg, readable);

            this.recipientInfoStore = CMSEnvelopedHelper.buildRecipientInformationStore(recipientInfos, this.macAlg, secureReadable);
        }


    }

    /**
     * Return the originator information associated with this message if present.
     *
     * @return OriginatorInformation, null if not present.
     */
    public OriginatorInformation getOriginatorInfo()
    {
        return originatorInfo;
    }

    /**
     * Return the MAC algorithm details for the MAC associated with the data in this object.
     *
     * @return AlgorithmIdentifier representing the MAC algorithm.
     */
    public AlgorithmIdentifier getMacAlgorithm()
    {
        return macAlg;
    }

    /**
     * return the object identifier for the mac algorithm.
     */
    public String getMacAlgOID()
    {
        return macAlg.getAlgorithm().toString();
    }

    /**
     * return the ASN.1 encoded encryption algorithm parameters, or null if
     * there aren't any.
     */
    public byte[] getMacAlgParams()
    {
        try
        {
            return encodeObj(macAlg.getParameters());
        }
        catch (Exception e)
        {
            throw new RuntimeException("exception getting encryption parameters " + e);
        }
    }

    /**
     * return a store of the intended recipients for this message
     */
    public RecipientInformationStore getRecipientInfos()
    {
        return recipientInfoStore;
    }

    public byte[] getMac()
        throws IOException
    {
        if (mac == null)
        {
            getAuthAttrs();
            mac = authData.getMac().getOctets();
        }
        return Arrays.clone(mac);
    }

    private ASN1Set getAuthAttrSet()
        throws IOException
    {
        if (authAttrs == null && authAttrNotRead)
        {
            ASN1SetParser set = authData.getAuthAttrs();

            if (set != null)
            {
                authAttrSet = (ASN1Set)set.toASN1Primitive();
            }

            authAttrNotRead = false;
        }

        return authAttrSet;
    }

    /**
     * return a table of the unauthenticated attributes indexed by
     * the OID of the attribute.
     * @exception java.io.IOException
     */
    public AttributeTable getAuthAttrs()
        throws IOException
    {
        if (authAttrs == null && authAttrNotRead)
        {
            ASN1Set set = getAuthAttrSet();

            if (set != null)
            {
                authAttrs = new AttributeTable(set);
            }
        }

        return authAttrs;
    }

    /**
     * return a table of the unauthenticated attributes indexed by
     * the OID of the attribute.
     * @exception java.io.IOException
     */
    public AttributeTable getUnauthAttrs()
        throws IOException
    {
        if (unauthAttrs == null && unauthAttrNotRead)
        {
            ASN1SetParser set = authData.getUnauthAttrs();

            unauthAttrNotRead = false;

            if (set != null)
            {
                ASN1EncodableVector v = new ASN1EncodableVector();
                ASN1Encodable o;

                while ((o = set.readObject()) != null)
                {
                    ASN1SequenceParser seq = (ASN1SequenceParser)o;

                    v.add(seq.toASN1Primitive());
                }

                unauthAttrs = new AttributeTable(new DERSet(v));
            }
        }

        return unauthAttrs;
    }

    private byte[] encodeObj(
        ASN1Encodable obj)
        throws IOException
    {
        if (obj != null)
        {
            return obj.toASN1Primitive().getEncoded();
        }

        return null;
    }

    /**
     * This will only be valid after the content has been read.
     *
     * @return the contents of the messageDigest attribute, if available. Null if not present.
     */
    public byte[] getContentDigest()
    {
        if (authAttrs != null)
        {
            return ASN1OctetString.getInstance(authAttrs.get(CMSAttributes.messageDigest).getAttrValues().getObjectAt(0)).getOctets();
        }

        return null;
    }
}
