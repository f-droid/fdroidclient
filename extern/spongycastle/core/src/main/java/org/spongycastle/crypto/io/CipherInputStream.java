package org.spongycastle.crypto.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.spongycastle.crypto.BufferedBlockCipher;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.SkippingCipher;
import org.spongycastle.crypto.StreamCipher;
import org.spongycastle.crypto.modes.AEADBlockCipher;
import org.spongycastle.util.Arrays;

/**
 * A CipherInputStream is composed of an InputStream and a cipher so that read() methods return data
 * that are read in from the underlying InputStream but have been additionally processed by the
 * Cipher. The cipher must be fully initialized before being used by a CipherInputStream.
 * <p>
 * For example, if the Cipher is initialized for decryption, the
 * CipherInputStream will attempt to read in data and decrypt them,
 * before returning the decrypted data.
 */
public class CipherInputStream
    extends FilterInputStream
{
    private static final int INPUT_BUF_SIZE = 2048;

    private SkippingCipher skippingCipher;
    private byte[] inBuf;

    private BufferedBlockCipher bufferedBlockCipher;
    private StreamCipher streamCipher;
    private AEADBlockCipher aeadBlockCipher;

    private byte[] buf;
    private byte[] markBuf;


    private int bufOff;
    private int maxBuf;
    private boolean finalized;
    private long markPosition;
    private int markBufOff;

    /**
     * Constructs a CipherInputStream from an InputStream and a
     * BufferedBlockCipher.
     */
    public CipherInputStream(
        InputStream is,
        BufferedBlockCipher cipher)
    {
        this(is, cipher, INPUT_BUF_SIZE);
    }

    /**
     * Constructs a CipherInputStream from an InputStream and a StreamCipher.
     */
    public CipherInputStream(
        InputStream is,
        StreamCipher cipher)
    {
        this(is, cipher, INPUT_BUF_SIZE);
    }

    /**
     * Constructs a CipherInputStream from an InputStream and an AEADBlockCipher.
     */
    public CipherInputStream(
        InputStream is,
        AEADBlockCipher cipher)
    {
        this(is, cipher, INPUT_BUF_SIZE);
    }

    /**
     * Constructs a CipherInputStream from an InputStream, a
     * BufferedBlockCipher, and a specified internal buffer size.
     */
    public CipherInputStream(
        InputStream is,
        BufferedBlockCipher cipher,
        int bufSize)
    {
        super(is);

        this.bufferedBlockCipher = cipher;
        this.inBuf = new byte[bufSize];
        this.skippingCipher = (cipher instanceof SkippingCipher) ? (SkippingCipher)cipher : null;
    }

    /**
     * Constructs a CipherInputStream from an InputStream, a StreamCipher, and a specified internal buffer size.
     */
    public CipherInputStream(
        InputStream is,
        StreamCipher cipher,
        int bufSize)
    {
        super(is);

        this.streamCipher = cipher;
        this.inBuf = new byte[bufSize];
        this.skippingCipher = (cipher instanceof SkippingCipher) ? (SkippingCipher)cipher : null;
    }

    /**
     * Constructs a CipherInputStream from an InputStream, an AEADBlockCipher, and a specified internal buffer size.
     */
    public CipherInputStream(
        InputStream is,
        AEADBlockCipher cipher,
        int bufSize)
    {
        super(is);

        this.aeadBlockCipher = cipher;
        this.inBuf = new byte[bufSize];
        this.skippingCipher = (cipher instanceof SkippingCipher) ? (SkippingCipher)cipher : null;
    }

    /**
     * Read data from underlying stream and process with cipher until end of stream or some data is
     * available after cipher processing.
     *
     * @return -1 to indicate end of stream, or the number of bytes (> 0) available.
     */
    private int nextChunk()
        throws IOException
    {
        if (finalized)
        {
            return -1;
        }

        bufOff = 0;
        maxBuf = 0;

        // Keep reading until EOF or cipher processing produces data
        while (maxBuf == 0)
        {
            int read = in.read(inBuf);
            if (read == -1)
            {
                finaliseCipher();
                if (maxBuf == 0)
                {
                    return -1;
                }
                return maxBuf;
            }

            try
            {
                ensureCapacity(read, false);
                if (bufferedBlockCipher != null)
                {
                    maxBuf = bufferedBlockCipher.processBytes(inBuf, 0, read, buf, 0);
                }
                else if (aeadBlockCipher != null)
                {
                    maxBuf = aeadBlockCipher.processBytes(inBuf, 0, read, buf, 0);
                }
                else
                {
                    streamCipher.processBytes(inBuf, 0, read, buf, 0);
                    maxBuf = read;
                }
            }
            catch (Exception e)
            {
                throw new CipherIOException("Error processing stream ", e);
            }
        }
        return maxBuf;
    }

    private void finaliseCipher()
        throws IOException
    {
        try
        {
            finalized = true;
            ensureCapacity(0, true);
            if (bufferedBlockCipher != null)
            {
                maxBuf = bufferedBlockCipher.doFinal(buf, 0);
            }
            else if (aeadBlockCipher != null)
            {
                maxBuf = aeadBlockCipher.doFinal(buf, 0);
            }
            else
            {
                maxBuf = 0; // a stream cipher
            }
        }
        catch (final InvalidCipherTextException e)
        {
            throw new InvalidCipherTextIOException("Error finalising cipher", e);
        }
        catch (Exception e)
        {
            throw new IOException("Error finalising cipher " + e);
        }
    }

    /**
     * Reads data from the underlying stream and processes it with the cipher until the cipher
     * outputs data, and returns the next available byte.
     * <p>
     * If the underlying stream is exhausted by this call, the cipher will be finalised.
     * </p>
     * @throws IOException if there was an error closing the input stream.
     * @throws InvalidCipherTextIOException if the data read from the stream was invalid ciphertext
     * (e.g. the cipher is an AEAD cipher and the ciphertext tag check fails).
     */
    public int read()
        throws IOException
    {
        if (bufOff >= maxBuf)
        {
            if (nextChunk() < 0)
            {
                return -1;
            }
        }

        return buf[bufOff++] & 0xff;
    }

    /**
     * Reads data from the underlying stream and processes it with the cipher until the cipher
     * outputs data, and then returns up to <code>b.length</code> bytes in the provided array.
     * <p>
     * If the underlying stream is exhausted by this call, the cipher will be finalised.
     * </p>
     * @param b the buffer into which the data is read.
     * @return the total number of bytes read into the buffer, or <code>-1</code> if there is no
     *         more data because the end of the stream has been reached.
     * @throws IOException if there was an error closing the input stream.
     * @throws InvalidCipherTextIOException if the data read from the stream was invalid ciphertext
     * (e.g. the cipher is an AEAD cipher and the ciphertext tag check fails).
     */
    public int read(
        byte[] b)
        throws IOException
    {
        return read(b, 0, b.length);
    }

    /**
     * Reads data from the underlying stream and processes it with the cipher until the cipher
     * outputs data, and then returns up to <code>len</code> bytes in the provided array.
     * <p>
     * If the underlying stream is exhausted by this call, the cipher will be finalised.
     * </p>
     * @param b   the buffer into which the data is read.
     * @param off the start offset in the destination array <code>b</code>
     * @param len the maximum number of bytes read.
     * @return the total number of bytes read into the buffer, or <code>-1</code> if there is no
     *         more data because the end of the stream has been reached.
     * @throws IOException if there was an error closing the input stream.
     * @throws InvalidCipherTextIOException if the data read from the stream was invalid ciphertext
     * (e.g. the cipher is an AEAD cipher and the ciphertext tag check fails).
     */
    public int read(
        byte[] b,
        int off,
        int len)
        throws IOException
    {
        if (bufOff >= maxBuf)
        {
            if (nextChunk() < 0)
            {
                return -1;
            }
        }

        int toSupply = Math.min(len, available());
        System.arraycopy(buf, bufOff, b, off, toSupply);
        bufOff += toSupply;
        return toSupply;
    }

    public long skip(
        long n)
        throws IOException
    {
        if (n <= 0)
        {
            return 0;
        }

        if (skippingCipher != null)
        {
            int avail = available();
            if (n <= avail)
            {
                bufOff += n;

                return n;
            }

            bufOff = maxBuf;

            long skip = in.skip(n - avail);

            long cSkip = skippingCipher.skip(skip);

            if (skip != cSkip)
            {
                throw new IOException("Unable to skip cipher " + skip + " bytes.");
            }

            return skip + avail;
        }
        else
        {
            int skip = (int)Math.min(n, available());
            bufOff += skip;

            return skip;
        }
    }

    public int available()
        throws IOException
    {
        return maxBuf - bufOff;
    }

    /**
     * Ensure the cipher text buffer has space sufficient to accept an upcoming output.
     *
     * @param updateSize the size of the pending update.
     * @param finalOutput <code>true</code> iff this the cipher is to be finalised.
     */
    private void ensureCapacity(int updateSize, boolean finalOutput)
    {
        int bufLen = updateSize;
        if (finalOutput)
        {
            if (bufferedBlockCipher != null)
            {
                bufLen = bufferedBlockCipher.getOutputSize(updateSize);
            }
            else if (aeadBlockCipher != null)
            {
                bufLen = aeadBlockCipher.getOutputSize(updateSize);
            }
        }
        else
        {
            if (bufferedBlockCipher != null)
            {
                bufLen = bufferedBlockCipher.getUpdateOutputSize(updateSize);
            }
            else if (aeadBlockCipher != null)
            {
                bufLen = aeadBlockCipher.getUpdateOutputSize(updateSize);
            }
        }

        if ((buf == null) || (buf.length < bufLen))
        {
            buf = new byte[bufLen];
        }
    }

    /**
     * Closes the underlying input stream and finalises the processing of the data by the cipher.
     *
     * @throws IOException if there was an error closing the input stream.
     * @throws InvalidCipherTextIOException if the data read from the stream was invalid ciphertext
     *             (e.g. the cipher is an AEAD cipher and the ciphertext tag check fails).
     */
    public void close()
        throws IOException
    {
        try
        {
            in.close();
        }
        finally
        {
            if (!finalized)
            {
                // Reset the cipher, discarding any data buffered in it
                // Errors in cipher finalisation trump I/O error closing input
                finaliseCipher();
            }
        }
        maxBuf = bufOff = 0;
        markBufOff = 0;
        markPosition = 0;
        if (markBuf != null)
        {
            Arrays.fill(markBuf, (byte)0);
            markBuf = null;
        }
        if (buf != null)
        {
            Arrays.fill(buf, (byte)0);
            buf = null;
        }
        Arrays.fill(inBuf, (byte)0);
    }

    /**
     * Mark the current position.
     * <p>
     * This method only works if markSupported() returns true - which means the underlying stream supports marking, and the cipher passed
     * in to this stream's constructor is a SkippingCipher (so capable of being reset to an arbitrary point easily).
     * </p>
     * @param readlimit the maximum read ahead required before a reset() may be called.
     */
    public void mark(int readlimit)
    {
        in.mark(readlimit);
        if (skippingCipher != null)
        {
            markPosition = skippingCipher.getPosition();
        }

        if (buf != null)
        {
            markBuf = new byte[buf.length];
            System.arraycopy(buf, 0, markBuf, 0, buf.length);
        }

        markBufOff = bufOff;
    }

    /**
     * Reset to the last marked position, if supported.
     *
     * @throws IOException if marking not supported by the cipher used, or the underlying stream.
     */
    public void reset()
        throws IOException
    {
        if (skippingCipher == null)
        {
            throw new IOException("cipher must implement SkippingCipher to be used with reset()");
        }

        in.reset();

        skippingCipher.seekTo(markPosition);

        if (markBuf != null)
        {
            buf = markBuf;
        }

        bufOff = markBufOff;
     }

    /**
     * Return true if mark(readlimit) is supported. This will be true if the underlying stream supports marking and the
     * cipher used is a SkippingCipher,
     *
     * @return true if mark(readlimit) supported, false otherwise.
     */
    public boolean markSupported()
    {
        if (skippingCipher != null)
        {
            return in.markSupported();
        }

        return false;
    }

}
