package org.spongycastle.jcajce.provider.asymmetric.x509;

import java.io.IOException;
import java.io.InputStream;

import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.util.encoders.Base64;

public class PEMUtil
{
    private final String _header1;
    private final String _header2;
    private final String _footer1;
    private final String _footer2;

    PEMUtil(
        String type)
    {
        _header1 = "-----BEGIN " + type + "-----";
        _header2 = "-----BEGIN X509 " + type + "-----";
        _footer1 = "-----END " + type + "-----";
        _footer2 = "-----END X509 " + type + "-----";
    }

    private String readLine(
        InputStream in)
        throws IOException
    {
        int             c;
        StringBuffer l = new StringBuffer();

        do
        {
            while (((c = in.read()) != '\r') && c != '\n' && (c >= 0))
            {
                if (c == '\r')
                {
                    continue;
                }

                l.append((char)c);
            }
        }
        while (c >= 0 && l.length() == 0);

        if (c < 0)
        {
            return null;
        }

        return l.toString();
    }

    ASN1Sequence readPEMObject(
        InputStream in)
        throws IOException
    {
        String line;
        StringBuffer pemBuf = new StringBuffer();

        while ((line = readLine(in)) != null)
        {
            if (line.startsWith(_header1) || line.startsWith(_header2))
            {
                break;
            }
        }

        while ((line = readLine(in)) != null)
        {
            if (line.startsWith(_footer1) || line.startsWith(_footer2))
            {
                break;
            }

            pemBuf.append(line);
        }

        if (pemBuf.length() != 0)
        {
            try
            {
                return ASN1Sequence.getInstance(Base64.decode(pemBuf.toString()));
            }
            catch (Exception e)
            {
                throw new IOException("malformed PEM data encountered");
            }
        }

        return null;
    }
}
