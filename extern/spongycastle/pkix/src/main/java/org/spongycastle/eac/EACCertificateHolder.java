package org.spongycastle.eac;

import java.io.IOException;
import java.io.OutputStream;

import org.spongycastle.asn1.ASN1Encoding;
import org.spongycastle.asn1.ASN1ParsingException;
import org.spongycastle.asn1.eac.CVCertificate;
import org.spongycastle.asn1.eac.PublicKeyDataObject;
import org.spongycastle.eac.operator.EACSignatureVerifier;

public class EACCertificateHolder
{
    private CVCertificate cvCertificate;

    private static CVCertificate parseBytes(byte[] certEncoding)
        throws IOException
    {
        try
        {
            return CVCertificate.getInstance(certEncoding);
        }
        catch (ClassCastException e)
        {
            throw new EACIOException("malformed data: " + e.getMessage(), e);
        }
        catch (IllegalArgumentException e)
        {
            throw new EACIOException("malformed data: " + e.getMessage(), e);
        }
        catch (ASN1ParsingException e)
        {
            if (e.getCause() instanceof IOException)
            {
                throw (IOException)e.getCause();
            }
            else
            {
                throw new EACIOException("malformed data: " + e.getMessage(), e);
            }
        }
    }

    public EACCertificateHolder(byte[] certEncoding)
        throws IOException
    {
        this(parseBytes(certEncoding));
    }

    public EACCertificateHolder(CVCertificate cvCertificate)
    {
        this.cvCertificate = cvCertificate;
    }

    /**
     * Return the underlying ASN.1 structure for the certificate in this holder.
     *
     * @return a X509CertificateStructure object.
     */
    public CVCertificate toASN1Structure()
    {
        return cvCertificate;
    }

    public PublicKeyDataObject getPublicKeyDataObject()
    {
        return cvCertificate.getBody().getPublicKey();
    }

    public boolean isSignatureValid(EACSignatureVerifier verifier)
        throws EACException
    {
        try
        {
            OutputStream vOut = verifier.getOutputStream();

            vOut.write(cvCertificate.getBody().getEncoded(ASN1Encoding.DER));

            vOut.close();

            return verifier.verify(cvCertificate.getSignature());
        }
        catch (Exception e)
        {
            throw new EACException("unable to process signature: " + e.getMessage(), e);
        }
    }
}
