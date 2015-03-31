package org.spongycastle.pqc.crypto.gmss;

import org.spongycastle.crypto.Digest;

public interface GMSSDigestProvider
{
    Digest get();
}
