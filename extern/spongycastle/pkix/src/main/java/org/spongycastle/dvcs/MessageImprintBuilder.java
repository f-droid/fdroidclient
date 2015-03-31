package org.spongycastle.dvcs;

import java.io.OutputStream;

import org.spongycastle.asn1.x509.DigestInfo;
import org.spongycastle.operator.DigestCalculator;

public class MessageImprintBuilder
{
    private final DigestCalculator digestCalculator;

    public MessageImprintBuilder(DigestCalculator digestCalculator)
    {
        this.digestCalculator = digestCalculator;
    }

    public MessageImprint build(byte[] message)
        throws DVCSException
    {
        try
        {
            OutputStream dOut = digestCalculator.getOutputStream();

            dOut.write(message);

            dOut.close();

            return new MessageImprint(new DigestInfo(digestCalculator.getAlgorithmIdentifier(), digestCalculator.getDigest()));
        }
        catch (Exception e)
        {
            throw new DVCSException("unable to build MessageImprint: " + e.getMessage(), e);
        }
    }
}
