package org.spongycastle.crypto.tls;

import java.io.ByteArrayOutputStream;

import org.spongycastle.crypto.Digest;

class DigestInputBuffer extends ByteArrayOutputStream
{
    void updateDigest(Digest d)
    {
        d.update(this.buf, 0, count);
    }
}
