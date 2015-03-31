package org.spongycastle.tsp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.DERObjectIdentifier;
import org.spongycastle.asn1.cmp.PKIFailureInfo;
import org.spongycastle.asn1.tsp.TimeStampReq;
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.asn1.x509.Extensions;

/**
 * Base class for an RFC 3161 Time Stamp Request.
 */
public class TimeStampRequest
{
    private static Set EMPTY_SET = Collections.unmodifiableSet(new HashSet());

    private TimeStampReq req;
    private Extensions extensions;

    public TimeStampRequest(TimeStampReq req)
    {
        this.req = req;
        this.extensions = req.getExtensions();
    }

    /**
     * Create a TimeStampRequest from the past in byte array.
     * 
     * @param req byte array containing the request.
     * @throws IOException if the request is malformed.
     */
    public TimeStampRequest(byte[] req) 
        throws IOException
    {
        this(new ByteArrayInputStream(req));
    }

    /**
     * Create a TimeStampRequest from the past in input stream.
     * 
     * @param in input stream containing the request.
     * @throws IOException if the request is malformed.
     */
    public TimeStampRequest(InputStream in) 
        throws IOException
    {
        this(loadRequest(in));
    }

    private static TimeStampReq loadRequest(InputStream in)
        throws IOException
    {
        try
        {
            return TimeStampReq.getInstance(new ASN1InputStream(in).readObject());
        }
        catch (ClassCastException e)
        {
            throw new IOException("malformed request: " + e);
        }
        catch (IllegalArgumentException e)
        {
            throw new IOException("malformed request: " + e);
        }
    }

    public int getVersion()
    {
        return req.getVersion().getValue().intValue();
    }

    public ASN1ObjectIdentifier getMessageImprintAlgOID()
    {
        return req.getMessageImprint().getHashAlgorithm().getAlgorithm();
    }

    public byte[] getMessageImprintDigest()
    {
        return req.getMessageImprint().getHashedMessage();
    }

    public ASN1ObjectIdentifier getReqPolicy()
    {
        if (req.getReqPolicy() != null)
        {
            return req.getReqPolicy();
        }
        else
        {
            return null;
        }
    }

    public BigInteger getNonce()
    {
        if (req.getNonce() != null)
        {
            return req.getNonce().getValue();
        }
        else
        {
            return null;
        }
    }

    public boolean getCertReq()
    {
        if (req.getCertReq() != null)
        {
            return req.getCertReq().isTrue();
        }
        else
        {
            return false;
        }
    }

    /**
     * Validate the timestamp request, checking the digest to see if it is of an
     * accepted type and whether it is of the correct length for the algorithm specified.
     *
     * @param algorithms a set of OIDs giving accepted algorithms.
     * @param policies if non-null a set of policies OIDs we are willing to sign under.
     * @param extensions if non-null a set of extensions OIDs we are willing to accept.
     * @throws TSPException if the request is invalid, or processing fails.
     */
    public void validate(
        Set    algorithms,
        Set    policies,
        Set    extensions)
        throws TSPException
    {
        algorithms = convert(algorithms);
        policies = convert(policies);
        extensions = convert(extensions);

        if (!algorithms.contains(this.getMessageImprintAlgOID()))
        {
            throw new TSPValidationException("request contains unknown algorithm.", PKIFailureInfo.badAlg);
        }

        if (policies != null && this.getReqPolicy() != null && !policies.contains(this.getReqPolicy()))
        {
            throw new TSPValidationException("request contains unknown policy.", PKIFailureInfo.unacceptedPolicy);
        }

        if (this.getExtensions() != null && extensions != null)
        {
            Enumeration en = this.getExtensions().oids();
            while(en.hasMoreElements())
            {
                String  oid = ((DERObjectIdentifier)en.nextElement()).getId();
                if (!extensions.contains(oid))
                {
                    throw new TSPValidationException("request contains unknown extension.", PKIFailureInfo.unacceptedExtension);
                }
            }
        }

        int digestLength = TSPUtil.getDigestLength(this.getMessageImprintAlgOID().getId());

        if (digestLength != this.getMessageImprintDigest().length)
        {
            throw new TSPValidationException("imprint digest the wrong length.", PKIFailureInfo.badDataFormat);
        }
    }

   /**
    * return the ASN.1 encoded representation of this object.
    * @return the default ASN,1 byte encoding for the object.
    */
    public byte[] getEncoded() throws IOException
    {
        return req.getEncoded();
    }

    Extensions getExtensions()
    {
        return extensions;
    }

    public boolean hasExtensions()
    {
        return extensions != null;
    }

    public Extension getExtension(ASN1ObjectIdentifier oid)
    {
        if (extensions != null)
        {
            return extensions.getExtension(oid);
        }

        return null;
    }

    public List getExtensionOIDs()
    {
        return TSPUtil.getExtensionOIDs(extensions);
    }

    /**
     * Returns a set of ASN1ObjectIdentifiers giving the non-critical extensions.
     * @return a set of ASN1ObjectIdentifiers.
     */
    public Set getNonCriticalExtensionOIDs()
    {
        if (extensions == null)
        {
            return EMPTY_SET;
        }

        return Collections.unmodifiableSet(new HashSet(Arrays.asList(extensions.getNonCriticalExtensionOIDs())));
    }

    /**
     * Returns a set of ASN1ObjectIdentifiers giving the critical extensions.
     * @return a set of ASN1ObjectIdentifiers.
     */
    public Set getCriticalExtensionOIDs()
    {
        if (extensions == null)
        {
            return EMPTY_SET;
        }

        return Collections.unmodifiableSet(new HashSet(Arrays.asList(extensions.getCriticalExtensionOIDs())));
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
}
