package org.spongycastle.cms;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;

import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1Set;
import org.spongycastle.asn1.BERSequenceGenerator;
import org.spongycastle.asn1.BERSet;
import org.spongycastle.asn1.DERSet;
import org.spongycastle.asn1.DERTaggedObject;
import org.spongycastle.asn1.cms.AttributeTable;
import org.spongycastle.asn1.cms.CMSObjectIdentifiers;
import org.spongycastle.asn1.cms.EnvelopedData;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.operator.GenericKey;
import org.spongycastle.operator.OutputEncryptor;

/**
 * General class for generating a CMS enveloped-data message stream.
 * <p>
 * A simple example of usage.
 * <pre>
 *      CMSEnvelopedDataStreamGenerator edGen = new CMSEnvelopedDataStreamGenerator();
 *
 *      edGen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(recipientCert).setProvider("SC"));
 *
 *      ByteArrayOutputStream  bOut = new ByteArrayOutputStream();
 *      
 *      OutputStream out = edGen.open(
 *                              bOut, new JceCMSContentEncryptorBuilder(CMSAlgorithm.DES_EDE3_CBC)
 *                                              .setProvider("SC").build());
 *      out.write(data);
 *      
 *      out.close();
 * </pre>
 */
public class CMSEnvelopedDataStreamGenerator
    extends CMSEnvelopedGenerator
{
    private ASN1Set              _unprotectedAttributes = null;
    private int                 _bufferSize;
    private boolean             _berEncodeRecipientSet;

    /**
     * base constructor
     */
    public CMSEnvelopedDataStreamGenerator()
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
        _bufferSize = bufferSize;
    }

    /**
     * Use a BER Set to store the recipient information
     */
    public void setBEREncodeRecipients(
        boolean berEncodeRecipientSet)
    {
        _berEncodeRecipientSet = berEncodeRecipientSet;
    }

    private ASN1Integer getVersion()
    {
        if (originatorInfo != null || _unprotectedAttributes != null)
        {
            return new ASN1Integer(2);
        }
        else
        {
            return new ASN1Integer(0);
        }
    }

    private OutputStream doOpen(
        ASN1ObjectIdentifier dataType,
        OutputStream         out,
        OutputEncryptor      encryptor)
        throws IOException, CMSException
    {
        ASN1EncodableVector recipientInfos = new ASN1EncodableVector();
        GenericKey encKey = encryptor.getKey();
        Iterator it = recipientInfoGenerators.iterator();

        while (it.hasNext())
        {
            RecipientInfoGenerator recipient = (RecipientInfoGenerator)it.next();

            recipientInfos.add(recipient.generate(encKey));
        }

        return open(dataType, out, recipientInfos, encryptor);
    }

    protected OutputStream open(
        ASN1ObjectIdentifier dataType,
        OutputStream         out,
        ASN1EncodableVector  recipientInfos,
        OutputEncryptor      encryptor)
        throws IOException
    {
        //
        // ContentInfo
        //
        BERSequenceGenerator cGen = new BERSequenceGenerator(out);

        cGen.addObject(CMSObjectIdentifiers.envelopedData);

        //
        // Encrypted Data
        //
        BERSequenceGenerator envGen = new BERSequenceGenerator(cGen.getRawOutputStream(), 0, true);

        envGen.addObject(getVersion());

        if (originatorInfo != null)
        {
            envGen.addObject(new DERTaggedObject(false, 0, originatorInfo));
        }

        if (_berEncodeRecipientSet)
        {
            envGen.getRawOutputStream().write(new BERSet(recipientInfos).getEncoded());
        }
        else
        {
            envGen.getRawOutputStream().write(new DERSet(recipientInfos).getEncoded());
        }

        BERSequenceGenerator eiGen = new BERSequenceGenerator(envGen.getRawOutputStream());

        eiGen.addObject(dataType);

        AlgorithmIdentifier encAlgId = encryptor.getAlgorithmIdentifier();

        eiGen.getRawOutputStream().write(encAlgId.getEncoded());

        OutputStream octetStream = CMSUtils.createBEROctetOutputStream(
            eiGen.getRawOutputStream(), 0, false, _bufferSize);

        OutputStream cOut = encryptor.getOutputStream(octetStream);

        return new CmsEnvelopedDataOutputStream(cOut, cGen, envGen, eiGen);
    }

    protected OutputStream open(
        OutputStream        out,
        ASN1EncodableVector recipientInfos,
        OutputEncryptor     encryptor)
        throws CMSException
    {
        try
        {
            //
            // ContentInfo
            //
            BERSequenceGenerator cGen = new BERSequenceGenerator(out);

            cGen.addObject(CMSObjectIdentifiers.envelopedData);

            //
            // Encrypted Data
            //
            BERSequenceGenerator envGen = new BERSequenceGenerator(cGen.getRawOutputStream(), 0, true);

            ASN1Set recipients;
            if (_berEncodeRecipientSet)
            {
                recipients = new BERSet(recipientInfos);
            }
            else
            {
                recipients = new DERSet(recipientInfos);
            }

            envGen.addObject(new ASN1Integer(EnvelopedData.calculateVersion(originatorInfo, recipients, _unprotectedAttributes)));

            if (originatorInfo != null)
            {
                envGen.addObject(new DERTaggedObject(false, 0, originatorInfo));
            }

            envGen.getRawOutputStream().write(recipients.getEncoded());

            BERSequenceGenerator eiGen = new BERSequenceGenerator(envGen.getRawOutputStream());

            eiGen.addObject(CMSObjectIdentifiers.data);

            AlgorithmIdentifier encAlgId = encryptor.getAlgorithmIdentifier();

            eiGen.getRawOutputStream().write(encAlgId.getEncoded());

            OutputStream octetStream = CMSUtils.createBEROctetOutputStream(
                eiGen.getRawOutputStream(), 0, false, _bufferSize);

            return new CmsEnvelopedDataOutputStream(encryptor.getOutputStream(octetStream), cGen, envGen, eiGen);
        }
        catch (IOException e)
        {
            throw new CMSException("exception decoding algorithm parameters.", e);
        }
    }

    /**
     * generate an enveloped object that contains an CMS Enveloped Data
     * object using the given encryptor.
     */
    public OutputStream open(
        OutputStream    out,
        OutputEncryptor encryptor)
        throws CMSException, IOException
    {
        return doOpen(new ASN1ObjectIdentifier(CMSObjectIdentifiers.data.getId()), out, encryptor);
    }

    /**
     * generate an enveloped object that contains an CMS Enveloped Data
     * object using the given encryptor and marking the data as being of the passed
     * in type.
     */
    public OutputStream open(
        ASN1ObjectIdentifier dataType,
        OutputStream         out,
        OutputEncryptor      encryptor)
        throws CMSException, IOException
    {
        return doOpen(dataType, out, encryptor);
    }

    private class CmsEnvelopedDataOutputStream
        extends OutputStream
    {
        private OutputStream   _out;
        private BERSequenceGenerator _cGen;
        private BERSequenceGenerator _envGen;
        private BERSequenceGenerator _eiGen;
    
        public CmsEnvelopedDataOutputStream(
            OutputStream   out,
            BERSequenceGenerator cGen,
            BERSequenceGenerator envGen,
            BERSequenceGenerator eiGen)
        {
            _out = out;
            _cGen = cGen;
            _envGen = envGen;
            _eiGen = eiGen;
        }
    
        public void write(
            int b)
            throws IOException
        {
            _out.write(b);
        }
        
        public void write(
            byte[] bytes,
            int    off,
            int    len)
            throws IOException
        {
            _out.write(bytes, off, len);
        }
        
        public void write(
            byte[] bytes)
            throws IOException
        {
            _out.write(bytes);
        }
        
        public void close()
            throws IOException
        {
            _out.close();
            _eiGen.close();

            if (unprotectedAttributeGenerator != null)
            {
                AttributeTable attrTable = unprotectedAttributeGenerator.getAttributes(new HashMap());
      
                ASN1Set unprotectedAttrs = new BERSet(attrTable.toASN1EncodableVector());

                _envGen.addObject(new DERTaggedObject(false, 1, unprotectedAttrs));
            }
    
            _envGen.close();
            _cGen.close();
        }
    }
}
