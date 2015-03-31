package org.spongycastle.crypto.tls;

import java.io.IOException;

public abstract class AbstractTlsPeer
    implements TlsPeer
{
    public void notifySecureRenegotiation(boolean secureRenegotiation) throws IOException
    {
        if (!secureRenegotiation)
        {
            /*
             * RFC 5746 3.4/3.6. In this case, some clients/servers may want to terminate the handshake instead
             * of continuing; see Section 4.1/4.3 for discussion.
             */
            throw new TlsFatalAlert(AlertDescription.handshake_failure);
        }
    }

    public void notifyAlertRaised(short alertLevel, short alertDescription, String message, Exception cause)
    {
    }

    public void notifyAlertReceived(short alertLevel, short alertDescription)
    {
    }

    public void notifyHandshakeComplete() throws IOException
    {
    }
}
