package org.spongycastle.cms;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1Encoding;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.util.io.Streams;

public abstract class RecipientInformation
{
    protected RecipientId rid;
    protected AlgorithmIdentifier   keyEncAlg;
    protected AlgorithmIdentifier messageAlgorithm;
    protected CMSSecureReadable     secureReadable;

    private AuthAttributesProvider additionalData;

    private byte[] resultMac;
    private RecipientOperator     operator;

    RecipientInformation(
        AlgorithmIdentifier     keyEncAlg,
        AlgorithmIdentifier     messageAlgorithm,
        CMSSecureReadable       secureReadable,
        AuthAttributesProvider  additionalData)
    {
        this.keyEncAlg = keyEncAlg;
        this.messageAlgorithm = messageAlgorithm;
        this.secureReadable = secureReadable;
        this.additionalData = additionalData;
    }

    public RecipientId getRID()
    {
        return rid;
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
     * Return the key encryption algorithm details for the key in this recipient.
     *
     * @return AlgorithmIdentifier representing the key encryption algorithm.
     */
    public AlgorithmIdentifier getKeyEncryptionAlgorithm()
    {
        return keyEncAlg;
    }

    /**
     * return the object identifier for the key encryption algorithm.
     *
     * @return OID for key encryption algorithm.
     */
    public String getKeyEncryptionAlgOID()
    {
        return keyEncAlg.getAlgorithm().getId();
    }

    /**
     * return the ASN.1 encoded key encryption algorithm parameters, or null if
     * there aren't any.
     *
     * @return ASN.1 encoding of key encryption algorithm parameters.
     */
    public byte[] getKeyEncryptionAlgParams()
    {
        try
        {
            return encodeObj(keyEncAlg.getParameters());
        }
        catch (Exception e)
        {
            throw new RuntimeException("exception getting encryption parameters " + e);
        }
    }

    /**
     * Return the content digest calculated during the read of the content if one has been generated. This will
     * only happen if we are dealing with authenticated data and authenticated attributes are present.
     *
     * @return byte array containing the digest.
     */
    public byte[] getContentDigest()
    {
        if (secureReadable instanceof CMSEnvelopedHelper.CMSDigestAuthenticatedSecureReadable)
        {
            return ((CMSEnvelopedHelper.CMSDigestAuthenticatedSecureReadable)secureReadable).getDigest();
        }

        return null;
    }

    /**
     * Return the MAC calculated for the recipient. Note: this call is only meaningful once all
     * the content has been read.
     *
     * @return  byte array containing the mac.
     */
    public byte[] getMac()
    {
        if (resultMac == null)
        {
            if (operator.isMacBased())
            {
                if (additionalData != null)
                {
                    try
                    {
                        Streams.drain(operator.getInputStream(new ByteArrayInputStream(additionalData.getAuthAttributes().getEncoded(ASN1Encoding.DER))));
                    }
                    catch (IOException e)
                    {
                        throw new IllegalStateException("unable to drain input: " + e.getMessage());
                    }
                }
                resultMac = operator.getMac();
            }
        }

        return resultMac;
    }

    /**
     * Return the decrypted/encapsulated content in the EnvelopedData after recovering the content
     * encryption/MAC key using the passed in Recipient.
     *
     * @param recipient recipient object to use to recover content encryption key
     * @return  the content inside the EnvelopedData this RecipientInformation is associated with.
     * @throws CMSException if the content-encryption/MAC key cannot be recovered.
     */
    public byte[] getContent(
        Recipient recipient)
        throws CMSException
    {
        try
        {
            return CMSUtils.streamToByteArray(getContentStream(recipient).getContentStream());
        }
        catch (IOException e)
        {
            throw new CMSException("unable to parse internal stream: " + e.getMessage(), e);
        }
    }

    /**
     * Return a CMSTypedStream representing the content in the EnvelopedData after recovering the content
     * encryption/MAC key using the passed in Recipient.
     *
     * @param recipient recipient object to use to recover content encryption key
     * @return  the content inside the EnvelopedData this RecipientInformation is associated with.
     * @throws CMSException if the content-encryption/MAC key cannot be recovered.
     */
    public CMSTypedStream getContentStream(Recipient recipient)
        throws CMSException, IOException
    {
        operator = getRecipientOperator(recipient);

        if (additionalData != null)
        {
            return new CMSTypedStream(secureReadable.getInputStream());
        }

        return new CMSTypedStream(operator.getInputStream(secureReadable.getInputStream()));
    }

    protected abstract RecipientOperator getRecipientOperator(Recipient recipient)
        throws CMSException, IOException;
}
