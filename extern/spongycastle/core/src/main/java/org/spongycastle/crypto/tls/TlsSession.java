package org.spongycastle.crypto.tls;

public interface TlsSession
{
    SessionParameters exportSessionParameters();

    byte[] getSessionID();

    void invalidate();

    boolean isResumable();
}
