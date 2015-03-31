package org.spongycastle.ocsp;

import java.io.IOException;
import java.io.InputStream;

import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.ocsp.BasicOCSPResponse;
import org.spongycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.spongycastle.asn1.ocsp.OCSPResponse;
import org.spongycastle.asn1.ocsp.ResponseBytes;

/**
 * @deprecated use classes in org.spongycastle.cert.ocsp.
 */
public class OCSPResp
{
    private OCSPResponse    resp;

    /**
     * @deprecated use classes in org.spongycastle.cert.ocsp.
     */
    public OCSPResp(
        OCSPResponse    resp)
    {
        this.resp = resp;
    }

    /**
     * @deprecated use classes in org.spongycastle.cert.ocsp.
     */
    public OCSPResp(
        byte[]          resp)
        throws IOException
    {
        this(new ASN1InputStream(resp));
    }

    /**
     * @deprecated use classes in org.spongycastle.cert.ocsp.
     */
    public OCSPResp(
        InputStream     in)
        throws IOException
    {
        this(new ASN1InputStream(in));
    }

    private OCSPResp(
        ASN1InputStream aIn)
        throws IOException
    {
        try
        {
            this.resp = OCSPResponse.getInstance(aIn.readObject());
        }
        catch (IllegalArgumentException e)
        {
            throw new IOException("malformed response: " + e.getMessage());
        }
        catch (ClassCastException e)
        {
            throw new IOException("malformed response: " + e.getMessage());
        }
    }

    public int getStatus()
    {
        return this.resp.getResponseStatus().getValue().intValue();
    }

    public Object getResponseObject()
        throws OCSPException
    {
        ResponseBytes   rb = this.resp.getResponseBytes();

        if (rb == null)
        {
            return null;
        }

        if (rb.getResponseType().equals(OCSPObjectIdentifiers.id_pkix_ocsp_basic))
        {
            try
            {
                ASN1Primitive obj = ASN1Primitive.fromByteArray(rb.getResponse().getOctets());
                return new BasicOCSPResp(BasicOCSPResponse.getInstance(obj));
            }
            catch (Exception e)
            {
                throw new OCSPException("problem decoding object: " + e, e);
            }
        }

        return rb.getResponse();
    }

    /**
     * return the ASN.1 encoded representation of this object.
     */
    public byte[] getEncoded()
        throws IOException
    {
        return resp.getEncoded();
    }
    
    public boolean equals(Object o)
    {
        if (o == this)
        {
            return true;
        }
        
        if (!(o instanceof OCSPResp))
        {
            return false;
        }
        
        OCSPResp r = (OCSPResp)o;
        
        return resp.equals(r.resp);
    }
    
    public int hashCode()
    {
        return resp.hashCode();
    }
}
