package org.spongycastle.tsp.cms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.MalformedURLException;

import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.DERIA5String;
import org.spongycastle.asn1.cms.AttributeTable;
import org.spongycastle.asn1.cms.CMSObjectIdentifiers;
import org.spongycastle.asn1.cms.ContentInfo;
import org.spongycastle.asn1.cms.Evidence;
import org.spongycastle.asn1.cms.TimeStampAndCRL;
import org.spongycastle.asn1.cms.TimeStampTokenEvidence;
import org.spongycastle.asn1.cms.TimeStampedData;
import org.spongycastle.cms.CMSException;
import org.spongycastle.operator.DigestCalculator;
import org.spongycastle.operator.DigestCalculatorProvider;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.tsp.TimeStampToken;

public class CMSTimeStampedData
{
    private TimeStampedData timeStampedData;
    private ContentInfo contentInfo;
    private TimeStampDataUtil util;

    public CMSTimeStampedData(ContentInfo contentInfo)
    {
        this.initialize(contentInfo);
    }

    public CMSTimeStampedData(InputStream in)
        throws IOException
    {
        try
        {
            initialize(ContentInfo.getInstance(new ASN1InputStream(in).readObject()));
        }
        catch (ClassCastException e)
        {
            throw new IOException("Malformed content: " + e);
        }
        catch (IllegalArgumentException e)
        {
            throw new IOException("Malformed content: " + e);
        }
    }

    public CMSTimeStampedData(byte[] baseData)
        throws IOException
    {
        this(new ByteArrayInputStream(baseData));
    }

    private void initialize(ContentInfo contentInfo)
    {
        this.contentInfo = contentInfo;

        if (CMSObjectIdentifiers.timestampedData.equals(contentInfo.getContentType()))
        {
            this.timeStampedData = TimeStampedData.getInstance(contentInfo.getContent());
        }
        else
        {
            throw new IllegalArgumentException("Malformed content - type must be " + CMSObjectIdentifiers.timestampedData.getId());
        }

        util = new TimeStampDataUtil(this.timeStampedData);
    }

    public byte[] calculateNextHash(DigestCalculator calculator)
        throws CMSException
    {
        return util.calculateNextHash(calculator);
    }

    /**
     * Return a new timeStampedData object with the additional token attached.
     *
     * @throws CMSException
     */
    public CMSTimeStampedData addTimeStamp(TimeStampToken token)
        throws CMSException
    {
        TimeStampAndCRL[] timeStamps = util.getTimeStamps();
        TimeStampAndCRL[] newTimeStamps = new TimeStampAndCRL[timeStamps.length + 1];

        System.arraycopy(timeStamps, 0, newTimeStamps, 0, timeStamps.length);

        newTimeStamps[timeStamps.length] = new TimeStampAndCRL(token.toCMSSignedData().toASN1Structure());

        return new CMSTimeStampedData(new ContentInfo(CMSObjectIdentifiers.timestampedData, new TimeStampedData(timeStampedData.getDataUri(), timeStampedData.getMetaData(), timeStampedData.getContent(), new Evidence(new TimeStampTokenEvidence(newTimeStamps)))));
    }

    public byte[] getContent()
    {
        if (timeStampedData.getContent() != null)
        {
            return timeStampedData.getContent().getOctets();
        }

        return null;
    }

    public URL getDataUri()
        throws MalformedURLException
    {
        DERIA5String dataURI = this.timeStampedData.getDataUri();

        if (dataURI != null)
        {
            return new URL(dataURI.getString());
        }

        return null;
    }

    public String getFileName()
    {
        return util.getFileName();
    }

    public String getMediaType()
    {
        return util.getMediaType();
    }

    public AttributeTable getOtherMetaData()
    {
        return util.getOtherMetaData();
    }

    public TimeStampToken[] getTimeStampTokens()
        throws CMSException
    {
        return util.getTimeStampTokens();
    }

    /**
     * Initialise the passed in calculator with the MetaData for this message, if it is
     * required as part of the initial message imprint calculation.
     *
     * @param calculator the digest calculator to be initialised.
     * @throws CMSException if the MetaData is required and cannot be processed
     */
    public void initialiseMessageImprintDigestCalculator(DigestCalculator calculator)
        throws CMSException
    {
        util.initialiseMessageImprintDigestCalculator(calculator);
    }

    /**
     * Returns an appropriately initialised digest calculator based on the message imprint algorithm
     * described in the first time stamp in the TemporalData for this message. If the metadata is required
     * to be included in the digest calculation, the returned calculator will be pre-initialised.
     *
     * @param calculatorProvider  a provider of DigestCalculator objects.
     * @return an initialised digest calculator.
     * @throws OperatorCreationException if the provider is unable to create the calculator.
     */
    public DigestCalculator getMessageImprintDigestCalculator(DigestCalculatorProvider calculatorProvider)
        throws OperatorCreationException
    {
        return util.getMessageImprintDigestCalculator(calculatorProvider);
    }

    /**
     * Validate the digests present in the TimeStampTokens contained in the CMSTimeStampedData.
     *
     * @param calculatorProvider provider for digest calculators
     * @param dataDigest the calculated data digest for the message
     * @throws ImprintDigestInvalidException if an imprint digest fails to compare
     * @throws CMSException  if an exception occurs processing the message.
     */
    public void validate(DigestCalculatorProvider calculatorProvider, byte[] dataDigest)
        throws ImprintDigestInvalidException, CMSException
    {
        util.validate(calculatorProvider, dataDigest);
    }

    /**
     * Validate the passed in timestamp token against the tokens and data present in the message.
     *
     * @param calculatorProvider provider for digest calculators
     * @param dataDigest the calculated data digest for the message.
     * @param timeStampToken  the timestamp token of interest.
     * @throws ImprintDigestInvalidException if the token is not present in the message, or an imprint digest fails to compare.
     * @throws CMSException if an exception occurs processing the message.
     */
    public void validate(DigestCalculatorProvider calculatorProvider, byte[] dataDigest, TimeStampToken timeStampToken)
        throws ImprintDigestInvalidException, CMSException
    {
        util.validate(calculatorProvider, dataDigest, timeStampToken);
    }

    public byte[] getEncoded()
        throws IOException
    {
        return contentInfo.getEncoded();
    }
}
