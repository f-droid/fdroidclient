package org.spongycastle.jcajce.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import org.spongycastle.crypto.io.InvalidCipherTextIOException;

/**
 * A CipherOutputStream is composed of an OutputStream and a cipher so that write() methods process
 * the written data with the cipher, and the output of the cipher is in turn written to the
 * underlying OutputStream. The cipher must be fully initialized before being used by a
 * CipherInputStream.
 * <p/>
 * For example, if the cipher is initialized for encryption, the CipherOutputStream will encrypt the
 * data before writing the encrypted data to the underlying stream.
 * <p/>
 * This is a reimplementation of {@link javax.crypto.CipherOutputStream} that is safe for use with
 * AEAD block ciphers, and does not silently catch {@link BadPaddingException} and
 * {@link IllegalBlockSizeException} errors. Any errors that occur during {@link Cipher#doFinal()
 * finalisation} are rethrown wrapped in an {@link InvalidCipherTextIOException}.
 */
public class CipherOutputStream
    extends FilterOutputStream
{
    private final Cipher cipher;
    private final byte[] oneByte = new byte[1];

    /**
     * Constructs a CipherOutputStream from an OutputStream and a Cipher.
     */
    public CipherOutputStream(OutputStream output, Cipher cipher)
    {
        super(output);
        this.cipher = cipher;
    }

    /**
     * Writes the specified byte to this output stream.
     *
     * @param b the <code>byte</code>.
     * @throws java.io.IOException if an I/O error occurs.
     */
    public void write(int b)
        throws IOException
    {
        oneByte[0] = (byte)b;
        write(oneByte, 0, 1);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array starting at offset
     * <code>off</code> to this output stream.
     *
     * @param b   the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     * @throws java.io.IOException if an I/O error occurs.
     */
    public void write(byte[] b, int off, int len)
        throws IOException
    {
        byte[] outData = cipher.update(b, off, len);
        if (outData != null)
        {
            out.write(outData);
        }
    }

    /**
     * Flushes this output stream by forcing any buffered output bytes that have already been
     * processed by the encapsulated cipher object to be written out.
     * <p/>
     * <p/>
     * Any bytes buffered by the encapsulated cipher and waiting to be processed by it will not be
     * written out. For example, if the encapsulated cipher is a block cipher, and the total number
     * of bytes written using one of the <code>write</code> methods is less than the cipher's block
     * size, no bytes will be written out.
     *
     * @throws java.io.IOException if an I/O error occurs.
     */
    public void flush()
        throws IOException
    {
        out.flush();
    }

    /**
     * Closes this output stream and releases any system resources associated with this stream.
     * <p/>
     * This method invokes the <code>doFinal</code> method of the encapsulated cipher object, which
     * causes any bytes buffered by the encapsulated cipher to be processed. The result is written
     * out by calling the <code>flush</code> method of this output stream.
     * <p/>
     * This method resets the encapsulated cipher object to its initial state and calls the
     * <code>close</code> method of the underlying output stream.
     *
     * @throws java.io.IOException if an I/O error occurs.
     * @throws InvalidCipherTextIOException if the data written to this stream was invalid
     * ciphertext (e.g. the cipher is an AEAD cipher and the ciphertext tag check
     * fails).
     */
    public void close()
        throws IOException
    {
        IOException error = null;
        try
        {
            byte[] outData = cipher.doFinal();
            if (outData != null)
            {
                out.write(outData);
            }
        }
        catch (GeneralSecurityException e)
        {
            error = new InvalidCipherTextIOException("Error during cipher finalisation", e);
        }
        catch (Exception e)
        {
            error = new IOException("Error closing stream: " + e);
        }
        try
        {
            flush();
            out.close();
        }
        catch (IOException e)
        {
            // Invalid ciphertext takes precedence over close error
            if (error == null)
            {
                error = e;
            }
        }
        if (error != null)
        {
            throw error;
        }
    }

}
