package org.spongycastle.crypto.tls;

import java.io.IOException;

import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.StreamCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.util.Arrays;

public class TlsStreamCipher
    implements TlsCipher
{
    private static boolean encryptThenMAC = false;

    protected TlsContext context;

    protected StreamCipher encryptCipher;
    protected StreamCipher decryptCipher;

    protected TlsMac writeMac;
    protected TlsMac readMac;

    public TlsStreamCipher(TlsContext context, StreamCipher clientWriteCipher,
        StreamCipher serverWriteCipher, Digest clientWriteDigest, Digest serverWriteDigest,
        int cipherKeySize) throws IOException
    {
        boolean isServer = context.isServer();

        this.context = context;

        this.encryptCipher = clientWriteCipher;
        this.decryptCipher = serverWriteCipher;

        int key_block_size = (2 * cipherKeySize) + clientWriteDigest.getDigestSize()
            + serverWriteDigest.getDigestSize();

        byte[] key_block = TlsUtils.calculateKeyBlock(context, key_block_size);

        int offset = 0;

        // Init MACs
        TlsMac clientWriteMac = new TlsMac(context, clientWriteDigest, key_block, offset,
            clientWriteDigest.getDigestSize());
        offset += clientWriteDigest.getDigestSize();
        TlsMac serverWriteMac = new TlsMac(context, serverWriteDigest, key_block, offset,
            serverWriteDigest.getDigestSize());
        offset += serverWriteDigest.getDigestSize();

        // Build keys
        KeyParameter clientWriteKey = new KeyParameter(key_block, offset, cipherKeySize);
        offset += cipherKeySize;
        KeyParameter serverWriteKey = new KeyParameter(key_block, offset, cipherKeySize);
        offset += cipherKeySize;

        if (offset != key_block_size)
        {
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }

        CipherParameters encryptParams, decryptParams;
        if (isServer)
        {
            this.writeMac = serverWriteMac;
            this.readMac = clientWriteMac;
            this.encryptCipher = serverWriteCipher;
            this.decryptCipher = clientWriteCipher;
            encryptParams = serverWriteKey;
            decryptParams = clientWriteKey;
        }
        else
        {
            this.writeMac = clientWriteMac;
            this.readMac = serverWriteMac;
            this.encryptCipher = clientWriteCipher;
            this.decryptCipher = serverWriteCipher;
            encryptParams = clientWriteKey;
            decryptParams = serverWriteKey;
        }

        this.encryptCipher.init(true, encryptParams);
        this.decryptCipher.init(false, decryptParams);
    }

    public int getPlaintextLimit(int ciphertextLimit)
    {
        return ciphertextLimit - writeMac.getSize();
    }

    public byte[] encodePlaintext(long seqNo, short type, byte[] plaintext, int offset, int len)
    {
        /*
         * TODO[draft-josefsson-salsa20-tls-02] Note that Salsa20 requires a 64-bit nonce. That
         * nonce is updated on the encryption of every TLS record, and is set to be the 64-bit TLS
         * record sequence number. In case of DTLS the 64-bit nonce is formed as the concatenation
         * of the 16-bit epoch with the 48-bit sequence number.
         */

        byte[] outBuf = new byte[len + writeMac.getSize()];

        encryptCipher.processBytes(plaintext, offset, len, outBuf, 0);

        if (encryptThenMAC)
        {
            byte[] mac = writeMac.calculateMac(seqNo, type, outBuf, 0, len);
            System.arraycopy(mac, 0, outBuf, len, mac.length);
        }
        else
        {
            byte[] mac = writeMac.calculateMac(seqNo, type, plaintext, offset, len);
            encryptCipher.processBytes(mac, 0, mac.length, outBuf, len);
        }

        return outBuf;
    }

    public byte[] decodeCiphertext(long seqNo, short type, byte[] ciphertext, int offset, int len)
        throws IOException
    {
        /*
         * TODO[draft-josefsson-salsa20-tls-02] Note that Salsa20 requires a 64-bit nonce. That
         * nonce is updated on the encryption of every TLS record, and is set to be the 64-bit TLS
         * record sequence number. In case of DTLS the 64-bit nonce is formed as the concatenation
         * of the 16-bit epoch with the 48-bit sequence number.
         */

        int macSize = readMac.getSize();
        if (len < macSize)
        {
            throw new TlsFatalAlert(AlertDescription.decode_error);
        }

        int plaintextLength = len - macSize;

        if (encryptThenMAC)
        {
            int ciphertextEnd = offset + len;
            checkMAC(seqNo, type, ciphertext, ciphertextEnd - macSize, ciphertextEnd, ciphertext, offset, plaintextLength);
            byte[] deciphered = new byte[plaintextLength];
            decryptCipher.processBytes(ciphertext, offset, plaintextLength, deciphered, 0);
            return deciphered;
        }
        else
        {
            byte[] deciphered = new byte[len];
            decryptCipher.processBytes(ciphertext, offset, len, deciphered, 0);
            checkMAC(seqNo, type, deciphered, plaintextLength, len, deciphered, 0, plaintextLength);
            return Arrays.copyOfRange(deciphered, 0, plaintextLength);
        }
    }

    private void checkMAC(long seqNo, short type, byte[] recBuf, int recStart, int recEnd, byte[] calcBuf, int calcOff, int calcLen)
        throws IOException
    {
        byte[] receivedMac = Arrays.copyOfRange(recBuf, recStart, recEnd);
        byte[] computedMac = readMac.calculateMac(seqNo, type, calcBuf, calcOff, calcLen);

        if (!Arrays.constantTimeAreEqual(receivedMac, computedMac))
        {
            throw new TlsFatalAlert(AlertDescription.bad_record_mac);
        }
    }
}
