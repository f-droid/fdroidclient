package org.spongycastle.cert.cmp;

import java.io.IOException;
import java.io.OutputStream;

import org.spongycastle.asn1.ASN1Encodable;
import org.spongycastle.asn1.DEROutputStream;

class CMPUtil
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
            throw new CMPRuntimeException("unable to DER encode object: " + e.getMessage(), e);
        }
    }
}
