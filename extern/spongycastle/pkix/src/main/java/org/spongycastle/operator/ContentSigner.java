package org.spongycastle.operator;

import java.io.OutputStream;

import org.spongycastle.asn1.x509.AlgorithmIdentifier;

public interface ContentSigner
{
    AlgorithmIdentifier getAlgorithmIdentifier();

    /**
     * Returns a stream that will accept data for the purpose of calculating
     * a signature. Use org.spongycastle.util.io.TeeOutputStream if you want to accumulate
     * the data on the fly as well.
     *
     * @return an OutputStream
     */
    OutputStream getOutputStream();

    /**
     * Returns a signature based on the current data written to the stream, since the
     * start or the last call to getSignature().
     *
     * @return bytes representing the signature.
     */
    byte[] getSignature();
}
