package org.spongycastle.cert.crmf;

import java.io.IOException;
import java.io.OutputStream;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.DEROutputStream;
import org.spongycastle.asn1.x509.ExtensionsGenerator;
import org.spongycastle.cert.CertIOException;

class CRMFUtil
{
    static void derEncodeToStream(ASN1Encodable obj, OutputStream stream)
    {
        DEROutputStream dOut = new DEROutputStream(stream);

        try
        {
            dOut.writeObject(obj);

            dOut.close();
        }
        catch (IOException e)
        {
            throw new CRMFRuntimeException("unable to DER encode object: " + e.getMessage(), e);
        }
    }

    static void addExtension(ExtensionsGenerator extGenerator, ASN1ObjectIdentifier oid, boolean isCritical, ASN1Encodable value)
        throws CertIOException
    {
        try
        {
            extGenerator.addExtension(oid, isCritical, value);
        }
        catch (IOException e)
        {
            throw new CertIOException("cannot encode extension: " + e.getMessage(), e);
        }
    }
}
