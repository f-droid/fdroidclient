package org.spongycastle.tsp;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.spongycastle.asn1.ASN1EncodableVector;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.DERBitString;
import org.spongycastle.asn1.DERInteger;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.asn1.DERUTF8String;
import org.spongycastle.asn1.cmp.PKIFailureInfo;
import org.spongycastle.asn1.cmp.PKIFreeText;
import org.spongycastle.asn1.cmp.PKIStatus;
import org.spongycastle.asn1.cmp.PKIStatusInfo;
import org.spongycastle.asn1.cms.ContentInfo;
import org.spongycastle.asn1.tsp.TimeStampResp;

/**
 * Generator for RFC 3161 Time Stamp Responses.
 * <p>
 * New generate methods have been introduced to give people more control over what ends up in the message.
 * Unfortunately it turns out that in some cases fields like statusString must be left out otherwise a an
 * otherwise valid timestamp will be rejected.
 * </p>
 * If you're after the most control with generating a response use:
 * <pre>
 *    TimeStampResponse tsResp;
 *
 *    try
 *    {
 *       tsResp = tsRespGen.generateGrantedResponse(request, new BigInteger("23"), new Date());
 *    }
 *    catch (Exception e)
 *    {
 *        tsResp = tsRespGen.generateRejectedResponse(e);
 *    }
 * </pre>
 * The generate method does this, but provides a status string of "Operation Okay".
 * <p>
 * It should be pointed out that generateRejectedResponse() may also, on very rare occasions throw a TSPException.
 * In the event that happens, there's a serious internal problem with your responder.
 * </p>
 */
public class TimeStampResponseGenerator
{
    int status;

    ASN1EncodableVector statusStrings;

    int failInfo;
    private TimeStampTokenGenerator tokenGenerator;
    private Set                     acceptedAlgorithms;
    private Set                     acceptedPolicies;
    private Set                     acceptedExtensions;

    /**
     *
     * @param tokenGenerator
     * @param acceptedAlgorithms a set of OIDs giving accepted algorithms.
     */
    public TimeStampResponseGenerator(
        TimeStampTokenGenerator tokenGenerator,
        Set                     acceptedAlgorithms)
    {
        this(tokenGenerator, acceptedAlgorithms, null, null);
    }

    /**
     *
     * @param tokenGenerator
     * @param acceptedAlgorithms a set of OIDs giving accepted algorithms.
     * @param acceptedPolicies if non-null a set of policies OIDs we are willing to sign under.
     */
    public TimeStampResponseGenerator(
        TimeStampTokenGenerator tokenGenerator,
        Set                     acceptedAlgorithms,
        Set                     acceptedPolicies)
    {
        this(tokenGenerator, acceptedAlgorithms, acceptedPolicies, null);
    }

    /**
     *
     * @param tokenGenerator
     * @param acceptedAlgorithms a set of OIDs giving accepted algorithms.
     * @param acceptedPolicies if non-null a set of policies OIDs we are willing to sign under.
     * @param acceptedExtensions if non-null a set of extensions OIDs we are willing to accept.
     */
    public TimeStampResponseGenerator(
        TimeStampTokenGenerator tokenGenerator,
        Set                     acceptedAlgorithms,
        Set                     acceptedPolicies,
        Set                     acceptedExtensions)
    {
        this.tokenGenerator = tokenGenerator;
        this.acceptedAlgorithms = convert(acceptedAlgorithms);
        this.acceptedPolicies = convert(acceptedPolicies);
        this.acceptedExtensions = convert(acceptedExtensions);

        statusStrings = new ASN1EncodableVector();
    }

    private void addStatusString(String statusString)
    {
        statusStrings.add(new DERUTF8String(statusString));
    }

    private void setFailInfoField(int field)
    {
        failInfo = failInfo | field;
    }

    private PKIStatusInfo getPKIStatusInfo()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();
        
        v.add(new DERInteger(status));
        
        if (statusStrings.size() > 0)
        {
            v.add(PKIFreeText.getInstance(new DERSequence(statusStrings)));
        }

        if (failInfo != 0)
        {
            DERBitString failInfoBitString = new FailInfo(failInfo);
            v.add(failInfoBitString);
        }

        return PKIStatusInfo.getInstance(new DERSequence(v));
    }

    /**
     * Return an appropriate TimeStampResponse.
     * <p>
     * If genTime is null a timeNotAvailable error response will be returned. Calling generate() is the
     * equivalent of:
     * <pre>
     *    TimeStampResponse tsResp;
     *
     *    try
     *    {
     *       tsResp = tsRespGen.generateGrantedResponse(request, serialNumber, genTime, "Operation Okay");
     *    }
     *    catch (Exception e)
     *    {
     *        tsResp = tsRespGen.generateRejectedResponse(e);
     *    }
     * </pre>
     * @param request the request this response is for.
     * @param serialNumber serial number for the response token.
     * @param genTime generation time for the response token.
     * @return a TimeStampResponse.
     * @throws TSPException
     */
    public TimeStampResponse generate(
        TimeStampRequest    request,
        BigInteger          serialNumber,
        Date                genTime)
        throws TSPException
    {
        try
        {
            return this.generateGrantedResponse(request, serialNumber, genTime, "Operation Okay");
        }
        catch (Exception e)
        {
            return this.generateRejectedResponse(e);
        }
    }

    /**
     * Return a granted response, if the passed in request passes validation.
     * <p>
     * If genTime is null a timeNotAvailable or a validation exception occurs a TSPValidationException will
     * be thrown. The parent TSPException will only occur on some sort of system failure.
     * </p>
     * @param request the request this response is for.
     * @param serialNumber serial number for the response token.
     * @param genTime generation time for the response token.
     * @return  the TimeStampResponse with a status of  PKIStatus.GRANTED
     * @throws TSPException on validation exception or internal error.
     */
    public TimeStampResponse generateGrantedResponse(
        TimeStampRequest    request,
        BigInteger          serialNumber,
        Date                genTime)
        throws TSPException
    {
        return generateGrantedResponse(request, serialNumber, genTime, null);
    }

    /**
     * Return a granted response, if the passed in request passes validation with the passed in status string.
     * <p>
     * If genTime is null a timeNotAvailable or a validation exception occurs a TSPValidationException will
     * be thrown. The parent TSPException will only occur on some sort of system failure.
     * </p>
     * @param request the request this response is for.
     * @param serialNumber serial number for the response token.
     * @param genTime generation time for the response token.
     * @return  the TimeStampResponse with a status of  PKIStatus.GRANTED
     * @throws TSPException on validation exception or internal error.
     */
    public TimeStampResponse generateGrantedResponse(
        TimeStampRequest    request,
        BigInteger          serialNumber,
        Date                genTime,
        String              statusString)
        throws TSPException
    {
        if (genTime == null)
        {
            throw new TSPValidationException("The time source is not available.", PKIFailureInfo.timeNotAvailable);
        }

        request.validate(acceptedAlgorithms, acceptedPolicies, acceptedExtensions);

        status = PKIStatus.GRANTED;
        statusStrings = new ASN1EncodableVector();

        if (statusString != null)
        {
            this.addStatusString(statusString);
        }

        PKIStatusInfo pkiStatusInfo = getPKIStatusInfo();

        ContentInfo tstTokenContentInfo;
        try
        {
            tstTokenContentInfo = tokenGenerator.generate(request, serialNumber, genTime).toCMSSignedData().toASN1Structure();
        }
        catch (TSPException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new TSPException(
                    "Timestamp token received cannot be converted to ContentInfo", e);
        }

        TimeStampResp resp = new TimeStampResp(pkiStatusInfo, tstTokenContentInfo);

        try
        {
            return new TimeStampResponse(resp);
        }
        catch (IOException e)
        {
            throw new TSPException("created badly formatted response!");
        }
    }

    /**
     * Generate a generic rejection response based on a TSPValidationException or
     * an Exception. Exceptions which are not an instance of TSPValidationException
     * will be treated as systemFailure. The return value of exception.getMessage() will
     * be used as the status string for the response.
     *
     * @param exception the exception thrown on validating the request.
     * @return a TimeStampResponse.
     * @throws TSPException if a failure response cannot be generated.
     */
    public TimeStampResponse generateRejectedResponse(Exception exception)
        throws TSPException
    {
        if (exception instanceof TSPValidationException)
        {
            return generateFailResponse(PKIStatus.REJECTION, ((TSPValidationException)exception).getFailureCode(), exception.getMessage());
        }
        else
        {
            return generateFailResponse(PKIStatus.REJECTION, PKIFailureInfo.systemFailure, exception.getMessage());
        }
    }

    /**
     * Generate a non-granted TimeStampResponse with chosen status and FailInfoField.
     * 
     * @param status the PKIStatus to set.
     * @param failInfoField the FailInfoField to set.
     * @param statusString an optional string describing the failure.
     * @return a TimeStampResponse with a failInfoField and optional statusString
     * @throws TSPException in case the response could not be created
     */
    public TimeStampResponse generateFailResponse(int status, int failInfoField, String statusString)
        throws TSPException
    {
        this.status = status;
        this.statusStrings = new ASN1EncodableVector();

        this.setFailInfoField(failInfoField);

        if (statusString != null)
        {
            this.addStatusString(statusString);
        }

        PKIStatusInfo pkiStatusInfo = getPKIStatusInfo();

        TimeStampResp resp = new TimeStampResp(pkiStatusInfo, null);

        try
        {
            return new TimeStampResponse(resp);
        }
        catch (IOException e)
        {
            throw new TSPException("created badly formatted response!");
        }
    }

    private Set convert(Set orig)
    {
        if (orig == null)
        {
            return orig;
        }

        Set con = new HashSet(orig.size());

        for (Iterator it = orig.iterator(); it.hasNext();)
        {
            Object o = it.next();

            if (o instanceof String)
            {
                con.add(new ASN1ObjectIdentifier((String)o));
            }
            else
            {
                con.add(o);
            }
        }

        return con;
    }

    class FailInfo extends DERBitString
    {
        FailInfo(int failInfoValue)
        {
            super(getBytes(failInfoValue), getPadBits(failInfoValue));
        }
    }
}
