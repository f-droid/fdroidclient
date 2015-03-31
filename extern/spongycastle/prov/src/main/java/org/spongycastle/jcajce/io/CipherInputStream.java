package org.spongycastle.jcajce.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import org.spongycastle.crypto.io.InvalidCipherTextIOException;

/**
 * A CipherInputStream is composed of an InputStream and a cipher so that read() methods return data
 * that are read in from the underlying InputStream but have been additionally processed by the
 * Cipher. The cipher must be fully initialized before being used by a CipherInputStream.
 * <p/>
 * For example, if the Cipher is initialized for decryption, the CipherInputStream will attempt to
 * read in data and decrypt them, before returning the decrypted data.
 * <p/>
 * This is a reimplementation of {@link javax.crypto.CipherInputStream} that is safe for use with
 * AEAD block ciphers, and does not silently catch {@link BadPaddingException} and
 * {@link IllegalBlockSizeException} errors. Any errors that occur during {@link Cipher#doFinal()
 * finalisation} are rethrown wrapped in an {@link InvalidCipherTextIOException}.
 */
public class CipherInputStream
    extends FilterInputStream
{
    private final Cipher cipher;
    private final byte[] inputBuffer = new byte[512];
    private boolean finalized = false;
    private byte[] buf;
    private int maxBuf;
    private int bufOff;

    /**
     * Constructs a CipherInputStream from an InputStream and an initialised Cipher.
     */
    public CipherInputStream(InputStream input, Cipher cipher)
    {
        super(input);
        this.cipher = cipher;
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
            int read = in.read(inputBuffer);
            if (read == -1)
            {
                buf = finaliseCipher();
                if ((buf == null) || (buf.length == 0))
                {
                    return -1;
                }
                maxBuf = buf.length;
                return maxBuf;
            }

            buf = cipher.update(inputBuffer, 0, read);
            if (buf != null)
            {
                maxBuf = buf.length;
            }
        }
        return maxBuf;
    }

    private byte[] finaliseCipher()
        throws InvalidCipherTextIOException
    {
        try
        {
            finalized = true;
            return cipher.doFinal();
        }
        catch (GeneralSecurityException e)
        {
            throw new InvalidCipherTextIOException("Error finalising cipher", e);
        }
    }

    /**
     * Reads data from the underlying stream and processes it with the cipher until the cipher
     * outputs data, and returns the next available byte.
     * <p/>
     * If the underlying stream is exhausted by this call, the cipher will be finalised.
     *
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
     * outputs data, and then returns up to <code>len</code> bytes in the provided array.
     * <p/>
     * If the underlying stream is exhausted by this call, the cipher will be finalised.
     *
     * @param b   the buffer into which the data is read.
     * @param off the start offset in the destination array <code>b</code>
     * @param len the maximum number of bytes read.
     * @return the total number of bytes read into the buffer, or <code>-1</code> if there is no
     *         more data because the end of the stream has been reached.
     * @throws IOException if there was an error closing the input stream.
     * @throws InvalidCipherTextIOException if the data read from the stream was invalid ciphertext
     * (e.g. the cipher is an AEAD cipher and the ciphertext tag check fails).
     */
    public int read(byte[] b, int off, int len)
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

    public long skip(long n)
        throws IOException
    {
        if (n <= 0)
        {
            return 0;
        }

        int skip = (int)Math.min(n, available());
        bufOff += skip;
        return skip;
    }

    public int available()
        throws IOException
    {
        return maxBuf - bufOff;
    }

    /**
     * Closes the underlying input stream, and then finalises the processing of the data by the
     * cipher.
     *
     * @throws IOException if there was an error closing the input stream.
     * @throws InvalidCipherTextIOException if the data read from the stream was invalid ciphertext
     * (e.g. the cipher is an AEAD cipher and the ciphertext tag check fails).
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
    }

    public void mark(int readlimit)
    {
    }

    public void reset()
        throws IOException
    {
    }

    public boolean markSupported()
    {
        return false;
    }

}
