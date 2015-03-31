package org.spongycastle.cms;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1Encoding;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Set;
import org.spongycastle.asn1.BERSequenceGenerator;
import org.spongycastle.asn1.BERSet;
import org.spongycastle.asn1.DERInteger;
import org.spongycastle.asn1.DEROctetString;
import org.spongycastle.asn1.DERSet;
import org.spongycastle.asn1.DERTaggedObject;
import org.spongycastle.asn1.cms.AuthenticatedData;
import org.spongycastle.asn1.cms.CMSObjectIdentifiers;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.operator.DigestCalculator;
import org.spongycastle.operator.MacCalculator;
import org.spongycastle.util.io.TeeOutputStream;

/**
 * General class for generating a CMS authenticated-data message stream.
 * <p>
 * A simple example of usage.
 * <pre>
 *      CMSAuthenticatedDataStreamGenerator edGen = new CMSAuthenticatedDataStreamGenerator();
 *
 *      edGen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(cert).setProvider("SC"));
 *
 *      ByteArrayOutputStream  bOut = new ByteArrayOutputStream();
 *
 *      OutputStream out = edGen.open(
 *                              bOut, new JceCMSMacCalculatorBuilder(CMSAlgorithm.DES_EDE3_CBC).setProvider("SC").build());*
 *      out.write(data);
 *
 *      out.close();
 * </pre>
 */
public class CMSAuthenticatedDataStreamGenerator
    extends CMSAuthenticatedGenerator
{
    // Currently not handled
//    private Object              _originatorInfo = null;
//    private Object              _unprotectedAttributes = null;
    private int bufferSize;
    private boolean berEncodeRecipientSet;
    private MacCalculator macCalculator;

    /**
     * base constructor
     */
    public CMSAuthenticatedDataStreamGenerator()
    {
    }

    /**
     * Set the underlying string size for encapsulated data
     *
     * @param bufferSize length of octet strings to buffer the data.
     */
    public void setBufferSize(
        int bufferSize)
    {
        this.bufferSize = bufferSize;
    }

    /**
     * Use a BER Set to store the recipient information. By default recipients are
     * stored in a DER encoding.
     *
     * @param useBerEncodingForRecipients true if a BER set should be used, false if DER.
     */
    public void setBEREncodeRecipients(
        boolean useBerEncodingForRecipients)
    {
        berEncodeRecipientSet = useBerEncodingForRecipients;
    }

    /**
     * generate an authenticated data structure with the encapsulated bytes marked as DATA.
     *
     * @param out the stream to store the authenticated structure in.
     * @param macCalculator calculator for the MAC to be attached to the data.
     */
    public OutputStream open(
        OutputStream    out,
        MacCalculator   macCalculator)
        throws CMSException
    {
        return open(CMSObjectIdentifiers.data, out, macCalculator);
    }

    public OutputStream open(
        OutputStream    out,
        MacCalculator   macCalculator,
        DigestCalculator digestCalculator)
        throws CMSException
    {
        return open(CMSObjectIdentifiers.data, out, macCalculator, digestCalculator);
    }

    /**
     * generate an authenticated data structure with the encapsulated bytes marked as type dataType.
     *
     * @param dataType the type of the data been written to the object.
     * @param out the stream to store the authenticated structure in.
     * @param macCalculator calculator for the MAC to be attached to the data.
     */
    public OutputStream open(
        ASN1ObjectIdentifier dataType,
        OutputStream         out,
        MacCalculator        macCalculator)
        throws CMSException
    {
        return open(dataType, out, macCalculator, null);
    }

    /**
     * generate an authenticated data structure with the encapsulated bytes marked as type dataType.
     *
     * @param dataType the type of the data been written to the object.
     * @param out the stream to store the authenticated structure in.
     * @param macCalculator calculator for the MAC to be attached to the data.
     * @param digestCalculator calculator for computing digest of the encapsulated data.
     */
    public OutputStream open(
        ASN1ObjectIdentifier dataType,
        OutputStream         out,
        MacCalculator        macCalculator,
        DigestCalculator     digestCalculator)
        throws CMSException
    {
        this.macCalculator = macCalculator;

        try
        {
            ASN1EncodableVector recipientInfos = new ASN1EncodableVector();

            for (Iterator it = recipientInfoGenerators.iterator(); it.hasNext();)
            {
                RecipientInfoGenerator recipient = (RecipientInfoGenerator)it.next();

                recipientInfos.add(recipient.generate(macCalculator.getKey()));
            }

            //
            // ContentInfo
            //
            BERSequenceGenerator cGen = new BERSequenceGenerator(out);

            cGen.addObject(CMSObjectIdentifiers.authenticatedData);

            //
            // Authenticated Data
            //
            BERSequenceGenerator authGen = new BERSequenceGenerator(cGen.getRawOutputStream(), 0, true);

            authGen.addObject(new DERInteger(AuthenticatedData.calculateVersion(originatorInfo)));

            if (originatorInfo != null)
            {
                authGen.addObject(new DERTaggedObject(false, 0, originatorInfo));
            }

            if (berEncodeRecipientSet)
            {
                authGen.getRawOutputStream().write(new BERSet(recipientInfos).getEncoded());
            }
            else
            {
                authGen.getRawOutputStream().write(new DERSet(recipientInfos).getEncoded());
            }

            AlgorithmIdentifier macAlgId = macCalculator.getAlgorithmIdentifier();

            authGen.getRawOutputStream().write(macAlgId.getEncoded());

            if (digestCalculator != null)
            {
                authGen.addObject(new DERTaggedObject(false, 1, digestCalculator.getAlgorithmIdentifier()));
            }
            
            BERSequenceGenerator eiGen = new BERSequenceGenerator(authGen.getRawOutputStream());

            eiGen.addObject(dataType);

            OutputStream octetStream = CMSUtils.createBEROctetOutputStream(
                    eiGen.getRawOutputStream(), 0, false, bufferSize);

            OutputStream mOut;

            if (digestCalculator != null)
            {
                mOut = new TeeOutputStream(octetStream, digestCalculator.getOutputStream());
            }
            else
            {
                mOut = new TeeOutputStream(octetStream, macCalculator.getOutputStream());
            }

            return new CmsAuthenticatedDataOutputStream(macCalculator, digestCalculator, dataType, mOut, cGen, authGen, eiGen);
        }
        catch (IOException e)
        {
            throw new CMSException("exception decoding algorithm parameters.", e);
        }
    }

    private class CmsAuthenticatedDataOutputStream
        extends OutputStream
    {
        private OutputStream dataStream;
        private BERSequenceGenerator cGen;
        private BERSequenceGenerator envGen;
        private BERSequenceGenerator eiGen;
        private MacCalculator macCalculator;
        private DigestCalculator digestCalculator;
        private ASN1ObjectIdentifier contentType;

        public CmsAuthenticatedDataOutputStream(
            MacCalculator   macCalculator,
            DigestCalculator digestCalculator,
            ASN1ObjectIdentifier contentType,
            OutputStream dataStream,
            BERSequenceGenerator cGen,
            BERSequenceGenerator envGen,
            BERSequenceGenerator eiGen)
        {
            this.macCalculator = macCalculator;
            this.digestCalculator = digestCalculator;
            this.contentType = contentType;
            this.dataStream = dataStream;
            this.cGen = cGen;
            this.envGen = envGen;
            this.eiGen = eiGen;
        }

        public void write(
            int b)
            throws IOException
        {
            dataStream.write(b);
        }

        public void write(
            byte[] bytes,
            int    off,
            int    len)
            throws IOException
        {
            dataStream.write(bytes, off, len);
        }

        public void write(
            byte[] bytes)
            throws IOException
        {
            dataStream.write(bytes);
        }

        public void close()
            throws IOException
        {
            dataStream.close();
            eiGen.close();

            Map parameters;

            if (digestCalculator != null)
            {
                parameters = Collections.unmodifiableMap(getBaseParameters(contentType, digestCalculator.getAlgorithmIdentifier(), digestCalculator.getDigest()));

                if (authGen == null)
                {
                    authGen = new DefaultAuthenticatedAttributeTableGenerator();
                }
                
                ASN1Set authed = new DERSet(authGen.getAttributes(parameters).toASN1EncodableVector());

                OutputStream mOut = macCalculator.getOutputStream();

                mOut.write(authed.getEncoded(ASN1Encoding.DER));

                mOut.close();

                envGen.addObject(new DERTaggedObject(false, 2, authed));
            }
            else
            {
                parameters = Collections.unmodifiableMap(new HashMap());                
            }

            envGen.addObject(new DEROctetString(macCalculator.getMac()));

            if (unauthGen != null)
            {
                envGen.addObject(new DERTaggedObject(false, 3, new BERSet(unauthGen.getAttributes(parameters).toASN1EncodableVector())));
            }

            envGen.close();
            cGen.close();
        }
    }
}