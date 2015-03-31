package org.spongycastle.cms;

import java.io.IOException;
import java.io.InputStream;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.ASN1OctetString;
import org.spongycastle.asn1.cms.CompressedData;
import org.spongycastle.asn1.cms.ContentInfo;
import org.spongycastle.operator.InputExpander;
import org.spongycastle.operator.InputExpanderProvider;

/**
 * containing class for an CMS Compressed Data object
 * <pre>
 *     CMSCompressedData cd = new CMSCompressedData(inputStream);
 *
 *     process(cd.getContent(new ZlibExpanderProvider()));
 * </pre>
 */
public class CMSCompressedData
{
    ContentInfo                 contentInfo;
    CompressedData              comData;

    public CMSCompressedData(
        byte[]    compressedData) 
        throws CMSException
    {
        this(CMSUtils.readContentInfo(compressedData));
    }

    public CMSCompressedData(
        InputStream    compressedData) 
        throws CMSException
    {
        this(CMSUtils.readContentInfo(compressedData));
    }

    public CMSCompressedData(
        ContentInfo contentInfo)
        throws CMSException
    {
        this.contentInfo = contentInfo;

        try
        {
            this.comData = CompressedData.getInstance(contentInfo.getContent());
        }
        catch (ClassCastException e)
        {
            throw new CMSException("Malformed content.", e);
        }
        catch (IllegalArgumentException e)
        {
            throw new CMSException("Malformed content.", e);
        }
    }

    public ASN1ObjectIdentifier getContentType()
    {
        return contentInfo.getContentType();
    }

    /**
     * Return the uncompressed content.
     *
     * @param expanderProvider a provider of expander algorithm implementations.
     * @return the uncompressed content
     * @throws CMSException if there is an exception un-compressing the data.
     */
    public byte[] getContent(InputExpanderProvider expanderProvider)
        throws CMSException
    {
        ContentInfo     content = comData.getEncapContentInfo();

        ASN1OctetString bytes = (ASN1OctetString)content.getContent();
        InputExpander   expander = expanderProvider.get(comData.getCompressionAlgorithmIdentifier());
        InputStream     zIn = expander.getInputStream(bytes.getOctetStream());

        try
        {
            return CMSUtils.streamToByteArray(zIn);
        }
        catch (IOException e)
        {
            throw new CMSException("exception reading compressed stream.", e);
        }
    }

    /**
     * return the ContentInfo
     */
    public ContentInfo toASN1Structure()
    {
        return contentInfo;
    }
    
    /**
     * return the ASN.1 encoded representation of this object.
     */
    public byte[] getEncoded()
        throws IOException
    {
        return contentInfo.getEncoded();
    }
}
